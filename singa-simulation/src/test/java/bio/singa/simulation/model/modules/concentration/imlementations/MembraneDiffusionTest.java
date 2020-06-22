package bio.singa.simulation.model.modules.concentration.imlementations;

import bio.singa.simulation.entities.simple.SmallMolecule;
import bio.singa.chemistry.features.permeability.MembranePermeability;
import bio.singa.features.model.Evidence;
import bio.singa.features.parameters.Environment;
import bio.singa.features.quantities.MolarConcentration;
import bio.singa.features.units.UnitRegistry;
import bio.singa.mathematics.vectors.Vector2D;
import bio.singa.simulation.model.agents.pointlike.Vesicle;
import bio.singa.simulation.model.agents.pointlike.VesicleLayer;
import bio.singa.simulation.model.agents.surfacelike.Membrane;
import bio.singa.simulation.model.agents.surfacelike.MembraneLayer;
import bio.singa.simulation.model.agents.surfacelike.MembraneTracer;
import bio.singa.simulation.model.graphs.AutomatonGraph;
import bio.singa.simulation.model.graphs.AutomatonGraphs;
import bio.singa.simulation.model.graphs.AutomatonNode;
import bio.singa.simulation.model.modules.concentration.imlementations.transport.MembraneDiffusion;
import bio.singa.simulation.model.sections.CellTopology;
import bio.singa.simulation.model.simulation.Simulation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import tech.units.indriya.ComparableQuantity;
import tech.units.indriya.quantity.Quantities;

import javax.measure.Quantity;
import javax.measure.quantity.Length;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static bio.singa.chemistry.features.permeability.MembranePermeability.CENTIMETRE_PER_SECOND;
import static bio.singa.features.units.UnitProvider.MOLE_PER_LITRE;
import static bio.singa.features.units.UnitRegistry.*;
import static bio.singa.simulation.model.sections.CellRegions.CELL_OUTER_MEMBRANE_REGION;
import static bio.singa.simulation.model.sections.CellRegions.CYTOPLASM_REGION;
import static bio.singa.simulation.model.sections.CellTopology.INNER;
import static bio.singa.simulation.model.sections.CellTopology.OUTER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tech.units.indriya.unit.MetricPrefix.MICRO;
import static tech.units.indriya.unit.MetricPrefix.NANO;
import static tech.units.indriya.unit.Units.METRE;
import static tech.units.indriya.unit.Units.SECOND;

/**
 * @author cl
 */
class MembraneDiffusionTest {

    private final SmallMolecule water = SmallMolecule.create("water")
            .assignFeature(new MembranePermeability(Quantities.getQuantity(3.5E-03, CENTIMETRE_PER_SECOND), Evidence.NO_EVIDENCE))
            .build();

    @BeforeAll
    static void initialize() {
        Environment.reset();
        UnitRegistry.reinitialize();
    }

    @AfterEach
    void cleanUp() {
        Environment.reset();
        UnitRegistry.reinitialize();
    }

    @Test
    @Disabled("migration to two compartment model")
    void shouldSimulateMembraneDiffusion() {

        Simulation simulation = new Simulation();
        Environment.setSystemExtend(Quantities.getQuantity(1, MICRO(METRE)));

        final AutomatonGraph automatonGraph = AutomatonGraphs.singularGraph();
        simulation.setGraph(automatonGraph);

        AutomatonNode membraneNode = automatonGraph.getNode(0, 0);
        membraneNode.setCellRegion(CELL_OUTER_MEMBRANE_REGION);
        membraneNode.getConcentrationContainer().initialize(CELL_OUTER_MEMBRANE_REGION.getInnerSubsection(), water, Quantities.getQuantity(2.0, MOLE_PER_LITRE).to(getConcentrationUnit()));
        membraneNode.getConcentrationContainer().initialize(CELL_OUTER_MEMBRANE_REGION.getOuterSubsection(), water, Quantities.getQuantity(1.0, MOLE_PER_LITRE).to(getConcentrationUnit()));
        automatonGraph.addNode(membraneNode);
        List<Membrane> membranes = MembraneTracer.regionsToMembrane(automatonGraph);
        MembraneLayer layer = new MembraneLayer();
        layer.addMembranes(membranes);
        simulation.setMembraneLayer(layer);

        MembraneDiffusion.inSimulation(simulation)
                .cargo(water)
                .build();

        simulation.nextEpoch();
        // delta should be about 3.5e-20 mol/um3
        ComparableQuantity<MolarConcentration> expectedLeft = Quantities.getQuantity(2.0, MOLE_PER_LITRE).to(getConcentrationUnit()).subtract(Quantities.getQuantity(3.5e-11, getConcentrationUnit()));
        ComparableQuantity<MolarConcentration> expectedRight = Quantities.getQuantity(1.0, MOLE_PER_LITRE).to(getConcentrationUnit()).add(Quantities.getQuantity(3.5e-11, getConcentrationUnit()));
        assertEquals(expectedLeft.getValue().doubleValue(), membraneNode.getConcentrationContainer().get(CELL_OUTER_MEMBRANE_REGION.getInnerSubsection(), water), 1e-12);
        assertEquals(expectedRight.getValue().doubleValue(), membraneNode.getConcentrationContainer().get(CELL_OUTER_MEMBRANE_REGION.getOuterSubsection(), water), 1e-12);
    }

