package labs.rsreu;

import java.util.concurrent.Callable;

/**
 * Класс задачи для выполнения в потоке
 */
public class IntegralCalculatorRun implements Callable<Double> {
    private final DefIntegralCalculator calculator;
    private final int taskId;
    private final IResultStorage storage;

    /**
     * Конструктор класса, для генерации потоков (хранилище - ленивое)
     * @param calculator - вычислительная функция
     * @param taskId - номер потока
     * @param storage - хранилище результатов
     */
    public IntegralCalculatorRun(DefIntegralCalculator calculator, int taskId, IResultStorage storage) {
        this.calculator = calculator;
        this.taskId = taskId;
        this.storage = storage;
    }

    /**
     * Задачи потока
     */
    @Override
    public Double call() {
        System.out.println("Запуск задачи " + taskId);

        // Вычисляем интеграл для подотрезка
        double result = calculator.calculate();

        // Добавляем результат в общее хранилище
//        storage.addResult(result);
        System.out.printf("Задача %d завершена, результат: %.9f%n", taskId, result);

        // Возвращаем результат
        return result;
    }
}
