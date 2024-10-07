package labs.rsreu;

/**
 * Класс для хранения результатов
 */
public class ResultStorage implements IResultStorage {
    // Сумма результатов
    private volatile double totalResult = 0.0;

    /**
     * Конструктор
     */
    public ResultStorage() {
        System.out.println("Создание НЕленивого хранилища результатов.");
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
