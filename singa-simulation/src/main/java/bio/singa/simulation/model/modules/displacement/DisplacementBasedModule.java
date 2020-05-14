package bio.singa.simulation.model.modules.displacement;

import bio.singa.mathematics.vectors.Vector2D;
import bio.singa.simulation.model.agents.pointlike.Vesicle;
import bio.singa.simulation.model.modules.AbstractUpdateModule;
import bio.singa.simulation.model.simulation.error.DisplacementDeviation;
import bio.singa.simulation.model.simulation.error.TimeStepManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import static bio.singa.simulation.model.modules.concentration.ModuleState.REQUIRING_RECALCULATION;
import static bio.singa.simulation.model.modules.concentration.ModuleState.SUCCEEDED_WITH_PENDING_CHANGES;
import static bio.singa.simulation.model.simulation.error.DisplacementDeviation.MAXIMAL_NEGATIVE_DEVIATION;
import static bio.singa.simulation.model.simulation.error.DisplacementDeviation.MINIMAL_DEVIATION;
import static bio.singa.simulation.model.simulation.error.ErrorManager.Reason.LOCAL_DEVIATION;

/**
 * @author cl
 */
public class DisplacementBasedModule extends AbstractUpdateModule {

    /**
     * The logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(DisplacementBasedModule.class);

    /**
     * The functions that are applied with each epoch.
     */
    private final Map<Function<Vesicle, DisplacementDelta>, Predicate<Vesicle>> deltaFunctions;

    private DisplacementDeviation largestLocalDeviation;

    public DisplacementBasedModule() {
        deltaFunctions = new HashMap<>();
        largestLocalDeviation = MINIMAL_DEVIATION;
    }

    public void addDeltaFunction(Function<Vesicle, DisplacementDelta> deltaFunction, Predicate<Vesicle> predicate) {
        deltaFunctions.put(deltaFunction, predicate);
    }

    @Override
    public void initialize() {

    }

    @Override
    public void onReset() {
        largestLocalDeviation = MINIMAL_DEVIATION;
    }

    @Override
    public void onCompletion() {

    }

    @Override
    public void calculateUpdates() {
        processAllVesicles(getSimulation().getVesicleLayer().getVesicles());
        if (!getSimulation().getScheduler().isSkipDisplacementChecks()) {
            evaluateModuleState();
        } else {
            setState(SUCCEEDED_WITH_PENDING_CHANGES);
        }
    }

    private void evaluateModuleState() {
        largestLocalDeviation = determineLocalDeviation();
        if (largestLocalDeviation.getValue() > getSimulation().getScheduler().getErrorManager().getDisplacementUpperThreshold()) {
            setState(REQUIRING_RECALCULATION);
        } else {
            getSimulation().getScheduler().getErrorManager().setLargestLocalDisplacementDeviation(largestLocalDeviation, this);
            setState(SUCCEEDED_WITH_PENDING_CHANGES);
        }
    }

    public DisplacementDeviation determineLocalDeviation() {
        DisplacementDeviation largestDeviation = MAXIMAL_NEGATIVE_DEVIATION;
        for (Vesicle vesicle : getSimulation().getVesicleLayer().getVesicles()) {
            // skip if no displacements where applied by this module
            Optional<DisplacementDelta> optionalDisplacementDelta = vesicle.getSpatialDelta(this);
            if (!optionalDisplacementDelta.isPresent()) {
                continue;
            }
            Vector2D displacement = optionalDisplacementDelta.get().getDeltaVector();
            // determine fraction of maximal allowed error
            double deviation = Math.log10(displacement.getMagnitude() / getSimulation().getScheduler().getErrorManager().getDisplacementReferenceLength());
            if (deviation > largestDeviation.getValue()) {
                largestDeviation = new DisplacementDeviation(vesicle, deviation);
            }
        }
        return largestDeviation;
    }

    @Override
    public void optimizeTimeStep() {
        while (getState() == REQUIRING_RECALCULATION) {
            getSimulation().getVesicleLayer().clearUpdates();
            TimeStepManager.decreaseTimeStep(LOCAL_DEVIATION);
            largestLocalDeviation = MAXIMAL_NEGATIVE_DEVIATION;
            calculateUpdates();
        }
    }

    private void processAllVesicles(List<Vesicle> vesicles) {
        // determine deltas
        for (Vesicle vesicle : vesicles) {
            logger.trace("Determining delta for {}.", vesicle.getStringIdentifier());
            determineDeltas(vesicle);
        }
    }

    private void determineDeltas(Vesicle vesicle) {
        for (Map.Entry<Function<Vesicle, DisplacementDelta>, Predicate<Vesicle>> entry : deltaFunctions.entrySet()) {
            // test predicate
            if (entry.getValue().test(vesicle)) {
                DisplacementDelta spatialDelta = entry.getKey().apply(vesicle);
                logDelta(vesicle, spatialDelta);
                vesicle.addPotentialSpatialDelta(spatialDelta);
            }
        }
    }

    private void logDelta(Vesicle vesicle, DisplacementDelta delta) {
        logger.trace("Displacement delta for {} at {} is {}",
                vesicle.getStringIdentifier(),
                vesicle.getPosition(),
                delta.getDeltaVector());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + (getIdentifier() != null ? " " + getIdentifier() : "");
    }

}
