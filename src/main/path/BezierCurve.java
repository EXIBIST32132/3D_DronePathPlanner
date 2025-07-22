package main.path;

import java.util.ArrayList;
import java.util.List;

public class BezierCurve {
    private final List<Waypoint> controlPoints;

    public BezierCurve(List<Waypoint> controlPoints) {
        this.controlPoints = controlPoints;
    }

    public List<Waypoint> getInterpolatedPoints() {
        List<Waypoint> curve = new ArrayList<>();
        int steps = 100;
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            curve.add(deCasteljau(controlPoints, t));
        }
        return curve;
    }

    private Waypoint deCasteljau(List<Waypoint> points, double t) {
        List<Waypoint> tmp = new ArrayList<>(points);
        int n = tmp.size();
        for (int r = 1; r < n; r++) {
            for (int i = 0; i < n - r; i++) {
                double x = (1 - t) * tmp.get(i).x + t * tmp.get(i + 1).x;
                double y = (1 - t) * tmp.get(i).y + t * tmp.get(i + 1).y;
                double z = (1 - t) * tmp.get(i).z + t * tmp.get(i + 1).z;
                tmp.set(i, new Waypoint(x, y, z));
            }
        }
        return tmp.get(0);
    }
}
