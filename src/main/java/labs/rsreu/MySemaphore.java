package labs.rsreu;

public class MySemaphore {
    private int permits;
    private int acquiredCount = 0; // Счетчик активных захватов

    public MySemaphore(int permits) {
        if (permits < 0) {
            throw new IllegalArgumentException("Количество разрешений должно быть неотрицательным");
        }
        this.permits = permits;
    }

    public synchronized void acquire() throws InterruptedException {
        while (permits <= 0) { // Изменено на <= 0
            wait();
        }
        permits--;
        acquiredCount++; // Увеличиваем счетчик захватов
    }

    public synchronized void release() {
        if (acquiredCount <= 0) { // Проверяем, были ли захваты
            throw new IllegalStateException("Попытка освободить семафор, который не заблокирован.");
        }
        permits++;
        acquiredCount--; // Уменьшаем счетчик захватов
        notify();
    }

    public synchronized boolean tryAcquire() {
        if (permits > 0) {
            permits--;
            acquiredCount++;
            return true;
        }
        return false;
    }
}
