package net.osmand.search.core;

import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import java.util.List;
import java.util.Map;

import java.util.*;


public class RegionPriorityProvider {

    private final int BBOX_STEP = 50000; // 50 km
    private final int BBOX_MAX = BBOX_STEP * 20; // 1000 km
    private final Map<Integer, List<BinaryMapIndexReader>> priorityMap;
    private LatLon searchLocation;

    public RegionPriorityProvider(SearchPhrase phrase) {
        this.priorityMap = new TreeMap<>();
        if (phrase != null && phrase.getSettings() != null) {
            this.searchLocation = phrase.getSettings().getOriginalLocation();
            initPriorityMap(phrase);
        }
    }

    public List<BinaryMapIndexReader> getOfflineIndexes(int minRadius, int maxRadius) {
        List<BinaryMapIndexReader> result = new ArrayList<>();

        int minPriority = (int) Math.floor((double) minRadius / BBOX_STEP);
        int maxPriority = (int) Math.ceil((double) maxRadius / BBOX_STEP);
        
        for (Map.Entry<Integer, List<BinaryMapIndexReader>> entry : priorityMap.entrySet()) {
            int p = entry.getKey();
            if (p >= minPriority && p <= maxPriority) {
                for (BinaryMapIndexReader r : entry.getValue()) {
                    if (!result.contains(r)) {
                        result.add(r);
                    }
                }
            }
        }
        return result;
    }

    private void initPriorityMap(SearchPhrase phrase) {
        if (searchLocation == null) {
            return;
        }

        if (phrase != null && phrase.getOfflineIndexes() != null) {
            for (BinaryMapIndexReader r : phrase.getOfflineIndexes()) {
                int priority = calculatePriorityValue(r);
                priorityMap.computeIfAbsent(priority, k -> new ArrayList<>()).add(r);
            }
        }
    }



    private int calculatePriorityValue(BinaryMapIndexReader region) {
        for (int i = 0; i * BBOX_STEP <= BBOX_MAX; i++) {
            QuadRect rect = SearchPhrase.calculateBbox(i * BBOX_STEP + 50, searchLocation);
            if (region.containsPoiData((int) rect.left, (int) rect.top, (int) rect.right, (int) rect.bottom)) {
                return i;
            }
        }
        return BBOX_MAX / BBOX_STEP + 1;
    }
}
