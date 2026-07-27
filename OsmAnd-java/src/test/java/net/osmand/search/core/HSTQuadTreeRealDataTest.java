package net.osmand.search.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import net.osmand.binary.BinaryMapDataObject;
import net.osmand.map.OsmandRegions;
import net.osmand.search.core.HashSkipTileQuadTree.TileEntry;

public class HSTQuadTreeRealDataTest {

    private static final int RANDOM_SEED = 42;
    private static final int TEST_RUNS = 3;

    static class RealMapObject {
        final long id;
        final String regionName;
        final BinaryMapDataObject dataObject;

        RealMapObject(long id, String regionName, BinaryMapDataObject dataObject) {
            this.id = id;
            this.regionName = regionName;
            this.dataObject = dataObject;
        }

        @Override
        public String toString() {
            return regionName + " (ID: " + id + ")";
        }
    }

    public static void main(String[] args) throws IOException {
        System.out.println("=== 1. Loading Real Map Data via OsmandRegions ===");
        HashSkipTileQuadTree<RealMapObject> tree = new HashSkipTileQuadTree<>();

        OsmandRegions or = new OsmandRegions(null);
        Map<String, LinkedList<BinaryMapDataObject>> obj = or.cacheAllCountries();

        List<RealMapObject> sampleObjects = new ArrayList<>();
        List<int[]> sampleBBoxes = new ArrayList<>();
        Map<Long, RealMapObject> sampleObjectsMap = new HashMap<>();

        prepareData(tree, obj, sampleObjects, sampleBBoxes, sampleObjectsMap);

        System.out.println("Building QuadTree hierarchy...");
        tree.build();
        System.out.println("QuadTree build completed successfully!\n");

        if (sampleObjects.isEmpty()) {
            System.err.println("No map objects loaded!");
            return;
        }

        System.out.println("=== 2. Integration Testing: HashSkipTileQuadTree.get(...) ===");
        long nanoTime = System.nanoTime();
        Random rnd = new Random(RANDOM_SEED);

        for (int run = 1; run <= TEST_RUNS; run++) {
            int targetIndex = rnd.nextInt(sampleObjects.size());
            RealMapObject targetObject = sampleObjects.get(targetIndex);
            int[] targetBBox = sampleBBoxes.get(targetIndex);

            runGetMethodTest(tree, run, targetObject, targetBBox, sampleObjectsMap);
        }
        System.out.printf("%.2f ms\n", (System.nanoTime() - nanoTime)/ 1e6);
        System.out.println("✅ ALL GET() TESTS PASSED SUCCESSFULLY!");
    }

