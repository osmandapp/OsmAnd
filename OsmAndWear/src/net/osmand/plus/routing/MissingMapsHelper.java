package net.osmand.plus.routing;

import static net.osmand.util.MapUtils.calculateMidPoint;

import androidx.annotation.NonNull;

import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.map.WorldRegion;
import net.osmand.plus.download.DownloadResources;
import net.osmand.plus.onlinerouting.OnlineRoutingHelper;
import net.osmand.plus.onlinerouting.engine.OnlineRoutingEngine;
import net.osmand.plus.onlinerouting.engine.OnlineRoutingEngine.OnlineRoutingResponse;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;
import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class MissingMapsHelper {
	private static final Log LOG = PlatformUtil.getLog(MissingMapsHelper.class);

	private static final int MIN_STRAIGHT_DIST = 20000;
	private final RouteCalculationParams params;

	public MissingMapsHelper(@NonNull RouteCalculationParams params) {
		this.params = params;
	}

	public boolean isAnyPointOnWater(@NonNull List<Location> points) throws IOException {
		for (int i = 0; i < points.size(); i++) {
			Location point = points.get(i);
			List<WorldRegion> downloadRegions = params.ctx.getRegions().getWorldRegionsAt(
					new LatLon(point.getLatitude(), point.getLongitude()), true);
			if (downloadRegions.isEmpty()) {
				return true;
			}
		}
		return false;
	}

	@NonNull
	public List<Location> getDistributedPathPoints(@NonNull List<Location> points) {
		List<Location> result = new ArrayList<>(points.size());
		for (Location point : points) {
			result.add(new Location(point));
		}
		for (int i = 0; i < result.size() - 1; i++) {
			int nextIndex = i + 1;
			while (result.get(i).distanceTo(result.get(nextIndex)) > MIN_STRAIGHT_DIST) {
				Location location = calculateMidPoint(result.get(i), result.get(nextIndex));
				result.add(nextIndex, location);
			}
		}
		return removeDensePoints(result);
	}

	public List<Location> getStartFinishIntermediatePoints() {
		List<Location> points = new ArrayList<>();
		points.add(new Location("", params.start.getLatitude(), params.start.getLongitude()));
		if (params.intermediates != null) {
			for (LatLon l : params.intermediates) {
				points.add(new Location("", l.getLatitude(), l.getLongitude()));
			}
		}
		points.add(new Location("", params.end.getLatitude(), params.end.getLongitude()));
		return points;
	}

	public List<Location> findOnlineRoutePoints() throws IOException, JSONException {
		List<LatLon> points = new ArrayList<>();
		List<Location> routeLocation = new ArrayList<>();
		OnlineRoutingHelper onlineRoutingHelper = params.ctx.getOnlineRoutingHelper();
		List<Location> location = getStartFinishIntermediatePoints();
		for (Location e : location) {
			points.add(new LatLon(e.getLatitude(), e.getLongitude()));
		}
		OnlineRoutingEngine engine = onlineRoutingHelper.startOsrmEngine(params.mode);
		if (engine != null) {
			OnlineRoutingResponse response = onlineRoutingHelper.calculateRouteOnline(
					engine, points, params.start.hasBearing() ? params.start.getBearing() : null,
					params.leftSide, false, params.calculationProgress);
			if (response != null) {
				routeLocation.addAll(response.getRoute());
			}
		}
		return removeDensePoints(routeLocation);
	}

	@NonNull
	public List<WorldRegion> getMissingMaps(@NonNull List<Location> points) throws IOException {
		DownloadResources downloadResources = params.ctx.getDownloadThread().getIndexes();
		Set<WorldRegion> downloadedCountries = new LinkedHashSet<>();
		Set<WorldRegion> downloadedRegions = new LinkedHashSet<>();
		Set<WorldRegion> result = new LinkedHashSet<>();
		for (int i = 0; i < points.size(); i++) {
			Location l = points.get(i);
			int point31x = MapUtils.get31TileNumberX(l.getLongitude());
			int point31y = MapUtils.get31TileNumberY(l.getLatitude());
			LatLon latLon = new LatLon(l.getLatitude(), l.getLongitude());
			List<WorldRegion> worldRegions = params.ctx.getRegions().getWorldRegionsAt(latLon, true);
			Set<WorldRegion> regions = new LinkedHashSet<>();
			boolean hasAnyRegionDownloaded =
					!downloadResources.getExternalMapFileNamesAt(point31x, point31y, true).isEmpty();
			for (WorldRegion region : worldRegions) {
				String mapName = region.getRegionDownloadName();
				String countryMapName = region.getSuperregion().getRegionDownloadName();
				List<WorldRegion> subregions = region.getSubregions();
				boolean isDownloaded = params.ctx.getResourceManager().checkIfObjectDownloaded(mapName);
				boolean isCountry = Algorithms.isEmpty(countryMapName) && !subregions.isEmpty();
				if (!isCountry) {
					if (!isDownloaded) {
						regions.add(region);
					} else {
						downloadedRegions.add(region);
						hasAnyRegionDownloaded = true;
					}
				} else {
					if (isDownloaded) {
						downloadedCountries.add(region);
						hasAnyRegionDownloaded = true;
					}
				}
			}
			if (!hasAnyRegionDownloaded) {
				result.addAll(regions);
			}
		}
		for (WorldRegion country : downloadedCountries) {
			List<WorldRegion> subregions = country.getSubregions();
			Iterator<WorldRegion> it = result.iterator();
			while (it.hasNext()) {
				WorldRegion region = it.next();
				if (subregions.contains(region)) {
					it.remove();
				}
			}
		}
		List<WorldRegion> regions = new ArrayList<>(result);
		regions.addAll(downloadedRegions);
		Iterator<WorldRegion> it = result.iterator();
		while (it.hasNext()) {
			WorldRegion region = it.next();
			for (int i = 0; i < regions.size(); i++) {
				WorldRegion r = regions.get(i);
				if (!r.equals(region) && r.containsRegion(region)) {
					it.remove();
				}
			}
		}
		return new ArrayList<>(result);
	}

	@NonNull
	private List<Location> removeDensePoints(@NonNull List<Location> routeLocation) {
		List<Location> mapsBasedOnPoints = new ArrayList<>();
		if (!routeLocation.isEmpty()) {
			mapsBasedOnPoints.add(routeLocation.get(0));
			for (int i = 0, j = i + 1; j < routeLocation.size(); j++) {
				if (j == routeLocation.size() - 1 || routeLocation.get(i).distanceTo(routeLocation.get(j)) >= MIN_STRAIGHT_DIST) {
					mapsBasedOnPoints.add(routeLocation.get(j));
					i = j;
				}
			}
		}
		return mapsBasedOnPoints;
	}
}
