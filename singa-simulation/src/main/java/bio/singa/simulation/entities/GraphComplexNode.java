package bio.singa.simulation.entities;

import bio.singa.mathematics.geometry.faces.Rectangle;
import bio.singa.mathematics.graphs.model.AbstractNode;
import bio.singa.mathematics.vectors.Vector2D;
import bio.singa.mathematics.vectors.Vectors2D;

import java.util.ArrayList;
import java.util.List;

/**
 * @author cl
 */
public class GraphComplexNode extends AbstractNode<GraphComplexNode, Vector2D, Integer> {

    private static Rectangle rectange = new Rectangle(1,1);

    private ChemicalEntity entity;
    private List<BindingSite> bindingSites;

    public GraphComplexNode(Integer identifier) {
        super(identifier, Vectors2D.generateRandom2DVector(rectange));
        bindingSites = new ArrayList<>();
    }

    public GraphComplexNode(Integer identifier, Vector2D position) {
        super(identifier, position);
    }

    private GraphComplexNode(GraphComplexNode node) {
        super(node.getIdentifier(), node.getPosition().getCopy());
        entity = node.getEntity();
        bindingSites = new ArrayList<>(node.getBindingSites());
    }

    public ChemicalEntity getEntity() {
        return entity;
    }

    public void setEntity(ChemicalEntity entity) {
        this.entity = entity;
    }

    public boolean isEntity(ChemicalEntity entity) {
        return this.entity.equals(entity);
    }

    public List<BindingSite> getBindingSites() {
        return bindingSites;
    }

    public void setBindingSites(List<BindingSite> bindingSites) {
        this.bindingSites = bindingSites;
    }

    public void addBindingSite(BindingSite bindingSite) {
        bindingSites.add(bindingSite);
    }

    public boolean hasBindingSite(BindingSite bindingSite) {
        return bindingSites.contains(bindingSite);
    }

    @Override
    public GraphComplexNode getCopy() {
        return new GraphComplexNode(this);
    }

    @Override
    public String toString() {
        return entity.getIdentifier();
    }
}
