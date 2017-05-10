package net.osmand.plus.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.os.Vibrator;
import android.support.annotation.DimenRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetView;

import net.osmand.data.LatLon;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionFactory;
import net.osmand.plus.quickaction.QuickActionRegistry;
import net.osmand.plus.quickaction.QuickActionsWidget;

import static net.osmand.plus.views.ContextMenuLayer.VIBRATE_SHORT;

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
    private int               previousMapPosition;


    private boolean inChangeMarkerPositionMode;
    private boolean isLayerOn;

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
        setQuickActionButtonMargin();
        isLayerOn = quickActionRegistry.isQuickActionOn();
        quickActionButton.setImageResource(R.drawable.map_quick_action);
        quickActionButton.setContentDescription(mapActivity.getString(R.string.configure_screen_quick_action));
        quickActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!showTutorialIfNeeded())
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


        quickActionButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Vibrator vibrator = (Vibrator) mapActivity.getSystemService(Context.VIBRATOR_SERVICE);
                vibrator.vibrate(VIBRATE_SHORT);
                quickActionButton.setScaleX(1.5f);
                quickActionButton.setScaleY(1.5f);
                quickActionButton.setAlpha(0.95f);
                quickActionButton.setOnTouchListener(onQuickActionTouchListener);
                return true;
            }
        });

    }

    public void refreshLayer() {
        setLayerState(true);
        isLayerOn = quickActionRegistry.isQuickActionOn();
        setUpQuickActionBtnVisibility();
    }

    private boolean showTutorialIfNeeded() {
        if (isLayerOn && !app.accessibilityEnabled() && !settings.IS_QUICK_ACTION_TUTORIAL_SHOWN.get() && android.os.Build.VERSION.SDK_INT >= 14) {
            TapTargetView.showFor(mapActivity,                 // `this` is an Activity
                    TapTarget.forView(quickActionButton, mapActivity.getString(R.string.quick_action_btn_tutorial_title), mapActivity.getString(R.string.quick_action_btn_tutorial_descr))
                            // All options below are optional
                            .outerCircleColor(R.color.osmand_orange)      // Specify a color for the outer circle
                            .targetCircleColor(R.color.color_white)   // Specify a color for the target circle
                            .titleTextSize(20)                  // Specify the size (in sp) of the title text
                            .descriptionTextSize(16)            // Specify the size (in sp) of the description text
                            .descriptionTextColor(R.color.color_white)            // Specify a color for both the title and description text
                            .titleTextColor(R.color.color_white)            // Specify a color for both the title and description text
                            .drawShadow(true)                   // Whether to draw a drop shadow or not
                            .cancelable(false)                  // Whether tapping outside the outer circle dismisses the view
                            .tintTarget(false)                   // Whether to tint the target view's color
                            .transparentTarget(false)           // Specify whether the target is transparent (displays the content underneath)
                            .targetRadius(50),                  // Specify the target radius (in dp)
                    new TapTargetView.Listener() {          // The listener can listen for regular clicks, long clicks or cancels
                        @Override
                        public void onTargetClick(TapTargetView view) {
                            super.onTargetClick(view);      // This call is optional
                            settings.IS_QUICK_ACTION_TUTORIAL_SHOWN.set(true);
                        }
                    });
            return true;
        } else
            return false;
    }

    private void setQuickActionButtonMargin() {
        FrameLayout.LayoutParams param = (FrameLayout.LayoutParams) quickActionButton.getLayoutParams();
        if (AndroidUiHelper.isOrientationPortrait(mapActivity)) {
            Pair<Integer, Integer> fabMargin = settings.getPortraitFabMargin();
            if (fabMargin != null) {
                param.rightMargin = fabMargin.first;
                param.bottomMargin = fabMargin.second;
            } else {
                param.bottomMargin = calculateTotalSizePx(R.dimen.map_button_size, R.dimen.map_button_spacing) * 2;
            }
        } else {
            Pair<Integer, Integer> fabMargin = settings.getLandscapeFabMargin();
            if (fabMargin != null) {
                param.rightMargin = fabMargin.first;
                param.bottomMargin = fabMargin.second;
            } else {
                param.rightMargin = calculateTotalSizePx(R.dimen.map_button_size, R.dimen.map_button_spacing_land) * 2;
            }
        }
        quickActionButton.setLayoutParams(param);
    }

    private int calculateTotalSizePx(@DimenRes int... dimensId) {
        int result = 0;
        for (int id : dimensId) {
            result += mapActivity.getResources().getDimensionPixelSize(id);
        }
        return result;
    }

    /**
     * @param isClosed
     * @return true, if state was changed
     */
    public boolean setLayerState(boolean isClosed) {
        if ((quickActionsWidget.getVisibility() == View.VISIBLE) != isClosed)    // check if state change is needed
            return false;

        quickActionButton.setImageResource(isClosed ? R.drawable.map_quick_action : R.drawable.map_action_cancel);
        quickActionButton.setContentDescription(mapActivity.getString(isClosed ? R.string.configure_screen_quick_action : R.string.shared_string_cancel));
        quickActionsWidget.setVisibility(isClosed ? View.GONE : View.VISIBLE);

        if (isClosed) {
            quitMovingMarker();
            quickActionRegistry.setUpdatesListener(null);
            quickActionsWidget.setSelectionListener(null);
        } else {
            enterMovingMode(mapActivity.getMapView().getCurrentRotatedTileBox());
            quickActionsWidget.setActions(quickActionRegistry.getFilteredQuickActions());
            quickActionRegistry.setUpdatesListener(MapQuickActionLayer.this);
            quickActionsWidget.setSelectionListener(MapQuickActionLayer.this);
        }

        return true;
    }

    private void enterMovingMode(RotatedTileBox tileBox) {
        previousMapPosition = view.getMapPosition();
        view.setMapPosition(OsmandSettings.MIDDLE_CONSTANT);
        MapContextMenu menu = mapActivity.getContextMenu();

        LatLon ll = menu.isActive() && tileBox.containsLatLon(menu.getLatLon()) ? menu.getLatLon() : tileBox.getCenterLatLon();
        boolean isFollowPoint = isFolowPoint(tileBox, menu);

        menu.updateMapCenter(null);
        menu.close();

        RotatedTileBox rb = new RotatedTileBox(tileBox);
        if (!isFollowPoint && previousMapPosition != OsmandSettings.BOTTOM_CONSTANT)
            rb.setCenterLocation(0.5f, 0.3f);

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

    private boolean isFolowPoint(RotatedTileBox tileBox, MapContextMenu menu) {
        return OsmAndLocationProvider.isLocationPermissionAvailable(mapActivity) &&
                mapActivity.getMapViewTrackingUtilities().isMapLinkedToLocation() ||
                menu.isActive() && tileBox.containsLatLon(menu.getLatLon());  // remove if not to folow if there is selected point on map
    }

    private void quitMovingMarker() {

        RotatedTileBox tileBox = mapActivity.getMapView().getCurrentRotatedTileBox();
        if (!isFolowPoint(tileBox, mapActivity.getContextMenu()) && previousMapPosition != OsmandSettings.BOTTOM_CONSTANT){
            RotatedTileBox rb = tileBox.copy();
            rb.setCenterLocation(0.5f, 0.5f);
            LatLon ll = tileBox.getCenterLatLon();
            rb.setLatLonCenter(ll.getLatitude(), ll.getLongitude());
            double lat = tileBox.getLatFromPixel(rb.getCenterPixelX(), rb.getCenterPixelY());
            double lon = tileBox.getLonFromPixel(rb.getCenterPixelX(), rb.getCenterPixelY());
            view.setLatLon(lat, lon);
        }
        view.setMapPosition(previousMapPosition);

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

    @Override
    public boolean onSingleTap(PointF point, RotatedTileBox tileBox) {
        if (isInChangeMarkerPositionMode() && !pressedQuickActionWidget(point.x, point.y)) {
            setLayerState(true);
            return true;
        } else
            return false;
    }

    private boolean pressedQuickActionWidget(float px, float py) {
        return py <= quickActionsWidget.getHeight();
    }

    @Override
    public void onDraw(Canvas canvas, RotatedTileBox box, DrawSettings settings) {
        if (isInChangeMarkerPositionMode()) {
            canvas.translate(box.getCenterPixelX() - contextMarker.getWidth() / 2, box.getCenterPixelY() - contextMarker.getHeight());
            contextMarker.draw(canvas);
        }
        setUpQuickActionBtnVisibility();
    }

    private void setUpQuickActionBtnVisibility() {
        boolean hideQuickButton = !isLayerOn ||
                contextMenuLayer.isInChangeMarkerPositionMode() ||
				contextMenuLayer.isInGpxDetailsMode() ||
                mapActivity.getContextMenu().isVisible() && !mapActivity.getContextMenu().findMenuFragment().get().isRemoving() ||
                mapActivity.getContextMenu().isVisible() && mapActivity.getContextMenu().findMenuFragment().get().isAdded() ||
                mapActivity.getContextMenu().getMultiSelectionMenu().isVisible() && mapActivity.getContextMenu().getMultiSelectionMenu().getFragmentByTag().isAdded() ||
                mapActivity.getContextMenu().getMultiSelectionMenu().isVisible() && !mapActivity.getContextMenu().getMultiSelectionMenu().getFragmentByTag().isRemoving();
        quickActionButton.setVisibility(hideQuickButton ? View.GONE : View.VISIBLE);
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
        quickActionsWidget.setActions(quickActionRegistry.getFilteredQuickActions());
    }

    @Override
    public void onActionSelected(QuickAction action) {
        QuickActionFactory.produceAction(action).execute(mapActivity);
        setLayerState(true);
    }

    public PointF getMovableCenterPoint(RotatedTileBox tb) {
        return new PointF(tb.getPixWidth() / 2, tb.getPixHeight() / 2);
    }

    public boolean isInChangeMarkerPositionMode() {
        return isLayerOn && inChangeMarkerPositionMode;
    }

    public boolean isLayerOn() {
        return isLayerOn;
    }

    public boolean onBackPressed() {
        return setLayerState(true);
    }

    View.OnTouchListener onQuickActionTouchListener = new View.OnTouchListener() {
        private int initialMarginX;
        private int initialMarginY;
        private float initialTouchX;
        private float initialTouchY;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    setUpInitialValues(v, event);
                    return true;
                case MotionEvent.ACTION_UP:
                    quickActionButton.setOnTouchListener(null);
                    quickActionButton.setPressed(false);
                    quickActionButton.setScaleX(1);
                    quickActionButton.setScaleY(1);
                    quickActionButton.setAlpha(1f);
                    FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) v.getLayoutParams();
                    if (AndroidUiHelper.isOrientationPortrait(mapActivity))
                        settings.setPortraitFabMargin(params.rightMargin, params.bottomMargin);
                    else
                        settings.setLandscapeFabMargin(params.rightMargin, params.bottomMargin);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (initialMarginX == 0 && initialMarginY == 0 && initialTouchX == 0 && initialTouchY == 0)
                        setUpInitialValues(v, event);

                    int padding = calculateTotalSizePx(R.dimen.map_button_margin);
                    FrameLayout parent = (FrameLayout) v.getParent();
                    FrameLayout.LayoutParams param = (FrameLayout.LayoutParams) v.getLayoutParams();

                    int deltaX = (int) (initialTouchX - event.getRawX());
                    int deltaY = (int) (initialTouchY - event.getRawY());

                    int newMarginX = interpolate(initialMarginX + deltaX, v.getWidth(), parent.getWidth() - padding * 2);
                    int newMarginY = interpolate(initialMarginY + deltaY, v.getHeight(), parent.getHeight() - padding * 2);

                    if (v.getHeight() + newMarginY <= parent.getHeight() - padding * 2 && newMarginY > 0)
                        param.bottomMargin = newMarginY;

                    if (v.getWidth() + newMarginX <= parent.getWidth() - padding * 2 && newMarginX > 0) {
                        param.rightMargin = newMarginX;
                    }

                    v.setLayoutParams(param);

                    return true;
            }
            return false;
        }

        private int interpolate(int value, int divider, int boundsSize) {
            int viewSize = divider;
            if (value <= divider && value > 0)
                return value * value / divider;
            else {
                int leftMargin = boundsSize - value - viewSize;
                if (leftMargin <= divider && value < boundsSize - viewSize)
                    return leftMargin - (leftMargin * leftMargin / divider) + value;
                else
                    return value;
            }
        }

        private void setUpInitialValues(View v, MotionEvent event) {
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) v.getLayoutParams();

            initialMarginX = params.rightMargin;
            initialMarginY = params.bottomMargin;

            initialTouchX = event.getRawX();
            initialTouchY = event.getRawY();
        }
    };
}
