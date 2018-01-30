package de.bioforscher.singa.simulation.modules.model;

import de.bioforscher.singa.chemistry.descriptive.entities.Enzyme;
import de.bioforscher.singa.chemistry.descriptive.entities.Species;
import de.bioforscher.singa.chemistry.descriptive.features.databases.chebi.ChEBIParserService;
import de.bioforscher.singa.chemistry.descriptive.features.databases.pubchem.PubChemParserService;
import de.bioforscher.singa.chemistry.descriptive.features.diffusivity.Diffusivity;
import de.bioforscher.singa.chemistry.descriptive.features.reactions.MichaelisConstant;
import de.bioforscher.singa.chemistry.descriptive.features.reactions.TurnoverNumber;
import de.bioforscher.singa.features.parameters.EnvironmentalParameters;
import de.bioforscher.singa.mathematics.geometry.faces.Rectangle;
import de.bioforscher.singa.mathematics.graphs.model.Graphs;
import de.bioforscher.singa.mathematics.graphs.model.GridCoordinateConverter;
import de.bioforscher.singa.mathematics.vectors.Vector2D;
import de.bioforscher.singa.simulation.features.permeability.MembraneEntry;
import de.bioforscher.singa.simulation.features.permeability.MembraneExit;
import de.bioforscher.singa.simulation.features.permeability.MembraneFlipFlop;
import de.bioforscher.singa.simulation.model.compartments.EnclosedCompartment;
import de.bioforscher.singa.simulation.model.compartments.Membrane;
import de.bioforscher.singa.simulation.model.concentrations.MembraneContainer;
import de.bioforscher.singa.simulation.model.graphs.AutomatonGraph;
import de.bioforscher.singa.simulation.model.graphs.AutomatonGraphs;
import de.bioforscher.singa.simulation.model.graphs.AutomatonNode;
import de.bioforscher.singa.simulation.modules.reactions.implementations.EquilibriumReaction;
import de.bioforscher.singa.simulation.modules.reactions.implementations.MichaelisMentenReaction;
import de.bioforscher.singa.simulation.modules.reactions.implementations.NthOrderReaction;
import de.bioforscher.singa.simulation.modules.reactions.model.ReactantRole;
import de.bioforscher.singa.simulation.modules.reactions.model.StoichiometricReactant;
import de.bioforscher.singa.simulation.modules.transport.FreeDiffusion;
import de.bioforscher.singa.simulation.modules.transport.PassiveMembraneTransport;
import de.bioforscher.singa.simulation.parser.sbml.BioModelsParserService;
import de.bioforscher.singa.simulation.parser.sbml.SBMLParser;
import de.bioforscher.singa.structure.features.molarmass.MolarMass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tec.uom.se.quantity.Quantities;

import javax.measure.Quantity;
import javax.measure.quantity.Time;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static de.bioforscher.singa.chemistry.descriptive.features.reactions.TurnoverNumber.PER_MINUTE;
import static de.bioforscher.singa.chemistry.descriptive.features.reactions.TurnoverNumber.PER_SECOND;
import static de.bioforscher.singa.features.model.FeatureOrigin.MANUALLY_ANNOTATED;
import static de.bioforscher.singa.features.units.UnitProvider.MOLE_PER_LITRE;
import static tec.uom.se.unit.MetricPrefix.MILLI;
import static tec.uom.se.unit.MetricPrefix.NANO;
import static tec.uom.se.unit.Units.METRE;
import static tec.uom.se.unit.Units.SECOND;

/**
 * A factory class that can be used to create different examples to test and explore certain aspects to the api.
 *
 * @author cl
 */
public class SimulationExamples {

    private static final Logger logger = LoggerFactory.getLogger(SimulationExamples.class);

    private static final Rectangle defaultBoundingBox = new Rectangle(new Vector2D(0, 600), new Vector2D(600, 0));

