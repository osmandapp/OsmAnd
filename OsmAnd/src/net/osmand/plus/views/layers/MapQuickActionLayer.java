package net.osmand.plus.views.layers;

import static android.view.Gravity.BOTTOM;
import static android.view.Gravity.END;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
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
import java.util.Objects;

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
		contextMarker.setLayoutParams(new FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
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
			ViewGroup container = activity.findViewById(R.id.MapHudButtonsOverlay);
			for (QuickActionButton button : actionButtons) {
				container.removeView(button);
			}

			List<QuickActionButton> buttons = new ArrayList<>();
			List<QuickActionButtonState> buttonStates = mapButtonsHelper.getButtonsStates();
			for (QuickActionButtonState state : buttonStates) {
				QuickActionButton button = (QuickActionButton) inflater.inflate(R.layout.map_quick_actions_button, container, false);
				button.setButtonState(state);
				button.setMapActivity(activity);
				button.setUseCustomPosition(true);

				container.addView(button, new FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, BOTTOM | END));
				buttons.add(button);
			}
			actionButtons = buttons;
			mapButtonStates = buttonStates;
		}
	}

	@NonNull
	public List<QuickActionButton> getActionButtons() {
		return actionButtons;
	}

	public void refreshLayer() {
		setSelectedButton(null);
		isLayerOn = mapButtonsHelper.hasEnabledButtons();

		for (QuickActionButton button : actionButtons) {
			button.update();
			if (isLayerOn) {
				button.updateMargins();
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
		return isWidgetVisibleForButton(button.getButtonId());
	}

	public boolean isWidgetVisibleForButton(@NonNull String buttonId) {
		return isWidgetVisible() && selectedButton != null
				&& Algorithms.stringsEqual(selectedButton.getButtonId(), buttonId);
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

		for (QuickActionButton actionButton : actionButtons) {
			updateButton(actionButton, true);
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
		for (QuickActionButton button : actionButtons) {
			updateButton(button, false);
		}
	}

	public boolean getCurrentWidgetState() {
		return currentWidgetState != null ? currentWidgetState : false;
	}

	@Override
	public boolean drawInScreenPixels() {
		return true;
	}

	public void invalidateRelatedButtons(@NonNull QuickActionButton trigger) {
		for (QuickActionButton actionButton : getActionButtons()) {
			if (isActionButtonsRelated(actionButton, trigger)) {
				actionButton.setInvalidated(true);
			}
		}
	}

	private boolean isActionButtonsRelated(@NonNull QuickActionButton b1,
	                                       @NonNull QuickActionButton b2) {
		QuickActionButtonState s1 = b1.getButtonState();
		QuickActionButtonState s2 = b2.getButtonState();
		if (s1 != null && s2 != null) {
			if (s1.isSingleAction() && s2.isSingleAction()) {
				QuickAction a1 = s1.getQuickActions().get(0);
				QuickAction a2 = s2.getQuickActions().get(0);
				return Objects.equals(a1.getType(), a2.getType());

				// There also can be types those have related UI elements and so should be
				// updated at the same time. We should implement such functionality.
				// For example, we can create a collection with nodes, each node is a list
				// of action types that are related to each other.
			} else {
				return Objects.equals(s1.getId(), s2.getId());
			}
		}
		return false;
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

	private void updateButton(@NonNull QuickActionButton button, boolean invalidated) {
		button.setInvalidated(invalidated);
		button.setNightMode(app.getDaynightHelper().isNightMode());
		button.update();
	}
}
