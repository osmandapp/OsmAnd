package net.osmand.plus.views.layers;

import static android.view.Gravity.BOTTOM;
import static android.view.Gravity.END;
import static net.osmand.plus.settings.backend.preferences.FabMarginPreference.setFabButtonMargin;
import static net.osmand.plus.utils.AndroidUtils.calculateTotalSizePx;
import static net.osmand.plus.utils.AndroidUtils.getMoveFabOnTouchListener;
import static net.osmand.plus.views.layers.ContextMenuLayer.VIBRATE_SHORT;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
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
import net.osmand.plus.quickaction.MapButtonsHelper;
import net.osmand.plus.quickaction.MapButtonsHelper.QuickActionUpdatesListener;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickAction.QuickActionSelectionListener;
import net.osmand.plus.quickaction.QuickActionsWidget;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.enums.MapPosition;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.controls.maphudbuttons.QuickActionButton;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.plus.views.mapwidgets.configure.buttons.QuickActionButtonState;
import net.osmand.util.Algorithms;

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
	private final MapButtonsHelper mapButtonsHelper;

	private List<QuickActionButton> actionButtons = new ArrayList<>();
	private List<QuickActionButtonState> mapButtonStates = new ArrayList<>();
	private QuickActionButton selectedButton;
	private QuickActionsWidget quickActionsWidget;

	private MapPosition previousMapPosition;

	private boolean isLayerOn;
	private boolean inMovingMarkerMode;
	private Boolean currentWidgetState;

	public MapQuickActionLayer(@NonNull Context context) {
		super(context);
		app = getApplication();
		settings = app.getSettings();
		mapButtonsHelper = app.getMapButtonsHelper();
	}

	@Override
	public void initLayer(@NonNull OsmandMapTileView view) {
		super.initLayer(view);
		createContextMarker();
	}

	private void createContextMarker() {
		Context context = AndroidUtils.createDisplayContext(getContext());
		contextMarker = new ImageView(context);
		contextMarker.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT));
		contextMarker.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.map_pin_context_menu));
		contextMarker.setClickable(true);
		int width = contextMarker.getDrawable().getMinimumWidth();
		int height = contextMarker.getDrawable().getMinimumHeight();
		contextMarker.layout(0, 0, width, height);
	}

	@Override
	protected void updateResources() {
		super.updateResources();
		createContextMarker();
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public void setMapActivity(@Nullable MapActivity mapActivity) {
		super.setMapActivity(mapActivity);
		if (mapActivity != null) {
			updateButtons();
			isLayerOn = mapButtonsHelper.hasEnabledButtons();
			quickActionsWidget = mapActivity.findViewById(R.id.quick_action_widget);
		} else {
			quickActionsWidget = null;
			actionButtons = new ArrayList<>();
		}
	}

	private void updateButtons() {
		MapActivity activity = getMapActivity();
		if (activity != null) {
			boolean nightMode = app.getDaynightHelper().isNightMode();
			LayoutInflater inflater = UiUtilities.getInflater(activity, nightMode);
			ViewGroup container = activity.findViewById(R.id.quick_actions_container);
			container.removeAllViews();

			List<QuickActionButton> buttons = new ArrayList<>();
			List<QuickActionButtonState> buttonStates = mapButtonsHelper.getButtonsStates();
			for (QuickActionButtonState state : buttonStates) {
				buttons.add(createActionButton(state, container, inflater, nightMode));
			}
			actionButtons = buttons;
			mapButtonStates = buttonStates;
		}
	}

	public void refreshLayer() {
		setSelectedButton(null);
		isLayerOn = mapButtonsHelper.hasEnabledButtons();

		for (QuickActionButton button : actionButtons) {
			updateButtonVisibility(button);
			if (isLayerOn) {
				updateButtonMargin(button);
			}
		}
	}

	@Nullable
	public QuickActionButton getSelectedButton() {
		return selectedButton;
	}

	public boolean isWidgetVisible() {
		return quickActionsWidget != null && quickActionsWidget.getVisibility() == View.VISIBLE;
	}

	public boolean isWidgetVisibleForButton(@NonNull QuickActionButton button) {
		return isWidgetVisible() && selectedButton != null
				&& Algorithms.stringsEqual(selectedButton.getButtonId(), button.getButtonId());
	}

	public boolean setSelectedButton(@Nullable QuickActionButton button) {
		boolean visible = button != null;
		MapActivity mapActivity = getMapActivity();
		// check if state change is needed
		boolean buttonChanged = selectedButton != button;
		boolean modeEnabled = currentWidgetState != null && currentWidgetState || isWidgetVisible();
		boolean modeDisabled = currentWidgetState == null || !currentWidgetState || !isWidgetVisible();
		if (mapActivity == null || modeEnabled == visible && modeDisabled == !visible && !buttonChanged) {
			return false;
		}
		selectedButton = button;
		currentWidgetState = visible;

		boolean nightMode = app.getDaynightHelper().isNightMode();
		for (QuickActionButton actionButton : actionButtons) {
			actionButton.update(nightMode, true);
		}
		if (visible) {
			enterMovingMode(mapActivity.getMapView().getCurrentRotatedTileBox());
			quickActionsWidget.setSelectedButton(button);
			mapButtonsHelper.addUpdatesListener(this);
			quickActionsWidget.setSelectionListener(this);
		} else {
			quitMovingMarker();
			mapButtonsHelper.removeUpdatesListener(this);
			quickActionsWidget.setSelectionListener(null);
		}
		if (settings.DO_NOT_USE_ANIMATIONS.get() || !quickActionsWidget.isAttachedToWindow()) {
			AndroidUiHelper.updateVisibility(quickActionsWidget, visible);
		} else {
			animateWidget(visible);
		}
		mapActivity.updateStatusBarColor();

		return true;
	}

	private void animateWidget(boolean show) {
		if (quickActionsWidget != null) {
			quickActionsWidget.animateWidget(show);
		}
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
		AndroidUiHelper.setVisibility(mapActivity, View.INVISIBLE, R.id.map_ruler_layout, R.id.map_center_info);
		AndroidUiHelper.setVisibility(mapActivity, View.GONE, R.id.map_left_widgets_panel, R.id.map_right_widgets_panel);
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
			setSelectedButton(null);
			return true;
		}
		return false;
	}

	private boolean pressedQuickActionWidget(float px, float py) {
		return quickActionsWidget != null && py <= quickActionsWidget.getHeight();
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox box, DrawSettings settings) {
		if (isInMovingMarkerMode()) {
			canvas.translate(box.getCenterPixelX() - contextMarker.getWidth() / 2f, box.getCenterPixelY() - contextMarker.getHeight());
			contextMarker.draw(canvas);
		}
		if (mapButtonStates != mapButtonsHelper.getButtonsStates()) {
			app.runInUIThread(this::updateButtons);
		}
		boolean nightMode = app.getDaynightHelper().isNightMode();
		for (QuickActionButton button : actionButtons) {
			button.update(nightMode, false);
		}
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
			quickActionsWidget.updateActions();
		}
	}

	@Override
	public void onActionSelected(@NonNull QuickActionButtonState buttonState, @NonNull QuickAction action) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			MapButtonsHelper.produceAction(action).execute(mapActivity);
			setSelectedButton(null);
		}
	}

	@NonNull
	public PointF getMovableCenterPoint(@NonNull RotatedTileBox tb) {
		return new PointF(tb.getPixWidth() / 2f, tb.getPixHeight() / 2f);
	}

	public boolean isInMovingMarkerMode() {
		return isLayerOn && inMovingMarkerMode;
	}

	public boolean isLayerOn() {
		return isLayerOn;
	}

	public boolean onBackPressed() {
		return setSelectedButton(null);
	}

	@SuppressLint("ClickableViewAccessibility")
	@NonNull
	private QuickActionButton createActionButton(@NonNull QuickActionButtonState buttonState,
	                                             @NonNull ViewGroup container,
	                                             @NonNull LayoutInflater inflater, boolean nightMode) {
		int size = app.getResources().getDimensionPixelSize(R.dimen.map_button_size);
		QuickActionButton button = (QuickActionButton) inflater.inflate(R.layout.map_quick_actions_button, container, false);
		button.setButtonState(buttonState);
		container.addView(button, new FrameLayout.LayoutParams(size, size, BOTTOM | END));

		button.setOnClickListener(v -> {
			requireMapActivity().getFragmentsHelper().dismissCardDialog();
			if (!buttonState.isDefaultButton() && buttonState.isSingleAction()) {
				List<QuickAction> actions = buttonState.getQuickActions();
				onActionSelected(buttonState, actions.get(0));
			} else if (!showTutorialIfNeeded(button)) {
				boolean visible = isWidgetVisibleForButton(button);
				setSelectedButton(visible ? null : button);
			}
		});
		button.setOnLongClickListener(v -> {
			Vibrator vibrator = (Vibrator) requireMapActivity().getSystemService(Context.VIBRATOR_SERVICE);
			vibrator.vibrate(VIBRATE_SHORT);
			button.setScaleX(1.5f);
			button.setScaleY(1.5f);
			button.setAlpha(0.95f);
			button.setOnTouchListener(getMoveFabOnTouchListener(app, getMapActivity(), button, buttonState.getFabMarginPref()));
			return true;
		});
		button.update(nightMode, true);
		updateButtonMargin(button);

		return button;
	}

	private boolean showTutorialIfNeeded(@NonNull QuickActionButton button) {
		MapActivity activity = getMapActivity();
		if (activity != null && isLayerOn() && !app.accessibilityEnabled() && !settings.IS_QUICK_ACTION_TUTORIAL_SHOWN.get()) {
			TapTarget tapTarget = TapTarget.forView(button, getString(R.string.quick_action_btn_tutorial_title), getString(R.string.quick_action_btn_tutorial_descr))
					// All options below are optional
					.outerCircleColor(R.color.osmand_orange)
					.targetCircleColor(R.color.card_and_list_background_light)
					.titleTextSize(20).descriptionTextSize(16)
					.descriptionTextColor(R.color.card_and_list_background_light)
					.titleTextColor(R.color.card_and_list_background_light)
					.drawShadow(true)
					.cancelable(false)
					.tintTarget(false)
					.transparentTarget(false)
					.targetRadius(50);
			TapTargetView.showFor(activity, tapTarget, new TapTargetView.Listener() {
				@Override
				public void onTargetClick(TapTargetView view) {
					super.onTargetClick(view);
					settings.IS_QUICK_ACTION_TUTORIAL_SHOWN.set(true);
				}
			});
			return true;
		}
		return false;
	}

	private void updateButtonMargin(@NonNull QuickActionButton button) {
		MapActivity activity = getMapActivity();
		if (activity != null) {
			QuickActionButtonState state = button.getButtonState();
			FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) button.getLayoutParams();
			if (AndroidUiHelper.isOrientationPortrait(activity)) {
				Pair<Integer, Integer> fabMargin = state.getFabMarginPref().getPortraitFabMargin();
				int defBottomMargin = calculateTotalSizePx(app, R.dimen.map_button_size, R.dimen.map_button_spacing) * 2;
				setFabButtonMargin(activity, button, params, fabMargin, 0, defBottomMargin);
			} else {
				Pair<Integer, Integer> fabMargin = state.getFabMarginPref().getLandscapeFabMargin();
				int defRightMargin = calculateTotalSizePx(app, R.dimen.map_button_size, R.dimen.map_button_spacing_land) * 2;
				setFabButtonMargin(activity, button, params, fabMargin, defRightMargin, 0);
			}
		}
	}

	private void updateButtonVisibility(@NonNull QuickActionButton button) {
		QuickActionButtonState buttonState = button.getButtonState();
		boolean visible = buttonState.isEnabled() && requireMapActivity().getWidgetsVisibilityHelper().shouldShowQuickActionButton();
		if (visible) {
			visible = app.getAppCustomization().isFeatureEnabled(buttonState.getId());
		}
		AndroidUiHelper.updateVisibility(button, visible);
	}
}
