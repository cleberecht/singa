package bio.singa.simulation.model.agents.membranes;

import bio.singa.features.parameters.Environment;
import bio.singa.mathematics.algorithms.geometry.SutherandHodgmanClipping;
import bio.singa.mathematics.algorithms.topology.FloodFill;
import bio.singa.mathematics.geometry.edges.LineSegment;
import bio.singa.mathematics.geometry.edges.SimpleLineSegment;
import bio.singa.mathematics.geometry.model.Polygon;
import bio.singa.mathematics.vectors.Vector2D;
import bio.singa.simulation.model.agents.organelles.Organelle;
import bio.singa.simulation.model.graphs.AutomatonGraph;
import bio.singa.simulation.model.graphs.AutomatonNode;
import bio.singa.simulation.model.sections.CellRegion;
import bio.singa.simulation.parser.organelles.OrganelleTemplate;

import java.util.*;

import static bio.singa.mathematics.geometry.model.Polygon.*;

/**
 * @author cl
 */
public class MembraneTracer {

    // input
    private HashMap<CellRegion, List<AutomatonNode>> regionNodeMapping;
    private AutomatonGraph graph;

    // output
    private List<Membrane> membranes;

    // working
    private LinkedList<AutomatonNode> currentNodes;
    private Deque<AutomatonNode> queue;
    private List<AutomatonNode> unprocessedNodes;

    public static Membrane membraneToRegion(Organelle organelle, AutomatonGraph graph) {
        return membraneToRegion(organelle.getMembraneRegion(), organelle.getInternalRegion(), organelle.getTemplate(), graph);
    }

    public static Membrane membraneToRegion(CellRegion membraneRegion, CellRegion internalRegion, OrganelleTemplate template, AutomatonGraph graph) {
        Membrane membrane = new Membrane(membraneRegion.getIdentifier(), membraneRegion);
        Polygon membranePolygon = template.getPolygon();
        // check if all segments are contained in a single node
        boolean isContained = true;
        // determine and setup membrane cells
        for (LineSegment lineSegment : membranePolygon.getEdges()) {
            Vector2D startingPoint = lineSegment.getStartingPoint();
            Vector2D endingPoint = lineSegment.getEndingPoint();
            for (AutomatonNode node : graph.getNodes()) {
                Polygon spatialRepresentation = node.getSpatialRepresentation();
                // evaluate line segment
                int startingPosition = spatialRepresentation.evaluatePointPosition(startingPoint);
                int endingPosition = spatialRepresentation.evaluatePointPosition(endingPoint);
                Set<Vector2D> intersections = spatialRepresentation.getIntersections(lineSegment);
                if (startingPosition >= ON_LINE && endingPosition >= ON_LINE) {
                    // completely inside
                    membrane.addSegment(node, lineSegment);
                    CellRegion region;
                    if (template.isGrouped()) {
                        region = template.getRegion(startingPoint);
                    } else {
                        region = membraneRegion;
                    }
                    node.setCellRegion(region);
                    break;
                } else if (startingPosition == INSIDE && endingPosition == OUTSIDE) {
                    // end outside or on line
                    Vector2D intersectionPoint = intersections.iterator().next();
                    membrane.addSegment(node, new SimpleLineSegment(startingPoint, intersectionPoint));
                    CellRegion region;
                    if (template.isGrouped()) {
                        region = template.getRegion(startingPoint);
                    } else {
                        region = membraneRegion;
                    }
                    node.setCellRegion(region);
                    isContained = false;
                } else if (startingPosition == OUTSIDE && endingPosition == INSIDE) {
                    // start outside or on line
                    Vector2D intersectionPoint = intersections.iterator().next();
                    membrane.addSegment(node, new SimpleLineSegment(intersectionPoint, endingPoint));
                    CellRegion region;
                    if (template.isGrouped()) {
                        region = template.getRegion(startingPoint);
                    } else {
                        region = membraneRegion;
                    }
                    node.setCellRegion(region);
                    isContained = false;
                } else if (intersections.size() == 2) {
                    // line only crosses the membrane
                    Iterator<Vector2D> iterator = intersections.iterator();
                    Vector2D first = iterator.next();
                    Vector2D second = iterator.next();
                    membrane.addSegment(node, new SimpleLineSegment(first, second));
                    CellRegion region;
                    if (template.isGrouped()) {
                        region = template.getRegion(startingPoint);
                    } else {
                        region = membraneRegion;
                    }
                    node.setCellRegion(region);
                    isContained = false;
                }
            }
        }

        // setup subsection representation for enclosed organelles
        if (isContained) {
            AutomatonNode containingNode = membrane.getSegments().iterator().next().getNode();
            containingNode.addSubsectionRepresentation(membraneRegion.getInnerSubsection(), membranePolygon);
            return membrane;
        }

        // fill region inside of the organelle
        for (AutomatonNode node : graph.getNodes()) {
            // determine cell that is completely inside of the membrane as starting point
            // therefore check if all segments of the representative region are inside
            boolean allPointsAreInside = true;
            for (Vector2D vector : node.getSpatialRepresentation().getVertices()) {
                if (membranePolygon.evaluatePointPosition(vector) == OUTSIDE) {
                    allPointsAreInside = false;
                    break;
                }
            }
            if (allPointsAreInside) {
                // use flood fill algorithm
                FloodFill.fill(graph.getGrid(), node.getIdentifier(),
                        currentNode -> currentNode.getCellRegion().equals(membraneRegion) || template.getRegions().contains(currentNode.getCellRegion()),
                        rectangularCoordinate -> graph.getNode(rectangularCoordinate).setCellRegion(internalRegion),
                        recurrentNode -> recurrentNode.getCellRegion().equals(internalRegion));
                break;
            }
        }

        // setup subsection representation for nodes compartmentalized by membranes
        for (AutomatonNode automatonNode : graph.getNodes()) {
            if (automatonNode.getCellRegion().equals(membraneRegion) || template.getRegions().contains(automatonNode.getCellRegion())) {
                // use sutherland hodgman to clip inner region
                Polygon nodePolygon = automatonNode.getSpatialRepresentation();
                Polygon innerPolygon = SutherandHodgmanClipping.clip(membranePolygon, nodePolygon);
                automatonNode.addSubsectionRepresentation(membraneRegion.getInnerSubsection(), innerPolygon);
            }
        }
        return membrane;
    }

