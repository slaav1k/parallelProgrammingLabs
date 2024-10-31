import labs.rsreu.MyLatch;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class MyLatchTest {

    @RepeatedTest(5)
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testSingleAwait() throws InterruptedException {
        MyLatch latch = new MyLatch(1);

        // Создаем поток, который вызовет await
        Thread thread = new Thread(() -> {
            try {
                latch.await(); // Ожидание завершения
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        thread.start();
        // Убедимся, что поток все еще жив
        assertTrue(thread.isAlive());

        // Уменьшаем счетчик
        latch.countDown();

        // Ждем завершения потока
        thread.join();
        assertFalse(thread.isAlive());
    }

    @Test
    void testCountDownWithoutAwait() {
        MyLatch latch = new MyLatch(1);
        latch.countDown();
        assertThrows(IllegalStateException.class, latch::countDown); // Должно выбросить исключение
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testMultipleCountDown() throws InterruptedException {
        MyLatch latch = new MyLatch(2); // Устанавливаем лимит на 2

        CountDownLatch finishLatch = new CountDownLatch(2);

        Runnable task = () -> {
            try {
                latch.await(); // Ожидание завершения
                finishLatch.countDown(); // Уменьшаем счетчик завершения
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };

        new Thread(task).start();
        new Thread(task).start();

        // Уменьшаем счетчик
        latch.countDown();
        latch.countDown();

        // Ждем завершения всех потоков
        assertTrue(finishLatch.await(1, TimeUnit.SECONDS));
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testMultithreading() throws InterruptedException {
        MyLatch latch = new MyLatch(3); // Устанавливаем лимит на 3
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(5); // 5 потоков

        Runnable task = () -> {
            try {
                startLatch.await();
                latch.await(); // Ожидание завершения
                finishLatch.countDown(); // Уменьшаем счетчик завершения
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };

        for (int i = 0; i < 5; i++) {
            new Thread(task).start();
        }

        startLatch.countDown(); // Начинаем все потоки

        // Уменьшаем счетчик 3 раза, чтобы все потоки могли завершиться
        latch.countDown();
        latch.countDown();
        latch.countDown();

        finishLatch.await(); // Ждем завершения всех потоков
    }
}
