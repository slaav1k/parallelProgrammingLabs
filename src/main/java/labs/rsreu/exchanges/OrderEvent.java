package labs.rsreu.exchanges;

import labs.rsreu.orders.Order;

public class OrderEvent {
    private Order order;

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }
}
