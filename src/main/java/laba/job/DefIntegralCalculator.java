package laba.job;

/**
 * Класс для нахождения определенного интеграла x * sin(x) (0, 1)
 */
public class DefIntegralCalculator {
    private double a;
    private double b;
    private int n;

    /**
     * Конструктор класса, задающий пределы интегрирования и количество интервалов.
     * @param a - нижний предел интегрирования
     * @param b - верхний предел интегрирования
     * @param n - количество интервалов разбиения
     * @throws IllegalArgumentException если количество интервалов <= 0 или если a > b
     */
    public DefIntegralCalculator(double a, double b, int n) {
        if (n <= 0) {
            throw new IllegalArgumentException("Количество интервалов должно быть положительным");
        }
        if (a > b) {
            throw new IllegalArgumentException("Нижний предел интегрирования не может быть больше верхнего");
        }

        this.a = a;
        this.b = b;
        this.n = n;
    }

    /**
     * Основной метод для вычисления интеграла, использующий поля объекта.
     * @return значение интеграла
     */
    public double calculate() {
        // Ширина интервала
        double h = (this.b - this.a) / this.n;

        // Сумма площадей трапеций
        double sum = 0.0;

        for (int i = 0; i < this.n; i++) {
            double x1 = this.a + i * h;
            double x2 = this.a + (i + 1) * h;
            sum += 0.5 * h * (f(x1) + f(x2));
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
