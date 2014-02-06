package net.osmand.access;

import net.osmand.data.LatLon;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.ContextMenuLayer;
import net.osmand.plus.views.OsmandMapTileView;
import android.graphics.PointF;
import android.os.Build;

// Accessibility actions for map view.
public class MapAccessibilityActions implements AccessibilityActionsProvider {

    private final MapActivity activity;

    public MapAccessibilityActions(final MapActivity activity) {
        this.activity = activity;
    }

    @Override
    public boolean onClick(PointF point, RotatedTileBox tileBox) {
        if ((Build.VERSION.SDK_INT >= 14) && activity.getMyApplication().accessibilityEnabled()) {
        	// not sure if it is very clear why should I mark destination first when I tap on the object
        	return activity.getMyApplication().getLocationProvider().emitNavigationHint();
        }
        return false;
    }

    @Override
    public boolean onLongClick(PointF point, RotatedTileBox tileBox) {
        if ((Build.VERSION.SDK_INT >= 14) && activity.getMyApplication().accessibilityEnabled()) {
            final OsmandMapTileView mapView = activity.getMapView();
	        final double lat = tileBox.getLatFromPixel((int)point.x, (int) point.y);
	        final double lon = tileBox.getLonFromPixel((int)point.x, (int) point.y);
            ContextMenuLayer cm = activity.getMapLayers().getContextMenuLayer();
            LatLon loc = cm.selectObjectsForContextMenu(tileBox, point);
            if (cm.getSelectedObjectName() != null) {
            	cm.showContextMenuForSelectedObjects(loc);
			} else {
				activity.getMapActions().contextMenuPoint(lat, lon);
			}
            
//            activity.getMapActions().contextMenuPoint(mapView.getLatitude(), mapView.getLongitude());
            return true;
        }
        return false;
    }

}
