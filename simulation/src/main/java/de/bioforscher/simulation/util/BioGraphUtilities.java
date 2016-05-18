package de.bioforscher.simulation.util;

import de.bioforscher.chemistry.descriptive.ChemicalEntity;
import de.bioforscher.mathematics.graphs.model.RegularNode;
import de.bioforscher.mathematics.graphs.model.UndirectedEdge;
import de.bioforscher.mathematics.graphs.model.UndirectedGraph;
import de.bioforscher.simulation.model.AutomatonGraph;
import de.bioforscher.simulation.model.BioNode;

import java.util.HashMap;
import java.util.Map;

public class BioGraphUtilities {

    /**
     * Creates and returns a map that contains all chemical entities that are present in the given graph as values and with the
     * name of the entity as key.
     *
     * @param graph A graph with species
     * @return All chemical entities in the graph.
     */
    public static Map<String, ChemicalEntity> generateMapOfEntities(AutomatonGraph graph) {
        Map<String, ChemicalEntity> results = new HashMap<>();
        for (BioNode node : graph.getNodes()) {
            for (ChemicalEntity entity : node.getConcentrations().keySet()) {
                results.put(entity.getName(), entity);
            }
        }
        return results;
    }

    /**
     * Populates the given graph with the given chemical entity in the desired concentration.
     *
     * @param graph         A graph to populate
     * @param entity        A chemical entity
     * @param concentration The desired concentration.
     */
    public static void fillGraphWithSpecies(AutomatonGraph graph, ChemicalEntity entity, double concentration) {
        for (BioNode node : graph.getNodes()) {
            node.addEntity(entity, concentration);
        }
    }

    /**
     * Casts a {@link UndirectedGraph} with nodes and edges to a {@link AutomatonGraph}. No new data is
     * generated. Indices are persistent. Both Graphs are independently modifiable.
     *
     * @param undirectedGraph The graph to be cast
     * @return The generated automaton graph.
     */
    public static AutomatonGraph castUndirectedGraphToBioGraph(UndirectedGraph undirectedGraph) {

        AutomatonGraph bioGraph = new AutomatonGraph();

        for (RegularNode regularNode : undirectedGraph.getNodes()) {
            int id = regularNode.getIdentifier();
            BioNode bioNode = new BioNode(id);
            bioNode.setPosition(regularNode.getPosition());
            bioGraph.addNode(bioNode);
        }

        for (UndirectedEdge undirectedEdge : undirectedGraph.getEdges()) {
            int id = undirectedEdge.getIdentifier();
            bioGraph.connect(id, bioGraph.getNode(undirectedEdge.getSource().getIdentifier()),
                    bioGraph.getNode(undirectedEdge.getTarget().getIdentifier()));
        }
        return bioGraph;
    }


}
