import labs.rsreu.clients.Client;
import labs.rsreu.clients.ClientsList;
import labs.rsreu.currencies.Currency;
import labs.rsreu.currencies.CurrencyPair;
import labs.rsreu.currencies.CurrencyPairRegistry;
import labs.rsreu.exchanges.Exchange;
import labs.rsreu.orders.Order;
import labs.rsreu.orders.OrderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;


import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.function.Consumer;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ExchangeTest {

    private Exchange exchange;
    private CurrencyPairRegistry currencyPairRegistry;
    private ClientsList clientsList;


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

        // Попытка создать ордер с неправильной валютной парой
        assertThrows(IllegalArgumentException.class, () -> exchange.createOrder(new Order(OrderType.BUY, client, new CurrencyPair(Currency.RUB, Currency.GBR),
                new BigDecimal("100"), new BigDecimal("1.25"), currencyPairRegistry), System.out::println));
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

        // Создаем ордер на покупку
        Order buyOrder = new Order(OrderType.BUY, client, new CurrencyPair(Currency.RUB, Currency.CNY), new BigDecimal("100"), new BigDecimal("300"), currencyPairRegistry);
        System.out.println(buyOrder);
        exchange.createOrder(buyOrder, System.out::println);

        // Создаем ордер на покупку
        Order sellOrder = new Order(OrderType.SELL, client2, new CurrencyPair(Currency.RUB, Currency.CNY), new BigDecimal("100"), new BigDecimal("200"), currencyPairRegistry);
        System.out.println(sellOrder);
        exchange.createOrder(sellOrder, System.out::println);

        // Проверяем, что ордер был выполнен (если есть подходящий ордер для сделки)
        List<Order> openOrders = exchange.getOpenOrders();
        System.out.println(openOrders);

        // Здесь система сама выполнит сделку, если найдется подходящий ордер
        // Проверяем, что баланс клиента обновился автоматически
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

        // Создаем ордер на покупку
        Order buyOrder = new Order(OrderType.BUY, client, new CurrencyPair(Currency.RUB, Currency.CNY), new BigDecimal("100"), new BigDecimal("300"), currencyPairRegistry);
        System.out.println(buyOrder);
        exchange.createOrder(buyOrder, System.out::println);

        // Создаем ордер на покупку
        Order sellOrder = new Order(OrderType.SELL, client2, new CurrencyPair(Currency.RUB, Currency.CNY), new BigDecimal("100"), new BigDecimal("200"), currencyPairRegistry);
        System.out.println(sellOrder);
        exchange.createOrder(sellOrder, System.out::println);

        // Проверяем, что ордер был выполнен (если есть подходящий ордер для сделки)
        List<Order> openOrders = exchange.getOpenOrders();
        System.out.println(openOrders);

        // Здесь система сама выполнит сделку, если найдется подходящий ордер
        // Проверяем, что баланс клиента обновился автоматически
        // Используем compareTo для сравнения значений без учета точности
        assertEquals(0, new BigDecimal("1033").compareTo(client.getBalance(Currency.RUB)));
        assertEquals(0, new BigDecimal("0").compareTo(client.getBalance(Currency.CNY)));

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

        // Создаем ордер на покупку
        Order buyOrder = new Order(OrderType.BUY, client, new CurrencyPair(Currency.RUB, Currency.CNY), new BigDecimal("100"), new BigDecimal("300"), currencyPairRegistry);
        System.out.println(buyOrder);
        exchange.createOrder(buyOrder, System.out::println);

        // Создаем ордер на продажу
        Order sellOrder = new Order(OrderType.SELL, client2, new CurrencyPair(Currency.RUB, Currency.CNY), new BigDecimal("50"), new BigDecimal("100"), currencyPairRegistry);
        System.out.println(sellOrder);
        exchange.createOrder(sellOrder, System.out::println);

        // Проверяем, что ордер был выполнен (если есть подходящий ордер для сделки)
        List<Order> openOrders = exchange.getOpenOrders();
        System.out.println(openOrders);

        assertEquals(1, openOrders.size());

        Order sellOrder2 = new Order(OrderType.SELL, client2, new CurrencyPair(Currency.RUB, Currency.CNY), new BigDecimal("50"), new BigDecimal("100"), currencyPairRegistry);
        System.out.println(sellOrder2);
        exchange.createOrder(sellOrder2, System.out::println);
        openOrders = exchange.getOpenOrders();
        System.out.println(openOrders);
        assertEquals(0, openOrders.size());

        // Здесь система сама выполнит сделку, если найдется подходящий ордер
        // Проверяем, что баланс клиента обновился автоматически
        // Используем compareTo для сравнения значений без учета точности
        assertEquals(0, new BigDecimal("1100").compareTo(client.getBalance(Currency.RUB)));
        assertEquals(0, new BigDecimal("200").compareTo(client.getBalance(Currency.CNY)));
    }



}