package bio.singa.simulation.model.rules;

import bio.singa.simulation.entities.ChemicalEntity;
import bio.singa.simulation.model.graphs.AutomatonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @author cl
 */
public class AssignmentRule {

    private static final Logger logger = LoggerFactory.getLogger(AssignmentRule.class);

    // private final AppliedExpression expression;

    private ChemicalEntity targetEntity;
    private Map<ChemicalEntity, String> entityReference;

    public AssignmentRule(ChemicalEntity targetEntity) {
        this.targetEntity = targetEntity;
        // this.expression = expression;
        entityReference = new HashMap<>();
    }

    public ChemicalEntity getTargetEntity() {
        return targetEntity;
    }

    public void setTargetEntity(ChemicalEntity targetEntity) {
        this.targetEntity = targetEntity;
    }

    public void referenceChemicalEntityToParameter(String parameterIdentifier, ChemicalEntity entity) {
        entityReference.put(entity, parameterIdentifier);
        // FIXME this is not done correctly
        // expression.setParameter(new Parameter<>(parameterIdentifier, Environment.emptyConcentration()));
    }

    public Map<ChemicalEntity, String> getEntityReference() {
        return entityReference;
    }

    public void setEntityReference(Map<ChemicalEntity, String> entityReference) {
        this.entityReference = entityReference;
    }

    public void applyRule(AutomatonNode node) {
        // set entity parameters
        for (Map.Entry<ChemicalEntity, String> entry : entityReference.entrySet()) {
            double concentration = node.getConcentrationContainer().get(node.getConcentrationContainer().getInnerSubsection(), entry.getKey());
            String parameterName = entityReference.get(entry.getKey());
            // expression.acceptValue(parameterName, concentration.getValue().doubleValue());
        }
        // Quantity<?> concentration = expression.evaluate();
        // logger.debug("Initialized concentration of {} to {}.", targetEntity.getIdentifier(), concentration);
        // node.getConcentrationContainer().set(node.getConcentrationContainer().getInnerSubsection(), targetEntity, concentration.getValue().doubleValue());
    }

}
