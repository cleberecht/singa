package bio.singa.mathematics.graphs.model;

import bio.singa.core.utility.Pair;
import bio.singa.mathematics.algorithms.graphs.DisconnectedSubgraphFinder;
import bio.singa.mathematics.geometry.faces.Rectangle;
import bio.singa.mathematics.graphs.grid.GridGraph;
import bio.singa.mathematics.graphs.grid.GridNode;
import bio.singa.mathematics.graphs.trees.BinaryTree;
import bio.singa.mathematics.graphs.trees.BinaryTreeNode;
import bio.singa.mathematics.topology.grids.rectangular.NeumannRectangularDirection;
import bio.singa.mathematics.topology.grids.rectangular.RectangularCoordinate;
import bio.singa.mathematics.vectors.Vector;
import bio.singa.mathematics.vectors.Vector2D;
import bio.singa.mathematics.vectors.Vectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.BiFunction;

/**
 * A factory class used to create graphs and convert other things to graphs.
 *
 * @author cl
 */
public class Graphs {

    public static final Rectangle DEFAULT_BOUNDING_BOX = new Rectangle(400, 400);
    private static final Logger logger = LoggerFactory.getLogger(Graphs.class);

    /**
     * Generates a linear graph with the given number of nodes. Each node will be connected to its predecessor.
     *
     * @param numberOfNodes The number of nodes the graph should contain.
     * @return A linear Graph
     */
    public static UndirectedGraph buildLinearGraph(int numberOfNodes) {
        return buildLinearGraph(numberOfNodes, DEFAULT_BOUNDING_BOX);
    }

    /**
     * Generates a linear graph with the given number of nodes. Each node will be connected to its predecessor.
     *
     * @param numberOfNodes The number of nodes the graph should contain.
     * @param boundingBox   A bounding box where the nodes should be positioned.
     * @return A linear Graph
     */
    public static UndirectedGraph buildLinearGraph(int numberOfNodes, Rectangle boundingBox) {
        logger.debug("Creating linear graph with {} nodes.", numberOfNodes);
        UndirectedGraph graph = new UndirectedGraph();
        for (int i = 0; i < numberOfNodes; i++) {
            graph.addNode(Nodes.createRandomlyPlacedNode(i, boundingBox));
        }
        for (int i = 0; i < numberOfNodes - 1; i++) {
            graph.addEdgeBetween(i, graph.getNode(i), graph.getNode(i + 1));
        }
        return graph;
    }

    /**
     * Generates a circular Graph with the given number of nodes. Each node will be connected to its predecessor and
     * successor.
     *
     * @param numberOfNodes The number of nodes the circle should contain.
     * @return A circular graph.
     */
    public static UndirectedGraph buildCircularGraph(int numberOfNodes) {
        return buildCircularGraph(numberOfNodes, DEFAULT_BOUNDING_BOX);
    }

    /**
     * Generates a circular Graph with the given number of nodes. Each node will be connected to its predecessor and
     * successor.
     *
     * @param numberOfNodes The number of nodes the circle should contain.
     * @param boundingBox   A bounding box where the nodes should be positioned.
     * @return A circular graph.
     */
    public static UndirectedGraph buildCircularGraph(int numberOfNodes, Rectangle boundingBox) {
        logger.debug("Creating circular graph with {} nodes.", numberOfNodes);
        UndirectedGraph graph = buildLinearGraph(numberOfNodes, boundingBox);
        graph.addEdgeBetween(numberOfNodes, graph.getNode(numberOfNodes - 1), graph.getNode(0));
        return graph;
    }

    /**
     * Generates a graph with a tree-like structure, where every node is connected to one predecessor and two
     * successors, thus forming a fractal structure.
     *
     * @param depth The depth of the tree.
     * @return A tree-like graph.
     */
    public static UndirectedGraph buildTreeGraph(int depth) {
        return buildTreeGraph(depth, DEFAULT_BOUNDING_BOX);
    }

