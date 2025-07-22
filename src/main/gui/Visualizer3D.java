
package main.gui;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.*;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;
import main.path.BezierCurve;
import main.path.Waypoint;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import main.gui.DashboardPanel;
import main.gui.UIControls;
import main.gui.WaypointNode;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.input.MouseButton;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.TextInputDialog;
import java.util.Optional;
import java.util.HashMap;

import java.util.ArrayList;
import java.util.List;
import javafx.stage.FileChooser;
import java.io.FileWriter;
import java.io.IOException;

public class Visualizer3D extends Application {
    private final Group sceneRoot = new Group();
    private final Group waypointGroup = new Group();
    private final Group pathGroup = new Group();
    private final PlaneModel plane = new PlaneModel();
    private final List<Waypoint> waypoints = new ArrayList<>();
    private final PerspectiveCamera camera = new PerspectiveCamera(true);

    private SubScene subScene;
    private double anchorX, anchorY;
    private double anchorAngleX = 0;
    private double anchorAngleY = 0;
    private final Rotate rotateX = new Rotate(0, Rotate.X_AXIS);
    private final Rotate rotateY = new Rotate(0, Rotate.Y_AXIS);

    private TextField xField = new TextField();
    private TextField yField = new TextField();
    private TextField zField = new TextField();
    private TextArea consoleOutput = new TextArea();
    private final List<WaypointNode> waypointNodes = new ArrayList<>();
    private int selectedWaypointIndex = -1;
    private VBox waypointListBox = new VBox(4);

    // Path management
    private final HashMap<String, List<WaypointNode>> paths = new HashMap<>();
    private String currentPathName = "Path 1";
    private VBox pathListBox = new VBox(4);
    private VBox pathManagerBox = new VBox(8);
    private int pathCounter = 1;
    private boolean simMode = true;

    private AnimationTimer simAnimation;
    private int simIndex = 0;
    private List<Waypoint> simPath = null;

