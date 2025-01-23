package net.osmand.plus.track.clickable;

import static net.osmand.IndexConstants.GPX_FILE_EXT;
import static net.osmand.data.MapObject.AMENITY_ID_RIGHT_SHIFT;

// THINK use similar icon="piste_high_difficulty" for no-name pistes
// THINK auto-reverse Way (assume downhill OR detect start by minDist to currentLocation)

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.NativeLibrary.RenderedObject;
import net.osmand.binary.HeightDataLoader;
import net.osmand.binary.HeightDataLoader.InterfaceIsCancelled;
import net.osmand.binary.ObfConstants;
import net.osmand.core.jni.ObfMapObject;
import net.osmand.core.jni.QVectorPointI;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.track.helpers.GpxUiHelper;
import net.osmand.plus.utils.FileUtils;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.ContextMenuLayer;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.GpxTrackAnalysis;
import net.osmand.shared.gpx.GpxUtilities;
import net.osmand.shared.gpx.RouteActivityHelper;
import net.osmand.shared.gpx.primitives.RouteActivity;
import net.osmand.shared.gpx.primitives.Track;
import net.osmand.shared.gpx.primitives.TrkSegment;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gnu.trove.list.array.TIntArrayList;

public class ClickableWayHelper {
    public static final Set<String> CLICKABLE_TAGS =
            Set.of("piste:type", "piste:difficulty", "mtb:scale", "dirtbike:scale");
    public static final Map<String, String> FORBIDDEN_TAGS =
            Map.of("area", "yes", "access", "no");
    public static final Map<String, String> GPX_COLORS = Map.ofEntries(
            Map.entry("0", "brown"),
            Map.entry("1", "green"),
            Map.entry("2", "blue"),
            Map.entry("3", "red"),
            Map.entry("4", "black"),
            Map.entry("5", "black"),
            Map.entry("6", "black"),
            Map.entry("novice", "green"),
            Map.entry("easy", "blue"),
            Map.entry("intermediate", "red"),
            Map.entry("advanced", "black"),
            Map.entry("expert", "black"),
            Map.entry("freeride", "yellow")
            // others are default (red)
    );

    private final OsmandApplication app;
    private final OsmandMapTileView view;
    private final ClickableWayMenuProvider activator;

    public ClickableWayHelper(@NonNull OsmandApplication app, @NonNull OsmandMapTileView view) {
        this.app = app;
        this.view = view;
        this.activator = new ClickableWayMenuProvider(view, this::readHeightData, this::openAsGpxFile);
    }

    @NonNull
    public ContextMenuLayer.IContextMenuProvider getContextMenuProvider() {
        return activator;
    }

    public boolean isClickableWay(@NonNull RenderedObject renderedObject) {
        return renderedObject.getX().size() > 1 && isClickableWayTags(renderedObject.getTags()); // v1
    }

    public boolean isClickableWay(@NonNull ObfMapObject obfMapObject, @NonNull Map<String, String> tags) {
        return obfMapObject.getPoints31().size() > 1 && isClickableWayTags(tags); // v2 with prefetched tags
    }

    @Nullable
    public ClickableWay loadClickableWay(@NonNull LatLon selectedLatLon, @NonNull RenderedObject renderedObject) {
        long osmId = ObfConstants.getOsmId(renderedObject.getId() >> AMENITY_ID_RIGHT_SHIFT);
        Map<String, String> tags = renderedObject.getTags();
        String name = renderedObject.getName();
        TIntArrayList xPoints = renderedObject.getX();
        TIntArrayList yPoints = renderedObject.getY();
        QuadRect bbox = calcSearchQuadRect(xPoints, yPoints);
        return loadClickableWay(selectedLatLon, bbox, xPoints, yPoints, osmId, name, tags);
    }

    @Nullable
    public ClickableWay loadClickableWay(@NonNull LatLon selectedLatLon,
                                         @NonNull ObfMapObject obfMapObject,
                                         @NonNull Map<String, String> tags) {
        long id = obfMapObject.getId().getId().longValue();
        long osmId = ObfConstants.getOsmId(id >> AMENITY_ID_RIGHT_SHIFT);
        String name = obfMapObject.getCaptionInNativeLanguage();
        TIntArrayList xPoints = new TIntArrayList();
        TIntArrayList yPoints = new TIntArrayList();
        QVectorPointI points31 = obfMapObject.getPoints31();
        for (int i = 0; i < points31.size(); i++) {
            xPoints.add(points31.get(i).getX());
            yPoints.add(points31.get(i).getY());
        }
        QuadRect bbox = calcSearchQuadRect(xPoints, yPoints);
        return loadClickableWay(selectedLatLon, bbox, xPoints, yPoints, osmId, name, tags);
    }

