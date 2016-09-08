package de.bioforscher.javafx.renderer;

import de.bioforscher.mathematics.geometry.edges.Line;
import de.bioforscher.mathematics.geometry.edges.Parabola;
import de.bioforscher.mathematics.vectors.Vector2D;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/**
 * Created by Christoph on 28.08.2016.
 */
public class RendererExamples extends Application implements Renderer {

    private GraphicsContext gc;
    private Canvas canvas;

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {

        this.canvas = new Canvas(500, 500);
        this.gc = this.canvas.getGraphicsContext2D();

        BorderPane root = new BorderPane();
        root.setCenter(this.canvas);

        // add axis
        this.gc.setLineWidth(5);
        this.gc.setStroke(Color.BLACK);

        Line xAxis = new Line(0,0);
        Line yAxis = new Line(0, Double.POSITIVE_INFINITY);

        drawLine(xAxis);
        drawLine(yAxis);

        this.gc.setStroke(Color.INDIANRED);
        Vector2D focus = new Vector2D(150,70);
        drawPoint(focus);

        this.gc.setLineWidth(2);
        this.gc.setFill(Color.CORAL);
        Line directrix = new Line(50, 0);
        drawLine(directrix);

        Parabola parabola = new Parabola(focus, directrix);
        drawParabola(parabola, 30);

        Line randomLine = new Line(70, 0.1);
        this.gc.setStroke(Color.DARKGOLDENROD);
        drawLine(randomLine);
        this.gc.setLineWidth(5);
        this.gc.setFill(Color.BROWN);
        parabola.getIntercepts(randomLine).forEach(this::drawPoint);

        // show
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.show();

    }

    @Override
    public Canvas getCanvas() {
        return this.canvas;
    }

}