    /**
     * Generates a graph with a tree-like structure, where every node is connected to one predecessor and two
     * successors, thus forming a fractal structure.
     *
     * @param depth       The depth of the tree.
     * @param boundingBox A bounding box where the nodes should be positioned.
     * @return A tree-like graph.
     */
    public static UndirectedGraph buildTreeGraph(int depth, Rectangle boundingBox) {
        logger.debug("Creating tree graph with with a depth of {}.", depth);
        if (depth < 1) {
            throw new IllegalArgumentException("The depth of a tree-like graph must be at least 1");
        }
        UndirectedGraph graph = new UndirectedGraph();
        RegularNode root = Nodes.createRandomlyPlacedNode(0, boundingBox);
        graph.addNode(root);
        Graphs.growTree(depth - 1, graph, root, boundingBox);
        return graph;
    }

    /**
     * A private method used to grow the tree-like graph structure. The given graph will be modified!
     *
     * @param depth       The current depth.
     * @param graph       The graph to add the new node.
     * @param predecessor The previously added node.
     * @param boundingBox A bounding box where the nodes should be positioned.
     */
    private static void growTree(int depth, UndirectedGraph graph, RegularNode predecessor, Rectangle boundingBox) {
        int next = graph.nextNodeIdentifier();
        graph.addNode(Nodes.createRandomlyPlacedNode(next, boundingBox));
        graph.addEdgeBetween(graph.nextEdgeIdentifier(), predecessor, graph.getNode(next));
        if (depth > 0) {
            growTree(depth - 1, graph, graph.getNode(next), boundingBox);
            growTree(depth - 1, graph, graph.getNode(next), boundingBox);
        }
    }

    /**
     * Generates a randomised graph based on the Erdös - Renyi model.
     *
     * @param numberOfNodes   The number of nodes the graph should contain.
     * @param edgeProbability The probability, that two nodes will be connected.
     * @return A randomized graph.
     */
    public static UndirectedGraph buildRandomGraph(int numberOfNodes, double edgeProbability) {
        return buildRandomGraph(numberOfNodes, edgeProbability, DEFAULT_BOUNDING_BOX);
    }

    /**
     * Generates a randomised graph based on the Erdös - Renyi model.
     *
     * @param numberOfNodes   The number of nodes the graph should contain.
     * @param edgeProbability The probability, that two nodes will be connected (must be between 0.0 (no nodes are
     *                        connected) and 1.0 (every node is connected))
     * @param boundingBox     A bounding box where the nodes should be positioned.
     * @return A randomized graph.
     */
    public static UndirectedGraph buildRandomGraph(int numberOfNodes, double edgeProbability, Rectangle boundingBox) {
        if (edgeProbability < 0.0 || edgeProbability > 1.0) {
            throw new IllegalArgumentException("To create a randomized graph, the edge probability must be between 0.0 (no nodes are connected) and 1.0 (every node is connected).");
        }
        logger.debug("Creating randomized graph with with {} nodes and an edge probability of {}.", numberOfNodes, edgeProbability);
        UndirectedGraph graph = new UndirectedGraph();
        for (int i = 0; i < numberOfNodes; i++) {
            graph.addNode(Nodes.createRandomlyPlacedNode(i, boundingBox));
        }
        int j = 0;
        for (RegularNode source : graph.getNodes()) {
            for (RegularNode target : graph.getNodes()) {
                if (!source.equals(target)) {
                    if (Math.random() < edgeProbability) {
                        graph.addEdgeBetween(j, source, target);
                        j++;
                    }
                }
            }
        }
        return graph;
    }

    /**
     * Generates a grid graph with columns and rows.
     *
     * @param columns The Number of columns
     * @param rows    The Number of rows
     * @return A rectangular grid graph.
     */
    public static GridGraph buildGridGraph(int columns, int rows) {
        return buildGridGraph(columns, rows, DEFAULT_BOUNDING_BOX);
    }

