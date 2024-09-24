package laba.maven;


import java.util.Locale;
import java.util.Scanner;

public class Runner {
    public static void main(String[] args) {

        System.out.println("Hello! Input radius of circle:");

        Scanner scanner = new Scanner(System.in).useLocale(Locale.US);

        double radius = scanner.nextDouble();

        double area = AreaCircleCalculator.calculate(radius);

        System.out.printf("Area of the circle = %.2f%n", area);

    }
}