    @Override
    public void start(Stage stage) {
        sceneRoot.getChildren().addAll(waypointGroup, pathGroup, plane);
        buildDottedCube(sceneRoot);

        // Add a large, visible box at the origin for debugging
        Box debugBox = new Box(50, 50, 50);
        debugBox.setMaterial(new PhongMaterial(Color.GREENYELLOW));
        debugBox.setTranslateX(0);
        debugBox.setTranslateY(0);
        debugBox.setTranslateZ(0);
        sceneRoot.getChildren().add(debugBox);

        Group world = new Group(sceneRoot);
        world.getTransforms().addAll(rotateX, rotateY);

        subScene = new SubScene(world, 900, 600, true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.DARKSLATEBLUE); // Debug background
        subScene.setCamera(camera);
        subScene.setHeight(900);
        subScene.setWidth(600);
        camera.setTranslateZ(-600);
        camera.setTranslateY(-50); // Slightly above center for better view

        log("Camera position: Z=" + camera.getTranslateZ() + ", Y=" + camera.getTranslateY());
        log("SubScene size: W=" + subScene.getWidth() + ", H=" + subScene.getHeight());

        initMouseControl(world);

        // Sidebar (drawer) setup
        VBox sidebarContent = new VBox(18);
        sidebarContent.setPadding(new Insets(18));
        sidebarContent.setStyle("-fx-background-color: #23272e; -fx-min-width: 280px; -fx-max-width: 320px;");

        // Title and status
        HBox titleBar = new HBox(8);
        ImageView logo = new ImageView(new Image("/main/gui/pathplanner_icon.png", 32, 32, true, true)); // Placeholder icon
        Label title = new Label("PathPlanner 3D");
        title.setStyle("-fx-font-size: 20px; -fx-text-fill: #f8f8f2; -fx-font-weight: bold;");
        Label status = new Label("● Connected");
        status.setStyle("-fx-text-fill: #4caf50; -fx-font-size: 14px;");
        titleBar.getChildren().addAll(logo, title, status);

        // Controls section
        TitledPane controlsPane = new TitledPane();
        controlsPane.setText("Controls");
        UIControls controls = new UIControls();
        controls.setStyle("-fx-background-color: transparent;");
        controlsPane.setContent(controls);
        controlsPane.setExpanded(true);
        controlsPane.setCollapsible(true);
        controlsPane.setTooltip(new Tooltip("Path and waypoint controls"));

        // Waypoint input fields
        HBox waypointInput = new HBox(6);
        xField.setPromptText("X");
        yField.setPromptText("Y");
        zField.setPromptText("Z");
        waypointInput.getChildren().addAll(xField, yField, zField);
        waypointInput.setStyle("-fx-background-color: transparent;");
        Button addBtn = new Button("Add");
        addBtn.setTooltip(new Tooltip("Add waypoint at X, Y, Z"));
        addBtn.setOnAction(e -> addWaypoint());
        waypointInput.getChildren().add(addBtn);

        // Path buttons
        HBox pathButtons = new HBox(6);
        Button pathBtn = new Button("Generate Path");
        pathBtn.setTooltip(new Tooltip("Generate Bezier path from waypoints"));
        pathBtn.setOnAction(e -> generatePath());
        Button clearBtn = new Button("Clear All");
        clearBtn.setTooltip(new Tooltip("Clear all waypoints and paths"));
        clearBtn.setOnAction(e -> clearAll());
        pathButtons.getChildren().addAll(pathBtn, clearBtn);

        // Console output
        consoleOutput.setEditable(false);
        consoleOutput.setPrefRowCount(4);
        consoleOutput.setStyle("-fx-control-inner-background: #181a20; -fx-text-fill: #bfbfbf;");

        // Telemetry section
        TitledPane telemetryPane = new TitledPane();
        telemetryPane.setText("Telemetry");
        DashboardPanel dashboard = new DashboardPanel();
        telemetryPane.setContent(dashboard);
        telemetryPane.setExpanded(true);
        telemetryPane.setCollapsible(true);
        telemetryPane.setTooltip(new Tooltip("Live drone telemetry"));

        // Waypoint list with reorder controls
        Label waypointListLabel = new Label("Waypoints:");
        waypointListLabel.setStyle("-fx-text-fill: #bfbfbf; -fx-font-size: 14px;");
        waypointListBox.setStyle("-fx-background-color: transparent;");
        updateWaypointListUI();

        // Mode toggle (Sim/Real)
        HBox modeToggleBox = new HBox(8);
        ToggleGroup modeGroup = new ToggleGroup();
        ToggleButton simBtn = new ToggleButton("Sim");
        ToggleButton realBtn = new ToggleButton("Real");
        simBtn.setToggleGroup(modeGroup);
        realBtn.setToggleGroup(modeGroup);
        simBtn.setSelected(true);
        simBtn.setOnAction(e -> switchMode(true));
        realBtn.setOnAction(e -> switchMode(false));
        modeToggleBox.getChildren().addAll(new Label("Mode:"), simBtn, realBtn);
        modeToggleBox.setStyle("-fx-padding: 0 0 8 0;");

        // Path manager UI
        Label pathListLabel = new Label("Paths:");
        pathListLabel.setStyle("-fx-text-fill: #bfbfbf; -fx-font-size: 14px;");
        pathListBox.setStyle("-fx-background-color: transparent;");
        updatePathListUI();
        Button addPathBtn = new Button("+");
        addPathBtn.setTooltip(new Tooltip("Add new path"));
        addPathBtn.setOnAction(e -> addNewPath());
        pathManagerBox.getChildren().addAll(pathListLabel, pathListBox, addPathBtn);
        pathManagerBox.setStyle("-fx-background-color: transparent;");

        // Add all to sidebarContent
        sidebarContent.getChildren().addAll(titleBar, controlsPane, new Label("Waypoint (X Y Z):"), waypointInput, pathButtons, waypointListLabel, waypointListBox, new Label("Console Output:"), consoleOutput, telemetryPane, modeToggleBox, pathManagerBox);
        VBox.setVgrow(consoleOutput, Priority.ALWAYS);

        // Make sidebar scrollable
        ScrollPane sidebar = new ScrollPane(sidebarContent);
        sidebar.setFitToWidth(true);
        sidebar.setStyle("-fx-background: #23272e; -fx-border-color: #444; -fx-border-width: 0 1 0 0;");
        sidebar.setPrefWidth(320);
        sidebar.setMinWidth(280);
        sidebar.setMaxWidth(340);

        // Add a default path for visibility
        if (waypointNodes.isEmpty()) {
            addDefaultPath();
        }

        // Layout
        BorderPane layout = new BorderPane();
        layout.setCenter(subScene);
        layout.setLeft(sidebar);
        BorderPane.setMargin(sidebar, new Insets(0, 12, 0, 0));

        Scene scene = new Scene(layout, 1300, 700);
        scene.getStylesheets().add(getClass().getResource("/main/gui/pathplanner_dark.css").toExternalForm()); // Placeholder for custom CSS
        stage.setScene(scene);
        stage.setTitle("PathPlanner 3D");
        stage.show();

        startSimAnimation();
        log("Application started. Ready to add waypoints.");
    }

