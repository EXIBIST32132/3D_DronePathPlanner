package main.gui;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class DashboardPanel extends VBox {
    private final Label gpsLabel = new Label("GPS: -");
    private final Label altitudeLabel = new Label("Altitude: -");
    private final Label headingLabel = new Label("Heading: -");

    public DashboardPanel() {
        this.setSpacing(10);
        this.getChildren().addAll(gpsLabel, altitudeLabel, headingLabel);
        this.setStyle("-fx-background-color: #2e2e2e; -fx-padding: 10; -fx-text-fill: white;");
        gpsLabel.setStyle("-fx-text-fill: white;");
        altitudeLabel.setStyle("-fx-text-fill: white;");
        headingLabel.setStyle("-fx-text-fill: white;");
    }

    public void update(String gps, String alt, String heading) {
        gpsLabel.setText("GPS: " + gps);
        altitudeLabel.setText("Altitude: " + alt);
        headingLabel.setText("Heading: " + heading);
    }
}
