package net.osmand.search.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.osmand.binary.BinaryMapDataObject;
import net.osmand.map.OsmandRegions;
import net.osmand.search.core.HashSkipTileQuadTree.TileEntry;
import net.osmand.util.Algorithms;

public class HSTQuadTreeJoinTest {

	static class RealMapObject {
		final long id;
		final String regionName;
		final String downloadName;
		final BinaryMapDataObject dataObject;
		final int[] bbox;

		RealMapObject(long id, String regionName, String downloadName, BinaryMapDataObject dataObject, int[] bbox) {
			this.id = id;
			this.regionName = regionName;
			this.downloadName = downloadName;
			this.dataObject = dataObject;
			this.bbox = bbox;
		}

		@Override
		public String toString() {
			return regionName + " (#" + id + ")";
		}
	}

	static class PairResult implements Comparable<PairResult> {
		final String name1;
		final String name2;

		PairResult(String name1, String name2) {
//			if (name1.compareTo(name2) <= 0) {
//				this.name1 = name1;
//				this.name2 = name2;
//			} else {
				this.name1 = name2;
				this.name2 = name1;
//			}
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (!(o instanceof PairResult))
				return false;
			PairResult that = (PairResult) o;
			return name1.equals(that.name1) && name2.equals(that.name2);
		}

		@Override
		public int hashCode() {
			return 31 * name1.hashCode() + name2.hashCode();
		}

		@Override
		public int compareTo(PairResult o) {
			int cmp = this.name1.compareTo(o.name1);
			return cmp != 0 ? cmp : this.name2.compareTo(o.name2);
		}
	}

