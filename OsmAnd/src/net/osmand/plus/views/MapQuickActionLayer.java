package net.osmand.plus.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.support.v4.content.ContextCompat;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;

import net.osmand.data.LatLon;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionRegistry;
import net.osmand.plus.quickaction.QuickActionsWidget;

/**
 * Created by okorsun on 23.12.16.
 */

public class MapQuickActionLayer extends OsmandMapLayer implements QuickActionRegistry.QuickActionUpdatesListener, QuickAction.QuickActionSelectionListener {

    private final ContextMenuLayer    contextMenuLayer;
    private       ImageView           contextMarker;
    private final MapActivity         mapActivity;
    private final OsmandApplication   app;
    private final OsmandSettings      settings;
    private final QuickActionRegistry quickActionRegistry;

    private ImageButton        quickActionButton;
    private QuickActionsWidget quickActionsWidget;

    private OsmandMapTileView view;
    private boolean           wasCollapseButtonVisible;


    private boolean inChangeMarkerPositionMode;

    public MapQuickActionLayer(MapActivity activity, ContextMenuLayer contextMenuLayer) {
        this.mapActivity = activity;
        this.contextMenuLayer = contextMenuLayer;
        app = activity.getMyApplication();
        settings = activity.getMyApplication().getSettings();
        quickActionRegistry = activity.getMapLayers().getQuickActionRegistry();
    }


    @Override
    public void initLayer(OsmandMapTileView view) {
        this.view = view;

        quickActionsWidget = (QuickActionsWidget) mapActivity.findViewById(R.id.quick_action_widget);
        quickActionButton = (ImageButton) mapActivity.findViewById(R.id.map_quick_actions_button);
        quickActionButton.setVisibility(settings.QUICK_ACTION.get() ? View.VISIBLE : View.GONE);
        quickActionButton.setImageResource(R.drawable.map_quick_action);
        quickActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setLayerState(quickActionsWidget.getVisibility() == View.VISIBLE);

            }
        });


        Context context = view.getContext();
        contextMarker = new ImageView(context);
        contextMarker.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        contextMarker.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.map_pin_context_menu));
        contextMarker.setClickable(true);
        int minw = contextMarker.getDrawable().getMinimumWidth();
        int minh = contextMarker.getDrawable().getMinimumHeight();
        contextMarker.layout(0, 0, minw, minh);

        setCloseWidgetOnTouch(mapActivity.findViewById(R.id.map_quick_actions_button_container));
        setCloseWidgetOnTouch(mapActivity.findViewById(R.id.bottom_controls_container));

