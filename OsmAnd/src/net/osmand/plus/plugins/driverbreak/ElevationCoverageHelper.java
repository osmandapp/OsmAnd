/*
 * OsmAnd, OsmAnd BV <info@osmand.net>, 2010–present
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.osmand.plus.plugins.driverbreak;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.plus.OsmandApplication;
import net.osmand.router.RouteSegmentResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Finds missing SRTM tiles grouped by administrative region.
 */
public final class ElevationCoverageHelper {

	public static final class RegionMissingTiles {
		public final ElevationAdministrativeRegion region;
		public final List<SRTMTileIndex> missingTiles;

		RegionMissingTiles(@NonNull ElevationAdministrativeRegion region,
				@NonNull List<SRTMTileIndex> missingTiles) {
			this.region = region;
			this.missingTiles = missingTiles;
		}

		public int getMissingTileCount() {
			return missingTiles.size();
		}
	}

	private ElevationCoverageHelper() {
	}

	@NonNull
	public static List<SRTMTileIndex> tilesInBoundingBox(@NonNull QuadRect bbox) {
		int minLon = (int) Math.floor(bbox.left);
		int maxLon = (int) Math.floor(bbox.right);
		int minLat = (int) Math.floor(bbox.bottom);
		int maxLat = (int) Math.floor(bbox.top);
		List<SRTMTileIndex> tiles = new ArrayList<>();
		for (int lat = minLat; lat <= maxLat; lat++) {
			for (int lon = minLon; lon <= maxLon; lon++) {
				tiles.add(new SRTMTileIndex(lon, lat));
			}
		}
		return tiles;
	}

	@NonNull
	public static List<SRTMTileIndex> missingTilesInBoundingBox(@NonNull SRTMElevationProvider provider,
			@NonNull QuadRect bbox) {
		List<SRTMTileIndex> missing = new ArrayList<>();
		for (SRTMTileIndex tile : tilesInBoundingBox(bbox)) {
			if (!provider.hasTile(tile.lonIndex, tile.latIndex)) {
				missing.add(tile);
			}
		}
		return missing;
	}

	@Nullable
	public static RegionMissingTiles missingTilesForRegion(@NonNull SRTMElevationProvider provider,
			@NonNull ElevationAdministrativeRegion region) {
		List<SRTMTileIndex> missing = missingTilesInBoundingBox(provider, region.getBoundingBox());
		if (missing.isEmpty()) {
			return null;
		}
		return new RegionMissingTiles(region, missing);
	}

	@NonNull
	public static List<RegionMissingTiles> missingTilesForRoute(@NonNull OsmandApplication app,
			@NonNull SRTMElevationProvider provider, @NonNull List<LatLon> routePoints,
			@NonNull Set<String> declinedRegionIds) {
		Map<String, RegionMissingTiles> grouped = new LinkedHashMap<>();
		Set<SRTMTileIndex> routeTiles = collectRouteTiles(routePoints);
		for (SRTMTileIndex tile : routeTiles) {
			if (provider.hasTile(tile.lonIndex, tile.latIndex)) {
				continue;
			}
			LatLon center = new LatLon(tile.latIndex + 0.5, tile.lonIndex + 0.5);
			ElevationAdministrativeRegion region = ElevationRegionResolver.resolveAt(app, center);
			if (region == null || declinedRegionIds.contains(region.getRegionId())) {
				continue;
			}
			RegionMissingTiles entry = grouped.get(region.getRegionId());
			if (entry == null) {
				List<SRTMTileIndex> tiles = new ArrayList<>();
				tiles.add(tile);
				grouped.put(region.getRegionId(), new RegionMissingTiles(region, tiles));
			} else if (!entry.missingTiles.contains(tile)) {
				entry.missingTiles.add(tile);
			}
		}
		return new ArrayList<>(grouped.values());
	}

	@NonNull
	public static List<LatLon> routeSamplePoints(@Nullable List<RouteSegmentResult> segments) {
		Set<LatLon> points = new LinkedHashSet<>();
		if (segments == null) {
			return new ArrayList<>();
		}
		for (RouteSegmentResult segment : segments) {
			if (segment == null) {
				continue;
			}
			points.add(segment.getPoint(segment.getStartPointIndex()));
			points.add(segment.getPoint(segment.getEndPointIndex()));
		}
		return new ArrayList<>(points);
	}

	@NonNull
	private static Set<SRTMTileIndex> collectRouteTiles(@NonNull Collection<LatLon> routePoints) {
		Set<SRTMTileIndex> tiles = new LinkedHashSet<>();
		for (LatLon point : routePoints) {
			tiles.add(SRTMTileIndex.fromCoordinate(point.getLatitude(), point.getLongitude()));
		}
		return tiles;
	}
}
