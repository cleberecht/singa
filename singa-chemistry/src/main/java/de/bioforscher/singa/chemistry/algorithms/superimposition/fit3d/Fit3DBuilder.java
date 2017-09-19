package de.bioforscher.singa.chemistry.algorithms.superimposition.fit3d;

import de.bioforscher.singa.chemistry.algorithms.superimposition.SubstructureSuperimpositionException;
import de.bioforscher.singa.chemistry.parser.pdb.structures.StructureParser.MultiParser;
import de.bioforscher.singa.chemistry.physical.atoms.Atom;
import de.bioforscher.singa.chemistry.physical.atoms.representations.RepresentationScheme;
import de.bioforscher.singa.chemistry.physical.atoms.representations.RepresentationSchemeFactory;
import de.bioforscher.singa.chemistry.physical.atoms.representations.RepresentationSchemeType;
import de.bioforscher.singa.chemistry.physical.branches.BranchSubstructure;
import de.bioforscher.singa.chemistry.physical.branches.StructuralMotif;
import de.bioforscher.singa.chemistry.physical.families.substitution.matrices.SubstitutionMatrix;
import de.bioforscher.singa.chemistry.physical.model.StructuralEntityFilter;

import java.util.Objects;
import java.util.function.Predicate;

import static de.bioforscher.singa.chemistry.physical.model.StructuralEntityFilter.AtomFilter;

/**
 * A builder that guides through the creation of a {@link Fit3D} alignment.
 *
 * @author fk
 */
public class Fit3DBuilder {

    /**
     * Default values for the Fit3D algorithm.
     */
    private static final double DEFAULT_DISTANCE_TOLERANCE = 1.0;
    private static final double DEFAULT_RMSD_CUTOFF = 2.5;
    private static final Predicate<Atom> DEFAULT_ATOM_FILTER = AtomFilter.isArbitrary();

    /**
     * Default values for the Fit3DSite algorithm.
     */
    private static final double DEFAULT_CUTOFF_SCORE = 5.0;
    private static final SubstitutionMatrix DEFAULT_SUBSTITUTION_MATRIX = SubstitutionMatrix.BLOSUM_45;

    /**
     * prevent instantiation
     */
    private Fit3DBuilder() {

    }

    /**
     * Creates a new instance of the actual {@link Builder}.
     *
     * @return The {@link QueryStep} to define the query motif.
     */
    public static QueryStep create() {
        return new Builder();
    }

    public interface QueryStep {
        /**
         * Defines the query motif for this {@link Fit3D} search.
         *
         * @param query The query motif to be used.
         * @return The {@link TargetStep} to define one or several targets.
         */
        TargetStep query(StructuralMotif query);

        /**
         * Defines a site that should be aligned against another.
         *
         * @param site The first site to be aligned.
         * @return The {@link SiteStep} to define the antagonist.
         */
        SiteStep site(StructuralMotif site);
    }

    public interface SiteStep {
        /**
         * Defines the second site for the pairwise site alignment.
         *
         * @param site The second site to be aligned.
         * @return The {@link AtomStep} to define optional restrictions on {@link Atom}s.
         */
        SiteParameterConfigurationStep vs(StructuralMotif site);
    }

    public interface SiteConfigurationStep {

        /**
         * Restricts the site alignment to the specified exchanges of the input sites.
         *
         * @return The {@link AtomStep} to define optional restrictions on {@link Atom}s.
         */
        AtomStep restrictToSpecifiedExchanges();

        /**
         * Ignores the specified exchanges of the input sites and allows alignment of any type against any type using a
         * heuristic that does not necessarily yield the best alignment possible.
         *
         * @return The {@link AtomStep} to define optional restrictions on {@link Atom}s.
         */
        AtomStep ignoreSpecifiedExchanges();

        /**
         * Guarantees to find the ideal alignment of the input sites.
         *
         * @return The {@link AtomStep} to define optional restrictions on {@link Atom}s.
         */
        AtomStep exhaustive();
    }

    public interface SiteParameterConfigurationStep extends SiteConfigurationStep {
        /**
         * The cutoff score that should be used when extending the site alignment.
         *
         * @param cutoffScore The cutoff score.
         * @return The {@link SiteParameterConfigurationStep} to configure other parameters.
         */
        SiteParameterConfigurationStep cutoffScore(double cutoffScore);

