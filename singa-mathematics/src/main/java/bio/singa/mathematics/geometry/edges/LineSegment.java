package bio.singa.mathematics.geometry.edges;

import bio.singa.mathematics.geometry.faces.Circle;
import bio.singa.mathematics.metrics.model.VectorMetricProvider;
import bio.singa.mathematics.vectors.Vector2D;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import static bio.singa.mathematics.geometry.faces.Polygons.*;

/**
 * @author cl
 */
public interface LineSegment {

    /**
     * Returns the starting point.
     *
     * @return The starting point.
     */
    Vector2D getStartingPoint();

    /**
     * Sets the point where the line starts.
     *
     * @param startingPoint The point where the line starts.
     */
    void setStartingPoint(Vector2D startingPoint);

    /**
     * Returns the ending point.
     *
     * @return The ending point.
     */
    Vector2D getEndingPoint();

    /**
     * Sets the point where the line ends.
     *
     * @param endingPoint The point where the line ends.
     */
    void setEndingPoint(Vector2D endingPoint);

    default double getLength() {
        return getStartingPoint().distanceTo(getEndingPoint());
    }

    default boolean isOnLine(Vector2D vector) {
        return Math.min(getStartingPoint().getX(), getEndingPoint().getX()) <= vector.getX()
                && Math.max(getStartingPoint().getX(), getEndingPoint().getX()) >= vector.getX()
                && Math.min(getStartingPoint().getY(), getEndingPoint().getY()) <= vector.getY()
                && Math.max(getStartingPoint().getY(), getEndingPoint().getY()) >= vector.getY();
    }

    default boolean isAboutOnLine(Vector2D vector) {
        double crossproduct = (vector.getY() - getStartingPoint().getY()) * (getEndingPoint().getX() - getStartingPoint().getX())
                - (vector.getX() - getStartingPoint().getX()) * (getEndingPoint().getY() - getStartingPoint().getY());

        if (Math.abs(crossproduct) > 1e-9) {
            return false;
        }

        double dotproduct = (vector.getX() - getStartingPoint().getX()) * (getEndingPoint().getX() - getStartingPoint().getX())
                + (vector.getY() - getStartingPoint().getY()) * (getEndingPoint().getY() - getStartingPoint().getY());

        if (dotproduct < 0) {
            return false;
        }

        double squaredlengthba = (getEndingPoint().getX() - getStartingPoint().getX()) * (getEndingPoint().getX() - getStartingPoint().getX())
                + (getEndingPoint().getY() - getStartingPoint().getY()) * (getEndingPoint().getY() - getStartingPoint().getY());
        if (dotproduct > squaredlengthba) {
            return false;
        }

        return true;
    }

    default boolean isHorizontal() {
        return getStartingPoint().getX() == getEndingPoint().getX();
    }

    default boolean isVertical() {
        return getStartingPoint().getY() == getEndingPoint().getY();
    }

    default boolean isCongruentTo(LineSegment otherSegment) {
        return (getStartingPoint().equals(otherSegment.getStartingPoint()) && getEndingPoint().equals(otherSegment.getEndingPoint())) ||
                (getStartingPoint().equals(otherSegment.getEndingPoint()) && getEndingPoint().equals(otherSegment.getStartingPoint()));
    }

    default Vector2D getUnitVector() {
        return getEndingPoint().subtract(getStartingPoint()).normalize();
    }


    default Set<Vector2D> getIntersectionWith(Circle circle) {
        Set<Vector2D> intersections = new HashSet<>();
        // see http://mathworld.wolfram.com/Circle-LineIntersection.html
        // transform line points, such that circle is at origin to origin
        Vector2D end = getEndingPoint().subtract(circle.getMidpoint());
        Vector2D start = getStartingPoint().subtract(circle.getMidpoint());
        double dx = end.getX() - start.getX();
        double dy = end.getY() - start.getY();
        double drSquared = Math.pow(dx, 2) + Math.pow(dy, 2);
        double d = start.getX() * end.getY() - end.getX() * start.getY();
        double discriminant = Math.pow(circle.getRadius(), 2) * drSquared - Math.pow(d, 2);
        if (discriminant >= 0) {
            double discriminantRt = Math.sqrt(discriminant);
            // two intersections
            // x positions
            double termX1 = d * dy;
            double termX2 = Math.copySign(1.0, dy) * dx * discriminantRt;
            double xPlus = (termX1 + termX2) / drSquared;
            double xMinus = (termX1 - termX2) / drSquared;
            // y positions
            double termY1 = -d * dx;
            double termY2 = Math.abs(dy) * discriminantRt;
            double yPlus = (termY1 + termY2) / drSquared;
            double yMinus = (termY1 - termY2) / drSquared;
            // add to intersections
            Vector2D first = new Vector2D(xPlus, yPlus).add(circle.getMidpoint());
            Vector2D second = new Vector2D(xMinus, yMinus).add(circle.getMidpoint());

            if (isOnLine(first)) {
                intersections.add(first);
            }
            if (isOnLine(second)) {
                intersections.add(second);
            }
        }
        return intersections;
    }

