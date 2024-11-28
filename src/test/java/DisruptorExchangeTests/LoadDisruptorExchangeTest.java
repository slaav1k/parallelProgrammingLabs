package DisruptorExchangeTests;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.util.DaemonThreadFactory;
import labs.rsreu.clients.Client;
import labs.rsreu.clients.ClientsList;
import labs.rsreu.currencies.Currency;
import labs.rsreu.currencies.CurrencyPair;
import labs.rsreu.currencies.CurrencyPairRegistry;
import labs.rsreu.exchanges.*;
import labs.rsreu.orders.OrderTask;
import labs.rsreu.orders.TransactionInfo;
import labs.rsreu.orders.TransactionInfoHandler;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class LoadDisruptorExchangeTest {
    private static final Random random = new Random();
    private static final int MAX_COUNT_CLIENTS = 2;
    private static final int MAX_COUNT_ORDERS = 100_000;

    @Test
    public void testLoadWithRandomOrders() throws InterruptedException {
        // Создаем биржу и регистрируем валютные пары
        CurrencyPairRegistry currencyPairRegistry = new CurrencyPairRegistry();
//        addRandomCurrencyPairs(currencyPairRegistry);
        addAllCurrencyPairs(currencyPairRegistry);

//        // Создаем responseBuffer для обработки ответов
        int bufferSize = 2048 * 128;
//        EventFactory<ResponseEvent> responseFactory = ResponseEvent::new;
////        RingBuffer<ResponseEvent> responseBuffer = RingBuffer.createSingleProducer(responseFactory, bufferSize, new BlockingWaitStrategy());
//        RingBuffer<ResponseEvent> responseBuffer = RingBuffer.create(
//                ProducerType.MULTI,         // Разрешаем доступ из нескольких потоков
//                responseFactory,            // Фабрика событий
//                bufferSize,                 // Размер буфера
//                new BlockingWaitStrategy()  // Используем блокирующую стратегию ожидания
//        );

        // Генерация и создание клиентов
        ClientsList clients = new ClientsList();
        generateClients(MAX_COUNT_CLIENTS, clients);

        // Создаем Disruptor
        Disruptor<ResponseEvent> disruptor = new Disruptor<>(
                ResponseEvent::new,          // Фабрика событий
                bufferSize,                  // Размер буфера
                DaemonThreadFactory.INSTANCE, // Потокобезопасная фабрика потоков
                ProducerType.MULTI,          // Поддержка многопоточной публикации
                new BlockingWaitStrategy()   // Стратегия ожидания
        );

        // Передача обработчика событий
        ResponseEventHandler handler = new ResponseEventHandler(clients);
        disruptor.handleEventsWith(handler);

        // Запуск Disruptor
        disruptor.start();

        // Получаем RingBuffer из Disruptor
        RingBuffer<ResponseEvent> responseBuffer = disruptor.getRingBuffer();



        IExchange exchange = new DisruptorExchange(currencyPairRegistry, responseBuffer);





        // Сохраняем начальный баланс биржи по каждой валюте
        EnumMap<Currency, BigDecimal> initialBalances = clients.getTotalBalances();

        // Создаем ExecutorService для обработки заявок в разных потоках
        ExecutorService executorService = Executors.newFixedThreadPool(clients.size());

        ConcurrentLinkedQueue<TransactionInfo> callbackQueue = new ConcurrentLinkedQueue<>();


        // Запускаем задачи для каждого клиента
        List<Callable<String>> tasks = new ArrayList<>();
        for (Client client : clients.getAllClients()) {
            tasks.add(new OrderTask(client, exchange, currencyPairRegistry, MAX_COUNT_ORDERS, callbackQueue));
        }

        // Выполняем все задачи параллельно
        List<Future<String>> results = executorService.invokeAll(tasks);

        // Ожидаем завершения всех задач
        for (Future<String> result : results) {
            try {
                System.out.println(result.get());
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }

        // Закрываем executor
        executorService.shutdown();
        disruptor.shutdown();

//        TransactionInfoHandler transactionInfoHandler = new TransactionInfoHandler(clients, callbackQueue);
//        transactionInfoHandler.processTransactions();


//        TransactionInfoHandler transactionInfoHandler = new TransactionInfoHandler(clients, responseBuffer);
//        transactionInfoHandler.processTransactionsDisruptor();

        // Сравниваем итоговый баланс биржи по каждой валюте
        EnumMap<Currency, BigDecimal> finalBalances = clients.getTotalBalances();

        // Проверяем, что баланс биржи по каждой валюте не изменился
        // Сравниваем балансы каждой валюты на бирже
        for (Currency currency : Currency.values()) {
            // Получаем баланс для текущей валюты для начального и итогового баланса
            BigDecimal initialBalance = initialBalances.get(currency);
            BigDecimal finalBalance = finalBalances.get(currency);

            // Устанавливаем точность для сравнения, например, до 10 знаков после запятой
            BigDecimal initialBalanceRounded = initialBalance.setScale(10, BigDecimal.ROUND_HALF_UP);
            BigDecimal finalBalanceRounded = finalBalance.setScale(10, BigDecimal.ROUND_HALF_UP);

            // Сравниваем балансы
            assertEquals(0, initialBalanceRounded.compareTo(finalBalanceRounded), "Баланс для валюты " + currency + " должен остаться неизменным. " +
                    "Expected: " + initialBalanceRounded + ", but was: " + finalBalanceRounded);
        }


    }

    // Метод для создания случайных клиентов и добавления их в биржу
    private void generateClients(int numberOfClients, ClientsList clients) {
        for (int i = 0; i < numberOfClients; i++) {
            // Генерация случайного клиента с балансом
            EnumMap<Currency, BigDecimal> initialBalances = new EnumMap<>(Currency.class);
            for (Currency currency : Currency.values()) {
                initialBalances.put(currency, BigDecimal.valueOf(100 + random.nextDouble() * 900));
            }
            // Добавляем клиента в биржу
            clients.createClient(initialBalances);
        }
    }


    // Метод для добавления случайных валютных пар
    private static void addRandomCurrencyPairs(CurrencyPairRegistry registry) {
        int totalPairs = Currency.values().length * (Currency.values().length - 1);
        int randomPairsCount = 1 + random.nextInt(totalPairs);
        int addedPairs = 0;

        while (addedPairs < randomPairsCount) {
            Currency baseCurrency = Currency.values()[random.nextInt(Currency.values().length)];
            Currency quoteCurrency = Currency.values()[random.nextInt(Currency.values().length)];
            if (baseCurrency != quoteCurrency) {
                CurrencyPair currencyPair = new CurrencyPair(baseCurrency, quoteCurrency);
                if (!registry.isValidCurrencyPair(currencyPair)) {
                    registry.addCurrencyPair(currencyPair);
                    addedPairs++;
                }
            }
        }
    }


    private static void addAllCurrencyPairs(CurrencyPairRegistry registry) {
        Currency[] currencies = Currency.values();

        for (Currency baseCurrency : currencies) {
            for (Currency quoteCurrency : currencies) {
                if (baseCurrency != quoteCurrency) { // Исключаем одинаковые валюты в паре
                    CurrencyPair currencyPair = new CurrencyPair(baseCurrency, quoteCurrency);
                    if (!registry.isValidCurrencyPair(currencyPair)) {
                        registry.addCurrencyPair(currencyPair);
                    }
                }
            }
        }
    }

}
