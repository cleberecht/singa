package bio.singa.simulation.reactions.modifications;

import bio.singa.simulation.entities.ChemicalEntity;
import bio.singa.simulation.entities.BindingSite;
import bio.singa.simulation.entities.ComplexEntity;

/**
 * @author cl
 */
public class RemoveModification extends AbstractComplexEntityModification {


    public RemoveModification(BindingSite bindingSite, ChemicalEntity entityToRemove) {
        super(bindingSite);
        setSecondaryEntity(entityToRemove);
    }

    @Override
    public void apply() {
        if (getCandidates().size() != 1) {
            logger.warn("Remove modifications only accept one candidate and return one result.");
        }
        ComplexEntity candidate = getCandidates().get(0);
        apply(candidate);
    }

    public void apply(ComplexEntity complex) {
        complex.remove(getSecondaryEntity(), getBindingSite())
                .ifPresent(this::addResult);
    }

    @Override
    public String toString() {
        return String.format("removing %s at %s", getSecondaryEntity(), getBindingSite());
    }

    @Override
    public ComplexEntityModification invert() {
        throw new IllegalStateException("Not yet implemented");
    }

}
