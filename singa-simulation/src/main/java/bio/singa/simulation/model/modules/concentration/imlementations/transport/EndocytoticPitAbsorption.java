package bio.singa.simulation.model.modules.concentration.imlementations.transport;

import bio.singa.chemistry.entities.ChemicalEntity;
import bio.singa.chemistry.features.reactions.FirstOrderRate;
import bio.singa.simulation.features.CargoAdditionRate;
import bio.singa.simulation.features.Cargoes;
import bio.singa.simulation.features.ScalingEntities;
import bio.singa.simulation.model.modules.concentration.*;
import bio.singa.simulation.model.modules.concentration.functions.UpdatableDeltaFunction;
import bio.singa.simulation.model.modules.qualitative.implementations.EndocytoticPit;
import bio.singa.simulation.model.sections.ConcentrationContainer;
import bio.singa.simulation.model.simulation.Simulation;
import bio.singa.simulation.model.simulation.Updatable;

import javax.measure.Quantity;
import java.util.*;

import static bio.singa.simulation.model.sections.CellTopology.MEMBRANE;

/**
 * @author cl
 */
public class EndocytoticPitAbsorption extends ConcentrationBasedModule<UpdatableDeltaFunction> {

    public EndocytoticPitAbsorption() {

    }

    public static EntityStep inSimulation(Simulation simulation) {
        return new EndocytoticPitAbsorptionBuilder(simulation);
    }

    private static boolean isCollectingEndocytoticPit(Updatable updatable) {
        return updatable instanceof EndocytoticPit && ((EndocytoticPit) updatable).isCollecting();
    }

    public static ModuleBuilder getBuilder(Simulation simulation) {
        return new EndocytoticPitAbsorptionBuilder(simulation);
    }

    public void postConstruct() {
        setApplicationCondition(EndocytoticPitAbsorption::isCollectingEndocytoticPit);
        UpdatableDeltaFunction function = new UpdatableDeltaFunction(this::calculateDeltas, container -> true);
        addDeltaFunction(function);
        getRequiredFeatures().add(CargoAdditionRate.class);
        getRequiredFeatures().add(Cargoes.class);
        getRequiredFeatures().add(ScalingEntities.class);
    }

    private Map<ConcentrationDeltaIdentifier, ConcentrationDelta> calculateDeltas(ConcentrationContainer concentrationContainer) {
        Map<ConcentrationDeltaIdentifier, ConcentrationDelta> deltas = new HashMap<>();
        EndocytoticPit currentPit = ((EndocytoticPit) supplier.getCurrentUpdatable());
        // get cargoes
        List<ChemicalEntity> cargoes = getFeature(Cargoes.class).getContent();
        // get base addition rate
        double additionRate = getScaledFeature(CargoAdditionRate.class);
        // apply for all cargoes
        // inhibiting entities
        ScalingEntities scalingEntities = getFeature(ScalingEntities.class);
        double modifier = 1;
        // no scaling entities no rate scaling
        if (scalingEntities != null) {
            ChemicalEntity catalyzingEntity = scalingEntities.getContent().get(0);
            ChemicalEntity inhibitingEntity = scalingEntities.getContent().get(1);
            // determine rate modifier
            double catalyzingConcentration = currentPit.getAssociatedNode().getConcentrationContainer().get(MEMBRANE, catalyzingEntity);
            double inhibitingConcentration = currentPit.getAssociatedNode().getConcentrationContainer().get(MEMBRANE, inhibitingEntity);
            // scale linear
            modifier = (catalyzingConcentration / (catalyzingConcentration + inhibitingConcentration)) * 1.4;
        }

        for (ChemicalEntity cargo : cargoes) {
            double concentration = currentPit.getAssociatedNode().getConcentrationContainer().get(MEMBRANE, cargo);
            if (concentration == 0.0) {
                continue;
            }
            double concentrationDelta = additionRate * modifier * concentration;
            // add to pit
            addDelta(deltas, new ConcentrationDeltaIdentifier(currentPit, currentPit.getCellRegion().getMembraneSubsection(), currentPit.getStringIdentifier(), cargo),
                    concentrationDelta);
            // remove from associated membrane
            addDelta(deltas, new ConcentrationDeltaIdentifier(currentPit.getAssociatedNode(), currentPit.getAssociatedNode().getCellRegion().getMembraneSubsection(), currentPit.getStringIdentifier(), cargo),
                    -concentrationDelta);
        }
        return deltas;
    }

