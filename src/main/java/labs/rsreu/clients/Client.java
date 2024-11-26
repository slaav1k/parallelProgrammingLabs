package labs.rsreu.clients;

import labs.rsreu.currencies.Currency;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Клиент
 */
public class Client {
    // Идентификатор
    private final int id;
    // Баланс валют
    private final EnumMap<Currency, BigDecimal> balance;
    private final Lock balanceLock = new ReentrantLock();
    private static int idCounter = 1;



    /**
     * Конструктор
     * @param balance - начальный баланс
     */
    public Client(EnumMap<Currency, BigDecimal> balance) {
        this.id = idCounter++;
        this.balance = balance;
    }

    /**
     * Конструктор копии
     * @param other - объект Клиент
     */
    public Client(Client other) {
        this.id = other.id;
        this.balance = other.balance;
    }

    /**
     * Получить баланс конкретной валюты
     *
     * @param currency - Валюта
     * @return - Баланс валюты
     */
    public BigDecimal getBalance(Currency currency) {
        balanceLock.lock();
        try {
            return balance.getOrDefault(currency, BigDecimal.ZERO);
        } finally {
            balanceLock.unlock();
        }
    }


    /**
     * Обновление баланса клиента (пополнение или снятие)
     * @param currency - Валюта
     * @param amount - Сумма
     * @param isDeposit - true, если это пополнение; false, если снятие
     */
    public void updateBalance(Currency currency, BigDecimal amount, boolean isDeposit) {
        balanceLock.lock();
        try {
//            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
//                throw new IllegalArgumentException("Amount must be positive");
//            }

            BigDecimal currentBalance = balance.getOrDefault(currency, BigDecimal.ZERO);
            BigDecimal newBalance;

//            if (isDeposit) {
//                newBalance = currentBalance.add(amount);
//                balance.put(currency, newBalance);
//            } else {
//                if (currentBalance.compareTo(amount) >= 0) {
//                    newBalance = currentBalance.subtract(amount);
//                    balance.put(currency, newBalance);
//                } else {
//                    throw new IllegalArgumentException("Insufficient funds or invalid amount");
//                }
//            }

            if (isDeposit) {
                newBalance = currentBalance.add(amount);
            } else {
                newBalance = currentBalance.subtract(amount);
            }

            // Обновляем баланс
            balance.put(currency, newBalance);

            // Логирование
            String sb = "|----------UPDATE-BALANCE------------|\n" +
                    "Client ID: " + id + "\n" +
                    "Currency: " + currency + "\n" +
                    "Old balance: " + currentBalance + "\n" +
                    "New balance: " + newBalance + "\n" +
                    "|------------------------------------|";

            System.out.println(sb);
        } finally {
            balanceLock.unlock();
        }
    }

    /**
     * ID клиента
     * @return - АйДи
     */
    public int getId() {
        return id;
    }

    /**
     * Баланс клиента
     * @return - Баланс (копия)
     */
    public EnumMap<Currency, BigDecimal> getAllBalances() {
        balanceLock.lock();
        try {
            return new EnumMap<>(balance);
        } finally {
            balanceLock.unlock();
        }
    }


    /**
     * Переопределенный туСтринг
     * @return - ТуСтринг
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Client ID: " + id + "\nBalance:\n");
        for (Currency currency : Currency.values()) {
            sb.append(currency.name()).append(": ").append(balance.getOrDefault(currency, BigDecimal.ZERO)).append("\n");
        }
        return sb.toString();
    }
}
