package bio.singa.mathematics.geometry.edges;

import bio.singa.mathematics.metrics.model.VectorMetricProvider;
import bio.singa.mathematics.vectors.Vector2D;

/**
 * A line is a straight one-dimensional figure having no thickness and extending
 * infinitely in both directions. It is uniquely defined by two distinct points
 * or a single point and its slope.
 *
 * @author cl
 * @see <a href="https://en.wikipedia.org/wiki/Line_(geometry)">Wikipedia: Line (geometry)</a>
 */
public class Line {

    protected final double yIntercept;
    protected final double slope;

    /**
     * Creates a new line from it's y-intercept and slope.
     *
     * @param yIntercept The y-intercept.
     * @param slope The slope.
     */
    public Line(double yIntercept, double slope) {
        if (Double.isNaN(yIntercept)) {
            throw new IllegalArgumentException("Unable to create a new line with Double.NaN as intercept.");
        } else if (Double.isNaN(slope)) {
            throw new IllegalArgumentException("Unable to create a new line with Double.NaN as slope.");
        }
        this.yIntercept = yIntercept;
        this.slope = slope;
    }

    /**
     * Creates a new Point from a strut point and the slope;
     *
     * @param strutPoint The strut point.
     * @param slope The slope.
     */
    public Line(Vector2D strutPoint, double slope) {
        if (!Double.isInfinite(slope)) {
            // classical line
            yIntercept = calculateYIntercept(strutPoint, slope);
            this.slope = slope;
        } else {
            // vertical line
            // here y-intercept is used to store x-intercept since there is no y intercept
            yIntercept = strutPoint.getX();
            this.slope = slope;
        }
    }

    /**
     * Creates a new Line from two points p1 = (x1, y1) and p2 = (x2, y2).
     *
     * @param x1 The x-value of the first point.
     * @param y1 The y-value of the first point.
     * @param x2 The x-value of the second point.
     * @param y2 The y-value of the second point.
     */
    public Line(double x1, double y1, double x2, double y2) {
        this(new Vector2D(x1, y1), new Vector2D(x2, y2));
    }

    /**
     * Creates a new Line from two points.
     *
     * @param first The first point.
     * @param second The second point.
     */
    public Line(Vector2D first, Vector2D second) {
        if (first.equals(second)) {
            // impossible to create line from two identical points
            throw new IllegalArgumentException("Unable to create line from two identical points: " + first + " and "
                    + second + ".");
        } else if (first.getX() == second.getX()) {
            // vertical line
            // here y-intercept is used to store x-intercept since there is no y intercept
            slope = Double.POSITIVE_INFINITY;
            yIntercept = first.getX();
        } else {
            // classical line
            slope = calculateSlope(first, second);
            yIntercept = calculateYIntercept(first, slope);
        }
    }

    /**
     * Gets the slope or gradient of the line, calculated by
     * <pre>
     * m = (y2 - y1)/(x2 - x1).</pre>
     *
     * @param first The first point of the line.
     * @param second The second point of the line.
     * @return The slope.
     */
    public static double calculateSlope(Vector2D first, Vector2D second) {
        if (first.getX() == second.getX()) {
            return Double.POSITIVE_INFINITY;
        }
        return (second.getY() - first.getY()) / (second.getX() - first.getX());
    }

    /**
     * Gets the y-intercept of the line, calculated by
     * <pre>
     * b = (y1 - x1) * slope</pre>
     *
     * @param first The first point of the line.
     * @param slope The slope of the line.
     * @return The y-intercept of the equation of the line segment.
     */
    public static double calculateYIntercept(Vector2D first, double slope) {
        return first.getY() - first.getX() * slope;
    }

    public double getSlope() {
        return slope;
    }

    /**
     * Gets the x-intercept of the line.<br>
     * If the line is horizontal the result will be {@link Double#NEGATIVE_INFINITY}.
     *
     * @return The x-intercept of the equation of the line segment.
     */
    public double getXIntercept() {
        if (isVertical()) {
            return yIntercept;
        } else {
            return -getYIntercept() / getSlope();
        }
    }

