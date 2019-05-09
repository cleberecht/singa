package bio.singa.mathematics.geometry.faces;

import bio.singa.mathematics.vectors.Vector2D;

import java.util.ArrayList;
import java.util.List;

import static bio.singa.mathematics.metrics.model.VectorMetricProvider.EUCLIDEAN_METRIC;

/**
 * @author cl
 */
public class Circles {

    private Circles() {

    }

    /**
     * Returns the circumference of a circle with the given radius.
     *
     * @param radius The radius.
     * @return The area.
     */
    public static double circumference(double radius) {
        return 2.0 * radius * Math.PI;
    }

    /**
     * Returns the area of a circle with the given radius.
     *
     * @param radius The radius.
     * @return The area.
     */
    public static double area(double radius) {
        return radius * radius * Math.PI;
    }

    /**
     * Returns a random normally-distributed vector placed inside the circle.
     *
     * @param circle The circle.
     * @return A random vector.
     */
    public static Vector2D randomPoint(Circle circle) {
        // https://stackoverflow.com/a/50746409
        double r = circle.getRadius() * Math.sqrt(Math.random());
        double theta = Math.random() * 2 * Math.PI;
        double x = circle.getMidpoint().getX() + r * Math.cos(theta);
        double y = circle.getMidpoint().getY() + r * Math.sin(theta);
        return new Vector2D(x, y);
    }

    /**
     * Returns the angle between two vectors that are projected on the circumference of the circle.
     *
     * @param circle The circle.
     * @param first The first vector.
     * @param second The second vector.
     * @return The angle in radians.
     */
    public static double centralAngle(Circle circle, Vector2D first, Vector2D second) {
        // https://math.stackexchange.com/a/185844
        double rSq = circle.getRadius() * circle.getRadius() * 2.0;
        double c = EUCLIDEAN_METRIC.calculateDistance(first, second);
        return Math.acos((rSq - c * c) / rSq);
    }

    /**
     * Returns the length of the arc created by projecting the two vectors on the circumference of the circle.
     *
     * @param circle The circle.
     * @param first The first vector.
     * @param second The second vector.
     * @return The length of the arc.
     */
    public static double arcLength(Circle circle, Vector2D first, Vector2D second) {
        return Math.abs(Circles.centralAngle(circle, first, second) * circle.getRadius());
    }

    /**
     * Returns a list of evenly spaced points on the circumference of the given circle,
     *
     * @param circle The circle.
     * @param numberOfPoints The number of points.
     * @return A list of evenly spaced points on the circumference of the given circle,
     */
    public static List<Vector2D> samplePoints(Circle circle, int numberOfPoints) {
        double angle = (2.0 * Math.PI) / (double) numberOfPoints;
        double commulativeAngle = 0.0;
        List<Vector2D> points = new ArrayList<>();
        while (commulativeAngle < 2 * Math.PI) {
            double x = Math.cos(commulativeAngle) * circle.getRadius();
            double y = Math.sin(commulativeAngle) * circle.getRadius();
            points.add(new Vector2D(x + circle.getMidpoint().getX(), y + circle.getMidpoint().getY()));
            commulativeAngle += angle;
        }
        return points;
    }

}
