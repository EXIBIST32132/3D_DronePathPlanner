package main.gui;

import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Translate;
import main.path.Waypoint;

public class WaypointNode extends Sphere {
    private final Waypoint waypoint;
    private final Translate translate;

    private double mouseX, mouseY;

    public WaypointNode(Waypoint waypoint) {
        super(2.5);
        this.waypoint = waypoint;
        this.translate = new Translate(waypoint.x, waypoint.y, waypoint.z);
        this.getTransforms().add(translate);

        this.setMaterial(new PhongMaterial(Color.YELLOW));

        // Enable drag
        this.setOnMousePressed(this::onMousePressed);
        this.setOnMouseDragged(this::onMouseDragged);
    }

    private void onMousePressed(MouseEvent e) {
        mouseX = e.getSceneX();
        mouseY = e.getSceneY();
    }

    private void onMouseDragged(MouseEvent e) {
        double dx = e.getSceneX() - mouseX;
        double dy = e.getSceneY() - mouseY;

        translate.setX(translate.getX() + dx * 0.2);
        translate.setY(translate.getY() - dy * 0.2); // Y is inverted in 3D
        waypoint.x = translate.getX();
        waypoint.y = translate.getY();

        mouseX = e.getSceneX();
        mouseY = e.getSceneY();
    }

    public Waypoint getWaypoint() {
        return waypoint;
    }
}
