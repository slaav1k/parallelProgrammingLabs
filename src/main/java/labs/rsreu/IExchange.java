package labs.rsreu;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.function.Consumer;

/**
 * Интерфейс для биржы
 */
public interface IExchange {
    /**
     * Создание клиента с начальным балансом.
     * @param balance - Начальный баланс клиента.
     * @return - Созданный клиент.
     */
    Client createClient(EnumMap<Currency, BigDecimal> balance);

    /**
     * Создание заявки на покупку или продажу валюты.
     * @param order - Заявка на покупку/продажу.
     * @param resultCallback - Результат добавления заявки
     */
    public void createOrder(Order order, Consumer<String> resultCallback);

    /**
     * Запрос текущих открытых заявок. (Складываются списки ордеров на продажу и покупку)
     * @return - Список всех открытых заявок.
     */
    List<Order> getOpenOrders();

    /**
     * Запрос состояния клиента.
     * @param clientId - Идентификатор клиента.
     * @return - Состояние (баланс) клиента.
     */
    Client getClientState(int clientId);

    /**
     * Сумма валют у клиентов на бирже
     * @return - список сумм по валютам
     */
    EnumMap<Currency, BigDecimal> getTotalBalances();
}
