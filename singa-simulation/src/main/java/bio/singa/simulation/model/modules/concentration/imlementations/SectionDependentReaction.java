package bio.singa.simulation.model.modules.concentration.imlementations;

import bio.singa.chemistry.entities.ChemicalEntity;
import bio.singa.simulation.model.modules.concentration.reactants.EntityExtractionCondition;
import bio.singa.chemistry.features.reactions.BackwardsRateConstant;
import bio.singa.chemistry.features.reactions.ForwardsRateConstant;
import bio.singa.chemistry.features.reactions.RateConstant;
import bio.singa.features.model.Feature;
import bio.singa.simulation.export.format.FormatReactionEquation;
import bio.singa.simulation.model.agents.pointlike.Vesicle;
import bio.singa.simulation.model.graphs.AutomatonNode;
import bio.singa.simulation.model.modules.concentration.*;
import bio.singa.simulation.model.modules.concentration.functions.UpdatableDeltaFunction;
import bio.singa.simulation.model.modules.concentration.reactants.EntityReducer;
import bio.singa.simulation.model.modules.concentration.reactants.Reactant;
import bio.singa.simulation.model.modules.concentration.reactants.ReactantRole;
import bio.singa.simulation.model.sections.CellSubsection;
import bio.singa.simulation.model.sections.ConcentrationContainer;
import bio.singa.simulation.model.simulation.Simulation;
import bio.singa.simulation.model.simulation.Updatable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static bio.singa.simulation.model.sections.CellTopology.INNER;
import static bio.singa.simulation.model.sections.CellTopology.MEMBRANE;

/**
 * @author cl
 */
public class SectionDependentReaction extends ConcentrationBasedModule<UpdatableDeltaFunction> {

    private static final Logger logger = LoggerFactory.getLogger(SectionDependentReaction.class);

    public static SectionDependentReactionBuilder inSimulation(Simulation simulation) {
        return new SectionDependentReactionBuilder(simulation);
    }

    private List<Reactant> substrates;
    private List<Reactant> products;

    private List<List<EntityExtractionCondition>> substrateConditions;
    private List<ChemicalEntity> dynamicSubstrates;

    private RateConstant forwardsReactionRate;
    private RateConstant backwardsReactionRate;

    public SectionDependentReaction() {
        substrates = new ArrayList<>();
        products = new ArrayList<>();
        substrateConditions = new ArrayList<>();
    }

    void postConstruct() {
        // apply
        // TODO apply condition ?
        setApplicationCondition(updatable -> true);
        // function
        // TODO apply condition this::containsReactants
        UpdatableDeltaFunction function = new UpdatableDeltaFunction(this::calculateDeltas, container -> true);
        addDeltaFunction(function);
        // feature
        getRequiredFeatures().add(ForwardsRateConstant.class);
        getRequiredFeatures().add(BackwardsRateConstant.class);
        // reference entities for this module
        for (Reactant substrate : substrates) {
            addReferencedEntity(substrate.getEntity());
        }
        for (Reactant product : products) {
            addReferencedEntity(product.getEntity());
        }

    }

    public List<Reactant> getSubstrates() {
        return substrates;
    }

    public void setSubstrates(List<Reactant> substrates) {
        this.substrates = substrates;
    }

    public void addSubstrate(Reactant substrate) {
        substrates.add(substrate);
    }

    public List<Reactant> getProducts() {
        return products;
    }

    public void setProducts(List<Reactant> products) {
        this.products = products;
    }

    public void addProduct(Reactant product) {
        products.add(product);
    }

    public List<List<EntityExtractionCondition>> getSubstrateConditions() {
        return substrateConditions;
    }

    public void addSubstrateCondition(List<EntityExtractionCondition> substrateConditions) {
        this.substrateConditions.add(substrateConditions);
    }

    public void addReactant(Reactant stoichiometricReactant) {
        if (stoichiometricReactant.isSubstrate()) {
            substrates.add(stoichiometricReactant);
        } else {
            products.add(stoichiometricReactant);
        }
        getSimulation().addReferencedEntity(stoichiometricReactant.getEntity());
    }

    public boolean substratesAvailable(Updatable updatable) {
        for (Reactant substrate : substrates) {
            if (!updatable.getConcentrationContainer().containsEntity(substrate.getPreferredTopology(), substrate.getEntity())) {
                return false;
            }
        }
        return true;
    }

    double determineEffectiveConcentration(ConcentrationContainer concentrationContainer, ReactantRole role) {
        List<Reactant> relevantReactants;
        if (role == ReactantRole.SUBSTRATE) {
            relevantReactants = substrates;
        } else if (role == ReactantRole.PRODUCT) {
            relevantReactants = products;
        } else {
            throw new IllegalArgumentException("The effective concentrations is only determined from substrates or products");
        }
        double product = 1.0;
        for (Reactant reactant : relevantReactants) {
            product *= concentrationContainer.get(reactant.getPreferredTopology(), reactant.getEntity());
        }
        return product;
    }

