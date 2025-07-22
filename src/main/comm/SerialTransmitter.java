package main.comm;

import com.fazecast.jSerialComm.SerialPort;
import main.path.Waypoint;

import java.io.OutputStream;
import java.util.List;
import com.google.gson.Gson;

public class SerialTransmitter {
    private SerialPort port;
    private final Gson gson = new Gson();

    public SerialTransmitter(String portName) {
        port = SerialPort.getCommPort(portName);
        port.setBaudRate(9600);
        port.openPort();
    }

    public void sendWaypoints(List<Waypoint> points) {
        try {
            OutputStream out = port.getOutputStream();
            String msg = gson.toJson(new WaypointMessage(points)) + "\n";
            out.write(msg.getBytes());
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendControlCommand(double roll, double pitch, double yaw, double throttle) {
        try {
            OutputStream out = port.getOutputStream();
            String msg = gson.toJson(new ControlCommand(roll, pitch, yaw, throttle)) + "\n";
            out.write(msg.getBytes());
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class WaypointMessage {
        String cmd = "waypoints";
        List<Waypoint> points;
        WaypointMessage(List<Waypoint> points) { this.points = points; }
    }
    private static class ControlCommand {
        String cmd = "move";
        double roll, pitch, yaw, throttle;
        ControlCommand(double roll, double pitch, double yaw, double throttle) {
            this.roll = roll; this.pitch = pitch; this.yaw = yaw; this.throttle = throttle;
        }
    }

    public void close() {
        port.closePort();
    }
}
