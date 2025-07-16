# Plane Drone Path Planner (Java + Arduino)

This project lets you design 3D flight paths using a JavaFX visualizer, then sends those paths to an Arduino-powered plane-shaped drone using serial communication. The drone interprets these paths as waypoints to follow using GPS and onboard control.

## 🛠 Features
- 3D JavaFX-based waypoint visualizer
- Cubic Bezier curve path interpolation
- Serial communication to Arduino (jSerialComm)
- Arduino C++ code to follow waypoints with GPS and IMU
- Modular Java structure for GUI, path, and comms

## 🧱 Requirements

### PC (Java side)
- Java 17+
- IntelliJ IDEA
- JavaFX SDK
- jSerialComm (add to `lib/`)

### Arduino
- Arduino Uno/Nano/Mega
- Neo-6M GPS
- MPU6050
- Motor + ESC + control surfaces (servo)

## 🧭 How it works

1. User selects points in 3D space.
2. Java app builds a smooth Bezier path.
3. Path is sent over serial to Arduino.
4. Arduino reads GPS, finds heading/distance.
5. Drone follows path point-by-point.

## 📂 Structure

src/
├── Main.java
├── gui/Visualizer3D.java
├── path/BezierCurve.java, Waypoint.java
├── comm/SerialTransmitter.java

