package bio.singa.simulation.model.agents.surfacelike;

import bio.singa.core.utility.Pair;
import bio.singa.features.parameters.Environment;
import bio.singa.features.units.UnitRegistry;
import bio.singa.mathematics.geometry.edges.LineSegment;
import bio.singa.mathematics.geometry.faces.Rectangle;
import bio.singa.mathematics.topology.grids.rectangular.NeumannRectangularDirection;
import bio.singa.mathematics.topology.grids.rectangular.RectangularCoordinate;
import bio.singa.mathematics.topology.grids.rectangular.RectangularGrid;
import bio.singa.mathematics.vectors.Vector2D;
import bio.singa.simulation.model.graphs.AutomatonGraph;
import bio.singa.simulation.model.graphs.AutomatonGraphs;
import bio.singa.simulation.model.graphs.AutomatonNode;
import bio.singa.simulation.model.sections.CellRegion;
import bio.singa.simulation.model.sections.CellSubsection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author cl
 */
public class GridMembraneBuilder {

    private Map<Integer, CellRegion> regionMapping;
    private Map<Pair<Integer>, CellRegion> membraneMapping;
    private Map<CellRegion, Membrane> membranes;

    private AutomatonGraph graph;

    private RectangularGrid<CellRegion> regionGrid;
    private final RectangularGrid<Integer> originalGrid;

    public GridMembraneBuilder(int[][] sectionArray) {
        int cols = sectionArray.length;
        int rows = sectionArray[0].length;
        graph = AutomatonGraphs.createRectangularAutomatonGraph(cols, rows);
        // fill grid
        Integer[][] boxed = new Integer[cols][rows];
        int currentColumn = 0;
        for (int[] row : sectionArray) {
            int currentRow = 0;
            for (int cell : row) {
                boxed[currentColumn][currentRow] = sectionArray[currentColumn][currentRow];
                currentRow++;
            }
            currentColumn++;
        }
        originalGrid = RectangularGrid.fromArray(boxed);
        regionMapping = new HashMap<>();
        membraneMapping = new HashMap<>();
    }

    public Map<Integer, CellRegion> getRegionMapping() {
        return regionMapping;
    }

    public void setRegionMapping(Map<Integer, CellRegion> regionMapping) {
        this.regionMapping = regionMapping;
    }

    public Map<Pair<Integer>, CellRegion> getMembraneMapping() {
        return membraneMapping;
    }

    public void setMembraneMapping(Map<Pair<Integer>, CellRegion> membraneMapping) {
        this.membraneMapping = membraneMapping;
    }

    public AutomatonGraph getGraph() {
        return graph;
    }

    public void setGraph(AutomatonGraph graph) {
        this.graph = graph;
    }

    public Map<CellRegion, Membrane> getMembranes() {
        return membranes;
    }

    public void setMembranes(Map<CellRegion, Membrane> membranes) {
        this.membranes = membranes;
    }

    public void createTopology() {
        mapRegionGrid(originalGrid);

        // associate nodes with regions
        for (int column = 0; column < originalGrid.getWidth(); column++) {
            for (int row = 0; row < originalGrid.getHeight(); row++) {
                AutomatonNode node = graph.getNode(column, row);
                node.setCellRegion(regionGrid.getValue(column, row));
            }
        }

        // setup node representations
        for (AutomatonNode node : graph.getNodes()) {
            // create rectangles centered on the nodes with side length of node distance
            Vector2D position = node.getPosition();
            double offset = Environment.convertSystemToSimulationScale(UnitRegistry.getSpace()) * 0.5;
            Vector2D topLeft = new Vector2D(position.getX() - offset, position.getY() - offset);
            Vector2D bottomRight = new Vector2D(position.getX() + offset, position.getY() + offset);
            node.setSpatialRepresentation(new Rectangle(topLeft, bottomRight));
        }

        // setup membrane and subsections
        membranes = new HashMap<>();
        for (int column = 0; column < originalGrid.getWidth(); column++) {
            for (int row = 0; row < originalGrid.getHeight(); row++) {
                Integer centerValue = originalGrid.getValue(column, row);
                RectangularCoordinate coordinate = new RectangularCoordinate(column, row);
                Map<NeumannRectangularDirection, Integer> valueMap = originalGrid.getValueMap(coordinate);
                for (Map.Entry<NeumannRectangularDirection, Integer> entry : valueMap.entrySet()) {
                    NeumannRectangularDirection direction = entry.getKey();
                    int neighborValue = entry.getValue();
                    AutomatonNode node = graph.getNode(column, row);
                    // determine and add segment
                    if (centerValue != neighborValue) {
                        Rectangle representation = (Rectangle) node.getSpatialRepresentation();
                        LineSegment segment;
                        switch (direction) {
                            case NORTH:
                                segment = representation.getTopEdge();
                                break;
                            case SOUTH:
                                segment = representation.getBottomEdge();
                                break;
                            case EAST:
                                segment = representation.getRightEdge();
                                break;
                            default:
                                segment = representation.getLeftEdge();
                                break;
                        }
                        CellRegion membraneRegion = regionGrid.getValue(column, row);
                        Membrane membrane = membranes.computeIfAbsent(membraneRegion, k -> {
                            // create new membrane
                            Membrane initializedMembrane = new Membrane(membraneRegion.getIdentifier());
                            // assign regions
                            initializedMembrane.setMembraneRegion(membraneRegion);
                            initializedMembrane.setInnerRegion(regionMapping.get(centerValue));
                            return initializedMembrane;
                        });
                        membrane.addSegment(node, segment);
                    }
                    // setup subsections
                    CellSubsection subsection = regionMapping.get(centerValue).getInnerSubsection();
                    node.addSubsectionRepresentation(subsection, node.getSpatialRepresentation());
                }
            }
        }

    }

    public void mapRegionGrid(RectangularGrid<Integer> originalGrid) {
        int cols = originalGrid.getWidth();
        int rows = originalGrid.getHeight();
        regionGrid = new RectangularGrid<>(cols, rows);
        for (int column = 0; column < cols; column++) {
            for (int row = 0; row < rows; row++) {
                RectangularCoordinate coordinate = new RectangularCoordinate(column, row);
                int center = originalGrid.getValue(coordinate);
                List<Integer> neighbours = originalGrid.getNeighboursOf(coordinate);
                boolean border = false;
                int other = center;
                for (Integer neighbour : neighbours) {
                    other = neighbour;
                    // is border
                    if (neighbour != center) {
                        border = true;
                        break;
                    }
                }
                // define region
                CellRegion region;
                if (border) {
                    Pair<Integer> pair = new Pair<>(center, other);
                    region = membraneMapping.get(pair);
                } else {
                    region = regionMapping.get(center);
                }
                regionGrid.setValue(column, row, region);
            }
        }
    }

}
