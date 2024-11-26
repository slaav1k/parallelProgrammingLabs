package ExchangeTests;

import labs.rsreu.clients.Client;
import labs.rsreu.clients.ClientsList;
import labs.rsreu.currencies.Currency;
import labs.rsreu.currencies.CurrencyPair;
import labs.rsreu.currencies.CurrencyPairRegistry;
import labs.rsreu.exchanges.Exchange;
import labs.rsreu.orders.Order;
import labs.rsreu.orders.OrderType;
import labs.rsreu.orders.TransactionInfo;
import labs.rsreu.orders.TransactionInfoHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;


import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.junit.jupiter.api.Assertions.*;

public class ExchangeTest {

    private Exchange exchange;
    private CurrencyPairRegistry currencyPairRegistry;
    private ClientsList clientsList;
    List<Object> open0rders;


    @BeforeEach
    public void setUp() {
        this.clientsList = new ClientsList();
        // Настройка Exchange с фиксированными валютными парами
        currencyPairRegistry = new CurrencyPairRegistry();

        List<CurrencyPair> fixedCurrencyPairs = List.of(
                new CurrencyPair(Currency.RUB, Currency.USD),
                new CurrencyPair(Currency.USD, Currency.EUR),
                new CurrencyPair(Currency.EUR, Currency.CNY),
                new CurrencyPair(Currency.CNY, Currency.RUB),
                new CurrencyPair(Currency.RUB, Currency.CNY),
                new CurrencyPair(Currency.GBR, Currency.CNY)
        );

        open0rders = new ArrayList<>();

        for (CurrencyPair pair : fixedCurrencyPairs) {
            if (!currencyPairRegistry.isValidCurrencyPair(pair)) {
                currencyPairRegistry.addCurrencyPair(pair);
            }
        }

        // Инициализация биржи
        exchange = new Exchange(currencyPairRegistry);
    }

    @RepeatedTest(10)
    public void testCreateClientRepeated() {
        // Тест повторяющий создание клиента
        Client client = clientsList.createClient(new EnumMap<>(Currency.class) {{
            put(Currency.RUB, new BigDecimal("1000"));
            put(Currency.CNY, new BigDecimal("500"));
        }});

        // Проверка баланса клиента
        assertEquals(new BigDecimal("1000"), client.getBalance(Currency.RUB));
        assertEquals(new BigDecimal("500"), client.getBalance(Currency.CNY));
    }


    @Test
    public void testCreateClientWithInvalidOrder() {
        Client client = clientsList.createClient(new EnumMap<>(Currency.class) {{
            put(Currency.RUB, new BigDecimal("1000"));
            put(Currency.CNY, new BigDecimal("500"));
        }});

        ConcurrentLinkedQueue<TransactionInfo> callbackQueue = new ConcurrentLinkedQueue<>();
        // Попытка создать ордер с неправильной валютной парой
        exchange.createOrder(new Order(OrderType.BUY, client.getId(), new CurrencyPair(Currency.RUB, Currency.GBR),
                new BigDecimal("100"), new BigDecimal("1.25")), callbackQueue::add);

        TransactionInfo info = callbackQueue.poll();
        assertTrue(info.isHasError());
        assertEquals("Invalid currency pair", info.getErrorMessage());
    }


