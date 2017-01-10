package de.bioforscher.chemistry.algorithms.superimposition.fit3d;

import de.bioforscher.chemistry.algorithms.superimposition.SubStructureSuperimposer;
import de.bioforscher.chemistry.algorithms.superimposition.SubstructureSuperimposition;
import de.bioforscher.chemistry.physical.atoms.Atom;
import de.bioforscher.chemistry.physical.atoms.AtomFilter;
import de.bioforscher.chemistry.physical.atoms.representations.RepresentationScheme;
import de.bioforscher.chemistry.physical.branches.BranchSubstructure;
import de.bioforscher.chemistry.physical.branches.StructuralMotif;
import de.bioforscher.chemistry.physical.leafes.LeafSubstructure;
import de.bioforscher.chemistry.physical.model.StructuralFamily;
import de.bioforscher.chemistry.physical.model.Structures;
import de.bioforscher.core.utility.Pair;
import de.bioforscher.mathematics.matrices.LabeledSymmetricMatrix;
import de.bioforscher.mathematics.matrices.Matrices;
import de.bioforscher.mathematics.vectors.RegularVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * An implementation of the Fit3D algorithm for substructure search.
 *
 * @author fk
 */
public class Fit3DAlignment implements Fit3D {

    public static final double DEFAULT_DISTANCE_TOLERANCE = 1.0;
    public static final double DEFAULT_RMSD_CUTOFF = 2.5;
    private static final Predicate<Atom> DEFAULT_ATOM_FILTER = AtomFilter.isArbitrary();

    private static final Logger logger = LoggerFactory.getLogger(Fit3DAlignment.class);

    private final StructuralMotif queryMotif;
    private final BranchSubstructure<?> target;
    private final double squaredDistanceTolerance;
    private final RepresentationScheme representationScheme;
    private double squaredQueryExtent;
    private LabeledSymmetricMatrix<LeafSubstructure<?, ?>> squaredDistanceMatrix;
    private List<List<LeafSubstructure<?, ?>>> environments;
    private HashMap<List<LeafSubstructure<?, ?>>, Set<Set<LeafSubstructure<?, ?>>>> candidates;
    private double rmsdCutoff;
    private TreeMap<Double, SubstructureSuperimposition> matches;
    private Predicate<Atom> atomFilter;

    Fit3DAlignment(Fit3DBuilder.Builder builder) {
        // obtain copies of the input structures
        // TODO this cast is not nice, can we do something better?
        this.queryMotif = builder.queryMotif.getCopy();
        this.target = (BranchSubstructure<?>) builder.target.getCopy();
        this.rmsdCutoff = builder.rmsdCutoff;
        // use squared distance tolerance
        this.squaredDistanceTolerance = builder.distanceTolerance * builder.distanceTolerance;
        this.atomFilter = builder.atomFilter;
        this.representationScheme = builder.representationScheme;

        if (this.queryMotif.size() > this.target.getLeafSubstructures().size()) {
            throw new Fit3DException("search target must contain at least as many atom-containing substructures " +
                    "as the queryMotif");
        }

        // initialize
        this.environments = new ArrayList<>();
        this.matches = new TreeMap<>();
        this.candidates = new HashMap<>();

        logger.info("computing Fit3D alignment of motif {} against {}", this.queryMotif, this.target);

        // reduce target structures to the types that are actually occurring in the query motif or defined exchanges
        reduceTargetStructure();

        // calculate squared motif extent
        calculateMotifExtent();

        // calculate squared distance matrix
        this.squaredDistanceMatrix = this.target.getSquaredDistanceMatrix();
        logger.debug("the target structure squared distance matrix is\n{}",
                this.squaredDistanceMatrix.getStringRepresentation());

        composeEnvironments();
        generateCandidates();
        computeMatches();
    }

    /**
     * Returns the computed matches of this Fit3D search.
     *
     * @return The matches of this search.
     */
    @Override
    public TreeMap<Double, SubstructureSuperimposition> getMatches() {
        return this.matches;
    }

    /**
     * Computes all matches of the generated candidates.
     */
    private void computeMatches() {
        this.candidates.values().stream()
                .flatMap(Collection::stream)
                .forEach(this::computeAlignments);
    }

