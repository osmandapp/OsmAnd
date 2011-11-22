package net.osmand.access;

import net.osmand.access.AccessibleToast;
import net.osmand.plus.R;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.ContextMenuLayer.IContextMenuProvider;

import android.content.Context;
import android.graphics.PointF;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.view.GestureDetector.OnGestureListener;
import android.widget.Toast;

// Provide touch exploration mode for map view
// when scrolling it by gestures is disabled.
//
public class MapExplorer implements OnGestureListener {

    private static final int HYSTERESIS = 5;
    private static final float VICINITY = 7;

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
            point.x -= mapView.getCenterPointX();
            point.y -= mapView.getCenterPointY();
            if (point.length() < (VICINITY * dm.density))
                newSelection = this;
        }
        if (newSelection != null) {
            dribbleCount = HYSTERESIS;
            if (newSelection != selectedObject) {
                final Context ctx = mapView.getContext();
                final String description = (newSelection != this) ? contextProvider.getObjectDescription(newSelection) : ctx.getString(R.string.i_am_here);
                AccessibleToast.makeText(mapView.getContext(), description, Toast.LENGTH_SHORT).show();
                selectedObject = newSelection;
            }
        } else if (dribbleCount > 0) {
            dribbleCount--;
        } else {
            selectedObject = newSelection;
        }
    }


    // OnGestureListener interface implementation.

    @Override
    public boolean onDown(MotionEvent e) {
        if (mapView.getSettings().SCROLL_MAP_BY_GESTURES.get())
            return fallback.onDown(e);
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
        if (mapView.getSettings().SCROLL_MAP_BY_GESTURES.get())
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
        if (mapView.getSettings().SCROLL_MAP_BY_GESTURES.get())
            return fallback.onSingleTapUp(e);
        return true;
    }

}
