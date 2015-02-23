package net.osmand.access;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadPoint;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.R;
import net.osmand.plus.views.ContextMenuLayer;
import net.osmand.plus.views.ContextMenuLayer.IContextMenuProvider;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.plus.views.OsmandMapTileView;
import android.graphics.PointF;
import android.os.Build;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;

// Provide touch exploration mode for map view
// when scrolling it by gestures is disabled.
//
public class MapExplorer implements OnGestureListener, IContextMenuProvider {

    private static final float VICINITY_RADIUS = 15;

    private OsmandMapTileView mapView;
    private OnGestureListener fallback;
    private Map<Object, IContextMenuProvider> selectedObjects = null;


    // OnGestureListener specified as a second argument
    // will be used when scrolling map by gestures
    // is enabled.
    public MapExplorer(OsmandMapTileView mapView, OnGestureListener fallback) {
        this.mapView = mapView;
        this.fallback = fallback;
    }


    // Compare two lists by content.
    private boolean different(Object l1, Object l2) {
    	if(l1 == null || l2 == null) {
    		return l1 != l2;
    	}
    	return l1.equals(l2);
    }

    // Find touched objects if any and emit accessible toast message
    // with it's brief description.
    private void describePointedObjects(RotatedTileBox tb,  MotionEvent event) {
        PointF point = new PointF(event.getX(), event.getY());
        List<Object> ns = new ArrayList<Object>();
        Map<Object, IContextMenuProvider> newSelectedObjects = new LinkedHashMap<Object, ContextMenuLayer.IContextMenuProvider>();
		for (OsmandMapLayer layer : mapView.getLayers()) {
			if (layer instanceof IContextMenuProvider) {
				ns.clear();
				((IContextMenuProvider) layer).collectObjectsFromPoint(point, tb , ns);
				for(Object o : ns) {
					newSelectedObjects.put(o, (IContextMenuProvider) layer);
				}
			}
		}
        if (newSelectedObjects.isEmpty()) {
        	ns.clear();
            collectObjectsFromPoint(point, tb, ns);
            for(Object o : ns) {
				newSelectedObjects.put(o, this);
			}
        }
        if (different(newSelectedObjects, selectedObjects)) {
            ContextMenuLayer contextMenuLayer = mapView.getLayerByClass(ContextMenuLayer.class);
            if (contextMenuLayer != null) {
                contextMenuLayer.setSelections(newSelectedObjects);
                if (!ns.isEmpty())
                    mapView.showMessage(mapView.getSettings().USE_SHORT_OBJECT_NAMES.get() ?
                                        contextMenuLayer.getSelectedObjectName() :
                                        contextMenuLayer.getSelectedObjectDescription());
            }
            selectedObjects = newSelectedObjects;
        }
    }


    // OnGestureListener interface implementation.

    @Override
    public boolean onDown(MotionEvent e) {
        if ((Build.VERSION.SDK_INT >= 14) || mapView.getSettings().SCROLL_MAP_BY_GESTURES.get())
            return fallback.onDown(e);
        ContextMenuLayer contextMenuLayer = mapView.getLayerByClass(ContextMenuLayer.class);
        if (contextMenuLayer != null)
            contextMenuLayer.setSelections(null);
        selectedObjects = null;
        describePointedObjects(mapView.getCurrentRotatedTileBox(), e);
        return false;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if ((Build.VERSION.SDK_INT >= 14) || mapView.getSettings().SCROLL_MAP_BY_GESTURES.get())
            return fallback.onFling(e1, e2, velocityX/3, velocityY/3);
        return true;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        fallback.onLongPress(e);
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        if ((Build.VERSION.SDK_INT >= 14) || mapView.getSettings().SCROLL_MAP_BY_GESTURES.get()) {
            return fallback.onScroll(e1, e2, distanceX, distanceY);
        } else {
            describePointedObjects(mapView.getCurrentRotatedTileBox(), e2);
        }
        return true;
    }

    @Override
    public void onShowPress(MotionEvent e) {
        if ((Build.VERSION.SDK_INT >= 14) || mapView.getSettings().SCROLL_MAP_BY_GESTURES.get())
            fallback.onShowPress(e);
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return fallback.onSingleTapUp(e);
    }


    // IContextMenuProvider interface implementation.

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
