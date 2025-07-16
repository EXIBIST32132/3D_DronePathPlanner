package main.comm;

import main.gui.DashboardPanel;

public class SerialReceiver extends Thread {
    private final DashboardPanel panel;

    public SerialReceiver(String portName, DashboardPanel panel) {
        this.panel = panel;
    }

    @Override
    public void run() {
        while (true) {
            try {
                // Simulated data – replace with real parsing from SerialPort
                panel.update("51.5033,-0.1195", "320m", "45°");
                Thread.sleep(1000); // Simulate incoming data rate
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
