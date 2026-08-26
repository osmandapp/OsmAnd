package net.osmand.search.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import net.osmand.binary.BinaryMapDataObject;
import net.osmand.map.OsmandRegions;
import net.osmand.util.MapUtils;

public class OsmGpxRegionsJoinTest {

	// =========================================================================
	// 1. FILTER & LOADING CONFIGURATION (Set to null, empty or 0 to disable)
	// =========================================================================
	public static final int LIMIT_TRACKS = 500_000;

	public static Set<String> ALLOWED_ACTIVITIES = new HashSet<>();
	static {
		// ALLOWED_ACTIVITIES.add("nospeed");
		// ALLOWED_ACTIVITIES.add("cycling");
		// ALLOWED_ACTIVITIES.add("running");
	}

	public static double MAX_SPEED_KMH = 0; // e.g. 100 (km/h). Set to 0 to disable.

	public static String TAG_CONTAINS = null; // e.g. "amenity". Set to null to disable.

	public static int START_YEAR = 2020; // e.g. 2021. Set to 0 to disable.
	public static int END_YEAR = 2025; // e.g. 2022. Set to 0 to disable.

	// Geographic BBox filter: [minLat, minLon, maxLat, maxLon]. Set to null to
	// disable.
	public static double[] FILTER_BBOX = null; // e.g. new double[] {24.0, 120.0, 26.0, 122.0}

	// =========================================================================
	// 2. OUTPUT DISPLAY CONFIGURATION
	// =========================================================================
	public static final int PRINT_TOP_REGIONS = 200;
	public static final int PRINT_TRACK_EXAMPLES = 7;

	static class GpxTrackObject {
		final long id;
		final String name;
		final String activity;
		final double speed;
		final String tags;
		final int[] bbox;

		GpxTrackObject(long id, String name, String activity, double speed, String tags, int[] bbox) {
			this.id = id;
			this.name = name;
			this.activity = activity;
			this.speed = speed;
			this.tags = tags;
			this.bbox = bbox;
		}
	}

	static class TrackFilter {
		public boolean test(String activity, double speedMps, String tags, String dateStr, double minLat, double minLon,
				double maxLat, double maxLon) {

			// 1. Activity filter
			if (ALLOWED_ACTIVITIES != null && !ALLOWED_ACTIVITIES.isEmpty()) {
				if (!ALLOWED_ACTIVITIES.contains(activity)) {
					return false;
				}
			}

			// 2. Max speed filter (Convert MAX_SPEED_KMH -> m/s)
			if (MAX_SPEED_KMH > 0) {
				double maxSpeedMps = MAX_SPEED_KMH / 3.6;
				if (speedMps >= maxSpeedMps) {
					return false;
				}
			}

			// 3. Tags filter
			if (TAG_CONTAINS != null && !TAG_CONTAINS.isEmpty()) {
				if (tags == null || !tags.contains(TAG_CONTAINS)) {
					return false;
				}
			}

			// 4. Date filter (parse year from "YYYY-MM-DD ..." format)
			if (START_YEAR > 0 || END_YEAR > 0) {
				if (dateStr == null || dateStr.length() < 4) {
					return false;
				}
				try {
					int year = Integer.parseInt(dateStr.substring(0, 4));
					if (START_YEAR > 0 && year < START_YEAR) {
						return false;
					}
					if (END_YEAR > 0 && year > END_YEAR) {
						return false;
					}
				} catch (NumberFormatException e) {
					return false;
				}
			}

			// 5. Geographic BBox intersection filter
			if (FILTER_BBOX != null && FILTER_BBOX.length == 4) {
				double fMinLat = FILTER_BBOX[0];
				double fMinLon = FILTER_BBOX[1];
				double fMaxLat = FILTER_BBOX[2];
				double fMaxLon = FILTER_BBOX[3];

				boolean intersects = !(maxLat < fMinLat || minLat > fMaxLat || maxLon < fMinLon || minLon > fMaxLon);
				if (!intersects) {
					return false;
				}
			}

			return true;
		}
	}

