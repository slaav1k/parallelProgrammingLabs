package labs.rsreu.exchanges;

import labs.rsreu.clients.Client;
import labs.rsreu.currencies.Currency;
import labs.rsreu.currencies.CurrencyPairRegistry;
import labs.rsreu.orders.Order;
import labs.rsreu.orders.OrderType;
import labs.rsreu.orders.TransactionInfo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

public class Exchange implements IExchange {
    private boolean isExchangeClosed = false;
    private final CurrencyPairRegistry currencyPairRegistry; // Валютные пары
    private final List<Order> buyOrders = new ArrayList<>(); // Список ордеров на покупку
    private final List<Order> sellOrders = new ArrayList<>(); // Список ордеров на продажу
    private final Lock lock = new ReentrantLock(); // Мьютекс для синхронизации

    public Exchange(CurrencyPairRegistry currencyPairRegistry) {
        this.currencyPairRegistry = currencyPairRegistry;
    }


    @Override
    public void createOrder(Order inputOrder, Consumer<TransactionInfo> resultCallback) {
        lock.lock();

        if (isExchangeClosed) {
            resultCallback.accept(new TransactionInfo(true, "Order cannot be created: Exchange is closed."));
            return;
        }

        try {

            // Проверка на валидность валютной пары
            if (!currencyPairRegistry.isValidCurrencyPair(inputOrder.getCurrencyPair())) {
                resultCallback.accept(new TransactionInfo(true, "Invalid currency pair"));
                return;
            }

            Order order = new Order(inputOrder);

            // Добавление ордера в соответствующий список (покупка или продажа)
            if (order.getType() == OrderType.BUY) {
                addBuyOrder(order, resultCallback);
            } else {
                addSellOrder(order, resultCallback);
            }

            // Уведомляем о завершении обработки ордера
            resultCallback.accept(new TransactionInfo(1,"Order " + order.getId() + ": клиента " + order.getClientId() + " успешно добавлен."));

        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<Order> getOpenOrders() {
        lock.lock();
        try {
            // Возвращаем все открытые ордера
            List<Order> allOrders = new ArrayList<>();
            allOrders.addAll(buyOrders);
            allOrders.addAll(sellOrders);
            return allOrders;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void closeExchange() {
        isExchangeClosed = true;
        // Отменяем все ордера
        for (Order order : buyOrders) {
            order.notifyStatus(new TransactionInfo(true, "Order canceled: Exchange is closed."));
        }
        for (Order order : sellOrders) {
            order.notifyStatus(new TransactionInfo(true, "Order canceled: Exchange is closed."));
        }
        buyOrders.clear();
        sellOrders.clear();
    }


    // Добавление ордера на покупку
    private void addBuyOrder(Order buyOrder, Consumer<TransactionInfo> resultCallback) {
        boolean isMatched = false;
        List<Order> updatedBuyOrders = new ArrayList<>();
        List<Order> updatedSellOrders = new ArrayList<>();

        for (Order eachSellOrder : sellOrders) {
            if (eachSellOrder.getClientId() != buyOrder.getClientId()
                    && eachSellOrder.getCurrencyPair().equals(buyOrder.getCurrencyPair())
                    && eachSellOrder.getPrice().compareTo(buyOrder.getPrice()) <= 0) {

                isMatched = true;

                BigDecimal transactionPrice = eachSellOrder.getPrice();
                List<BigDecimal> transactionsAmountCurrency = processTransaction(buyOrder, eachSellOrder, transactionPrice);

                BigDecimal transactionAmountBuyCurrency = transactionsAmountCurrency.get(1);
                BigDecimal transactionAmountSellCurrency = transactionsAmountCurrency.get(0);

                resultCallback.accept(new TransactionInfo(
                        new Order(OrderType.BUY, buyOrder.getClientId(), buyOrder.getCurrencyPair(), transactionAmountBuyCurrency, transactionAmountSellCurrency),
                        new Order(OrderType.SELL, eachSellOrder.getClientId(), eachSellOrder.getCurrencyPair(), transactionAmountBuyCurrency, transactionAmountSellCurrency)
                ));

                // Обновляем остатки в ордерах, но не устанавливаем значения равные нулю
                BigDecimal remainingAmountBuyCurrency = buyOrder.getAmountFirst().subtract(transactionAmountBuyCurrency);
                BigDecimal remainingAmountSellCurrency = buyOrder.getAmountSecond().subtract(transactionAmountSellCurrency);

                BigDecimal remainingAmountSellOrderBuyCurrency = eachSellOrder.getAmountFirst().subtract(transactionAmountBuyCurrency);
                BigDecimal remainingAmountSellOrderSellCurrency = eachSellOrder.getAmountSecond().subtract(transactionAmountSellCurrency);

                if (remainingAmountBuyCurrency.compareTo(BigDecimal.ZERO) > 0) {
                    buyOrder.setAmountFirst(remainingAmountBuyCurrency);
                    buyOrder.setAmountSecond(remainingAmountSellCurrency);
                    updatedBuyOrders.add(buyOrder);
                }

                if (remainingAmountSellOrderBuyCurrency.compareTo(BigDecimal.ZERO) > 0) {
                    eachSellOrder.setAmountFirst(remainingAmountSellOrderBuyCurrency);
                    eachSellOrder.setAmountSecond(remainingAmountSellOrderSellCurrency);
                    updatedSellOrders.add(eachSellOrder);
                }
            }
        }

        // Обновляем списки после завершения итерации
        sellOrders.removeIf(order -> !updatedSellOrders.contains(order));
        if (!isMatched) {
            buyOrders.add(buyOrder);
        } else {
            buyOrders.removeIf(order -> !updatedBuyOrders.contains(order));
            buyOrders.addAll(updatedBuyOrders);
        }
        buyOrders.sort(Comparator.comparing(Order::getPrice));
    }


    private void addSellOrder(Order sellOrder, Consumer<TransactionInfo> resultCallback) {
        boolean isMatched = false;
        List<Order> updatedSellOrders = new ArrayList<>();
        List<Order> updatedBuyOrders = new ArrayList<>();

        for (Order eachBuyOrder : buyOrders) {
            if (eachBuyOrder.getClientId() != sellOrder.getClientId()
                    && eachBuyOrder.getCurrencyPair().equals(sellOrder.getCurrencyPair())
                    && eachBuyOrder.getPrice().compareTo(sellOrder.getPrice()) >= 0) {

                isMatched = true;

                BigDecimal transactionPrice = eachBuyOrder.getPrice();
                List<BigDecimal> transactionsAmountCurrency = processTransaction(eachBuyOrder, sellOrder, transactionPrice);
                BigDecimal transactionAmountBuyCurrency = transactionsAmountCurrency.get(1);
                BigDecimal transactionAmountSellCurrency = transactionsAmountCurrency.get(0);

                resultCallback.accept(new TransactionInfo(
                        new Order(OrderType.BUY, eachBuyOrder.getClientId(), eachBuyOrder.getCurrencyPair(), transactionAmountBuyCurrency, transactionAmountSellCurrency),
                        new Order(OrderType.SELL, sellOrder.getClientId(), sellOrder.getCurrencyPair(), transactionAmountBuyCurrency, transactionAmountSellCurrency)
                ));

                // Обновляем остатки в ордерах
                BigDecimal remainingAmountSellCurrency = sellOrder.getAmountFirst().subtract(transactionAmountBuyCurrency);
                BigDecimal remainingAmountBuyCurrency = sellOrder.getAmountSecond().subtract(transactionAmountSellCurrency);

                BigDecimal remainingAmountBuyOrderSellCurrency = eachBuyOrder.getAmountFirst().subtract(transactionAmountBuyCurrency);
                BigDecimal remainingAmountBuyOrderBuyCurrency = eachBuyOrder.getAmountSecond().subtract(transactionAmountSellCurrency);

                if (remainingAmountSellCurrency.compareTo(BigDecimal.ZERO) > 0) {
                    sellOrder.setAmountSecond(remainingAmountSellCurrency);
                    sellOrder.setAmountFirst(remainingAmountBuyCurrency);
                    updatedSellOrders.add(sellOrder);
                }

                if (remainingAmountBuyOrderBuyCurrency.compareTo(BigDecimal.ZERO) > 0) {
                    eachBuyOrder.setAmountFirst(remainingAmountBuyOrderSellCurrency);
                    eachBuyOrder.setAmountSecond(remainingAmountBuyOrderBuyCurrency);
                    updatedBuyOrders.add(eachBuyOrder);
                }
            }
        }

        // Обновляем списки после завершения итерации
        buyOrders.removeIf(order -> !updatedBuyOrders.contains(order));
        if (!isMatched) {
            sellOrders.add(sellOrder);
        } else {
            sellOrders.removeIf(order -> !updatedSellOrders.contains(order));
            sellOrders.addAll(updatedSellOrders);
        }
        sellOrders.sort(Comparator.comparing(Order::getPrice).reversed());
    }



    private List<BigDecimal> processTransaction(Order buyOrder, Order sellOrder, BigDecimal transactionPrice) {
        int buyer = buyOrder.getClientId();
        int seller = sellOrder.getClientId();

        Currency buyCurrency = buyOrder.getCurrencyPair().getCurrencyFirst();
        Currency sellCurrency = buyOrder.getCurrencyPair().getCurrencySecond();

        BigDecimal buyAmountBuyer = buyOrder.getAmountFirst();
        BigDecimal sellAmountSeller = sellOrder.getAmountFirst();
        BigDecimal buyAmountOrderTmp = buyAmountBuyer.min(sellAmountSeller); // как меняется первая валюта
        BigDecimal priceBuyAmountOrderTmp = buyAmountOrderTmp.multiply(transactionPrice); // как меняется вторая валюта

        return Arrays.asList(priceBuyAmountOrderTmp, buyAmountOrderTmp);

    }



}