    private void addDelta(Map<ConcentrationDeltaIdentifier, ConcentrationDelta> deltas, ConcentrationDeltaIdentifier identifier, double concentrationDelta) {
        if (deltas.containsKey(identifier)) {
            deltas.put(identifier, deltas.get(identifier).add(concentrationDelta));
        } else {
            deltas.put(identifier, new ConcentrationDelta(this, identifier.getSubsection(), identifier.getEntity(), concentrationDelta));
        }
    }

    public interface EntityStep {
        EntityStep identifier(String identifier);

        AccelerationStep forEntity(ChemicalEntity chemicalEntity);

        AccelerationStep forAllEntities(ChemicalEntity... chemicalEntities);

        AccelerationStep forAllEntities(Collection<ChemicalEntity> chemicalEntities);

        AccelerationStep cargo(Cargoes cargoes);

    }

    public interface AccelerationStep {

        InhibitionStep acceleratingEntity(ChemicalEntity entity);

        RateStep scalingEntities(ScalingEntities scalingEntities);

    }

    public interface InhibitionStep {

        RateStep inhibitingEntity(ChemicalEntity entity);

    }

    public interface RateStep {

        BuildStep rate(Quantity<FirstOrderRate> rate);

        BuildStep cargoAdditionRate(CargoAdditionRate rate);

    }

    public interface BuildStep {
        EndocytoticPitAbsorption build();
    }

    public static class EndocytoticPitAbsorptionBuilder implements EntityStep, AccelerationStep, InhibitionStep, RateStep, BuildStep, ModuleBuilder<EndocytoticPitAbsorption> {

        EndocytoticPitAbsorption module;
        private Simulation simulation;
        private ChemicalEntity acceleratingEntity;


        public EndocytoticPitAbsorptionBuilder(Simulation simulation) {
            this.simulation = simulation;
            createModule(simulation);
        }

        @Override
        public EndocytoticPitAbsorption getModule() {
            return module;
        }


        @Override
        public EndocytoticPitAbsorption createModule(Simulation simulation) {
            module = ModuleFactory.setupModule(EndocytoticPitAbsorption.class,
                    ModuleFactory.Scope.SEMI_NEIGHBOURHOOD_DEPENDENT,
                    ModuleFactory.Specificity.UPDATABLE_SPECIFIC);
            return module;
        }

        public EntityStep identifier(String identifier) {
            module.setIdentifier(identifier);
            return this;
        }

        public AccelerationStep forEntity(ChemicalEntity chemicalEntity) {
            return forAllEntities(Collections.singletonList(chemicalEntity));
        }

        public AccelerationStep forAllEntities(ChemicalEntity... chemicalEntities) {
            return forAllEntities(Arrays.asList(chemicalEntities));
        }

        public AccelerationStep forAllEntities(Collection<ChemicalEntity> chemicalEntities) {
            Cargoes cargoes = Cargoes.of(chemicalEntities)
                    .comment("cargoes added to the endocytotic pit during collection phase")
                    .build();
            return cargo(cargoes);
        }

        @Override
        public InhibitionStep acceleratingEntity(ChemicalEntity acceleratingEntity) {
            this.acceleratingEntity = acceleratingEntity;
            return this;
        }

        @Override
        public RateStep inhibitingEntity(ChemicalEntity inhibitingEntity) {
            return scalingEntities(ScalingEntities.of(acceleratingEntity, inhibitingEntity).build());
        }

        @Override
        public BuildStep rate(Quantity<FirstOrderRate> rate) {
            return cargoAdditionRate(new CargoAdditionRate(rate));
        }

        @Override
        public AccelerationStep cargo(Cargoes cargoes) {
            module.setFeature(cargoes);
            return this;
        }

        @Override
        public RateStep scalingEntities(ScalingEntities scalingEntities) {
            if (scalingEntities == null) {
                return this;
            }
            module.setFeature(scalingEntities);
            return this;
        }

        @Override
        public BuildStep cargoAdditionRate(CargoAdditionRate rate) {
            module.setFeature(rate);
            return this;
        }

        public EndocytoticPitAbsorption build() {
            module.postConstruct();
            simulation.addModule(module);
            return module;
        }

    }

}