    private void determineSubstrates(ConcentrationContainer container) {
        dynamicSubstrates = new ArrayList<>();
        for (List<EntityExtractionCondition> catalystCondition : substrateConditions) {
            dynamicSubstrates.addAll(EntityReducer.apply(container.getPool(INNER).getValue().getReferencedEntities(), catalystCondition));
        }
        for (List<EntityExtractionCondition> catalystCondition : substrateConditions) {
            dynamicSubstrates.addAll(EntityReducer.apply(container.getPool(MEMBRANE).getValue().getReferencedEntities(), catalystCondition));
        }
    }

    /**
     * Calculates all deltas for all reactants for the reaction.
     *
     * @param concentrationContainer The concentration container to calculate the deltas for.
     * @return The calculated deltas.
     */
    private Map<ConcentrationDeltaIdentifier, ConcentrationDelta> calculateDeltas(ConcentrationContainer concentrationContainer) {
        Map<ConcentrationDeltaIdentifier, ConcentrationDelta> deltas = new HashMap<>();
        Updatable currentUpdatable = supplier.getCurrentUpdatable();
        if (currentUpdatable instanceof Vesicle) {
            handlePartialDistributionInVesicles(deltas, (Vesicle) currentUpdatable);
        } else {
            double velocity = calculateVelocity(concentrationContainer);
            if (velocity != 0.0) {
                // add deltas for substrates
                for (Reactant substrate : substrates) {
                    CellSubsection substrateSubsection = concentrationContainer.getSubsection(substrate.getPreferredTopology());
                    addDelta(deltas, new ConcentrationDeltaIdentifier(supplier.getCurrentUpdatable(), substrateSubsection, substrate.getEntity()), -velocity);
                }
                // add deltas for products
                for (Reactant product : products) {
                    CellSubsection productSubsection = concentrationContainer.getSubsection(product.getPreferredTopology());
                    addDelta(deltas, new ConcentrationDeltaIdentifier(supplier.getCurrentUpdatable(), productSubsection, product.getEntity()), velocity);
                }
            }
        }
        return deltas;
    }

    private void handlePartialDistributionInVesicles(Map<ConcentrationDeltaIdentifier, ConcentrationDelta> deltas, Vesicle vesicle) {
        Map<AutomatonNode, Double> associatedNodes = vesicle.getAssociatedNodes();
        ConcentrationContainer vesicleContainer;
        if (supplier.isStrutCalculation()) {
            vesicleContainer = getScope().getHalfStepConcentration(vesicle);
        } else {
            vesicleContainer = vesicle.getConcentrationContainer();
        }
        for (Map.Entry<AutomatonNode, Double> entry : associatedNodes.entrySet()) {
            AutomatonNode node = entry.getKey();
            ConcentrationContainer nodeContainer;
            if (supplier.isStrutCalculation()) {
                nodeContainer = getScope().getHalfStepConcentration(node);
            } else {
                nodeContainer = node.getConcentrationContainer();
            }
            // assuming equal distribution of entities on the membrane surface the fraction of the associated surface is
            // used to scale the velocity
            double velocity = calculateVelocity(vesicleContainer, nodeContainer) * entry.getValue();
            if (velocity != 0.0) {
                // add deltas for substrates
                for (Reactant substrate : substrates) {
                    CellSubsection substrateSubsection;
                    if (substrate.getPreferredTopology().equals(MEMBRANE)) {
                        substrateSubsection = vesicleContainer.getMembraneSubsection();
                        addDelta(deltas, new ConcentrationDeltaIdentifier(vesicle, substrateSubsection, substrate.getEntity()), -velocity);
                    } else {
                        substrateSubsection = nodeContainer.getSubsection(substrate.getPreferredTopology());
                        addDelta(deltas, new ConcentrationDeltaIdentifier(node, substrateSubsection, substrate.getEntity()), -velocity);
                    }
                }
                // add deltas for products
                for (Reactant product : products) {
                    CellSubsection productSubsection;
                    if (product.getPreferredTopology().equals(MEMBRANE)) {
                        productSubsection = vesicleContainer.getMembraneSubsection();
                        addDelta(deltas, new ConcentrationDeltaIdentifier(vesicle, productSubsection, product.getEntity()), velocity);
                    } else {
                        productSubsection = nodeContainer.getSubsection(product.getPreferredTopology());
                        addDelta(deltas, new ConcentrationDeltaIdentifier(node, productSubsection, product.getEntity()), velocity);
                    }
                }
            }
        }
    }

