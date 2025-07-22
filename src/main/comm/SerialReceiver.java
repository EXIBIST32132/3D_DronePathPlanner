package main.comm;

import main.gui.DashboardPanel;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import com.fazecast.jSerialComm.SerialPort;

public class SerialReceiver extends Thread {
    private final DashboardPanel panel;
    private final Gson gson = new Gson();
    private PlaneTelemetryListener telemetryListener;
    private SerialPort port;

    public SerialReceiver(String portName, DashboardPanel panel) {
        this.panel = panel;
        port = SerialPort.getCommPort(portName);
        port.setBaudRate(9600);
        port.openPort();
    }

    public void setTelemetryListener(PlaneTelemetryListener listener) {
        this.telemetryListener = listener;
    }

    @Override
    public void run() {
        try {
            InputStream in = port.getInputStream();
            byte[] buffer = new byte[1024];
            StringBuilder sb = new StringBuilder();
            while (true) {
                int len = in.read(buffer);
                if (len > 0) {
                    sb.append(new String(buffer, 0, len));
                    int idx;
                    while ((idx = sb.indexOf("\n")) >= 0) {
                        String line = sb.substring(0, idx).trim();
                        sb.delete(0, idx + 1);
                        if (!line.isEmpty()) {
                            try {
                                JsonObject obj = JsonParser.parseString(line).getAsJsonObject();
                                String gps = obj.has("lat") && obj.has("lon") ? obj.get("lat").getAsString() + "," + obj.get("lon").getAsString() : "-";
                                String alt = obj.has("alt") ? obj.get("alt").getAsString() : "-";
                                String heading = obj.has("heading") ? obj.get("heading").getAsString() : "-";
                                panel.update(gps, alt, heading);
                                if (telemetryListener != null) {
                                    telemetryListener.onTelemetry(obj);
                                }
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public interface PlaneTelemetryListener {
        void onTelemetry(JsonObject obj);
    }
}
