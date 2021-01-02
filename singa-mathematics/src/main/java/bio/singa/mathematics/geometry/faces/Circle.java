package bio.singa.mathematics.geometry.faces;

import bio.singa.mathematics.geometry.edges.Line;
import bio.singa.mathematics.geometry.edges.SimpleLineSegment;
import bio.singa.mathematics.vectors.Vector2D;

import java.util.Set;
import java.util.TreeSet;

import static bio.singa.mathematics.metrics.model.VectorMetricProvider.EUCLIDEAN_METRIC;

public class Circle {

    private Vector2D midpoint;
    private double radius;

    public Circle(Vector2D midpoint, double radius) {
        this.midpoint = midpoint;
        this.radius = radius;
    }

    /**
     * Circumscribed circle from 3 points (e.g. triangle)
     *
     * @param firstPoint The first point of the triangle.
     * @param secondPoint The second point of the triangle.
     * @param thirdPoint The third point of the triangle.
     */
    public Circle(Vector2D firstPoint, Vector2D secondPoint, Vector2D thirdPoint) {

        SimpleLineSegment abLine = new SimpleLineSegment(firstPoint, secondPoint);
        Line abBisect = abLine.getPerpendicularBisector();

        SimpleLineSegment acLine = new SimpleLineSegment(firstPoint, thirdPoint);
        Line acBisect = acLine.getPerpendicularBisector();

        midpoint = abBisect.getIntersectionWith(acBisect)
                .orElseThrow(() -> new IllegalStateException("Unable to determine intersections."));;

        radius = midpoint.distanceTo(firstPoint);

    }

    public Vector2D getMidpoint() {
        return midpoint;
    }

    public void setMidpoint(Vector2D midpoint) {
        this.midpoint = midpoint;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }



    /**
     * Returns the angle between two vectors that are projected on the circle circumference.
     *
     * @param first The first vector.
     * @param second The second vector.
     * @return The angle in radians.
     */
    public double getCentralAngleBetween(Vector2D first, Vector2D second) {
        // https://math.stackexchange.com/questions/185829/how-do-you-find-an-angle-between-two-points-on-the-edge-of-a-circle
        double rSq = radius * radius * 2.0;
        double c = EUCLIDEAN_METRIC.calculateDistance(first, second);
        // in rad for angle multiply by (180/Math.PI)
        return Math.acos((rSq - c * c)/rSq);
    }

    public double getArcLengthBetween(Vector2D first, Vector2D second) {
        return Math.abs(getCentralAngleBetween(first, second) * radius);
    }

    public Set<Double> getXValue(double yValue) {
        Set<Double> values = new TreeSet<>();
        double sqrt = Math.sqrt(-Math.pow(midpoint.getY(), 2) + 2 * midpoint.getY() * yValue + Math.pow(radius, 2) - Math.pow(yValue, 2));
        values.add(midpoint.getX() + sqrt);
        values.add(midpoint.getX() - sqrt);
        return values;
    }

    public Set<Double> getYValue(double xValue) {
        Set<Double> values = new TreeSet<>();
        double sqrt = Math.sqrt(-Math.pow(midpoint.getX(), 2) + 2 * midpoint.getX() * xValue + Math.pow(radius, 2) - Math.pow(xValue, 2));
        values.add(midpoint.getY() + sqrt);
        values.add(midpoint.getY() - sqrt);
        return values;
    }

    public double getCircumference() {
        return Circles.circumference(radius);
    }

    public double getArea() {
        return Circles.area(radius);
    }

}
