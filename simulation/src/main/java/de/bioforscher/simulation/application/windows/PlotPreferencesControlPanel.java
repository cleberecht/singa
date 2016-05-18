package de.bioforscher.simulation.application.windows;

import de.bioforscher.simulation.util.SingaPerferences;
import javafx.event.ActionEvent;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

public class PlotPreferencesControlPanel extends GridPane {

    private Spinner<Integer> spDataPoints;
    private Spinner<Integer> spTickSpacing;

    private Stage owner;
    private SingaPerferences preferences;

    public PlotPreferencesControlPanel(Stage owner) {
        super();
        this.preferences = new SingaPerferences();
        this.owner = owner;
        this.initialize();
    }

    public void initialize() {

        this.setAlignment(Pos.CENTER);
        this.setHgap(10);
        this.setVgap(10);
        this.setPadding(new Insets(10, 10, 10, 10));

        ColumnConstraints column1 = new ColumnConstraints();
        column1.setHalignment(HPos.LEFT);
        this.getColumnConstraints().add(column1);

        ColumnConstraints column2 = new ColumnConstraints();
        column2.setHalignment(HPos.RIGHT);
        this.getColumnConstraints().add(column2);

        int maximalPoints = preferences.preferences.getInt(SingaPerferences.Plot.MAXIMAL_DATA_POINTS, SingaPerferences.Plot.MAXIMAL_DATA_POINTS_VALUE);
        int tickSpacing = preferences.preferences.getInt(SingaPerferences.Plot.TICK_SPACING, SingaPerferences.Plot.TICK_SPACING_VALUE);

        TextFlow description = new TextFlow();
        description.getChildren().add(new Text("The provided options can be used to customize plots."));
        this.add(description, 0, 0, 2, 1);

        Separator separator1 = new Separator();
        separator1.setOrientation(Orientation.HORIZONTAL);
        this.add(separator1, 0, 1, 2, 1);

        Label labDataPoints = new Label("Maximal number of values:");
        this.add(labDataPoints, 0, 2, 1, 1);

        spDataPoints = new Spinner<>(1, 1000, maximalPoints);
        this.add(spDataPoints, 1, 2, 1, 1);

        Label labTickSpacing = new Label("Tick spacing:");
        this.add(labTickSpacing, 0, 3, 1, 1);

        spTickSpacing = new Spinner<>(1, 1000, tickSpacing);
        this.add(spTickSpacing, 1, 3, 1, 1);

        Separator separator2 = new Separator();
        separator2.setOrientation(Orientation.HORIZONTAL);
        this.add(separator2, 0, 5, 2, 1);

        Button btnDefaults = new Button("Resore Defaults");
        btnDefaults.setMaxWidth(Double.MAX_VALUE);
        btnDefaults.setOnAction(this::restoreDefault);
        this.add(btnDefaults, 1, 4, 1, 1);

        Button btnApply = new Button("Apply");
        btnApply.setMaxWidth(Double.MAX_VALUE);
        btnApply.setStyle("-fx-font-weight: bold;");
        btnApply.setOnAction(this::applyChanges);
        this.add(btnApply, 1, 6, 1, 1);

        Button btnCancel = new Button("Cancel");
        btnCancel.setOnAction(this::discardChanges);
        btnCancel.setMaxWidth(Double.MAX_VALUE);
        this.add(btnCancel, 0, 6, 1, 1);
    }

    public void applyChanges(ActionEvent event) {
        this.preferences.preferences.putInt(SingaPerferences.Plot.MAXIMAL_DATA_POINTS, spDataPoints.getValue());
        this.preferences.preferences.putInt(SingaPerferences.Plot.TICK_SPACING, spTickSpacing.getValue());
        this.owner.close();
    }

    public void restoreDefault(ActionEvent event) {
        this.preferences.restorePlotDefaults();
        this.spDataPoints.getValueFactory().setValue(SingaPerferences.Plot.MAXIMAL_DATA_POINTS_VALUE);
        this.spTickSpacing.getValueFactory().setValue(SingaPerferences.Plot.TICK_SPACING_VALUE);
    }

    public void discardChanges(ActionEvent event) {
        this.owner.close();
    }

}
