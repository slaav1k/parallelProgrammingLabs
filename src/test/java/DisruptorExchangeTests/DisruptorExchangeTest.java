package DisruptorExchangeTests;

import labs.rsreu.clients.Client;
import labs.rsreu.clients.ClientsList;
import labs.rsreu.currencies.Currency;
import labs.rsreu.currencies.CurrencyPair;
import labs.rsreu.currencies.CurrencyPairRegistry;
import labs.rsreu.exchanges.AsyncExchange;
import labs.rsreu.exchanges.DisruptorExchange;
import labs.rsreu.exchanges.IExchange;
import labs.rsreu.orders.Order;
import labs.rsreu.orders.OrderType;
import labs.rsreu.orders.TransactionInfo;
import labs.rsreu.orders.TransactionInfoHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DisruptorExchangeTest {
    private IExchange exchange;
    private ClientsList clientsList;
    private CurrencyPairRegistry currencyPairRegistry;

    @BeforeEach
    public void setUp() {
        this.clientsList = new ClientsList();

        currencyPairRegistry = new CurrencyPairRegistry();

        List<CurrencyPair> fixedCurrencyPairs = List.of(
                new CurrencyPair(Currency.RUB, Currency.USD),
                new CurrencyPair(Currency.USD, Currency.EUR),
                new CurrencyPair(Currency.EUR, Currency.CNY),
                new CurrencyPair(Currency.CNY, Currency.RUB),
                new CurrencyPair(Currency.RUB, Currency.CNY),
                new CurrencyPair(Currency.GBR, Currency.CNY)
        );

        for (CurrencyPair pair : fixedCurrencyPairs) {
            if (!currencyPairRegistry.isValidCurrencyPair(pair)) {
                currencyPairRegistry.addCurrencyPair(pair);
            }
        }


        exchange = new DisruptorExchange(currencyPairRegistry);
    }

    @Test
    public void testCreateClient() {
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

        Order order = new Order(OrderType.BUY, client.getId(), new CurrencyPair(Currency.RUB, Currency.GBR),
                new BigDecimal("100"), new BigDecimal("1.25"));

        ConcurrentLinkedQueue<TransactionInfo> callbackQueue = new ConcurrentLinkedQueue<>();
        // Попытка создать ордер с неправильной валютной парой
        exchange.createOrder(order, callbackQueue::add);

        TransactionInfo info = callbackQueue.poll();
        assertTrue(info.isHasError());
        assertEquals("Order " + order.getId() + " has invalid currency pair.", info.getErrorMessage());
    }

    @Test
    public void testBalanceUpdateAfterOrderExecution() throws InterruptedException {
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

        Thread.sleep(10);

        // Используем compareTo для сравнения значений без учета точности
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
            assertEquals("Order " + order.getId() + " successfully submitted.", status.getMessage());
        });

        Thread.sleep(100);

        // Закрываем биржу
//        exchange.closeExchange();


        order.addStatusCallback(status -> {
            assertEquals("Order canceled: Exchange is closed.", status.getErrorMessage());
        });

    }


    @Test
    public void testPartOrderExecution() throws InterruptedException {
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
            else if (status.isHasMessage()) System.out.println(status.getMessage());
            else callbackQueue.add(status);
        });

        // Создаем ордер на продажу
        Order sellOrder = new Order(OrderType.SELL, client2.getId(), new CurrencyPair(Currency.RUB, Currency.CNY), new BigDecimal("50"), new BigDecimal("100"));
        System.out.println(sellOrder);
        exchange.createOrder(sellOrder, status -> {
            if (status.isHasError()) System.out.println(status.getErrorMessage());
            else if (status.isHasMessage()) System.out.println(status.getMessage());
            else callbackQueue.add(status);
        });

        // Проверяем, что ордер был выполнен (если есть подходящий ордер для сделки)
        List<Order> openOrders = exchange.getOpenOrders();
        System.out.println(openOrders);


        Order sellOrder2 = new Order(OrderType.SELL, client2.getId(), new CurrencyPair(Currency.RUB, Currency.CNY), new BigDecimal("50"), new BigDecimal("100"));
        System.out.println(sellOrder2);
        exchange.createOrder(sellOrder2, status -> {
            if (status.isHasError()) System.out.println(status.getErrorMessage());
            else if (status.isHasMessage()) System.out.println(status.getMessage());
            else callbackQueue.add(status);
        });
        openOrders = exchange.getOpenOrders();
        System.out.println(openOrders);


        TransactionInfoHandler transactionInfoHandler = new TransactionInfoHandler(clientsList, callbackQueue);
        transactionInfoHandler.processTransactions();

    }




}
