package main;

import javafx.application.Application;
import javafx.scene.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.Sphere;
import javafx.stage.Stage;

public class Test3D extends Application {

    @Override
    public void start(Stage primaryStage) {
        Group root3D = new Group();

        // Add Cube Outline (Transparent)
        Box cube = new Box(400, 400, 400);
        cube.setTranslateX(0);
        cube.setTranslateY(0);
        cube.setTranslateZ(0);
        cube.setMaterial(new PhongMaterial(Color.TRANSPARENT));
        cube.setDrawMode(DrawMode.LINE);
        root3D.getChildren().add(cube);

        // Add 3D Plane (for now, just a Box)
        Box plane = new Box(20, 10, 40); // Bigger size so it's visible
        plane.setTranslateX(0);
        plane.setTranslateY(0);
        plane.setTranslateZ(0);
        plane.setMaterial(new PhongMaterial(Color.BLUE));
        root3D.getChildren().add(plane);

        // Add Waypoint Sphere (just one for now)
        Sphere waypoint = new Sphere(10);
        waypoint.setMaterial(new PhongMaterial(Color.RED));
        waypoint.setTranslateX(100);
        waypoint.setTranslateY(0);
        waypoint.setTranslateZ(0);
        root3D.getChildren().add(waypoint);

        // Lighting
        AmbientLight ambientLight = new AmbientLight(Color.color(0.4, 0.4, 0.4));
        PointLight pointLight = new PointLight(Color.WHITE);
        pointLight.setTranslateX(-500);
        pointLight.setTranslateY(-300);
        pointLight.setTranslateZ(-500);
        root3D.getChildren().addAll(ambientLight, pointLight);

        // Camera
        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setTranslateZ(-800);
        camera.setTranslateY(-100);
        camera.setNearClip(0.1);
        camera.setFarClip(10000);
        camera.setFieldOfView(35);

        SubScene subScene = new SubScene(root3D, 800, 600, true, SceneAntialiasing.BALANCED);
        subScene.setCamera(camera);
        subScene.setFill(Color.DARKGRAY);

        // Put subscene in main UI
        Group root = new Group();
        root.getChildren().add(subScene);

        Scene scene = new Scene(root, 800, 600, true);
        primaryStage.setTitle("3D Drone Visualizer");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
