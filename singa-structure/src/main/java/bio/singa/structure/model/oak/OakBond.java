package bio.singa.structure.model.oak;

import bio.singa.chemistry.model.CovalentBondType;

/**
 * @author cl
 */
public class OakBond {

    private final int identifier;

    protected OakAtom source;

    protected OakAtom target;

    private CovalentBondType bondType;

    public OakBond(int identifier) {
        this.identifier = identifier;
    }

    public OakBond(int identifier, CovalentBondType bondType) {
        this(identifier);
        this.bondType = bondType;
    }

    /**
     * This is a copy constructor. Creates a new bond with the same attributes as the given bond. The source and target
     * atoms of this bond are NOT copied. Due to the nature of this operation it would be bad to keep a part of the
     * relations to the lifecycle of the bond to copy. If you want to keep the neighbouring atoms, copy the
     * superordinate substructure that contains this bond and it will also traverse and copy the connected atoms.
     *
     * @param bond The bond to copy.
     */
    public OakBond(OakBond bond) {
        identifier = bond.identifier;
        bondType = bond.bondType;
    }

    public int getIdentifier() {
        return identifier;
    }

    public CovalentBondType getBondType() {
        return bondType;
    }

    public void setBondType(CovalentBondType bondType) {
        this.bondType = bondType;
    }

    public OakAtom getSource() {
        return source;
    }

    public void setSource(OakAtom source) {
        this.source = source;
    }

    public OakAtom getTarget() {
        return target;
    }

    public void setTarget(OakAtom target) {
        this.target = target;
    }

    public boolean connectsAtom(OakAtom oakAtom) {
        return source.equals(oakAtom) || target.equals(oakAtom);
    }

    public OakBond getCopy() {
        return new OakBond(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OakBond bond = (OakBond) o;
        if (identifier != bond.getIdentifier()) return false;
        return bondType == bond.bondType;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (bondType != null ? bondType.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return identifier+": "+source.getAtomName()+"-"+source.getAtomIdentifier()+" -- "+target.getAtomName()+"-"+target.getAtomIdentifier();
    }
}
