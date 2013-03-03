package net.osmand.access;

import net.osmand.access.AccessibilityActionsProvider;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.OsmandMapTileView;
import android.os.Build;

// Accessibility actions for map view.
public class MapAccessibilityActions implements AccessibilityActionsProvider {

    private final MapActivity activity;

    public MapAccessibilityActions(final MapActivity activity) {
        this.activity = activity;
    }

    @Override
    public boolean onClick() {
        if ((Build.VERSION.SDK_INT >= 14) && activity.getMyApplication().getInternalAPI().accessibilityEnabled()) {
            activity.emitNavigationHint();
            return true;
        }
        return false;
    }

    @Override
    public boolean onLongClick() {
        if ((Build.VERSION.SDK_INT >= 14) && activity.getMyApplication().getInternalAPI().accessibilityEnabled()) {
            final OsmandMapTileView mapView = activity.getMapView();
            activity.getMapActions().contextMenuPoint(mapView.getLatitude(), mapView.getLongitude());
            return true;
        }
        return false;
    }

}
