package de.bioforscher.singa.chemistry.parser.plip;

import de.bioforscher.singa.chemistry.physical.branches.Chain;
import de.bioforscher.singa.chemistry.physical.leaves.LeafSubstructure;
import de.bioforscher.singa.mathematics.algorithms.graphs.NeighbourhoodExtractor;
import de.bioforscher.singa.mathematics.graphs.model.GenericGraph;
import de.bioforscher.singa.mathematics.graphs.model.GenericNode;

import java.util.*;
import java.util.stream.Collectors;

public class PlipShellGenerator {

    private final Chain chain;
    private final LeafSubstructure<?, ?> reference;
    private final InteractionContainer interChainInteractions;
    private final InteractionContainer referenceInteractions;
    private final Map<InteractionShell, List<LeafSubstructure<?, ?>>> shells;

    private GenericGraph<LeafSubstructure<?, ?>> graph;

    private PlipShellGenerator(Chain chain, LeafSubstructure<?, ?> reference, InteractionContainer interChainInteractions,
                               InteractionContainer referenceInteractions) {
        this.chain = chain;
        this.reference = reference;
        this.interChainInteractions = interChainInteractions;
        this.referenceInteractions = referenceInteractions;
        this.shells = new TreeMap<>();
        this.graph = new GenericGraph<>();
        generateInteractionGraph();
        computeShells();
    }

    public static PlipShellGenerator getInteractionShellsForLigand(Chain chain, LeafSubstructure<?, ?> reference,
                                                                   InteractionContainer interChainInteractions,
                                                                   InteractionContainer referenceInteractions) {
        return new PlipShellGenerator(chain, reference, interChainInteractions, referenceInteractions);
    }

    private void computeShells() {
        GenericNode<LeafSubstructure<?, ?>> referenceNode = graph.getNodeWithContent(reference)
                .orElseThrow(() -> new IllegalArgumentException("No such reference node in interaction graph."));
        for (InteractionShell interactionShell : InteractionShell.values()) {
            shells.put(interactionShell, interactionShell.from(graph, referenceNode));
        }
    }

    public Map<InteractionShell, List<LeafSubstructure<?, ?>>> getShells() {
        return shells;
    }

    private void generateInteractionGraph() {
        // compute first shell (directly interacting with reference)
        Set<LeafSubstructure<?, ?>> firstShell = referenceInteractions.getInteractions().stream()
                .filter(interaction -> interaction.getTarget().equals(reference.getIdentifier()))
                .map(Interaction::getSource)
                .map(leafIdentifier -> chain.getLeafSubstructures().stream()
                        .filter(leafSubstructure -> leafSubstructure.getIdentifier().equals(leafIdentifier))
                        .findFirst())
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());
        // add reference and its interactions to graph
        graph.addNode(reference);
        for (LeafSubstructure<?, ?> leafSubstructure : firstShell) {
            graph.addNode(leafSubstructure);
            graph.addEdgeBetween(reference, leafSubstructure);
        }
        // generate interaction graph from inter chain interactions
        for (Interaction interaction : interChainInteractions.getInteractions()) {
            LeafSubstructure<?, ?> source = chain.getLeafSubstructure(interaction.getSource());
            LeafSubstructure<?, ?> target = chain.getLeafSubstructure(interaction.getTarget());
            graph.addEdgeBetween(source, target);
        }
    }

    public GenericGraph<LeafSubstructure<?, ?>> getGraph() {
        return graph;
    }

    public enum InteractionShell {

        FIRST, SECOND, THIRD;

        public List<LeafSubstructure<?, ?>> from(GenericGraph<LeafSubstructure<?, ?>> graph,
                                                 GenericNode<LeafSubstructure<?, ?>> referenceNode) {
            return NeighbourhoodExtractor.extractShell(graph, referenceNode, this.ordinal() + 1).stream()
                    .map(GenericNode::getContent)
                    .collect(Collectors.toList());
        }
    }
}