    /**
     * Generates a grid graph with columns and rows.
     *
     * @param boundingBox Rectangle where the Graph is positioned.
     * @param columns     The Number of columns
     * @param rows        The Number of rows
     * @return A rectangular grid graph.
     */
    public static GridGraph buildGridGraph(int columns, int rows, Rectangle boundingBox) {
        logger.debug("Creating randomized grid graph with with {} columns and {} rows.", columns, rows);
        GridGraph graph = new GridGraph(columns, rows);
        double horizontalSpacing = boundingBox.getWidth() / columns;
        double verticalSpacing = boundingBox.getHeight() / rows;

        if (horizontalSpacing > verticalSpacing) {
            horizontalSpacing = verticalSpacing;
        } else if (verticalSpacing > horizontalSpacing) {
            verticalSpacing = horizontalSpacing;
        }

        double verticalOffset = 0.5 * verticalSpacing;
        double horizontalOffset = 0.5 * horizontalSpacing;

        // adding nodes
        logger.trace("Creating and placing nodes ...");
        for (int columnIndex = 0; columnIndex < columns; columnIndex++) {
            for (int rowIndex = 0; rowIndex < rows; rowIndex++) {
                GridNode node = new GridNode(new RectangularCoordinate(columnIndex, rowIndex));
                node.setPosition(new Vector2D(horizontalSpacing * (columnIndex + 1) - horizontalOffset, verticalSpacing * (rowIndex + 1) - verticalOffset));
                graph.addNode(node);
            }
        }

        // add connections
        logger.trace("Adding connections ...");
        for (int columnIndex = 0; columnIndex < columns; columnIndex++) {
            for (int rowIndex = 0; rowIndex < rows; rowIndex++) {
                RectangularCoordinate coordinate = new RectangularCoordinate(columnIndex, rowIndex);
                GridNode source = graph.getNode(coordinate);
                if (columnIndex < columns - 1) {
                    GridNode target = graph.getNode(coordinate.getNeighbour(NeumannRectangularDirection.EAST));
                    graph.addEdgeBetween(source, target);
                }
                if (rowIndex < rows - 1) {
                    GridNode target = graph.getNode(coordinate.getNeighbour(NeumannRectangularDirection.SOUTH));
                    graph.addEdgeBetween(source, target);
                }
            }
        }

        return graph;
    }

    /**
     * Converts a {@link BinaryTree} to a {@link GenericGraph}.
     *
     * @param tree          The tree.
     * @param <ContentType> The content of the resulting generic graph.
     * @return The generic graph.
     */
    public static <ContentType> GenericGraph<ContentType> convertTreeToGraph(BinaryTree<ContentType> tree) {
        logger.debug("Converting tree to graph.");
        GenericGraph<ContentType> graph = new GenericGraph<>();
        BinaryTreeNode<ContentType> root = tree.getRoot();
        GenericNode<ContentType> rootNode = convertNode(root, graph);
        graph.addNode(rootNode);
        traverseNode(graph, rootNode, root.getLeft());
        traverseNode(graph, rootNode, root.getRight());
        return graph;
    }

    /**
     * Converts a {@link BinaryTreeNode} to a {@link GenericNode} and connects it in the given graph to the source node.
     * Recursively traverses all child nodes.
     *
     * @param graph         The graph where the node is added.
     * @param source        The node to connect to.
     * @param treeNode      The node to convert.
     * @param <ContentType> The content type of the node.
     */
    private static <ContentType> void traverseNode(GenericGraph<ContentType> graph,
                                                   GenericNode<ContentType> source, BinaryTreeNode<ContentType> treeNode) {
        // add current tree to the graph and connect it
        GenericNode<ContentType> graphNode = convertNode(treeNode, graph);
        graph.addNode(graphNode);
        graph.addEdgeBetween(source, graphNode);
        // traverse the children
        if (treeNode.getLeft() != null) {
            traverseNode(graph, graphNode, treeNode.getLeft());
        }
        if (treeNode.getRight() != null) {
            traverseNode(graph, graphNode, treeNode.getRight());
        }
    }