    /**
     * This simulation simulates the thermal decomposition of dinitrogen pentaoxide. From: Brauer, G. (2012). Handbook
     * of preparative inorganic chemistry, volume 2. Elsevier. 489–491.
     *
     * @return The ready to go simulation.
     */
    public static Simulation createDecompositionReactionExample() {
        // setup simulation
        Simulation simulation = new Simulation();
        // get required species
        Species dinitrogenPentaoxide = ChEBIParserService.parse("CHEBI:29802");
        Species nitrogenDioxide = ChEBIParserService.parse("CHEBI:33101");
        Species oxygen = ChEBIParserService.parse("CHEBI:15379");

        // setup graph with a single node
        AutomatonGraph graph = AutomatonGraphs.useStructureFrom(Graphs.buildLinearGraph(1, defaultBoundingBox));

        // initialize species in graph with desired concentration
        graph.initializeSpeciesWithConcentration(dinitrogenPentaoxide, 0.02);
        graph.initializeSpeciesWithConcentration(nitrogenDioxide, 0.0);
        graph.initializeSpeciesWithConcentration(oxygen, 0.0);

        // setup time step size
        EnvironmentalParameters.setTimeStep(Quantities.getQuantity(10.0, MILLI(SECOND)));

        // create reaction
        NthOrderReaction reaction = new NthOrderReaction(simulation, Quantities.getQuantity(0.07, PER_SECOND));
        reaction.setElementary(true);
        reaction.getStoichiometricReactants().addAll(Arrays.asList(
                new StoichiometricReactant(dinitrogenPentaoxide, ReactantRole.DECREASING, 2),
                new StoichiometricReactant(nitrogenDioxide, ReactantRole.INCREASING, 4),
                new StoichiometricReactant(oxygen, ReactantRole.INCREASING)
        ));

        // add graph
        simulation.setGraph(graph);
        // add the reaction
        simulation.getModules().add(reaction);

        return simulation;
    }

    /**
     * This simulation simulates the synthesis of 1,3,5-octatriene (C8H12) from Buta-1,3-diene (C4H6).
     *
     * @return The ready to go simulation.
     */
    public static Simulation createSynthesisReactionExample() {
        // setup simulation
        Simulation simulation = new Simulation();
        // get required species
        Species butadiene = ChEBIParserService.parse("CHEBI:39478");
        Species octatriene = ChEBIParserService.parse("CHEBI:77504");

        // setup graph with a single node
        AutomatonGraph graph = AutomatonGraphs.useStructureFrom(Graphs.buildLinearGraph(1, defaultBoundingBox));

        // initialize species in graph with desired concentration
        graph.initializeSpeciesWithConcentration(butadiene, 0.02);
        graph.initializeSpeciesWithConcentration(octatriene, 0.0);

        // setup time step size
        EnvironmentalParameters.setTimeStep(Quantities.getQuantity(1.0, SECOND));

        // create reaction
        NthOrderReaction reaction = new NthOrderReaction(simulation, Quantities.getQuantity(0.614, PER_SECOND));
        reaction.setElementary(false);
        reaction.getStoichiometricReactants().addAll(Arrays.asList(
                new StoichiometricReactant(butadiene, ReactantRole.DECREASING, 2, 2),
                new StoichiometricReactant(octatriene, ReactantRole.INCREASING)
        ));


        // add graph
        simulation.setGraph(graph);
        // add the reaction module
        simulation.getModules().add(reaction);

        return simulation;
    }

    /**
     * This simulation simulates a equilibrium reaction.
     *
     * @return The ready to go simulation.
     */
    public static Simulation createEquilibriumReactionExample() {
        // setup simulation
        Simulation simulation = new Simulation();

        // set up arbitrary species
        Species speciesA = new Species.Builder("CHEBI:00001")
                .name("A")
                .build();

        Species speciesB = new Species.Builder("CHEBI:00002")
                .name("B")
                .build();

        // setup graph with a single node
        AutomatonGraph graph = AutomatonGraphs.useStructureFrom(Graphs.buildLinearGraph(1, defaultBoundingBox));

        // initialize species in graph with desired concentration
        graph.initializeSpeciesWithConcentration(speciesA, 1.0);
        graph.initializeSpeciesWithConcentration(speciesB, 0.0);

        // setup time step size
        EnvironmentalParameters.setTimeStep(Quantities.getQuantity(10.0, MILLI(SECOND)));

        // create reaction
        EquilibriumReaction reaction = new EquilibriumReaction(simulation, Quantities.getQuantity(10, PER_SECOND),
                Quantities.getQuantity(10, PER_SECOND));
        reaction.setElementary(true);
        reaction.getStoichiometricReactants().addAll(Arrays.asList(
                new StoichiometricReactant(speciesA, ReactantRole.DECREASING),
                new StoichiometricReactant(speciesB, ReactantRole.INCREASING)
        ));

        // add graph
        simulation.setGraph(graph);
        // add the reaction module
        simulation.getModules().add(reaction);

        return simulation;
    }