	public static void main(String[] args) throws Exception {
		File file = new File(System.getProperty("maps") != null ? System.getProperty("maps") : ".",
				"osm_gpx_data.csv.gz");

		if (!file.exists()) {
			System.err.println("File not found: " + file.getAbsolutePath());
			return;
		}

		TrackFilter filter = new TrackFilter();

		HashSkipTileQuadTree<GpxTrackObject> tracksTree = new HashSkipTileQuadTree<>();
		System.out.printf("=== 1. Reading max %d tracks from %s ===\n", LIMIT_TRACKS, file.getName());

		long startCsv = System.currentTimeMillis();
		int loadedTracksCount = loadGpxTracks(file, tracksTree, LIMIT_TRACKS, filter);
		long tracksBuildStart = System.nanoTime();
		tracksTree.build();
		long tracksBuildMs = (System.nanoTime() - tracksBuildStart) / 1_000_000;

		System.out.printf("Loaded %d tracks in %d ms (Tree build time: %d ms)\n\n", loadedTracksCount,
				System.currentTimeMillis() - startCsv, tracksBuildMs);

		HashSkipTileQuadTree<HSTQuadTreeJoinTest.RealMapObject> regionsTree = new HashSkipTileQuadTree<>();
		System.out.println("=== 2. Loading OsmAnd Regions ===");
		long startRegions = System.currentTimeMillis();

		OsmandRegions or = new OsmandRegions(null);
		Map<String, LinkedList<BinaryMapDataObject>> obj = or.cacheAllCountries();
		loadOsmandRegions(or, obj, regionsTree);

		long regionsBuildStart = System.nanoTime();
		regionsTree.build();
		long regionsBuildMs = (System.nanoTime() - regionsBuildStart) / 1_000_000;
		System.out.printf("Loaded OsmAnd Regions in %d ms (Tree build time: %d ms)\n\n",
				System.currentTimeMillis() - startRegions, regionsBuildMs);

		List<Map.Entry<String, Set<String>>> sortedRegions = benchmarkIntersection(tracksTree, loadedTracksCount,
				regionsTree);
//		benchmarkQueryByBboxPerRegion(sortedRegions, obj, tracksTree);
//		benchmarkTrackTileDensityPerRegion(sortedRegions, obj, tracksTree);
		System.out.println(sortedRegions.size());
	}

