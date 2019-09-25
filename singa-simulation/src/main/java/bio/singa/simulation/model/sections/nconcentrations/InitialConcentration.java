package bio.singa.simulation.model.sections.nconcentrations;

import bio.singa.chemistry.entities.ChemicalEntity;
import bio.singa.features.model.Evidence;
import bio.singa.features.quantities.MolarConcentration;
import bio.singa.simulation.model.sections.CellSubsection;
import bio.singa.simulation.model.sections.CellTopology;
import bio.singa.simulation.model.simulation.Simulation;
import bio.singa.simulation.model.simulation.Updatable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.units.indriya.ComparableQuantity;

import javax.measure.Quantity;
import javax.measure.quantity.Time;
import java.util.TreeMap;

/**
 * @author cl
 */
public class InitialConcentration {

    private static final Logger logger = LoggerFactory.getLogger(InitialConcentration.class);

    private TreeMap<Integer, ConcentrationCondition> conditions;
    private CellSubsection subsection;
    private CellTopology topology;
    private ChemicalEntity entity;
    private Quantity<MolarConcentration> concentration;
    private ComparableQuantity<Time> time;
    private boolean fix;

    private Evidence evidence;

    InitialConcentration() {
        conditions = new TreeMap<>();
    }

    TreeMap<Integer, ConcentrationCondition> getConditions() {
        return conditions;
    }

    void setConditions(TreeMap<Integer, ConcentrationCondition> conditions) {
        this.conditions = conditions;
    }

    ChemicalEntity getEntity() {
        return entity;
    }

    void setEntity(ChemicalEntity entity) {
        this.entity = entity;
    }

    Quantity<MolarConcentration> getConcentration() {
        return concentration;
    }

    void setConcentration(Quantity<MolarConcentration> concentration) {
        this.concentration = concentration;
    }

    ComparableQuantity<Time> getTime() {
        return time;
    }

    void setTime(ComparableQuantity<Time> time) {
        this.time = time;
    }

    boolean isFix() {
        return fix;
    }

    void setFix(boolean fix) {
        this.fix = fix;
    }

    Evidence getEvidence() {
        return evidence;
    }

    void setEvidence(Evidence evidence) {
        this.evidence = evidence;
    }

    public void addCondition(ConcentrationCondition condition) {
        conditions.put(condition.getPriority(), condition);
        if (condition instanceof TopologyCondition) {
            topology = ((TopologyCondition) condition).getTopology();
            if (subsection != null) {
                logger.warn("Topology and subsection conditions have been set for the initial concentration. Subsection will be used.");
            }
        }
        if (condition instanceof SectionCondition) {
            subsection = ((SectionCondition) condition).getSubsection();
            if (topology != null) {
                logger.warn("Topology and subsection conditions have been set for the initial concentration. Subsection will be used.");
            }
        }
    }

    public boolean test(Updatable updatable) {
        for (ConcentrationCondition condition : conditions.values()) {
            // return if any condition fails
            if (!condition.test(updatable)) {
                return false;
            }
        }
        return true;
    }

    public void apply(Updatable updatable) {
        if (test(updatable)) {
            if (subsection != null) {
                updatable.getConcentrationContainer().initialize(subsection, entity, concentration);
            } else {
                updatable.getConcentrationContainer().initialize(topology, entity, concentration);
            }
        }
    }

    public void register(Simulation simulation) {

    }

}
