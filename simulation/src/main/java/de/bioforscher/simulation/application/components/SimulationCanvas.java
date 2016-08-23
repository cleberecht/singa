package de.bioforscher.simulation.application.components;

import de.bioforscher.chemistry.descriptive.ChemicalEntity;
import de.bioforscher.mathematics.vectors.Vector2D;
import de.bioforscher.simulation.application.BioGraphSimulation;
import de.bioforscher.simulation.application.renderer.GraphRenderer;
import de.bioforscher.simulation.model.BioNode;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

public class SimulationCanvas extends Canvas {

    private BioGraphSimulation owner;
    private GraphRenderer renderer;
    private BioGraphContextMenu graphContextMenu;

    public SimulationCanvas(BioGraphSimulation owner) {
        this.owner = owner;
        this.renderer = new GraphRenderer(this);
        this.graphContextMenu = new BioGraphContextMenu(this.owner.getSimulation(), this);

        this.addEventHandler(MouseEvent.MOUSE_CLICKED, this::handleClick);
        this.widthProperty().addListener(observable -> draw());
        this.heightProperty().addListener(observable -> draw());

        SimulationSpace.getInstance().getWidth().bind(this.widthProperty());
        SimulationSpace.getInstance().getHeight().bind(this.heightProperty());
    }

    private void handleClick(MouseEvent event) {
        if (event.getButton().equals(MouseButton.SECONDARY)) {
            handleRightClick(event);
        } else if (event.getButton().equals(MouseButton.PRIMARY)) {
            handleLeftClick(event);
        }
    }

    private void handleRightClick(MouseEvent event) {
        boolean isNode = false;
        for (BioNode node : this.owner.getGraph().getNodes()) {
            if (isClickedOnNode(event, node)) {
                BioNodeContextMenu bioNodeContextMenu = new BioNodeContextMenu(node, this.owner);
                bioNodeContextMenu.show(this.owner.getPlotPane(), event.getScreenX(), event.getScreenY());
                isNode = true;
                break;
            }
        }
        if (!isNode) {
            this.graphContextMenu.show(this.owner.getPlotPane(), event.getScreenX(), event.getScreenY());
        }
    }

    private void handleLeftClick(MouseEvent event) {
        for (BioNode node : this.owner.getGraph().getNodes()) {
            if (isClickedOnNode(event, node)) {
                ChemicalEntity species = this.renderer.getBioRenderingOptions().getNodeHighlightSpecies();
                node.setConcentration(species, this.owner.getConcentrationSlider().getValue());
                draw();
                break;
            }
        }
    }

    private boolean isClickedOnNode(MouseEvent event, BioNode node) {
        return node.getPosition().isNearVector(new Vector2D(event.getX()+this.renderer.getOptions().getStandardNodeDiameter() / 2,
                        event.getY()+this.renderer.getOptions().getStandardNodeDiameter() / 2),
                this.renderer.getOptions().getStandardNodeDiameter() / 2);
    }

    public GraphRenderer getRenderer() {
        return this.renderer;
    }

    public void setRenderer(GraphRenderer renderer) {
        this.renderer = renderer;
    }

    public void draw() {
        this.renderer.render(this.owner.getGraph());
    }

    public void resetGraphContextMenu() {
        this.graphContextMenu = new BioGraphContextMenu(this.owner.getSimulation(), this);
    }


}
