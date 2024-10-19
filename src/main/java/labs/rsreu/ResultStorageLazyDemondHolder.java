package labs.rsreu;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Класс для ленивой инициализации хранилища результатов
 * initialization-on-demand holder idiom
 */
public class ResultStorageLazyDemondHolder implements IResultStorage {
    // Поле для хранения суммы результатов
    private volatile double totalResult = 0.0;
    // Поле для отслеживания итераций
    private final AtomicInteger iterations = new AtomicInteger(0);
    // Общее количество шагов для вычисления (например, общее число интервалов)
    private int totalSteps;
    // Порог для вывода прогресса
    private final int progressThreshold = 15_000_000;
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * Приватный конструктор для предотвращения создания новых экземпляров
     * и обеспечения использования единственного объекта (Singleton).
     */
    private ResultStorageLazyDemondHolder() {
        System.out.println("Создание ленивого хранилища результатов.");
    }

    /**
     * Вложенный статический класс для хранения единственного экземпляра.
     * Инициализация происходит только при первом обращении к Holder.INSTANCE.
     */
    private static class Holder {
        // Единственный экземпляр класса ResultStorageLazyDemondHolder.
        private static final ResultStorageLazyDemondHolder INSTANCE = new ResultStorageLazyDemondHolder();
    }

    /**
     * Метод для получения единственного экземпляра класса.
     * Использует инициализацию через вложенный класс Holder для потокобезопасной ленивой инициализации.
     * @return - экземпляр класса ResultStorageLazy
     */
    public static ResultStorageLazyDemondHolder getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * Устанавливает общее количество шагов для вычисления интеграла.
     * @param totalSteps - общее количество интервалов разбиения
     */
    public void setTotalSteps(int totalSteps) {
        this.totalSteps = totalSteps;
    }

    /**
     * Синхронизированный метод для добавления нового результата.
     * @param result - результат, который нужно добавить в общее хранилище
     */
    public synchronized void addResult(double result) {
        totalResult += result;
    }

    /**
     * Метод для увеличения числа итераций и вывода прогресса,
     * если достигнут порог.
     */
    public void incrementIterations() {
        lock.lock(); // Получаем замок
        try {
            int currentIterations = iterations.incrementAndGet();
            if (currentIterations % progressThreshold == 0) {
                double percentCompleted = (double) currentIterations / totalSteps * 100;
                System.out.printf("Прогресс: %.2f%%%n", percentCompleted);
            }
        } finally {
            lock.unlock(); // Освобождаем замок
        }
    }

    /**
     * Метод для получения общего результата.
     * @return - общий результат вычислений
     */
    public double getTotalResult() {
        return totalResult;
    }
}
