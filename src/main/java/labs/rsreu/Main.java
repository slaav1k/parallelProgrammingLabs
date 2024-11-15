package labs.rsreu;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;

public class Main {
    private static final int MAX_COUNT_ORDERS = 50;
    private static final Random random = new Random();

    public static void main(String[] args) {
        int numberOfClients = 10;
        CurrencyPairRegistry registry = new CurrencyPairRegistry();
        addRandomCurrencyPairs(registry);
        IExchange exchange = new Exchange(registry);

        List<Client> clients = createClients(numberOfClients, exchange);

        EnumMap<Currency, BigDecimal> totalBalances = exchange.getTotalBalances();
        for (Map.Entry<Currency, BigDecimal> entry : totalBalances.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }


        // Создаем ExecutorService для выполнения задач
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfClients);

        // Список для хранения задач
        List<Future<String>> futures = new ArrayList<>();

        // Отправляем задачи на выполнение
        clients.forEach(client -> {
            OrderTask orderTask = new OrderTask(client, exchange, registry, MAX_COUNT_ORDERS);
            futures.add(executorService.submit(orderTask)); // Отправляем задачу
        });

        // Ожидаем завершения всех задач и выводим результат
        for (Future<String> future : futures) {
            try {
                System.out.println(future.get()); // Выводим результат завершения задачи
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        // Завершаем работу ExecutorService
        executorService.shutdown();

        EnumMap<Currency, BigDecimal> totalBalances2 = exchange.getTotalBalances();
        for (Map.Entry<Currency, BigDecimal> entry : totalBalances2.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
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

    // Создаем случайных клиентов
    private static List<Client> createClients(int numberOfClients, IExchange exchange) {
        List<Client> clients = new ArrayList<>(numberOfClients);
        for (int i = 0; i < numberOfClients; i++) {
            EnumMap<Currency, BigDecimal> initialBalances = new EnumMap<>(Currency.class);
            for (Currency currency : Currency.values()) {
                if (random.nextBoolean()) {
                    BigDecimal randomAmount = BigDecimal.valueOf(100 + random.nextDouble() * 900);
                    initialBalances.put(currency, randomAmount);
                }
            }
            Client client = exchange.createClient(initialBalances);
            clients.add(client);
            System.out.println("Создан клиент: " + client);
        }
        return clients;
    }
}