    /**
     * Computes all valid alignments of a given {@link LeafSubstructure}.
     *
     * @param leafSubstructures the {@link LeafSubstructure} for which alignments should be computed.
     */
    private void computeAlignments(Set<LeafSubstructure<?, ?>> leafSubstructures) {
        ValidAlignmentGenerator validAlignmentGenerator =
                new ValidAlignmentGenerator(this.queryMotif.getLeafSubstructures(), new ArrayList<>(leafSubstructures));
        List<List<Pair<LeafSubstructure<?, ?>>>> validAlignments = validAlignmentGenerator.getValidAlignments();
        for (List<Pair<LeafSubstructure<?, ?>>> validAlignment : validAlignments) {
            // create candidate for alignment
            List<LeafSubstructure<?, ?>> alignmentCandidate = validAlignment.stream()
                    .map(Pair::getSecond).collect(Collectors.toList());
            // apply representation scheme if defined
            SubstructureSuperimposition superimposition;
            if (this.representationScheme != null) {
                superimposition = SubStructureSuperimposer
                        .calculateSubstructureSuperimposition(this.queryMotif.getLeafSubstructures(),
                                alignmentCandidate, this.representationScheme);
            } else {
                superimposition = SubStructureSuperimposer
                        .calculateSubstructureSuperimposition(this.queryMotif.getLeafSubstructures(),
                                alignmentCandidate, this.atomFilter);
            }
            if (superimposition.getRmsd() <= this.rmsdCutoff) {
                this.matches.put(superimposition.getRmsd(), superimposition);
            }
        }
    }

    /**
     * Generates all candidates based on the pre-computed environments.
     */
    private void generateCandidates() {
        for (List<LeafSubstructure<?, ?>> environment : this.environments) {
            Set<Set<LeafSubstructure<?, ?>>> currentCandidates = new ValidCandidateGenerator(
                    this.queryMotif.getLeafSubstructures(),
                    environment).getValidCandidates();
            this.candidates.put(environment, currentCandidates);
        }
    }

    public List<List<LeafSubstructure<?, ?>>> getEnvironments() {
        return this.environments;
    }

    /**
     * Determines the maximal spatial extent of the query motif, measured on the centroid of all atoms.
     */
    private void calculateMotifExtent() {
        LabeledSymmetricMatrix<LeafSubstructure<?, ?>> queryDistanceMatrix =
                Structures.calculateSquaredDistanceMatrix(this.queryMotif);
        // position of maximal element is always symmetric, hence we consider the first
        Pair<Integer> positionOfMaximalElement = Matrices.getPositionsOfMaximalElement(queryDistanceMatrix).stream()
                .findFirst()
                .orElseThrow(() -> new Fit3DException("could not determine extent of the query motif"));
        this.squaredQueryExtent = queryDistanceMatrix.getElement(positionOfMaximalElement.getFirst(),
                positionOfMaximalElement.getSecond());
        logger.debug("the squared query motif extent is {}", this.squaredQueryExtent);
    }


    /**
     * Reduces the target structure only to the {@link StructuralFamily} types that are contained in the query motif or
     * its defined exchanges.
     */
    private void reduceTargetStructure() {
        // collect all containing types (own types <b>plus</b> exchangeable types) of the query motif
        Set<StructuralFamily> containingTypes = this.queryMotif.getLeafSubstructures().stream()
                .map(LeafSubstructure::getContainingTypes)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
        List<Integer> toBeRemoved = this.target.getLeafSubstructures().stream()
                .filter(leafSubstructure -> !containingTypes.contains(leafSubstructure.getFamily()))
                .map(LeafSubstructure::getIdentifier)
                .collect(Collectors.toList());
        toBeRemoved.forEach(this.target::removeSubstructure);
    }

    /**
     * Creates all micro-environments that can be built by iterating over the backbone.
     */
    private void composeEnvironments() {
        // iterate over reduced target structure
        for (LeafSubstructure<?, ?> currentSubstructure : this.target.getLeafSubstructures()) {
            // collect environments within the bounds if the motif extent
            RegularVector distanceToOthers = this.squaredDistanceMatrix.getColumnByLabel(currentSubstructure);
            List<LeafSubstructure<?, ?>> environment = new ArrayList<>();
            for (int i = 0; i < distanceToOthers.getElements().length; i++) {
                double currentDistance = distanceToOthers.getElement(i);
                if (currentDistance <= this.squaredQueryExtent + this.squaredDistanceTolerance) {
                    environment.add(this.squaredDistanceMatrix.getColumnLabel(i));
                }
            }
            if (environment.size() >= this.queryMotif.size()) {
                logger.debug("possible environment {} within {} around {} added", environment,
                        this.squaredQueryExtent + DEFAULT_DISTANCE_TOLERANCE, currentSubstructure);
                this.environments.add(environment);
            }
        }
    }
}