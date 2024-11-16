package labs.rsreu.exchanges;

import labs.rsreu.orders.Order;

import java.util.List;
import java.util.function.Consumer;

/**
 * Интерфейс для биржи
 */
public interface IExchange {

    /**
     * Создание заявки на покупку или продажу валюты.
     * @param order - Заявка на покупку/продажу.
     * @param resultCallback - Результат добавления заявки
     */
    void createOrder(Order order, Consumer<String> resultCallback);

    /**
     * Запрос текущих открытых заявок. (Складываются списки ордеров на продажу и покупку)
     * @return - Список всех открытых заявок.
     */
    List<Order> getOpenOrders();

}