    /**
     * Gets the x-value in respect to a given y-value.
     *
     * @param y The y-value.
     * @return The x-value.
     */
    public double getXValue(double y) {
        if (isVertical()) {
            return yIntercept;
        } else {
            return (y - getYIntercept()) / getSlope();
        }

    }

    /**
     * Gets the y-intercept of the equation that is defined by the two points of
     * the line segment.
     *
     * @return The y-intercept of the equation of the line segment.
     */
    public double getYIntercept() {
        if (isVertical()) {
            return Double.NaN;
        } else {
            return yIntercept;
        }
    }

    /**
     * Gets the y-value in respect to a given x-value.
     *
     * @param x The x-value.
     * @return The y-value.
     */
    public double getYValue(double x) {
        return slope * x + yIntercept;
    }


    /**
     * Returns the angle to the x-axis in radians.
     *
     * @return he angle to the x-axis in radians.
     */
    public double getAngleToXAxis() {
        return Math.atan(slope / 1);
    }

    public double getAngleTo(Line line) {
        return Math.abs(Math.atan(slope) - Math.atan(line.slope));
    }

    /**
     * Returns the perpendicular slope.
     * <pre>
     * s* = -1 / slope</pre>
     *
     * @return the perpendiculat slope
     */
    public double getPerpendicularSlope() {
        return -1 / slope;
    }

    /**
     * Returns a new line parallel to this line, separated by the given
     * ({@link VectorMetricProvider#EUCLIDEAN_METRIC Euclidean}-)distance.
     * Negative distances return lines below, respectively left of this line and positive distances vice versa.
     *
     * @param distance The offset distance of the new parallel line.
     * @return A new line parallel to this one.
     */
    public Line getParallel(double distance) {
        if (isHorizontal()) {
            return new Line(yIntercept + distance, slope);
        } else if (isVertical()) {
            return new Line(new Vector2D(yIntercept + distance, 0), Double.POSITIVE_INFINITY);
        } else {
            return new Line(yIntercept + distance * Math.sqrt(1 + slope * slope), slope);
        }
    }

    /**
     * Returns a point where the two lines intersect, if they are not parallel.
     *
     * @param line Another line.
     * @return The intersection.
     */
    public Vector2D getIntersectWithLine(Line line) {
        final double a = slope;
        final double b = line.getSlope();
        final double c = getYIntercept();
        final double d = line.getYIntercept();
        return new Vector2D((d - c) / (a - b), (a * d - b * c) / (a - b));
    }

    /**
     * Returns the mirror image of the given point, using this line as the mirror axis.
     *
     * @param originalVector The original point.
     * @return The mirrored point.
     */
    public Vector2D mirrorVector(Vector2D originalVector) {
        double d = (originalVector.getX() + (originalVector.getY() - getYIntercept()) * getSlope())
                / (1 + getSlope() * getSlope());
        return new Vector2D(2 * d - originalVector.getX(), 2 * d * getSlope() - originalVector.getY() + 2 * yIntercept);
    }

    /**
     * Returns {@code true}, if this line is horizontal (i.e. it's slope is zero).
     *
     * @return {@code true}, if this line is horizontal
     */
    public boolean isHorizontal() {
        return slope == 0.0;
    }

    /**
     * Returns {@code true}, if this line is vertical (i.e. it's slope is infinite).
     *
     * @return {@code true}, if this line is vertical
     */
    public boolean isVertical() {
        return Double.isInfinite(slope);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(slope);
        result = prime * result + (int) (temp ^ temp >>> 32);
        temp = Double.doubleToLongBits(yIntercept);
        result = prime * result + (int) (temp ^ temp >>> 32);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Line other = (Line) obj;
        if (Double.doubleToLongBits(slope) != Double.doubleToLongBits(other.slope)) {
            return false;
        }
        return Double.doubleToLongBits(yIntercept) == Double.doubleToLongBits(other.yIntercept);
    }

    @Override
    public String toString() {
        return "Line [yIntercept=" + getYIntercept() + ", slope=" + slope + "]";
    }

}
