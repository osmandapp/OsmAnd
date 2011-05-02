package net.osmand.data.preparation;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import net.osmand.osm.Node;
import net.osmand.osm.Way;

public class MapSwingAlgorithms {

	public static Point2D.Float getIntersectionPoint(Line2D.Float line1, Line2D.Float line2) {
		if (!line1.intersectsLine(line2))
			return null;
		double px = line1.getX1(), py = line1.getY1(), rx = line1.getX2() - px, ry = line1.getY2() - py;
		double qx = line2.getX1(), qy = line2.getY1(), sx = line2.getX2() - qx, sy = line2.getY2() - qy;

		double det = sx * ry - sy * rx;
		if (det == 0) {
			return null;
		} else {
			double z = (sx * (qy - py) + sy * (px - qx)) / det;
			if (z <= 0 || z >= 1)
				return null; // intersection at end point!
			return new Point2D.Float((float) (px + z * rx), (float) (py + z * ry));
		}
	} // end intersection line-line

	public static boolean isClockwiseWay(List<Way> ways) {
		if (ways.isEmpty()) {
			return false;
		}
		List<Node> nodes;
		if (ways.size() == 1) {
			nodes = ways.get(0).getNodes();
		} else {
			nodes = new ArrayList<Node>();
			boolean first = true;
			for (Way e : ways) {
				if (first) {
					first = false;
					nodes.addAll(e.getNodes());
				} else {
					nodes.addAll(e.getNodes().subList(1, e.getNodes().size()));
				}
			}
		}
		if (nodes.isEmpty()) {
			return false;
		}
		double angle = 0;
		double prevAng = 0;
		double firstAng = 0;
		double selfIntersection = 0;
		boolean open = nodes.get(nodes.size() - 1).getId() != nodes.get(0).getId();

		for (int i = 1; open ? i < nodes.size() : i <= nodes.size(); i++) {// nodes.get(i).getId()
			double ang;
			if (i < nodes.size()) {
				ang = Math.atan2(nodes.get(i).getLatitude() - nodes.get(i - 1).getLatitude(), nodes.get(i).getLongitude()
						- nodes.get(i - 1).getLongitude());
				// find self intersection
				Line2D.Float l = new Line2D.Float((float) nodes.get(i).getLongitude(), (float) nodes.get(i).getLatitude(), (float) nodes
						.get(i - 1).getLongitude(), (float) nodes.get(i - 1).getLatitude());
				for (int j = i - 2; j > i - 7; j--) {
					if (j < 1) {
						break;
					}
					Line2D.Float l2 = new Line2D.Float((float) nodes.get(j).getLongitude(), (float) nodes.get(j).getLatitude(),
							(float) nodes.get(j - 1).getLongitude(), (float) nodes.get(j - 1).getLatitude());
					java.awt.geom.Point2D.Float point = getIntersectionPoint(l, l2);
					if (point != null) {
						double dang = Math.atan2(nodes.get(j).getLatitude() - nodes.get(j - 1).getLatitude(), nodes.get(j).getLongitude()
								- nodes.get(j - 1).getLongitude());
						if (adjustDirection(ang - dang) < 0) {
							selfIntersection += 2 * Math.PI;
						} else {
							selfIntersection -= 2 * Math.PI;
						}
					}

				}
			} else {
				ang = firstAng;
			}
			if (i > 1) {
				angle += adjustDirection(ang - prevAng);
				prevAng = ang;
			} else {
				prevAng = ang;
				firstAng = ang;
			}

		}
		return (angle - selfIntersection) < 0;
	}

	private static double adjustDirection(double ang) {
		if (ang < -Math.PI) {
			ang += 2 * Math.PI;
		} else if (ang > Math.PI) {
			ang -= 2 * Math.PI;
		}
		return ang;
	}

}
