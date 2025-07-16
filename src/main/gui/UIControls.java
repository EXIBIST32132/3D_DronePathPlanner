package main.gui;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

public class UIControls extends HBox {
    public Button clearBtn = new Button("Clear");
    public Button exportBtn = new Button("Export CSV");
    public Button sendBtn = new Button("Send to Arduino");

    public UIControls() {
        this.setPadding(new Insets(10));
        this.setSpacing(10);
        this.getChildren().addAll(clearBtn, exportBtn, sendBtn);
    }
}
