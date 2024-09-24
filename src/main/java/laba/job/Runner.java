package laba.job;

public class Runner {
    public static void main(String[] args) {
        // Нижний предел
        double a = 0;

        // Верхний предел
        double b = 1;

        // Количество интервалов (больше - точнее)
        int n = 100_000_000;

        DefIntegralCalculator calculator = new DefIntegralCalculator(a, b, n);

        Runnable task = new Runnable() {
            @Override
            public void run() {
                for (int i = 1; i <= 10; i++) {
                    long startTime = System.nanoTime();

                    double result = calculator.calculate();

                    long endTime = System.nanoTime();

                    long duration = endTime - startTime;

                    double durationInSeconds = (double) duration / 1_000_000_000.0;

                    System.out.println("Run #" + i + ": Integral of sin(x) * x from " + a + " to " + b + " is: " + result);
                    System.out.printf("Time taken: %.9f seconds%n", durationInSeconds); // вывод с точностью до наносекунд
                }
            }
        };


        Thread thread = new Thread(task);
        thread.start();

    }
}