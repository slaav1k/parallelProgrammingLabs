package labs.rsreu;

/**
 * Собственная реализация блокировки через мониторы
 */
public class MyLock {
    private boolean locked = false;

    /**
     * Метод для блокировки
     * @throws InterruptedException если поток прерван
     */
    public synchronized void lock() throws InterruptedException {
        while (locked) {
            wait();
        }
        locked = true; // Установка флага блокировки
    }

    /**
     * Метод для не блокирующей попытки захвата блокировки
     * @return результат попытки
     */
    public synchronized boolean tryLock() {
        if (!locked) {
            locked = true; // Захват блокировки
            return true;
        }
        return false; // Блокировка не захвачена
    }

    /**
     * Метод для разблокировки
     */
    public synchronized void unlock() {
        if (!locked) {
            throw new IllegalStateException("Unlock attempted on a lock that is not locked");
        }
        locked = false; // Сброс флага блокировки
        notifyAll(); // Уведомляем все ожидающие потоки
    }

}
