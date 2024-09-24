package laba.maven;

public class AreaCircleCalculator {
    public static double calculate(double radius) {
        if (radius < 0) {
            throw new IllegalArgumentException("Radius cannot be negative");
        }
        return Math.PI * radius * radius;
    }
}
