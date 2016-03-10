package net.osmand.access;

import android.graphics.PointF;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadPoint;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.R;
import net.osmand.plus.views.ContextMenuLayer.IContextMenuProvider;
import net.osmand.plus.views.OsmandMapTileView;

import java.util.List;
import java.util.Map;

// Provide touch exploration mode for map view
// when scrolling it by gestures is disabled.
//
public class MapExplorer extends SimpleOnGestureListener implements IContextMenuProvider {

    private static final float VICINITY_RADIUS = 15;

    private OsmandMapTileView mapView;
    private SimpleOnGestureListener fallback;
    private Map<Object, IContextMenuProvider> selectedObjects = null;


    // OnGestureListener specified as a second argument
    // will be used when scrolling map by gestures
    // is enabled.
    public MapExplorer(OsmandMapTileView mapView, SimpleOnGestureListener fallback) {
        this.mapView = mapView;
        this.fallback = fallback;
    }


    // OnGestureListener interface implementation.

    @Override
    public boolean onDown(MotionEvent e) {
        return fallback.onDown(e);
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return fallback.onFling(e1, e2, velocityX / 3, velocityY / 3);
    }

    @Override
    public void onLongPress(MotionEvent e) {
        fallback.onLongPress(e);
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return fallback.onScroll(e1, e2, distanceX, distanceY);
    }

    @Override
    public void onShowPress(MotionEvent e) {
        fallback.onShowPress(e);
    }

	@Override
	public boolean onSingleTapConfirmed(MotionEvent e) {
		return fallback.onSingleTapConfirmed(e);
	}

	@Override
	public boolean onDoubleTap(MotionEvent e) {
		return fallback.onDoubleTap(e);
	}

	// IContextMenuProvider interface implementation.

    @Override
    public boolean disableSingleTap() {
        return false;
    }

    @Override
    public boolean disableLongPressOnMap() {
        return false;
    }

    @Override
    public boolean isObjectClickable(Object o) {
        return false;
    }

    @Override
    public void collectObjectsFromPoint(PointF point, RotatedTileBox tileBox, List<Object> objects) {
        int radius = (int)(VICINITY_RADIUS * tileBox.getDensity());
	    final QuadPoint p = tileBox.getCenterPixelPoint();
	    int dx = (int)Math.abs(point.x - p.x);
        int dy = (int)Math.abs(point.y - p.y);
        if ((dx < radius) && (dy < radius))
            objects.add(this);
    }

    @Override
    public LatLon getObjectLocation(Object o) {
	    final RotatedTileBox tb = mapView.getCurrentRotatedTileBox();
	    return tb.getCenterLatLon();
    }

    @Override
    public String getObjectDescription(Object o) {
        return mapView.getContext().getString(R.string.i_am_here);
    }

    @Override
    public PointDescription getObjectName(Object o) {
        return new PointDescription(PointDescription.POINT_TYPE_MARKER, mapView.getContext().getString(R.string.i_am_here));
    }

}
