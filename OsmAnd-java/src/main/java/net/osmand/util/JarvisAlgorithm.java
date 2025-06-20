package net.osmand.util;

import java.util.ArrayList;
import java.util.List;
import net.osmand.osm.edit.Node;

public class JarvisAlgorithm {

    public static ArrayList<Node> createConvexPolygon(List<Node> points) {
        if (points == null) {
            return null;
        }
        int n = points.size();
        if (n < 3) {
            return null;
        }

        ArrayList<Node> next=new ArrayList<>();

        int leftMost = 0;
        for (int i = 1; i < n; i++)
            if (points.get(i).getLatitude() < points.get(leftMost).getLatitude())
                leftMost = i;
        int p = leftMost, q;

        next.add(points.get(p));

        int counter = 0;
        do {
            if (counter > points.size()) {
                return null;
            }
            q = (p + 1) % n;
            for (int i = 0; i < n; i++) {
                if (orientation(points.get(p), points.get(i), points.get(q))) {
                    q = i;
                }
            }
            next.add(points.get(q));
            counter++;
            p = q;
        } while (p != leftMost);

        return next;
    }

    public static List<Node> expandPolygon(List<Node> polygon, int meters) {
        if (polygon == null || polygon.size() < 3) {
            return polygon;
        }
        double radius = (double) meters / MapUtils.METERS_IN_DEGREE;
        List<Node> expanded = new ArrayList<>();
        for (Node p : polygon) {
            List<Node> l = findPointsAroundPoint(p, radius,16);
            expanded.addAll(l);
        }
        return createConvexPolygon(expanded);
    }

    private static boolean orientation(Node p, Node q, Node r) {
        double val = (q.getLongitude() - p.getLongitude()) * (r.getLatitude() - q.getLatitude()) -
                (q.getLatitude() - p.getLatitude()) * (r.getLongitude() - q.getLongitude());

        if (val >= 0)
            return false;
        return true;
    }

    private static List<Node> findPointsAroundPoint(Node node, double radius, int numPoints) {
        List<Node> points = new ArrayList<>();
        // Calculate the angle increment for each point
        double angleIncrement = 2 * Math.PI / numPoints;

        // Generate points around the center
        for (int i = 0; i < numPoints; i++) {
            double angle = i * angleIncrement;
            double x = node.getLatitude() + radius * Math.cos(angle);
            double y = node.getLongitude() + radius * Math.sin(angle);
            x = round(x);
            y = round(y);
            points.add(new Node(x, y, node.getId()));
        }
        return points;
    }

    private static double round(double d) {
        return Math.round( d * 100000.0) / 100000.0;
    }
}