        /**
         * The {@link SubstitutionMatrix} to be used to calculate the Xie score.
         *
         * @param substitutionMatrix The substitution matrix.
         * @return The {@link SiteParameterConfigurationStep} to configure other parameters.
         */
        SiteParameterConfigurationStep substitutionMatrix(SubstitutionMatrix substitutionMatrix);
    }

    public interface TargetStep {
        /**
         * Defines the target against which this {@link Fit3D} search should be run.
         *
         * @param target The target {@link BranchSubstructure} against which the search should be run.
         * @return The {@link AtomStep} to define optional restrictions on {@link Atom}s.
         */
        AtomStep target(BranchSubstructure<?, ?> target);

        /**
         * Defines the targets against which this Fit3D search should be run in batch mode. This can either be a list of
         * PDB-IDs or file paths pointing to target files in PDB format.
         *
         * @param multiParser The {@link MultiParser} that provides the structures to that should be run in batch mode.
         * @return The {@link BatchParameterStep} to define the level of parallelism the batch search should use.
         */
        BatchParameterStep targets(MultiParser multiParser);
    }

    public interface BatchParameterStep {
        /**
         * Defines for a batch search the level of parallelism (number of cores) that should be used by {@link Fit3D}.
         *
         * @param limitedParallelism The desired level of parallelism.
         * @return The {@link AtomStep} to define optional restrictions on {@link Atom}s.
         */
        AtomStep limitedParallelism(int limitedParallelism);

        /**
         * Allows Fit3D in search mode to use all available cores for processing.
         *
         * @return The {@link AtomStep} to define optional restrictions on {@link Atom}s.
         */
        AtomStep maximalParallelism();

        /**
         * Sets the option to skip all targets that consist of only alpha carbon atoms to avoid a lot of noise in the
         * results.
         *
         * @return The {@link ParameterStep} that can be used to define optional parameters.
         */
        BatchParameterStep skipAlphaCarbonTargets();


        /**
         * Sets the option to skip all targets that consist of only backbone atoms to avoid a lot of noise in the
         * results.
         *
         * @return The {@link ParameterStep} that can be used to define optional parameters.
         */
        BatchParameterStep skipBackboneTargets();
    }

    public interface AtomStep {
        /**
         * Creates a new {@link Fit3D} search and starts calculation.
         *
         * @return A new {@link Fit3D} search when finished.
         */
        Fit3D run() throws SubstructureSuperimpositionException;

        /**
         * Defines a {@link StructuralEntityFilter.AtomFilter} filter to be used for the {@link Fit3D} alignment (e.g.
         * only sidechain atoms).
         *
         * @param atomFilter The {@link StructuralEntityFilter.AtomFilter} filter to be used for the alignment.
         * @return The {@link ParameterStep} that can be used to define optional parameters.
         */
        ParameterStep atomFilter(Predicate<Atom> atomFilter);

        /**
         * Defines a single point {@link RepresentationScheme} that should be used to represent single residues.
         *
         * @param representationSchemeType The {@link RepresentationSchemeType} that should be used.
         * @return The {@link ParameterStep} that can be used to define optional parameters.
         */
        ParameterStep representationScheme(RepresentationSchemeType representationSchemeType);
    }

    public interface ParameterStep {
        /**
         * Creates a new {@link Fit3D} search and starts calculation.
         *
         * @return A new {@link Fit3D} search when finished.
         */
        Fit3D run() throws SubstructureSuperimpositionException;

        /**
         * Defines the RMSD cutoff up to which matches should be reported. If a {@link Fit3DSiteAlignment} is performed
         * this is the cutoff that is used for the internal call of the Fit3D algorithm.
         *
         * @param rmsdCutoff The RMSD cutoff up to which alignments should be reported.
         * @return The {@link ParameterStep} that can be used to define optional parameters.
         */
        ParameterStep rmsdCutoff(double rmsdCutoff);

        /**
         * Defines the distance tolerance that is accepted when extracting local environments. If a {@link
         * Fit3DSiteAlignment} is performed this is the cutoff that is used for the internal call of the Fit3D
         * algorithm.
         *
         * @param distanceTolerance The distance tolerance considered when extracting local environments.
         * @return The {@link ParameterStep} that can be used to define optional parameters.
         */
        ParameterStep distanceTolerance(double distanceTolerance);
    }

    public static class Builder implements QueryStep, SiteStep, SiteParameterConfigurationStep, SiteConfigurationStep, TargetStep, AtomStep, BatchParameterStep, ParameterStep {

