package net.osmand.plus.routing;

import net.osmand.IndexConstants;
import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.map.WorldRegion;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.util.Algorithms;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static java.lang.Math.PI;
import static java.lang.Math.asin;
import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.toDegrees;
import static java.lang.Math.toRadians;

public class SuggestedMapsProvider {

	private static final int EARTH_RADIUS = 6371000;
	private static final int MINIMAL_DISTANCE = 20000;
	public static TargetPointsHelper.TargetPoint start;
	public static LatLon[] intermediates;
	public static TargetPointsHelper.TargetPoint end;

	public static boolean checkIfObjectDownloaded(String downloadName, OsmandApplication app) {
		ResourceManager rm = app.getResourceManager();
		final String regionName = Algorithms.capitalizeFirstLetterAndLowercase(downloadName)
				+ IndexConstants.BINARY_MAP_INDEX_EXT;
		final String roadsRegionName = Algorithms.capitalizeFirstLetterAndLowercase(downloadName) + ".road"
				+ IndexConstants.BINARY_MAP_INDEX_EXT;
		return rm.getIndexFileNames().containsKey(regionName) || rm.getIndexFileNames().containsKey(roadsRegionName);
	}

	public static LinkedList<Location> getLocationBasedOnDistance(LinkedList<Location> points) {
		while (points.getFirst().distanceTo(points.getLast()) > MINIMAL_DISTANCE) {
			float bearing = points.getFirst().bearingTo(points.getLast());
			LatLon latLon = findPointAtDistanceFrom(points.getFirst().getLatitude(), points.getFirst().getLongitude(), MINIMAL_DISTANCE, bearing);
			Location location = new Location("", latLon.getLatitude(), latLon.getLongitude());
			points.add(0, location);
		}
		return points;
	}

	public static LatLon findPointAtDistanceFrom(double latitude, double longitude, double distanceInMetres, double bearing) {
		double bearingRad = toRadians(bearing);
		double latRad = toRadians(latitude);
		double lonRad = toRadians(longitude);
		double distFraction = distanceInMetres / EARTH_RADIUS;

		double latitudeResult = asin(sin(latRad) * cos(distFraction) + cos(latRad) * sin(distFraction) * cos(bearingRad));
		double a = atan2(sin(bearingRad) * sin(distFraction) * cos(latRad), cos(distFraction) - sin(latRad) * sin(latitudeResult));
		double longitudeResult = (lonRad + a + 3 * PI) % (2 * PI) - PI;
		return new LatLon(toDegrees(latitudeResult), toDegrees(longitudeResult));
	}

	public static LinkedList<Location> getStartFinishIntermediatesPoints(RouteCalculationParams params) {
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

	public static List<WorldRegion> getSuggestedMaps(LinkedList<Location> points, OsmandApplication app) throws IOException {
		List<WorldRegion> suggestedMaps = new ArrayList<>();
		for (int i = 0; i < points.size(); i++) {
			final Location o = points.get(i);
			LatLon latLonPoint = new LatLon(o.getLatitude(), o.getLongitude());
			List<WorldRegion> downloadRegions = app.getRegions().getWorldRegionsAt(latLonPoint);

			Collections.sort(downloadRegions, new Comparator<WorldRegion>() {
				@Override
				public int compare(WorldRegion o1, WorldRegion o2) {
					return Boolean.compare(o2.containsRegion(o1), o1.containsRegion(o2));
				}
			});

			for (WorldRegion downloadRegion : downloadRegions) {
				String mapName = downloadRegion.getRegionDownloadName();
				String parentMapName = downloadRegion.getSuperregion().getRegionDownloadName();
				boolean isSuggestedMapsNeeded = !checkIfObjectDownloaded(mapName, app);
				if (isSuggestedMapsNeeded && !Algorithms.isEmpty(parentMapName)) {
					suggestedMaps.add(downloadRegion);
					break;
				}
			}
		}

		Set<WorldRegion> listWithoutDuplicates = new LinkedHashSet<>(suggestedMaps);
		suggestedMaps.clear();
		suggestedMaps.addAll(listWithoutDuplicates);

		return suggestedMaps;
	}
}
