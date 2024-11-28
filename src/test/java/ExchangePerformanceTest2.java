import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.RingBuffer;
import labs.rsreu.clients.Client;
import labs.rsreu.clients.ClientsList;
import labs.rsreu.currencies.Currency;
import labs.rsreu.currencies.CurrencyPair;
import labs.rsreu.currencies.CurrencyPairRegistry;
import labs.rsreu.exchanges.Exchange;
import labs.rsreu.exchanges.IExchange;
import labs.rsreu.exchanges.AsyncExchange;
import labs.rsreu.exchanges.DisruptorExchange;
import labs.rsreu.orders.OrderTask;
import labs.rsreu.orders.TransactionInfo;
import labs.rsreu.orders.TransactionInfoHandler;
import labs.rsreu.exchanges.ResponseEvent;
import labs.rsreu.exchanges.ResponseEventHandler;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.util.DaemonThreadFactory;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;

public class ExchangePerformanceTest2 {
    private static final Random random = new Random();
    private static final int MAX_COUNT_CLIENTS = 2;
    private static final int MAX_COUNT_ORDERS = 10_000;

    @Test
    public void compareExchangeImplementationsSpeed() throws InterruptedException {
        // Создаем реестр валютных пар и добавляем все возможные валютные пары
        CurrencyPairRegistry currencyPairRegistry = new CurrencyPairRegistry();
        addAllCurrencyPairs(currencyPairRegistry);

        // Генерация и создание клиентов
        ClientsList clients = new ClientsList();
        generateClients(MAX_COUNT_CLIENTS, clients);

        // Обычная биржа Exchange
        IExchange exchange1 = new Exchange(currencyPairRegistry);
        long startTime1 = System.nanoTime();
        executeOrdersForExchange(exchange1, clients, currencyPairRegistry);
        long endTime1 = System.nanoTime();
        long duration1 = endTime1 - startTime1;

        // Асинхронная биржа AsyncExchange
        IExchange exchange2 = new AsyncExchange(currencyPairRegistry);
        long startTime2 = System.nanoTime();
        executeOrdersForExchange(exchange2, clients, currencyPairRegistry);
        long endTime2 = System.nanoTime();
        long duration2 = endTime2 - startTime2;

        // Биржа на Disruptor
        int bufferSize = 2048 * 128; // Размер буфера событий
//        Disruptor<ResponseEvent> disruptor = new Disruptor<>(
//                ResponseEvent::new,
//                bufferSize,
//                DaemonThreadFactory.INSTANCE,
//                ProducerType.MULTI,
//                new com.lmax.disruptor.BlockingWaitStrategy()
//        );
//
//        // Обработчик событий
//        ResponseEventHandler responseHandler = new ResponseEventHandler(clients);
//        disruptor.handleEventsWith(responseHandler);
//
//        // Запускаем Disruptor
//        disruptor.start();

        EventFactory<ResponseEvent> responseFactory = ResponseEvent::new;
        RingBuffer<ResponseEvent> responseBuffer = RingBuffer.create(
                ProducerType.MULTI,         // Разрешаем доступ из нескольких потоков
                responseFactory,            // Фабрика событий
                bufferSize,                 // Размер буфера
                new BlockingWaitStrategy()  // Используем блокирующую стратегию ожидания
        );

        // Создаем биржу на основе Disruptor
//        IExchange exchange3 = new DisruptorExchange(currencyPairRegistry, disruptor.getRingBuffer());
        IExchange exchange3 = new DisruptorExchange(currencyPairRegistry, responseBuffer);
        long startTime3 = System.nanoTime();
        executeOrdersForExchange(exchange3, clients, currencyPairRegistry);



        long endTime3 = System.nanoTime();
        long duration3 = endTime3 - startTime3;

        TransactionInfoHandler transactionInfoHandler = new TransactionInfoHandler(clients, responseBuffer);
        transactionInfoHandler.processTransactionsDisruptor();

        // Останавливаем Disruptor
//        disruptor.shutdown();


        // Вывод результатов
        System.out.println("Execution time for Exchange: " + duration1 + " ns");
        System.out.println("Execution time for AsyncExchange: " + duration2 + " ns");
        System.out.println("Execution time for DisruptorExchange: " + duration3 + " ns");

        double requestsPerSecond1 = (double) MAX_COUNT_CLIENTS * MAX_COUNT_ORDERS / (duration1 / 1_000_000_000.0);
        double requestsPerSecond2 = (double) MAX_COUNT_CLIENTS * MAX_COUNT_ORDERS / (duration2 / 1_000_000_000.0);
        double requestsPerSecond3 = (double) MAX_COUNT_CLIENTS * MAX_COUNT_ORDERS / (duration3 / 1_000_000_000.0);

        System.out.println("Requests per second for Exchange: " + requestsPerSecond1);
        System.out.println("Requests per second for AsyncExchange: " + requestsPerSecond2);
        System.out.println("Requests per second for DisruptorExchange: " + requestsPerSecond3);

        // Проверяем, что DisruptorExchange быстрее или равен AsyncExchange и Exchange
        assertTrue(requestsPerSecond3 >= requestsPerSecond2, "DisruptorExchange should be faster or at least equal to AsyncExchange");
        assertTrue(requestsPerSecond3 >= requestsPerSecond1, "DisruptorExchange should be faster or at least equal to Exchange");
    }

    // Метод для запуска заявок для теста
    private void executeOrdersForExchange(IExchange exchange, ClientsList clients, CurrencyPairRegistry currencyPairRegistry) throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(clients.size());
        ConcurrentLinkedQueue<TransactionInfo> callbackQueue = new ConcurrentLinkedQueue<>();

        List<Callable<String>> tasks = new ArrayList<>();
        for (Client client : clients.getAllClients()) {
            tasks.add(new OrderTask(client, exchange, currencyPairRegistry, MAX_COUNT_ORDERS, callbackQueue));
        }

        List<Future<String>> results = executorService.invokeAll(tasks);
        for (Future<String> result : results) {
            try {
                result.get();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }

        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.MINUTES);

        TransactionInfoHandler transactionInfoHandler = new TransactionInfoHandler(clients, callbackQueue);
        transactionInfoHandler.processTransactions();
    }

    private void generateClients(int numberOfClients, ClientsList clients) {
        for (int i = 0; i < numberOfClients; i++) {
            EnumMap<Currency, BigDecimal> initialBalances = new EnumMap<>(Currency.class);
            for (Currency currency : Currency.values()) {
                initialBalances.put(currency, BigDecimal.valueOf(100 + random.nextDouble() * 900));
            }
            clients.createClient(initialBalances);
        }
    }

    private static void addAllCurrencyPairs(CurrencyPairRegistry registry) {
        Currency[] currencies = Currency.values();
        for (Currency baseCurrency : currencies) {
            for (Currency quoteCurrency : currencies) {
                if (baseCurrency != quoteCurrency) {
                    CurrencyPair currencyPair = new CurrencyPair(baseCurrency, quoteCurrency);
                    if (!registry.isValidCurrencyPair(currencyPair)) {
                        registry.addCurrencyPair(currencyPair);
                    }
                }
            }
        }
    }
}
