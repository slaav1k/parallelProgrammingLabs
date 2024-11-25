import labs.rsreu.clients.Client;
import labs.rsreu.clients.ClientsList;
import labs.rsreu.currencies.Currency;
import labs.rsreu.currencies.CurrencyPair;
import labs.rsreu.currencies.CurrencyPairRegistry;
import labs.rsreu.exchanges.Exchange;
import labs.rsreu.exchanges.IExchange;
import labs.rsreu.exchanges.AsyncExchange;
import labs.rsreu.orders.OrderTask;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;

public class ExchangePerformanceTest {
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

        // Подсчет начального времени для обычной реализации Exchange
        IExchange exchange1 = new Exchange(currencyPairRegistry);
        long startTime1 = System.nanoTime();
        executeOrdersForExchange(exchange1, clients, currencyPairRegistry);
        long endTime1 = System.nanoTime();
        long duration1 = endTime1 - startTime1;

        // Подсчет начального времени для асинхронной реализации AsyncExchange
        IExchange exchange2 = new AsyncExchange(currencyPairRegistry);
        long startTime2 = System.nanoTime();
        executeOrdersForExchange(exchange2, clients, currencyPairRegistry);
        long endTime2 = System.nanoTime();
        long duration2 = endTime2 - startTime2;

        // Выводим результаты
        System.out.println("Execution time for Exchange: " + duration1 + " ns");
        System.out.println("Execution time for AsyncExchange: " + duration2 + " ns");

        // Рассчитываем количество заявок, обработанных в секунду
        double requestsPerSecond1 = (double) MAX_COUNT_CLIENTS * MAX_COUNT_ORDERS / (duration1 / 1_000_000_000.0);
        double requestsPerSecond2 = (double) MAX_COUNT_CLIENTS * MAX_COUNT_ORDERS / (duration2 / 1_000_000_000.0);

        System.out.println("Requests per second for Exchange: " + requestsPerSecond1);
        System.out.println("Requests per second for AsyncExchange: " + requestsPerSecond2);

        // Проверяем, что асинхронная реализация быстрее или равна обычной
        assertTrue(requestsPerSecond2 >= requestsPerSecond1, "AsyncExchange should be faster or at least equal to Exchange");
    }

    // Метод для запуска заявок для теста
    private void executeOrdersForExchange(IExchange exchange, ClientsList clients, CurrencyPairRegistry currencyPairRegistry) throws InterruptedException {
        // Создаем ExecutorService для обработки заявок в разных потоках
        ExecutorService executorService = Executors.newFixedThreadPool(clients.size());

        // Запускаем задачи для каждого клиента
        List<Callable<String>> tasks = new ArrayList<>();
        for (Client client : clients.getAllClients()) {
            tasks.add(new OrderTask(client, exchange, currencyPairRegistry, MAX_COUNT_ORDERS));
        }

        // Выполняем все задачи параллельно
        List<Future<String>> results = executorService.invokeAll(tasks);

        // Ожидаем завершения всех задач
        for (Future<String> result : results) {
            try {
                result.get(); // Не выводим, но ожидаем завершения
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }

        // Закрываем executor
        executorService.shutdown();
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

    // Метод для добавления всех валютных пар
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
