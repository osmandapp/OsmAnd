package net.osmand.data;

import androidx.annotation.NonNull;

import net.osmand.Location;
import net.osmand.data.QuadRect;
import net.osmand.osm.edit.Node;
import net.osmand.plus.helpers.TargetPointsHelper;

import java.util.List;

/* To do: Inclusion mechanism here will not be able to work well if a set of points is crossing the -180 / +180 longitude line.
 * To do: An Interface with getLatitude and getLongitude applied to Location, Node and TargetPoint (and LatLon?) would help to avoid dupplicating methods.
 */
public class QuadRectHelper {
	public static QuadRect createRectForBoundingBoxDetermination() {
		/* With following initiale values, first position will automatically initialize boundaries of the QuadRect,
		 * thus avoiding the need to detect first insertion.
		 */
		return new QuadRect(Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE, Double.MAX_VALUE);
	}

	public static void includeLatLon(@NonNull QuadRect r, double latitude, double longitude) {
		r.left = Math.min(r.left, longitude);
		r.right = Math.max(r.right, longitude);
		r.top = Math.max(r.top, latitude);
		r.bottom = Math.min(r.bottom, latitude);
	}

	/* This function does the same thing than includeLatLon but on a set of Locations.
	 * Code is dupplicated for optimization purposes, the set of points possibly being huge.
	 */
	public static void includeLocations(@NonNull QuadRect r, @NonNull List<Location> list) {
		double latitude;
		double longitude;
		
		for (Location l : list) {
			latitude = l.getLatitude();
			longitude = l.getLongitude();

			r.left = Math.min(r.left, longitude);
			r.right = Math.max(r.right, longitude);
			r.top = Math.max(r.top, latitude);
			r.bottom = Math.min(r.bottom, latitude);
		}
	}

	/* This function does the same thing than includeLatLon but on a set of Nodes.
	 * Code is dupplicated for optimization purposes, the set of points possibly being huge.
	 */
	public static void includeNodes(@NonNull QuadRect r, @NonNull List<Node> nodes) {
		double latitude;
		double longitude;
		
		for (Node n : nodes) {
			latitude = n.getLatitude();
			longitude = n.getLongitude();

			r.left = Math.min(r.left, longitude);
			r.right = Math.max(r.right, longitude);
			r.top = Math.max(r.top, latitude);
			r.bottom = Math.min(r.bottom, latitude);
		}
	}

	/* This function does the same thing than includeLatLon but on a set of TargetPoints.
	 * Code is dupplicated for optimization purposes, the set of points possibly being huge.
	 */
	public static void includeTargetPoints(@NonNull QuadRect r, @NonNull List<TargetPointsHelper.TargetPoint> points) {
		double latitude;
		double longitude;
		
		for (TargetPointsHelper.TargetPoint p : points) {
			latitude = p.getLatitude();
			longitude = p.getLongitude();

			r.left = Math.min(r.left, longitude);
			r.right = Math.max(r.right, longitude);
			r.top = Math.max(r.top, latitude);
			r.bottom = Math.min(r.bottom, latitude);
		}
	}
	
	/* Returns true if at least one point has been included
	 * i.e. If both left to right and bottom to top are increasing or equal.
	*/
	@NonNull
	public static boolean pointsHaveBeenIncluded(@NonNull QuadRect r) {
		return (r.left <= r.right && r.bottom <= r.top) ? true : false;
	}
}
