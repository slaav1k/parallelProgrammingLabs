package DisruptorExchangeTests;

import com.lmax.disruptor.dsl.ProducerType;
import labs.rsreu.clients.Client;
import labs.rsreu.clients.ClientsList;
import labs.rsreu.currencies.Currency;
import labs.rsreu.currencies.CurrencyPair;
import labs.rsreu.currencies.CurrencyPairRegistry;
import labs.rsreu.exchanges.DisruptorExchange;
import labs.rsreu.exchanges.IExchange;
import labs.rsreu.exchanges.ResponseEvent;
import labs.rsreu.orders.Order;
import labs.rsreu.orders.OrderType;
import labs.rsreu.orders.TransactionInfo;
import labs.rsreu.orders.TransactionInfoHandler;
import com.lmax.disruptor.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;

public class DisruptorExchangeTest2 {
    private IExchange exchange;
    private ClientsList clientsList;
    private CurrencyPairRegistry currencyPairRegistry;

    private RingBuffer<ResponseEvent> responseBuffer;

    @BeforeEach
    public void setUp() {
        clientsList = new ClientsList();
        currencyPairRegistry = new CurrencyPairRegistry();

        // Добавляем валютные пары
        List<CurrencyPair> fixedCurrencyPairs = List.of(
                new CurrencyPair(Currency.RUB, Currency.USD),
                new CurrencyPair(Currency.USD, Currency.EUR),
                new CurrencyPair(Currency.EUR, Currency.CNY),
                new CurrencyPair(Currency.CNY, Currency.RUB),
                new CurrencyPair(Currency.RUB, Currency.CNY)
        );

        for (CurrencyPair pair : fixedCurrencyPairs) {
            currencyPairRegistry.addCurrencyPair(pair);
        }

        // Создаем буфер
        EventFactory<ResponseEvent> responseFactory = ResponseEvent::new;
        int bufferSize = 2048 * 128; // Размер буфера
        responseBuffer = RingBuffer.create(
                ProducerType.MULTI,            // Разрешаем многопоточность
                responseFactory,               // Фабрика событий
                bufferSize,                    // Размер буфера
                new BlockingWaitStrategy()     // Стратегия ожидания
        );

        // Создаем биржу с одним параметром - буфером
        exchange = new DisruptorExchange(currencyPairRegistry, responseBuffer);
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


        // Получаем количество событий, которое сейчас в буфере (которые были опубликованы)
        long sequence = responseBuffer.getCursor(); // Это текущая позиция "курсор" в буфере

        // Проходим по всем событиям, которые были опубликованы
        for (long i = 0; i <= sequence; i++) {
            ResponseEvent event = responseBuffer.get(i);
            TransactionInfo info = event.getTransactionInfo();

            System.out.println("Event at index " + i + ": " + info);
        }


//        TransactionInfo info = callbackQueue.poll();
//        TransactionInfo info = responseBuffer.get(responseBuffer.next()).getTransactionInfo();
//        assertTrue(info.isHasError());
//        assertEquals("ERROR Order " + order.getId() + " has invalid currency pair.", info.getErrorMessage());
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
                    System.out.println("Received status update: " + status);});

        // Создаем ордер на продажу
        Order sellOrder = new Order(OrderType.SELL, client2.getId(), new CurrencyPair(Currency.RUB, Currency.CNY), new BigDecimal("100"), new BigDecimal("200"));
        System.out.println(sellOrder);
        exchange.createOrder(sellOrder, status2 -> {
                    System.out.println("Received status update: " + status2);});

        // Проверяем, что ордер был выполнен (если есть подходящий ордер для сделки)
        List<Order> openOrders = exchange.getOpenOrders();
        System.out.println(openOrders);

        long sequence = responseBuffer.getCursor();

        for (long i = 0; i <= sequence; i++) {
            ResponseEvent event = responseBuffer.get(i);
            TransactionInfo info = event.getTransactionInfo();

            System.out.println("Event at index " + i + ": " + info);
        }

        TransactionInfoHandler transactionInfoHandler = new TransactionInfoHandler(clientsList, responseBuffer);
        transactionInfoHandler.processTransactionsDisruptor();

        Thread.sleep(10);


        System.out.println(client);

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

        // Создаем ордер и проверяем сообщение о добавлении
        exchange.createOrder(order, status -> {
            assertEquals("Order " + order.getId() + " successfully submitted.", status.getMessage());
        });

        // Закрываем биржу
        exchange.closeExchange();

        order.addStatusCallback(status -> {
            assertEquals("Order canceled: Exchange is closed.", status.getErrorMessage());
        });
    }

    @Test
    public void testPartOrderExecution() throws InterruptedException {
        // Создаем клиентов с начальным балансом
        Client client = clientsList.createClient(new EnumMap<>(Currency.class) {{
            put(Currency.RUB, new BigDecimal("1000"));
            put(Currency.CNY, new BigDecimal("500"));
        }});

        Client client2 = clientsList.createClient(new EnumMap<>(Currency.class) {{
            put(Currency.RUB, new BigDecimal("500"));
            put(Currency.CNY, new BigDecimal("1000"));
        }});

        ConcurrentLinkedQueue<TransactionInfo> callbackQueue = new ConcurrentLinkedQueue<>();
        CountDownLatch latch = new CountDownLatch(2);

        // Покупатель создает заявку на покупку
        Order buyOrder = new Order(OrderType.BUY, client.getId(), new CurrencyPair(Currency.RUB, Currency.CNY), new BigDecimal("100"), new BigDecimal("300"));
        exchange.createOrder(buyOrder, transactionInfo -> {
            callbackQueue.add(transactionInfo);
            latch.countDown();
        });

        // Продавец создает заявку на продажу
        Order sellOrder = new Order(OrderType.SELL, client2.getId(), new CurrencyPair(Currency.RUB, Currency.CNY), new BigDecimal("50"), new BigDecimal("100"));
        exchange.createOrder(sellOrder, transactionInfo -> {
            callbackQueue.add(transactionInfo);
            latch.countDown();
        });

        latch.await();

        // Создаем второй ордер для исполнения
        // Создаем второй ордер для исполнения
        Order sellOrder2 = new Order(OrderType.SELL, client2.getId(), new CurrencyPair(Currency.RUB, Currency.CNY), new BigDecimal("50"), new BigDecimal("100"));
        exchange.createOrder(sellOrder2, transactionInfo -> {
            callbackQueue.add(transactionInfo);
            latch.countDown();
        });

        // Ожидаем завершения всех операций
        latch.await();

        // Обрабатываем транзакции
        TransactionInfoHandler transactionHandler = new TransactionInfoHandler(clientsList, callbackQueue);
        transactionHandler.processTransactions();

        // Проверяем, что ордера были частично выполнены
        assertEquals(0, client.getBalance(Currency.RUB).compareTo(new BigDecimal("1100")));
        assertEquals(0, client.getBalance(Currency.CNY).compareTo(new BigDecimal("200")));
        assertEquals(0, client2.getBalance(Currency.RUB).compareTo(new BigDecimal("400")));
        assertEquals(0, client2.getBalance(Currency.CNY).compareTo(new BigDecimal("1200")));
    }
}

