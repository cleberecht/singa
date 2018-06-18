package de.bioforscher.singa.simulation.modules.newmodules.module;

import de.bioforscher.singa.chemistry.descriptive.entities.ChemicalEntity;
import de.bioforscher.singa.features.model.Feature;
import de.bioforscher.singa.features.model.ScalableFeature;
import de.bioforscher.singa.features.parameters.Environment;
import de.bioforscher.singa.simulation.exceptions.NumericalInstabilityException;
import de.bioforscher.singa.simulation.modules.model.DeltaIdentifier;
import de.bioforscher.singa.simulation.modules.model.LocalError;
import de.bioforscher.singa.simulation.modules.model.Updatable;
import de.bioforscher.singa.simulation.modules.newmodules.Delta;
import de.bioforscher.singa.simulation.modules.newmodules.functions.AbstractDeltaFunction;
import de.bioforscher.singa.simulation.modules.newmodules.scope.UpdateScope;
import de.bioforscher.singa.simulation.modules.newmodules.simulation.Simulation;
import de.bioforscher.singa.simulation.modules.newmodules.simulation.UpdateScheduler;
import de.bioforscher.singa.simulation.modules.newmodules.specifity.UpdateSpecificity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.measure.Quantity;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import static de.bioforscher.singa.simulation.modules.newmodules.module.ModuleState.*;

/**
 * @author cl
 */
public abstract class ConcentrationBasedModule<DeltaFunctionType extends AbstractDeltaFunction> implements UpdateModule {

    /**
     * The logger
     */
    private static final Logger logger = LoggerFactory.getLogger(ConcentrationBasedModule.class);

    /**
     * The default value where deltas validated to be effectively zero.
     */
    private static final double DEFAULT_NUMERICAL_CUTOFF = 1e-100;

    /**
     * The default value where numerical errors to be considered irretrievably unstable
     */
    private static final double DEFAULT_ERROR_CUTOFF = 100;

    private Simulation simulation;
    protected FieldSupplier supplier;
    private UpdateScope scope;
    private UpdateSpecificity<DeltaFunctionType> specificity;
    private UpdateScheduler updateScheduler;
    private ModuleFeatureManager featureManager;
    private Predicate<Updatable> applicationCondition;
    private Set<ChemicalEntity> referencedChemicalEntities;
    private String identifier;

    private ModuleState state;
    private double deltaCutoff = DEFAULT_NUMERICAL_CUTOFF;
    private double errorCutoff = DEFAULT_ERROR_CUTOFF;

    public ConcentrationBasedModule() {
        supplier = new FieldSupplier();
        featureManager = new ModuleFeatureManager(supplier);
        referencedChemicalEntities = new HashSet<>();
        state = PENDING;
        applicationCondition = updatable -> true;
    }

    protected void addDeltaFunction(DeltaFunctionType deltaFunction) {
        specificity.addDeltaFunction(deltaFunction);
    }

    public void setApplicationCondition(Predicate<Updatable> applicationCondition) {
        this.applicationCondition = applicationCondition;
    }

    public Set<ChemicalEntity> getReferencedEntities() {
        return referencedChemicalEntities;
    }

    public void addReferencedEntity(ChemicalEntity chemicalEntity) {
        referencedChemicalEntities.add(chemicalEntity);
    }

    public void addReferencedEntities(Collection<? extends ChemicalEntity> chemicalEntities) {
        referencedChemicalEntities.addAll(chemicalEntities);
    }

    public double getDeltaCutoff() {
        return deltaCutoff;
    }

    public void setDeltaCutoff(double deltaCutoff) {
        this.deltaCutoff = deltaCutoff;
    }

    public double getErrorCutoff() {
        return errorCutoff;
    }

    public void setErrorCutoff(double errorCutoff) {
        this.errorCutoff = errorCutoff;
    }

    public Simulation getSimulation() {
        return simulation;
    }

