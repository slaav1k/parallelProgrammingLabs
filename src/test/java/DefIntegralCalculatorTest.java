import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import laba.job.DefIntegralCalculator;
import org.junit.jupiter.api.function.Executable;

/**
 * Тестирование расчетов
 */
public class DefIntegralCalculatorTest {

    @RepeatedTest(10)
    public void testCalculateNTimes() {
        // Тест для стандартного случая, N повторений
        DefIntegralCalculator calculator = new DefIntegralCalculator(0, 1, 1000);
        double result = calculator.calculate();
        assertEquals(0.301, result, 0.01);
    }

    @Test
    public void testCalculateStandard() {
        // Тест для стандартного случая, интеграл от 0 до 1 с 1000 интервалами
        DefIntegralCalculator calculator = new DefIntegralCalculator(0, 1, 1000);
        double result = calculator.calculate();
        assertEquals(0.301268, result, 0.00001);
    }

    @Test
    public void testCalculateZeroInterval() {
        // Тест, когда нижний и верхний предел равны
        DefIntegralCalculator calculator = new DefIntegralCalculator(1, 1, 1000);
        double result = calculator.calculate();
        assertEquals(0.0, result, 0.000001);
    }


    @Test
    public void testCalculateLargeN() {
        // Тест с большим количеством интервалов разбиения
        DefIntegralCalculator calculator = new DefIntegralCalculator(0, 1, 10_000);
        double result = calculator.calculate();
        assertEquals(0.301268, result, 0.000001);
    }

    @Test
    public void testNegativeIntervals() {
        // Тест на случай отрицательного количества интервалов
        Executable executable = () -> new DefIntegralCalculator(0, 1, -100).calculate();
        assertThrows(IllegalArgumentException.class, executable);
    }
}
