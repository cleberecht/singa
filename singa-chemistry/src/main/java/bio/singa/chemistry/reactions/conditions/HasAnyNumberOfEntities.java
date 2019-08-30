package bio.singa.chemistry.reactions.conditions;

import bio.singa.chemistry.entities.ChemicalEntity;
import bio.singa.chemistry.entities.complex.ComplexEntity;

public class HasAnyNumberOfEntities implements CandidateCondition {

    private final ChemicalEntity chemicalEntity;

    public HasAnyNumberOfEntities(ChemicalEntity chemicalEntity) {
        this.chemicalEntity = chemicalEntity;
    }

    @Override
    public boolean test(ComplexEntity graphComplex) {
        return graphComplex.countParts(chemicalEntity) != 0;
    }

}