    /**
     * This simulation simulates a {@link MichaelisMentenReaction}, where D-Fructose 1-phosphate is convertet to
     * glycerone phosphate and D-glyceraldehyde using fructose bisphosphate aldolase. From: Callens, M. et al. (1991).
     * Kinetic properties of fructose bisphosphate aldolase from Trypanosoma brucei compared to aldolase from rabbit
     * muscle and Staphylococcus aureus. Sabio-RK pdbIdentifier: 28851
     *
     * @return The ready to go simulation.
     */
    public static Simulation createMichaelisMentenReactionExample() {

        // setup simulation
        Simulation simulation = new Simulation();
        // get required species
        Species fructosePhosphate = ChEBIParserService.parse("CHEBI:18105");
        Species glyceronePhosphate = ChEBIParserService.parse("CHEBI:16108");
        Species glyceraldehyde = ChEBIParserService.parse("CHEBI:17378");

        // setup enzyme
        Enzyme aldolase = new Enzyme.Builder("P07752")
                .name("Fructose-bisphosphate aldolase")
                .addSubstrate(fructosePhosphate)
                .assignFeature(new MolarMass(82142, MANUALLY_ANNOTATED))
                .assignFeature(new MichaelisConstant(Quantities.getQuantity(9.0e-3, MOLE_PER_LITRE), MANUALLY_ANNOTATED))
                .assignFeature(new TurnoverNumber(Quantities.getQuantity(76, PER_MINUTE), MANUALLY_ANNOTATED))
                .build();

        // setup graph with a single node
        AutomatonGraph graph = AutomatonGraphs.useStructureFrom(Graphs.buildLinearGraph(1, defaultBoundingBox));

        // initialize species in graph with desired concentration
        graph.initializeSpeciesWithConcentration(fructosePhosphate, 0.1);
        graph.initializeSpeciesWithConcentration(aldolase, 0.2);
        graph.initializeSpeciesWithConcentration(glyceronePhosphate, 0.0);
        graph.initializeSpeciesWithConcentration(glyceraldehyde, 0.0);

        // setup time step size
        EnvironmentalParameters.setTimeStep(Quantities.getQuantity(1.0, MILLI(SECOND)));

        // create reaction using the properties of the enzyme
        MichaelisMentenReaction reaction = new MichaelisMentenReaction(simulation, aldolase);
        reaction.getStoichiometricReactants().addAll(Arrays.asList(
                new StoichiometricReactant(fructosePhosphate, ReactantRole.DECREASING),
                new StoichiometricReactant(glyceronePhosphate, ReactantRole.INCREASING),
                new StoichiometricReactant(glyceraldehyde, ReactantRole.INCREASING)
        ));

        // add graph
        simulation.setGraph(graph);
        // add the reactions module
        simulation.getModules().add(reaction);

        return simulation;
    }

    /**
     * This simulation simulates a diffusion of small molecules in a rectangular gird graph.
     *
     * @param numberOfNodes The number of nodes on one "side" of the rectangle.
     * @param timeStep The size of the time step.
     * @return The ready to go simulation.
     */
    public static Simulation createDiffusionModuleExample(int numberOfNodes, Quantity<Time> timeStep) {

        // get required species
        Species methanol = ChEBIParserService.parse("CHEBI:17790");
        methanol.setFeature(Diffusivity.class);
        Species ethyleneGlycol = ChEBIParserService.parse("CHEBI:30742");
        ethyleneGlycol.setFeature(Diffusivity.class);
        Species valine = ChEBIParserService.parse("CHEBI:27266");
        valine.setFeature(Diffusivity.class);
        Species sucrose = ChEBIParserService.parse("CHEBI:17992");
        sucrose.setFeature(Diffusivity.class);

        // setup rectangular graph with number of nodes
        AutomatonGraph graph = AutomatonGraphs.useStructureFrom(Graphs.buildGridGraph(numberOfNodes, numberOfNodes, defaultBoundingBox));

        // initialize species in graph with desired concentration leaving the right "half" empty
        for (AutomatonNode node : graph.getNodes()) {
            if (node.getIdentifier() % numberOfNodes < numberOfNodes / 2) {
                node.setConcentration(methanol, 1);
                node.setConcentration(ethyleneGlycol, 1);
                node.setConcentration(valine, 1);
                node.setConcentration(sucrose, 1);
            } else {
                node.setConcentration(methanol, 0);
                node.setConcentration(ethyleneGlycol, 0);
                node.setConcentration(valine, 0);
                node.setConcentration(sucrose, 0);
            }
        }

        // setup time step size as given
        EnvironmentalParameters.setTimeStep(timeStep);
        // setup node distance to diameter / (numberOfNodes - 1)
        EnvironmentalParameters.setNodeSpacingToDiameter(Quantities.getQuantity(2500.0, NANO(METRE)), numberOfNodes);

        // setup simulation
        Simulation simulation = new Simulation();
        // add graph
        simulation.setGraph(graph);
        // add diffusion module
        simulation.getModules().add(new FreeDiffusion(simulation));
        // add desired species to the simulation for easy access
        simulation.getChemicalEntities().addAll(Arrays.asList(methanol, ethyleneGlycol, valine, sucrose));

        return simulation;
    }

