package bio.singa.mathematics.topology.grids.rectangular;

import bio.singa.mathematics.topology.model.DiscreteGrid;

import java.util.*;

/**
 * @author cl
 */
public class RectangularGrid<ValueType> implements DiscreteGrid<ValueType, NeumannRectangularDirection, RectangularCoordinate> {

    final ValueType[][] values;
    private final int width;
    private final int height;

    public RectangularGrid(int width, int height) {
        this.width = width;
        this.height = height;
        values = (ValueType[][]) new Object[width][height];
    }

    public static <SValueType> RectangularGrid<SValueType> fromArray(SValueType[][] array) {
        int cols = array.length;
        int rows = array[0].length;
        RectangularGrid<SValueType> originalGrid = new RectangularGrid<>(cols, rows);
        int currentRow = 0;
        for (SValueType[] row : array) {
            int currentColumn = 0;
            for (SValueType cell : row) {
                originalGrid.setValue(currentRow, currentColumn, cell);
                currentColumn++;
            }
            currentRow++;
        }
        return originalGrid;
    }

    public void setValue(int column, int row, ValueType value) {
        values[column][row] = value;
    }

    public void setValue(RectangularCoordinate coordinate, ValueType value) {
        setValue(coordinate.getColumn(), coordinate.getRow(), value);
    }

    public ValueType getValue(int column, int row) {
        return values[column][row];
    }

    @Override
    public ValueType getValue(RectangularCoordinate coordinate) {
        return getValue(coordinate.getColumn(), coordinate.getRow());
    }

    public Map<NeumannRectangularDirection, ValueType> getValueMap(RectangularCoordinate coordinate) {
        HashMap<NeumannRectangularDirection, ValueType> resultMap = new HashMap<>();
        for (NeumannRectangularDirection direction : NeumannRectangularDirection.values()) {
            RectangularCoordinate neighbour = coordinate.getNeighbour(direction);
            if (isInRange(neighbour)) {
                resultMap.put(direction, getValue(neighbour));
            }
        }
        return resultMap;
    }

    public ValueType removeValue(RectangularCoordinate coordinate) {
        ValueType nodeType = getValue(coordinate);
        setValue(coordinate, null);
        return nodeType;
    }

    public List<ValueType> getValues() {
        List<ValueType> results = new ArrayList<>();
        for (int columnIndex = 0; columnIndex < width; columnIndex++) {
            for (int rowIndex = 0; rowIndex < height; rowIndex++) {
                ValueType value = values[columnIndex][rowIndex];
                if (value != null) {
                    results.add(value);
                }
            }
        }
        return results;
    }


    public List<ValueType> getColumn(int columnIndex) {
        if (columnIndex > width - 1) {
            throw new IndexOutOfBoundsException("The row " + columnIndex + " is out of bounds.");
        }
        return Arrays.asList(values[columnIndex]);
    }

    public List<ValueType> getRow(int rowIndex) {
        if (rowIndex > height - 1) {
            throw new IndexOutOfBoundsException("The row " + rowIndex + " is out of bounds.");
        }
        List<ValueType> results = new ArrayList<>();
        for (int columnIndex = 0; columnIndex < width; columnIndex++) {
            results.add(values[columnIndex][rowIndex]);
        }
        return results;
    }

    public boolean containsValue(Object value) {
        for (int columnIndex = 0; columnIndex < width; columnIndex++) {
            for (int rowIndex = 0; rowIndex < height; rowIndex++) {
                if (getValue(columnIndex, rowIndex).equals(value)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean isInRange(RectangularCoordinate coordinate) {
        return coordinate.getColumn() < width && coordinate.getColumn() >= 0 && coordinate.getRow() < height && coordinate.getRow() >= 0;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int columnIndex = 0; columnIndex < width; columnIndex++) {
            for (int rowIndex = 0; rowIndex < height; rowIndex++) {
                sb.append(getValue(columnIndex, rowIndex)).append(" ");
            }
            sb.append(System.lineSeparator());
        }
        return sb.toString();
    }
}
