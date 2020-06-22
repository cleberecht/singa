package bio.singa.simulation.export.format;

import bio.singa.chemistry.features.reactions.MichaelisConstant;
import bio.singa.chemistry.features.reactions.RateConstant;
import bio.singa.chemistry.features.reactions.TurnoverNumber;
import bio.singa.features.model.Evidence;
import bio.singa.simulation.entities.ChemicalEntity;
import bio.singa.simulation.entities.SimpleEntity;
import bio.singa.simulation.model.modules.concentration.imlementations.reactions.Reaction;
import bio.singa.simulation.model.modules.concentration.imlementations.reactions.ReactionBuilder;
import bio.singa.simulation.model.simulation.Simulation;
import org.junit.jupiter.api.Test;
import tech.units.indriya.quantity.Quantities;
import tech.units.indriya.unit.ProductUnit;

import static bio.singa.features.units.UnitProvider.MOLE_PER_LITRE;
import static bio.singa.simulation.model.sections.CellTopology.MEMBRANE;
import static bio.singa.simulation.model.sections.CellTopology.OUTER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static tech.units.indriya.AbstractUnit.ONE;
import static tech.units.indriya.unit.Units.MINUTE;
import static tech.units.indriya.unit.Units.SECOND;

/**
 * @author cl
 */
class FormatReactionKineticsTest {

    @Test
    void testMichaelisMentenFormat() {
        Simulation simulation = new Simulation();

        MichaelisConstant michaelisConstant = new MichaelisConstant(Quantities.getQuantity(9.0e-3, MOLE_PER_LITRE), Evidence.NO_EVIDENCE);
        TurnoverNumber turnoverNumber = new TurnoverNumber(76, new ProductUnit<>(ONE.divide(MINUTE)), Evidence.NO_EVIDENCE);

        ChemicalEntity substrate = SimpleEntity.create("A").build();
        ChemicalEntity product1 = SimpleEntity.create("B").build();
        ChemicalEntity product2 = SimpleEntity.create("C").build();
        ChemicalEntity enzyme = SimpleEntity.create("E").build();

        Reaction reaction = ReactionBuilder.staticReactants(simulation)
                .addSubstrate(substrate)
                .addCatalyst(enzyme)
                .addProduct(product1)
                .addProduct(product2)
                .michaelisMenten()
                .michaelisConstant(michaelisConstant)
                .turnover(turnoverNumber)
                .build();

        assertEquals("$\\frac{k_{\\text{cat}} \\cdot [\\text{E}] \\cdot [\\text{A}]}{K_m \\cdot [\\text{A}]}$", FormatReactionKinetics.formatTex(reaction).get(0));
    }

    @Test
    void testNthOrderReactionFormat() {
        Simulation simulation = new Simulation();

        ChemicalEntity substrate1 = SimpleEntity.create("A").build();
        ChemicalEntity substrate2 = SimpleEntity.create("B").build();
        ChemicalEntity product1 = SimpleEntity.create("C").build();
        ChemicalEntity product2 = SimpleEntity.create("D").build();

        RateConstant k = RateConstant.create(1.0)
                .forward().secondOrder()
                .concentrationUnit(MOLE_PER_LITRE)
                .timeUnit(SECOND)
                .build();

        Reaction reaction = ReactionBuilder.staticReactants(simulation)
                .addSubstrate(substrate1, 2)
                .addSubstrate(substrate2)
                .addProduct(product1)
                .addProduct(product2)
                .irreversible()
                .rate(k)
                .build();

       assertEquals("$k_{1} \\cdot [\\text{A}] \\cdot [\\text{B}]$", FormatReactionKinetics.formatTex(reaction).get(0));
    }

    @Test
    void testReversibleReactionFormat() {
        Simulation simulation = new Simulation();

        ChemicalEntity substrate = SimpleEntity.create("A").build();
        ChemicalEntity product = SimpleEntity.create("B").build();

        RateConstant kf = RateConstant.create(2.0)
                .forward().firstOrder()
                .timeUnit(SECOND)
                .build();

        RateConstant kb = RateConstant.create(1.0)
                .backward().firstOrder()
                .timeUnit(SECOND)
                .build();

        Reaction reaction = ReactionBuilder.staticReactants(simulation)
                .addSubstrate(substrate, MEMBRANE)
                .addProduct(product, OUTER)
                .reversible()
                .forwardReactionRate(kf)
                .backwardReactionRate(kb)
                .build();
        assertEquals("$k_{1} \\cdot [\\text{A}] - k_{-1} \\cdot [\\text{B}]$", FormatReactionKinetics.formatTex(reaction).get(0));
    }

}