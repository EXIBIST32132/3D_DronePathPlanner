module main {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson;
    requires com.fazecast.jSerialComm;

    exports main;
    exports main.comm;
    exports main.gui;
    exports main.path;

    opens main.gui to javafx.graphics; // <-- This line fixes the error
}