    /**
     * This simulation simulates a multiple reactions involving iodine.
     *
     * @return The ready to go simulation.
     */
    public static Simulation createIodineMultiReactionExample() {

        // setup simulation
        Simulation simulation = new Simulation();
        logger.info("Setting up the passive membrane diffusion example ...");
        // get required species
        logger.debug("Importing species ...");
        // Hydron (H+)
        Species hydron = ChEBIParserService.parse("CHEBI:15378");
        // Iodide (I-)
        Species iodide = ChEBIParserService.parse("CHEBI:16382");
        // Diiodine (I2)
        Species diiodine = ChEBIParserService.parse("CHEBI:17606");
        // Water (H2O)
        Species water = ChEBIParserService.parse("CHEBI:15377");
        // Hypoiodous acid (HOI)
        Species hia = ChEBIParserService.parse("CHEBI:29231");
        // Iodous acid (HIO2)
        Species ia = ChEBIParserService.parse("CHEBI:29229");
        // Iodine dioxide (IO2)
        Species iodineDioxid = ChEBIParserService.parse("CHEBI:29901");
        // Iodate (IO3-)
        Species iodate = ChEBIParserService.parse("CHEBI:29226");

        logger.debug("Setting up example graph ...");
        // setup graph with a single node
        AutomatonGraph graph = AutomatonGraphs.useStructureFrom(
                Graphs.buildLinearGraph(1, defaultBoundingBox));
        // initialize species in graph with desired concentration
        logger.debug("Initializing starting concentrations of species and node states in graph ...");
        graph.getNode(0).setConcentrations(0.05, hydron, iodide, diiodine, water, hia, ia, iodineDioxid, iodate);

        // setup time step size
        logger.debug("Adjusting time step size ... ");
        EnvironmentalParameters.setTimeStep(Quantities.getQuantity(1.0, MILLI(SECOND)));

        logger.debug("Composing simulation ... ");

        // create first reaction
        NthOrderReaction firstReaction = new NthOrderReaction(simulation, Quantities.getQuantity(1.43e3, PER_SECOND));
        firstReaction.setElementary(true);
        firstReaction.getStoichiometricReactants().addAll(Arrays.asList(
                new StoichiometricReactant(hydron, ReactantRole.DECREASING, 2),
                new StoichiometricReactant(iodide, ReactantRole.DECREASING),
                new StoichiometricReactant(iodate, ReactantRole.DECREASING),
                new StoichiometricReactant(hia, ReactantRole.INCREASING),
                new StoichiometricReactant(ia, ReactantRole.INCREASING)
        ));

        // create second reaction
        NthOrderReaction secondReaction = new NthOrderReaction(simulation, Quantities.getQuantity(2.0e4, PER_SECOND));
        secondReaction.setElementary(true);
        secondReaction.getStoichiometricReactants().addAll(Arrays.asList(
                new StoichiometricReactant(hydron, ReactantRole.DECREASING),
                new StoichiometricReactant(ia, ReactantRole.DECREASING),
                new StoichiometricReactant(iodide, ReactantRole.DECREASING),
                new StoichiometricReactant(hia, ReactantRole.INCREASING)
        ));

        // create second reaction
        EquilibriumReaction thirdReaction = new EquilibriumReaction(simulation, Quantities.getQuantity(3.1e4, PER_SECOND),
                Quantities.getQuantity(2.2, PER_SECOND));
        thirdReaction.setElementary(true);
        thirdReaction.getStoichiometricReactants().addAll(Arrays.asList(
                new StoichiometricReactant(hia, ReactantRole.DECREASING),
                new StoichiometricReactant(iodide, ReactantRole.DECREASING),
                new StoichiometricReactant(hydron, ReactantRole.DECREASING),
                new StoichiometricReactant(diiodine, ReactantRole.INCREASING),
                new StoichiometricReactant(water, ReactantRole.INCREASING)
        ));

        // add reactions
        simulation.getModules().addAll(Arrays.asList(firstReaction, secondReaction, thirdReaction));

        // add graph
        simulation.setGraph(graph);
        // add the reactions module

        return simulation;
    }

