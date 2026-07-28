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

public class HSTQuadTreeSimpleBenchmarkTest {

    static class RealMapObject {
        final long id;
        final String regionName;
        final BinaryMapDataObject dataObject;
        final int[] bbox;

        RealMapObject(long id, String regionName, BinaryMapDataObject dataObject, int[] bbox) {
            this.id = id;
            this.regionName = regionName;
            this.dataObject = dataObject;
            this.bbox = bbox;
        }

        @Override
        public String toString() {
            return regionName + " (#" + id + ")";
        }
    }

    static class IntersectionStat implements Comparable<IntersectionStat> {
        final RealMapObject targetObject;
        final Set<Long> intersectedObjIds = new HashSet<>();
        final List<String> intersectedNames = new ArrayList<>();
        long searchTimeNs;
        int inspectedEntriesCount;

        IntersectionStat(RealMapObject targetObject) {
            this.targetObject = targetObject;
        }

        @Override
        public int compareTo(IntersectionStat o) {
            return Integer.compare(o.intersectedObjIds.size(), this.intersectedObjIds.size());
        }
    }

    public static void main(String[] args) throws IOException {
        System.out.println("=== 1. Loading Real Map Data via OsmandRegions ===");
        HashSkipTileQuadTree<RealMapObject> tree = new HashSkipTileQuadTree<>();

        OsmandRegions or = new OsmandRegions(null);
        Map<String, LinkedList<BinaryMapDataObject>> obj = or.cacheAllCountries();

        List<RealMapObject> sampleObjects = new ArrayList<>();
        Map<Long, RealMapObject> sampleObjectsMap = new HashMap<>();

        prepareData(tree, obj, sampleObjects, sampleObjectsMap);

        System.out.println("Building QuadTree hierarchy...");
        long buildStart = System.nanoTime();
        tree.build();
        long buildTimeMs = (System.nanoTime() - buildStart) / 1_000_000;
        System.out.printf("QuadTree build completed in %d ms.\n\n", buildTimeMs);

        if (sampleObjects.isEmpty()) {
            System.err.println("No map objects loaded!");
            return;
        }

        System.out.printf("=== 2. Benchmarking Intersection Search for %d Objects ===\n", sampleObjects.size());

        for (int i = 0; i < Math.min(100, sampleObjects.size()); i++) {
            HashSkipTileQuadTree.SkipStats dummyStats = new HashSkipTileQuadTree.SkipStats(tree);
            tree.get(sampleObjects.get(i).bbox, dummyStats);
        }

        List<IntersectionStat> benchmarkResults = new ArrayList<>(sampleObjects.size());

        long totalSearchTimeNs = 0;
        long maxSearchTimeNs = 0;
        String maxTimeObject = "";

        long totalInspectedEntries = 0;
        int maxInspectedEntries = 0;
        String maxInspectedObject = "";

        for (RealMapObject targetObj : sampleObjects) {
            IntersectionStat stat = new IntersectionStat(targetObj);
            HashSkipTileQuadTree.SkipStats stats = new HashSkipTileQuadTree.SkipStats(tree);

            long startNs = System.nanoTime();
            List<TileEntry<RealMapObject>> results = tree.get(targetObj.bbox, stats);
            long elapsedNs = System.nanoTime() - startNs;

            stat.searchTimeNs = elapsedNs;
            stat.inspectedEntriesCount = stats.inspectedEntries.size();

            for (TileEntry<RealMapObject> entry : results) {
                if (entry.objId != targetObj.id) { 
                    if (stat.intersectedObjIds.add(entry.objId)) {
                        RealMapObject matchedObj = sampleObjectsMap.get(entry.objId);
                        stat.intersectedNames.add(matchedObj != null ? matchedObj.regionName : "Unknown");
                    }
                }
            }

            benchmarkResults.add(stat);

            totalSearchTimeNs += elapsedNs;
            if (elapsedNs > maxSearchTimeNs) {
                maxSearchTimeNs = elapsedNs;
                maxTimeObject = targetObj.regionName;
            }

            totalInspectedEntries += stat.inspectedEntriesCount;
            if (stat.inspectedEntriesCount > maxInspectedEntries) {
                maxInspectedEntries = stat.inspectedEntriesCount;
                maxInspectedObject = targetObj.regionName;
            }
        }

        double totalTimeMs = totalSearchTimeNs / 1e6;
        double avgTimeMs = (totalSearchTimeNs / (double) sampleObjects.size()) / 1e6;
        double maxTimeMs = maxSearchTimeNs / 1e6;
        double avgInspected = totalInspectedEntries / (double) sampleObjects.size();

        System.out.println("=========================================================================");
        System.out.println("                         BENCHMARK RESULTS                               ");
        System.out.println("=========================================================================");
        System.out.printf("Total Objects Evaluated : %d\n", sampleObjects.size());
        System.out.printf("Total Query Time        : %.2f ms\n", totalTimeMs);
        System.out.printf("Average Query Time      : %.4f ms (%.2f µs)\n", avgTimeMs, avgTimeMs * 1000);
        System.out.printf("Max Query Time          : %.2f ms ('%s')\n", maxTimeMs, maxTimeObject);
        System.out.println("-------------------------------------------------------------------------");
        System.out.printf("Average Inspected Entries: %.2f\n", avgInspected);
        System.out.printf("Max Inspected Entries    : %d ('%s')\n", maxInspectedEntries, maxInspectedObject);
        System.out.println("=========================================================================\n");

        // Сортируем по количеству пересечений и выводим Top 100
        Collections.sort(benchmarkResults);

        System.out.println("=== TOP 100 REGIONS BY BBOX INTERSECTIONS ===");
        int limit = Math.min(100, benchmarkResults.size());
        for (int i = 0; i < limit; i++) {
            IntersectionStat stat = benchmarkResults.get(i);
            String sampleIntersections = String.join(", ", stat.intersectedNames.subList(0, Math.min(5, stat.intersectedNames.size())));
            if (stat.intersectedNames.size() > 5) {
                sampleIntersections += ", ...";
            }

            System.out.printf("%3d. %-35s | Intersections: %3d | Search Time: %6.2f µs | Inspected: %3d | Intersects: [%s]\n",
                    i + 1,
                    stat.targetObject.regionName,
                    stat.intersectedObjIds.size(),
                    stat.searchTimeNs / 1000.0,
                    stat.inspectedEntriesCount,
                    sampleIntersections
            );
        }
    }

