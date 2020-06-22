package bio.singa.mathematics.vectors;

import bio.singa.mathematics.algorithms.graphs.ShortestPathFinder;
import bio.singa.mathematics.concepts.Addable;
import bio.singa.mathematics.geometry.edges.LineSegment;
import bio.singa.mathematics.graphs.model.*;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * This class contains only static utility methods to create and handle different Vectors.
 *
 * @author cl
 */
public class Vectors {

    /**
     * prevent instantiation
     */
    private Vectors() {
    }

    /**
     * Checks whether each Vector in the Collection has the same Dimension.
     *
     * @param vectors The vectors to check.
     * @return True, if all Vectors have the same dimension.
     */
    public static boolean haveSameDimension(Collection<Vector> vectors) {
        Iterator<Vector> iterator = vectors.iterator();
        int requiredDimension = iterator.next().getDimension();
        while (iterator.hasNext()) {
            if (requiredDimension != iterator.next().getDimension()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the average value of an {@link Vector}s elements or Double.NaN if not present.
     *
     * @param vector The {@link Vector} that holds the values.
     * @return The average of all {@link Vector} elements.
     */
    public static double getAverage(Vector vector) {
        OptionalDouble optionalAverage = vector.streamElements().average();
        if (optionalAverage.isPresent()) {
            return optionalAverage.getAsDouble();
        } else {
            return Double.NaN;
        }
    }

    /**
     * Returns the median value for a given {@link Vector}. That is the middle element if vector size is odd and the
     * average of the two middle elements of vector is even.
     *
     * @param vector The {@link Vector} that holds the values.
     * @return The median value of all {@link Vector} elements.
     */
    public static double getMedian(Vector vector) {
        double[] elements = vector.getCopy().getElements();
        Arrays.sort(elements);
        if (elements.length % 2 == 0) {
            return (elements[(elements.length / 2) - 1] + elements[elements.length / 2]) / 2.0;
        }
        return elements[elements.length / 2];
    }

    /**
     * Returns the variance of an {@link Vector}s elements.
     *
     * @param vector The {@link Vector} that holds the values.
     * @return The standard deviation of all {@link Vector} elements.
     */
    public static double getVariance(Vector vector) {
        double mean = getAverage(vector);
        double dv = 0D;
        for (double d : vector.getElements()) {
            double dm = d - mean;
            dv += dm * dm;
        }
        return dv / (vector.getDimension() - 1);
    }

    /**
     * Returns the standard deviation of an {@link Vector}s elements.
     *
     * @param vector The {@link Vector} that holds the values.
     * @return The standard deviation of all {@link Vector} elements.
     */
    public static double getStandardDeviation(Vector vector) {
        return Math.sqrt(getVariance(vector));
    }

    /**
     * Compares all values of the given index for all given vectors and returns the largest value found.
     *
     * @param index The index of the value, that is to be compared.
     * @param vectors A collection of Vectors.
     * @return The maximal value of the given index.
     */
    public static double getMaximalValueForIndex(int index, List<? extends Vector> vectors) {
        double maximalValue = -Double.MAX_VALUE;
        for (Vector vector : vectors) {
            if (vector.getElement(index) > maximalValue) {
                maximalValue = vector.getElement(index);
            }
        }
        return maximalValue;
    }

    /**
     * Compares all values of the given index for all given vectors and returns the smallest value found.
     *
     * @param index The index of the value, that is to be compared.
     * @param vectors A collection of Vectors.
     * @return The minimal value of the given index.
     */
    public static double getMinimalValueForIndex(int index, List<? extends Vector> vectors) {
        double minimalValue = Double.MAX_VALUE;
        for (Vector vector : vectors) {
            if (vector.getElement(index) < minimalValue) {
                minimalValue = vector.getElement(index);
            }
        }
        return minimalValue;
    }

    /**
     * Gets the first index with the maximal element in this vector.
     *
     * @param vector The vector.
     * @return The index of the maximal element
     */
    public static int getIndexWithMaximalElement(Vector vector) {
        int maximalIndex = -1;
        double maximalValue = -Double.MAX_VALUE;
        for (int index = 0; index < vector.getDimension(); index++) {
            double currentValue = vector.getElement(index);
            if (currentValue > maximalValue) {
                maximalValue = currentValue;
                maximalIndex = index;
            }
        }
        return maximalIndex;
    }

    /**
     * Gets the first index with the minimal element in this vector.
     *
     * @param vector The vector.
     * @return The index of the minimal element
     */
    public static int getIndexWithMinimalElement(Vector vector) {
        int minimalIndex = -1;
        double minimalValue = Double.MAX_VALUE;
        for (int index = 0; index < vector.getDimension(); index++) {
            double currentValue = vector.getElement(index);
            if (currentValue < minimalValue) {
                minimalValue = currentValue;
                minimalIndex = index;
            }
        }
        return minimalIndex;
    }

    /**
     * Gets the first index with the absolute maximal element in this vector.
     *
     * @param vector The vector.
     * @return The index of the absolute maximal element.
     */
    public static int getIndexWithAbsoluteMaximalElement(Vector vector) {
        int maximalIndex = -1;
        double maximalValue = -Double.MAX_VALUE;
        for (int index = 0; index < vector.getDimension(); index++) {
            double currentValue = Math.abs(vector.getElement(index));
            if (currentValue > maximalValue) {
                maximalValue = currentValue;
                maximalIndex = index;
            }
        }
        return maximalIndex;
    }

    /**
     * Gets the first index with the absolute minimal element in this vector.
     *
     * @param vector The vector.
     * @return The index of the minimal element
     */
    public static int getIndexWithAbsoluteMinimalElement(Vector vector) {
        int minimalIndex = -1;
        double minimalValue = Double.MAX_VALUE;
        for (int index = 0; index < vector.getDimension(); index++) {
            double currentValue = Math.abs(vector.getElement(index));
            if (currentValue < minimalValue) {
                minimalValue = currentValue;
                minimalIndex = index;
            }
        }
        return minimalIndex;
    }

    public static <VectorType extends Vector> List<VectorType> getVectorsWithMinimalValueForIndex(List<VectorType> vectors, int index) {
        if (vectors.size() == 1) {
            return vectors;
        }
        double minimalValue = Double.MAX_VALUE;
        List<VectorType> minimalVectors = new ArrayList<>();
        for (VectorType vector : vectors) {
            double currentValue = vector.getElement(index);
            if (Double.compare(currentValue, minimalValue) == 0)
                minimalVectors.add(vector);
            else if (Double.compare(currentValue, minimalValue) < 0) {
                minimalValue = currentValue;
                minimalVectors.clear();
                minimalVectors.add(vector);
            }
        }
        return minimalVectors;
    }

    /**
     * Computes the centroid of all vectors in the collection by summing them and dividing by the number of vectors in
     * the collection.
     *
     * @param vectors The vectors to calculate the centroid from.
     * @param <VectorType> The type of the vector.
     * @return The centroid.
     */
    public static <VectorType extends Vector> Vector getCentroid(Collection<VectorType> vectors) {
        return Addable.sum(vectors).divide(vectors.size());
    }

    /**
     * This method creates an orthonormalized set of vectors in an inner product space, in this case the Euclidean space
     * R^n equipped with the standard inner product ({@link Vector#dotProduct(Vector)}). The Gram-Schmidt process takes
     * a finite, linearly independent list S = {v1, ..., vk} for k ≤ n and generates an orthogonal list S′ = {u1, ...,
     * uk} that spans the same k-dimensional subspace of R^n as S.
     *
     * @param vectors The original vectors to be orthonormalized.
     * @return A list of orthonormal vectors.
     * @see <a href="https://en.wikipedia.org/wiki/Gram%E2%80%93Schmidt_process">Wikipedia: Gram-Schmidt process</a>
     */
    public static List<Vector> orthonormalizeVectors(List<Vector> vectors) {
        // using modified Gram-Schmidt process

        // all vectors need to have the same dimensionality
        if (!Vectors.haveSameDimension(vectors)) {
            throw new IllegalArgumentException("All vectors need to have the same dimensionality.");
        }
        int dimension = vectors.iterator().next().getDimension();
        // the number of vectors needs to be equal or smaller than the dimension of the vectors
        if (vectors.size() > dimension) {
            throw new IllegalArgumentException("The number of vectors needs to be equal or smaller than the dimension" +
                    " of the vectors");
        }
        // orthonormalize the given vectors
        List<Vector> orthonormalizedVectors = new ArrayList<>();
        List<Vector> normalizedVectors = new ArrayList<>();
        for (Vector vector : vectors) {
            if (orthonormalizedVectors.isEmpty()) {
                // just normalize
                orthonormalizedVectors.add(vector);
                normalizedVectors.add(vector.normalize());
            } else {
                // successively modify the original vector with the existing orthonormalized vectors
                Vector projectionSum = accumulateGramSchmidtProjection(vector, orthonormalizedVectors);
                orthonormalizedVectors.add(projectionSum);
                // lastly, normalize
                normalizedVectors.add(projectionSum.normalize());
            }
        }
        return normalizedVectors;
    }

    /**
     * The projection for the Graham-Schmidt process. This operator projects the first vector (v) orthogonally onto the
     * line spanned by the second vector (u). If u is the zero vector the projection will be also the zero vector. The
     * projection is calculated by: <p> proj(u,v) = ((v . u) / (u . u)) * u <p> with ( . ) denoting the inner product
     * (generally the dot product)
     *
     * @param first The vector (v) to be projected.
     * @param second The vector (u) to project with.
     * @return The projected vector.
     */
    public static Vector gramSchmidtProjection(Vector first, Vector second) {
        if (!second.isZero()) {
            return second.multiply(first.dotProduct(second) / second.dotProduct(second));
        } else {
            return second;
        }
    }

    /**
     * Accumulates the {@link Vectors#gramSchmidtProjection(Vector, Vector) Graham-Schmidt projection} for each vector
     * in the given list of Vectors. This orthonormalizes the vector to every vector and increases the numerical
     * stability of the orthonormalization.
     *
     * @param vector The vector to be projected.
     * @param orthogonalizedVectors The vectors to project with.
     * @return The projected vector.
     */
    public static Vector accumulateGramSchmidtProjection(Vector vector, List<Vector> orthogonalizedVectors) {
        // successively modify the original vector with the existing orthonormalized vectors
        Vector projectionSum = new RegularVector(vector.getDimension());
        boolean firstRun = true;
        for (Vector orthonormalizedVector : orthogonalizedVectors) {
            if (firstRun) {
                // in the first run use the original vector
                projectionSum = vector.subtract(gramSchmidtProjection(vector, orthonormalizedVector));
                firstRun = false;
            } else {
                // than accumulate changes
                projectionSum = projectionSum.subtract(gramSchmidtProjection(projectionSum, orthonormalizedVector));
            }
        }
        return projectionSum;
    }


    /**
     * Returns all vectors from the starting/end positions from the segments in order of their connection.
     *
     * @param segments The segments
     * @return An ordered list of segments
     */
    public static List<Vector2D> getVectorsInOrder(List<? extends LineSegment> segments) {
        UndirectedGraph graph = new UndirectedGraph();
        for (LineSegment segment : segments) {
            RegularNode start = graph.snapNode(segment.getStartingPoint());
            RegularNode end = graph.snapNode(segment.getEndingPoint());
            graph.addEdgeBetween(start, end);
        }

        Optional<RegularNode> pathStartOptional = graph.getNode(GraphPredicates::isLeafNode);
        if (!pathStartOptional.isPresent()) {
            return Collections.emptyList();
        }
        RegularNode pathStart = pathStartOptional.get();

        Optional<RegularNode> pathEndOptional = graph.getNode(node -> GraphPredicates.isLeafNode(node) && !GraphPredicates.haveSameIdentifiers(node, pathStart));
        if (!pathEndOptional.isPresent()) {
            return Collections.emptyList();
        }
        RegularNode pathEnd = pathEndOptional.get();

        GraphPath<RegularNode, UndirectedEdge> path = ShortestPathFinder.findBasedOnPredicate(graph, pathStart, node -> GraphPredicates.haveSameIdentifiers(node, pathEnd));
        List<Vector2D> vectors = new ArrayList<>();
        for (RegularNode node : path.getNodes()) {
            vectors.add(node.getPosition());
        }
        return vectors;
    }


}