    public static Simulation createSimulationFromSBML() {
        // setup simulation
        Simulation simulation = new Simulation();
        // BIOMD0000000023
        // BIOMD0000000064
        // BIOMD0000000184 for ca oscillations

        logger.info("Setting up simulation for model BIOMD0000000184 ...");
        SBMLParser model = BioModelsParserService.parseModelById("BIOMD0000000184");

        logger.debug("Setting up example graph ...");
        // setup graph with a single node
        AutomatonGraph graph = AutomatonGraphs.useStructureFrom(
                Graphs.buildLinearGraph(1, defaultBoundingBox));

        model.getCompartments().keySet().forEach(graph::addCellSection);

        // initialize species in graph with desired concentration
        logger.debug("Initializing starting concentrations of species and node states in graph ...");
        AutomatonNode bioNode = graph.getNodes().iterator().next();
        model.getStartingConcentrations().forEach((entity, value) -> {
            logger.debug("Initialized concentration of {} to {}.", entity.getIdentifier(), value);
            bioNode.setConcentration(entity, value);
        });

        // setup time step size
        logger.debug("Adjusting time step size ... ");
        EnvironmentalParameters.setTimeStep(Quantities.getQuantity(1.0, SECOND));

        // add graph
        simulation.setGraph(graph);
        // add reaction to the reactions used in the simulations
        model.getReactions().forEach(reaction -> reaction.setSimulation(simulation));
        simulation.getModules().addAll(model.getReactions());
        // add, sort and apply assignment rules
        simulation.setAssignmentRules(new ArrayList<>(model.getAssignmentRules()));
        simulation.applyAssignmentRules();

        return simulation;
    }

    public static Simulation createCompartmentTestEnvironment() {

        logger.info("Setting up Compartment Test Example ...");
        // setup rectangular graph with number of nodes
        logger.debug("Setting up example graph ...");
        int numberOfNodes = 50;
        AutomatonGraph graph = AutomatonGraphs.useStructureFrom(Graphs.buildGridGraph(
                numberOfNodes, numberOfNodes, defaultBoundingBox, false));
        // setup simulation
        logger.debug("Composing simulation ... ");
        Simulation simulation = new Simulation();
        // add graph
        simulation.setGraph(graph);

        return simulation;
    }

    public static Simulation createPassiveMembraneTransportExample() {
        logger.info("Setting up the passive membrane diffusion example ...");
        // get required species
        logger.debug("Importing species ...");

        // all species
        Set<Species> allSpecies = new HashSet<>();
        // Domperidone
        Species domperidone = PubChemParserService.parse("CID:3151");
        domperidone.setFeature(new MembraneEntry(1.48e9, MANUALLY_ANNOTATED));
        domperidone.setFeature(new MembraneExit(1.76e3, MANUALLY_ANNOTATED));
        domperidone.setFeature(new MembraneFlipFlop(3.50e2, MANUALLY_ANNOTATED));
        allSpecies.add(domperidone);
        // Loperamide
        Species loperamide = PubChemParserService.parse("CID:3955");
        loperamide.setFeature(new MembraneEntry(8.59e8, MANUALLY_ANNOTATED));
        loperamide.setFeature(new MembraneExit(1.81e3, MANUALLY_ANNOTATED));
        loperamide.setFeature(new MembraneFlipFlop(6.71e5, MANUALLY_ANNOTATED));
        allSpecies.add(loperamide);
        // Propranolol
        Species propranolol = PubChemParserService.parse("CID:4946");
        propranolol.setFeature(new MembraneEntry(1.27e9, MANUALLY_ANNOTATED));
        propranolol.setFeature(new MembraneExit(3.09e4, MANUALLY_ANNOTATED));
        propranolol.setFeature(new MembraneFlipFlop(4.75e6, MANUALLY_ANNOTATED));
        allSpecies.add(propranolol);
        // Desipramine
        Species desipramine = PubChemParserService.parse("CID:2995");
        desipramine.setFeature(new MembraneEntry(2.13e9, MANUALLY_ANNOTATED));
        desipramine.setFeature(new MembraneExit(4.86e4, MANUALLY_ANNOTATED));
        desipramine.setFeature(new MembraneFlipFlop(1.09e7, MANUALLY_ANNOTATED));
        allSpecies.add(desipramine);

        // setup rectangular graph with number of nodes
        logger.debug("Setting up example graph ...");
        GridCoordinateConverter converter = new GridCoordinateConverter(11, 11);
        AutomatonGraph graph = AutomatonGraphs.createRectangularAutomatonGraph(converter.getNumberOfColumns(), converter.getNumberOfRows());

        EnclosedCompartment left = new EnclosedCompartment("LC", "Left");
        EnclosedCompartment right = new EnclosedCompartment("RC", "Right");

        AutomatonGraphs.splitRectangularGraphWithMembrane(graph, converter, right, left);

        // set concentrations
        // only 5 left most nodes
        for (AutomatonNode node : graph.getNodes()) {
            if (node.getIdentifier() % converter.getNumberOfColumns() < 5) {
                for (Species species : allSpecies) {
                    node.setConcentration(species, 1.0);
                }
            } else {
                for (Species species : allSpecies) {
                    node.setConcentration(species, 0.0);
                }
            }
        }

        // setup time step size as given
        logger.debug("Adjusting time step size ... ");
        EnvironmentalParameters.setTimeStep(Quantities.getQuantity(100, NANO(SECOND)));
        // setup node distance to diameter
        logger.debug("Adjusting spatial step size ... ");
        EnvironmentalParameters.setNodeSpacingToDiameter(Quantities.getQuantity(2500.0, NANO(METRE)), converter.getNumberOfColumns());

        // setup simulation
        logger.debug("Composing simulation ... ");
        Simulation simulation = new Simulation();
        // add graph
        simulation.setGraph(graph);
        // add diffusion module
        simulation.getModules().add(new FreeDiffusion(simulation));
        // add transmembrane transport
        simulation.getModules().add(new PassiveMembraneTransport(simulation));
        // add desired species to the simulation for easy access
        simulation.getChemicalEntities().addAll(allSpecies);
        return simulation;
    }

