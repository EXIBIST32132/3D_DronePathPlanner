# 3D PathPlanner for Foam Planes/Drones

A modern JavaFX application inspired by FRC PathPlanner, designed for planning, visualizing, and running 3D paths for foam airplanes and drones.

## Features

- **Project Browser:**
  - Grid of cards for all saved paths, each with a name and 2D preview.
  - Create, rename, delete, and autosave paths visually.
- **Autosave/Load:**
  - Each path is autosaved as a JSON file in your home directory (e.g., `~/.pathplanner_<pathName>.json`).
  - Paths are loaded automatically on startup.
- **3D Path Editor/Visualizer:**
  - Catmull-Rom spline path generation through all waypoints.
  - Realistic plane animation along the path, with play/pause, replay, and time slider controls.
  - Intuitive orbit camera controls (yaw and zoom).
  - Modern, styled UI for all controls and hotbars.
- **Double-clickable Mac App:**
  - Bundle as a `.app` with a custom icon using `jpackage`.
- **Robust error handling and debug logging.**

## Usage

1. **Build the JAR:**
   ```sh
   ./mvnw clean package
   ```
2. **Run with JavaFX (for development):**
   ```sh
   java --module-path /path/to/javafx-sdk-24.0.2/lib --add-modules javafx.controls,javafx.fxml,javafx.graphics -jar target/Drone-1.0-SNAPSHOT.jar
   ```
3. **Create a Mac .app with Icon:**
   - Convert your PNGs to `.icns` and place in `src/main/resources/icons/icon.icns`.
   - Use `jpackage`:
     ```sh
     jpackage \
       --type app-image \
       --input target \
       --main-jar Drone-1.0-SNAPSHOT.jar \
       --main-class main.gui.Visualizer3D \
       --name PathPlanner3D \
       --icon src/main/resources/icons/icon.icns \
       --java-options '--add-opens=main.gui=javafx.graphics' \
       --module-path /path/to/javafx-sdk-24.0.2/lib \
       --add-modules javafx.controls,javafx.fxml,javafx.graphics
     ```
   - Double-click the resulting `.app` to run.

## Path Autosave Location
- Each path is saved as a JSON file in your home directory, e.g.:
  - `~/.pathplanner_MyPath.json`
- Paths are loaded automatically on startup.

## Requirements
- Java 17+ (tested with OpenJDK 23)
- JavaFX 24.0.2 SDK
- Maven
- (For Mac .app) Xcode command line tools for `iconutil` and `jpackage`

## Credits
- Inspired by [PathPlanner](https://github.com/mjansen4857/pathplanner)
- Uses [Gson](https://github.com/google/gson) and [jSerialComm](https://fazecast.github.io/jSerialComm/)

---

**Enjoy planning and flying your 3D paths!**

