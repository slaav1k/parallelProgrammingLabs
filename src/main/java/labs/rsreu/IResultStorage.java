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

    /**
     * Устанавливает общее количество шагов для вычисления интеграла.
     * @param totalSteps - общее количество интервалов разбиения
     */
    void setTotalSteps(int totalSteps);

    /**
     * Метод для увеличения числа итераций и вывода прогресса,
     * если достигнут порог.
     */
    void incrementIterations();
}
