package labs.rsreu.exchanges;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import labs.rsreu.currencies.Currency;
import labs.rsreu.orders.Order;
import labs.rsreu.orders.OrderType;
import labs.rsreu.orders.TransactionInfo;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Consumer;

public class DisruptorExchangeOrderHandler implements EventHandler<OrderEvent> {
    private final RingBuffer<ResponseEvent> responseBuffer;
    private final PriorityQueue<Order> sellOrdersQueue;
    private final PriorityQueue<Order> buyOrdersQueue;
    private boolean isHandlerClosed;

    public DisruptorExchangeOrderHandler(RingBuffer<ResponseEvent> responseBuffer) {
        this.responseBuffer = responseBuffer;
        this.sellOrdersQueue = new PriorityQueue<>(Comparator.comparing(Order::getPrice).reversed());
        this.buyOrdersQueue = new PriorityQueue<>(Comparator.comparing(Order::getPrice));
    }

    @Override
    public void onEvent(OrderEvent event, long sequence, boolean endOfBatch) {
        if (!isHandlerClosed) {
            processOrder(event.getOrder());
        }
    }


    private void processOrder(Order order) {
        if (order.getType() == OrderType.BUY) {
            buyOrdersQueue.offer(order);
            processBuyOrder(order);
        } else {
            sellOrdersQueue.offer(order);
            processSellOrder(order);
        }
    }

    private void processBuyOrder(Order buyOrder) {
//        List<Order> sellOrders = getSellOrders();
//        Iterator<Order> iterator = sellOrders.iterator();
        Iterator<Order> iterator = sellOrdersQueue.iterator();
        while (iterator.hasNext()) {
            Order eachSellOrder = iterator.next();
            if (eachSellOrder.getClientId() != buyOrder.getClientId()
                    && eachSellOrder.getCurrencyPair().equals(buyOrder.getCurrencyPair())
                    && eachSellOrder.getPrice().compareTo(buyOrder.getPrice()) <= 0) {
                BigDecimal transactionPrice = eachSellOrder.getPrice();
//                BigDecimal transactionPrice = buyOrder.getPrice();
                List<BigDecimal> transactionsAmountCurrency = processTransaction(buyOrder, eachSellOrder, transactionPrice);

                BigDecimal transactionAmountBuyCurrency = transactionsAmountCurrency.get(1);
                BigDecimal transactionAmountSellCurrency = transactionsAmountCurrency.get(0);

//                buyOrder.notifyStatus(new TransactionInfo(
//                        new Order(OrderType.BUY, buyOrder.getClientId(), buyOrder.getCurrencyPair(), transactionAmountBuyCurrency, transactionAmountSellCurrency),
//                        new Order(OrderType.SELL, eachSellOrder.getClientId(), eachSellOrder.getCurrencyPair(), transactionAmountBuyCurrency, transactionAmountSellCurrency)
//                ));

                ResponseEvent responseEvent = new ResponseEvent();
                responseEvent.setTransactionInfo(new TransactionInfo(
                        new Order(OrderType.BUY, buyOrder.getClientId(), buyOrder.getCurrencyPair(), transactionAmountBuyCurrency, transactionAmountSellCurrency),
                        new Order(OrderType.SELL, eachSellOrder.getClientId(), eachSellOrder.getCurrencyPair(), transactionAmountBuyCurrency, transactionAmountSellCurrency)
                ));
                responseBuffer.publishEvent((event, sequence) -> event.setTransactionInfo(responseEvent.getTransactionInfo()));

                // Обновляем остатки в ордерах, но не устанавливаем значения равные нулю
                BigDecimal remainingAmountBuyCurrency = buyOrder.getAmountFirst().subtract(transactionAmountBuyCurrency);
                BigDecimal remainingAmountSellCurrency = buyOrder.getAmountSecond().subtract(transactionAmountSellCurrency);

                BigDecimal remainingAmountSellOrderBuyCurrency = eachSellOrder.getAmountFirst().subtract(transactionAmountBuyCurrency);
                BigDecimal remainingAmountSellOrderSellCurrency = eachSellOrder.getAmountSecond().subtract(transactionAmountSellCurrency);

                // Обновление ордера на покупку, если остаток больше нуля
                if (remainingAmountBuyCurrency.compareTo(BigDecimal.ZERO) == 0) {
//                    buyOrder.notifyStatus(new TransactionInfo("Buyer " + buyOrder.getClientId() + " all bought. Order "
//                            + buyOrder.getId() + " closed."));
//                    ResponseEvent responseEvent = new ResponseEvent();
                    responseEvent.setTransactionInfo(new TransactionInfo("Buyer " + buyOrder.getClientId() + " all bought. Order "
                            + buyOrder.getId() + " closed."));
                    responseBuffer.publishEvent((event, sequence) -> event.setTransactionInfo(responseEvent.getTransactionInfo()));

                } else {
                    buyOrder.setAmountFirst(remainingAmountBuyCurrency);
                    buyOrder.setAmountSecond(remainingAmountSellCurrency);
//                    buyOrder.notifyStatus(new TransactionInfo("Buyer " + buyOrder.getClientId() + " bought some part. Order "
//                            + buyOrder.getId() + " still open."));
                    responseEvent.setTransactionInfo(new TransactionInfo("Buyer " + buyOrder.getClientId() + " bought some part. Order "
                            + buyOrder.getId() + " still open."));
                    responseBuffer.publishEvent((event, sequence) -> event.setTransactionInfo(responseEvent.getTransactionInfo()));

//                    orderBuffer.publishEvent((event, sequence) -> event.setOrder(buyOrder));
                    buyOrdersQueue.offer(buyOrder);
                }

                // Обновление ордера на продажу, если остаток больше нуля
                if (remainingAmountSellOrderBuyCurrency.compareTo(BigDecimal.ZERO) == 0) {
                    iterator.remove();
//                    orderBuffer.publishEvent((event, sequence) -> event.setOrder(buyOrder));
                    sellOrdersQueue.remove(eachSellOrder);
//                    orderQueue.remove(eachSellOrder);
//                    eachSellOrder.notifyStatus(new TransactionInfo("Seller " + eachSellOrder.getClientId() + " all sold. Order "
//                            + eachSellOrder.getId() + " closed."));
                    responseEvent.setTransactionInfo(new TransactionInfo("Seller " + eachSellOrder.getClientId() + " all sold. Order "
                            + eachSellOrder.getId() + " closed."));
                    responseBuffer.publishEvent((event, sequence) -> event.setTransactionInfo(responseEvent.getTransactionInfo()));

                } else {
                    eachSellOrder.setAmountFirst(remainingAmountSellOrderBuyCurrency);
                    eachSellOrder.setAmountSecond(remainingAmountSellOrderSellCurrency);
//                    eachSellOrder.notifyStatus(new TransactionInfo("Seller " + eachSellOrder.getClientId() + " sold some part. Order "
//                            + eachSellOrder.getId() + " still open."));
                    responseEvent.setTransactionInfo(new TransactionInfo("Seller " + eachSellOrder.getClientId() + " sold some part. Order "
                            + eachSellOrder.getId() + " still open."));
                    responseBuffer.publishEvent((event, sequence) -> event.setTransactionInfo(responseEvent.getTransactionInfo()));
                }
            }
        }
    }

