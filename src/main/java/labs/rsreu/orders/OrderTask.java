package labs.rsreu.orders;

import labs.rsreu.clients.Client;
import labs.rsreu.exchanges.IExchange;
import labs.rsreu.currencies.Currency;
import labs.rsreu.currencies.CurrencyPair;
import labs.rsreu.currencies.CurrencyPairRegistry;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;


public class OrderTask implements Callable<String> {
    private static final Random random = new Random();

    private final Client client;
    private final IExchange exchange;
    private final CurrencyPairRegistry registry;
    private final int countOrdersMax;

    public OrderTask(Client client, IExchange exchange, CurrencyPairRegistry registry, int countOrdersMax) {
        this.client = client;
        this.exchange = exchange;
        this.registry = registry;
        this.countOrdersMax = countOrdersMax;
    }

    @Override
    public String call() throws Exception {
        try {
//            int orderCount = 1 + random.nextInt(countOrdersMax);
            for (int i = 0; i < countOrdersMax; i++) {
                OrderType orderType = getRandomOrderType();
                CurrencyPair currencyPair = getRandomCurrencyPair(client);
                BigDecimal amountFirst = getAmountFirst(client, currencyPair, orderType);
                BigDecimal amountSecond = getAmountSecond(client, currencyPair, amountFirst, orderType);

                // Создаем ордер
                Order order = new Order(
                        orderType,                      // тип ордера (BUY или SELL)
//                        client.getId(),                 // ID клиента
                        client,
                        currencyPair,                   // валютная пара
                        amountFirst,                    // количество первой валюты
                        amountSecond,                   // количество второй валюты
                        registry
                );

                System.out.println(order);

//                exchange.createOrder(order, result -> {
////                    System.out.println("Order " + order.getId() + " завершен для клиента " + client.getId() + ": " + result);
//                });

                exchange.createOrder(order, status -> {
                    System.out.println("Received status update: " + status);
                });

            }

            return "Заказы для клиента " + client.getId() + " успешно завершены.";

        } catch (Exception e) {
            return "Ошибка у клиента " + client.getId() + ": " + e.getMessage();
        }
    }

    // Генерация случайного типа ордера (покупка или продажа)
    private static OrderType getRandomOrderType() {
        return random.nextBoolean() ? OrderType.BUY : OrderType.SELL;
    }

    // Генерация случайной валютной пары для клиента
    private static CurrencyPair getRandomCurrencyPair(Client client) {
        List<Currency> availableCurrencies = new ArrayList<>(client.getAllBalances().keySet());
        if (availableCurrencies.isEmpty()) return null;

        Currency baseCurrency = availableCurrencies.get(random.nextInt(availableCurrencies.size()));
        Currency quoteCurrency;
        do {
            quoteCurrency = Currency.values()[random.nextInt(Currency.values().length)];
        } while (quoteCurrency == baseCurrency);

        return new CurrencyPair(baseCurrency, quoteCurrency);
    }

    // Получение количества первой валюты для ордера
    private static BigDecimal getAmountFirst(Client client, CurrencyPair currencyPair, OrderType orderType) {
        Currency baseCurrency = currencyPair.getCurrencyFirst();
        BigDecimal availableAmount = client.getBalance(baseCurrency);
        double randomAmount = 1 + random.nextDouble() * availableAmount.doubleValue();
        return BigDecimal.valueOf(randomAmount);
    }

    // Получение количества второй валюты для ордера
    private static BigDecimal getAmountSecond(Client client, CurrencyPair currencyPair, BigDecimal amountFirst, OrderType orderType) {
        Currency quoteCurrency = currencyPair.getCurrencySecond();
        BigDecimal availableAmount = client.getBalance(quoteCurrency);
        double randomAmount = 1 + random.nextDouble() * availableAmount.doubleValue();
        return BigDecimal.valueOf(randomAmount);
    }
}

