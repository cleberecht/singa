package de.bioforscher.simulation.modules.model.updates;

import de.bioforscher.chemistry.descriptive.ChemicalEntity;
import de.bioforscher.simulation.model.graphs.AutomatonGraph;
import de.bioforscher.simulation.model.graphs.BioNode;
import de.bioforscher.simulation.modules.reactions.model.Reaction;

/**
 * This {@link UpdateBehavior} calculates an update and applies it immediately. This behavior should by applied to
 * panes that don't require neighbor nodes to calculate their next state (e.g. most {@link
 * Reaction}s). Modules that implement {@link ImmediateUpdateBehavior} are mostly
 * "embarrassingly parallel".
 *
 * @see <a href="https://en.wikipedia.org/wiki/Embarrassingly_parallel">Wikipedia: Embarrassingly parallel</a>
 */
public interface ImmediateUpdateBehavior extends UpdateBehavior {

    /**
     * Updates every node in the graph at once.
     *
     * @param graph
     */
    @Override
    default void updateGraph(AutomatonGraph graph) {
        graph.getNodes().forEach(this::updateNode);
    }

    /**
     * Updates every chemical entity in the given node at once.
     *
     * @param node
     */
    default void updateNode(BioNode node) {
        node.getConcentrations().keySet().forEach(entity -> updateSpecies(node, entity));
    }

    /**
     * Updates the concentration of a species in a node with the value calculated by the {@link
     * #calculateUpdate(BioNode, ChemicalEntity)} method.
     *
     * @param node
     * @param entity
     */
    default void updateSpecies(BioNode node, ChemicalEntity entity) {
        node.setConcentration(entity, calculateUpdate(node, entity).getQuantity());
    }

}