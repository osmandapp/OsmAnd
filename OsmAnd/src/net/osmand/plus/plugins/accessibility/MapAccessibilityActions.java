package net.osmand.plus.plugins.accessibility;

import android.graphics.PointF;

import net.osmand.data.RotatedTileBox;
import net.osmand.plus.activities.MapActivity;

// Accessibility actions for map view.
public class MapAccessibilityActions implements AccessibilityActionsProvider {

    private final MapActivity activity;

    public MapAccessibilityActions(MapActivity activity) {
        this.activity = activity;
    }

    @Override
    public boolean onClick(PointF point, RotatedTileBox tileBox) {
        if (activity.getMyApplication().accessibilityEnabled()) {
            activity.getMyApplication().getLocationProvider().emitNavigationHint();
            return true;
        }
        return false;
    }

    @Override
    public boolean onLongClick(PointF point, RotatedTileBox tileBox) {
        if (activity.getMyApplication().accessibilityEnabled() && activity.getMapViewTrackingUtilities().isMapLinkedToLocation()) {
            PointF centerPixel = new PointF(tileBox.getCenterPixelX(), tileBox.getCenterPixelY());
            activity.getMapLayers().getContextMenuLayer().showContextMenu(centerPixel, tileBox, true);
            return true;
        }
        return false;
    }

}
