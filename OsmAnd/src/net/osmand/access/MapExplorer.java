package net.osmand.access;

import java.lang.Math;
import java.util.List;

import net.osmand.osm.LatLon;
import net.osmand.plus.R;
import net.osmand.plus.views.ContextMenuLayer;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.ContextMenuLayer.IContextMenuProvider;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.PointF;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.view.GestureDetector.OnGestureListener;

// Provide touch exploration mode for map view
// when scrolling it by gestures is disabled.
//
public class MapExplorer implements OnGestureListener, IContextMenuProvider {

    private static final int HYSTERESIS = 5;
    private static final float VICINITY_RADIUS = 15;

    private OsmandMapTileView mapView;
    private OnGestureListener fallback;
    private Object selectedObject = null;
    private IContextMenuProvider contextProvider;
    private int dribbleCount;
    private final DisplayMetrics dm = new DisplayMetrics();


    // OnGestureListener specified as a second argument
    // will be used when scrolling map by gestures
    // is enabled.
    public MapExplorer(OsmandMapTileView mapView, OnGestureListener fallback) {
        this.mapView = mapView;
        this.fallback = fallback;
        ((WindowManager)(mapView.getContext().getSystemService(Context.WINDOW_SERVICE))).getDefaultDisplay().getMetrics(dm);
    }


    // Find touched object if any and emit accessible toast message
    // with it's brief description.
    private void describePointedObject(MotionEvent event) {
        PointF point = new PointF(event.getX(), event.getY());
        Object newSelection = null;
        for(OsmandMapLayer layer : mapView.getLayers())
            if(layer instanceof IContextMenuProvider) {
                newSelection = ((IContextMenuProvider)layer).getPointObject(point);
                if(newSelection != null) {
                    contextProvider = (IContextMenuProvider)layer;
                    break;
                }
            }
        if (newSelection == null) {
            newSelection = getPointObject(point);
            contextProvider = this;
        }
        if (newSelection != null) {
            dribbleCount = 0;
            if (newSelection != selectedObject) {
                ContextMenuLayer contextMenuLayer = mapView.getContextMenuLayer();
                mapView.showMessage(contextProvider.getObjectDescription(newSelection));
                if (contextMenuLayer != null)
                    contextMenuLayer.setSelection(newSelection, contextProvider);
                selectedObject = newSelection;
            }
        } else if ((selectedObject != null) && (++dribbleCount > HYSTERESIS)) {
            ContextMenuLayer contextMenuLayer = mapView.getContextMenuLayer();
            if (contextMenuLayer != null)
                contextMenuLayer.setSelection(null, null);
            selectedObject = null;
        }
    }


    // OnGestureListener interface implementation.

    @Override
    public boolean onDown(MotionEvent e) {
        if (mapView.getSettings().SCROLL_MAP_BY_GESTURES.get())
            return fallback.onDown(e);
        ContextMenuLayer contextMenuLayer = mapView.getContextMenuLayer();
        if (contextMenuLayer != null)
            contextMenuLayer.setSelection(null, null);
        selectedObject = null;
        describePointedObject(e);
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
            describePointedObject(e2);
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
    public Object getPointObject(PointF point) {
        int radius = (int)(VICINITY_RADIUS * dm.density);
        int dx = (int)Math.abs(point.x - mapView.getCenterPointX());
        int dy = (int)Math.abs(point.y - mapView.getCenterPointY());
        if ((dx < radius) && (dy < radius))
            return this;
        return null;
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
    public DialogInterface.OnClickListener getActionListener(List<String> actionsList, Object o) {
        return null;
    }

}