    private void processSellOrder(Order sellOrder) {
//        List<Order> buyOrders = getBuyOrders();
//        Iterator<Order> iterator = buyOrders.iterator();
        Iterator<Order> iterator = buyOrdersQueue.iterator();
        while (iterator.hasNext()) {
            Order eachBuyOrder = iterator.next();

            // Убедимся, что ордера принадлежат разным клиентам, что валютные пары совпадают и что цена удовлетворяет условиям
            if (eachBuyOrder.getClientId() != sellOrder.getClientId()
                    && eachBuyOrder.getCurrencyPair().equals(sellOrder.getCurrencyPair())
                    && eachBuyOrder.getPrice().compareTo(sellOrder.getPrice()) >= 0) {

                BigDecimal transactionPrice = eachBuyOrder.getPrice();
//                BigDecimal transactionPrice = sellOrder.getPrice();
                List<BigDecimal> transactionsAmountCurrency = processTransaction(eachBuyOrder, sellOrder, transactionPrice);
                BigDecimal transactionAmountBuyCurrency = transactionsAmountCurrency.get(1);
                BigDecimal transactionAmountSellCurrency = transactionsAmountCurrency.get(0);


//                sellOrder.notifyStatus(new TransactionInfo(
//                        new Order(OrderType.BUY, eachBuyOrder.getClientId(), eachBuyOrder.getCurrencyPair(), transactionAmountBuyCurrency, transactionAmountSellCurrency),
//                        new Order(OrderType.SELL, sellOrder.getClientId(), sellOrder.getCurrencyPair(), transactionAmountBuyCurrency, transactionAmountSellCurrency)
//                ));

                ResponseEvent responseEvent = new ResponseEvent();
                responseEvent.setTransactionInfo(new TransactionInfo(
                        new Order(OrderType.BUY, eachBuyOrder.getClientId(), eachBuyOrder.getCurrencyPair(), transactionAmountBuyCurrency, transactionAmountSellCurrency),
                        new Order(OrderType.SELL, sellOrder.getClientId(), sellOrder.getCurrencyPair(), transactionAmountBuyCurrency, transactionAmountSellCurrency)
                ));
                responseBuffer.publishEvent((event, sequence) -> event.setTransactionInfo(responseEvent.getTransactionInfo()));


                // Обновляем остатки в ордерах
                BigDecimal remainingAmountSellCurrency = sellOrder.getAmountFirst().subtract(transactionAmountBuyCurrency);
                BigDecimal remainingAmountBuyCurrency = sellOrder.getAmountSecond().subtract(transactionAmountSellCurrency);

                BigDecimal remainingAmountBuyOrderSellCurrency = eachBuyOrder.getAmountFirst().subtract(transactionAmountBuyCurrency);
                BigDecimal remainingAmountBuyOrderBuyCurrency = eachBuyOrder.getAmountSecond().subtract(transactionAmountSellCurrency);

                // Обновление ордера на продажу, если остаток больше нуля
                if (remainingAmountSellCurrency.compareTo(BigDecimal.ZERO) == 0) {
//                    sellOrder.notifyStatus(new TransactionInfo("Seller " + sellOrder.getClientId() + " all sold. Order "
//                            + sellOrder.getId() + " closed."));
                    responseEvent.setTransactionInfo(new TransactionInfo("Seller " + sellOrder.getClientId() + " all sold. Order "
                            + sellOrder.getId() + " closed."));
                    responseBuffer.publishEvent((event, sequence) -> event.setTransactionInfo(responseEvent.getTransactionInfo()));
                } else {
                    sellOrder.setAmountFirst(remainingAmountSellCurrency);
                    sellOrder.setAmountSecond(remainingAmountBuyCurrency);
//                    sellOrder.notifyStatus(new TransactionInfo("Seller " + sellOrder.getClientId() + " sold some part. Order "
//                            + sellOrder.getId() + " still open."));
                    responseEvent.setTransactionInfo(new TransactionInfo("Seller " + sellOrder.getClientId() + " sold some part. Order "
                            + sellOrder.getId() + " still open."));
                    responseBuffer.publishEvent((event, sequence) -> event.setTransactionInfo(responseEvent.getTransactionInfo()));
//                    orderQueue.add(sellOrder);
                    sellOrdersQueue.offer(sellOrder);
                }

                // Обновление ордера на покупку, если остаток больше нуля
                if (remainingAmountBuyOrderSellCurrency.compareTo(BigDecimal.ZERO) == 0) {
                    iterator.remove();
                    sellOrdersQueue.remove(eachBuyOrder);
//                    orderQueue.remove(eachBuyOrder);
//                    eachBuyOrder.notifyStatus(new TransactionInfo("Buyer " + eachBuyOrder.getClientId() + " all bought. Order "
//                            + eachBuyOrder.getId() + " closed."));
                    responseEvent.setTransactionInfo(new TransactionInfo("Buyer " + eachBuyOrder.getClientId() + " all bought. Order "
                            + eachBuyOrder.getId() + " closed."));
                    responseBuffer.publishEvent((event, sequence) -> event.setTransactionInfo(responseEvent.getTransactionInfo()));
                } else {
                    eachBuyOrder.setAmountFirst(remainingAmountBuyOrderSellCurrency);
                    eachBuyOrder.setAmountSecond(remainingAmountBuyOrderBuyCurrency);
//                    eachBuyOrder.notifyStatus(new TransactionInfo("Buyer " + eachBuyOrder.getClientId() + " bought some part. Order "
//                            + eachBuyOrder.getId() + " still open."));
                    responseEvent.setTransactionInfo(new TransactionInfo("Buyer " + eachBuyOrder.getClientId() + " bought some part. Order "
                            + eachBuyOrder.getId() + " still open."));
                    responseBuffer.publishEvent((event, sequence) -> event.setTransactionInfo(responseEvent.getTransactionInfo()));

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

    public void closeExchange() {
        isHandlerClosed = true; // Установка флага остановки
        // Оповещение клиентов об отмене всех оставшихся ордеров
        notifyAllOrdersCancelled();
    }

    private void notifyAllOrdersCancelled() {
        for (Order order : sellOrdersQueue) {
            order.notifyStatus(new TransactionInfo(true,"Order canceled: Exchange is closed."));
        }
        for (Order order : buyOrdersQueue) {
            order.notifyStatus(new TransactionInfo(true,"Order canceled: Exchange is closed."));
        }

        sellOrdersQueue.clear();
        buyOrdersQueue.clear();
    }

}