    @Test
    public void testBalanceUpdateAfterOrderExecution() {
        // Создаем клиента с начальным балансом
        Client client = clientsList.createClient(new EnumMap<>(Currency.class) {{
            put(Currency.RUB, new BigDecimal("1000"));
            put(Currency.CNY, new BigDecimal("500"));
        }});

        Client client2 = clientsList.createClient(new EnumMap<>(Currency.class) {{
            put(Currency.RUB, new BigDecimal("500"));
            put(Currency.CNY, new BigDecimal("1000"));
        }});

        ConcurrentLinkedQueue<TransactionInfo> callbackQueue = new ConcurrentLinkedQueue<>();

        // Создаем ордер на покупку
        Order buyOrder = new Order(OrderType.BUY, client.getId(), new CurrencyPair(Currency.RUB, Currency.CNY), new BigDecimal("100"), new BigDecimal("300"));
        System.out.println(buyOrder);
        exchange.createOrder(buyOrder, status -> {
            if (status.isHasError()) System.out.println(status.getErrorMessage());
            else callbackQueue.add(status);
        });

        // Создаем ордер на покупку
        Order sellOrder = new Order(OrderType.SELL, client2.getId(), new CurrencyPair(Currency.RUB, Currency.CNY), new BigDecimal("100"), new BigDecimal("200"));
        System.out.println(sellOrder);
        exchange.createOrder(sellOrder, status -> {
            if (status.isHasError()) System.out.println(status.getErrorMessage());
            else callbackQueue.add(status);
        });

        // Проверяем, что ордер был выполнен (если есть подходящий ордер для сделки)
        List<Order> openOrders = exchange.getOpenOrders();
        System.out.println(openOrders);

        TransactionInfoHandler transactionInfoHandler = new TransactionInfoHandler(clientsList, callbackQueue);
        transactionInfoHandler.processTransactions();

        // Используем compareTo для сравнения значений без учета точности
        assertEquals(0, new BigDecimal("1100").compareTo(client.getBalance(Currency.RUB)));
        assertEquals(0, new BigDecimal("200").compareTo(client.getBalance(Currency.CNY)));

    }

    @Test
    public void testNegativeBalanceUpdateAfterOrderExecution() {
        // Создаем клиента с начальным балансом
        Client client = clientsList.createClient(new EnumMap<>(Currency.class) {{
            put(Currency.RUB, new BigDecimal("1000"));
            put(Currency.CNY, new BigDecimal("100"));
        }});

        Client client2 = clientsList.createClient(new EnumMap<>(Currency.class) {{
            put(Currency.RUB, new BigDecimal("500"));
            put(Currency.CNY, new BigDecimal("1000"));
        }});

        ConcurrentLinkedQueue<TransactionInfo> callbackQueue = new ConcurrentLinkedQueue<>();

        // Создаем ордер на покупку
        Order buyOrder = new Order(OrderType.BUY, client.getId(), new CurrencyPair(Currency.RUB, Currency.CNY), new BigDecimal("100"), new BigDecimal("300"));
        System.out.println(buyOrder);
        exchange.createOrder(buyOrder, status -> {
            if (status.isHasError()) System.out.println(status.getErrorMessage());
            else callbackQueue.add(status);
        });

        // Создаем ордер на покупку
        Order sellOrder = new Order(OrderType.SELL, client2.getId(), new CurrencyPair(Currency.RUB, Currency.CNY), new BigDecimal("100"), new BigDecimal("200"));
        System.out.println(sellOrder);
        exchange.createOrder(sellOrder, status -> {
            if (status.isHasError()) System.out.println(status.getErrorMessage());
            else callbackQueue.add(status);
        });

        // Проверяем, что ордер был выполнен (если есть подходящий ордер для сделки)
        List<Order> openOrders = exchange.getOpenOrders();
        System.out.println(openOrders);

        TransactionInfoHandler transactionInfoHandler = new TransactionInfoHandler(clientsList, callbackQueue);
        transactionInfoHandler.processTransactions();


        assertEquals(0, new BigDecimal("1100").compareTo(client.getBalance(Currency.RUB)));
        assertEquals(0, new BigDecimal("-200").compareTo(client.getBalance(Currency.CNY)));

    }

