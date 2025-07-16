package main.comm;

import com.fazecast.jSerialComm.SerialPort;
import main.path.Waypoint;

import java.io.OutputStream;
import java.util.List;

public class SerialTransmitter {
    private SerialPort port;

    public SerialTransmitter(String portName) {
        port = SerialPort.getCommPort(portName);
        port.setBaudRate(9600);
        port.openPort();
    }

    public void sendWaypoints(List<Waypoint> points) {
        try {
            OutputStream out = port.getOutputStream();
            for (Waypoint wp : points) {
                String msg = wp.x + "," + wp.y + "," + wp.z + "\n";
                out.write(msg.getBytes());
                out.flush();
                Thread.sleep(50); // prevent Arduino buffer overflow
            }
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void close() {
        port.closePort();
    }
}
