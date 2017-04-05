package de.bioforscher.singa.chemistry.descriptive.molecules;

import de.bioforscher.singa.chemistry.descriptive.elements.Element;
import de.bioforscher.singa.chemistry.descriptive.elements.ElementProvider;
import de.bioforscher.singa.mathematics.graphs.model.AbstractNode;
import de.bioforscher.singa.mathematics.vectors.Vector2D;

/**
 * Created by Christoph on 21/11/2016.
 */
public class MoleculeAtom extends AbstractNode<MoleculeAtom, Vector2D> {

    private Element element;

    public MoleculeAtom(int identifier) {
        super(identifier);
        this.element = ElementProvider.UNKOWN;
    }

    public MoleculeAtom(int identifier, Vector2D position) {
        this(identifier, position, ElementProvider.UNKOWN);
    }

    public MoleculeAtom(int identifier, Vector2D position, Element element) {
        super(identifier, position);
        this.element = element;
    }

    public Element getElement() {
        return this.element;
    }

    public void setElement(Element element) {
        this.element = element;
    }

    @Override
    public String toString() {
        return this.element.toString() + ":" + this.getIdentifier();
    }
}