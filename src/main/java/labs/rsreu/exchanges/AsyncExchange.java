package labs.rsreu.exchanges;

import labs.rsreu.clients.Client;
import labs.rsreu.currencies.Currency;
import labs.rsreu.currencies.CurrencyPairRegistry;
import labs.rsreu.orders.Order;
import labs.rsreu.orders.TransactionInfo;


import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;


public class AsyncExchange implements IExchange{
    private final BlockingQueue<Order> orderQueue;
    private final CurrencyPairRegistry currencyPairRegistry;
    private final AsyncExchangeOrderHandler orderHandler;
    private final Lock lock = new ReentrantLock();
    private boolean isExchangeClosed = false;

    public AsyncExchange(/*BlockingQueue<Order> orderQueue, */CurrencyPairRegistry currencyPairRegistry) {
        this.currencyPairRegistry = currencyPairRegistry;
//        this.orderQueue = orderQueue;
        this.orderQueue = new LinkedBlockingQueue<>();
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        orderHandler = new AsyncExchangeOrderHandler(orderQueue);
        executorService.submit(orderHandler);
    }


    @Override
    public void createOrder(Order order, Consumer<TransactionInfo> resultCallback) {
        if (isExchangeClosed) {
            resultCallback.accept(new TransactionInfo(true, "Order cannot be created: Exchange is closed."));
            return;
        }

        if (!currencyPairRegistry.isValidCurrencyPair(order.getCurrencyPair())) {
            resultCallback.accept(new TransactionInfo(true, "Order " + order.getId() + " has invalid currency pair"));
            return;
        }
        order.addStatusCallback(resultCallback);
        resultCallback.accept(new TransactionInfo(1,"Order " + order.getId() + " successfully submitted."));
        try {
            orderQueue.put(new Order(order));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            resultCallback.accept(new TransactionInfo(true, "Order submission failed: " + e.getMessage()));
        }

    }

    @Override
    public List<Order> getOpenOrders() {
        lock.lock();
        try {
            return new ArrayList<>(orderQueue);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void closeExchange() {
        isExchangeClosed = true;
        orderHandler.closeExchange();
    }

}
