package labs.rsreu;

/**
 * Класс для ленивой инициализации хранилища результатов
 *  initialization-on-demand holder idiom
 */
public class ResultStorageLazyDemondHolder implements IResultStorage {
    // Поле для хранения суммы результатов
    private volatile double totalResult = 0.0;

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
        // Единственный экземпляр класса ResultStorageLazy.
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
     * Синхронизированный метод для добавления нового результата.
     * @param result - результат, который нужно добавить в общее хранилище
     */
    public synchronized void addResult(double result) {
        totalResult += result;
    }

    /**
     * Метод для получения общего результата.
     * @return - общий результат вычислений
     */
    public double getTotalResult() {
        return totalResult;
    }
}