    public Predicate<Updatable> getApplicationCondition() {
        return applicationCondition;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public FieldSupplier getSupplier() {
        return supplier;
    }

    public UpdateScope getScope() {
        return scope;
    }

    void setScope(UpdateScope scope) {
        this.scope = scope;
    }

    public UpdateSpecificity getSpecificity() {
        return specificity;
    }

    void setSpecificity(UpdateSpecificity<DeltaFunctionType> specificity) {
        this.specificity = specificity;
    }

    public void setSimulation(Simulation simulation) {
        this.simulation = simulation;
        updateScheduler = simulation.getScheduler();
    }

    @Override
    public ModuleState getState() {
        return state;
    }

    public void handleDelta(DeltaIdentifier deltaIdentifier, Delta delta) {
        logDelta(deltaIdentifier, delta);
        if (supplier.isStrutCalculation()) {
            delta = delta.multiply(2.0);
            supplier.getCurrentHalfDeltas().put(deltaIdentifier, delta);
            supplier.getCurrentUpdatable().addPotentialDelta(delta);
        } else {
            supplier.getCurrentFullDeltas().put(deltaIdentifier, delta);
        }
    }

    private void logDelta(DeltaIdentifier deltaIdentifier, Delta delta) {
        logger.trace("{} delta for {} in {}:{} = {}",
                supplier.isStrutCalculation() ? "Half" : "Full",
                deltaIdentifier.getEntity().getIdentifier(),
                deltaIdentifier.getUpdatable().getStringIdentifier(),
                deltaIdentifier.getSubsection().getIdentifier(),
                delta.getQuantity());
    }

    /**
     * Returns true if the delta is valid, i.e. it is not zero and nor below the numerical threshold.
     *
     * @param delta The delta to be evaluated.
     * @return true if the delta is valid, i.e. it is not zero and nor below the numerical threshold.
     */
    public boolean deltaIsValid(Delta delta) {
        return deltaIsNotZero(delta) && deltaIsAboveNumericCutoff(delta);
    }

    /**
     * Returns true if the delta is not zero.
     *
     * @param delta The delta to be evaluated.
     * @return true if the delta is not zero.
     */
    private boolean deltaIsNotZero(Delta delta) {
        return delta.getQuantity().getValue().doubleValue() != 0.0;
    }

    /**
     * Returns true if the delta is above the numerical cutoff (not effectivley zero).
     *
     * @param delta The delta to be evaluated.
     * @return true if the delta is above the numerical cutoff (not effectivley zero).
     */
    private boolean deltaIsAboveNumericCutoff(Delta delta) {
        return Math.abs(delta.getQuantity().getValue().doubleValue()) > deltaCutoff;
    }

    private void checkErrorStability(double fullDelta, double halfDelta, double error) {
        if (error > errorCutoff) {
            throw new NumericalInstabilityException("The simulation experiences numerical instabilities. The local " +
                    "error between the full step delta (" + fullDelta + ") and half step delta (" + halfDelta + ") is "
                    + error + ". This can be an result of time steps that have been initially chosen too large" +
                    " or an implementation error in module that calculated the delta.");
        }
    }

    /**
     * The local error is calculated and the largest local error of the current epoch resulting from the executing
     * module is returned. The local error is calculated according to the midpoint method
     * E = abs(1 - (fullDelta / 2.0 * halfDelta)). Intuitively, applying the the delta for the current time step once
     * results in the same result as if the delta for half the time step would be applied twice. This method calculates
     * the difference between the full delta and twice the half delta. If the difference is large the error is large and
     * vice versa.
     *
     * @return The calculated local error.
     * @throws NumericalInstabilityException if any of the encountered errors is the result of an numerical instability.
     */
    public LocalError determineLargestLocalError() {
        // no deltas mean this module did not change anything in the course of this simulation step
        if (supplier.getCurrentFullDeltas().isEmpty()) {
            return LocalError.MINIMAL_EMPTY_ERROR;
        }
        // compare full and half deltas
        double largestLocalError = -Double.MAX_VALUE;
        DeltaIdentifier largestIdentifier = null;
        for (DeltaIdentifier identifier : supplier.getCurrentFullDeltas().keySet()) {
            double fullDelta = supplier.getCurrentFullDeltas().get(identifier).getQuantity().getValue().doubleValue();
            double halfDelta = supplier.getCurrentHalfDeltas().get(identifier).getQuantity().getValue().doubleValue();
            // calculate error
            double localError = Math.abs(1 - (fullDelta / halfDelta));
            // check for numerical instabilities
            checkErrorStability(fullDelta, halfDelta, localError);
            // determine the largest error in the current deltas
            if (largestLocalError < localError) {
                largestIdentifier = identifier;
                largestLocalError = localError;
            }
        }
        // safety check
        Objects.requireNonNull(largestIdentifier);
        LocalError localError = new LocalError(largestIdentifier.getUpdatable(), largestIdentifier.getEntity(), largestLocalError);
        logger.debug("The largest error was {} for {}", localError.getValue(), localError.getUpdatable());
        // set local error and return local error
        return localError;
    }

    @Override
    public void calculateUpdates() {
        scope.processAllUpdatables(simulation.getUpdatables());
        evaluateModuleState();
    }

    @Override
    public LocalError optimizeTimeStep() {
        Updatable updatable = supplier.getLargestLocalError().getUpdatable();
        while (state == REQUIRING_RECALCULATION) {
            // reset previous error
            supplier.resetError();
            // determine new local error with decreased time step
            updateScheduler.decreaseTimeStep();
            scope.processUpdatable(updatable);
            // evaluate module state by error
            evaluateModuleState();
        }
        logger.debug("Optimized local error for {} was {} with time step of {}.", this, supplier.getLargestLocalError().getValue(), Environment.getTimeStep());
        return supplier.getLargestLocalError();
    }

    private void evaluateModuleState() {
        if (supplier.getLargestLocalError().getValue() < updateScheduler.getRecalculationCutoff()) {
            state = SUCCEEDED;
        } else {
            logger.trace("Recalculation required for error {}.", supplier.getLargestLocalError().getValue());
            state = REQUIRING_RECALCULATION;
        }
    }

    @Override
    public void resetState() {
        state = PENDING;
        supplier.resetError();
    }

    public void addModuleToSimulation() {
        simulation.getModules().add(this);
        for (ChemicalEntity chemicalEntity : referencedChemicalEntities) {
            simulation.addReferencedEntity(chemicalEntity);
        }
    }

    @Override
    public void scaleScalableFeatures() {
        featureManager.scaleScalableFeatures();
    }

    @Override
    public Set<Class<? extends Feature>> getRequiredFeatures() {
        return featureManager.getRequiredFeatures();
    }

    @Override
    public <FeatureContentType extends Quantity<FeatureContentType>> Quantity<FeatureContentType> getScaledFeature(Class<? extends ScalableFeature<FeatureContentType>> featureClass) {
        return featureManager.getScaledFeature(featureClass);
    }

    public <FeatureType extends Feature> void setFeature(FeatureType feature) {
        featureManager.setFeature(feature);
    }

    public Collection<Feature<?>> getFeatures() {
        return featureManager.getAllFeatures();
    }

    public String listFeatures(String precedingSpaces) {
        return featureManager.listFeatures(precedingSpaces);
    }

    protected <FeatureContentType extends Quantity<FeatureContentType>> Quantity<FeatureContentType> getScaledFeature(ChemicalEntity entity, Class<? extends ScalableFeature<FeatureContentType>> featureClass) {
        ScalableFeature<FeatureContentType> feature = entity.getFeature(featureClass);
        if (supplier.isStrutCalculation()) {
            return feature.getHalfScaledQuantity();
        }
        return feature.getScaledQuantity();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

}
