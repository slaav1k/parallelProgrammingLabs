import labs.rsreu.MyLock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class MyLockTest {

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testLockUnlock() throws InterruptedException {
        MyLock lock = new MyLock();
        lock.lock(); // Захватываем лок
        lock.unlock(); // Освобождаем лок
        assertTrue(lock.tryLock()); // Теперь мы можем захватить лок снова
        lock.unlock(); // Освобождаем лок снова
    }

    

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testDoubleUnlock() throws InterruptedException {
        MyLock lock = new MyLock();
        lock.lock(); // Захватываем лок
        lock.unlock(); // Освобождаем лок

        // Проверяем, что попытка разблокировать лок, который не заблокирован, выбрасывает исключение
        assertThrows(IllegalStateException.class, lock::unlock);
    }

    @Test
    void testTryLock() throws InterruptedException {
        MyLock lock = new MyLock();
        assertTrue(lock.tryLock()); // Лок должен быть доступен
        lock.unlock(); // Освобождаем лок

        lock.lock(); // Захватываем лок
        assertFalse(lock.tryLock()); // Второй захват не должен быть успешным
        lock.unlock(); // Освобождаем лок
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testMultithreadingLock() throws InterruptedException {
        MyLock lock = new MyLock();
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(2); // Два потока

        Runnable task = () -> {
            try {
                startLatch.await();
                lock.lock();
                Thread.sleep(100); // имитируем работу
                lock.unlock();
                finishLatch.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };

        new Thread(task).start();
        new Thread(task).start();

        startLatch.countDown(); // Начинаем оба потока
        finishLatch.await(); // Ждем завершения обоих потоков
    }
}
