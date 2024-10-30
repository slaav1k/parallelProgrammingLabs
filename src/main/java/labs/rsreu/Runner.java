package labs.rsreu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.*;

public class Runner {
    public static void main(String[] args) throws InterruptedException {
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
//        CountDownLatch latch = new CountDownLatch(countThreads);
        MyLatch latch = new MyLatch(countThreads);
        List<Future<Double>> futures = new ArrayList<>();
        HashMap<Integer, Long> times = new HashMap<Integer, Long>();
//        Semaphore semaphore = new Semaphore(6);
        MySemaphore semaphore = new MySemaphore(6);

        // Создаем и запускаем задачи
        for (int i = 0; i < countThreads; i++) {
            double start = a + i * intervalLength;
            double end = start + intervalLength;
            int taskId = i;

            // Создаем отдельный калькулятор для каждого подотрезка
            DefIntegralCalculator calculator = new DefIntegralCalculator(start, end, n / countThreads, resultStorage);
            Callable<Double> task = new IntegralCalculatorRun(calculator, taskId + 1, resultStorage, semaphore);

            // Отправляем задачу в пул и сохраняем Future
            try {
//                futures.add(executor.submit(task));

                futures.add(
                        executor.submit(() -> {
                            try {
                                return task.call();
                            } finally {
                                long finishTime = System.nanoTime();
                                synchronized (times) {
                                    times.put(taskId, finishTime);
                                }
                                System.out.printf("Поток %d завершен в: %d\n", taskId + 1, finishTime);
                                latch.countDown();
                            }
                        })
                );
            } catch (RejectedExecutionException e) {
                System.err.println("Ошибка добавления задачи в пул: " + e.getMessage());
            }
        }

        latch.await();

        // Ожидание завершения всех задач и вывод результатов
        for (Future<Double> future : futures) {
            try {
                // Получаем результат, если он есть
                double result = future.get();
                totalRes += result;
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                executor.shutdownNow();
            }
        }
        System.out.printf("Общий прогресс: %.2f%%%n", 100.0);
        executor.shutdown();


        // Проверка завершения всех задач
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }


        // Находим максимальное время завершения
        long maxTime = maxCompletionTime(times);

        // Вывод задержки для каждой задачи
        for (int i = 0; i < countThreads; i++) {
            long delay = maxTime - times.get(i);
            System.out.printf("Поток %d завершен за: %.2f мкс до завершения всех позадач\n", i + 1, delay / 1_000_000.0);
        }


        long endTime = System.nanoTime();
        double durationInSeconds = (endTime - startTime) / 1_000_000_000.0;

//        System.out.printf("Общий результат интеграла от %.2f до %.2f: %.9f%n", a, b, resultStorage.getTotalResult());
        System.out.printf("Общий результат интеграла от %.2f до %.2f: %.9f%n", a, b, totalRes);
        System.out.printf("Time taken: %.9f seconds%n", durationInSeconds);
    }

    /*
    Расчет максимального время выполнения
     */
    private static long maxCompletionTime(HashMap<Integer, Long> completionTimes) {
        long maxCompletionTime = Long.MIN_VALUE;
        for (long time : completionTimes.values()) {
            if (time > maxCompletionTime) {
                maxCompletionTime = time;
            }
        }
        return maxCompletionTime;
    }
}