    private double calculateVelocity(ConcentrationContainer vesicleContainer, ConcentrationContainer nodeContainer) {
        // get rates
        final double forwardsRateConstant = getScaledForwardsReactionRate();
        final double backwardsRateConstant = getScaledBackwardsReactionRate();

        // determine substrate concentration
        double substrateConcentration = 1.0;
        for (Reactant substrate : substrates) {
            if (substrate.getPreferredTopology().equals(MEMBRANE)) {
                substrateConcentration *= vesicleContainer.get(MEMBRANE, substrate.getEntity());
            } else {
                substrateConcentration *= nodeContainer.get(substrate.getPreferredTopology(), substrate.getEntity());
            }
        }

        // determine product concentration
        double productConcentration = 1.0;
        for (Reactant product : products) {
            if (product.getPreferredTopology().equals(MEMBRANE)) {
                productConcentration *= vesicleContainer.get(MEMBRANE, product.getEntity());
            } else {
                productConcentration *= nodeContainer.get(product.getPreferredTopology(), product.getEntity());
            }
        }

        // calculate velocity
        return forwardsRateConstant * substrateConcentration - backwardsRateConstant * productConcentration;
    }

    private double calculateVelocity(ConcentrationContainer concentrationContainer) {
        // get rates
        final double forwardsRateConstant = getScaledForwardsReactionRate();
        final double backwardsRateConstant = getScaledBackwardsReactionRate();

        // determine substrate concentration
        double substrateConcentration = 1.0;
        for (Reactant substrate : substrates) {
            substrateConcentration *= concentrationContainer.get(substrate.getPreferredTopology(), substrate.getEntity());
        }

        // determine product concentration
        double productConcentration = 1.0;
        for (Reactant product : products) {
            productConcentration *= concentrationContainer.get(product.getPreferredTopology(), product.getEntity());
        }

        // calculate velocity
        return forwardsRateConstant * substrateConcentration - backwardsRateConstant * productConcentration;
    }

    private void addDelta(Map<ConcentrationDeltaIdentifier, ConcentrationDelta> deltas, ConcentrationDeltaIdentifier identifier, double concentrationDelta) {
        if (deltas.containsKey(identifier)) {
            deltas.put(identifier, deltas.get(identifier).add(concentrationDelta));
        } else {
            deltas.put(identifier, new ConcentrationDelta(this, identifier.getSubsection(), identifier.getEntity(), concentrationDelta));
        }
    }

    @Override
    public void checkFeatures() {
        boolean forwardsRateFound = false;
        boolean backwardsRateFound = false;
        for (Feature<?> feature : getFeatures()) {
            // any forwards rate constant
            if (feature instanceof ForwardsRateConstant) {
                forwardsRateFound = true;
                logger.debug("Required feature {} has been set to {}.", feature.getDescriptor(), feature.getContent());
            }
            // any backwards rate constant
            if (feature instanceof BackwardsRateConstant) {
                backwardsRateFound = true;
                logger.debug("Required feature {} has been set to {}.", feature.getDescriptor(), feature.getContent());
            }
        }
        if (!forwardsRateFound) {
            logger.warn("Required feature {} has not been set.", ForwardsRateConstant.class.getSimpleName());
        }
        if (!backwardsRateFound) {
            logger.warn("Required feature {} has not been set.", BackwardsRateConstant.class.getSimpleName());
        }
    }

    private double getScaledForwardsReactionRate() {
        if (forwardsReactionRate == null) {
            for (Feature<?> feature : getFeatures()) {
                // any forwards rate constant
                if (feature instanceof ForwardsRateConstant) {
                    forwardsReactionRate = (RateConstant) feature;
                    break;
                }
            }
        }
        if (supplier.isStrutCalculation()) {
            return forwardsReactionRate.getHalfScaledQuantity();
        }
        return forwardsReactionRate.getScaledQuantity();
    }

    private double getScaledBackwardsReactionRate() {
        if (backwardsReactionRate == null) {
            for (Feature<?> feature : getFeatures()) {
                // any forwards rate constant
                if (feature instanceof BackwardsRateConstant) {
                    backwardsReactionRate = (RateConstant) feature;
                    break;
                }
            }
        }
        if (supplier.isStrutCalculation()) {
            return backwardsReactionRate.getHalfScaledQuantity();
        }
        return backwardsReactionRate.getScaledQuantity();
    }

    public String getReactionString() {
        return FormatReactionEquation.formatASCII(this);
    }

    @Override
    public String toString() {
        return getIdentifier() + " (" + getReactionString() + ")";
    }

    public static ModuleBuilder getBuilder(Simulation simulation) {
        return new SectionDependentReactionBuilder(simulation);
    }

}
