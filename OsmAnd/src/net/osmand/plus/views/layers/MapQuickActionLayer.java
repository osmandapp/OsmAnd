package net.osmand.plus.views.layers;

import static net.osmand.plus.settings.backend.preferences.FabMarginPreference.setFabButtonMargin;
import static net.osmand.plus.utils.AndroidUtils.*;
import static net.osmand.plus.utils.AndroidUtils.calculateTotalSizePx;
import static net.osmand.plus.views.layers.ContextMenuLayer.VIBRATE_SHORT;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.os.Build;
import android.os.Vibrator;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.util.Pair;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetView;

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
import net.osmand.plus.settings.backend.preferences.FabMarginPreference;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.OsmandMapTileView;
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

    private ImageButton quickActionButton;
    private QuickActionsWidget quickActionsWidget;

    private int previousMapPosition;

    private boolean inMovingMarkerMode;
    private boolean isLayerOn;

    private boolean nightMode;
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
            quickActionButton = mapActivity.findViewById(R.id.map_quick_actions_button);
            setQuickActionButtonMargin();
            isLayerOn = quickActionRegistry.isQuickActionOn();
            nightMode = app.getDaynightHelper().isNightMode();
            updateQuickActionButton(false);
            quickActionButton.setContentDescription(getString(R.string.configure_screen_quick_action));
            quickActionButton.setOnClickListener(v -> {
                mapActivity.dismissCardDialog();
                if (!showTutorialIfNeeded()) {
                    setLayerState(!isWidgetVisible());
                }
            });
            quickActionButton.setOnLongClickListener(v -> {
                Vibrator vibrator = (Vibrator) mapActivity.getSystemService(Context.VIBRATOR_SERVICE);
                vibrator.vibrate(VIBRATE_SHORT);
                quickActionButton.setScaleX(1.5f);
                quickActionButton.setScaleY(1.5f);
                quickActionButton.setAlpha(0.95f);
                quickActionButton.setOnTouchListener(getMoveFabOnTouchListener(app, mapActivity, quickActionButton, settings.QUICK_ACTION_FAB_MARGIN));
                return true;
            });
        } else {
            quickActionsWidget = null;
            quickActionButton = null;
        }
    }

    public void refreshLayer() {
        setLayerState(false);
        isLayerOn = quickActionRegistry.isQuickActionOn();
        setupQuickActionBtnVisibility();
        if (isLayerOn) {
            setQuickActionButtonMargin();
        }
    }

    private boolean showTutorialIfNeeded() {
        MapActivity mapActivity = getMapActivity();
        if (mapActivity != null && isLayerOn && !app.accessibilityEnabled()
                && !settings.IS_QUICK_ACTION_TUTORIAL_SHOWN.get()) {
            TapTargetView.showFor(mapActivity,                 // `this` is an Activity
                    TapTarget.forView(quickActionButton, getString(R.string.quick_action_btn_tutorial_title), getString(R.string.quick_action_btn_tutorial_descr))
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
        MapActivity mapActivity = getMapActivity();
        if (mapActivity != null) {
            int defRightMargin = calculateTotalSizePx(app, R.dimen.map_button_size, R.dimen.map_button_spacing_land) * 2;
            int defBottomMargin = calculateTotalSizePx(app, R.dimen.map_button_size, R.dimen.map_button_spacing) * 2;
            FrameLayout.LayoutParams param = (FrameLayout.LayoutParams) quickActionButton.getLayoutParams();
            FabMarginPreference preference = settings.QUICK_ACTION_FAB_MARGIN;
            if (AndroidUiHelper.isOrientationPortrait(mapActivity)) {
                Pair<Integer, Integer> fabMargin = preference.getPortraitFabMargin();
                setFabButtonMargin(mapActivity, quickActionButton, param, fabMargin, 0, defBottomMargin);
            } else {
                Pair<Integer, Integer> fabMargin = preference.getLandscapeFabMargin();
                setFabButtonMargin(mapActivity, quickActionButton, param, fabMargin, defRightMargin, 0);
            }
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

		updateQuickActionButton(showWidget);
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
        int[] animationCoordinates = getCenterViewCoordinates(quickActionButton);
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

	private void updateQuickActionButton(boolean widgetVisible) {
	    if (quickActionButton != null) {
            quickActionButton.setBackgroundResource(nightMode ? R.drawable.btn_circle_night : R.drawable.btn_circle_trans);
            int iconId = !widgetVisible ? R.drawable.ic_quick_action : R.drawable.ic_action_close;
            int colorId = ColorUtilities.getMapButtonIconColorId(nightMode);
            setMapButtonIcon(quickActionButton, app.getUIUtilities().getIcon(iconId, colorId));
            quickActionButton.setContentDescription(getString(!widgetVisible ? R.string.configure_screen_quick_action : R.string.shared_string_cancel));
        }
	}

    private void enterMovingMode(RotatedTileBox tileBox) {
        MapActivity mapActivity = getMapActivity();
        if (mapActivity == null) {
            return;
        }
        previousMapPosition = view.getMapPosition();
        MapContextMenu menu = mapActivity.getContextMenu();

        LatLon ll = menu.isActive() && NativeUtilities.containsLatLon(getMapRenderer(), tileBox, menu.getLatLon())
                ? menu.getLatLon() : tileBox.getCenterLatLon();
        boolean isFollowPoint = isFollowPoint(tileBox, menu);

        menu.updateMapCenter(null);
        menu.close();

        RotatedTileBox rb = new RotatedTileBox(tileBox);
        if (!isFollowPoint && previousMapPosition != OsmandSettings.BOTTOM_CONSTANT)
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
        if (!isFollowPoint(tileBox, mapActivity.getContextMenu()) && previousMapPosition != OsmandSettings.BOTTOM_CONSTANT) {
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
        manager.updateProviders(this, inMovingMarkerMode);
        manager.updateMapDisplayPosition();
    }

    @Nullable @Override
    public Integer getMapDisplayPosition() {
        if (inMovingMarkerMode) {
            return OsmandSettings.MIDDLE_BOTTOM_CONSTANT;
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
		boolean nightMode = app.getDaynightHelper().isNightMode();
		if (isInMovingMarkerMode()) {
			canvas.translate(box.getCenterPixelX() - contextMarker.getWidth() / 2, box.getCenterPixelY() - contextMarker.getHeight());
			contextMarker.draw(canvas);
		}
		if (this.nightMode != nightMode) {
            this.nightMode = nightMode;
            boolean widgetVisible = false;
            if (currentWidgetState != null) {
                widgetVisible = currentWidgetState;
            }
            updateQuickActionButton(widgetVisible);
        }
		setupQuickActionBtnVisibility();
	}

    private void setupQuickActionBtnVisibility() {
        MapActivity mapActivity = getMapActivity();
        if (mapActivity != null) {
            boolean visible = mapActivity.getWidgetsVisibilityHelper().shouldShowQuickActionButton();
            quickActionButton.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
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
