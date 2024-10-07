package labs.rsreu;

/**
 * Класс для ленивой инициализации хранилища результатов
 * "Double-Checked Locking" idiom
 */
public class ResultStorageLazy implements IResultStorage {
    // Лениво инициализированный экземпляр
    private static ResultStorageLazy instance;
    // Сумма результатов
    private volatile double totalResult = 0.0;

    /**
     * Конструктор приватный для предотвращения создания новых экземпляров
     */
    private ResultStorageLazy() {
        System.out.println("Создание ленивого хранилища результатов.");
    }

//    /**
//     * Метод для получения экземпляра хранилища (ленивая инициализация)
//     * @return - единственный экземпляр ResultStorageLazy
//     */
//    public static synchronized ResultStorageLazy getInstance() {
//        if (instance == null) {
//            instance = new ResultStorageLazy();
//        }
//        return instance;
//    }

    /**
     * Метод для получения экземпляра хранилища (ленивая инициализация)
     * @return - единственный экземпляр ResultStorageLazy
     */
    public static ResultStorageLazy getInstance() {
        if (instance == null) {
            synchronized // заменил this на сам класс
            (ResultStorage.class) { if (instance == null)
                {
                    instance = new ResultStorageLazy();
                }
            }
        }
        return instance;
    }

    /**
     * Синхронизированный метод для добавления результатов
     * @param result - обновленный результат
     */
    public synchronized void addResult(double result) {
        totalResult += result;
    }

    /**
     * Получение общего результата
     * @return - общий результат
     */
    public double getTotalResult() {
        return totalResult;
    }
}