    default Optional<Vector2D> getIntersectionWith(LineSegment lineSegment) {

        double p0_x = getStartingPoint().getX();
        double p0_y = getStartingPoint().getY();
        double p1_x = getEndingPoint().getX();
        double p1_y = getEndingPoint().getY();

        double p2_x = lineSegment.getStartingPoint().getX();
        double p2_y = lineSegment.getStartingPoint().getY();
        double p3_x = lineSegment.getEndingPoint().getX();
        double p3_y = lineSegment.getEndingPoint().getY();

        double s1_x = p1_x - p0_x;
        double s1_y = p1_y - p0_y;
        double s2_x = p3_x - p2_x;
        double s2_y = p3_y - p2_y;

        double s = (-s1_y * (p0_x - p2_x) + s1_x * (p0_y - p2_y)) / (-s2_x * s1_y + s1_x * s2_y);
        double t = (s2_x * (p0_y - p2_y) - s2_y * (p0_x - p2_x)) / (-s2_x * s1_y + s1_x * s2_y);

        if (s >= 0 && s <= 1 && t >= 0 && t <= 1) {
            // collision detected
            return Optional.of(new Vector2D(p0_x + (t * s1_x), p0_y + (t * s1_y)));
        }
        return Optional.empty();
    }

    default double distanceTo(Vector2D vector) {
        Vector2D start = getStartingPoint();
        Vector2D end = getEndingPoint();
        // |w-v|^2 -  avoid a sqrt
        double l2 = VectorMetricProvider.SQUARED_EUCLIDEAN_METRIC.calculateDistance(start, end);
        // find projection of point p onto the line, where t = [(p-v) . (w-v)] / |w-v|^2
        // clamp t from [0,1] to handle points outside the segment
        double t = Math.max(0, Math.min(1, vector.subtract(start).dotProduct(end.subtract(start)) / l2));
        // projection falls on the segment v + t * (w - v);
        Vector2D projection = start.add(end.subtract(start).multiply(t));
        return VectorMetricProvider.EUCLIDEAN_METRIC.calculateDistance(vector, projection);
    }

    default Vector2D getRandomPoint() {
        if (isHorizontal()) {
            // x can be varied
            double segmentStartX = getStartingPoint().getX();
            double segmentEndX = getEndingPoint().getX();
            // switch points if necessary
            if (segmentStartX >= segmentEndX) {
                double temp = segmentStartX;
                segmentStartX = segmentEndX;
                segmentEndX = temp;
            }
            // determine random initial position
            double startY = getStartingPoint().getY();
            double startX = ThreadLocalRandom.current().nextDouble(segmentStartX, segmentEndX);
            return new Vector2D(startX, startY);

        }
        if (isVertical()) {
            // y can be varied
            double segmentStartY = getStartingPoint().getY();
            double segmentEndY = getEndingPoint().getY();
            // switch points if necessary
            if (segmentStartY >= segmentEndY) {
                double temp = segmentStartY;
                segmentStartY = segmentEndY;
                segmentEndY = temp;
            }
            // determine random initial position
            double startX = getStartingPoint().getX();
            double startY = ThreadLocalRandom.current().nextDouble(segmentStartY, segmentEndY);
            return new Vector2D(startX, startY);
        }
        SimpleLineSegment simpleLineSegment = (SimpleLineSegment) this;
        double start = getStartingPoint().getX();
        double end = getEndingPoint().getX();
        // switch points if necessary
        if (start >= end) {
            double temp = start;
            start = end;
            end = temp;
        }
        // calculate initial position
        double xValue = ThreadLocalRandom.current().nextDouble(start, end);
        double yValue = simpleLineSegment.getYValue(xValue);
        return new Vector2D(xValue, yValue);
    }

    default int evaluatePointPosition(Vector2D point) {
        Vector2D start = getStartingPoint();
        Vector2D end = getEndingPoint();
        double d = (point.getX() - start.getX()) * (end.getY() - start.getY()) - (point.getY() - start.getY()) * (end.getX() - start.getX());
        if (d == 0) {
            return ON_LINE;
        }
        if (d > 0) {
            return OUTSIDE;
        }
        return INSIDE;
    }

    default boolean isCongruent(LineSegment lineSegment) {
        return (lineSegment.getStartingPoint().equals(getStartingPoint()) && lineSegment.getEndingPoint().equals(getEndingPoint())) ||
                (lineSegment.getStartingPoint().equals(getEndingPoint()) && lineSegment.getEndingPoint().equals(getStartingPoint()));
    }

}