    public static List<Membrane> regionsToMembrane(AutomatonGraph graph) {
        MembraneTracer composer = new MembraneTracer(graph);
        return composer.membranes;
    }

    public MembraneTracer(AutomatonGraph graph) {
        this.graph = graph;
        membranes = new ArrayList<>();
        if (graph.getNodes().size() == 1) {
            traceSingleNode(graph.getNode(0, 0));
        } else {
            currentNodes = new LinkedList<>();
            queue = new ArrayDeque<>();
            unprocessedNodes = new ArrayList<>();
            initializeRegionNodeMapping();
            for (CellRegion cellRegion : regionNodeMapping.keySet()) {
                while (!regionNodeMapping.get(cellRegion).isEmpty()) {
                    traverseRegion(cellRegion);
                    currentNodes.clear();
                    queue.clear();
                }
            }
        }
    }

    private void traceSingleNode(AutomatonNode node) {
        double simulationExtend = Environment.getSimulationExtend();
        // add horizontal membrane segment
        Vector2D start = new Vector2D(0, simulationExtend / 2.0);
        Vector2D end = new Vector2D(simulationExtend, simulationExtend / 2.0);
        Membrane membrane = new Membrane(node.getCellRegion().getIdentifier(), node.getCellRegion());
        membrane.addSegment(node, new SimpleLineSegment(start, end));
        membranes.add(membrane);
    }

