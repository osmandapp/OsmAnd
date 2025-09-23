package net.osmand.plus.track.clickable;

import static net.osmand.IndexConstants.GPX_FILE_EXT;
import static net.osmand.data.MapObject.AMENITY_ID_RIGHT_SHIFT;

// THINK use similar icon="piste_high_difficulty" for no-name pistes
// THINK auto-reverse Way (assume downhill OR detect start by minDist to currentLocation)

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.NativeLibrary.RenderedObject;
import net.osmand.binary.HeightDataLoader;
import net.osmand.binary.HeightDataLoader.Cancellable;
import net.osmand.binary.ObfConstants;
import net.osmand.core.jni.ObfMapObject;
import net.osmand.core.jni.QVectorPointI;
import net.osmand.data.Amenity;
import net.osmand.data.BaseDetailsObject;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.track.helpers.GpxUiHelper;
import net.osmand.plus.utils.FileUtils;
import net.osmand.plus.views.layers.ContextMenuLayer;
import net.osmand.search.AmenitySearcher;
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
            Set.of("piste:type", "piste:difficulty", "mtb:scale", "dirtbike:scale",
                    "snowmobile=yes", "snowmobile=designated", "snowmobile=permissive"
            );
    public static final Map<String, String> FORBIDDEN_TAGS =
            Map.of("area", "yes", "access", "no", "aerialway", "*");
    public static final Set<String> REQUIRED_TAGS_ANY =
            Set.of("name", "ref", "piste:name", "mtb:name");
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
    private final ClickableWayMenuProvider activator;

    public ClickableWayHelper(@NonNull OsmandApplication app) {
        this.app = app;
        this.activator = new ClickableWayMenuProvider(app, this::readHeightData, this::openAsGpxFile);
    }

    @NonNull
    public ContextMenuLayer.IContextMenuProvider getContextMenuProvider() {
        return activator;
    }

    public boolean isClickableWay(@NonNull RenderedObject renderedObject) {
        String name = renderedObject.getName();
        return renderedObject.getX().size() > 1 && isClickableWayTags(name, renderedObject.getTags()); // v1
    }

    public boolean isClickableWay(@NonNull ObfMapObject obfMapObject, @NonNull Map<String, String> tags) {
        String name = obfMapObject.getCaptionInNativeLanguage();
        return obfMapObject.getPoints31().size() > 1 && isClickableWayTags(name, tags); // v2 with prefetched tags
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

    public ClickableWay loadClickableWay(@NonNull Amenity amenity) {
        long osmId = amenity.getOsmId();
        String name = amenity.getName();
        TIntArrayList xPoints = amenity.getX();
        TIntArrayList yPoints = amenity.getY();
        LatLon selectedLatLon = amenity.getLocation();
        Map<String, String> tags = amenity.getOsmTags();
        QuadRect bbox = calcSearchQuadRect(xPoints, yPoints);
        return loadClickableWay(selectedLatLon, bbox, xPoints, yPoints, osmId, name, tags);
    }

    public boolean isClickableWayAmenity(Amenity amenity) {
        return isClickableWayTags(amenity.getName(), amenity.getOsmTags());
    }

    private ClickableWay loadClickableWay(LatLon selectedLatLon, QuadRect bbox,
                                          TIntArrayList xPoints, TIntArrayList yPoints,
                                          long osmId, String name, Map<String, String> tags) {
        GpxFile gpxFile = new GpxFile(Version.getFullVersion(app));
        RouteActivityHelper helper = app.getRouteActivityHelper();
        for (String clickableTagValue : CLICKABLE_TAGS) {
            String tag = clickableTagValue.split("=")[0];
            if (tags.containsKey(tag)) {
                RouteActivity activity = helper.findActivityByTag(clickableTagValue);
                if (activity != null) {
                    String activityType = activity.getId();
                    gpxFile.getMetadata().getExtensionsToWrite().put(GpxUtilities.ACTIVITY_TYPE, activityType);
                    break;
                }
            }
        }

        gpxFile.getExtensionsToWrite().putAll(tags);
        gpxFile.getExtensionsToWrite().put("way_id", Long.toString(osmId));

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

    private boolean isClickableWayTags(@Nullable String name, @NonNull Map<String, String> tags) {
        for (Map.Entry<String, String> forbidden : FORBIDDEN_TAGS.entrySet()) {
            if (forbidden.getValue().equals(tags.get(forbidden.getKey()))
                    || "*".equals(forbidden.getValue()) && tags.containsKey(forbidden.getKey())
            ) {
                return false;
            }
        }
        for (String required : REQUIRED_TAGS_ANY) {
            // some objects have name passed from object props but not in the tags
            boolean isRequiredNameFound = "name".equals(required) && !Algorithms.isEmpty(name);
            if (tags.containsKey(required) || isRequiredNameFound) {
                for (String key : tags.keySet()) {
                    if (CLICKABLE_TAGS.contains(key)) {
                        return true;
                    }
                    String value = tags.get(key); // snowmobile=yes, etc
                    if (value != null && CLICKABLE_TAGS.contains(key + "=" + value)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean readHeightData(@Nullable ClickableWay clickableWay, @Nullable Cancellable canceller) {
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
        MapActivity mapActivity = app.getOsmandMap().getMapView().getMapActivity();
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

    public void openClickableWayAmenity(Amenity amenity, boolean adjustMapPosition) {
        AmenitySearcher amenitySearcher = app.getResourceManager().getAmenitySearcher();
        AmenitySearcher.Settings settings = app.getResourceManager().getDefaultAmenitySearchSettings();
        BaseDetailsObject detailedObject = amenitySearcher.searchDetailedObject(amenity, settings);
        if (detailedObject != null) {
            ClickableWay clickableWay = loadClickableWay(detailedObject.getSyntheticAmenity());
            readHeightData(clickableWay, null);
            openAsGpxFile(clickableWay);
        }
    }
}