	private static List<Map.Entry<String, Set<String>>> benchmarkIntersection(
			HashSkipTileQuadTree<GpxTrackObject> tracksTree, int loadedTracksCount,
			HashSkipTileQuadTree<HSTQuadTreeJoinTest.RealMapObject> regionsTree) {
		System.out.println("=== 3. Executing Spatial Join (Tracks x Regions) ===");
		HashSkipTileQuadTreeJoiner<GpxTrackObject, HSTQuadTreeJoinTest.RealMapObject> joiner = new HashSkipTileQuadTreeJoiner<>(
				tracksTree, regionsTree);

		HashSkipTileQuadTree.SkipStats stats1 = new HashSkipTileQuadTree.SkipStats(tracksTree);
		HashSkipTileQuadTree.SkipStats stats2 = new HashSkipTileQuadTree.SkipStats(regionsTree);

		Map<String, Set<String>> regionToTracksMap = new HashMap<>();

		long joinStartNs = System.nanoTime();
		int[] totalIntersections = new int[1];
		joiner.joinAllBuckets((trackEntry, regionEntry) -> {
			GpxTrackObject track = trackEntry.obj;
			HSTQuadTreeJoinTest.RealMapObject region = regionEntry.obj;
			totalIntersections[0]++;
			String downloadName = region.downloadName;
			if (downloadName != null && !downloadName.isEmpty()) {
				regionToTracksMap.computeIfAbsent(downloadName, k -> new HashSet<>())
						.add(track.name + " (#" + track.id + ")");
			}
		}, stats1, stats2);

		long joinElapsedMs = (System.nanoTime() - joinStartNs) / 1_000_000;

		System.out.println("\n=========================================================================");
		System.out.println("                         BENCHMARK RESULTS                               ");
		System.out.println("=========================================================================");
		System.out.printf(" Total Tracks Processed    : %,d\n", loadedTracksCount);
		System.out.printf(" Total Intersecting Regions: %,d\n", regionToTracksMap.size());
		System.out.printf(" Total Intersections: %,d\n", totalIntersections[0]);
		System.out.printf(" 🚀 Spatial Join Time       : %d ms\n", joinElapsedMs);
		System.out.println("-------------------------------------------------------------------------");
		System.out.println(" 📊 Skip Stats (Tracks Tree):");
		System.out.printf("    Total Skips Count   : %,d\n", stats1.totalSkipsCount);
		System.out.printf("    Total Elements Skip : %,d\n", stats1.totalElementsSkipped);
		stats1.printStats();
		System.out.println(" 📊 Skip Stats (Regions Tree):");
		System.out.printf("    Total Skips Count   : %,d\n", stats2.totalSkipsCount);
		System.out.printf("    Total Elements Skip : %,d\n", stats2.totalElementsSkipped);
		stats2.printStats();
		System.out.println("=========================================================================\n");

		List<Map.Entry<String, Set<String>>> sortedRegions = new ArrayList<>(regionToTracksMap.entrySet());
		sortedRegions.sort((e1, e2) -> Integer.compare(e2.getValue().size(), e1.getValue().size()));

		System.out.printf("=== TOP %d REGIONS BY INTERSECTING GPX TRACKS ===\n", PRINT_TOP_REGIONS);
		int topLimit = Math.min(PRINT_TOP_REGIONS, sortedRegions.size());
		for (int i = 0; i < topLimit; i++) {
			Map.Entry<String, Set<String>> entry = sortedRegions.get(i);
			String regionName = entry.getKey();
			Set<String> tracks = entry.getValue();

			StringBuilder examplesBuilder = new StringBuilder();
			Iterator<String> it = tracks.iterator();
			int sampleCount = 0;
			while (it.hasNext() && sampleCount < PRINT_TRACK_EXAMPLES) {
				if (sampleCount > 0) {
					examplesBuilder.append(" | ");
				}
				examplesBuilder.append(it.next());
				sampleCount++;
			}

			System.out.printf("%3d. %-32s | Count: %-5d | Examples: %s\n", i + 1, regionName, tracks.size(),
					examplesBuilder.toString());
		}
		return sortedRegions;
	}

	private static int loadGpxTracks(File gzFile, HashSkipTileQuadTree<GpxTrackObject> tree, int limit,
			TrackFilter filter) throws Exception {
		int count = 0;

		try (GZIPInputStream gis = new GZIPInputStream(new FileInputStream(gzFile));
				BufferedReader reader = new BufferedReader(new InputStreamReader(gis, StandardCharsets.UTF_8))) {

			String headerLine = reader.readLine();
			if (headerLine == null)
				return 0;

			String line;
			int lineNum = 0;
			while ((line = reader.readLine()) != null && count < limit) {
				String[] cols = parseCsvLine(line);
				if (++lineNum % 50000 == 0) {
					System.out.printf("Read %d lines...\n", lineNum);
				}
				if (cols.length < 24)
					continue;

				try {
					long id = Long.parseLong(cols[0]);
					String dateStr = cols[2];
					String name = cols[3];

					double minLat = Double.parseDouble(cols[9]);
					double minLon = Double.parseDouble(cols[10]);
					double maxLon = Double.parseDouble(cols[11]);
					double maxLat = Double.parseDouble(cols[12]);

					String tags = cols[13];
					String activity = cols[14];
					double speed = cols[15].isEmpty() ? 0.0 : Double.parseDouble(cols[15]);

					if (filter != null
							&& !filter.test(activity, speed, tags, dateStr, minLat, minLon, maxLat, maxLon)) {
						continue;
					}

					int min31X = MapUtils.get31TileNumberX(minLon);
					int max31X = MapUtils.get31TileNumberX(maxLon);
					int min31Y = MapUtils.get31TileNumberY(maxLat);
					int max31Y = MapUtils.get31TileNumberY(minLat);

					int[] bbox31 = new int[] { Math.min(min31X, max31X), Math.min(min31Y, max31Y),
							Math.max(min31X, max31X), Math.max(min31Y, max31Y) };

					GpxTrackObject trackObj = new GpxTrackObject(id, name, activity, speed, tags, bbox31);
					tree.addObject(trackObj, bbox31, id);
					count++;
				} catch (Exception e) {
					// Skip malformed CSV lines
				}
			}
		}
		return count;
	}