    private VBox buildUIControls() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(10));
        box.setStyle("-fx-background-color: #2e2e2e");

        xField.setPromptText("X");
        yField.setPromptText("Y");
        zField.setPromptText("Z");

        Button addBtn = new Button("Add Waypoint");
        Button pathBtn = new Button("Generate Path");
        Button clearBtn = new Button("Clear All");
        Button exportBtn = new Button("Export CSV");
        exportBtn.setStyle("-fx-background-color: #444a54; -fx-text-fill: #fff; -fx-font-size: 13px; -fx-padding: 4 12 4 12;");
        exportBtn.setOnAction(e -> exportCurrentPathToCSV());

        addBtn.setOnAction(e -> addWaypoint());
        pathBtn.setOnAction(e -> generatePath());
        clearBtn.setOnAction(e -> clearAll());

        consoleOutput.setEditable(false);
        consoleOutput.setPrefRowCount(6);

        box.getChildren().addAll(
                new Label("Waypoint (X Y Z):"), xField, yField, zField,
                addBtn, pathBtn, clearBtn, exportBtn,
                new Label("Console Output:"), consoleOutput
        );
        return box;
    }

    private void updateWaypointListUI() {
        waypointListBox.getChildren().clear();
        for (int i = 0; i < waypointNodes.size(); i++) {
            int idx = i;
            WaypointNode node = waypointNodes.get(i);
            Waypoint wp = node.getWaypoint();
            HBox row = new HBox(6);
            row.setStyle("-fx-background-color: " + (idx == selectedWaypointIndex ? "#3a3f4b" : "transparent") + "; -fx-padding: 2 0 2 0;");
            Label coord = new Label(String.format("(%.1f, %.1f, %.1f)", wp.x, wp.y, wp.z));
            coord.setStyle("-fx-text-fill: #e0e0e0; -fx-font-size: 13px;");
            Button upBtn = new Button("↑");
            upBtn.setTooltip(new Tooltip("Move up"));
            upBtn.setDisable(idx == 0);
            upBtn.setOnAction(e -> {
                if (idx > 0) {
                    swapWaypoints(idx, idx - 1);
                }
            });
            Button downBtn = new Button("↓");
            downBtn.setTooltip(new Tooltip("Move down"));
            downBtn.setDisable(idx == waypointNodes.size() - 1);
            downBtn.setOnAction(e -> {
                if (idx < waypointNodes.size() - 1) {
                    swapWaypoints(idx, idx + 1);
                }
            });
            row.getChildren().addAll(coord, upBtn, downBtn);
            row.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY) {
                    selectedWaypointIndex = idx;
                    highlightSelectedWaypoint();
                    updateWaypointListUI();
                }
            });
            waypointListBox.getChildren().add(row);
        }
    }

    private void swapWaypoints(int i, int j) {
        // Swap in both lists
        WaypointNode tmpNode = waypointNodes.get(i);
        waypointNodes.set(i, waypointNodes.get(j));
        waypointNodes.set(j, tmpNode);
        Waypoint tmpWp = waypoints.get(i);
        waypoints.set(i, waypoints.get(j));
        waypoints.set(j, tmpWp);
        // Update 3D order
        waypointGroup.getChildren().clear();
        waypointGroup.getChildren().addAll(waypointNodes);
        generatePath();
        updateWaypointListUI();
    }

    private void highlightSelectedWaypoint() {
        for (int i = 0; i < waypointNodes.size(); i++) {
            WaypointNode node = waypointNodes.get(i);
            if (i == selectedWaypointIndex) {
                node.setMaterial(new PhongMaterial(Color.LIME));
                node.setScaleX(1.5);
                node.setScaleY(1.5);
                node.setScaleZ(1.5);
            } else {
                node.setMaterial(new PhongMaterial(Color.YELLOW));
                node.setScaleX(1.0);
                node.setScaleY(1.0);
                node.setScaleZ(1.0);
            }
        }
    }

    private void addWaypoint() {
        try {
            double x = Double.parseDouble(xField.getText());
            double y = Double.parseDouble(yField.getText());
            double z = Double.parseDouble(zField.getText());
            Waypoint wp = new Waypoint(x, y, z);
            WaypointNode node = new WaypointNode(wp);
            waypointNodes.add(node);
            waypoints.add(wp);
            node.setOnMousePressed(e -> {
                node.setScaleX(1.5);
                node.setScaleY(1.5);
                node.setScaleZ(1.5);
                node.setMaterial(new PhongMaterial(Color.LIME));
            });
            node.setOnMouseReleased(e -> {
                node.setScaleX(1.0);
                node.setScaleY(1.0);
                node.setScaleZ(1.0);
                node.setMaterial(new PhongMaterial(Color.YELLOW));
                generatePath();
            });
            node.setOnMouseDragged(e -> {
                generatePath();
            });
            // Add right-click context menu for deletion
            ContextMenu contextMenu = new ContextMenu();
            MenuItem deleteItem = new MenuItem("Delete Waypoint");
            deleteItem.setOnAction(ev -> {
                waypointGroup.getChildren().remove(node);
                int idx = waypointNodes.indexOf(node);
                if (idx >= 0) {
                    waypointNodes.remove(idx);
                    waypoints.remove(idx);
                }
                generatePath();
                updateWaypointListUI();
                log("Deleted waypoint at (" + x + ", " + y + ", " + z + ")");
            });
            contextMenu.getItems().add(deleteItem);
            node.setOnContextMenuRequested(ev -> {
                contextMenu.show(node, ev.getScreenX(), ev.getScreenY());
            });
            waypointGroup.getChildren().add(node);
            log("Added waypoint: (" + x + ", " + y + ", " + z + ")");
            updateWaypointListUI();
        } catch (Exception e) {
            log("Invalid coordinates");
        }
    }

    private void generatePath() {
        pathGroup.getChildren().clear();
        if (waypointNodes.size() < 2) return;
        // Update waypoints from nodes
        waypoints.clear();
        for (WaypointNode node : waypointNodes) {
            // Use the node's transform for position
            Waypoint wp = node.getWaypoint();
            wp.x = node.getTransforms().get(0) instanceof javafx.scene.transform.Translate ? ((javafx.scene.transform.Translate)node.getTransforms().get(0)).getX() : wp.x;
            wp.y = node.getTransforms().get(0) instanceof javafx.scene.transform.Translate ? ((javafx.scene.transform.Translate)node.getTransforms().get(0)).getY() : wp.y;
            wp.z = node.getTransforms().get(0) instanceof javafx.scene.transform.Translate ? ((javafx.scene.transform.Translate)node.getTransforms().get(0)).getZ() : wp.z;
            waypoints.add(new Waypoint(wp.x, wp.y, wp.z));
        }
        BezierCurve curve = new BezierCurve(waypoints);
        for (Waypoint wp : curve.getInterpolatedPoints()) {
            Sphere s = new Sphere(1.2);
            s.setMaterial(new PhongMaterial(Color.CYAN));
            s.setTranslateX(wp.x);
            s.setTranslateY(wp.y);
            s.setTranslateZ(wp.z);
            pathGroup.getChildren().add(s);
        }
        log("Generated path with " + curve.getInterpolatedPoints().size() + " points");
        updateWaypointListUI();
        // Save to current path
        paths.put(currentPathName, new ArrayList<>(waypointNodes));
        if (simMode) startSimAnimation();
    }

    private void clearAll() {
        waypoints.clear();
        waypointNodes.clear();
        waypointGroup.getChildren().clear();
        pathGroup.getChildren().clear();
        selectedWaypointIndex = -1;
        updateWaypointListUI();
        log("Cleared all waypoints and paths");
        // Save to current path
        paths.put(currentPathName, new ArrayList<>(waypointNodes));
        if (simMode) startSimAnimation();
    }

    private void startSimAnimation() {
        if (simAnimation != null) simAnimation.stop();
        simAnimation = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!simMode) return;
                if (waypointNodes.size() < 2) return;
                if (simPath == null || simPath.size() != new BezierCurve(waypoints).getInterpolatedPoints().size()) {
                    simPath = new BezierCurve(waypoints).getInterpolatedPoints();
                    simIndex = 0;
                }
                if (simPath != null && simIndex < simPath.size()) {
                    Waypoint wp = simPath.get(simIndex++);
                    plane.setTranslateX(wp.x);
                    plane.setTranslateY(wp.y);
                    plane.setTranslateZ(wp.z);
                } else if (simPath != null) {
                    simIndex = 0; // Loop
                }
            }
        };
        simAnimation.start();
    }

    private void stopSimAnimation() {
        if (simAnimation != null) simAnimation.stop();
    }

    private void buildDottedCube(Group group) {
        final double size = 300;
        final int steps = 20;
        Group lines = new Group();
        for (int x = 0; x <= 1; x++) {
            for (int y = 0; y <= 1; y++) {
                for (int z = 0; z <= 1; z++) {
                    for (int dx = 0; dx <= 1; dx++) {
                        for (int dy = 0; dy <= 1; dy++) {
                            for (int dz = 0; dz <= 1; dz++) {
                                if (dx + dy + dz != 1) continue;
                                for (int i = 0; i < steps; i += 2) {
                                    double t1 = i / (double) steps;
                                    double t2 = (i + 1) / (double) steps;
                                    double x1 = size * (x + dx * t1) - size / 2;
                                    double y1 = size * (y + dy * t1) - size / 2;
                                    double z1 = size * (z + dz * t1) - size / 2;
                                    double x2 = size * (x + dx * t2) - size / 2;
                                    double y2 = size * (y + dy * t2) - size / 2;
                                    double z2 = size * (z + dz * t2) - size / 2;

                                    Cylinder line = create3DLine(x1, y1, z1, x2, y2, z2);
                                    lines.getChildren().add(line);
                                }
                            }
                        }
                    }
                }
            }
        }
        group.getChildren().add(lines);
    }

    private Cylinder create3DLine(double x1, double y1, double z1, double x2, double y2, double z2) {
        double dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);

        Cylinder cylinder = new Cylinder(0.5, length);
        cylinder.setMaterial(new PhongMaterial(Color.GRAY));
        cylinder.setTranslateX((x1 + x2) / 2);
        cylinder.setTranslateY((y1 + y2) / 2);
        cylinder.setTranslateZ((z1 + z2) / 2);

        double angleX = Math.toDegrees(Math.atan2(dy, dz));
        double angleY = Math.toDegrees(Math.atan2(dx, dz));

        cylinder.getTransforms().addAll(
                new Rotate(angleX, Rotate.X_AXIS),
                new Rotate(angleY, Rotate.Y_AXIS)
        );

        return cylinder;
    }

    private void initMouseControl(Group group) {
        subScene.setOnMousePressed(e -> {
            anchorX = e.getSceneX();
            anchorY = e.getSceneY();
            anchorAngleX = rotateX.getAngle();
            anchorAngleY = rotateY.getAngle();
        });

        subScene.setOnMouseDragged(e -> {
            rotateX.setAngle(anchorAngleX - (anchorY - e.getSceneY()) * 0.5);
            rotateY.setAngle(anchorAngleY + (anchorX - e.getSceneX()) * 0.5);
        });

        subScene.addEventHandler(ScrollEvent.SCROLL, event -> {
            double zoomFactor = 1.05;
            if (event.getDeltaY() < 0) {
                camera.setTranslateZ(camera.getTranslateZ() * zoomFactor);
            } else {
                camera.setTranslateZ(camera.getTranslateZ() / zoomFactor);
            }
        });
    }

    private void log(String message) {
        consoleOutput.appendText(message + "\n");
        System.out.println(message);
    }

    private void switchMode(boolean sim) {
        simMode = sim;
        if (simMode) {
            startSimAnimation();
        } else {
            stopSimAnimation();
        }
        log(sim ? "Switched to Sim mode" : "Switched to Real mode");
        // In Sim mode, allow editing and previewing
        // In Real mode, lock editing and enable send-to-drone
    }

    private void addNewPath() {
        pathCounter++;
        String name = "Path " + pathCounter;
        paths.put(name, new ArrayList<>());
        currentPathName = name;
        waypointNodes.clear();
        waypoints.clear();
        waypointGroup.getChildren().clear();
        pathGroup.getChildren().clear();
        updatePathListUI();
        updateWaypointListUI();
        log("Created new path: " + name);
    }

    private void updatePathListUI() {
        pathListBox.getChildren().clear();
        for (String name : paths.keySet()) {
            HBox row = new HBox(6);
            Label nameLabel = new Label(name);
            nameLabel.setStyle("-fx-text-fill: #e0e0e0; -fx-font-size: 13px;" + (name.equals(currentPathName) ? "-fx-font-weight: bold;" : ""));
            Button selectBtn = new Button("Select");
            selectBtn.setDisable(name.equals(currentPathName));
            selectBtn.setOnAction(e -> selectPath(name));
            Button renameBtn = new Button("✎");
            renameBtn.setTooltip(new Tooltip("Rename path"));
            renameBtn.setOnAction(e -> renamePath(name));
            Button deleteBtn = new Button("🗑");
            deleteBtn.setTooltip(new Tooltip("Delete path"));
            deleteBtn.setDisable(paths.size() <= 1);
            deleteBtn.setOnAction(e -> deletePath(name));
            row.getChildren().addAll(nameLabel, selectBtn, renameBtn, deleteBtn);
            pathListBox.getChildren().add(row);
        }
    }

    private void selectPath(String name) {
        // Save current waypoints to current path
        paths.put(currentPathName, new ArrayList<>(waypointNodes));
        // Switch to new path
        currentPathName = name;
        waypointNodes.clear();
        waypoints.clear();
        waypointGroup.getChildren().clear();
        pathGroup.getChildren().clear();
        for (WaypointNode node : paths.get(name)) {
            waypointNodes.add(new WaypointNode(new Waypoint(node.getWaypoint().x, node.getWaypoint().y, node.getWaypoint().z)));
        }
        for (WaypointNode node : waypointNodes) {
            waypoints.add(node.getWaypoint());
            waypointGroup.getChildren().add(node);
        }
        generatePath();
        updatePathListUI();
        updateWaypointListUI();
        log("Selected path: " + name);
        if (simMode) startSimAnimation();
    }

    private void renamePath(String name) {
        TextInputDialog dialog = new TextInputDialog(name);
        dialog.setTitle("Rename Path");
        dialog.setHeaderText("Rename Path");
        dialog.setContentText("New name:");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(newName -> {
            if (!newName.trim().isEmpty() && !paths.containsKey(newName)) {
                List<WaypointNode> nodes = paths.remove(name);
                paths.put(newName, nodes);
                if (currentPathName.equals(name)) currentPathName = newName;
                updatePathListUI();
                log("Renamed path to: " + newName);
            }
        });
    }

    private void deletePath(String name) {
        if (paths.size() <= 1) return;
        paths.remove(name);
        if (currentPathName.equals(name)) {
            currentPathName = paths.keySet().iterator().next();
            selectPath(currentPathName);
        }
        updatePathListUI();
        log("Deleted path: " + name);
    }

    private void addDefaultPath() {
        double[][] pts = { { -100, 0, -100 }, { 0, 50, 0 }, { 100, 0, 100 } };
        for (double[] pt : pts) {
            Waypoint wp = new Waypoint(pt[0], pt[1], pt[2]);
            WaypointNode node = new WaypointNode(wp);
            waypointNodes.add(node);
            waypoints.add(wp);
            waypointGroup.getChildren().add(node);
        }
        generatePath();
    }

    private void exportCurrentPathToCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Path as CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        java.io.File file = fileChooser.showSaveDialog(null);
        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                writer.write("x,y,z\n");
                for (WaypointNode node : waypointNodes) {
                    Waypoint wp = node.getWaypoint();
                    writer.write(wp.x + "," + wp.y + "," + wp.z + "\n");
                }
                log("Exported path to: " + file.getAbsolutePath());
            } catch (IOException e) {
                log("Failed to export: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        launch();
    }
}
