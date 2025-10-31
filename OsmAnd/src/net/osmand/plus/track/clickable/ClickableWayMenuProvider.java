package net.osmand.plus.track.clickable;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.CallbackWithObject;
import net.osmand.binary.HeightDataLoader.CancellableCallback;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.controllers.NetworkRouteDrawable;
import net.osmand.plus.views.layers.ContextMenuLayer;
import net.osmand.plus.views.layers.MapSelectionResult;
import net.osmand.plus.views.layers.MapSelectionRules;
import net.osmand.router.network.NetworkRouteSelector;
import net.osmand.shared.gpx.primitives.RouteActivity;
import net.osmand.shared.gpx.primitives.WptPt;

import java.util.Map;
import java.util.stream.Collectors;

public class ClickableWayMenuProvider implements ContextMenuLayer.IContextMenuProvider {
    private final OsmandApplication app;
    private final CancellableCallback<ClickableWay> readHeightData;
    private final CallbackWithObject<ClickableWay> openAsGpxFile;

    public ClickableWayMenuProvider(@NonNull OsmandApplication app,
                                    @NonNull CancellableCallback<ClickableWay> readHeightData,
                                    @NonNull CallbackWithObject<ClickableWay> openAsGpxFile) {
        this.app = app;
        this.readHeightData = readHeightData;
        this.openAsGpxFile = openAsGpxFile;
        // could be derived from OsmandMapLayer(ctx) in case of necessity
    }

    @Override
    public boolean showMenuAction(@Nullable Object object) {
        if (object instanceof ClickableWay that) {
            MapActivity mapActivity = app.getOsmandMap().getMapView().getMapActivity();
            if (mapActivity != null) {
                OsmAndTaskManager.executeTask(new ClickableWayAsyncTask(mapActivity, that, readHeightData, openAsGpxFile));
                return true;
            }
        }
        return false;
    }

    @Override
    public LatLon getObjectLocation(@NonNull Object o) {
        WptPt wpt = ((ClickableWay) o).getSelectedGpxPoint().getSelectedPoint();
        return new LatLon(wpt.getLatitude(), wpt.getLongitude());
    }

    @NonNull
    @Override
    public PointDescription getObjectName(@NonNull Object o) {
        ClickableWay way = (ClickableWay) o;
        String name = way.getWayName();
        RouteActivity activity = getRouteActivity(way);
        if (activity != null) {
            String activityTranslated = app.getPoiTypes().getPoiTranslator()
                    .getTranslation(Amenity.ROUTE_ACTIVITY_TYPE + "_" + activity.getId());
            PointDescription details = new PointDescription(PointDescription.POINT_TYPE_GPX, activityTranslated, name);

            Map<String, String> shieldTags = way.getGpxTags().entrySet().stream()
                    .filter(e -> NetworkRouteSelector.RouteKey.SHIELD_TO_OSMC.containsKey(e.getKey()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            shieldTags.putIfAbsent("shield_fg", activity.getIconName());
            Drawable iconDrawable = NetworkRouteDrawable.getIconByShieldTags(shieldTags, app);
            if (iconDrawable != null) {
                details.setIconDrawable(iconDrawable);
            }
            return details;
        } else {
            return new PointDescription(PointDescription.POINT_TYPE_GPX, name);
        }
    }

    @Nullable
    private RouteActivity getRouteActivity(ClickableWay way) {
        String activityType = way.getActivityType();
        return activityType != null ? app.getRouteActivityHelper().findRouteActivity(activityType) : null;
    }

    @Override
    public void collectObjectsFromPoint(@NonNull MapSelectionResult result, @NonNull MapSelectionRules rules) {
    }
}
