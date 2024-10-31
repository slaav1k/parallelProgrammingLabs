import labs.rsreu.MySemaphore;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class MySemaphoreTest {

    @RepeatedTest(5)
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testSingleAcquire() throws InterruptedException {
        MySemaphore semaphore = new MySemaphore(1);
        semaphore.acquire();
        // Здесь мы проверяем, что захват второго разрешения не удался
        assertFalse(semaphore.tryAcquire()); // Ожидаем, что tryAcquire() вернет false

        semaphore.release(); // Освобождаем разрешение
        assertTrue(semaphore.tryAcquire()); // Теперь tryAcquire() должен вернуть true
        semaphore.release(); // Освобождаем разрешение снова
    }

    @RepeatedTest(5)
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testDoubleAcquire() throws InterruptedException {
        MySemaphore semaphore = new MySemaphore(1);
        CountDownLatch acquireLatch = new CountDownLatch(1);
        CountDownLatch releaseLatch = new CountDownLatch(1);

        Thread thread1 = new Thread(() -> {
            try {
                semaphore.acquire();
                acquireLatch.countDown();
                acquireLatch.await();
                semaphore.acquire(); // второй вызов acquire
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        thread1.start();
        acquireLatch.countDown();

        Thread thread2 = new Thread(() -> {
            try {
                acquireLatch.await();
                semaphore.release();
                releaseLatch.countDown();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        thread2.start();

        assertTrue(releaseLatch.await(1, TimeUnit.SECONDS));

        thread1.join();
        thread2.join();
    }

    @Test
    void testReleaseWithoutAcquire() {
        MySemaphore semaphore = new MySemaphore(1);
        assertThrows(IllegalStateException.class, semaphore::release);
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testDoubleRelease() throws InterruptedException {
        MySemaphore semaphore = new MySemaphore(1); // Установим лимит на 1
        semaphore.acquire(); // Захватываем семафор

        // Освобождаем семафор первый раз
        semaphore.release();

        // Проверяем, что двойное освобождение выбрасывает исключение
        assertThrows(IllegalStateException.class, () -> {
            semaphore.release(); // Пытаемся освободить семафор второй раз
        });
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testMultithreading() throws InterruptedException {
        MySemaphore semaphore = new MySemaphore(3); // Установим лимит на 3
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(5); // 5 потоков

        Runnable task = () -> {
            try {
                startLatch.await();
                semaphore.acquire();
                Thread.sleep(100); // имитируем работу
                semaphore.release();
                finishLatch.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };

        for (int i = 0; i < 5; i++) {
            new Thread(task).start();
        }

        startLatch.countDown(); // Начинаем все потоки
        finishLatch.await(); // Ждем завершения всех потоков
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testTimeoutAcquire() throws InterruptedException {
        MySemaphore semaphore = new MySemaphore(1);
        semaphore.acquire();

        // Создаем поток, который попытается захватить семафор и должен прерваться
        Thread thread = new Thread(() -> {
            try {
                semaphore.acquire();
            } catch (InterruptedException e) {
                // Ожидаем прерывание
            }
        });

        thread.start();
        thread.join(1000); // Ждем 1 секунду

        // Убедимся, что поток завершился
        assertTrue(thread.isAlive()); // поток все еще должен быть жив

        semaphore.release(); // Освобождаем семафор
        thread.join(); // Ждем завершения потока
    }
}