        StructuralMotif queryMotif;
        MultiParser multiParser;
        BranchSubstructure<?, ?> target;
        int parallelism;
        double rmsdCutoff = DEFAULT_RMSD_CUTOFF;
        double distanceTolerance = DEFAULT_DISTANCE_TOLERANCE;
        Predicate<Atom> atomFilter = DEFAULT_ATOM_FILTER;
        RepresentationScheme representationScheme;
        StructuralMotif site1;
        StructuralMotif site2;
        double cutoffScore = DEFAULT_CUTOFF_SCORE;
        boolean exhaustive;
        boolean restrictToExchanges;
        SubstitutionMatrix substitutionMatrix = DEFAULT_SUBSTITUTION_MATRIX;
        boolean skipAlphaCarbonTargets;
        boolean skipBackboneTargets;

        @Override
        public TargetStep query(StructuralMotif query) {
            Objects.requireNonNull(query);
            this.queryMotif = query;
            return this;
        }

        @Override
        public SiteStep site(StructuralMotif site1) {
            Objects.requireNonNull(site1);
            this.site1 = site1;
            return this;
        }

        @Override
        public AtomStep target(BranchSubstructure<?, ?> target) {
            Objects.requireNonNull(target);
            this.target = target;
            return this;
        }

        @Override
        public BatchParameterStep targets(MultiParser multiParser) {
            Objects.requireNonNull(multiParser);

            this.multiParser = multiParser;
            return this;
        }

        @Override
        public Fit3D run() throws SubstructureSuperimpositionException {
            // decide which implementation should be used
            if (this.multiParser != null) {
                return new Fit3DAlignmentBatch(this);
            }
            if (this.site1 != null && this.site2 != null) {
                return new Fit3DSiteAlignment(this);
            }
            return new Fit3DAlignment(this);
        }

        @Override
        public ParameterStep rmsdCutoff(double rmsdCutoff) {
            if (rmsdCutoff <= 0.0) {
                throw new Fit3DException("RMSD cutoff must be positive");
            }
            this.rmsdCutoff = rmsdCutoff;
            return this;
        }

        @Override
        public ParameterStep distanceTolerance(double distanceTolerance) {
            if (distanceTolerance <= 0.0) {
                throw new Fit3DException("distance tolerance cutoff must be positive");
            }
            this.distanceTolerance = distanceTolerance;
            return this;
        }

        @Override
        public ParameterStep atomFilter(Predicate<Atom> atomFilter) {
            Objects.requireNonNull(atomFilter);
            this.atomFilter = atomFilter;
            return this;
        }

        @Override
        public ParameterStep representationScheme(RepresentationSchemeType representationSchemeType) {
            Objects.requireNonNull(representationSchemeType);
            this.representationScheme = RepresentationSchemeFactory.createRepresentationScheme(representationSchemeType);
            return this;
        }

        @Override
        public AtomStep limitedParallelism(int limitedParallelism) {
            if (limitedParallelism <= 0.0) {
                throw new Fit3DException("level of parallelism for batch calculation must be positive");
            }
            this.parallelism = limitedParallelism;
            return this;
        }

        @Override
        public AtomStep maximalParallelism() {
            this.parallelism = Runtime.getRuntime().availableProcessors();
            return this;
        }

        @Override
        public BatchParameterStep skipAlphaCarbonTargets() {
            this.skipAlphaCarbonTargets = true;
            return this;
        }

        @Override
        public BatchParameterStep skipBackboneTargets() {
            this.skipBackboneTargets = true;
            return this;
        }

        @Override
        public SiteParameterConfigurationStep vs(StructuralMotif site2) {
            Objects.requireNonNull(site2);
            this.site2 = site2;
            return this;
        }

        @Override
        public SiteParameterConfigurationStep cutoffScore(double cutoffScore) {
            this.cutoffScore = cutoffScore;
            return this;
        }

        @Override
        public SiteParameterConfigurationStep substitutionMatrix(SubstitutionMatrix substitutionMatrix) {
            this.substitutionMatrix = substitutionMatrix;
            return this;
        }

        @Override
        public AtomStep restrictToSpecifiedExchanges() {
            this.restrictToExchanges = true;
            return this;
        }

        @Override
        public AtomStep ignoreSpecifiedExchanges() {
            this.restrictToExchanges = false;
            return this;
        }

        @Override
        public AtomStep exhaustive() {
            this.exhaustive = true;
            return this;
        }
    }
}
