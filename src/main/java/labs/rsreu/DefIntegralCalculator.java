package labs.rsreu;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Класс для нахождения определенного интеграла x * sin(x) (0, 1)
 */
public class DefIntegralCalculator {
    private double a;
    private double b;
    private int n;
    private AtomicInteger progress;

    /**
     * Конструктор класса, задающий пределы интегрирования и количество интервалов.
     * @param a - нижний предел интегрирования
     * @param b - верхний предел интегрирования
     * @param n - количество интервалов разбиения
     * @param progress - переменная для отслеживания прогресса
     * @throws IllegalArgumentException если количество интервалов <= 0 или если a > b
     */
    public DefIntegralCalculator(double a, double b, int n, AtomicInteger progress) {
        if (n <= 0) {
            throw new IllegalArgumentException("Количество интервалов должно быть положительным");
        }
        if (a > b) {
            throw new IllegalArgumentException("Нижний предел интегрирования не может быть больше верхнего");
        }

        this.a = a;
        this.b = b;
        this.n = n;
        this.progress = progress;
    }

    /**
     * Основной метод для вычисления интеграла, использующий поля объекта.
     * @return значение интеграла
     */
    public double calculate() {
        double h = (this.b - this.a) / this.n;
        double sum = 0.0;

        for (int i = 0; i < this.n; i++) {
            if (Thread.currentThread().isInterrupted()) {
                return sum;
            }
            double x1 = this.a + i * h;
            double x2 = this.a + (i + 1) * h;
            sum += 0.5 * h * (f(x1) + f(x2));

            // Увеличиваем прогресс
            progress.incrementAndGet();
        }

        return sum;
    }

    /**
     * Функция под интегралом
     * @param x - значение переменной х
     * @return - значение функции
     */
    private double f(double x) {
        return Math.sin(x) * x;
    }
}
