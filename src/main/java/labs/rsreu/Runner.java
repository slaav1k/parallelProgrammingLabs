package labs.rsreu;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

public class Runner {
    private static final Map<Integer, Thread> tasks = new HashMap<Integer, Thread>();
    private static int taskCount = 0;

    public static void main(String[] args) throws InterruptedException {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Command line interface started. Available commands: start <n>, stop <n>, await <n>, exit");
        try {
            while (true) {

                String command = scanner.nextLine();
                String[] parts = command.split(" ");
                if (parts.length == 0) continue;

                switch (parts[0]) {
                    case "start":
                        if (parts.length != 2) {
                            System.out.println("Usage: start <n>");
                            break;
                        }
                        int intervals = Integer.parseInt(parts[1]);
                        startTask(intervals);
                        break;
                    case "stop":
                        if (parts.length != 2) {
                            System.out.println("Usage: stop <n>");
                            break;
                        }
                        stopTask(Integer.parseInt(parts[1]));
                        break;
                    case "await":
                        if (parts.length != 2) {
                            System.out.println("Usage: await <n>");
                            break;
                        }
                        awaitTask(Integer.parseInt(parts[1]));
                        break;
                    case "exit":
                        exitProgram();
                        return;
                    default:
                        System.out.println("Unknown command");
                }
            }
        }
        catch (InterruptedException e) {
            System.out.println("Interrupted");
        }

    }

    private static void startTask(int n) {
        int taskId = taskCount++;

        // Нижний предел
        double a = 0;

        // Верхний предел
        double b = 1;

        // Количество интервалов
//        n = 100_000_000;

        DefIntegralCalculator calculator = new DefIntegralCalculator(a, b, n);

        AtomicBoolean running = new AtomicBoolean(true);

        Runnable task = new Runnable() {
            @Override
            public void run() {
                try {


                    long startTime = System.nanoTime();
                    double result = calculator.calculate(running);

                    long endTime = System.nanoTime();

                    long duration = endTime - startTime;

                    double durationInSeconds = (double) duration / 1_000_000_000.0;

                    System.out.printf("Integral of sin(x) * x from " + a + " to " + b + " is: " + result + " Time taken: %.9f seconds%n", durationInSeconds); // вывод с точностью до наносекунд
//                    System.out.println("Task " + taskId + " finished!");
                } catch (Exception e) {
                    System.out.println("Task " + taskId + " encountered an error: " + e.getMessage());
                }
            }

        };

        Thread thread = new Thread(task);
        thread.setUncaughtExceptionHandler((t, e) -> {
            System.out.println("Task " + taskId + " encountered an error: " + e.getMessage());
        });
        tasks.put(taskId, thread);

        thread.start();

        System.out.println("Task " + taskId + " started");
    }

    private static void stopTask(int taskId) {
        Thread task = tasks.get(taskId);

        if (task != null) {
            task.interrupt();
            System.out.println("Task " + taskId + " stopped");
        }
         else {
            System.out.println("Task " + taskId + " not exist");
        }
    }

    private static void awaitTask(int taskId) throws InterruptedException {
        Thread task = tasks.get(taskId);
        if (task != null) {
            task.join();
            System.out.println("Task " + taskId + " finished");
        }
        else {
            System.out.println("Task " + taskId + " not exist");
        }
    }

    private static void exitProgram() {
        System.out.println("Exiting program");
        tasks.values().forEach(Thread::interrupt);
        tasks.clear();
    }

}