//        quickActionButton.setOnTouchListener(new View.OnTouchListener() {
//            private int lastAction;
//            private int initialMarginX;
//            private int initialMarginY;
//            private float initialTouchX;
//            private float initialTouchY;
//
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                switch (event.getAction()) {
//                    case MotionEvent.ACTION_DOWN:
//                        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) v.getLayoutParams();
//
//
//                        initialMarginX = params.rightMargin;
//                        initialMarginY = params.bottomMargin;
//
//                        //get the touch location
//                        initialTouchX = event.getRawX();
//                        initialTouchY = event.getRawY();
//
//                        lastAction = event.getAction();
//                        return true;
//
//                    case MotionEvent.ACTION_UP:
//                        if (lastAction == MotionEvent.ACTION_DOWN) {
//                            setLayerState();
//                        }
//                        lastAction = event.getAction();
//                        return true;
//                    case MotionEvent.ACTION_MOVE:
//                        int deltaX = (int) (initialTouchX - event.getRawX());
//                        int deltaY = (int) (initialTouchY - event.getRawY());
//                        if (deltaX < 10 && deltaY < 10)
//                            return false;
//
//                        int newMarginX = initialMarginX + deltaX;
//                        int newMarginY = initialMarginY + deltaY;
//
//                        FrameLayout parent = (FrameLayout) v.getParent();
//                        FrameLayout.LayoutParams param = (FrameLayout.LayoutParams) v.getLayoutParams();
//                        if (v.getHeight() + newMarginY <= parent.getHeight() && newMarginY > 0)
//                            param.bottomMargin = newMarginY;
//
//                        if (v.getWidth() + newMarginX <= parent.getWidth() && newMarginX > 0) {
//                            param.rightMargin = newMarginX;
//                        }
//
//                        v.setLayoutParams(param);
//
//
//                        lastAction = event.getAction();
//                        return true;
//                }
//                return false;
//            }
//        });
    }

    /**
     * @param isClosed
     * @return true, if state was changed
     */
    public boolean setLayerState(boolean isClosed) {
        if ((quickActionsWidget.getVisibility() == View.VISIBLE) != isClosed)    // check if state change is needed
            return false;

        quickActionButton.setImageResource(isClosed ? R.drawable.map_quick_action : R.drawable.map_action_cancel);
        quickActionsWidget.setVisibility(isClosed ? View.GONE : View.VISIBLE);

        if (isClosed) {
            quitMovingMarker();
            quickActionRegistry.setUpdatesListener(null);
            quickActionsWidget.setSelectionListener(null);
        } else {
            enterMovingMode(mapActivity.getMapView().getCurrentRotatedTileBox());
            quickActionsWidget.setActions(quickActionRegistry.getQuickActions());
            quickActionRegistry.setUpdatesListener(MapQuickActionLayer.this);
            quickActionsWidget.setSelectionListener(MapQuickActionLayer.this);
        }

//        if (isClosed) {
//            contextMenuLayer.quitMovingMarker();
//        }
//        else {
//            LatLon centerLatLon = mapActivity.getMapView().getCurrentRotatedTileBox().getCenterLatLon();
//            contextMenuLayer.showContextMenu(centerLatLon.getLatitude(), centerLatLon.getLongitude(), false);
//            contextMenuLayer.enterMovingMode(mapActivity.getMapView().getCurrentRotatedTileBox());
//        }
        return true;
    }

    private void setCloseWidgetOnTouch(View view) {
        view.setOnTouchListener(new View.OnTouchListener() {
            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        break;
                    case MotionEvent.ACTION_UP:
                        int deltaX = (int) (initialTouchX - event.getRawX());
                        int deltaY = (int) (initialTouchY - event.getRawY());
                        if (deltaX < 10 && deltaY < 10) {
                            setLayerState(true);
                        }

                }
                return false;
            }
        });
    }

    private void enterMovingMode(RotatedTileBox tileBox) {
        MapContextMenu menu = mapActivity.getContextMenu();

        menu.updateMapCenter(null);
        menu.hide();

        LatLon         ll = tileBox.getCenterLatLon();
        RotatedTileBox rb = new RotatedTileBox(tileBox);
        rb.setCenterLocation(0.5f, 0.5f);
        rb.setLatLonCenter(ll.getLatitude(), ll.getLongitude());
        double lat = rb.getLatFromPixel(tileBox.getCenterPixelX(), tileBox.getCenterPixelY());
        double lon = rb.getLonFromPixel(tileBox.getCenterPixelX(), tileBox.getCenterPixelY());
        view.setLatLon(lat, lon);

        inChangeMarkerPositionMode = true;
        mark(View.INVISIBLE, R.id.map_ruler_layout,
                R.id.map_left_widgets_panel, R.id.map_right_widgets_panel, R.id.map_center_info);

        View collapseButton = mapActivity.findViewById(R.id.map_collapse_button);
        if (collapseButton != null && collapseButton.getVisibility() == View.VISIBLE) {
            wasCollapseButtonVisible = true;
            collapseButton.setVisibility(View.INVISIBLE);
        } else {
            wasCollapseButtonVisible = false;
        }

        view.refreshMap();
    }

    private void quitMovingMarker() {
        inChangeMarkerPositionMode = false;
        mark(View.VISIBLE, R.id.map_ruler_layout,
                R.id.map_left_widgets_panel, R.id.map_right_widgets_panel, R.id.map_center_info);

        View collapseButton = mapActivity.findViewById(R.id.map_collapse_button);
        if (collapseButton != null && wasCollapseButtonVisible) {
            collapseButton.setVisibility(View.VISIBLE);
        }
        view.refreshMap();
    }

    private void mark(int status, int... widgets) {
        for (int widget : widgets) {
            View v = mapActivity.findViewById(widget);
            if (v != null) {
                v.setVisibility(status);
            }
        }
    }

    public void refreshLayer() {
        quickActionButton.setVisibility(settings.QUICK_ACTION.get() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDraw(Canvas canvas, RotatedTileBox box, DrawSettings settings) {
        if (inChangeMarkerPositionMode) {
            canvas.translate(box.getPixWidth() / 2 - contextMarker.getWidth() / 2, box.getPixHeight() / 2 - contextMarker.getHeight());
            contextMarker.draw(canvas);
        }
    }

    @Override
    public void destroyLayer() {

    }

    @Override
    public boolean drawInScreenPixels() {
        return true;
    }


    @Override
    public void onActionsUpdated() {
        quickActionsWidget.setActions(quickActionRegistry.getQuickActions());
    }

    @Override
    public void onActionSelected(QuickAction action) {
        action.execute(mapActivity);
    }

    public PointF getMovableCenterPoint(RotatedTileBox tb) {
        return new PointF(tb.getPixWidth() / 2, tb.getPixHeight() / 2);
    }

    public boolean isInChangeMarkerPositionMode() {
        return inChangeMarkerPositionMode;
    }

    public boolean onBackPressed() {
        return setLayerState(true);
    }
}
