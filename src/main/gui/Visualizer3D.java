package main.gui;

import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.Sphere;
import main.comm.SerialReceiver;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import main.path.BezierCurve;
import main.path.Waypoint;

import java.util.ArrayList;
import java.util.List;

public class Visualizer3D extends Application {
    private final List<Waypoint> waypoints = new ArrayList<>();
    private final Group sceneRoot = new Group();
    private final Group pathGroup = new Group();
    private final Group nodeGroup = new Group();
    private final PlaneModel plane = new PlaneModel();
    private BezierCurve currentPath;
    private int pathIndex = 0;

    private SerialReceiver serialReceiver;
    private DashboardPanel dashboard;

    @Override
    public void start(Stage stage) {
        dashboard = new DashboardPanel();

        SubScene subScene = new SubScene(sceneRoot, 900, 600, true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.web("#1e1e1e"));
        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setTranslateZ(-600);
        subScene.setCamera(camera);

        sceneRoot.getChildren().addAll(pathGroup, nodeGroup, plane);
        addGridSpace(sceneRoot);

        BorderPane layout = new BorderPane();
        layout.setCenter(subScene);
        layout.setRight(dashboard);
        BorderPane.setMargin(dashboard, new Insets(10));

        Scene scene = new Scene(layout, 1200, 600, true);
        stage.setScene(scene);
        stage.setTitle("Drone Mission Visualizer");
        stage.show();

        subScene.setOnMouseClicked(e -> {
            Waypoint wp = new Waypoint(e.getX() - 450, e.getY() - 300, 0);
            waypoints.add(wp);
            WaypointNode node = new WaypointNode(wp);
            nodeGroup.getChildren().add(node);
            updatePath();
        });

        serialReceiver = new SerialReceiver("COM3", dashboard);
        serialReceiver.start();

        startPlaneAnimation();
    }

    private void updatePath() {
        pathGroup.getChildren().clear();
        if (waypoints.size() < 2) return;
        currentPath = new BezierCurve(waypoints);
        for (Waypoint wp : currentPath.getInterpolatedPoints()) {
            Sphere s = new Sphere(1);
            s.setTranslateX(wp.x);
            s.setTranslateY(wp.y);
            s.setTranslateZ(wp.z);
            s.setMaterial(plane.getPathMaterial());
            pathGroup.getChildren().add(s);
        }
    }

    private void startPlaneAnimation() {
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (currentPath == null || pathIndex >= currentPath.getInterpolatedPoints().size()) return;
                Waypoint wp = currentPath.getInterpolatedPoints().get(pathIndex++);
                plane.setTranslateX(wp.x);
                plane.setTranslateY(wp.y);
                plane.setTranslateZ(wp.z);
            }
        };
        timer.start();
    }

    private void addGridSpace(Group root) {
        Box space = new Box(400, 400, 400);
        space.setMaterial(new PhongMaterial(Color.color(0.2, 0.2, 0.2, 0.1)));
        space.setDrawMode(DrawMode.LINE);
        root.getChildren().add(space);
    }
}
