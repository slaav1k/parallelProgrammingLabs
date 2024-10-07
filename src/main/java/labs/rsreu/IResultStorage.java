package labs.rsreu;

/**
 * Интерфейс для классов хранилищ общего результата расчета
 */
public interface IResultStorage {
    /**
     * Синхронизированный метод для добавления результатов
     * @param result - обновленный результат
     */
    void addResult(double result);

    /**
     * Получение общего результата
     * @return - общий результат
     */
    double getTotalResult();
}
