package bio.singa.mathematics.graphs.model;

import bio.singa.mathematics.vectors.Vector;

import java.util.*;

/**
 * The graph contains nodes connected by edges of a certain type.
 *
 * @param <NodeType> The type of the nodes in the graph.
 * @param <EdgeType> The type of the edges in the graph.
 * @author cl
 */
public interface Graph<NodeType extends Node<NodeType, ? extends Vector, IdentifierType>, EdgeType extends Edge<NodeType>, IdentifierType> {

    /**
     * Returns all nodes.
     *
     * @return All nodes.
     */
    Collection<NodeType> getNodes();

    /**
     * Returns the node with the given identifier.
     *
     * @param identifier The identifier of the node.
     * @return The node with the given identifier.
     */
    NodeType getNode(IdentifierType identifier);

    /**
     * Adds a node to the graph.
     *
     * @param node The node to be added.
     * @return The identifier of the node.
     */
    IdentifierType addNode(NodeType node);

    /**
     * Removes the node from the graph. Edges connected to this node will also be removed.
     *
     * @param node The node to be removed.
     * @return The node that has been removed.
     */
    NodeType removeNode(NodeType node);

    /**
     * Removes the node with the given identifier from the graph. Edges connected to this node will also be removed.
     *
     * @param identifier The identifier of the node to be removed.
     * @return The node that has been removed.
     */
    NodeType removeNode(IdentifierType identifier);

    /**
     * Returns all edges.
     *
     * @return All edges.
     */
    Collection<EdgeType> getEdges();

    /**
     * Returns the node with the given identifier.
     *
     * @param identifier The identifier of the node.
     * @return The edge with the given identifier.
     */
    EdgeType getEdge(int identifier);

    /**
     * Adds an edge with the given identifier between the two given nodes. If an edge with the identifier is already
     * present in the graph it is overwritten.
     *
     * @param identifier The identifier of the edge to be inserted.
     * @param source The source node.
     * @param target The target node.
     * @return The identifier of the added edge.
     */
    int addEdgeBetween(int identifier, NodeType source, NodeType target);

    /**
     * Adds the given edge to the graph setting the given source an target nodes. If an edge with the identifier is
     * already present in the graph it is overwritten.
     *
     * @param edge The edge to be added.
     * @param source The source node.
     * @param target The target node.
     * @return The identifier of the added edge.
     */
    int addEdgeBetween(EdgeType edge, NodeType source, NodeType target);

    /**
     * Adds a new edge between the source and target nodes to the graph using the next free edge identifier.
     *
     * @param source The source node.
     * @param target The target node.
     * @return The identifier of the added edge.
     */
    int addEdgeBetween(NodeType source, NodeType target);

    /**
     * Returns the any edge between both nodes. The ordering (source, target) that may be defined does not matter.
     *
     * @param first The first node.
     * @param second The second node.
     * @return Any edge that connects both nodes, or an empty node otherwise.
     */
    default Optional<EdgeType> getEdgeBetween(NodeType first, NodeType second) {
        return getEdges().stream()
                .filter(edge -> edge.containsNode(first) && edge.containsNode(second))
                .findAny();
    }

    /**
     * Returns all nodes touching the given node (i.e. neighbours regardless of the eventual directionality of the
     * connecting edge). Prefer {@link Node#getNeighbours()} to this method if possible, since this has higher
     * complexity.
     * @param node The node.
     * @return The neighbours.
     */
    default List<NodeType> getTouchingNodes(NodeType node) {
        List<NodeType> touchingNodes = new ArrayList<>();
        for (EdgeType edge : getEdges()) {
            if (edge.containsNode(node)) {
                if (edge.getSource() != node) {
                    touchingNodes.add(edge.getSource());
                    continue;
                }
                touchingNodes.add(edge.getTarget());
            }
        }
        return touchingNodes;
    }

    /**
     * Returns true, if the graph contains the node and false otherwise.
     *
     * @param node The node to be searched.
     * @return true, if the graph contains the node and false otherwise.
     */
    boolean containsNode(Object node);

    /**
     * Returns true, if the graph contains the edge and false otherwise.
     *
     * @param edge The edge to be searched.
     * @return true, if the graph contains the edge and false otherwise.
     */
    boolean containsEdge(Object edge);

    /**
     * Returns the next free node identifier.
     *
     * @return The next free node identifier.
     */
    IdentifierType nextNodeIdentifier();

    /**
     * Returns the next free edge identifier.
     *
     * @return The next free edge identifier.
     */
    default int nextEdgeIdentifier() {
        if (getEdges().isEmpty()) {
            return 0;
        }
        return getEdges().size() + 1;
    }

    // TODO write tests that ensure proper copying of graphs
    default Graph<NodeType, EdgeType, IdentifierType> getCopy() {
        Graph<NodeType, EdgeType, IdentifierType> copy;
        try {
            copy = getClass().newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Failed to create a new graph.");
        }
        // copy and add nodes
        Objects.requireNonNull(copy);
        Collection<NodeType> nodes = getNodes();
        for (NodeType node : nodes) {
            NodeType nodeCopy = node.getCopy();
            copy.addNode(nodeCopy);
        }
        // create and add edges for the nodes (preserving edge identifier)
        Collection<EdgeType> edges = getEdges();
        for (EdgeType edge : edges) {
            NodeType source = copy.getNode(edge.getSource().getIdentifier());
            NodeType target = copy.getNode(edge.getTarget().getIdentifier());
            copy.addEdgeBetween(edge.getIdentifier(), source, target);
        }
        return copy;
    }
}
