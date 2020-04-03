package bio.singa.simulation.model.modules.concentration.imlementations;

import bio.singa.chemistry.entities.simple.Protein;
import bio.singa.chemistry.entities.simple.SmallMolecule;
import bio.singa.chemistry.features.reactions.MichaelisConstant;
import bio.singa.chemistry.features.reactions.TurnoverNumber;
import bio.singa.features.model.Evidence;
import bio.singa.features.units.UnitRegistry;
import bio.singa.simulation.model.graphs.AutomatonGraph;
import bio.singa.simulation.model.graphs.AutomatonGraphs;
import bio.singa.simulation.model.graphs.AutomatonNode;
import bio.singa.simulation.model.modules.concentration.imlementations.reactions.ReactionBuilder;
import bio.singa.simulation.model.sections.CellSubsection;
import bio.singa.simulation.model.simulation.Simulation;
import bio.singa.simulation.model.simulation.error.TimeStepManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tech.units.indriya.quantity.Quantities;
import tech.units.indriya.unit.ProductUnit;

import javax.measure.Quantity;
import javax.measure.quantity.Time;

import static bio.singa.features.units.UnitProvider.MOLE_PER_LITRE;
import static bio.singa.simulation.model.sections.CellRegions.EXTRACELLULAR_REGION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static tech.units.indriya.AbstractUnit.ONE;
import static tech.units.indriya.unit.Units.MINUTE;
import static tech.units.indriya.unit.Units.SECOND;

/**
 * @author cl
 */
class MichaelisMentenReactionTest {

    @BeforeAll
    static void initialize() {
        UnitRegistry.reinitialize();
    }

    @AfterEach
    void cleanUp() {
        UnitRegistry.reinitialize();
    }

    @Test
    @DisplayName("example reaction - with enzyme fructose bisphosphate aldolase")
    void testMichaelisMentenReaction() {
        // SABIO Entry ID: 28851
        // Kinetic properties of fructose bisphosphate aldolase from Trypanosoma brucei compared to aldolase from rabbit
        // muscle and Staphylococcus aureus.
        // create simulation
        Simulation simulation = new Simulation();
        simulation.setMaximalTimeStep(Quantities.getQuantity(0.1, SECOND));
        // setup graph
        AutomatonGraph graph = AutomatonGraphs.singularGraph();

        // get required species
        SmallMolecule fructosePhosphate = SmallMolecule.create("FP").build();
        SmallMolecule glyceronePhosphate = SmallMolecule.create("GP").build();
        SmallMolecule glyceraldehyde = SmallMolecule.create("GA").build();
        Protein aldolase = Protein.create("P07752").build();

        // rates
        MichaelisConstant michaelisConstant = new MichaelisConstant(Quantities.getQuantity(9.0e-3, MOLE_PER_LITRE), Evidence.NO_EVIDENCE);
        TurnoverNumber turnoverNumber = new TurnoverNumber(76, new ProductUnit<>(ONE.divide(MINUTE)), Evidence.NO_EVIDENCE);

        // set concentrations
        CellSubsection subsection = EXTRACELLULAR_REGION.getInnerSubsection();
        for (AutomatonNode node : graph.getNodes()) {
            node.getConcentrationContainer().initialize(subsection, fructosePhosphate, Quantities.getQuantity(1.0, MOLE_PER_LITRE));
            node.getConcentrationContainer().initialize(subsection, aldolase, Quantities.getQuantity(0.01, MOLE_PER_LITRE));
        }

        // setup reaction
        // create reaction using the properties of the enzyme
        ReactionBuilder.staticReactants(simulation)
                .addSubstrate(fructosePhosphate)
                .addCatalyst(aldolase)
                .addProduct(glyceraldehyde)
                .addProduct(glyceronePhosphate)
                .michaelisMenten()
                .michaelisConstant(michaelisConstant)
                .turnover(turnoverNumber)
                .build();

        // add graph
        simulation.setGraph(graph);

        AutomatonNode node = graph.getNode(0, 0);
        Quantity<Time> currentTime;
        Quantity<Time> firstCheckpoint = Quantities.getQuantity(39.2, SECOND);
        boolean firstCheckpointPassed = false;
        Quantity<Time> secondCheckpoint = Quantities.getQuantity(90, SECOND);
        // run simulation
        while ((currentTime = TimeStepManager.getElapsedTime().to(SECOND)).getValue().doubleValue() < secondCheckpoint.getValue().doubleValue()) {
            simulation.nextEpoch();
            if (!firstCheckpointPassed && currentTime.getValue().doubleValue() > firstCheckpoint.getValue().doubleValue()) {
                assertEquals(0.50, UnitRegistry.concentration(node.getConcentrationContainer().get(subsection, fructosePhosphate)).to(MOLE_PER_LITRE).getValue().doubleValue(), 1e-2);
                assertEquals(0.49, UnitRegistry.concentration(node.getConcentrationContainer().get(subsection, glyceronePhosphate)).to(MOLE_PER_LITRE).getValue().doubleValue(), 1e-2);
                assertEquals(0.49, UnitRegistry.concentration(node.getConcentrationContainer().get(subsection, glyceraldehyde)).to(MOLE_PER_LITRE).getValue().doubleValue(), 1e-2);
                assertEquals(0.01, UnitRegistry.concentration(node.getConcentrationContainer().get(subsection, aldolase)).to(MOLE_PER_LITRE).getValue().doubleValue());
                firstCheckpointPassed = true;
            }
        }
        // check final values
        assertEquals(0.0, UnitRegistry.concentration(node.getConcentrationContainer().get(subsection, fructosePhosphate)).to(MOLE_PER_LITRE).getValue().doubleValue(), 1e-3);
        assertEquals(1.0, UnitRegistry.concentration(node.getConcentrationContainer().get(subsection, glyceronePhosphate)).to(MOLE_PER_LITRE).getValue().doubleValue(), 1e-3);
        assertEquals(1.0, UnitRegistry.concentration(node.getConcentrationContainer().get(subsection, glyceraldehyde)).to(MOLE_PER_LITRE).getValue().doubleValue(), 1e-3);
        assertEquals(0.01, UnitRegistry.concentration(node.getConcentrationContainer().get(subsection, aldolase)).to(MOLE_PER_LITRE).getValue().doubleValue());
    }

}