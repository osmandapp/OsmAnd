package net.osmand.plus.views.layers;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.QUICK_ACTION_HUD_ID;
import static net.osmand.plus.utils.AndroidUtils.getCenterViewCoordinates;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.os.Build;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;

import net.osmand.core.android.MapRendererView;
import net.osmand.data.LatLon;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.MapDisplayPositionManager;
import net.osmand.plus.helpers.MapDisplayPositionManager.IMapDisplayPositionProvider;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickAction.QuickActionSelectionListener;
import net.osmand.plus.quickaction.QuickActionRegistry;
import net.osmand.plus.quickaction.QuickActionRegistry.QuickActionUpdatesListener;
import net.osmand.plus.quickaction.QuickActionsWidget;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.enums.MapPosition;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.controls.maphudbuttons.QuickActionButton;
import net.osmand.plus.views.layers.base.OsmandMapLayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by okorsun on 23.12.16.
 */

public class MapQuickActionLayer extends OsmandMapLayer implements QuickActionUpdatesListener,
        QuickActionSelectionListener, IMapDisplayPositionProvider {

    private ImageView contextMarker;
    private final OsmandApplication app;
    private final OsmandSettings settings;
    private final QuickActionRegistry quickActionRegistry;

    private QuickActionButton quickActionButton;
    private QuickActionsWidget quickActionsWidget;

    private MapPosition previousMapPosition;

    private boolean isLayerOn;
    private boolean inMovingMarkerMode;
    private Boolean currentWidgetState;

    public MapQuickActionLayer(@NonNull Context context) {
        super(context);
        app = getApplication();
        settings = app.getSettings();
        quickActionRegistry = app.getQuickActionRegistry();
    }

    @Override
    public void initLayer(@NonNull OsmandMapTileView view) {
        super.initLayer(view);

        Context context = getContext();
        contextMarker = new ImageView(context);
        contextMarker.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        contextMarker.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.map_pin_context_menu));
        contextMarker.setClickable(true);
        int minw = contextMarker.getDrawable().getMinimumWidth();
        int minh = contextMarker.getDrawable().getMinimumHeight();
        contextMarker.layout(0, 0, minw, minh);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void setMapActivity(@Nullable MapActivity mapActivity) {
        super.setMapActivity(mapActivity);
        if (mapActivity != null) {
            quickActionsWidget = mapActivity.findViewById(R.id.quick_action_widget);

            ImageButton button = mapActivity.findViewById(R.id.map_quick_actions_button);
            quickActionButton = new QuickActionButton(mapActivity, button, QUICK_ACTION_HUD_ID);

            isLayerOn = quickActionRegistry.isQuickActionOn();
        } else {
            quickActionsWidget = null;
            quickActionButton = null;
        }
    }

    public void refreshLayer() {
        setLayerState(false);
        isLayerOn = quickActionRegistry.isQuickActionOn();
        quickActionButton.updateVisibility();
        if (isLayerOn) {
            quickActionButton.setQuickActionButtonMargin();
        }
    }

	public boolean isWidgetVisible() {
		return quickActionsWidget != null && quickActionsWidget.getVisibility() == View.VISIBLE;
	}

	/**
	 * @param showWidget
	 * @return true, if state was changed
	 */
	public boolean setLayerState(boolean showWidget) {
        MapActivity mapActivity = getMapActivity();
        if (mapActivity == null) {
            return false;
        }
		// check if state change is needed
		boolean quickActionModeEnabled = currentWidgetState != null && currentWidgetState || isWidgetVisible();
		boolean quickActionModeDisabled = currentWidgetState == null || !currentWidgetState || !isWidgetVisible();
		if (quickActionModeEnabled == showWidget && quickActionModeDisabled == !showWidget) {
			return false;
		}
		currentWidgetState = showWidget;

		quickActionButton.updateButton(showWidget);
		if (settings.DO_NOT_USE_ANIMATIONS.get() || !quickActionsWidget.isAttachedToWindow()) {
			AndroidUiHelper.updateVisibility(quickActionsWidget, showWidget);
		} else {
			animateWidget(showWidget);
		}
		mapActivity.updateStatusBarColor();

		if (!showWidget) {
		    quitMovingMarker();
		    quickActionRegistry.removeUpdatesListener(this);
		    quickActionsWidget.setSelectionListener(null);
		} else {
		    enterMovingMode(mapActivity.getMapView().getCurrentRotatedTileBox());
		    quickActionsWidget.setActions(quickActionRegistry.getFilteredQuickActions());
		    quickActionRegistry.addUpdatesListener(this);
		    quickActionsWidget.setSelectionListener(this);
		}
		return true;
	}

    private void animateWidget(boolean show) {
	    if (quickActionsWidget == null) {
	        return;
        }
        AnimatorSet set = new AnimatorSet();
        List<Animator> animators = new ArrayList<>();
        int[] animationCoordinates = getCenterViewCoordinates(quickActionButton.getView());
        int centerX = quickActionsWidget.getWidth() / 2;
        int centerY = quickActionsWidget.getHeight() / 2;
        float initialValueX = show ? animationCoordinates[0] - centerX : 0;
        float finalValueX = show ? 0 : animationCoordinates[0] - centerX;
        float initialValueY = show ? animationCoordinates[1] - centerY : 0;
        float finalValueY = show ? 0 : animationCoordinates[1] - centerY;
        animators.add(ObjectAnimator.ofFloat(quickActionsWidget, View.TRANSLATION_X, initialValueX, finalValueX));
        animators.add(ObjectAnimator.ofFloat(quickActionsWidget, View.TRANSLATION_Y, initialValueY, finalValueY));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            float initialRadius = show ? 0 : (float) Math.sqrt(Math.pow(quickActionsWidget.getWidth() / 2, 2) + Math.pow(quickActionsWidget.getHeight() / 2, 2));
            float finalRadius = show ? (float) Math.sqrt(Math.pow(quickActionsWidget.getWidth() / 2, 2) + Math.pow(quickActionsWidget.getHeight() / 2, 2)) : 0;
            Animator circleAnimator = ViewAnimationUtils.createCircularReveal(quickActionsWidget, centerX, centerY, initialRadius, finalRadius);
            animators.add(circleAnimator);
        }
        float initialValueScale = show ? 0f : 1f;
        float finalValueScale = show ? 1f : 0f;
        animators.add(ObjectAnimator.ofFloat(quickActionsWidget, View.SCALE_X, initialValueScale, finalValueScale));
        animators.add(ObjectAnimator.ofFloat(quickActionsWidget, View.SCALE_Y, initialValueScale, finalValueScale));
        set.setDuration(300).playTogether(animators);
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                if (show) {
                    quickActionsWidget.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (!show) {
                    quickActionsWidget.setVisibility(View.GONE);
                    quickActionsWidget.setTranslationX(0);
                    quickActionsWidget.setTranslationY(0);
                }
            }
        });
        set.start();
    }

    private void enterMovingMode(RotatedTileBox tileBox) {
        MapActivity mapActivity = getMapActivity();
        if (mapActivity == null) {
            return;
        }
        previousMapPosition = mapActivity.getMapPositionManager().getNavigationMapPosition();
        MapContextMenu menu = mapActivity.getContextMenu();

        LatLon ll = menu.isActive() && NativeUtilities.containsLatLon(getMapRenderer(), tileBox, menu.getLatLon())
                ? menu.getLatLon() : tileBox.getCenterLatLon();
        boolean isFollowPoint = isFollowPoint(tileBox, menu);

        menu.updateMapCenter(null);
        menu.close();

        RotatedTileBox rb = new RotatedTileBox(tileBox);
        if (!isFollowPoint && previousMapPosition != MapPosition.BOTTOM)
            rb.setCenterLocation(0.5f, 0.3f);

        rb.setLatLonCenter(ll.getLatitude(), ll.getLongitude());

        MapRendererView mapRenderer = view.getMapRenderer();
        if (mapRenderer != null) {
            if (!isFollowPoint) {
                view.setLatLon(ll.getLatitude(), ll.getLongitude());
            }
        } else {
            double lat = rb.getLatFromPixel(tileBox.getCenterPixelX(), tileBox.getCenterPixelY());
            double lon = rb.getLonFromPixel(tileBox.getCenterPixelX(), tileBox.getCenterPixelY());
            view.setLatLon(lat, lon);
        }
        inMovingMarkerMode = true;
        AndroidUiHelper.setVisibility(mapActivity, View.INVISIBLE,
                R.id.map_ruler_layout, R.id.map_left_widgets_panel,
                R.id.map_right_widgets_panel, R.id.map_center_info);
        updateMapDisplayPosition();
        view.refreshMap();
    }

    private void quitMovingMarker() {
        MapActivity mapActivity = getMapActivity();
        if (mapActivity == null) {
            return;
        }
        RotatedTileBox tileBox = mapActivity.getMapView().getCurrentRotatedTileBox();
        if (!isFollowPoint(tileBox, mapActivity.getContextMenu()) && previousMapPosition != MapPosition.BOTTOM) {
            RotatedTileBox rb = tileBox.copy();
            rb.setCenterLocation(0.5f, 0.5f);
            LatLon ll = tileBox.getCenterLatLon();
            MapRendererView mapRenderer = view.getMapRenderer();
            if (mapRenderer != null) {
                NativeUtilities.calculateTarget31(mapRenderer, ll.getLatitude(), ll.getLongitude(), true);
            } else {
                rb.setLatLonCenter(ll.getLatitude(), ll.getLongitude());
                double lat = tileBox.getLatFromPixel(rb.getCenterPixelX(), rb.getCenterPixelY());
                double lon = tileBox.getLonFromPixel(rb.getCenterPixelX(), rb.getCenterPixelY());
                view.setLatLon(lat, lon);
            }
        }
        inMovingMarkerMode = false;
        AndroidUiHelper.setVisibility(mapActivity, View.VISIBLE,
                R.id.map_ruler_layout, R.id.map_left_widgets_panel,
                R.id.map_right_widgets_panel, R.id.map_center_info);
        updateMapDisplayPosition();
        view.refreshMap();
    }

    private boolean isFollowPoint(RotatedTileBox tileBox, MapContextMenu menu) {
        return OsmAndLocationProvider.isLocationPermissionAvailable(getContext()) &&
                app.getMapViewTrackingUtilities().isMapLinkedToLocation() ||
                menu.isActive() && NativeUtilities.containsLatLon(getMapRenderer(), tileBox, menu.getLatLon());  // remove if not to follow if there is selected point on map
    }

    private void updateMapDisplayPosition() {
        MapDisplayPositionManager manager = app.getMapViewTrackingUtilities().getMapDisplayPositionManager();
        manager.updateMapPositionProviders(this, inMovingMarkerMode);
        manager.updateMapDisplayPosition();
    }

    @Nullable
    @Override
    public MapPosition getMapDisplayPosition() {
        if (inMovingMarkerMode) {
            return MapPosition.MIDDLE_BOTTOM;
        }
        return null;
    }

    @Override
    public boolean onSingleTap(@NonNull PointF point, @NonNull RotatedTileBox tileBox) {
        if (isInMovingMarkerMode() && !pressedQuickActionWidget(point.x, point.y)) {
            setLayerState(false);
            return true;
        } else
            return false;
    }

    private boolean pressedQuickActionWidget(float px, float py) {
        return quickActionsWidget != null && py <= quickActionsWidget.getHeight();
    }

    @Override
    public void onDraw(Canvas canvas, RotatedTileBox box, DrawSettings settings) {
        if (isInMovingMarkerMode()) {
            canvas.translate(box.getCenterPixelX() - contextMarker.getWidth() / 2, box.getCenterPixelY() - contextMarker.getHeight());
            contextMarker.draw(canvas);
        }
        boolean nightMode = app.getDaynightHelper().isNightMode();
        quickActionButton.update(nightMode, false, false);
    }

    public boolean getCurrentWidgetState() {
        return currentWidgetState != null ? currentWidgetState : false;
    }

    @Override
    public boolean drawInScreenPixels() {
        return true;
    }


    @Override
    public void onActionsUpdated() {
	    if (quickActionsWidget != null) {
            quickActionsWidget.setActions(quickActionRegistry.getFilteredQuickActions());
        }
    }

    @Override
    public void onActionSelected(QuickAction action) {
        MapActivity mapActivity = getMapActivity();
        if (mapActivity != null) {
            QuickActionRegistry.produceAction(action).execute(mapActivity);
            setLayerState(false);
        }
    }

    public PointF getMovableCenterPoint(RotatedTileBox tb) {
        return new PointF(tb.getPixWidth() / 2, tb.getPixHeight() / 2);
    }

    public boolean isInMovingMarkerMode() {
        return isLayerOn && inMovingMarkerMode;
    }

    public boolean isLayerOn() {
        return isLayerOn;
    }

    public boolean onBackPressed() {
        return setLayerState(false);
    }
}
