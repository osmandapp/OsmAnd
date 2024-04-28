package net.osmand.router;

import net.osmand.data.LatLon;
import net.osmand.map.WorldRegion;
import net.osmand.util.Algorithms;

import java.util.List;

public class MissingMapsCalculationResult {

	private final RoutingContext context;
	private final LatLon startPoint;
	private final LatLon endPoint;

	public boolean requestMapsToUpdate;
	public List<WorldRegion> mapsToUpdate;
	public List<WorldRegion> missingMaps;
	public List<WorldRegion> potentiallyUsedMaps;

	public MissingMapsCalculationResult(RoutingContext context, LatLon startPoint, LatLon endPoint) {
		this.context = context;
		this.startPoint = startPoint;
		this.endPoint = endPoint;
	}

	public boolean hasMissingMaps() {
		return !Algorithms.isEmpty(missingMaps) || !Algorithms.isEmpty(mapsToUpdate);
	}

	public List<WorldRegion> getMissingMaps() {
		return missingMaps;
	}

	public List<WorldRegion> getMapsToUpdate() {
		return mapsToUpdate;
	}

	public List<WorldRegion> getUsedMaps() {
		return potentiallyUsedMaps;
	}

	public RoutingContext getRoutingContext() {
		return context;
	}

	public LatLon getStartPoint() {
		return startPoint;
	}

	public LatLon getEndPoint() {
		return endPoint;
	}

	public String getErrorMessage() {
		String msg = "";
		if (mapsToUpdate != null) {
			msg = mapsToUpdate + " need to be updated";
		}
		if (missingMaps != null) {
			if (msg.length() > 0) {
				msg += " and ";
			}
			msg += missingMaps + " need to be downloaded";
		}
		msg = "To calculate the route maps " + msg;
		return msg;
	}
}
