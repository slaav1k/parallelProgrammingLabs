package labs.rsreu.exchanges;

import labs.rsreu.clients.Client;
import labs.rsreu.currencies.Currency;
import labs.rsreu.orders.Order;
import labs.rsreu.orders.OrderType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

public class AsyncExchangeOrderHandler implements Runnable {
    private final BlockingQueue<Order> orderQueue;
    private boolean isExchangeClosed = false;
//    private final PriorityQueue<Order> sellOrdersQueue;
//    private final PriorityQueue<Order> buyOrdersQueue;

    public AsyncExchangeOrderHandler(BlockingQueue<Order> orderQueue) {
        this.orderQueue = orderQueue;
//        this.sellOrdersQueue = new PriorityQueue<>(Comparator.comparing(Order::getPrice).reversed());
//        this.buyOrdersQueue = new PriorityQueue<>(Comparator.comparing(Order::getPrice));
    }


    @Override
    public void run() {
        try {
            while (!isExchangeClosed) {
                Order order = orderQueue.take();

                if (order.getType() == OrderType.BUY) {
//                    buyOrdersQueue.offer(order); // офер возращает тру фалсе и позволяет
                    // обработать исключение без выбрасывания
                    processBuyOrder(order);
                } else {
//                    sellOrdersQueue.offer(order);
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
//        Iterator<Order> iterator = sellOrdersQueue.iterator();
        while (iterator.hasNext()) {
            Order eachSellOrder = iterator.next();
            if (eachSellOrder.getClient().getId() != buyOrder.getClient().getId()
                    && eachSellOrder.getCurrencyPair().equals(buyOrder.getCurrencyPair())
                    && eachSellOrder.getPrice().compareTo(buyOrder.getPrice()) <= 0) {

                BigDecimal transactionPrice = buyOrder.getPrice();
                List<BigDecimal> transactionsAmountCurrency = processTransaction(buyOrder, eachSellOrder, transactionPrice);

                BigDecimal transactionAmountBuyCurrency = transactionsAmountCurrency.get(1);
                BigDecimal transactionAmountSellCurrency = transactionsAmountCurrency.get(0);

                // Обновляем остатки в ордерах, но не устанавливаем значения равные нулю
                BigDecimal remainingAmountBuyCurrency = buyOrder.getAmountFirst().subtract(transactionAmountBuyCurrency);
                BigDecimal remainingAmountSellCurrency = buyOrder.getAmountSecond().subtract(transactionAmountSellCurrency);

                BigDecimal remainingAmountSellOrderBuyCurrency = eachSellOrder.getAmountFirst().subtract(transactionAmountBuyCurrency);
                BigDecimal remainingAmountSellOrderSellCurrency = eachSellOrder.getAmountSecond().subtract(transactionAmountSellCurrency);

                // Обновление ордера на покупку, если остаток больше нуля
                if (remainingAmountBuyCurrency.compareTo(BigDecimal.ZERO) == 0) {
//                    System.out.println("Покупатель купил все, заявка удалена.");
//                    resultCallback.accept("Покупатель купил все, заявка удалена.");
                    buyOrder.notifyStatus("Buyer " + buyOrder.getClient().getId() + " all bought. Order "
                            + buyOrder.getId() + " closed.");

                } else {
                    buyOrder.setAmountFirst(remainingAmountBuyCurrency);
                    buyOrder.setAmountSecond(remainingAmountSellCurrency);
                    buyOrder.notifyStatus("Buyer " + buyOrder.getClient().getId() + " bought some part. Order "
                            + buyOrder.getId() + " still open.");
                    orderQueue.add(buyOrder);
                }

                // Обновление ордера на продажу, если остаток больше нуля
                if (remainingAmountSellOrderBuyCurrency.compareTo(BigDecimal.ZERO) == 0) {
                    iterator.remove();
                    orderQueue.remove(eachSellOrder);
//                    sellOrdersQueue.remove(eachSellOrder);
                    eachSellOrder.notifyStatus("Seller " + eachSellOrder.getClient().getId() + " all sold. Order "
                            + eachSellOrder.getId() + " closed.");
//                    System.out.println("Продавец продал все, заявка удалена.");
//                    resultCallback.accept("Продавец продал все, заявка удалена.");

                } else {
                    eachSellOrder.setAmountFirst(remainingAmountSellOrderBuyCurrency);
                    eachSellOrder.setAmountSecond(remainingAmountSellOrderSellCurrency);
                    eachSellOrder.notifyStatus("Seller " + eachSellOrder.getClient().getId() + " sold some part. Order "
                            + eachSellOrder.getId() + " still open.");
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
            if (eachBuyOrder.getClient().getId() != sellOrder.getClient().getId()
                    && eachBuyOrder.getCurrencyPair().equals(sellOrder.getCurrencyPair())
                    && eachBuyOrder.getPrice().compareTo(sellOrder.getPrice()) >= 0) {


                BigDecimal transactionPrice = sellOrder.getPrice();
                List<BigDecimal> transactionsAmountCurrency = processTransaction(eachBuyOrder, sellOrder, transactionPrice);
                BigDecimal transactionAmountBuyCurrency = transactionsAmountCurrency.get(1);
                BigDecimal transactionAmountSellCurrency = transactionsAmountCurrency.get(0);


                // Обновляем остатки в ордерах
                BigDecimal remainingAmountSellCurrency = sellOrder.getAmountFirst().subtract(transactionAmountBuyCurrency);
                BigDecimal remainingAmountBuyCurrency = sellOrder.getAmountSecond().subtract(transactionAmountSellCurrency);

                BigDecimal remainingAmountBuyOrderSellCurrency = eachBuyOrder.getAmountFirst().subtract(transactionAmountBuyCurrency);
                BigDecimal remainingAmountBuyOrderBuyCurrency = eachBuyOrder.getAmountSecond().subtract(transactionAmountSellCurrency);

                // Обновление ордера на продажу, если остаток больше нуля
                if (remainingAmountSellCurrency.compareTo(BigDecimal.ZERO) == 0) {
//                    System.out.println("Продавец продал все, заявка удалена.");
                    sellOrder.notifyStatus("Seller " + sellOrder.getClient().getId() + " all sold. Order "
                            + sellOrder.getId() + " closed.");
//                    resultCallback.accept("Продавец продал все, заявка удалена.");
                } else {
                    sellOrder.setAmountFirst(remainingAmountSellCurrency);
                    sellOrder.setAmountSecond(remainingAmountBuyCurrency);
                    sellOrder.notifyStatus("Seller " + sellOrder.getClient().getId() + " sold some part. Order "
                            + sellOrder.getId() + " still open.");
                    orderQueue.add(sellOrder);
                }

                // Обновление ордера на покупку, если остаток больше нуля
                if (remainingAmountBuyOrderSellCurrency.compareTo(BigDecimal.ZERO) == 0) {
                    iterator.remove();
                    orderQueue.remove(eachBuyOrder);
//                    buyOrdersQueue.remove(eachBuyOrder);
//                    System.out.println("Покупатель купил все, заявка удалена.");
                    eachBuyOrder.notifyStatus("Buyer " + eachBuyOrder.getClient().getId() + " all bought. Order "
                            + eachBuyOrder.getId() + " closed.");
//                    resultCallback.accept("Покупатель купил все, заявка удалена.");
                } else {
                    eachBuyOrder.setAmountFirst(remainingAmountBuyOrderSellCurrency);
                    eachBuyOrder.setAmountSecond(remainingAmountBuyOrderBuyCurrency);
                    eachBuyOrder.notifyStatus("Buyer " + eachBuyOrder.getClient().getId() + " bought some part. Order "
                            + eachBuyOrder.getId() + " still open.");

                }
            }
        }

    }


    private List<BigDecimal> processTransaction(Order buyOrder, Order sellOrder, BigDecimal transactionPrice) {
        Client buyer = buyOrder.getClient();
        Client seller = sellOrder.getClient();

        labs.rsreu.currencies.Currency buyCurrency = buyOrder.getCurrencyPair().getCurrencyFirst();
        Currency sellCurrency = buyOrder.getCurrencyPair().getCurrencySecond();

        BigDecimal buyAmountBuyer = buyOrder.getAmountFirst();
        BigDecimal sellAmountSeller = sellOrder.getAmountFirst();
        BigDecimal buyAmountOrderTmp = buyAmountBuyer.min(sellAmountSeller);
        BigDecimal priceBuyAmountOrderTmp = buyAmountOrderTmp.multiply(transactionPrice);

        BigDecimal buyerOldBalanceSellCurrency = buyer.getBalance(sellCurrency);
        BigDecimal sellerOldBalanceBuyCurrency = seller.getBalance(buyCurrency);
        BigDecimal buyerOldBalanceBuyCurrency = buyer.getBalance(buyCurrency);
        BigDecimal sellerOldBalanceSellCurrency = seller.getBalance(sellCurrency);

        // Проверка баланса покупателя
        BigDecimal transactionAmountSellCurrency;
        BigDecimal transactionAmountBuyCurrency;
        if (buyer.getBalance(sellCurrency).compareTo(priceBuyAmountOrderTmp) >= 0) {
            transactionAmountSellCurrency = priceBuyAmountOrderTmp;
            transactionAmountBuyCurrency = buyAmountOrderTmp;
        } else {
            transactionAmountSellCurrency = buyer.getBalance(sellCurrency);
            transactionAmountBuyCurrency = transactionAmountSellCurrency.divide(transactionPrice, RoundingMode.DOWN);
        }

        // Обновление балансов
        buyer.updateBalance(sellCurrency, transactionAmountSellCurrency, false);
        buyer.updateBalance(buyCurrency, transactionAmountBuyCurrency, true);

        seller.updateBalance(sellCurrency, transactionAmountSellCurrency, true);
        seller.updateBalance(buyCurrency, transactionAmountBuyCurrency, false);

        // Логирование сделки
        String transactionLog = "|----------TRANSACTION COMPLETE----------|\n" +
                "ID ордера (покупка): " + buyOrder.getId() +
                ", Покупатель ID: " + buyer.getId() + "\n" +
                "ID ордера (продажа): " + sellOrder.getId() +
                ", Продавец ID: " + seller.getId() + "\n" +
                "Валютная пара: " + buyOrder.getCurrencyPair() + "\n" +
                "Сумма транзакции " + buyCurrency + ": " + transactionAmountBuyCurrency + "\n" +
                "Сумма транзакции " + sellCurrency + ": " + transactionAmountSellCurrency + "\n" +
                "Баланс покупателя до сделки: " + buyerOldBalanceSellCurrency + " " +
                sellCurrency + ", после сделки: " + buyer.getBalance(sellCurrency) + "\n" +
                "Баланс покупателя до сделки: " + buyerOldBalanceBuyCurrency + " " +
                buyCurrency + ", после сделки: " + buyer.getBalance(buyCurrency) + "\n" +
                "Баланс продавца до сделки: " + sellerOldBalanceBuyCurrency + " " +
                buyCurrency + ", после сделки: " + seller.getBalance(buyCurrency) + "\n" +
                "Баланс продавца до сделки: " + sellerOldBalanceSellCurrency + " " +
                sellCurrency + ", после сделки: " + seller.getBalance(sellCurrency) + "\n" +
                "|----------------------------------------|";
        System.out.println(transactionLog);
        return Arrays.asList(transactionAmountSellCurrency, transactionAmountBuyCurrency);

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
            order.notifyStatus("Order canceled: Exchange is closed.");
        }


        orderQueue.clear();
    }
}