    /**
     * Converts a {@link BinaryTreeNode} to a {@link GenericNode} with the next free node identifer from the graph.
     * Places the node randomly in a 200 x 200 rectangle.
     *
     * @param treeNode      The node to convert.
     * @param graph         The graph to take the index from.
     * @param <ContentType> The content type of the node.
     * @return The converted {@link GenericNode}.
     */
    private static <ContentType> GenericNode<ContentType> convertNode(BinaryTreeNode<ContentType> treeNode,
                                                                      GenericGraph<ContentType> graph) {
        GenericNode<ContentType> result = new GenericNode<>(graph.nextNodeIdentifier(), treeNode.getData());
        result.setPosition(Vectors.generateRandom2DVector(new Rectangle(200, 200)));
        return result;
    }

    /**
     * Given a graph, this method returns a list of all disconnected subgraphs. The subgraphs are copies and changes are
     * not reflected back into the original graph, but node and edge identifiers, as well as attached data is
     * conserved.
     *
     * @param graph            The graph to decompose.
     * @param <NodeType>       The type of the nodes.
     * @param <EdgeType>       The type of the edges.
     * @param <GraphType>      The type of the graph.
     * @param <VectorType>     The position type.
     * @param <IdentifierType> The type of the identifier.
     * @return A list of all disconnected subgraphs.
     */
    public static <NodeType extends Node<NodeType, VectorType, IdentifierType>,
            EdgeType extends Edge<NodeType>, VectorType extends Vector, IdentifierType,
            GraphType extends Graph<NodeType, EdgeType, IdentifierType>> List<GraphType> findDisconnectedSubgraphs(GraphType graph) {
        return DisconnectedSubgraphFinder.findDisconnectedSubgraphs(graph);
    }

    /**
     * Returns a modular product graph of the graphs G and G'.
     *
     * @param graph1 The first graph (G).
     * @param graph2 The second graph (G').
     * @return A modular graph with nodes containing pairs of nodes u and u' from G and G'.
     */
    public static <NodeType extends Node<NodeType, VectorType, IdentifierType>,
            EdgeType extends Edge<NodeType>, VectorType extends Vector, IdentifierType,
            GraphType extends Graph<NodeType, EdgeType, IdentifierType>> GenericGraph<Pair<NodeType>> modularProduct(
            GraphType graph1, GraphType graph2) {
        return modularProduct(graph1, graph2, (n1, n2) -> true, (e1, e2) -> true);
    }