	private static void loadOsmandRegions(OsmandRegions or, Map<String, LinkedList<BinaryMapDataObject>> objMap,
			HashSkipTileQuadTree<HSTQuadTreeJoinTest.RealMapObject> regionsTree) {
		long objectIdCounter = 0;

		for (Map.Entry<String, LinkedList<BinaryMapDataObject>> entry : objMap.entrySet()) {
			String regionName = entry.getKey();
			LinkedList<BinaryMapDataObject> list = entry.getValue();
			if (list == null || regionName == null)
				continue;

			for (BinaryMapDataObject o : list) {
				int[] coordinates = o.getCoordinates();
				if (coordinates == null || coordinates.length < 2)
					continue;

				int minX = Integer.MAX_VALUE;
				int minY = Integer.MAX_VALUE;
				int maxX = Integer.MIN_VALUE;
				int maxY = Integer.MIN_VALUE;

				for (int i = 0; i < coordinates.length; i += 2) {
					int x = coordinates[i];
					int y = coordinates[i + 1];
					minX = Math.min(minX, x);
					maxX = Math.max(maxX, x);
					minY = Math.min(minY, y);
					maxY = Math.max(maxY, y);
				}

				if (minX > maxX || minY > maxY)
					continue;

				int[] bbox = new int[] { minX, minY, maxX, maxY };
				long currentId = ++objectIdCounter;
				String dwName = or.getDownloadName(o);

				HSTQuadTreeJoinTest.RealMapObject realObj = new HSTQuadTreeJoinTest.RealMapObject(currentId, regionName,
						dwName, o, bbox);
				regionsTree.addObject(realObj, bbox, currentId);
			}
		}
	}

	static void benchmarkQueryByBboxPerRegion(List<Map.Entry<String, Set<String>>> sortedRegions,
			Map<String, LinkedList<BinaryMapDataObject>> regionsMap, HashSkipTileQuadTree<GpxTrackObject> tracksTree) {

		System.out.println("\n=========================================================================");
		System.out.println("            QUERY BY BBOX BENCHMARK (TOP 200 REGIONS BY ZOOM)            ");
		System.out.println("=========================================================================");

		int topLimit = Math.min(PRINT_TOP_REGIONS, sortedRegions.size());

		for (int i = 0; i < topLimit; i++) {
			String countryName = sortedRegions.get(i).getKey();
			LinkedList<BinaryMapDataObject> list = regionsMap.get(countryName);

			if (list == null || list.isEmpty()) {
				continue;
			}

			int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
			int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;

			for (BinaryMapDataObject o : list) {
				int[] coordinates = o.getCoordinates();
				if (coordinates == null)
					continue;
				for (int j = 0; j < coordinates.length; j += 2) {
					int x = coordinates[j];
					int y = coordinates[j + 1];
					minX = Math.min(minX, x);
					maxX = Math.max(maxX, x);
					minY = Math.min(minY, y);
					maxY = Math.max(maxY, y);
				}
			}

			if (minX > maxX || minY > maxY)
				continue;
			int[] queryBBox = new int[] { minX, minY, maxX, maxY };

			HashSkipTileQuadTree.SkipStats totalStats = new HashSkipTileQuadTree.SkipStats(tracksTree);
			long startTotal = System.nanoTime();
			List<HashSkipTileQuadTree.TileEntry<GpxTrackObject>> totalEntries = tracksTree.get(queryBBox, totalStats);
			double totalTimeMs = (System.nanoTime() - startTotal) / 1e6;

			Set<Long> uniqueTrackIds = new HashSet<>();
			if (totalEntries != null) {
				for (HashSkipTileQuadTree.TileEntry<GpxTrackObject> entry : totalEntries) {
					uniqueTrackIds.add(entry.obj.id);
				}
			}

			StringBuilder zoomsBreakdown = new StringBuilder();
			for (int z = 3; z <= 16; z++) {
				HashSkipTileQuadTree.SkipStats zStats = new HashSkipTileQuadTree.SkipStats(tracksTree);
				long startZ = System.nanoTime();

				List<HashSkipTileQuadTree.TileEntry<GpxTrackObject>> zEntries = tracksTree.get(queryBBox, z, z, zStats);
				if (zEntries != null && !zEntries.isEmpty()) {
					if (zoomsBreakdown.length() > 0) {
						zoomsBreakdown.append(" | ");
					}
					Set<Long> zUnique = new HashSet<>();
					for (HashSkipTileQuadTree.TileEntry<GpxTrackObject> e : zEntries) {
						zUnique.add(e.obj.id);
					}
					zoomsBreakdown.append(
							String.format("z=%d: %,d (%.1fms)", z, zUnique.size(), (System.nanoTime() - startZ) / 1e6));
				}
			}

			System.out.printf("%-35s | Tracks: %,d (Entries: %,d, %.1f ms) | %s\n", countryName, uniqueTrackIds.size(),
					totalEntries != null ? totalEntries.size() : 0, totalTimeMs, zoomsBreakdown.toString());
		}
	}

