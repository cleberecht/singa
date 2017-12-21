package de.bioforscher.singa.mathematics.topology.grids;

public class Cell {

    private final short q;
    private final short r;
    private CellState state;

    public Cell(short q, short r) {
        this(q, r, CellState.Alive);
    }

    public Cell(int q, int r) {
        this(q, r, CellState.Alive);
    }

    public Cell(int q, int r, CellState state) {
        this((short) q, (short) r, state);
    }

    public Cell(short q, short r, CellState state) {
        this.q = q;
        this.r = r;
        this.state = state;
    }

    public short getQ() {
        return q;
    }

    public short getR() {
        return r;
    }

    public CellState getState() {
        return state;
    }

    public void setState(CellState state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return "Cell [q=" + q + ", r=" + r + "]";
    }

}