    public static Simulation createDiffusionAndMembraneTransportExample() {
        Species domperidone = new Species.Builder("CHEBI:31515")
                .name("domperidone")
                .assignFeature(Diffusivity.class)
                .assignFeature(new MembraneEntry(1.48e9, MANUALLY_ANNOTATED))
                .assignFeature(new MembraneExit(1.76e3, MANUALLY_ANNOTATED))
                .assignFeature(new MembraneFlipFlop(3.50e2, MANUALLY_ANNOTATED))
                .build();

        Simulation simulation = new Simulation();
        GridCoordinateConverter gcc = new GridCoordinateConverter(30, 20);
        // setup rectangular graph with number of nodes
        AutomatonGraph graph = AutomatonGraphs.useStructureFrom(Graphs.buildGridGraph(
                20, 30, defaultBoundingBox, false));
        // create compartments and membrane
        EnclosedCompartment inner = new EnclosedCompartment("I", "Inner");
        EnclosedCompartment outer = new EnclosedCompartment("O", "Outer");
        Membrane membrane = Membrane.forCompartment(inner);
        // initialize species in graph with desired concentration
        for (AutomatonNode node : graph.getNodes()) {
            Vector2D coordinate = gcc.convert(node.getIdentifier());
            if ((coordinate.getX() == 2 && coordinate.getY() > 2 && coordinate.getY() < 17) ||
                    (coordinate.getX() == 27 && coordinate.getY() > 2 && coordinate.getY() < 17) ||
                    (coordinate.getY() == 2 && coordinate.getX() > 1 && coordinate.getX() < 28) ||
                    (coordinate.getY() == 17 && coordinate.getX() > 1 && coordinate.getX() < 28)) {
                // setup membrane
                node.setCellSection(membrane);
                node.setConcentrationContainer(new MembraneContainer(outer, inner, membrane));
                node.setAvailableConcentration(domperidone, outer, Quantities.getQuantity(1.0, MOLE_PER_LITRE));
            } else if (coordinate.getX() > 2 && coordinate.getY() > 2 && coordinate.getX() < 27 && coordinate.getY() < 17) {
                node.setCellSection(inner);
                node.setConcentration(domperidone, 0.0);
            } else {
                node.setCellSection(outer);
                node.setConcentration(domperidone, 1.0);
            }
        }

        simulation.setGraph(graph);

        FreeDiffusion diffusion = new FreeDiffusion(simulation);
        PassiveMembraneTransport membraneTransport = new PassiveMembraneTransport(simulation);

        simulation.getModules().add(diffusion);
        simulation.getModules().add(membraneTransport);

        return simulation;
    }


}
