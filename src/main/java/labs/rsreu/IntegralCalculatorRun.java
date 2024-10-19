package labs.rsreu;

import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Класс задачи для выполнения в потоке
 */
public class IntegralCalculatorRun implements Callable<Double> {
    private final DefIntegralCalculator calculator;
    private final int taskId;
    private final IResultStorage storage;
    private final Semaphore semaphore;

    /**
     * Конструктор класса, для генерации потоков (хранилище - ленивое)
     * @param calculator - вычислительная функция
     * @param taskId - номер потока
     * @param storage - хранилище результатов
     * @param semaphore - семафор, огрничитель на N потоков
     */
    public IntegralCalculatorRun(DefIntegralCalculator calculator, int taskId, IResultStorage storage, Semaphore semaphore) {
        this.calculator = calculator;
        this.taskId = taskId;
        this.storage = storage;
        this.semaphore = semaphore;
    }

    /**
     * Задачи потока
     */
    @Override
    public Double call() throws InterruptedException {
        System.out.println("Запуск задачи " + taskId);
        double result = 0.0;
        boolean permit = false;
//        try {
//            permit = semaphore.tryAcquire(1, TimeUnit.SECONDS);
//            if (permit) {
//                System.out.printf("Семафор для задачи %d получен.%n", taskId);
//                // Вычисляем интеграл для подотрезка
//                 result = calculator.calculate();
//
//                // Добавляем результат в общее хранилище
////        storage.addResult(result);
//                System.out.printf("Задача %d завершена, результат: %.9f%n", taskId, result);
//            } else {
//                result = 0.0;
//                System.out.printf("Семафор для задачи %d НЕ получен.%n", taskId);
//            }
//        } catch (InterruptedException e) {
////            throw new RuntimeException(e);
//            Thread.currentThread().interrupt(); // Восстанавливаем состояние прерывания
//            System.err.printf("Задача %d была прервана.%n", taskId);
//        } finally {
//            if (permit) {
//                semaphore.release();
//            }
//        }

        // Несколько попыток захвата семафора
        while (!permit) {
            try {
                permit = semaphore.tryAcquire(1, TimeUnit.SECONDS);
                if (permit) {
                    System.out.printf("Семафор для задачи %d получен.%n", taskId);
                    result = calculator.calculate();
                    System.out.printf("Задача %d завершена, результат: %.9f%n", taskId, result);
                } else {
                    System.out.printf("Семафор для задачи %d не получен. Повторная попытка...%n", taskId);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Восстанавливаем состояние прерывания
                System.err.printf("Задача %d была прервана.%n", taskId);
                break;
            }
        }

        if (permit) {
            semaphore.release();
        }

//        semaphore.acquire();
//        result = calculator.calculate();
//        System.out.printf("Задача %d завершена, результат: %.9f%n", taskId, result);
//        semaphore.release();


        // Возвращаем результат
        return result;
    }
}
