package labs.rsreu;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Runner {
    private static final List<Thread> threads = new ArrayList<>();

    public static void main(String[] args) {
        // Нижний и верхний пределы интегрирования
        double a = 0;
        double b = 1;

        // Количество интервалов
        int n = 100_000_000;

        // Количество потоков
        int countThreads = 10;

        // Длина каждого подотрезка
        double intervalLength = (b - a) / countThreads;

        // Создание объекта хранилища с ленивой инициализацией "Double-Checked Locking" idiom
//        IResultStorage resultStorage = ResultStorageLazy.getInstance();

        // Создание объекта хранилища обычной без ленивой инициализации
//        IResultStorage resultStorage = new ResultStorage();

        // Создание объекта хранилища с ленивой инициализацией  initialization-on-demand holder idiom
        IResultStorage resultStorage = ResultStorageLazyDemondHolder.getInstance();

        // Переменная для отслеживания прогресса выполнения
        AtomicInteger progress = new AtomicInteger(0);

        long startTime = System.nanoTime();

        // Создаем потоки для каждого подотрезка
        for (int i = 0; i < countThreads; i++) {
            double start = a + i * intervalLength;
            double end = start + intervalLength;

            // Создаем отдельный калькулятор для каждого подотрезка
            DefIntegralCalculator calculator = new DefIntegralCalculator(start, end, n / countThreads, progress);

            // Создаем и запускаем поток для выполнения задачи
            Runnable task = new IntegralCalculatorRun(calculator, i + 1, resultStorage);
            Thread thread = new Thread(task);
            threads.add(thread);
            thread.start();
        }

        // Поток для мониторинга процесса выполнения вычисления
        Runnable checkProgress = new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        break;
                    }
                    int completedSteps = progress.get();
                    double percentCompleted = (double) completedSteps / n * 100; // расчет процентов
                    System.out.printf("Общий прогресс: %.2f%%%n", percentCompleted);
                }
            }
        };

        Thread progressThread = new Thread(checkProgress);
        progressThread.setDaemon(true);
        progressThread.start();

        // Ожидание завершения всех потоков
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        long endTime = System.nanoTime();
        long duration = endTime - startTime;
        double durationInSeconds = (double) duration / 1_000_000_000.0;

        progressThread.interrupt();
        System.out.printf("Общий прогресс: %.2f%%%n", 100.0);

        // Получаем общий результат и выводим
        double totalResult = resultStorage.getTotalResult();
        System.out.printf("Общий результат интеграла от %.2f до %.2f: %.9f%n", a, b, totalResult);
        System.out.printf("Time taken: %.9f seconds%n", durationInSeconds);
    }
}