    /**
     * Returns a modular product graph of the graphs G and G'.
     *
     * @param graph1           The first graph (G).
     * @param graph2           The second graph (G').
     * @param nodeCondition    A bifunction that defines which nodes to be paired
     * @param <NodeType>       The type of the nodes.
     * @param <EdgeType>       The type of the edges.
     * @param <GraphType>      The type of the graph.
     * @param <VectorType>     The position type.
     * @param <IdentifierType> The type of the identifier.
     * @return A modular graph with nodes containing pairs of nodes u and u' from G and G'.
     */
    public static <NodeType extends Node<NodeType, VectorType, IdentifierType>,
            EdgeType extends Edge<NodeType>, VectorType extends Vector, IdentifierType,
            GraphType extends Graph<NodeType, EdgeType, IdentifierType>> GenericGraph<Pair<NodeType>> modularProduct(
            GraphType graph1, GraphType graph2, BiFunction<NodeType, NodeType, Boolean> nodeCondition,
            BiFunction<EdgeType, EdgeType, Boolean> edgeCondition) {
        List<NodeType> nodes1 = new ArrayList<>(graph1.getNodes());
        List<NodeType> nodes2 = new ArrayList<>(graph2.getNodes());
        List<Pair<NodeType>> nodePairs = new ArrayList<>();
        // generate node pairs
        for (int i = 0; i < nodes1.size(); i++) {
            for (int j = 0; j < nodes2.size(); j++) {
                NodeType node1 = nodes1.get(i);
                NodeType node2 = nodes2.get(j);
                if (!nodeCondition.apply(node1, node2)) {
                    logger.debug("node condition does not apply for nodes {}-{}", node1, node2);
                    continue;
                }
                Pair<NodeType> nodePair = new Pair<>(node1, node2);
                nodePairs.add(nodePair);
            }
        }
        GenericGraph<Pair<NodeType>> modularProduct = new GenericGraph<>();
        nodePairs.forEach(modularProduct::addNode);
        // check for connectivity of product nodes
        for (int i = 0; i < nodePairs.size(); i++) {
            Pair<NodeType> pair1 = nodePairs.get(i);
            for (int j = i + 1; j < nodePairs.size(); j++) {
                // pairs (u,v) and (u',v') are connected iff:
                // u is adjacent with u' and v is adjacent with v' , or
                // u is not adjacent with u' and v is not adjacent with v', and
                // edge condition between edge connecting u and u' and edge connecting v and v' is met
                Pair<NodeType> pair2 = nodePairs.get(j);
                if (pair1.getFirst().equals(pair2.getFirst()) || pair1.getSecond().equals(pair2.getSecond())) {
                    continue;
                }
                boolean uCondition = pair1.getFirst().getNeighbours().contains(pair2.getFirst());
                boolean vCondition = pair1.getSecond().getNeighbours().contains(pair2.getSecond());
                boolean edgeMatch = false;
                if (uCondition && vCondition) {
                    EdgeType uEdge = graph1.getEdgeBetween(pair1.getFirst(), pair2.getFirst()).orElseThrow(NoSuchElementException::new);
                    EdgeType vEdge = graph2.getEdgeBetween(pair1.getSecond(), pair2.getSecond()).orElseThrow(NoSuchElementException::new);
                    edgeMatch = edgeCondition.apply(uEdge, vEdge);
                }
                if ((uCondition && vCondition && edgeMatch) || (!uCondition && !vCondition)) {
                    // connect pair nodes
                    modularProduct.addEdgeBetween(pair1, pair2);
                }
            }
        }
        return modularProduct;
    }

    public static <NodeType extends Node<NodeType, VectorType, IdentifierType>,
            EdgeType extends Edge<NodeType>, VectorType extends Vector, IdentifierType,
            GraphType extends Graph<NodeType, EdgeType, IdentifierType>> GraphType copy(GraphType graph) {
        GraphType resultGraph;
        try {
            resultGraph = (GraphType) graph.getClass().getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new RuntimeException("Failed to create a new graph.");
        }
        Objects.requireNonNull(resultGraph);
        Map<IdentifierType, IdentifierType> identifierMapping = new HashMap<>();
        for (NodeType node : graph.getNodes()) {
            // conserve identifier
            IdentifierType newIdentifier = resultGraph.nextNodeIdentifier();
            IdentifierType oldIdentifier = node.getIdentifier();
            identifierMapping.put(oldIdentifier, newIdentifier);
            // create copy
            NodeType copy = node.getCopy();
            copy.setIdentifier(newIdentifier);
            resultGraph.addNode(copy);
        }
        for (EdgeType edge : graph.getEdges()) {
            EdgeType edgeCopy = edge.getCopy();
            edgeCopy.setIdentifier(resultGraph.nextEdgeIdentifier());
            NodeType source = resultGraph.getNode(identifierMapping.get(edge.getSource().getIdentifier()));
            NodeType target = resultGraph.getNode(identifierMapping.get(edge.getTarget().getIdentifier()));
            resultGraph.addEdgeBetween(edgeCopy, source, target);
        }
        return resultGraph;
    }

}
