package labs.rsreu;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Order {
    private final int id; // Уникальный идентификатор заказа
    private final OrderType type; // Тип заказа (покупка или продажа)
    private final int clientId; // Идентификатор клиента
    private final CurrencyPair currencyPair; // Валютная пара для обмена
    private BigDecimal amountFirst; // Количество первой валюты
    private BigDecimal amountSecond; // Количество второй валюты
    private BigDecimal price; // Цена обмена
    private static int idCounter = 1; // Статический счетчик для создания уникальных ID
    private final Lock lock = new ReentrantLock(); // Блокировка для обеспечения потокобезопасности

    /**
     * Конструктор заказа с проверкой валидности валютной пары
     * @param type - тип заказа (покупка или продажа)
     * @param clientId - идентификатор клиента
     * @param currencyPair - валютная пара для обмена
     * @param amountFirst - количество первой валюты
     * @param amountSecond - количество второй валюты
     * @param registry - реестр валютных пар для проверки валидности
     */
    public Order(OrderType type, int clientId, CurrencyPair currencyPair, BigDecimal amountFirst, BigDecimal amountSecond, CurrencyPairRegistry registry) {
        if (!registry.isValidCurrencyPair(currencyPair)) {
            throw new IllegalArgumentException("Invalid currency pair");
        }
        this.id = idCounter++;
        this.type = type;
        this.clientId = clientId;
        this.currencyPair = currencyPair;
        this.amountFirst = amountFirst;
        this.amountSecond = amountSecond;
        this.price = calculatePrice();
    }

    /**
     * Конструктор копии
     * @param other - другой заказ
     */
    public Order(Order other) {
        this.id = other.id;
        this.type = other.type;
        this.clientId = other.clientId;
        this.currencyPair = other.currencyPair;
        this.amountFirst = other.amountFirst;
        this.amountSecond = other.amountSecond;
        this.price = other.price;
    }

    /**
     * Устанавливает количество первой валюты и пересчитывает цену
     * @param amountFirst - новое количество первой валюты
     * @throws IllegalArgumentException если количество отрицательное или равно нулю
     */
    public void setAmountFirst(BigDecimal amountFirst) {
        lock.lock();
        try {
            if (amountFirst.compareTo(BigDecimal.ZERO) > 0) {
                this.amountFirst = amountFirst;
                this.price = calculatePrice();
            } else {
                throw new IllegalArgumentException("Amount must be positive");
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Устанавливает количество второй валюты и пересчитывает цену
     * @param amountSecond - новое количество второй валюты
     * @throws IllegalArgumentException если количество отрицательное или равно нулю
     */
    public void setAmountSecond(BigDecimal amountSecond) {
        lock.lock();
        try {
            if (amountSecond.compareTo(BigDecimal.ZERO) > 0) {
                this.amountSecond = amountSecond;
                this.price = calculatePrice();
            } else {
                throw new IllegalArgumentException("Amount must be positive");
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Вычисляет цену на основе количества валют
     * @return вычисленная цена = количество второй валюты / количество первой валюты
     */
    private BigDecimal calculatePrice() {
        lock.lock();
        try {
            return amountSecond.divide(amountFirst, 15, RoundingMode.HALF_UP);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String toString() {
        return "Order ID: " + id +
                ", Client ID: " + clientId +
                ", Type: " + type +
                ", Currency Pair: " + currencyPair +
                ", Amount First: " + amountFirst +
                ", Amount Second: " + amountSecond +
                ", Price: " + price;
    }

    /**
     * Возвращает текущую цену заказа
     * @return цена заказа
     */
    public BigDecimal getPrice() {
        lock.lock();
        try {
            return price;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Геттер валютная пара
     * @return - Валютная пара
     */
    public CurrencyPair getCurrencyPair() {
        return currencyPair;
    }

    /**
     * Геттер тип ордера
     * @return ТипОрдера
     */
    public OrderType getType() {
        return type;
    }

    /**
     * Геттер айДи ордера
     * @return -айДиОрдера
     */
    public int getId() {
        return id;
    }

    /**
     * Геттер айДи клиента, создавшего ордер
     * @return - айДи Клиента
     */
    public int getClientId() {
        return clientId;
    }

    /**
     * Геттер первая валюта
     * @return - первая валюта
     */
    public BigDecimal getAmountFirst() {
        return amountFirst;
    }

    /**
     * Геттер вторая валюта
     * @return - вторая валюта
     */
    public BigDecimal getAmountSecond() {
        return amountSecond;
    }
}
