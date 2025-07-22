package main.gui;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Sphere;

public class PlaneModel extends Group {
    public PlaneModel() {
        Box body = new Box(10, 3, 20);
        body.setMaterial(new PhongMaterial(Color.ORANGE));

        Sphere nose = new Sphere(2);
        nose.setTranslateZ(-10);
        nose.setMaterial(new PhongMaterial(Color.RED));

        this.getChildren().addAll(body, nose);
    }
}