package bio.singa.simulation.model.simulation;

import bio.singa.chemistry.entities.ChemicalEntity;
import bio.singa.chemistry.entities.simple.SmallMolecule;
import bio.singa.chemistry.features.reactions.RateConstant;
import bio.singa.features.units.UnitRegistry;
import bio.singa.simulation.model.graphs.AutomatonGraph;
import bio.singa.simulation.model.graphs.AutomatonGraphs;
import bio.singa.simulation.model.modules.concentration.imlementations.reactions.Reaction;
import bio.singa.simulation.model.modules.concentration.imlementations.reactions.ReactionBuilder;
import bio.singa.simulation.model.simulation.error.TimeStepManager;
import org.junit.jupiter.api.Test;
import tech.units.indriya.quantity.Quantities;

import java.util.ArrayList;
import java.util.List;

import static bio.singa.features.units.UnitProvider.MICRO_MOLE_PER_LITRE;
import static bio.singa.simulation.model.sections.CellTopology.INNER;
import static tech.units.indriya.unit.Units.SECOND;

/**
 * @author cl
 */
public class NumericalApproximationTest {

    @Test
    void testSimpleProblem() {

        double rate = 1.0;

        List<Double> tolerances = new ArrayList<>();
        tolerances.add(0.1);
        tolerances.add(0.05);
        tolerances.add(0.025);
        tolerances.add(0.01);
        tolerances.add(0.005);
        tolerances.add(0.0025);
        tolerances.add(0.001);

        for (Double tolerance : tolerances) {

            Simulation simulation = new Simulation();
            simulation.getScheduler().getErrorManager().setLocalNumericalTolerance(tolerance);

            AutomatonGraph automatonGraph = AutomatonGraphs.singularGraph();
            simulation.setGraph(automatonGraph);

            ChemicalEntity entityA = SmallMolecule.create("A").build();
            ChemicalEntity entityP = SmallMolecule.create("P").build();
            ChemicalEntity entityQ = SmallMolecule.create("Q").build();

            UnitRegistry.setTime(Quantities.getQuantity(1, SECOND));
            automatonGraph.getNodes().iterator().next().getConcentrationManager().getConcentrationContainer().initialize(INNER, entityA, Quantities.getQuantity(1.0, MICRO_MOLE_PER_LITRE));

            Reaction reaction = ReactionBuilder.staticReactants(simulation)
                    .addSubstrate(entityA, INNER)
                    .addProduct(entityP, INNER)
                    .addProduct(entityQ, INNER)
                    .irreversible()
                    .rate(RateConstant.create(rate).forward().firstOrder().timeUnit(SECOND).build())
                    .build();

            while (TimeStepManager.getElapsedTime().isLessThanOrEqualTo(Quantities.getQuantity(10, SECOND))) {
                simulation.nextEpoch();

                double concentrationNumerical = UnitRegistry.concentration(automatonGraph.getNodes().iterator().next().getConcentrationManager().getConcentrationContainer().get(INNER, entityP)).to(MICRO_MOLE_PER_LITRE).getValue().doubleValue();
                double time = TimeStepManager.getElapsedTime().to(SECOND).getValue().doubleValue();
                double concentrationAnalytical = 1.0 * (1.0 - Math.exp(-rate * time));
                double localError = simulation.getScheduler().getErrorManager().getGlobalNumericalError().getValue();
//                System.out.println(cutoff + ", " + time + ", " + concentrationNumerical + ", " + concentrationAnalytical + ", " + localError);

            }
        }

    }
}