    private ClickableWay loadClickableWay(LatLon selectedLatLon, QuadRect bbox,
                                          TIntArrayList xPoints, TIntArrayList yPoints,
                                          long osmId, String name, Map<String, String> tags) {
        GpxFile gpxFile = new GpxFile(Version.getFullVersion(app));

        if (!Algorithms.isEmpty(name)) {
            gpxFile.getMetadata().setName(name);
        } else {
            gpxFile.getMetadata().setName(Long.toString(osmId));
        }

        RouteActivityHelper helper = app.getRouteActivityHelper();
        for (String tag : tags.keySet()) {
            RouteActivity activity = helper.findActivityByTag(tag);
            if (activity != null) {
                String activityType = activity.getId();
                gpxFile.getMetadata().getExtensionsToWrite().put(GpxUtilities.ACTIVITY_TYPE, activityType);
                break;
            }
        }

        gpxFile.getExtensionsToWrite().putAll(tags);

        TrkSegment trkSegment = new TrkSegment();
        for (int i = 0; i < Math.min(xPoints.size(), yPoints.size()); i++) {
            WptPt wpt = new WptPt();
            wpt.setLat(MapUtils.get31LatitudeY(yPoints.get(i)));
            wpt.setLon(MapUtils.get31LongitudeX(xPoints.get(i)));
            trkSegment.getPoints().add(wpt);
        }

        Track track = new Track();
        track.getSegments().add(trkSegment);
        gpxFile.setTracks(List.of(track)); // immutable

        String color = getGpxColorByTags(tags);
        if (color != null) {
            gpxFile.setColor(color);
        }

        return new ClickableWay(gpxFile, osmId, name, selectedLatLon, bbox);
    }

    @Nullable
    private String getGpxColorByTags(Map<String, String> tags) {
        for (String t : CLICKABLE_TAGS) {
            String val = tags.get(t);
            if (val != null) {
                for (Map.Entry<String, String> matchColor : GPX_COLORS.entrySet()) {
                    if (val.contains(matchColor.getKey())) {
                        return matchColor.getValue();
                    }
                }
            }
        }
        return null;
    }

    private QuadRect calcSearchQuadRect(TIntArrayList x, TIntArrayList y) {
        QuadRect bbox = new QuadRect();
        for (int i = 0; i < Math.min(x.size(), y.size()); i++) {
            bbox.expand(x.get(i), y.get(i), x.get(i), y.get(i));
        }
        return bbox; // (int)MapUtils.measuredDist31((int)bbox.left, (int)bbox.top, (int)bbox.right, (int)bbox.bottom);
    }

    private boolean isClickableWayTags(@NonNull Map<String, String> tags) {
        for (Map.Entry<String, String> forbidden : FORBIDDEN_TAGS.entrySet()) {
            if (forbidden.getValue().equals(tags.get(forbidden.getKey()))) {
                return false;
            }
        }
        for (String key : tags.keySet()) {
            if (CLICKABLE_TAGS.contains(key)) {
                return true;
            }
        }
        return false;
    }

    private boolean readHeightData(@Nullable ClickableWay clickableWay, @Nullable InterfaceIsCancelled canceller) {
        if (clickableWay != null) {
            HeightDataLoader loader = new HeightDataLoader(app.getResourceManager().getReverseGeocodingMapFiles());
            List<WptPt> waypoints =
                    loader.loadHeightDataAsWaypoints(clickableWay.getOsmId(), clickableWay.getBbox(), canceller);
            if ((canceller == null || !canceller.isCancelled())
                    && !Algorithms.isEmpty(waypoints)
                    && !Algorithms.isEmpty(clickableWay.getGpxFile().getTracks())
                    && !Algorithms.isEmpty(clickableWay.getGpxFile().getTracks().get(0).getSegments())) {
                clickableWay.getGpxFile().getTracks().get(0).getSegments().get(0).setPoints(waypoints);
                return true;
            }
        }
        return false;
    }

    private boolean openAsGpxFile(@Nullable ClickableWay clickableWay) {
        MapActivity mapActivity = view.getMapActivity();
        if (clickableWay != null && mapActivity != null) {
            GpxFile gpxFile = clickableWay.getGpxFile();
            GpxTrackAnalysis analysis = gpxFile.getAnalysis(0);
            String safeFileName = clickableWay.getGpxFileName() + GPX_FILE_EXT;
            File file = new File(FileUtils.getTempDir(app), safeFileName);
            WptPt selectedPoint = clickableWay.getSelectedGpxPoint().getSelectedPoint();
            GpxUiHelper.saveAndOpenGpx(mapActivity, file, gpxFile, selectedPoint, analysis, null, true);
            return true;
        }
        return false;
    }
}