	public static void main(String[] args) throws IOException {
		System.out.println("=== 1. Loading Real Map Data via OsmandRegions ===");
		HashSkipTileQuadTree<RealMapObject> tree = new HashSkipTileQuadTree<>();

		OsmandRegions or = new OsmandRegions(null);
		Map<String, LinkedList<BinaryMapDataObject>> obj = or.cacheAllCountries();

		List<RealMapObject> sampleObjects = new ArrayList<>();
		Map<Long, RealMapObject> sampleObjectsMap = new HashMap<>();

		prepareData(tree, or, obj, sampleObjects, sampleObjectsMap);

		System.out.println("Building QuadTree hierarchy...");
		long buildStart = System.nanoTime();
		int size = tree.build();
		long buildTimeMs = (System.nanoTime() - buildStart) / 1_000_000;
		System.out.printf("QuadTree build completed - results %d in %d ms.\n\n", size, buildTimeMs);

		if (sampleObjects.isEmpty()) {
			System.err.println("No map objects loaded!");
			return;
		}

		// =========================================================================
		// 2. RUNNING SPATIAL JOIN BENCHMARK (HashSkipTileQuadTreeJoiner)
		// =========================================================================
		System.out.println("=== 2. Running HashSkipTileQuadTreeJoiner Benchmark ===");

		HashSkipTileQuadTreeJoiner<RealMapObject, RealMapObject> joiner = new HashSkipTileQuadTreeJoiner<>(tree, tree);

		HashSkipTileQuadTree.SkipStats stats1 = new HashSkipTileQuadTree.SkipStats(tree);
		HashSkipTileQuadTree.SkipStats stats2 = new HashSkipTileQuadTree.SkipStats(tree);

		Set<PairResult> joinerPairs = new HashSet<>();

		// Warmup JIT
		for (int i = 0; i < 3; i++) {
			joiner.joinAllBuckets((e1, e2) -> {
			}, null, null);
		}

		long joinStartNs = System.nanoTime();
		joiner.joinAllBuckets((e1, e2) -> {
			if (e1.objId != e2.objId) { // Исключаем самопересечение
				String dw1 = e1.obj.downloadName;
				String dw2 = e2.obj.downloadName;
				if (!Algorithms.isEmpty(dw1) && !Algorithms.isEmpty(dw2) && !dw1.equals(dw2)) {
					joinerPairs.add(new PairResult(dw1, dw2));
				}
			}
		}, stats1, stats2);
		long joinElapsedNs = System.nanoTime() - joinStartNs;

		double joinTimeMs = joinElapsedNs / 1e6;

		// =========================================================================
		// 3. RUNNING REFERENCE CHECK (N x Query Benchmark via tree.get BBox)
		// =========================================================================
		System.out.println("=== 3. Running Reference Baseline (tree.get BBox Queries) ===");

		Set<PairResult> baselinePairs = new HashSet<>();
		long baselineStartNs = System.nanoTime();

		for (RealMapObject targetObj : sampleObjects) {
			HashSkipTileQuadTree.SkipStats queryStats = new HashSkipTileQuadTree.SkipStats(tree);
			List<TileEntry<RealMapObject>> results = tree.get(targetObj.bbox, queryStats);

			for (TileEntry<RealMapObject> entry : results) {
				if (entry.objId != targetObj.id) {
					String dw1 = targetObj.downloadName;
					String dw2 = entry.obj.downloadName;
					if (!Algorithms.isEmpty(dw1) && !Algorithms.isEmpty(dw2) && !dw1.equals(dw2)) {
						baselinePairs.add(new PairResult(dw1, dw2));
					}
				}
			}
		}
		stats1.printStats();
		stats2.printStats();
		long baselineElapsedNs = System.nanoTime() - baselineStartNs;
		double baselineTimeMs = baselineElapsedNs / 1e6;

		// =========================================================================
		// 4. FUNCTIONAL TEST & DATA PARITY VALIDATION
		// =========================================================================
		System.out.println("\n=========================================================================");
		System.out.println("                     FUNCTIONAL TEST & DATA PARITY                       ");
		System.out.println("=========================================================================");

		Set<PairResult> missingInJoiner = new HashSet<>(baselinePairs);
		missingInJoiner.removeAll(joinerPairs);

		Set<PairResult> extraInJoiner = new HashSet<>(joinerPairs);
		extraInJoiner.removeAll(baselinePairs);

		boolean paritySuccess = missingInJoiner.isEmpty() && extraInJoiner.isEmpty();

		System.out.printf("Total Intersecting Region Pairs (Joiner)  : %d\n", joinerPairs.size());
		System.out.printf("Total Intersecting Region Pairs (Baseline): %d\n", baselinePairs.size());
		System.out.printf("Data Parity Result                        : %s\n",
				paritySuccess ? "✅ PERFECT MATCH (0 Mismatches)" : "❌ MISMATCHES DETECTED!");

		if (!missingInJoiner.isEmpty()) {
			System.err.printf("⚠️ Missing Pairs in Joiner (%d):\n", missingInJoiner.size());
			int limit = 0;
			for (PairResult p : missingInJoiner) {
				System.err.printf("   - %s <-> %s\n", p.name1, p.name2);
				if (++limit >= 10)
					break;
			}
		}

		if (!extraInJoiner.isEmpty()) {
			System.err.printf("⚠️ Extra Pairs in Joiner (%d):\n", extraInJoiner.size());
			int limit = 0;
			for (PairResult p : extraInJoiner) {
				System.err.printf("   - %s <-> %s\n", p.name1, p.name2);
				if (++limit >= 10)
					break;
			}
		}

		// =========================================================================
		// 5. PERFORMANCE RESULTS SUMMARY
		// =========================================================================
		System.out.println("-------------------------------------------------------------------------");
		System.out.println("                         PERFORMANCE SUMMARY                             ");
		System.out.println("-------------------------------------------------------------------------");
		System.out.printf(" 🚀 Joiner Total Time (joinAllBuckets) : %.2f ms\n", joinTimeMs);
		System.out.printf(" 🐢 Baseline Total Time (N x tree.get) : %.2f ms\n", baselineTimeMs);
		System.out.printf(" ⚡ Speedup Factor                     : %.2fx faster!\n",
				baselineElapsedNs / (double) joinElapsedNs);
		System.out.println("-------------------------------------------------------------------------");
		System.out.println(" 📊 Joiner Skip Stats (Tree 1):");
		System.out.printf("    Total Skips Count   : %d\n", stats1.totalSkipsCount);
		System.out.printf("    Total Elements Skip : %d\n", stats1.totalElementsSkipped);
		System.out.println(" 📊 Joiner Skip Stats (Tree 2 - Index Tree Iteration):");
		System.out.printf("    Total Skips Count   : %d\n", stats2.totalSkipsCount);
		System.out.printf("    Total Elements Skip : %d\n", stats2.totalElementsSkipped);
		System.out.println("=========================================================================\n");

		// Top 20 Intersecting Pairs
		List<PairResult> sortedPairs = new ArrayList<>(joinerPairs);
		Collections.sort(sortedPairs);

		System.out.println("=== SAMPLE TOP 20 INTERSECTING REGION PAIRS ===");
		int limit = Math.min(5, sortedPairs.size());
		for (int i = 0; i < limit; i++) {
			PairResult pair = sortedPairs.get(i);
			System.out.printf("%2d. %-30s <---> %-30s\n", i + 1, pair.name1, pair.name2);
		}
	}

	private static void prepareData(HashSkipTileQuadTree<RealMapObject> tree, OsmandRegions or,
			Map<String, LinkedList<BinaryMapDataObject>> obj, List<RealMapObject> sampleObjects,
			Map<Long, RealMapObject> sampleObjectsMap) {
		long objectIdCounter = 0;
		int totalRegions = 0;

		for (Entry<String, LinkedList<BinaryMapDataObject>> e : obj.entrySet()) {
			String regionName = e.getKey();
			LinkedList<BinaryMapDataObject> lst = e.getValue();
			if (lst == null || regionName == null)
				continue;

			totalRegions++;

			for (BinaryMapDataObject o : lst) {
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

//                for(int k = 0; k < 10; k++) {
				int[] bbox = new int[] { minX, minY, maxX, maxY };
				long currentId = ++objectIdCounter;
				String dwName = or.getDownloadName(o);

				RealMapObject realObj = new RealMapObject(currentId, regionName, dwName, o, bbox);

				sampleObjects.add(realObj);
				sampleObjectsMap.put(currentId, realObj);

				tree.addObject(realObj, bbox, currentId);
			}
		}

		System.out.printf("Loaded %d regions with %d total binary map objects.\n", totalRegions, sampleObjects.size());
	}
}