    @Test
    void testConversionOfArea() {

        setSpace(Quantities.getQuantity(2, MICRO(METRE)));
        setTime(Quantities.getQuantity(1, MICRO(SECOND)));

        MembranePermeability membranePermeability = new MembranePermeability(Quantities.getQuantity(3.5E-03, CENTIMETRE_PER_SECOND), Evidence.NO_EVIDENCE);
        membranePermeability.scale();
        double scaledQuantity = membranePermeability.getScaledQuantity();
        Quantity<MolarConcentration> concentration = Quantities.getQuantity(0.1, MOLE_PER_LITRE).to(UnitRegistry.getConcentrationUnit());

        double result = scaledQuantity * concentration.getValue().doubleValue() * getArea().getValue().doubleValue();

        assertEquals(7.0E-6, Quantities.getQuantity(result, UnitRegistry.getConcentrationUnit()).to(MOLE_PER_LITRE).getValue().doubleValue(), 1.0E-16);

    }

    @Test
    void shouldDiffuseFromVesicle() {

        ComparableQuantity<Length> systemExtend = Quantities.getQuantity(20, MICRO(METRE));
        Environment.setSystemExtend(systemExtend);
        Environment.setSimulationExtend(500);
        Environment.setNodeSpacingToDiameter(systemExtend, 10);

        Simulation simulation = new Simulation();

        Vesicle vesicle = new Vesicle(new Vector2D(20, 20), Quantities.getQuantity(ThreadLocalRandom.current().nextDouble(100, 200), NANO(METRE)));

        vesicle.getConcentrationContainer().initialize(OUTER, water, Quantities.getQuantity(50.0, MOLE_PER_LITRE));

        // add vesicle transport layer
        VesicleLayer vesicleLayer = new VesicleLayer(simulation);
        vesicleLayer.addVesicle(vesicle);
        simulation.setVesicleLayer(vesicleLayer);

        // define graphs
        AutomatonGraph graph = AutomatonGraphs.createRectangularAutomatonGraph(10, 10);
        simulation.setGraph(graph);

        for (AutomatonNode node : graph.getNodes()) {
            node.setCellRegion(CYTOPLASM_REGION);
            node.getConcentrationContainer().initialize(INNER, water, Quantities.getQuantity(40.0, MOLE_PER_LITRE));
        }

        // add diffusion
        MembraneDiffusion.inSimulation(simulation)
                .cargo(water)
                .build();

        // vesicleLayer.addVesicleModule(new VesicleDiffusion(simulation));
        // simulate a couple of epochs
        AutomatonNode node = graph.getNode(0, 0);
        double previousVesicleConcentration = UnitRegistry.convert(Quantities.getQuantity(50.0, MOLE_PER_LITRE)).getValue().doubleValue();
        double previousNodeConcentration = 0.0;
        for (int i = 0; i < 10; i++) {
            simulation.nextEpoch();
            // node increasing
            double currentNodeConcentration = node.getConcentrationContainer().get(INNER, water);
            assertTrue(currentNodeConcentration > previousNodeConcentration);
            previousNodeConcentration = currentNodeConcentration;
            // vesicle decreasing
            double currentVesicleConcentration = vesicle.getConcentrationContainer().get(CellTopology.OUTER, water);
            assertTrue(currentVesicleConcentration < previousVesicleConcentration);
            previousVesicleConcentration = currentVesicleConcentration;
        }
    }

}