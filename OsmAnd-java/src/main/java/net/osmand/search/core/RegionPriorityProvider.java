package net.osmand.search.core;

import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.util.MapUtils;

import java.util.List;
import java.util.Map;

import java.util.*;


public class RegionPriorityProvider {

    private final int BBOX_STEP = 50000; // 50 km
    private final int BBOX_MAX = BBOX_STEP * 20; // 1000 km
    private final Map<Integer, List<BinaryMapIndexReader>> priorityMap;
    private LinkedHashMap<BinaryMapIndexReader, Integer> regionsPriority;
    private LatLon searchLocation;

    private static volatile RegionPriorityProvider instance;
    private int lastIndexesCount = -1;
    private static final double LOCATION_SHIFT_THRESHOLD_METERS = 30000; // 30 km

    private RegionPriorityProvider() {
        this.priorityMap = new TreeMap<>();
    }

    public static RegionPriorityProvider getInstance(SearchPhrase phrase) {
        if (instance == null) {
            synchronized (RegionPriorityProvider.class) {
                if (instance == null) {
                    instance = new RegionPriorityProvider();
                }
            }
        }
        instance.checkAndUpdate(phrase);
        return instance;
    }

    private synchronized void checkAndUpdate(SearchPhrase phrase) {
        if (phrase == null || phrase.getSettings() == null) {
            return;
        }

        LatLon newLocation = phrase.getSettings().getOriginalLocation();
        int cnt = phrase.getOfflineIndexes().size();
        if (shouldReinitialize(newLocation, cnt)) {
            this.searchLocation = newLocation == null ? this.searchLocation : newLocation;
            this.lastIndexesCount = cnt;
            this.priorityMap.clear();
            this.regionsPriority = null;
            initPriorityMap(phrase);
        }
    }

    private boolean shouldReinitialize(LatLon newLocation, int cnt) {
        if (this.searchLocation == null || this.lastIndexesCount != cnt) {
            return true;
        }

        if (newLocation != null) {
            double distance = MapUtils.getDistance(this.searchLocation, newLocation);
            return distance >= LOCATION_SHIFT_THRESHOLD_METERS;
        }
        return false;
    }

    public Collection<BinaryMapIndexReader> getOfflineIndexes(SearchPhrase phrase) {
        checkAndUpdate(phrase);
        initRegionsPriority();
        if (regionsPriority == null) {
            return Collections.emptyList();
        }
        phrase.getOfflineIndexes();
        return regionsPriority.keySet();
    }

    public List<BinaryMapIndexReader> getOfflineIndexes(int minRadius, int maxRadius, SearchPhrase phrase) {
        checkAndUpdate(phrase);
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
    
    public int getRegionWeight(BinaryMapIndexReader reader) {
        if (reader == null || priorityMap.isEmpty()) {
            return 0;
        }
        initRegionsPriority();
        if (regionsPriority == null) {
            return 0;
        }
        Integer priority = regionsPriority.get(reader);
        if (priority == null) {
            return 0;
        }
        return priority;
    }

    private void initRegionsPriority() {
        if (regionsPriority != null) {
            return;
        }
        regionsPriority = new LinkedHashMap<>();
        for (Map.Entry<Integer, List<BinaryMapIndexReader>> entry : priorityMap.entrySet()) {
            int priority = entry.getKey();
            for (BinaryMapIndexReader reader : entry.getValue()) {
                if (!regionsPriority.containsKey(reader)) {
                    regionsPriority.put(reader, priority);
                }
            }
        }
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
            QuadRect rect = MapUtils.calculate31BboxUsingRhumb(i * BBOX_STEP + 50, searchLocation);
            if (region.containsPoiData((int) rect.left, (int) rect.top, (int) rect.right, (int) rect.bottom)) {
                return i;
            }
        }
        return BBOX_MAX / BBOX_STEP + 1;
    }
}
