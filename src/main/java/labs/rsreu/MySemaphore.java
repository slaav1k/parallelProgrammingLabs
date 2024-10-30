package labs.rsreu;

/**
 * Собственная реализация семафора через мониторы
 */
public class MySemaphore {
    private int permits;

    /**
     * Конструктор, инициализирующий семафор с заданным количеством разрешений.
     * @param permits - начальное количество разрешений
     */
    public MySemaphore(int permits) {
        if (permits < 0) {
            throw new IllegalArgumentException("Количество разрешений должно быть неотрицательным");
        }
        this.permits = permits;
    }

    /**
     * Метод для получения разрешения. Если разрешений недостаточно, поток блокируется.
     * @throws InterruptedException если поток прерван
     */
    public synchronized void acquire() throws InterruptedException {
        while (permits == 0) {
            wait();
        }
        permits--;
    }

    /**
     *  Метод для попытки захватить семафор без блокировки.
     *  Если семафор доступен, метод уменьшает количество разрешений
     * @return - успешен ли результат
     */
    public synchronized boolean tryAcquire() {
        if (permits > 0) {
            permits--;
            return true;
        }
        return false;
    }

    /**
     * Метод для освобождения разрешения. Если есть ожидающие потоки, они будут разблокированы.
     */
    public synchronized void release() {
        permits++;
        notify();
    }
}
