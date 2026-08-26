package net.osmand.search.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import net.osmand.binary.BinaryMapDataObject;
import net.osmand.map.OsmandRegions;
import net.osmand.search.core.HashSkipTileQuadTree.TileEntry;
import net.osmand.util.Algorithms;

public class HSTQuadTreeBenchmarkTest {

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

    static class IntersectionStat implements Comparable<IntersectionStat> {
        final RealMapObject targetObject;
        
        final Set<String> treeDownloadNames = new TreeSet<>();
        final Set<String> queryDownloadNames = new TreeSet<>();
        
        long treeSearchTimeNs;
        long regionQueryTimeNs;
        int inspectedEntriesCount;

        IntersectionStat(RealMapObject targetObject) {
            this.targetObject = targetObject;
        }

        @Override
        public int compareTo(IntersectionStat o) {
            return Integer.compare(o.treeDownloadNames.size(), this.treeDownloadNames.size());
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
        tree.build();
        long buildTimeMs = (System.nanoTime() - buildStart) / 1_000_000;
        System.out.printf("QuadTree build completed in %d ms.\n\n", buildTimeMs);

        if (sampleObjects.isEmpty()) {
            System.err.println("No map objects loaded!");
            return;
        }

        System.out.printf("=== 2. Benchmarking & Comparing vs OsmandRegions.query() [%d Objects] ===\n", sampleObjects.size());

        // Warmup JIT
        for (int i = 0; i < Math.min(100, sampleObjects.size()); i++) {
            RealMapObject o = sampleObjects.get(i);
            HashSkipTileQuadTree.SkipStats dummyStats = new HashSkipTileQuadTree.SkipStats(tree);
            tree.get(o.bbox, dummyStats);
            or.query(o.bbox[0], o.bbox[2], o.bbox[1], o.bbox[3]);
        }

        List<IntersectionStat> benchmarkResults = new ArrayList<>(sampleObjects.size());

        long treeTotalTimeNs = 0;
        long treeMaxTimeNs = 0;
        String treeMaxObject = "";

        long regionTotalTimeNs = 0;
        long regionMaxTimeNs = 0;
        String regionMaxObject = "";

        long totalInspectedEntries = 0;
        int maxInspectedEntries = 0;
        String maxInspectedObject = "";

        int totalMismatchesCount = 0;

        for (RealMapObject targetObj : sampleObjects) {
            IntersectionStat stat = new IntersectionStat(targetObj);
            int[] bbox = targetObj.bbox;

            // 1. HashSkipTileQuadTree Search
            HashSkipTileQuadTree.SkipStats stats = new HashSkipTileQuadTree.SkipStats(tree);
            long treeStartNs = System.nanoTime();
            List<TileEntry<RealMapObject>> treeResults = tree.get(bbox, stats);
            long treeElapsedNs = System.nanoTime() - treeStartNs;

            stat.treeSearchTimeNs = treeElapsedNs;
            stat.inspectedEntriesCount = stats.inspectedEntries.size();

            for (TileEntry<RealMapObject> entry : treeResults) {
                BinaryMapDataObject bo = entry.obj.dataObject;
                String dw = or.getDownloadName(bo);
                if (!Algorithms.isEmpty(dw) && or.isDownloadOfType(bo, OsmandRegions.MAP_TYPE)) {
                    stat.treeDownloadNames.add(dw);
                }
            }

            // 2. OsmandRegions Standard Query Search (lx, rx, ty, by)
            long regionStartNs = System.nanoTime();
            List<BinaryMapDataObject> queryResults = or.query(bbox[0], bbox[2], bbox[1], bbox[3]);
            long regionElapsedNs = System.nanoTime() - regionStartNs;

            stat.regionQueryTimeNs = regionElapsedNs;

            if (queryResults != null) {
                for (BinaryMapDataObject bo : queryResults) {
                    String dw = or.getDownloadName(bo);
                    if (!Algorithms.isEmpty(dw) && or.isDownloadOfType(bo, OsmandRegions.MAP_TYPE)) {
                        stat.queryDownloadNames.add(dw);
                    }
                }
            }

            // 3. Validation & Differential Check
            if (!stat.treeDownloadNames.equals(stat.queryDownloadNames)) {
                totalMismatchesCount++;
                Set<String> missingInTree = new TreeSet<>(stat.queryDownloadNames);
                missingInTree.removeAll(stat.treeDownloadNames);

                Set<String> extraInTree = new TreeSet<>(stat.treeDownloadNames);
                extraInTree.removeAll(stat.queryDownloadNames);

                System.err.printf(" MISMATCH for target region '%s' all %d (dwName: %s, ID: #%d):\n", 
                        targetObj.regionName, stat.treeDownloadNames.size(), targetObj.downloadName, targetObj.id);
                if (!missingInTree.isEmpty()) {
                    System.err.println("   Missing in Tree: " + missingInTree);
                }
                if (!extraInTree.isEmpty()) {
                    System.err.println("   Extra in Tree:   " + extraInTree);
                }
            }

            benchmarkResults.add(stat);

            // Metrics - QuadTree
            treeTotalTimeNs += treeElapsedNs;
            if (treeElapsedNs > treeMaxTimeNs) {
                treeMaxTimeNs = treeElapsedNs;
                treeMaxObject = targetObj.regionName;
            }

            // Metrics - OsmandRegions
            regionTotalTimeNs += regionElapsedNs;
            if (regionElapsedNs > regionMaxTimeNs) {
                regionMaxTimeNs = regionElapsedNs;
                regionMaxObject = targetObj.regionName;
            }

            // Metrics - Inspect
            totalInspectedEntries += stat.inspectedEntriesCount;
            if (stat.inspectedEntriesCount > maxInspectedEntries) {
                maxInspectedEntries = stat.inspectedEntriesCount;
                maxInspectedObject = targetObj.regionName;
            }
        }

        // Print benchmark statistics
        double treeTotalMs = treeTotalTimeNs / 1e6;
        double treeAvgMs = (treeTotalTimeNs / (double) sampleObjects.size()) / 1e6;
        double treeMaxMs = treeMaxTimeNs / 1e6;

        double regionTotalMs = regionTotalTimeNs / 1e6;
        double regionAvgMs = (regionTotalTimeNs / (double) sampleObjects.size()) / 1e6;
        double regionMaxMs = regionMaxTimeNs / 1e6;

        double avgInspected = totalInspectedEntries / (double) sampleObjects.size();

        System.out.println("=========================================================================");
        System.out.println("                         BENCHMARK COMPARISON                            ");
        System.out.println("=========================================================================");
        System.out.printf("Total Objects Evaluated : %d\n", sampleObjects.size());
        System.out.printf("Data Parity Status      : %s\n", 
                totalMismatchesCount == 0 ? "✅ PERFECT MATCH (0 Mismatches)" : "❌ MISMATCHES DETECTED: " + totalMismatchesCount);
        System.out.println("-------------------------------------------------------------------------");
        System.out.println(" 🚀 HashSkipTileQuadTree:");
        System.out.printf("    Total Time          : %.2f ms\n", treeTotalMs);
        System.out.printf("    Average Time        : %.4f ms (%.2f µs)\n", treeAvgMs, treeAvgMs * 1000);
        System.out.printf("    Max Time            : %.2f ms ('%s')\n", treeMaxMs, treeMaxObject);
        System.out.printf("    Avg Inspected       : %.2f\n", avgInspected);
        System.out.printf("    Max Inspected       : %d ('%s')\n", maxInspectedEntries, maxInspectedObject);
        System.out.println("-------------------------------------------------------------------------");
        System.out.println(" 🐢 OsmandRegions.query():");
        System.out.printf("    Total Time          : %.2f ms\n", regionTotalMs);
        System.out.printf("    Average Time        : %.4f ms (%.2f µs)\n", regionAvgMs, regionAvgMs * 1000);
        System.out.printf("    Max Time            : %.2f ms ('%s')\n", regionMaxMs, regionMaxObject);
        System.out.println("-------------------------------------------------------------------------");
        System.out.printf(" ⚡ Speedup Factor      : %.2fx faster!\n", regionTotalTimeNs / (double) treeTotalTimeNs);
        System.out.println("=========================================================================\n");

        Collections.sort(benchmarkResults);

        System.out.println("=== TOP 100 REGIONS BY INTERSECTED DOWNLOAD NAMES ===");
        int limit = Math.min(100, benchmarkResults.size());
        for (int i = 0; i < limit; i++) {
            IntersectionStat stat = benchmarkResults.get(i);
            List<String> dwNamesList = new ArrayList<>(stat.treeDownloadNames);
            String sampleIntersections = String.join(", ", dwNamesList.subList(0, Math.min(5, dwNamesList.size())));
            if (dwNamesList.size() > 5) {
                sampleIntersections += ", ...";
            }

            System.out.printf("%3d. %-35s | Regions: %3d | QuadTree: %6.2f µs | OsmAnd: %6.2f µs | Inspected: %3d | Intersects: [%s]\n",
                    i + 1,
                    stat.targetObject.regionName,
                    stat.treeDownloadNames.size(),
                    stat.treeSearchTimeNs / 1000.0,
                    stat.regionQueryTimeNs / 1000.0,
                    stat.inspectedEntriesCount,
                    sampleIntersections
            );
        }
    }

    private static void prepareData(HashSkipTileQuadTree<RealMapObject> tree,
                                    OsmandRegions or,
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