	private static String[] parseCsvLine(String line) {
		List<String> result = new ArrayList<>();
		boolean inQuotes = false;
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < line.length(); i++) {
			char c = line.charAt(i);
			if (c == '"') {
				inQuotes = !inQuotes;
			} else if (c == ',' && !inQuotes) {
				result.add(sb.toString().trim());
				sb.setLength(0);
			} else {
				sb.append(c);
			}
		}
		result.add(sb.toString().trim());
		return result.toArray(new String[0]);
	}

	static void benchmarkTrackTileDensityPerRegion(List<Map.Entry<String, Set<String>>> sortedRegions,
			Map<String, LinkedList<BinaryMapDataObject>> regionsMap, HashSkipTileQuadTree<GpxTrackObject> tracksTree) {

		// Shift parameter: includes tracks from higher zoom levels up to (Z + SHIFT_Z)
		final int SHIFT_Z = 2;

		System.out.println("\n=========================================================================================================");
		System.out.println("                 TRACK-TILE DENSITY BENCHMARK BY REGION (ZOOMS 3 to 16)                                  ");
		System.out.println("=========================================================================================================");

		int topLimit = Math.min(PRINT_TOP_REGIONS, sortedRegions.size());

		class RegionDensityStat {
			String countryName;
			int uniqueTracksCount;
			long trackTilesZ16;
			long countryTilesZ16;
			double densityZ16;
			String zoomBreakdownStr;
		}

		List<RegionDensityStat> regionStats = new ArrayList<>();

		for (int i = 0; i < topLimit; i++) {
			String countryName = sortedRegions.get(i).getKey();
			LinkedList<BinaryMapDataObject> list = regionsMap.get(countryName);

			if (list == null || list.isEmpty()) {
				continue;
			}

			// 1. Calculate the bounding box for the entire country/region
			int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
			int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;

			for (BinaryMapDataObject o : list) {
				int[] coordinates = o.getCoordinates();
				if (coordinates == null)
					continue;
				for (int j = 0; j < coordinates.length; j += 2) {
					int x = coordinates[j];
					int y = coordinates[j + 1];
					minX = Math.min(minX, x);
					maxX = Math.max(maxX, x);
					minY = Math.min(minY, y);
					maxY = Math.max(maxY, y);
				}
			}

			if (minX > maxX || minY > maxY)
				continue;
			int[] countryBBox31 = new int[] { minX, minY, maxX, maxY };

			// 2. Fetch all unique GPX tracks located inside the region's BBox
			List<HashSkipTileQuadTree.TileEntry<GpxTrackObject>> totalEntries = tracksTree.get(countryBBox31, null);
			if (totalEntries == null || totalEntries.isEmpty()) {
				continue;
			}

			Map<Long, GpxTrackObject> uniqueTracks = new HashMap<>();
			for (HashSkipTileQuadTree.TileEntry<GpxTrackObject> entry : totalEntries) {
				uniqueTracks.put(entry.obj.id, entry.obj);
			}

			int totalTrackCount = uniqueTracks.size();
			if (totalTrackCount == 0)
				continue;

			// 3. Determine the native zoom level for each track in the quad-tree index
			Map<GpxTrackObject, Integer> trackNativeZoomMap = new HashMap<>();
			for (GpxTrackObject track : uniqueTracks.values()) {
				int nativeZ = tracksTree.getNativeZoom(track.bbox);
				trackNativeZoomMap.put(track, nativeZ);
			}

			// 4. Calculate TrackTiles, CountryTiles, and Density per zoom level [3..16]
			long[] trackTilesPerZoom = new long[17];
			long[] countryTilesPerZoom = new long[17];

			for (int z = 3; z <= 16; z++) {
				int shift = 31 - z;

				// 4a. Convert 31-bit country BBox coordinates to tile counts at Zoom Z
				long cMinX = countryBBox31[0] >> shift;
				long cMinY = countryBBox31[1] >> shift;
				long cMaxX = countryBBox31[2] >> shift;
				long cMaxY = countryBBox31[3] >> shift;
				countryTilesPerZoom[z] = Math.max(1, (cMaxX - cMinX + 1)) * Math.max(1, (cMaxY - cMinY + 1));

				// 4b. Calculate track tiles considering only tracks with nativeZ <= (z +
				// SHIFT_Z)
				long currentZoomTrackTiles = 0;
				int maxAllowedZoom = z + SHIFT_Z;

				for (Map.Entry<GpxTrackObject, Integer> entry : trackNativeZoomMap.entrySet()) {
					int nativeZ = entry.getValue();

					// Exclude tracks that reside on zoom levels deeper than (z + SHIFT_Z)
					if (nativeZ <= maxAllowedZoom) {
						int[] tBbox = entry.getKey().bbox;
						long tMinX = tBbox[0] >> shift;
						long tMinY = tBbox[1] >> shift;
						long tMaxX = tBbox[2] >> shift;
						long tMaxY = tBbox[3] >> shift;

						long tilesX = Math.max(1, tMaxX - tMinX + 1);
						long tilesY = Math.max(1, tMaxY - tMinY + 1);
						currentZoomTrackTiles += (tilesX * tilesY);
					}
				}
				trackTilesPerZoom[z] = currentZoomTrackTiles;
			}

			// 5. Build detailed per-zoom breakdown string
			StringBuilder zoomsBreakdown = new StringBuilder();
			for (int z = 3; z <= 16; z++) {
				long trTiles = trackTilesPerZoom[z];
				long cTiles = countryTilesPerZoom[z];
				double density = cTiles > 0 ? (double) trTiles / cTiles : 0.0;

				if (zoomsBreakdown.length() > 0) {
					zoomsBreakdown.append(" | ");
				}
				zoomsBreakdown.append(String.format("Z=%d: %.1f (%,d/%,d)", z, density, trTiles, cTiles));
			}

			RegionDensityStat stat = new RegionDensityStat();
			stat.countryName = countryName;
			stat.uniqueTracksCount = totalTrackCount;
			stat.trackTilesZ16 = trackTilesPerZoom[16];
			stat.countryTilesZ16 = countryTilesPerZoom[16];
			stat.densityZ16 = countryTilesPerZoom[16] > 0 ? (double) trackTilesPerZoom[16] / countryTilesPerZoom[16]
					: 0.0;
			stat.zoomBreakdownStr = zoomsBreakdown.toString();

			regionStats.add(stat);
		}

		// 6. Sort regions by Density at Z=16 in descending order
		regionStats.sort((r1, r2) -> Double.compare(r2.densityZ16, r1.densityZ16));

		// 7. Print formatted benchmark table
		for (int i = 0; i < regionStats.size(); i++) {
			RegionDensityStat stat = regionStats.get(i);
			System.out.printf(
					"%3d. %-28s | Tracks: %,5d | TrackTilesZ16: %,8d | RegionTilesZ16: %,10d | DensityZ16: %.1f | %s\n",
					i + 1, stat.countryName, stat.uniqueTracksCount, stat.trackTilesZ16, stat.countryTilesZ16,
					stat.densityZ16, stat.zoomBreakdownStr);
		}
	}
}