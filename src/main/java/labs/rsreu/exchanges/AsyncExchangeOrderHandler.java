package labs.rsreu.exchanges;

import labs.rsreu.clients.Client;
import labs.rsreu.currencies.Currency;
import labs.rsreu.orders.Order;
import labs.rsreu.orders.OrderType;
import labs.rsreu.orders.TransactionInfo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

public class AsyncExchangeOrderHandler implements Runnable {
    private final BlockingQueue<Order> orderQueue;
    private boolean isExchangeClosed = false;


    public AsyncExchangeOrderHandler(BlockingQueue<Order> orderQueue) {
        this.orderQueue = orderQueue;
    }


    @Override
    public void run() {
        try {
            while (!isExchangeClosed) {
                Order order = orderQueue.take();

                if (order.getType() == OrderType.BUY) {
                    processBuyOrder(order);
                } else {
                    processSellOrder(order);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();  // Восстановим флаг прерывания
            System.err.println("Order processing was interrupted: " + e.getMessage());
        }
    }

    private void processBuyOrder(Order buyOrder) {
        List<Order> sellOrders = getSellOrders();
        Iterator<Order> iterator = sellOrders.iterator();
        while (iterator.hasNext()) {
            Order eachSellOrder = iterator.next();
            if (eachSellOrder.getClientId() != buyOrder.getClientId()
                    && eachSellOrder.getCurrencyPair().equals(buyOrder.getCurrencyPair())
                    && eachSellOrder.getPrice().compareTo(buyOrder.getPrice()) <= 0) {

                BigDecimal transactionPrice = buyOrder.getPrice();
                List<BigDecimal> transactionsAmountCurrency = processTransaction(buyOrder, eachSellOrder, transactionPrice);

                BigDecimal transactionAmountBuyCurrency = transactionsAmountCurrency.get(1);
                BigDecimal transactionAmountSellCurrency = transactionsAmountCurrency.get(0);

                buyOrder.notifyStatus(new TransactionInfo(
                        new Order(OrderType.BUY, buyOrder.getClientId(), buyOrder.getCurrencyPair(), transactionAmountBuyCurrency, transactionAmountSellCurrency),
                        new Order(OrderType.SELL, eachSellOrder.getClientId(), eachSellOrder.getCurrencyPair(), transactionAmountBuyCurrency, transactionAmountSellCurrency)
                ));

                // Обновляем остатки в ордерах, но не устанавливаем значения равные нулю
                BigDecimal remainingAmountBuyCurrency = buyOrder.getAmountFirst().subtract(transactionAmountBuyCurrency);
                BigDecimal remainingAmountSellCurrency = buyOrder.getAmountSecond().subtract(transactionAmountSellCurrency);

                BigDecimal remainingAmountSellOrderBuyCurrency = eachSellOrder.getAmountFirst().subtract(transactionAmountBuyCurrency);
                BigDecimal remainingAmountSellOrderSellCurrency = eachSellOrder.getAmountSecond().subtract(transactionAmountSellCurrency);

                // Обновление ордера на покупку, если остаток больше нуля
                if (remainingAmountBuyCurrency.compareTo(BigDecimal.ZERO) == 0) {
//                    System.out.println("Покупатель купил все, заявка удалена.");
//                    resultCallback.accept("Покупатель купил все, заявка удалена.");
                    buyOrder.notifyStatus(new TransactionInfo("Buyer " + buyOrder.getClientId() + " all bought. Order "
                            + buyOrder.getId() + " closed."));

                } else {
                    buyOrder.setAmountFirst(remainingAmountBuyCurrency);
                    buyOrder.setAmountSecond(remainingAmountSellCurrency);
                    buyOrder.notifyStatus(new TransactionInfo("Buyer " + buyOrder.getClientId() + " bought some part. Order "
                            + buyOrder.getId() + " still open."));
                    orderQueue.add(buyOrder);
                }

                // Обновление ордера на продажу, если остаток больше нуля
                if (remainingAmountSellOrderBuyCurrency.compareTo(BigDecimal.ZERO) == 0) {
                    iterator.remove();
                    orderQueue.remove(eachSellOrder);
//                    sellOrdersQueue.remove(eachSellOrder);
                    eachSellOrder.notifyStatus(new TransactionInfo("Seller " + eachSellOrder.getClientId() + " all sold. Order "
                            + eachSellOrder.getId() + " closed."));
//                    System.out.println("Продавец продал все, заявка удалена.");
//                    resultCallback.accept("Продавец продал все, заявка удалена.");

                } else {
                    eachSellOrder.setAmountFirst(remainingAmountSellOrderBuyCurrency);
                    eachSellOrder.setAmountSecond(remainingAmountSellOrderSellCurrency);
                    eachSellOrder.notifyStatus(new TransactionInfo("Seller " + eachSellOrder.getClientId() + " sold some part. Order "
                            + eachSellOrder.getId() + " still open."));
                }
            }
        }
    }

    private void processSellOrder(Order sellOrder) {
        List<Order> buyOrders = getBuyOrders();
        Iterator<Order> iterator = buyOrders.iterator();
//        Iterator<Order> iterator = buyOrdersQueue.iterator();

        while (iterator.hasNext()) {
            Order eachBuyOrder = iterator.next();

            // Убедимся, что ордера принадлежат разным клиентам, что валютные пары совпадают и что цена удовлетворяет условиям
            if (eachBuyOrder.getClientId() != sellOrder.getClientId()
                    && eachBuyOrder.getCurrencyPair().equals(sellOrder.getCurrencyPair())
                    && eachBuyOrder.getPrice().compareTo(sellOrder.getPrice()) >= 0) {


                BigDecimal transactionPrice = sellOrder.getPrice();
                List<BigDecimal> transactionsAmountCurrency = processTransaction(eachBuyOrder, sellOrder, transactionPrice);
                BigDecimal transactionAmountBuyCurrency = transactionsAmountCurrency.get(1);
                BigDecimal transactionAmountSellCurrency = transactionsAmountCurrency.get(0);


                sellOrder.notifyStatus(new TransactionInfo(
                        new Order(OrderType.BUY, eachBuyOrder.getClientId(), eachBuyOrder.getCurrencyPair(), transactionAmountBuyCurrency, transactionAmountSellCurrency),
                        new Order(OrderType.SELL, sellOrder.getClientId(), sellOrder.getCurrencyPair(), transactionAmountBuyCurrency, transactionAmountSellCurrency)
                ));

                // Обновляем остатки в ордерах
                BigDecimal remainingAmountSellCurrency = sellOrder.getAmountFirst().subtract(transactionAmountBuyCurrency);
                BigDecimal remainingAmountBuyCurrency = sellOrder.getAmountSecond().subtract(transactionAmountSellCurrency);

                BigDecimal remainingAmountBuyOrderSellCurrency = eachBuyOrder.getAmountFirst().subtract(transactionAmountBuyCurrency);
                BigDecimal remainingAmountBuyOrderBuyCurrency = eachBuyOrder.getAmountSecond().subtract(transactionAmountSellCurrency);

                // Обновление ордера на продажу, если остаток больше нуля
                if (remainingAmountSellCurrency.compareTo(BigDecimal.ZERO) == 0) {
//                    System.out.println("Продавец продал все, заявка удалена.");
                    sellOrder.notifyStatus(new TransactionInfo("Seller " + sellOrder.getClientId() + " all sold. Order "
                            + sellOrder.getId() + " closed."));
//                    resultCallback.accept("Продавец продал все, заявка удалена.");
                } else {
                    sellOrder.setAmountFirst(remainingAmountSellCurrency);
                    sellOrder.setAmountSecond(remainingAmountBuyCurrency);
                    sellOrder.notifyStatus(new TransactionInfo("Seller " + sellOrder.getClientId() + " sold some part. Order "
                            + sellOrder.getId() + " still open."));
                    orderQueue.add(sellOrder);
                }

                // Обновление ордера на покупку, если остаток больше нуля
                if (remainingAmountBuyOrderSellCurrency.compareTo(BigDecimal.ZERO) == 0) {
                    iterator.remove();
                    orderQueue.remove(eachBuyOrder);
//                    buyOrdersQueue.remove(eachBuyOrder);
//                    System.out.println("Покупатель купил все, заявка удалена.");
                    eachBuyOrder.notifyStatus(new TransactionInfo("Buyer " + eachBuyOrder.getClientId() + " all bought. Order "
                            + eachBuyOrder.getId() + " closed."));
//                    resultCallback.accept("Покупатель купил все, заявка удалена.");
                } else {
                    eachBuyOrder.setAmountFirst(remainingAmountBuyOrderSellCurrency);
                    eachBuyOrder.setAmountSecond(remainingAmountBuyOrderBuyCurrency);
                    eachBuyOrder.notifyStatus(new TransactionInfo("Buyer " + eachBuyOrder.getClientId() + " bought some part. Order "
                            + eachBuyOrder.getId() + " still open."));

                }
            }
        }

    }


    private List<BigDecimal> processTransaction(Order buyOrder, Order sellOrder, BigDecimal transactionPrice) {
        int buyer = buyOrder.getClientId();
        int seller = sellOrder.getClientId();

        Currency buyCurrency = buyOrder.getCurrencyPair().getCurrencyFirst();
        Currency sellCurrency = buyOrder.getCurrencyPair().getCurrencySecond();

        BigDecimal buyAmountBuyer = buyOrder.getAmountFirst();
        BigDecimal sellAmountSeller = sellOrder.getAmountFirst();
        BigDecimal buyAmountOrderTmp = buyAmountBuyer.min(sellAmountSeller);
        BigDecimal priceBuyAmountOrderTmp = buyAmountOrderTmp.multiply(transactionPrice);


        return Arrays.asList(priceBuyAmountOrderTmp, buyAmountOrderTmp);

    }

    private List<Order> getSellOrders() {
        return orderQueue.stream()
                .filter(order -> order.getType() == OrderType.SELL) // Фильтруем ордера на продажу
                .sorted(Comparator.comparing(Order::getPrice).reversed()) // Сортируем по цене (убывание)
                .collect(Collectors.toList());
    }

    private List<Order> getBuyOrders() {
        return orderQueue.stream()
                .filter(order -> order.getType() == OrderType.BUY) // Фильтруем ордера на покупку
                .sorted(Comparator.comparing(Order::getPrice)) // Сортируем по цене
                .collect(Collectors.toList());
    }


    public void closeExchange() {
        isExchangeClosed = true;

        for (Order order : orderQueue) {
            order.notifyStatus(new TransactionInfo(true,"Order canceled: Exchange is closed."));
        }


        orderQueue.clear();
    }
}
