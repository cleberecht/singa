package bio.singa.simulation.model.modules.concentration.scope;

import bio.singa.simulation.model.modules.concentration.*;
import bio.singa.simulation.model.modules.concentration.imlementations.transport.Diffusion;
import bio.singa.simulation.model.modules.concentration.specifity.UpdateSpecificity;
import bio.singa.simulation.model.sections.ConcentrationContainer;
import bio.singa.simulation.model.simulation.Updatable;
import bio.singa.simulation.model.simulation.error.NumericalError;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Dependent Updatable {@link ConcentrationBasedModule}s require an integer state of basically all updatables in a
 * simulation. First all updates for all updatables are calculated. Afterwards all half step concentrations are
 * determined and further all errors are calculated, looking for the {@link Updatable} with the largest
 * {@link NumericalError} (e.g. {@link Diffusion}).
 *
 * @author cl
 */
public class DependentUpdate implements UpdateScope {

    /**
     * The storage of the half concentrations.
     */
    private Map<Updatable, ConcentrationContainer> halfConcentrations;

    /**
     * The associated module.
     */
    private ConcentrationBasedModule<?> module;

    /**
     * Initializes the update scope for the corresponding module.
     * @param module The module.
     */
    public DependentUpdate(ConcentrationBasedModule<?> module) {
        this.module = module;
        halfConcentrations = new HashMap<>();
    }

    /**
     * Returns a object, managing shared properties of the module.
     * @return The supplier.
     */
    private FieldSupplier supply() {
        return module.getSupplier();
    }

    /**
     * Returns the update specificity behaviour of the module, required for the actual computation of the updates.
     * @return The update specificity behaviour.
     */
    private UpdateSpecificity specify() {
        return module.getSpecificity();
    }

    @Override
    public void processAllUpdatables(Collection<? extends Updatable> updatables) {
        // clear used deltas
        supply().clearDeltas();
        // calculate all full updates first
        supply().setStrutCalculation(false);
        for (Updatable updatable : updatables) {
            if (module.getApplicationCondition().test(updatable)) {
                supply().setCurrentUpdatable(updatable);
                specify().processContainer(updatable.getConcentrationContainer());
            }
        }
        module.inBetweenHalfSteps();
        // explicitly calculate half step concentrations
        determineHalfStepConcentrations();
        supply().setStrutCalculation(true);
        for (ConcentrationDeltaIdentifier identifier : supply().getCurrentFullDeltas().keySet()) {
            supply().setCurrentUpdatable(identifier.getUpdatable());
            specify().processContainer(getHalfStepConcentration(identifier.getUpdatable()), identifier.getSubsection(), identifier.getEntity());
        }
        module.inBetweenHalfSteps();
        // set largest local error
        supply().setLargestLocalError(module.determineLargestLocalError());
    }

    @Override
    public void processUpdatable(Updatable updatable) {
        processAllUpdatables(module.getSimulation().getUpdatables());
    }

    @Override
    public void clearPotentialDeltas() {
        for (Updatable current : module.getSimulation().getUpdatables()) {
            current.getConcentrationManager().clearPotentialDeltas();
        }
    }

    /**
     * Determines all half step concentrations for each calculated full delta.
     */
    private void determineHalfStepConcentrations() {
        // clean up previous values
        halfConcentrations.clear();
        // for each full delta
        for (Map.Entry<ConcentrationDeltaIdentifier, ConcentrationDelta> entry : supply().getCurrentFullDeltas().entrySet()) {
            // get required values
            final ConcentrationDeltaIdentifier identifier = entry.getKey();
            final ConcentrationDelta fullDelta = entry.getValue();
            final Updatable updatable = identifier.getUpdatable();
            ConcentrationContainer container;
            // check if container has been initialized
            if (halfConcentrations.containsKey(updatable)) {
                container = halfConcentrations.get(updatable);
            } else {
                container = updatable.getConcentrationContainer().fullCopy();
                halfConcentrations.put(updatable, container);
            }
            // get full concentration
            double fullConcentration = updatable.getConcentrationContainer().get(identifier.getSubsection(), identifier.getEntity());
            // add half of the full delta
            double halfStepConcentration = fullConcentration + (fullDelta.getValue() * 0.5);
            // update concentration
            container.set(identifier.getSubsection(), identifier.getEntity(), halfStepConcentration);
        }
    }

    @Override
    public ConcentrationContainer getHalfStepConcentration(Updatable updatable) {
        ConcentrationContainer container = halfConcentrations.get(updatable);
        if (container == null) {
            // if there was no concentration container assigned, there was no delta calculated for this node,
            // therefore the concentration between half and full did not change
            return updatable.getConcentrationContainer();
        }
        return container;
    }

}
