package net.osmand.binary;

import static net.osmand.router.RouteResultPreparation.SHIFT_ID;

import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteSubregion;
import net.osmand.data.QuadRect;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HeightDataLoader {
    public static final int ZOOM_TO_LOAD_TILES = 15;
    public static final int ZOOM_TO_LOAD_TILES_SHIFT_L = ZOOM_TO_LOAD_TILES + 1;
    public static final int ZOOM_TO_LOAD_TILES_SHIFT_R = 31 - ZOOM_TO_LOAD_TILES;

    public interface Cancellable {
        boolean isCancelled();
    }

    public interface CancellableCallback<T> {
        boolean callback(T object, Cancellable canceller);
    }

    private final static Log log = PlatformUtil.getLog(HeightDataLoader.class);
    private final Map<RouteSubregion, List<RouteDataObject>> loadedSubregions = new HashMap<>();
    private final Map<BinaryMapIndexReader, List<RouteSubregion>> readers = new LinkedHashMap<>();

    public HeightDataLoader(BinaryMapIndexReader[] readers) {
        for (BinaryMapIndexReader r : readers) {
            List<RouteSubregion> subregions = new ArrayList<>();
            for (BinaryMapRouteReaderAdapter.RouteRegion rInd : r.getRoutingIndexes()) {
                List<RouteSubregion> subregs = rInd.getSubregions();
                // create a copy to avoid leaks to the original structure
                for (RouteSubregion rs : subregs) {
                    subregions.add(new RouteSubregion(rs));
                }
            }
            this.readers.put(r, subregions);
        }
    }

    public List<WptPt> loadHeightDataAsWaypoints(long osmId, QuadRect bbox31, Cancellable canceller) {
        Map<Long, RouteDataObject> results = new HashMap<>();
        ResultMatcher<RouteDataObject> matcher = new ResultMatcher<>() {
            @Override
            public boolean publish(RouteDataObject routeDataObject) {
                return routeDataObject != null && routeDataObject.getId() >> SHIFT_ID == osmId;
            }

            @Override
            public boolean isCancelled() {
                return results.containsKey(osmId) || (canceller != null && canceller.isCancelled());
            }
        };

        try {
            loadRouteDataObjects(bbox31, results, matcher);
        } catch (IOException e) {
            log.error(e);
        }

        RouteDataObject found = results.get(osmId);
        if (found != null && found.getPointsLength() > 0) {
            List<WptPt> waypoints = new ArrayList<>();
            float[] heightArray = found.calculateHeightArray();
            for (int i = 0; i < found.getPointsLength(); i++) {
                WptPt point = new WptPt();
                point.setLat(MapUtils.get31LatitudeY(found.getPoint31YTile(i)));
                point.setLon(MapUtils.get31LongitudeX(found.getPoint31XTile(i)));
                if (heightArray != null && heightArray.length > i * 2 + 1) {
                    point.setEle(heightArray[i * 2 + 1]);
                }
                waypoints.add(point);
            }
            return waypoints;
        }

        return null;
    }

    private boolean loadRouteDataObjects(QuadRect bbox31,
                                         Map<Long, RouteDataObject> results,
                                         ResultMatcher<RouteDataObject> matcher) throws IOException {
        int loaded = 0;
        int left = (int) bbox31.left >> ZOOM_TO_LOAD_TILES_SHIFT_R;
        int top = (int) bbox31.top >> ZOOM_TO_LOAD_TILES_SHIFT_R;
        int right = (int) bbox31.right >> ZOOM_TO_LOAD_TILES_SHIFT_R;
        int bottom = (int) bbox31.bottom >> ZOOM_TO_LOAD_TILES_SHIFT_R;
        for (int x = left; x <= right; x++) {
            for (int y = top; y <= bottom; y++) {
                if (matcher != null && matcher.isCancelled()) {
                    return loaded > 0;
                }
                loaded += loadRouteDataObjects(x, y, results, matcher);
            }
        }
        return loaded > 0;
    }

    private int loadRouteDataObjects(int x, int y,
                                     Map<Long, RouteDataObject> results,
                                     ResultMatcher<RouteDataObject> matcher) throws IOException {
        int loaded = 0;
        HashSet<Long> deletedIds = new HashSet<>();
        Map<Long, BinaryMapRouteReaderAdapter.RouteRegion> usedIds = new HashMap<>();
        BinaryMapIndexReader.SearchRequest<RouteDataObject> req = BinaryMapIndexReader.buildSearchRouteRequest(
                x << ZOOM_TO_LOAD_TILES_SHIFT_L, (x + 1) << ZOOM_TO_LOAD_TILES_SHIFT_L,
                y << ZOOM_TO_LOAD_TILES_SHIFT_L, (y + 1) << ZOOM_TO_LOAD_TILES_SHIFT_L, null);
        for (Map.Entry<BinaryMapIndexReader, List<RouteSubregion>> readerSubregions : readers.entrySet()) {
            req.clearSearchResults();
            BinaryMapIndexReader reader = readerSubregions.getKey();
            synchronized (reader) {
                List<RouteSubregion> routeSubregions = readerSubregions.getValue();
                List<RouteSubregion> subregions = reader.searchRouteIndexTree(req, routeSubregions);
                for (RouteSubregion sub : subregions) {
                    List<RouteDataObject> objects = loadedSubregions.get(sub);
                    if (objects == null) {
                        objects = reader.loadRouteIndexData(sub);
                        loadedSubregions.put(sub, objects);
                    }
                    for (RouteDataObject obj : objects) {
                        if (matcher != null && matcher.isCancelled()) {
                            return loaded;
                        }
                        if (matcher == null || matcher.publish(obj)) {
                            if (deletedIds.contains(obj.id)) {
                                // live-updates, osmand_change=delete
                                continue;
                            }
                            if (obj.isRoadDeleted()) {
                                deletedIds.add(obj.id);
                                continue;
                            }
                            if (usedIds.containsKey(obj.id) && usedIds.get(obj.id) != obj.region) {
                                // live-update, changed tags
                                continue;
                            }
                            loaded += (results.put(obj.getId() >> SHIFT_ID, obj) == null) ? 1 : 0;
                            usedIds.put(obj.id, obj.region);
                        }
                    }
                }
            }
        }
        return loaded;
    }
}
