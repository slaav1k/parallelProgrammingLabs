import laba.maven.AreaCircleCalculator;
import org.junit.jupiter.api.RepeatedTest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.function.Executable;



public class AreaCircleCalculatorTest {
    @RepeatedTest(50)
    public void testRadius5() {
        double result = AreaCircleCalculator.calculate(5);
        assertEquals(78.54, result, 0.01);
    }

    @RepeatedTest(50)
    public void testRadius10() {
        double result = AreaCircleCalculator.calculate(10);
        assertEquals(314.16, result, 0.01);
    }

    @RepeatedTest(50)
    public void testRadius0() {
        double result = AreaCircleCalculator.calculate(0);
        assertEquals(0.00, result, 0.01);
    }

    @RepeatedTest(50)
    public void testRadiusNegative() {
        Executable executable = () -> AreaCircleCalculator.calculate(-1);
        assertThrows(IllegalArgumentException.class, executable, "Radius cannot be negative");
    }
}
