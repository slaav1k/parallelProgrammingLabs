package labs.rsreu.exchanges;

import labs.rsreu.clients.Client;
import labs.rsreu.currencies.Currency;
import labs.rsreu.currencies.CurrencyPairRegistry;
import labs.rsreu.orders.Order;


import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;


public class AsyncExchange implements IExchange{
    private final Map<Integer, Client> clients = new HashMap<>();
    private final BlockingQueue<Order> orderQueue;
    private final CurrencyPairRegistry currencyPairRegistry;
    private final Lock lock = new ReentrantLock();

    public AsyncExchange(BlockingQueue<Order> orderQueue, CurrencyPairRegistry currencyPairRegistry) {
        this.currencyPairRegistry = currencyPairRegistry;
        this.orderQueue = orderQueue;
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        AsyncExchangeOrderHandler orderHandler = new AsyncExchangeOrderHandler(orderQueue);
        executorService.submit(orderHandler);
    }


    @Override
    public void createOrder(Order order, Consumer<String> resultCallback) {
        CompletableFuture.runAsync(() -> {
            try {
                // Проверка на валидность валютной пары
                if (!currencyPairRegistry.isValidCurrencyPair(order.getCurrencyPair())) {
                    resultCallback.accept("Order " + order.getId() + " has invalid currency pair");
                    return;
                }

                orderQueue.put(new Order(order));
                resultCallback.accept("Order " + order.getId() + " successfully submitted.");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                resultCallback.accept("Order submission failed: " + e.getMessage());
            }
        });
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

}
