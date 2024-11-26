//package AsyncExchangeTests;
//
//import labs.rsreu.clients.Client;
//import labs.rsreu.clients.ClientsList;
//import labs.rsreu.currencies.Currency;
//import labs.rsreu.currencies.CurrencyPair;
//import labs.rsreu.currencies.CurrencyPairRegistry;
//import labs.rsreu.exchanges.AsyncExchange;
//import labs.rsreu.exchanges.IExchange;
//import labs.rsreu.orders.Order;
//import labs.rsreu.orders.OrderType;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//
//import java.math.BigDecimal;
//import java.util.EnumMap;
//import java.util.List;
//import java.util.function.Consumer;
//
//import static java.lang.Thread.sleep;
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertThrows;
//
//public class AsyncExchangeTest {
//    private IExchange exchange;
////    private AsyncExchangeOrderHandler handler;
//    private ClientsList clientsList;
////    private BlockingQueue<Order> orderQueue;
//    private CurrencyPairRegistry currencyPairRegistry;
//
//    @BeforeEach
//    public void setUp() {
//        this.clientsList = new ClientsList();
//
//        currencyPairRegistry = new CurrencyPairRegistry();
//
//        List<CurrencyPair> fixedCurrencyPairs = List.of(
//                new CurrencyPair(Currency.RUB, Currency.USD),
//                new CurrencyPair(Currency.USD, Currency.EUR),
//                new CurrencyPair(Currency.EUR, Currency.CNY),
//                new CurrencyPair(Currency.CNY, Currency.RUB),
//                new CurrencyPair(Currency.RUB, Currency.CNY),
//                new CurrencyPair(Currency.GBR, Currency.CNY)
//        );
//
//        for (CurrencyPair pair : fixedCurrencyPairs) {
//            if (!currencyPairRegistry.isValidCurrencyPair(pair)) {
//                currencyPairRegistry.addCurrencyPair(pair);
//            }
//        }
//
////        orderQueue = new LinkedBlockingQueue<>();
//
//        exchange = new AsyncExchange(/*orderQueue, */currencyPairRegistry);
////        handler = new AsyncExchangeOrderHandler(orderQueue);
////        Thread handlerThread = new Thread(handler);
////        handlerThread.setDaemon(true); // Поток завершится вместе с основным потоком
////        handlerThread.start();
//    }
//
//    @Test
//    public void testCreateClient() {
//        Client client = clientsList.createClient(new EnumMap<>(Currency.class) {{
//            put(Currency.RUB, new BigDecimal("1000"));
//            put(Currency.CNY, new BigDecimal("500"));
//        }});
//
//        // Проверка баланса клиента
//        assertEquals(new BigDecimal("1000"), client.getBalance(Currency.RUB));
//        assertEquals(new BigDecimal("500"), client.getBalance(Currency.CNY));
//    }
//
//    @Test
//    public void testCreateClientWithInvalidOrder() {
//        Client client = clientsList.createClient(new EnumMap<>(Currency.class) {{
//            put(Currency.RUB, new BigDecimal("1000"));
//            put(Currency.CNY, new BigDecimal("500"));
//        }});
//
//        // Попытка создать ордер с неправильной валютной парой
//        assertThrows(IllegalArgumentException.class, () -> exchange.createOrder(new Order(OrderType.BUY, client, new CurrencyPair(Currency.RUB, Currency.GBR),
//                new BigDecimal("100"), new BigDecimal("1.25"), currencyPairRegistry), System.out::println));
//    }
//
//
//    @Test
//    public void testBalanceUpdateAfterOrderExecution() throws InterruptedException {
//        // Создаем клиента с начальным балансом
//        Client client = clientsList.createClient(new EnumMap<>(Currency.class) {{
//            put(Currency.RUB, new BigDecimal("1000"));
//            put(Currency.CNY, new BigDecimal("500"));
//        }});
//
//        Client client2 = clientsList.createClient(new EnumMap<>(Currency.class) {{
//            put(Currency.RUB, new BigDecimal("500"));
//            put(Currency.CNY, new BigDecimal("1000"));
//        }});
//
//        // Создаем ордер на покупку
//        Order buyOrder = new Order(OrderType.BUY, client, new CurrencyPair(Currency.RUB, Currency.CNY), new BigDecimal("100"), new BigDecimal("300"), currencyPairRegistry);
//        System.out.println(buyOrder);
////        exchange.createOrder(buyOrder, System.out::println);
//        exchange.createOrder(buyOrder, status -> {
//            System.out.println("Received status update: " + status);
//        });
//
//        // Создаем ордер на продажу
//        Order sellOrder = new Order(OrderType.SELL, client2, new CurrencyPair(Currency.RUB, Currency.CNY), new BigDecimal("100"), new BigDecimal("200"), currencyPairRegistry);
//        System.out.println(sellOrder);
////        exchange.createOrder(sellOrder, System.out::println);
//        exchange.createOrder(sellOrder, status -> {
//            System.out.println("Received status update: " + status);
//        });
//
//        // Проверяем, что ордер был выполнен (если есть подходящий ордер для сделки)
//        List<Order> openOrders = exchange.getOpenOrders();
//        System.out.println(openOrders);
//
//        // Здесь система сама выполнит сделку, если найдется подходящий ордер
//        // Проверяем, что баланс клиента обновился автоматически
//        // Используем compareTo для сравнения значений без учета точности
//
//        Thread.sleep(10);
//
//
//        assertEquals(0, new BigDecimal("1100").compareTo(client.getBalance(Currency.RUB)));
//        assertEquals(0, new BigDecimal("200").compareTo(client.getBalance(Currency.CNY)));
//
//        System.out.println(client);
//
//    }
//
//
//    @Test
//    public void testAddOrderOnClosedExchange() {
//        // Закрываем биржу
//        exchange.closeExchange();
//
//        Client client = clientsList.createClient(new EnumMap<>(Currency.class) {{
//            put(Currency.RUB, new BigDecimal("500"));
//            put(Currency.CNY, new BigDecimal("1000"));
//        }});
//
//        // Пытаемся добавить ордер
//        Consumer<String> callback = message -> assertEquals("Order cannot be created: Exchange is closed.", message);
//        Order order = new Order(OrderType.SELL, client, new CurrencyPair(Currency.RUB, Currency.CNY), new BigDecimal("50"), new BigDecimal("100"), currencyPairRegistry);
//        exchange.createOrder(order, callback);
//    }
//
//
//    @Test
//    public void testOrderClosesWhenExchangeCloses() throws InterruptedException {
//        // Создаем клиента с балансом
//        Client client = clientsList.createClient(new EnumMap<>(Currency.class) {{
//            put(Currency.RUB, new BigDecimal("500"));
//            put(Currency.CNY, new BigDecimal("1000"));
//        }});
//
//        // Создаем ордер на открытую биржу
//        Order order = new Order(OrderType.SELL, client, new CurrencyPair(Currency.RUB, Currency.CNY), new BigDecimal("50"), new BigDecimal("100"), currencyPairRegistry);
//
//        String statusOrder = "";
//
//        Thread.sleep(100);
//
//
//
//        // Создаем ордер и проверяем сообщение о добавлении
//        exchange.createOrder(order, status -> {
//                    assertEquals("Order " + order.getId() + " successfully submitted.", status);
//        });
//
//        // Закрываем биржу
//        exchange.closeExchange();
//
//
//        order.addStatusCallback(status -> {
//            assertEquals("Order canceled: Exchange is closed.", status);
//        });
//
//    }
//
//    @Test
//    public void testPartOrderExecution() throws InterruptedException {
//        // Создаем клиента с начальным балансом
//        Client client = clientsList.createClient(new EnumMap<>(Currency.class) {{
//            put(Currency.RUB, new BigDecimal("1000"));
//            put(Currency.CNY, new BigDecimal("500"));
//        }});
//
//        Client client2 = clientsList.createClient(new EnumMap<>(Currency.class) {{
//            put(Currency.RUB, new BigDecimal("500"));
//            put(Currency.CNY, new BigDecimal("1000"));
//        }});
//
//        // Создаем ордер на покупку
//        Order buyOrder = new Order(OrderType.BUY, client, new CurrencyPair(Currency.RUB, Currency.CNY), new BigDecimal("100"), new BigDecimal("300"), currencyPairRegistry);
//        System.out.println(buyOrder);
//        exchange.createOrder(buyOrder, System.out::println);
//
//        // Создаем ордер на продажу
//        Order sellOrder = new Order(OrderType.SELL, client2, new CurrencyPair(Currency.RUB, Currency.CNY), new BigDecimal("50"), new BigDecimal("100"), currencyPairRegistry);
//        System.out.println(sellOrder);
//        exchange.createOrder(sellOrder, System.out::println);
//
//        // Проверяем, что ордер был выполнен (если есть подходящий ордер для сделки)
//        List<Order> openOrders = exchange.getOpenOrders();
//        System.out.println(openOrders);
//
//
//        Order sellOrder2 = new Order(OrderType.SELL, client2, new CurrencyPair(Currency.RUB, Currency.CNY), new BigDecimal("50"), new BigDecimal("100"), currencyPairRegistry);
//        System.out.println(sellOrder2);
//        exchange.createOrder(sellOrder2, System.out::println);
//        openOrders = exchange.getOpenOrders();
//        System.out.println(openOrders);
//
////        Thread.sleep(100);
////
////        assertEquals(0, openOrders.size());
//
//        // Здесь система сама выполнит сделку, если найдется подходящий ордер
//        // Проверяем, что баланс клиента обновился автоматически
//        // Используем compareTo для сравнения значений без учета точности
////        assertEquals(0, new BigDecimal("1100").compareTo(client.getBalance(Currency.RUB)));
////        assertEquals(0, new BigDecimal("200").compareTo(client.getBalance(Currency.CNY)));
//    }
//
//
//
//}
