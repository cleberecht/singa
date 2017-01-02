package de.bioforscher.chemistry.physical.branches;

import de.bioforscher.mathematics.vectors.Vector3D;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * To handle structures with multiple models (of NMR).
 */
public class StructuralModel extends BranchSubstructure<StructuralModel> {

    /**
     * Creates a new BranchSubstructure. The identifier is considered in the superordinate BranchSubstructure.
     *
     * @param identifier The identifier of this BranchSubstructure.
     */
    public StructuralModel(int identifier) {
        super(identifier);
    }

    public StructuralModel(StructuralModel structuralModel) {
        super(structuralModel);
    }

    public List<Chain> getAllChains() {
        return this.getSubstructures().stream()
                .map(Chain.class::cast)
                .collect(Collectors.toList());
    }

    @Override
    public StructuralModel getCopy() {
        return new StructuralModel(this);
    }

    @Override
    public void setPosition(Vector3D position) {
        //FIXME not yet implemented
        throw new NotImplementedException();
    }
}