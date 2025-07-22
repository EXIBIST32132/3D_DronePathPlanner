import javafx.application.Application;
import javafx.scene.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.stage.Stage;

public class Test3D extends Application {
    @Override
    public void start(Stage stage) {
        Box box = new Box(100, 100, 100);
        box.setMaterial(new PhongMaterial(Color.RED));
        Group root = new Group(box);

        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setTranslateZ(-400);

        SubScene subScene = new SubScene(root, 600, 400, true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.DARKSLATEBLUE);
        subScene.setCamera(camera);

        Group mainRoot = new Group(subScene);
        Scene scene = new Scene(mainRoot, 800, 600, true);
        stage.setScene(scene);
        stage.setTitle("JavaFX 3D Test");
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
