package net.osmand.search.core;

import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

public class SearchUnitProvider  {

    private final int BBOX_STEP = 50000; // 50 km
    private final int BBOX_MAX = BBOX_STEP * 20; // 1000 km

    private Map<Integer, SearchUnit> searchUnitMap;
    private Map<BinaryMapIndexReader, Integer> priorityMap;

    private final LatLon searchLocation;
    public SearchUnitProvider(SearchPhrase phrase) {
        if (phrase == null) {
            searchLocation = null;
        } else {
            searchLocation = phrase.getSettings().getOriginalLocation();
        }
        priorityMap = new HashMap<>();
        searchUnitMap = new TreeMap<>();
    }

    public void sortOfflineIndexes(List<BinaryMapIndexReader> offlineIdexes) {
        if (searchLocation == null || offlineIdexes.size() <= 1) {
            return;
        }
        Map<BinaryMapIndexReader, Integer> distanceMap = new HashMap<>();
        for (BinaryMapIndexReader reader : offlineIdexes) {
            if (reader.getRegionCenter() != null) {
                distanceMap.put(reader, (int) MapUtils.getDistance(searchLocation, reader.getRegionCenter()));
            } else {
                return;
            }
        }
        offlineIdexes.sort((reader1, reader2) -> {
            int dist1 = distanceMap.get(reader1);
            int dist2 = distanceMap.get(reader2);
            return Integer.compare(dist1, dist2);
        });
    }

    public void setSearchResults(BinaryMapIndexReader region, List<SearchResult> results) {
        if (Algorithms.isEmpty(results)) {
            return;
        }
        int priority = calculatePriority(region);
        SearchUnit unit = searchUnitMap.computeIfAbsent(priority, new Function<Integer, SearchUnit>() {
            @Override
            public SearchUnit apply(Integer priority) {
                return new SearchUnit(priority);
            }
        });
        for (SearchResult result : results) {
            if (result.file == region) {
                unit.addSearchResult(result);
            }
        }
        unit.addOfflineIndex(region);
    }

    public void setSearchResult(SearchResult result) {
        int priority = calculatePriority(result.file);
        SearchUnit unit = searchUnitMap.computeIfAbsent(priority, new Function<Integer, SearchUnit>() {
            @Override
            public SearchUnit apply(Integer priority) {
                return new SearchUnit(priority);
            }
        });
        unit.addSearchResult(result);
        if (result.file != null) {
            unit.addOfflineIndex(result.file);
        }
    }

    public Collection<SearchUnit> getSearchUnits() {
        return searchUnitMap.values();
    }

    private boolean isLiveUpdate(BinaryMapIndexReader reader) {
        return reader.getHHRoutingIndexes().isEmpty();
    }

    public int calculatePriority(BinaryMapIndexReader region) {
        if (searchLocation == null || region == null) {
            return -1;
        }
        return priorityMap.computeIfAbsent(region, new Function<BinaryMapIndexReader, Integer>() {
            @Override
            public Integer apply(BinaryMapIndexReader reader) {
                for (int i = 0; i * BBOX_STEP <= BBOX_MAX; i++) {
                    QuadRect rect = SearchPhrase.calculateBbox(i * BBOX_STEP, searchLocation);
                    if (region.containsPoiData((int)rect.left, (int)rect.top, (int)rect.right, (int)rect.bottom)) {
                        return i;
                    }
                }
                return BBOX_MAX / BBOX_STEP + 1;
            }
        });
    }
}