    @Test
    public void testPartOrderExecution() {
        // Создаем клиента с начальным балансом
        Client client = clientsList.createClient(new EnumMap<>(Currency.class) {{
            put(Currency.RUB, new BigDecimal("1000"));
            put(Currency.CNY, new BigDecimal("500"));
        }});

        Client client2 = clientsList.createClient(new EnumMap<>(Currency.class) {{
            put(Currency.RUB, new BigDecimal("500"));
            put(Currency.CNY, new BigDecimal("1000"));
        }});

        ConcurrentLinkedQueue<TransactionInfo> callbackQueue = new ConcurrentLinkedQueue<>();

        // Создаем ордер на покупку
        Order buyOrder = new Order(OrderType.BUY, client.getId(), new CurrencyPair(Currency.RUB, Currency.CNY), new BigDecimal("100"), new BigDecimal("300"));
        System.out.println(buyOrder);
        exchange.createOrder(buyOrder, status -> {
            if (status.isHasError()) System.out.println(status.getErrorMessage());
            else callbackQueue.add(status);
        });

        // Создаем ордер на продажу
        Order sellOrder = new Order(OrderType.SELL, client2.getId(), new CurrencyPair(Currency.RUB, Currency.CNY), new BigDecimal("50"), new BigDecimal("100"));
        System.out.println(sellOrder);
        exchange.createOrder(sellOrder, status -> {
            if (status.isHasError()) System.out.println(status.getErrorMessage());
            else callbackQueue.add(status);
        });

        // Проверяем, что ордер был выполнен (если есть подходящий ордер для сделки)
        List<Order> openOrders = exchange.getOpenOrders();
        System.out.println(openOrders);

        assertEquals(1, openOrders.size());

        Order sellOrder2 = new Order(OrderType.SELL, client2.getId(), new CurrencyPair(Currency.RUB, Currency.CNY), new BigDecimal("50"), new BigDecimal("100"));
        System.out.println(sellOrder2);
        exchange.createOrder(sellOrder2, status -> {
            if (status.isHasError()) System.out.println(status.getErrorMessage());
            else callbackQueue.add(status);
        });
        openOrders = exchange.getOpenOrders();
        System.out.println(openOrders);
        assertEquals(0, openOrders.size());

        TransactionInfoHandler transactionInfoHandler = new TransactionInfoHandler(clientsList, callbackQueue);
        transactionInfoHandler.processTransactions();

        assertEquals(0, new BigDecimal("1100").compareTo(client.getBalance(Currency.RUB)));
        assertEquals(0, new BigDecimal("200").compareTo(client.getBalance(Currency.CNY)));
    }

    @Test
    public void testAddOrderOnClosedExchange() {
        // Закрываем биржу
        exchange.closeExchange();

        Client client = clientsList.createClient(new EnumMap<>(Currency.class) {{
            put(Currency.RUB, new BigDecimal("500"));
            put(Currency.CNY, new BigDecimal("1000"));
        }});

        ConcurrentLinkedQueue<TransactionInfo> callbackQueue = new ConcurrentLinkedQueue<>();
        // Попытка создать ордер с неправильной валютной парой
        exchange.createOrder(new Order(OrderType.BUY, client.getId(), new CurrencyPair(Currency.RUB, Currency.GBR),
                new BigDecimal("100"), new BigDecimal("1.25")), callbackQueue::add);

        TransactionInfo info = callbackQueue.poll();
        assertTrue(info.isHasError());
        assertEquals("Order cannot be created: Exchange is closed.", info.getErrorMessage());
    }


    @Test
    public void testOrderClosesWhenExchangeCloses() throws InterruptedException {
        // Создаем клиента с балансом
        Client client = clientsList.createClient(new EnumMap<>(Currency.class) {{
            put(Currency.RUB, new BigDecimal("500"));
            put(Currency.CNY, new BigDecimal("1000"));
        }});

        // Создаем ордер на открытую биржу
        Order order = new Order(OrderType.SELL, client.getId(), new CurrencyPair(Currency.RUB, Currency.CNY), new BigDecimal("50"), new BigDecimal("100"));

        String statusOrder = "";

        Thread.sleep(100);



        // Создаем ордер и проверяем сообщение о добавлении
        exchange.createOrder(order, status -> {
            assertEquals("Order " + order.getId() + ": клиента " + client.getId() + " успешно добавлен.", status.getMessage());
        });

        // Закрываем биржу
        exchange.closeExchange();


        order.addStatusCallback(status -> {
            assertEquals("Order canceled: Exchange is closed.", status.getErrorMessage());
        });

    }




}
