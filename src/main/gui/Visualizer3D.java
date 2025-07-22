
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

import java.util.*;
import java.nio.file.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.Gson;
import java.lang.reflect.Type;

import javafx.stage.FileChooser;
import java.io.FileWriter;
import java.io.IOException;
import javafx.scene.PointLight;
import javafx.scene.AmbientLight;
import javafx.scene.PerspectiveCamera;
import javafx.geometry.Point3D;
import javafx.scene.control.Slider;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import java.io.FileReader;
import java.io.BufferedReader;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.StackPane;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;

public class Visualizer3D extends Application {
    private static final String PATHS_FILE = System.getProperty("user.home") + "/.pathplanner_paths.json";
    private final Group sceneRoot = new Group();
    private final Group waypointGroup = new Group();
    private final Group pathGroup = new Group();
    private final PlaneModel plane = new PlaneModel();
    private final List<Waypoint> waypoints = new ArrayList<>();
    // Remove cameraPivot
    // Camera orbit state
    private double mouseOldX, mouseOldY;
    private double cameraYaw = 0; // azimuth
    private double cameraPitch = 0; // elevation
    private double cameraDistance = 800;

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
    private List<Waypoint> currentSimPath = new ArrayList<>();
    private boolean isPlaying = true;
    private Slider animationSlider;
    private int animationStep = 0;
    private int animationSteps = 1;
    private double secondsPerStep = 0.02; // 20ms per step (50 FPS)
    private Label timeLabel;

    private enum AppState { PROJECT_BROWSER, PATH_EDITOR }
    private AppState appState = AppState.PROJECT_BROWSER;
    private BorderPane layout;
    private TilePane projectGrid;
    private StackPane rootPane;
    private Button backBtn;

    private ScrollPane sidebar;
    private HBox hotbar;

