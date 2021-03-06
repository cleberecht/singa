package bio.singa.mathematics.vectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Vector2DTest {

    private static Vector2D first;
    private static Vector2D second;
    private static double scalar;

    @BeforeAll
    static void initialize() {
        first = new Vector2D(10.0, 20.0);
        second = new Vector2D(15.0, 25.0);
        scalar = 2.0;
    }

    @Test
    void testAddCalculation() {
        Vector2D actual = first.add(second);
        assertArrayEquals(new double[]{25.0, 45.0}, actual.getElements());
    }

    @Test
    void testSubtractCalculation() {
        Vector2D actual = first.subtract(second);
        assertArrayEquals(new double[]{-5.0, -5.0}, actual.getElements());
    }

    @Test
    void testMultiplicationCalculation() {
        Vector2D actual = first.multiply(second);
        assertArrayEquals(new double[]{150.0, 500.0}, actual.getElements());
    }

    @Test
    void testMultiplicationWithScalarCalculation() {
        Vector2D actual = first.multiply(scalar);
        assertArrayEquals(new double[]{20.0, 40.0}, actual.getElements());
    }

    @Test
    void testAdditivelyInvertCalculation() {
        Vector2D actual = first.additivelyInvert();
        assertArrayEquals(new double[]{-10.0, -20.0}, actual.getElements());
    }

    @Test
    void testAdditivelyInvertElementCalculation() {
        Vector2D actual = first.invertX().invertY();
        assertArrayEquals(new double[]{-10, -20.0}, actual.getElements());
    }

    @Test
    void testMagnitudeCalculation() {
        double actual = first.getMagnitude();
        assertEquals(10.0 * Math.sqrt(5), actual);
    }

    @Test
    void testDivisionWithScalarCalculation() {
        Vector2D actual = first.divide(scalar);
        assertArrayEquals(new double[]{5.0, 10.0}, actual.getElements());
    }

    @Test
    void testDivisionCalculation() {
        Vector2D actual = first.divide(second);
        assertArrayEquals(new double[]{2.0 / 3.0, 0.8}, actual.getElements());
    }

    @Test
    void testDotProductCalculation() {
        double actual = first.dotProduct(second);
        assertEquals(650.0, actual);
    }

    @Test
    void testAngleCalculation() {
        double actual = first.angleTo(second);
        assertEquals(0.07677189126977, actual, 1e-10);
    }

    @Test
    void testMidpointCalculation() {
        Vector2D actual = first.getMidpointTo(second);
        assertArrayEquals(new double[]{12.5, 22.5}, actual.getElements());
    }

    @Test
    void shouldBeNearEachOther() {
        boolean actualTrue = first.isNearVector(second, 3.0);
        boolean actualFalse = first.isNearVector(second, 2.0);
        assertTrue(actualTrue);
        assertFalse(actualFalse);
    }

    @Test
    void testNormalizationCalculation() {
        Vector2D actual = first.normalize();
        assertArrayEquals(new double[]{1 / Math.sqrt(5.0), 2 / Math.sqrt(5)}, actual.getElements(), 1e-15);
        assertEquals(1.0, actual.getMagnitude(), 1e-15);
    }

}