    private static void prepareData(HashSkipTileQuadTree<RealMapObject> tree,
                                    Map<String, LinkedList<BinaryMapDataObject>> obj,
                                    List<RealMapObject> sampleObjects,
                                    Map<Long, RealMapObject> sampleObjectsMap) {
        long objectIdCounter = 0;
        int totalRegions = 0;

        for (Entry<String, LinkedList<BinaryMapDataObject>> e : obj.entrySet()) {
            String regionName = e.getKey();
            LinkedList<BinaryMapDataObject> lst = e.getValue();
            if (lst == null) continue;
			if (regionName == null) {
				continue;
			}

            totalRegions++;

            for (BinaryMapDataObject o : lst) {
                int[] coordinates = o.getCoordinates();
                if (coordinates == null || coordinates.length < 2) continue;

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

                if (minX > maxX || minY > maxY) continue;

                int[] bbox = new int[] { minX, minY, maxX, maxY };
                long currentId = ++objectIdCounter;
                RealMapObject realObj = new RealMapObject(currentId, regionName, o, bbox);

                sampleObjects.add(realObj);
                sampleObjectsMap.put(currentId, realObj);

                tree.addObject(realObj, bbox, currentId);
            }
        }

        System.out.printf("Loaded %d regions with %d total binary map objects.\n", totalRegions, sampleObjects.size());
    }
}