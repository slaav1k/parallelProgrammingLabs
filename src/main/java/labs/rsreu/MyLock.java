package labs.rsreu;

/**
 * Собственная реализация блокировки через мониторы
 */
public class MyLock {
    // Флаг для отслеживания состояния блокировки
    private boolean isLocked = false;

    // Метод для блокировки
    public synchronized void lock() throws InterruptedException {
        while (isLocked) {
            wait(); // Ожидание, пока блокировка не станет доступной
        }
        isLocked = true; // Установка флага блокировки
    }

    // Метод для не блокирующей попытки захвата блокировки
    public synchronized boolean tryLock() {
        if (!isLocked) {
            isLocked = true; // Захват блокировки
            return true;
        }
        return false; // Блокировка не захвачена
    }

    // Метод для разблокировки
    public synchronized void unlock() {
        if (!isLocked) {
            throw new IllegalStateException("Unlock attempted on a lock that is not locked");
        }
        isLocked = false; // Сброс флага блокировки
        notifyAll(); // Уведомляем ожидающие потоки
    }
}