    private static void runGetMethodTest(HashSkipTileQuadTree<RealMapObject> tree,
                                         int runIndex,
                                         RealMapObject targetObject,
                                         int[] targetBBox,
                                         Map<Long, RealMapObject> sampleObjectsMap) {
        
        int[] queryBBox = targetBBox;

        System.out.println("=========================================================================");
        System.out.printf("TEST RUN #%d: Target Region '%s' (ID: %d)\n", runIndex, targetObject.regionName, targetObject.id);
        System.out.printf("QUERY BBOX: minX=%d, minY=%d, maxX=%d, maxY=%d\n", 
                queryBBox[0], queryBBox[1], queryBBox[2], queryBBox[3]);
        System.out.println("=========================================================================");

        HashSkipTileQuadTree.SkipStats stats = new HashSkipTileQuadTree.SkipStats(tree);
        List<TileEntry<RealMapObject>> results = tree.get(queryBBox, stats);

        Set<Long> resultObjIds = new HashSet<>();
        boolean targetFoundInResults = false;

        System.out.println("\n--- 1. RETURNED / INTERSECTED OBJECTS ---");
        for (TileEntry<RealMapObject> entry : results) {
            resultObjIds.add(entry.objId);
            boolean isTarget = (entry.objId == targetObject.id);
            if (isTarget) {
                targetFoundInResults = true;
            }

            System.out.printf("  • Match objId=%d (%s) [Z%d, TileId: %d] %s\n", 
                    entry.objId, entry.obj.regionName, entry.z, entry.tileId, isTarget ? "🎯 [TARGET MATCH]" : "");

            if (!intersectsBBox(entry.bbox31, queryBBox)) {
                throw new AssertionError(String.format(
                    "❌ ERROR: Returned TileEntry (#%d) bounding box does not intersect queryBBox!", entry.objId
                ));
            }
        }

        System.out.println("\n--- 2. INSPECTED OBJECTS WITH NAMES (STATS.INSPECTED_ENTRIES) ---");
        boolean printInsStats = false;
        for (int i = 0; printInsStats && i < stats.inspectedEntries.size(); i++) {
            long inspectedId = stats.inspectedEntries.get(i);
            RealMapObject obj = sampleObjectsMap.get(inspectedId);
            String name = (obj != null) ? obj.regionName : "Unknown";

            boolean isTarget = (inspectedId == targetObject.id);
            System.out.printf("  [%03d] Inspected objId=%d (%s) %s\n", 
                    i + 1, inspectedId, name, isTarget ? "🎯 [TARGET]" : "");
        }

        System.out.println("\n--- 3. SKIPPED TILES DETAILED LIST ---");
        boolean printSkipStats = false;
        for (int i = 0; printSkipStats && i < stats.skipRecords.size(); i++) {
            var rec = stats.skipRecords.get(i);
            System.out.printf("  [%03d] Skipped bucket=%d tile z=%d x=%d y=%d (objects skipped %d)\n", 
                    i + 1, rec.bucketZoom(),  rec.zoom(), rec.x(), rec.y(), rec.skippedElements());
        }

        System.out.println("\n--- 4. DETAILED SKIP STATISTICS ---");
        System.out.printf("Total Inspected Entries : %d\n", stats.inspectedEntries.size());
        System.out.printf("Total Skips Triggered   : %d\n", stats.totalSkipsCount);
        System.out.printf("Total Items Skipped     : %d\n", stats.totalElementsSkipped);

        for (int level = 0; level < stats.skipsPerLevel.length; level++) {
            int zoom = stats.indexedZooms[level];
            System.out.printf("  Level %d (Z%-2d): %d skips, %d items skipped\n", 
                    level, zoom, stats.skipsPerLevel[level], stats.elementsSkippedPerLevel[level]);
        }
        
        if (!targetFoundInResults) {
            throw new AssertionError(String.format(
                "❌ ERROR: Target object ID %d was NOT found in get() results!", targetObject.id
            ));
        }

        System.out.println("\nTarget ID in Results: ✅ YES");
        System.out.println();
    }

    private static void prepareData(HashSkipTileQuadTree<RealMapObject> tree,
                                    Map<String, LinkedList<BinaryMapDataObject>> obj,
                                    List<RealMapObject> sampleObjects,
                                    List<int[]> sampleBBoxes,
                                    Map<Long, RealMapObject> sampleObjectsMap) {
        long objectIdCounter = 0;
        int totalRegions = 0;

        for (Entry<String, LinkedList<BinaryMapDataObject>> e : obj.entrySet()) {
            String regionName = e.getKey();
            LinkedList<BinaryMapDataObject> lst = e.getValue();
            if (lst == null) continue;

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
                RealMapObject realObj = new RealMapObject(currentId, regionName, o);

                sampleObjects.add(realObj);
                sampleBBoxes.add(bbox);
                sampleObjectsMap.put(currentId, realObj);

                tree.addObject(realObj, bbox, currentId);
            }
        }

        System.out.printf("Loaded %d regions with %d total binary map objects.\n", totalRegions, sampleObjects.size());
    }

    private static boolean intersectsBBox(int[] a, int[] b) {
        return a[0] <= b[2] && a[2] >= b[0] && a[1] <= b[3] && a[3] >= b[1];
    }
}