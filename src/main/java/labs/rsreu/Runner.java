package labs.rsreu;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class Runner {
    public static void main(String[] args) {
        // Нижний и верхний пределы интегрирования
        double a = 0;
        double b = 1;

        // Количество интервалов
        int n = 100_000_000;

        // Количество потоков
        int countThreads = 10;

        // Общий результат
        double totalRes = 0;

        // Создание объекта хранилища с ленивой инициализацией
        IResultStorage resultStorage = ResultStorageLazyDemondHolder.getInstance();
        resultStorage.setTotalSteps(n);

        // Длина каждого подотрезка
        double intervalLength = (b - a) / countThreads;

        long startTime = System.nanoTime();

        // Создаем пул потоков
        ExecutorService executor = Executors.newFixedThreadPool(countThreads);
        List<Future<Double>> futures = new ArrayList<>();

        // Создаем и запускаем задачи
        for (int i = 0; i < countThreads; i++) {
            double start = a + i * intervalLength;
            double end = start + intervalLength;

            // Создаем отдельный калькулятор для каждого подотрезка
            DefIntegralCalculator calculator = new DefIntegralCalculator(start, end, n / countThreads, resultStorage);
            Callable<Double> task = new IntegralCalculatorRun(calculator, i + 1, resultStorage);

            // Отправляем задачу в пул и сохраняем Future
            try {
                futures.add(executor.submit(task));
            } catch (RejectedExecutionException e) {
                System.err.println("Ошибка добавления задачи в пул: " + e.getMessage());
            }
        }

        executor.shutdown();

        // Ожидание завершения всех задач и вывод результатов
        for (Future<Double> future : futures) {
            try {
                // Получаем результат, если он есть
                double result = future.get();
                totalRes += result;
                System.out.printf("Результат задачи: %.9f%n", result);
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }


        // Проверка завершения всех задач
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow(); // Принудительное завершение, если не завершено
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt(); // Восстанавливаем состояние прерывания
        }


        System.out.printf("Общий прогресс: %.2f%%%n", 100.0);


        long endTime = System.nanoTime();
        double durationInSeconds = (endTime - startTime) / 1_000_000_000.0;

//        System.out.printf("Общий результат интеграла от %.2f до %.2f: %.9f%n", a, b, resultStorage.getTotalResult());
        System.out.printf("Общий результат интеграла от %.2f до %.2f: %.9f%n", a, b, totalRes);
        System.out.printf("Time taken: %.9f seconds%n", durationInSeconds);
    }
}
