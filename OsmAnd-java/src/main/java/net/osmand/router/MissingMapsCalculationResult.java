package net.osmand.router;

import net.osmand.data.LatLon;
import net.osmand.map.WorldRegion;
import net.osmand.util.Algorithms;

import java.util.List;

public class MissingMapsCalculationResult {

	private final RoutingContext missingMapsRoutingContext;
	private final List<LatLon> missingMapsPoints;

	public boolean requestMapsToUpdate;
	public List<WorldRegion> mapsToUpdate;
	public List<WorldRegion> missingMaps;
	public List<WorldRegion> potentiallyUsedMaps;

	public MissingMapsCalculationResult(RoutingContext missingMapsRoutingContext, List<LatLon> missingMapsPoints) {
		this.missingMapsRoutingContext = missingMapsRoutingContext;
		this.missingMapsPoints = missingMapsPoints;
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

	public RoutingContext getMissingMapsRoutingContext() {
		return missingMapsRoutingContext;
	}

	public List<LatLon> getMissingMapsPoints() {
		return missingMapsPoints;
	}
}
