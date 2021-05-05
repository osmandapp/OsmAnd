package net.osmand.plus.routing;

import net.osmand.IndexConstants;
import net.osmand.Location;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.data.LatLon;
import net.osmand.map.WorldRegion;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

public class SuggestedMapsProvider {

	public static OsmandApplication ctx;
	public static TargetPointsHelper.TargetPoint start;
	public static LatLon[] intermediates;
	public static TargetPointsHelper.TargetPoint end;
	public Location currentLocation;

	public static boolean checkIfObjectDownloaded(String downloadName) {
		ResourceManager rm = ctx.getResourceManager();
		final String regionName = Algorithms.capitalizeFirstLetterAndLowercase(downloadName)
				+ IndexConstants.BINARY_MAP_INDEX_EXT;
		final String roadsRegionName = Algorithms.capitalizeFirstLetterAndLowercase(downloadName) + ".road"
				+ IndexConstants.BINARY_MAP_INDEX_EXT;
		return rm.getIndexFileNames().containsKey(regionName) || rm.getIndexFileNames().containsKey(roadsRegionName);
	}

	private static LinkedList<Location> getLocationBasedOnDistance(LinkedList<Location> points, int distance) {
		while (points.getFirst().distanceTo(points.getLast()) > distance) {
			Location mp = MapUtils.calculateMidPoint(points.getFirst(), points.getLast());
			points.add(0, mp);
		}

		Location minimalDistance = new Location("", points.getLast().getLatitude() - points.getFirst().getLatitude(), points.getLast().getLongitude() - points.getFirst().getLongitude());
		points.subList(0, points.size() - 2).clear();

		while (points.getFirst().distanceTo(points.getLast()) >= distance) {
			Location mp = new Location("", points.getFirst().getLatitude() + minimalDistance.getLatitude(), points.getFirst().getLongitude() + minimalDistance.getLongitude());
			points.add(0, mp);
		}
		return points;
	}

	public static LinkedList<Location> getGeneralLocation(RouteCalculationParams params) {
		LinkedList<Location> points = new LinkedList<>();
		points.add(new Location("", params.start.getLatitude(), params.start.getLongitude()));
		if (params.intermediates != null) {
			for (LatLon l : params.intermediates) {
				points.add(new Location("", l.getLatitude(), l.getLongitude()));
			}
		}
		points.add(new Location("", params.end.getLatitude(), params.end.getLongitude()));
		return points;
	}

	public static Set<WorldRegion> getSuggestedOfflineMap(LinkedList<Location> points) throws IOException {
		Map<WorldRegion, BinaryMapDataObject> selectedObjects = new LinkedHashMap<>();
		for (int i = 0; i < points.size(); i++) {
			final Location o = points.get(i);
			LatLon latLonPoint = new LatLon(o.getLatitude(), o.getLongitude());
			Map.Entry<WorldRegion, BinaryMapDataObject> worldRegion = ctx.getRegions().getSmallestBinaryMapDataObjectAt(latLonPoint);
			Map.Entry<WorldRegion, BinaryMapDataObject> worldParentRegion = ctx.getRegions().getSmallestBinaryMapDataObjectAt(latLonPoint);
			String worldRegionName = worldRegion.getKey().getRegionDownloadName();
			String worldParentRegionName = worldParentRegion.getKey().getRegionDownloadName();
			boolean isRegionDownload = checkIfObjectDownloaded(worldRegionName);
			boolean isParentRegionDownload = checkIfObjectDownloaded(worldParentRegionName);
			if (!isRegionDownload && !isParentRegionDownload) {
				selectedObjects.put(worldRegion.getKey(), worldRegion.getValue());
			}
		}
		return selectedObjects.keySet();
	}
}