    @Override
    public void start(Stage stage) {
        // Add all 3D objects to sceneRoot
        sceneRoot.getChildren().addAll(waypointGroup, pathGroup, plane);
        buildDottedCube(sceneRoot);

        // Add lighting for 3D visibility
        PointLight light = new PointLight(Color.WHITE);
        light.setTranslateX(0);
        light.setTranslateY(-400);
        light.setTranslateZ(-400);
        sceneRoot.getChildren().add(light);

        AmbientLight ambient = new AmbientLight(Color.color(0.5, 0.5, 0.5));
        sceneRoot.getChildren().add(ambient);

        Group world = new Group(sceneRoot);

        // Set up camera
        PerspectiveCamera camera = new PerspectiveCamera(true); // fixedEyeAtCameraZero is set via constructor
        camera.setNearClip(0.1);
        camera.setFarClip(2000.0);
        camera.setFieldOfView(45);
        updateCameraPosition(camera);

        subScene = new SubScene(world, 900, 600, true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.DARKSLATEBLUE); // Debug background
        subScene.setCamera(camera);
        subScene.setHeight(900);
        subScene.setWidth(600);

        log("Camera position: (" + camera.getTranslateX() + ", " + camera.getTranslateY() + ", " + camera.getTranslateZ() + ")");
        log("SubScene size: W=" + subScene.getWidth() + ", H=" + subScene.getHeight());

        initArcballCameraControls(camera);

        // Sidebar (drawer) setup
        VBox sidebarContent = new VBox(18);
        sidebarContent.setPadding(new Insets(18));
        sidebarContent.setStyle("-fx-background-color: #23272e; -fx-min-width: 280px; -fx-max-width: 320px;");

        // Title and status
        HBox titleBar = new HBox(8);
        ImageView logo = new ImageView(new Image(getClass().getResource("pathplanner_icon.png").toExternalForm(), 32, 32, true, true)); // Placeholder icon
        Label title = new Label("PathPlanner 3D");
        title.setStyle("-fx-font-size: 20px; -fx-text-fill: #f8f8f2; -fx-font-weight: bold;");
        Label status = new Label("● Connected");
        status.setStyle("-fx-text-fill: #4caf50; -fx-font-size: 14px;");
        titleBar.getChildren().addAll(logo, title, status);

        // Controls section (styled, grouped, tooltips)
        VBox controlsBox = new VBox(12);
        controlsBox.setStyle("-fx-background-color: #23272e; -fx-padding: 14; -fx-border-radius: 8; -fx-border-color: #444; -fx-border-width: 1; -fx-spacing: 10;");
        Button generateBtn = new Button("Generate Path");
        generateBtn.setOnAction(e -> generatePath());
        generateBtn.setTooltip(new Tooltip("Generate a new path from the current waypoints"));
        Button clearBtn = new Button("Clear All");
        clearBtn.setOnAction(e -> clearAll());
        clearBtn.setTooltip(new Tooltip("Remove all waypoints and paths"));
        Button sendBtn = new Button("Send to Arduino");
        sendBtn.setOnAction(e -> log("[Stub] Send to Arduino clicked")); // Implement as needed
        sendBtn.setTooltip(new Tooltip("Send the current path to the Arduino"));
        generateBtn.setStyle("-fx-font-size: 14px; -fx-padding: 6 18 6 18; -fx-background-radius: 6; -fx-background-color: #444a54; -fx-text-fill: #fff;");
        clearBtn.setStyle("-fx-font-size: 14px; -fx-padding: 6 18 6 18; -fx-background-radius: 6; -fx-background-color: #444a54; -fx-text-fill: #fff;");
        sendBtn.setStyle("-fx-font-size: 14px; -fx-padding: 6 18 6 18; -fx-background-radius: 6; -fx-background-color: #444a54; -fx-text-fill: #fff;");
        controlsBox.getChildren().addAll(generateBtn, clearBtn, sendBtn);

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
        Button clearBt = new Button("Clear All");
        clearBt.setTooltip(new Tooltip("Clear all waypoints and paths"));
        clearBt.setOnAction(e -> clearAll());
        pathButtons.getChildren().addAll(pathBtn, clearBt);

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
        sidebarContent.getChildren().addAll(titleBar, controlsBox, new Label("Waypoint (X Y Z):"), waypointInput, pathButtons, waypointListLabel, waypointListBox, new Label("Console Output:"), consoleOutput, telemetryPane, modeToggleBox, pathManagerBox);
        VBox.setVgrow(consoleOutput, Priority.ALWAYS);

        // Make sidebar scrollable
        sidebar = new ScrollPane(sidebarContent);
        sidebar.setFitToWidth(true);
        sidebar.setStyle("-fx-background: #23272e; -fx-border-color: #444; -fx-border-width: 0 1 0 0;");
        sidebar.setPrefWidth(320);
        sidebar.setMinWidth(280);
        sidebar.setMaxWidth(340);

        // Add a default path for visibility
        if (waypointNodes.isEmpty()) {
            addDefaultPath();
        }

        // Add hotbar at the bottom
        hotbar = new HBox(14);
        hotbar.setStyle("-fx-background-color: #23272e; -fx-padding: 8 16 8 16; -fx-alignment: center; -fx-border-color: #444; -fx-border-width: 1 0 0 0;");
        ToggleButton playPauseBtn = new ToggleButton();
        playPauseBtn.setSelected(false);
        playPauseBtn.setTooltip(new Tooltip("Play/Pause animation"));
        playPauseBtn.setGraphic(new ImageView(new Image(getClass().getResource("play_pause_icon.png").toExternalForm(), 24, 24, true, true)));
        playPauseBtn.setOnAction(e -> {
            isPlaying = !playPauseBtn.isSelected();
            playPauseBtn.setText(isPlaying ? "Pause" : "Play");
        });
        Button replayBtn = new Button();
        replayBtn.setTooltip(new Tooltip("Replay animation"));
        replayBtn.setGraphic(new ImageView(new Image(getClass().getResource("replay_icon.png").toExternalForm(), 22, 22, true, true)));
        replayBtn.setOnAction(e -> {
            simIndex = 0;
            if (animationSlider != null) animationSlider.setValue(0);
            updatePlanePosition();
            updateTimeLabel();
        });
        animationSlider = new Slider(0, 1, 0);
        animationSlider.setPrefWidth(400);
        animationSlider.setStyle("-fx-padding: 0 8 0 8;");
        timeLabel = new Label("Time: 0.00s / 0.00s");
        timeLabel.setStyle("-fx-text-fill: #e0e0e0; -fx-font-size: 15px; -fx-padding: 0 0 0 12px;");
        animationSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (!isPlaying && currentSimPath != null && !currentSimPath.isEmpty()) {
                animationStep = (int) (newVal.doubleValue() * (currentSimPath.size() - 1));
                updatePlanePosition();
                updateTimeLabel();
            }
        });
        hotbar.getChildren().addAll(playPauseBtn, replayBtn, animationSlider, timeLabel);

        // Layout
        rootPane = new StackPane();
        layout = new BorderPane();
        rootPane.getChildren().add(layout);
        Scene scene = new Scene(rootPane, 1300, 700);
        scene.getStylesheets().add(getClass().getResource("/main/gui/pathplanner_dark.css").toExternalForm()); // Placeholder for custom CSS
        stage.setScene(scene);
        stage.setTitle("PathPlanner 3D");
        stage.show();
        stage.getIcons().add(new Image(getClass().getResourceAsStream("pathplanner_icon.png")));

        showProjectBrowser();
        log("Application started. Ready to add waypoints.");

        loadPathsFromDisk();
        showProjectBrowser();
        log("Application started. Ready to add waypoints.");
    }

    private void savePathsToDisk() {
        try {
            Gson gson = new Gson();
            Map<String, List<Waypoint>> serializable = new HashMap<>();
            for (Map.Entry<String, List<WaypointNode>> entry : paths.entrySet()) {
                List<Waypoint> wps = new ArrayList<>();
                for (WaypointNode node : entry.getValue()) {
                    wps.add(new Waypoint(node.getWaypoint().x, node.getWaypoint().y, node.getWaypoint().z));
                }
                serializable.put(entry.getKey(), wps);
            }
            Files.write(Paths.get(PATHS_FILE), gson.toJson(serializable).getBytes());
            log("Paths autosaved to disk.");
        } catch (Exception e) {
            log("Failed to save paths: " + e.getMessage());
        }
    }

    private void loadPathsFromDisk() {
        try {
            Gson gson = new Gson();
            if (!Files.exists(Paths.get(PATHS_FILE))) return;
            String json = new String(Files.readAllBytes(Paths.get(PATHS_FILE)));
            Type type = new TypeToken<Map<String, List<Waypoint>>>(){}.getType();
            Map<String, List<Waypoint>> loaded = gson.fromJson(json, type);
            paths.clear();
            for (Map.Entry<String, List<Waypoint>> entry : loaded.entrySet()) {
                List<WaypointNode> nodes = new ArrayList<>();
                for (Waypoint wp : entry.getValue()) {
                    nodes.add(new WaypointNode(new Waypoint(wp.x, wp.y, wp.z)));
                }
                paths.put(entry.getKey(), nodes);
            }
            log("Paths loaded from disk.");
        } catch (Exception e) {
            log("Failed to load paths: " + e.getMessage());
        }
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
        generatePath(); // Always update path after reordering
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
            generatePath(); // Always update path after adding
        } catch (Exception e) {
            log("Invalid coordinates");
        }
    }

    private void generatePath() {
        if (animationSlider == null) return; // Prevent NPE if called too early
        pathGroup.getChildren().clear();
        // Always rebuild waypoints from all nodes, regardless of count
        waypoints.clear();
        for (WaypointNode node : waypointNodes) {
            Waypoint wp = node.getWaypoint();
            wp.x = node.getTransforms().get(0) instanceof javafx.scene.transform.Translate ? ((javafx.scene.transform.Translate)node.getTransforms().get(0)).getX() : wp.x;
            wp.y = node.getTransforms().get(0) instanceof javafx.scene.transform.Translate ? ((javafx.scene.transform.Translate)node.getTransforms().get(0)).getY() : wp.y;
            wp.z = node.getTransforms().get(0) instanceof javafx.scene.transform.Translate ? ((javafx.scene.transform.Translate)node.getTransforms().get(0)).getZ() : wp.z;
            waypoints.add(new Waypoint(wp.x, wp.y, wp.z));
        }
        System.out.println("[DEBUG] Waypoints: " + waypoints.size());
        for (int i = 0; i < waypoints.size(); i++) {
            Waypoint wp = waypoints.get(i);
            System.out.println("[DEBUG] Waypoint " + i + ": (" + wp.x + ", " + wp.y + ", " + wp.z + ")");
        }
        if (waypoints.size() < 2) return;
        // Draw spheres for each waypoint
        for (Waypoint wp : waypoints) {
            Sphere s = new Sphere(2.5);
            s.setMaterial(new PhongMaterial(Color.YELLOW));
            s.setTranslateX(wp.x);
            s.setTranslateY(wp.y);
            s.setTranslateZ(wp.z);
            pathGroup.getChildren().add(s);
        }
        // Catmull-Rom spline for smooth path
        List<Waypoint> splinePoints = interpolateCatmullRom(waypoints, 40);
        currentSimPath = splinePoints;
        animationSteps = currentSimPath.size();
        animationSlider.setMax(animationSteps - 1);
        // Draw cylinders connecting each consecutive pair of spline points
        for (int i = 0; i < splinePoints.size() - 1; i++) {
            Waypoint a = splinePoints.get(i);
            Waypoint b = splinePoints.get(i + 1);
            Cylinder line = create3DLine(a.x, a.y, a.z, b.x, b.y, b.z, 1.2, Color.ORANGE);
            pathGroup.getChildren().add(line);
        }
        log("Generated Catmull-Rom path with " + waypoints.size() + " waypoints and " + animationSteps + " spline points");
        updateWaypointListUI();
        paths.put(currentPathName, new ArrayList<>(waypointNodes));
        if (simMode) startSimAnimation();
    }

    // Catmull-Rom spline interpolation for smooth multi-waypoint paths
    private List<Waypoint> interpolateCatmullRom(List<Waypoint> wps, int stepsPerSegment) {
        List<Waypoint> result = new ArrayList<>();
        if (wps.size() < 2) return result;
        // For endpoints, duplicate the first and last point
        List<Waypoint> pts = new ArrayList<>();
        pts.add(wps.get(0));
        pts.addAll(wps);
        pts.add(wps.get(wps.size() - 1));
        for (int i = 0; i < pts.size() - 3; i++) {
            Waypoint p0 = pts.get(i);
            Waypoint p1 = pts.get(i + 1);
            Waypoint p2 = pts.get(i + 2);
            Waypoint p3 = pts.get(i + 3);
            for (int s = 0; s < stepsPerSegment; s++) {
                double t = s / (double) stepsPerSegment;
                double t2 = t * t;
                double t3 = t2 * t;
                double x = 0.5 * ((2 * p1.x) + (-p0.x + p2.x) * t + (2*p0.x - 5*p1.x + 4*p2.x - p3.x) * t2 + (-p0.x + 3*p1.x - 3*p2.x + p3.x) * t3);
                double y = 0.5 * ((2 * p1.y) + (-p0.y + p2.y) * t + (2*p0.y - 5*p1.y + 4*p2.y - p3.y) * t2 + (-p0.y + 3*p1.y - 3*p2.y + p3.y) * t3);
                double z = 0.5 * ((2 * p1.z) + (-p0.z + p2.z) * t + (2*p0.z - 5*p1.z + 4*p2.z - p3.z) * t2 + (-p0.z + 3*p1.z - 3*p2.z + p3.z) * t3);
                result.add(new Waypoint(x, y, z));
            }
        }
        result.add(wps.get(wps.size() - 1));
        System.out.println("[DEBUG] Catmull-Rom points: " + result.size());
        return result;
    }

    // Helper to create a 3D line (cylinder) between two points
    private Cylinder create3DLine(double x1, double y1, double z1, double x2, double y2, double z2, double radius, Color color) {
        double dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        Cylinder cylinder = new Cylinder(radius, length);
        cylinder.setMaterial(new PhongMaterial(color));
        cylinder.setTranslateX((x1 + x2) / 2);
        cylinder.setTranslateY((y1 + y2) / 2);
        cylinder.setTranslateZ((z1 + z2) / 2);
        // Calculate rotation
        double phi = Math.atan2(dy, Math.sqrt(dx * dx + dz * dz));
        double theta = Math.atan2(dx, dz);
        cylinder.getTransforms().addAll(
            new Rotate(-Math.toDegrees(theta), Rotate.Y_AXIS),
            new Rotate(Math.toDegrees(phi), Rotate.X_AXIS)
        );
        return cylinder;
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
        simIndex = 0;
        isPlaying = true;
        if (animationSlider != null) animationSlider.setValue(0);
        updateTimeLabel();
        simAnimation = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!simMode || currentSimPath == null || currentSimPath.size() < 2) return;
                if (isPlaying) {
                    if (simIndex >= currentSimPath.size()) simIndex = 0; // Loop
                    animationSlider.setValue(simIndex);
                    updatePlanePosition();
                    updateTimeLabel();
                    simIndex++;
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

    private void initArcballCameraControls(PerspectiveCamera camera) {
        subScene.setOnMousePressed(e -> {
            mouseOldX = e.getSceneX();
            mouseOldY = e.getSceneY();
        });
        subScene.setOnMouseDragged(e -> {
            double dx = e.getSceneX() - mouseOldX;
            // Only allow yaw (horizontal orbit), no pitch
            cameraYaw += dx * 0.5;
            updateCameraPosition(camera);
            mouseOldX = e.getSceneX();
            mouseOldY = e.getSceneY();
        });
        subScene.setOnScroll(event -> {
            double zoomFactor = 1.05;
            if (event.getDeltaY() < 0) {
                cameraDistance *= zoomFactor;
            } else {
                cameraDistance /= zoomFactor;
            }
            cameraDistance = Math.max(100, Math.min(2000, cameraDistance));
            updateCameraPosition(camera);
        });
        updateCameraPosition(camera);
    }

    private void updateCameraPosition(PerspectiveCamera camera) {
        // Spherical to Cartesian for camera position
        double yawRad = Math.toRadians(cameraYaw);
        double pitchRad = Math.toRadians(cameraPitch);
        double x = cameraDistance * Math.cos(pitchRad) * Math.sin(yawRad);
        double y = cameraDistance * Math.sin(pitchRad);
        double z = cameraDistance * Math.cos(pitchRad) * Math.cos(yawRad);
        camera.setTranslateX(x);
        camera.setTranslateY(-y); // JavaFX Y is down
        camera.setTranslateZ(z);

        // Calculate direction vector from camera to origin
        double dx = -x;
        double dy = y; // JavaFX Y is down
        double dz = -z;
        double r = Math.sqrt(dx*dx + dy*dy + dz*dz);

        // Calculate yaw and pitch to look at the origin
        double lookYaw = Math.toDegrees(Math.atan2(dx, dz));
        double lookPitch = Math.toDegrees(Math.asin(dy / r));

        camera.getTransforms().setAll(
            new Rotate(lookYaw, Rotate.Y_AXIS),
            new Rotate(lookPitch, Rotate.X_AXIS)
        );
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

    private void importPathFromCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Path from CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        java.io.File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                List<WaypointNode> importedNodes = new ArrayList<>();
                boolean first = true;
                while ((line = reader.readLine()) != null) {
                    if (first && line.toLowerCase().contains("x") && line.toLowerCase().contains("y") && line.toLowerCase().contains("z")) {
                        first = false; // skip header
                        continue;
                    }
                    String[] parts = line.trim().split(",");
                    if (parts.length < 3) continue;
                    try {
                        double x = Double.parseDouble(parts[0].trim());
                        double y = Double.parseDouble(parts[1].trim());
                        double z = Double.parseDouble(parts[2].trim());
                        Waypoint wp = new Waypoint(x, y, z);
                        WaypointNode node = new WaypointNode(wp);
                        importedNodes.add(node);
                    } catch (NumberFormatException ex) {
                        // skip invalid lines
                    }
                }
                if (!importedNodes.isEmpty()) {
                    waypointNodes.clear();
                    waypoints.clear();
                    waypointGroup.getChildren().clear();
                    for (WaypointNode node : importedNodes) {
                        waypointNodes.add(node);
                        waypoints.add(node.getWaypoint());
                        waypointGroup.getChildren().add(node);
                    }
                    generatePath();
                    log("Imported " + importedNodes.size() + " waypoints from: " + file.getName());
                } else {
                    log("No valid waypoints found in: " + file.getName());
                }
            } catch (Exception ex) {
                log("Failed to import: " + ex.getMessage());
            }
        }
    }

    private void updatePlanePosition() {
        if (currentSimPath == null || currentSimPath.isEmpty()) return;
        int idx = Math.max(0, Math.min(simIndex, currentSimPath.size() - 1));
        Waypoint wp = currentSimPath.get(idx);
        plane.setTranslateX(wp.x);
        plane.setTranslateY(wp.y);
        plane.setTranslateZ(wp.z);
    }

    private void updateTimeLabel() {
        double currentTime = simIndex * secondsPerStep;
        double totalTime = (animationSteps - 1) * secondsPerStep;
        if (timeLabel != null) {
            timeLabel.setText(String.format("Time: %.2fs / %.2fs", currentTime, totalTime));
        }
    }

    private void showProjectBrowser() {
        appState = AppState.PROJECT_BROWSER;
        projectGrid = new TilePane();
        projectGrid.setHgap(24);
        projectGrid.setVgap(24);
        projectGrid.setPrefColumns(3);
        projectGrid.setStyle("-fx-padding: 40; -fx-background-color: #181a20;");
        // Add cards for each path
        for (String name : paths.keySet()) {
            StackPane card = createPathCard(name);
            projectGrid.getChildren().add(card);
        }
        // Add 'New Path' card
        StackPane newCard = new StackPane();
        newCard.setStyle("-fx-background-color: #23272e; -fx-border-color: #4caf50; -fx-border-width: 2; -fx-border-radius: 12; -fx-background-radius: 12; -fx-pref-width: 220; -fx-pref-height: 160; -fx-alignment: center;");
        Label plus = new Label("+");
        plus.setStyle("-fx-font-size: 48px; -fx-text-fill: #4caf50;");
        newCard.getChildren().add(plus);
        newCard.setOnMouseClicked(e -> {
            String newName = "Path " + (paths.size() + 1);
            paths.put(newName, new ArrayList<>());
            showProjectBrowser();
        });
        projectGrid.getChildren().add(newCard);
        layout.setCenter(projectGrid);
        layout.setLeft(sidebar);
        layout.setBottom(null);
        layout.setTop(null);
    }

    private StackPane createPathCard(String name) {
        StackPane card = new StackPane();
        card.setStyle("-fx-background-color: #23272e; -fx-border-color: #444; -fx-border-width: 2; -fx-border-radius: 12; -fx-background-radius: 12; -fx-pref-width: 220; -fx-pref-height: 160; -fx-alignment: center;");
        VBox vbox = new VBox(8);
        vbox.setStyle("-fx-alignment: center;");
        Label title = new Label(name);
        title.setStyle("-fx-font-size: 20px; -fx-text-fill: #fff; -fx-font-weight: bold;");
        // Add a simple 2D preview of the path
        Canvas preview = new Canvas(180, 60);
        drawPathPreview(preview, paths.get(name));
        vbox.getChildren().addAll(title, preview);
        card.getChildren().add(vbox);
        card.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                selectPath(name);
                showPathEditor();
            }
        });
        // Context menu for rename/delete
        ContextMenu menu = new ContextMenu();
        MenuItem rename = new MenuItem("Rename");
        rename.setOnAction(ev -> renamePath(name));
        MenuItem delete = new MenuItem("Delete");
        delete.setOnAction(ev -> {
            deletePath(name);
            showProjectBrowser();
        });
        menu.getItems().addAll(rename, delete);
        card.setOnContextMenuRequested(ev -> menu.show(card, ev.getScreenX(), ev.getScreenY()));
        return card;
    }

    private void drawPathPreview(Canvas canvas, List<WaypointNode> nodes) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(javafx.scene.paint.Color.web("#23272e"));
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        if (nodes == null || nodes.size() < 2) return;
        // Find bounds
        double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        for (WaypointNode node : nodes) {
            double x = node.getWaypoint().x;
            double y = node.getWaypoint().y;
            if (x < minX) minX = x;
            if (x > maxX) maxX = x;
            if (y < minY) minY = y;
            if (y > maxY) maxY = y;
        }
        double pad = 10;
        double scaleX = (canvas.getWidth() - 2 * pad) / (maxX - minX + 1e-6);
        double scaleY = (canvas.getHeight() - 2 * pad) / (maxY - minY + 1e-6);
        // Draw path
        gc.setStroke(javafx.scene.paint.Color.ORANGE);
        gc.setLineWidth(2.0);
        for (int i = 0; i < nodes.size() - 1; i++) {
            double x1 = pad + (nodes.get(i).getWaypoint().x - minX) * scaleX;
            double y1 = pad + (nodes.get(i).getWaypoint().y - minY) * scaleY;
            double x2 = pad + (nodes.get(i + 1).getWaypoint().x - minX) * scaleX;
            double y2 = pad + (nodes.get(i + 1).getWaypoint().y - minY) * scaleY;
            gc.strokeLine(x1, canvas.getHeight() - y1, x2, canvas.getHeight() - y2);
        }
        // Draw waypoints
        gc.setFill(javafx.scene.paint.Color.YELLOW);
        for (WaypointNode node : nodes) {
            double x = pad + (node.getWaypoint().x - minX) * scaleX;
            double y = pad + (node.getWaypoint().y - minY) * scaleY;
            gc.fillOval(x - 3, canvas.getHeight() - y - 3, 6, 6);
        }
    }

    private void showPathEditor() {
        appState = AppState.PATH_EDITOR;
        // Add back button
        if (backBtn == null) {
            backBtn = new Button("Back to Project Browser");
            backBtn.setStyle("-fx-font-size: 14px; -fx-padding: 6 18 6 18; -fx-background-radius: 6; -fx-background-color: #444a54; -fx-text-fill: #fff; -fx-margin: 10;");
            backBtn.setOnAction(e -> showProjectBrowser());
        }
        HBox topBar = new HBox(backBtn);
        topBar.setStyle("-fx-background-color: #23272e; -fx-padding: 10 0 10 10;");
        layout.setTop(topBar);
        // Restore sidebar, hotbar, and subScene
        layout.setLeft(sidebar);
        layout.setCenter(subScene);
        layout.setBottom(null); // hotbar will be set by path editor logic
        layout.setBottom(hotbar);
    }

    public static void main(String[] args) {
        launch();
    }
}
