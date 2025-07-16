package main.gui;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Sphere;

public class PlaneModel extends Group {
    private final PhongMaterial pathMaterial;

    public PlaneModel() {
        Box body = new Box(10, 3, 20);
        body.setMaterial(new PhongMaterial(Color.ORANGE));
        Sphere head = new Sphere(2);
        head.setTranslateZ(-10);
        head.setMaterial(new PhongMaterial(Color.RED));

        this.getChildren().addAll(body, head);

        this.pathMaterial = new PhongMaterial(Color.CYAN);
    }

    public PhongMaterial getPathMaterial() {
        return pathMaterial;
    }
}
