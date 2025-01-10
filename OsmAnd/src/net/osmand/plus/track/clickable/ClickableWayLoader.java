package net.osmand.plus.track.clickable;

import static net.osmand.data.MapObject.AMENITY_ID_RIGHT_SHIFT;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.NativeLibrary.RenderedObject;
import net.osmand.binary.ObfConstants;
import net.osmand.core.jni.ObfMapObject;
import net.osmand.core.jni.QVectorPointI;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.Version;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.ContextMenuLayer;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.GpxUtilities;
import net.osmand.shared.gpx.RouteActivityHelper;
import net.osmand.shared.gpx.primitives.RouteActivity;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.Map;
import java.util.Set;

import gnu.trove.list.array.TIntArrayList;

public class ClickableWayLoader {
    public static final Set<String> clickableTags = Set.of("piste:type", "mtb:scale", "dirtbike:scale");
    public static final Map<String, String> forbiddenTags = Map.of("area", "yes", "access", "no");

    private final OsmandApplication app;
    private final ClickableWayActivator activator;

    public ClickableWayLoader(@NonNull OsmandApplication app, @NonNull OsmandMapTileView view) {
        this.app = app;
        this.activator = new ClickableWayActivator(app, view);
    }

    @NonNull
    public ContextMenuLayer.IContextMenuProvider getContextMenuProvider() {
        return activator;
    }

    public boolean isClickableWayV1(@NonNull RenderedObject renderedObject) {
        return renderedObject.getX().size() > 1 && isClickableWayTags(renderedObject.getTags()); // v1
    }

    public boolean isClickableWayV2(@NonNull ObfMapObject obfMapObject, @NonNull Map<String, String> tags) {
        return obfMapObject.getPoints31().size() > 1 && isClickableWayTags(tags); // v2 with prefetched tags
    }

    @Nullable
    public ClickableWay searchClickableWayV1(@NonNull LatLon searchLatLon, @NonNull RenderedObject renderedObject) {
        long osmId = ObfConstants.getOsmId(renderedObject.getId() >> AMENITY_ID_RIGHT_SHIFT);
        Map<String, String> tags = renderedObject.getTags();
        String name = renderedObject.getName();
        TIntArrayList xPoints = renderedObject.getX();
        TIntArrayList yPoints = renderedObject.getY();
        int searchRadius = calcSearchRadius(xPoints, yPoints);
        return searchClickableWay(searchLatLon, searchRadius, xPoints, yPoints, osmId, name, tags);
    }

    @Nullable
    public ClickableWay searchClickableWayV2(@NonNull LatLon searchLatLon, @NonNull ObfMapObject obfMapObject,
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
        int searchRadius = calcSearchRadius(xPoints, yPoints);
        return searchClickableWay(searchLatLon, searchRadius, xPoints, yPoints, osmId, name, tags);
    }

    private ClickableWay searchClickableWay(LatLon searchLatLon, int searchRadius,
                                            TIntArrayList xPoints, TIntArrayList yPoints,
                                            long osmId, String name, Map<String, String> tags) {
        GpxFile gpxFile = new GpxFile(Version.getFullVersion(app));

        if (!Algorithms.isEmpty(name)) {
            gpxFile.getMetadata().setName(name);
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

        gpxFile.getExtensionsToWrite().putAll(tags); // TODO check prefix, check /:/ in tag

        // TODO fill trkpt from xPoints/xPoints
        // TODO fetch elevation data from routing-section

        // TODO cache <id, GpxFile>
        // TODO calc distance stats, elevation stats, etc (automatically if data exists)
        // THINK auto-reverse Way (assume downhill OR detect start by minDist to currentLocation)

        return new ClickableWay(gpxFile, osmId, name, searchLatLon);
    }

    private int calcSearchRadius(TIntArrayList x, TIntArrayList y) {
        QuadRect bbox = new QuadRect();
        for (int i = 0; i < Math.min(x.size(), y.size()); i++) {
            bbox.expand(x.get(i), y.get(i), x.get(i), y.get(i));
        }
        return (int)MapUtils.measuredDist31((int)bbox.left, (int)bbox.top, (int)bbox.right, (int)bbox.bottom);
    }

    private boolean isClickableWayTags(@NonNull Map<String, String> tags) {
        for (Map.Entry<String, String> forbidden : forbiddenTags.entrySet()) {
            if (forbidden.getValue().equals(tags.get(forbidden.getKey()))) {
                return false;
            }
        }
        for (String key : tags.keySet()) {
            if (clickableTags.contains(key)) {
                return true;
            }
        }
        return false;
    }
}
