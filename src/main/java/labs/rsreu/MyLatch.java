package labs.rsreu;

/**
 * Моя реализация защелки через мониторы
 */
public class MyLatch {
    private int count;

    public MyLatch(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Количество должно быть неотрицательным");
        }
        this.count = count;
    }

    /**
     * Уменьшает счетчик на 1. Если счетчик достигает 0, уведомляет ожидающие потоки.
     */
    public synchronized void countDown() {
        if (count > 0) {
            count--;
            if (count == 0) {
                notifyAll(); // Уведомляем ожидающие потоки, что защелка открыта
            }
        }
    }

    /**
     * Блокирует текущий поток до тех пор, пока счетчик не станет равным 0.
     */
    public synchronized void await() throws InterruptedException {
        while (count > 0) {
            wait(); // Ожидаем, пока счетчик не станет 0
        }
    }

    /**
     * Пытается блокировать текущий поток до тех пор, пока счетчик не станет равным 0,
     * но не блокирует, если он не может сразу продолжить выполнение.
     * @return успешен ли результат
     * true, если счетчик стал равным 0; false, если поток не был заблокирован
     */
    public synchronized boolean tryAwait() {
        if (count == 0) {
            return true; // Защелка уже открыта
        }

        try {
            wait(); // Ожидаем, пока счетчик не станет 0
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Восстанавливаем состояние прерывания
            return false; // Не удалось дождаться завершения
        }

        return count == 0; // Проверяем состояние счетчика
    }
}
