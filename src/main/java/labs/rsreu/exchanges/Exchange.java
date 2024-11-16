package labs.rsreu.exchanges;

import labs.rsreu.clients.Client;
import labs.rsreu.currencies.Currency;
import labs.rsreu.currencies.CurrencyPairRegistry;
import labs.rsreu.orders.Order;
import labs.rsreu.orders.OrderType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

public class Exchange implements IExchange {
    private final CurrencyPairRegistry currencyPairRegistry; // Валютные пары
    private final List<Order> buyOrders = new ArrayList<>(); // Список ордеров на покупку
    private final List<Order> sellOrders = new ArrayList<>(); // Список ордеров на продажу
    private final Lock lock = new ReentrantLock(); // Мьютекс для синхронизации

    public Exchange(CurrencyPairRegistry currencyPairRegistry) {
        this.currencyPairRegistry = currencyPairRegistry;
    }


    @Override
    public void createOrder(Order inputOrder, Consumer<String> resultCallback) {
        lock.lock();
        try {
//            // Проверка, что клиент - клиент нашей биржи
//            if (!clients.containsKey(inputOrder.getClientId())) {
//                resultCallback.accept("Client with ID " + inputOrder.getClientId() + " does not exist.");
//                return;
//            }

            // Проверка на валидность валютной пары
            if (!currencyPairRegistry.isValidCurrencyPair(inputOrder.getCurrencyPair())) {
                resultCallback.accept("Invalid currency pair");
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
            resultCallback.accept("Order " + order.getId() + ": клиента " + order.getClient().getId() + " успешно добавлен.");

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

    // Добавление ордера на покупку
    private void addBuyOrder(Order buyOrder, Consumer<String> resultCallback) {
        boolean isMatched = false;
        Iterator<Order> iterator = sellOrders.iterator();
        while (iterator.hasNext()) {
            Order eachSellOrder = iterator.next();
            if (eachSellOrder.getClient().getId() != buyOrder.getClient().getId()
                && eachSellOrder.getCurrencyPair().equals(buyOrder.getCurrencyPair())
                && eachSellOrder.getPrice().compareTo(buyOrder.getPrice()) <= 0) {

                isMatched = true;

                BigDecimal transactionPrice = eachSellOrder.getPrice();
                List<BigDecimal> transactionsAmountCurrency = processTransaction(buyOrder, eachSellOrder, transactionPrice);

                BigDecimal transactionAmountBuyCurrency = transactionsAmountCurrency.get(1);
                BigDecimal transactionAmountSellCurrency = transactionsAmountCurrency.get(0);

                // Обновляем остатки в ордерах, но не устанавливаем значения равные нулю
                BigDecimal remainingAmountBuyCurrency = buyOrder.getAmountFirst().subtract(transactionAmountBuyCurrency);
                BigDecimal remainingAmountSellCurrency = buyOrder.getAmountSecond().subtract(transactionAmountSellCurrency);

                BigDecimal remainingAmountSellOrderBuyCurrency = eachSellOrder.getAmountFirst().subtract(transactionAmountBuyCurrency);
                BigDecimal remainingAmountSellOrderSellCurrency = eachSellOrder.getAmountSecond().subtract(transactionAmountSellCurrency);

                if (remainingAmountBuyCurrency.compareTo(BigDecimal.ZERO) == 0 && remainingAmountSellCurrency.compareTo(BigDecimal.ZERO) == 0) {
                    resultCallback.accept("Заявка отменена. У одного из участников не хватило денег.");
                }
                // Обновление ордера на покупку, если остаток больше нуля
                if (remainingAmountBuyCurrency.compareTo(BigDecimal.ZERO) == 0) {
                    resultCallback.accept("Покупатель купил все, заявка удалена.");

                } else {
                    buyOrder.setAmountFirst(remainingAmountBuyCurrency);
                    buyOrder.setAmountSecond(remainingAmountSellCurrency);
                    buyOrders.add(buyOrder);
                    buyOrders.sort(Comparator.comparing(Order::getPrice));
                }

                // Обновление ордера на продажу, если остаток больше нуля
                if (remainingAmountSellOrderBuyCurrency.compareTo(BigDecimal.ZERO) == 0) {
                    iterator.remove();
//                    System.out.println("Продавец продал все, заявка удалена.");
                    resultCallback.accept("Продавец продал все, заявка удалена.");

                } else {
                    eachSellOrder.setAmountFirst(remainingAmountSellOrderBuyCurrency);
                    eachSellOrder.setAmountSecond(remainingAmountSellOrderSellCurrency);
                }
            }

        }

        // Если ордер не исполнен, добавляем в очередь
        if (!isMatched) {
            buyOrders.add(buyOrder);
            buyOrders.sort(Comparator.comparing(Order::getPrice)); // Сортировка по цене
        }
    }

    // Добавление ордера на продажу
    private void addSellOrder(Order sellOrder, Consumer<String> resultCallback) {
        boolean isMatched = false;
        Iterator<Order> iterator = buyOrders.iterator();
        while (iterator.hasNext()) {
            Order eachBuyOrder = iterator.next();

            // Убедимся, что ордера принадлежат разным клиентам, что валютные пары совпадают и что цена удовлетворяет условиям
            if (eachBuyOrder.getClient().getId() != sellOrder.getClient().getId()
                    && eachBuyOrder.getCurrencyPair().equals(sellOrder.getCurrencyPair())
                    && eachBuyOrder.getPrice().compareTo(sellOrder.getPrice()) >= 0) {

                isMatched = true;

                BigDecimal transactionPrice = eachBuyOrder.getPrice();
                List<BigDecimal> transactionsAmountCurrency = processTransaction(eachBuyOrder, sellOrder, transactionPrice);
                BigDecimal transactionAmountBuyCurrency = transactionsAmountCurrency.get(1);
                BigDecimal transactionAmountSellCurrency = transactionsAmountCurrency.get(0);


                // Обновляем остатки в ордерах
                BigDecimal remainingAmountSellCurrency = sellOrder.getAmountFirst().subtract(transactionAmountBuyCurrency);
                BigDecimal remainingAmountBuyCurrency = sellOrder.getAmountSecond().subtract(transactionAmountSellCurrency);

                BigDecimal remainingAmountBuyOrderSellCurrency = eachBuyOrder.getAmountFirst().subtract(transactionAmountBuyCurrency);
                BigDecimal remainingAmountBuyOrderBuyCurrency = eachBuyOrder.getAmountSecond().subtract(transactionAmountSellCurrency);

                // Обновление ордера на продажу, если остаток больше нуля
                if (remainingAmountBuyCurrency.compareTo(BigDecimal.ZERO) == 0 && remainingAmountSellCurrency.compareTo(BigDecimal.ZERO) == 0) {
                    resultCallback.accept("Заявка отменена. У одного из участников не хватило денег.");
                }

                if (remainingAmountSellCurrency.compareTo(BigDecimal.ZERO) == 0) {
//                    System.out.println("Продавец продал все, заявка удалена.");
                    resultCallback.accept("Продавец продал все, заявка удалена.");
                } else {
                    sellOrder.setAmountFirst(remainingAmountSellCurrency);
                    sellOrder.setAmountSecond(remainingAmountBuyCurrency);
                    sellOrders.add(sellOrder);
                    sellOrders.sort(Comparator.comparing(Order::getPrice).reversed());
                }

                // Обновление ордера на покупку, если остаток больше нуля
                if (remainingAmountBuyOrderSellCurrency.compareTo(BigDecimal.ZERO) == 0) {
                    iterator.remove();
                    resultCallback.accept("Покупатель купил все, заявка удалена.");
                } else {
                    eachBuyOrder.setAmountFirst(remainingAmountBuyOrderSellCurrency);
                    eachBuyOrder.setAmountSecond(remainingAmountBuyOrderBuyCurrency);
                }
            }
        }

        // Если ордер не исполнен, добавляем в очередь
        if (!isMatched) {
            sellOrders.add(sellOrder);
            sellOrders.sort(Comparator.comparing(Order::getPrice).reversed()); // Сортировка по цене
        }
    }


    private List<BigDecimal> processTransaction(Order buyOrder, Order sellOrder, BigDecimal transactionPrice) {
        Client buyer = buyOrder.getClient();
        Client seller = sellOrder.getClient();

        Currency buyCurrency = buyOrder.getCurrencyPair().getCurrencyFirst();
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

//        transactionAmountSellCurrency = priceBuyAmountOrderTmp;
//        transactionAmountBuyCurrency = buyAmountOrderTmp;

        // Обновление балансов
        buyer.updateBalance(sellCurrency, transactionAmountSellCurrency, false);
        buyer.updateBalance(buyCurrency, transactionAmountBuyCurrency, true);

        seller.updateBalance(sellCurrency, transactionAmountSellCurrency, true);
        seller.updateBalance(buyCurrency, transactionAmountBuyCurrency, false);

//        try {
//            buyer.updateBalance(sellCurrency, transactionAmountSellCurrency, false);
//            buyer.updateBalance(buyCurrency, transactionAmountBuyCurrency, true);
//
//            seller.updateBalance(sellCurrency, transactionAmountSellCurrency, true);
//            seller.updateBalance(buyCurrency, transactionAmountBuyCurrency, false);
//        } catch (Exception e) {
//            transactionAmountSellCurrency = BigDecimal.ZERO;
//            transactionAmountBuyCurrency = BigDecimal.ZERO;
//            restoreBalance(buyer, sellCurrency, buyerOldBalanceSellCurrency);
//            restoreBalance(buyer, buyCurrency, buyerOldBalanceBuyCurrency);
//            restoreBalance(seller, sellCurrency, sellerOldBalanceSellCurrency);
//            restoreBalance(seller, buyCurrency, sellerOldBalanceBuyCurrency);
//        }


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


    private void restoreBalance(Client client, Currency currency, BigDecimal oldBalance) {
        BigDecimal currentBalance = client.getBalance(currency);
        BigDecimal difference = oldBalance.subtract(currentBalance);

        if (difference.compareTo(BigDecimal.ZERO) > 0) {
            // Если старый баланс больше текущего — нужно прибавить разницу
            client.updateBalance(currency, difference, true);
        } else if (difference.compareTo(BigDecimal.ZERO) < 0) {
            // Если старый баланс меньше текущего — нужно вычесть разницу
            client.updateBalance(currency, difference.abs(), false);
        }
        // Если разница равна нулю, ничего не делаем
    }

}

