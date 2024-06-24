package net.osmand.router;

import net.osmand.data.LatLon;
import net.osmand.map.OsmandRegions;
import net.osmand.map.WorldRegion;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class MissingMapsCalculationResult {

	private final RoutingContext missingMapsRoutingContext;
	private final List<LatLon> missingMapsPoints;

	Set<String> usedMaps = new LinkedHashSet<>();
	Set<String> mapsToDownload = new LinkedHashSet<>();
	Set<String> missingMaps = new LinkedHashSet<>();
	Set<String> mapsToUpdate = new LinkedHashSet<>();
	private List<WorldRegion> regionsToDownload;
	private List<WorldRegion> missingRegions;
	private List<WorldRegion> regionsToUpdate;
	private List<WorldRegion> usedRegions;

	public MissingMapsCalculationResult(RoutingContext missingMapsRoutingContext, List<LatLon> missingMapsPoints) {
		this.missingMapsRoutingContext = missingMapsRoutingContext;
		this.missingMapsPoints = missingMapsPoints;
	}
	
	public void addMissingMaps(String region) {
		missingMaps.add(region);
		mapsToDownload.add(region);
	}
	
	public void addUsedMaps(String region) {
		usedMaps.add(region);		
	}
	
	public void addMapToUpdate(String region) {
		mapsToUpdate.add(region);
		mapsToDownload.add(region);		
	}

	public boolean hasMissingMaps() {
		return !Algorithms.isEmpty(mapsToDownload);
	}
	
	private List<WorldRegion> convert(OsmandRegions or, Set<String> maps) {
		if (maps.isEmpty()) {
			return null;
		}
		List<WorldRegion> l = new ArrayList<>();
		for (String m : maps) {
			WorldRegion wr = or.getRegionDataByDownloadName(m);
			if (wr != null) {
				l.add(wr);
			}
		}
		return l;
	}

	public List<WorldRegion> getMapsToDownload() {
		return regionsToDownload;
	}

	public List<WorldRegion> getMissingMaps() {
		return missingRegions;
	}

	public List<WorldRegion> getMapsToUpdate() {
		return regionsToUpdate;
	}

	public List<WorldRegion> getUsedMaps() {
		return usedRegions;
	}

	public RoutingContext getMissingMapsRoutingContext() {
		return missingMapsRoutingContext;
	}

	public List<LatLon> getMissingMapsPoints() {
		return missingMapsPoints;
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
			msg = missingMaps + " need to be downloaded";
		}
		msg = "To calculate the route maps " + msg;
		return msg;
	}

	public MissingMapsCalculationResult prepare(OsmandRegions or) {
		regionsToDownload = convert(or, mapsToDownload);
		missingRegions = convert(or, missingMaps);
		regionsToUpdate = convert(or, mapsToUpdate);
		usedRegions = convert(or, usedMaps);
		return this;
	}
}
