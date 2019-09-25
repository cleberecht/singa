package bio.singa.simulation.model.sections.nconcentrations;

import bio.singa.mathematics.geometry.model.Polygon;
import bio.singa.simulation.model.agents.pointlike.Vesicle;
import bio.singa.simulation.model.graphs.AutomatonNode;
import bio.singa.simulation.model.sections.CellRegion;
import bio.singa.simulation.model.simulation.Updatable;

import static bio.singa.simulation.model.sections.nconcentrations.NodeTypeCondition.isNode;
import static bio.singa.simulation.model.sections.nconcentrations.NodeTypeCondition.isVesicle;

/**
 * @author cl
 */
public class AreaCondition extends AbstractConcentrationCondition {

    private Polygon polygon;

    public AreaCondition(Polygon polygon) {
        super(0);
        this.polygon = polygon;
    }

    public static AreaCondition inPolygon(Polygon polygon) {
        return new AreaCondition(polygon);
    }

    public static AreaCondition forRegion(CellRegion region) {
        return inPolygon(region.getAreaRepresentation());
    }

    @Override
    public boolean test(Updatable updatable) {
        if (isNode().test(updatable)) {
            AutomatonNode automatonNode = (AutomatonNode) updatable;
            return automatonNode.getSubsectionRepresentations().entrySet().stream()
                    .anyMatch(entry -> entry.getValue().getCentroid().isInside(polygon));
        } else if (isVesicle().test(updatable)) {
            Vesicle vesicle = ((Vesicle) updatable);
            return vesicle.getPosition().isInside(polygon);
        }
        return false;
    }

    @Override
    public String toString() {
        return "updatable is in area of " + polygon;
    }
}
