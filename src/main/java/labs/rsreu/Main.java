package labs.rsreu;

import labs.rsreu.clients.Client;
import labs.rsreu.clients.ClientsList;
import labs.rsreu.currencies.Currency;
import labs.rsreu.currencies.CurrencyPair;
import labs.rsreu.currencies.CurrencyPairRegistry;
import labs.rsreu.exchanges.Exchange;
import labs.rsreu.exchanges.IExchange;
import labs.rsreu.orders.OrderTask;
import labs.rsreu.orders.TransactionInfo;
import labs.rsreu.orders.TransactionInfoHandler;

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

        ClientsList clients = new ClientsList();
        generateClients(numberOfClients, clients);

        EnumMap<Currency, BigDecimal> totalBalances = clients.getTotalBalances();
        for (Map.Entry<labs.rsreu.currencies.Currency, BigDecimal> entry : totalBalances.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }


        // Создаем ExecutorService для выполнения задач
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfClients);

        // Список для хранения задач
        List<Future<String>> futures = new ArrayList<>();

        ConcurrentLinkedQueue<TransactionInfo> callbackQueue = new ConcurrentLinkedQueue<>();

        // Отправляем задачи на выполнение
        clients.getAllClients().forEach(client -> {
            OrderTask orderTask = new OrderTask(client, exchange, registry, MAX_COUNT_ORDERS, callbackQueue);
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

        System.out.println("do " + clients);

        System.out.println("all callbacks " + callbackQueue);

        TransactionInfoHandler transactionInfoHandler = new TransactionInfoHandler(clients, callbackQueue);
        transactionInfoHandler.processTransactions();

        System.out.println("posle " + clients);

        EnumMap<Currency, BigDecimal> totalBalances2 = clients.getTotalBalances();
        for (Map.Entry<labs.rsreu.currencies.Currency, BigDecimal> entry : totalBalances2.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
    }

    // Метод для добавления случайных валютных пар
    private static void addRandomCurrencyPairs(CurrencyPairRegistry registry) {
        int totalPairs = labs.rsreu.currencies.Currency.values().length * (labs.rsreu.currencies.Currency.values().length - 1);
        int randomPairsCount = 1 + random.nextInt(totalPairs);
        int addedPairs = 0;

        while (addedPairs < randomPairsCount) {
            labs.rsreu.currencies.Currency baseCurrency = labs.rsreu.currencies.Currency.values()[random.nextInt(labs.rsreu.currencies.Currency.values().length)];
            labs.rsreu.currencies.Currency quoteCurrency = labs.rsreu.currencies.Currency.values()[random.nextInt(labs.rsreu.currencies.Currency.values().length)];
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
    private static void generateClients(int numberOfClients, ClientsList clientsList) {
        for (int i = 0; i < numberOfClients; i++) {
            EnumMap<Currency, BigDecimal> initialBalances = new EnumMap<>(Currency.class);
            for (labs.rsreu.currencies.Currency currency : Currency.values()) {
                if (random.nextBoolean()) {
                    BigDecimal randomAmount = BigDecimal.valueOf(100 + random.nextDouble() * 900);
                    initialBalances.put(currency, randomAmount);
                }
            }
            Client client = clientsList.createClient(initialBalances);
            System.out.println("Создан клиент: " + client);
        }
    }
}

