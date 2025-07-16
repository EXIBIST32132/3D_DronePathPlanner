package main.path;

import java.util.ArrayList;
import java.util.List;

public class BezierCurve {
    private List<Waypoint> controlPoints;

    public BezierCurve(List<Waypoint> controlPoints) {
        this.controlPoints = controlPoints;
    }

    public List<Waypoint> getInterpolatedPoints() {
        List<Waypoint> result = new ArrayList<>();
        for (double t = 0; t <= 1; t += 0.02) {
            result.add(deCasteljau(controlPoints, t));
        }
        return result;
    }

    private Waypoint deCasteljau(List<Waypoint> points, double t) {
        if (points.size() == 1) return points.get(0);
        List<Waypoint> next = new ArrayList<>();
        for (int i = 0; i < points.size() - 1; i++) {
            double x = (1 - t) * points.get(i).x + t * points.get(i + 1).x;
            double y = (1 - t) * points.get(i).y + t * points.get(i + 1).y;
            double z = (1 - t) * points.get(i).z + t * points.get(i + 1).z;
            next.add(new Waypoint(x, y, z));
        }
        return deCasteljau(next, t);
    }
}