    private void initializeRegionNodeMapping() {
        // get different regions and associate nodes
        regionNodeMapping = new HashMap<>();
        for (AutomatonNode node : graph.getNodes()) {
            // initialize region if it contains a membrane and it is not already present
            CellRegion cellRegion = node.getCellRegion();
            if (!regionNodeMapping.containsKey(cellRegion) && cellRegion.hasMembrane()) {
                regionNodeMapping.put(cellRegion, new ArrayList<>());
            }
            // add node if it has a membrane
            if (cellRegion.hasMembrane()) {
                regionNodeMapping.get(cellRegion).add(node);
            }
        }
    }

    private void traverseRegion(CellRegion region) {
        unprocessedNodes = regionNodeMapping.get(region);
        AutomatonNode startingNode = null;
        boolean cyclic = true;
        // see if the membrane is a cycle or linear
        for (AutomatonNode currentNode : unprocessedNodes) {
            int neighbouringRegions = 0;
            for (AutomatonNode node : currentNode.getNeighbours()) {
                if (node.getCellRegion().equals(region)) {
                    neighbouringRegions++;
                }
            }
            if (neighbouringRegions == 1) {
                // membrane is linear
                startingNode = currentNode;
                cyclic = false;
            }
            if (neighbouringRegions > 2) {
                throw new IllegalStateException("The automaton graph has membrane that has more than two neighboring " +
                        "nodes that are also membrane and the same region.");
            }
        }
        // membrane is cyclic
        if (startingNode == null) {
            startingNode = unprocessedNodes.iterator().next();
        }

        // initialize starting point
        queue.push(startingNode);
        // as long as there are nodes on the queue
        AutomatonNode currentNode;
        // depth first traversal
        while ((currentNode = queue.poll()) != null) {
            if (!currentNodes.contains(currentNode)) {
                currentNodes.add(currentNode);
                for (AutomatonNode neighbour : currentNode.getNeighbours()) {
                    if (neighbour.getCellRegion().equals(region)) {
                        processNode(neighbour);
                    }
                }
            }
        }

        // create membrane section
        Membrane membrane = new Membrane(region.getIdentifier(), region);
        // add full segments first
        // get first node
        ListIterator<AutomatonNode> iterator = currentNodes.listIterator();
        AutomatonNode previousNode = iterator.next();
        currentNode = iterator.next();
        while (iterator.hasNext()) {
            AutomatonNode nextNode = iterator.next();
            membrane.addSegment(currentNode, new SimpleLineSegment(currentNode.getPosition(), previousNode.getPosition()));
            membrane.addSegment(currentNode, new SimpleLineSegment(currentNode.getPosition(), nextNode.getPosition()));
            previousNode = currentNode;
            currentNode = nextNode;
        }

        // add first and last node for cyclic membranes
        if (cyclic) {
            Iterator<AutomatonNode> forwardIterator = currentNodes.iterator();
            AutomatonNode first = forwardIterator.next();
            AutomatonNode second = forwardIterator.next();
            membrane.addSegment(first, new SimpleLineSegment(first.getPosition(), currentNodes.getLast().getPosition()));
            membrane.addSegment(first, new SimpleLineSegment(first.getPosition(), second.getPosition()));

            Iterator<AutomatonNode> backwardIterator = currentNodes.descendingIterator();
            AutomatonNode last = backwardIterator.next();
            AutomatonNode beforeLast = backwardIterator.next();
            membrane.addSegment(last, new SimpleLineSegment(last.getPosition(), beforeLast.getPosition()));
            membrane.addSegment(last, new SimpleLineSegment(last.getPosition(), first.getPosition()));
        }
        membranes.add(membrane);

        // cut segments to size
        for (MembraneSegment entry : membrane.getSegments()) {
            Polygon spatialRepresentation = entry.getNode().getSpatialRepresentation();
            Set<Vector2D> intersections = spatialRepresentation.getIntersections(entry);
            // starting point should be always associated node
            entry.setEndingPoint(intersections.iterator().next());
        }

    }

    /**
     * Adds a node to the queue and subgraph, and removes it from the unprocessed nodes.
     *
     * @param node The nodes.
     */
    private void processNode(AutomatonNode node) {
        // add initial node to queue
        queue.push(node);
        // and remove it from the unprocessed stack
        unprocessedNodes.remove(node);
    }

}
