package net.osmand.access;

import java.util.ArrayList;
import java.util.List;

import net.osmand.osm.LatLon;
import net.osmand.plus.R;
import net.osmand.plus.views.ContextMenuLayer;
import net.osmand.plus.views.ContextMenuLayer.IContextMenuProvider;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.plus.views.OsmandMapTileView;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.PointF;
import android.util.DisplayMetrics;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.WindowManager;

// Provide touch exploration mode for map view
// when scrolling it by gestures is disabled.
//
public class MapExplorer implements OnGestureListener, IContextMenuProvider {

    private static final float VICINITY_RADIUS = 15;

    private OsmandMapTileView mapView;
    private OnGestureListener fallback;
    private List<Object> selectedObjects = null;
    private IContextMenuProvider contextProvider;
    private final DisplayMetrics dm = new DisplayMetrics();


    // OnGestureListener specified as a second argument
    // will be used when scrolling map by gestures
    // is enabled.
    public MapExplorer(OsmandMapTileView mapView, OnGestureListener fallback) {
        this.mapView = mapView;
        this.fallback = fallback;
        ((WindowManager)(mapView.getContext().getSystemService(Context.WINDOW_SERVICE))).getDefaultDisplay().getMetrics(dm);
    }


    // Compare two lists by content.
    private boolean different(List<Object> l1, List<Object> l2) {
        if ((l1 != null) && !l1.isEmpty()) {
            if ((l2 != null) && !l2.isEmpty()) {
                if (l1.size() != l2.size())
                    return true;
                for (int i = 0; i < l1.size(); i++)
                    if (l1.get(i) != l2.get(i))
                        return true;
                return false;
            }
            return true;
        }
        return ((l2 != null) && !l2.isEmpty());
    }

    // Find touched objects if any and emit accessible toast message
    // with it's brief description.
    private void describePointedObjects(MotionEvent event) {
        PointF point = new PointF(event.getX(), event.getY());
        List<Object> newSelections = new ArrayList<Object>();
        for (OsmandMapLayer layer : mapView.getLayers())
            if (layer instanceof IContextMenuProvider) {
                ((IContextMenuProvider)layer).collectObjectsFromPoint(point, newSelections);
                if (!newSelections.isEmpty()) {
                    contextProvider = (IContextMenuProvider)layer;
                    break;
                }
            }
        if (newSelections.isEmpty()) {
            collectObjectsFromPoint(point, newSelections);
            contextProvider = this;
        }
        if (different(newSelections, selectedObjects)) {
            // FIXME Map explorer
//            ContextMenuLayer contextMenuLayer = mapView.getContextMenuLayer();
//            if (contextMenuLayer != null) {
//                contextMenuLayer.setSelections(newSelections, contextProvider);
//                if (!newSelections.isEmpty())
//                    mapView.showMessage(mapView.getSettings().USE_SHORT_OBJECT_NAMES.get() ?
//                                        contextMenuLayer.getSelectedObjectName() :
//                                        contextMenuLayer.getSelectedObjectDescription());
//            }
//            selectedObjects = newSelections;
        }
    }


    // OnGestureListener interface implementation.

    @Override
    public boolean onDown(MotionEvent e) {
        if (mapView.getSettings().SCROLL_MAP_BY_GESTURES.get())
            return fallback.onDown(e);
        ContextMenuLayer contextMenuLayer = mapView.getLayerByClass(ContextMenuLayer.class);
        // FIXME
//        if (contextMenuLayer != null)
//            contextMenuLayer.setSelections(null, null);
        selectedObjects = null;
        describePointedObjects(e);
        return false;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (mapView.getSettings().SCROLL_MAP_BY_GESTURES.get())
            return fallback.onFling(e1, e2, velocityX, velocityY);
        return true;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        fallback.onLongPress(e);
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        if (mapView.getSettings().SCROLL_MAP_BY_GESTURES.get()) {
            return fallback.onScroll(e1, e2, distanceX, distanceY);
        } else {
            describePointedObjects(e2);
        }
        return true;
    }

    @Override
    public void onShowPress(MotionEvent e) {
        if (mapView.getSettings().SCROLL_MAP_BY_GESTURES.get())
            fallback.onShowPress(e);
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return fallback.onSingleTapUp(e);
    }


    // IContextMenuProvider interface implementation.

    @Override
    public void collectObjectsFromPoint(PointF point, List<Object> objects) {
        int radius = (int)(VICINITY_RADIUS * dm.density);
        int dx = (int)Math.abs(point.x - mapView.getCenterPointX());
        int dy = (int)Math.abs(point.y - mapView.getCenterPointY());
        if ((dx < radius) && (dy < radius))
            objects.add(this);
    }

    @Override
    public LatLon getObjectLocation(Object o) {
        return mapView.getLatLonFromScreenPoint(mapView.getCenterPointX(), mapView.getCenterPointY());
    }

    @Override
    public String getObjectDescription(Object o) {
        return mapView.getContext().getString(R.string.i_am_here);
    }

    @Override
    public String getObjectName(Object o) {
        return mapView.getContext().getString(R.string.i_am_here);
    }

    @Override
    public DialogInterface.OnClickListener getActionListener(List<String> actionsList, Object o) {
        return null;
    }

}
