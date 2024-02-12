package net.osmand.plus.views.layers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import net.osmand.plus.quickaction.MapButtonsHelper;
import net.osmand.plus.quickaction.MapButtonsHelper.QuickActionUpdatesListener;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickAction.QuickActionSelectionListener;
import net.osmand.plus.quickaction.QuickActionsWidget;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.enums.MapPosition;
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

		Context context = getContext();
		contextMarker = new ImageView(context);
		contextMarker.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT));
		contextMarker.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.map_pin_context_menu));
		contextMarker.setClickable(true);
		int width = contextMarker.getDrawable().getMinimumWidth();
		int height = contextMarker.getDrawable().getMinimumHeight();
		contextMarker.layout(0, 0, width, height);
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
				ImageButton view = (ImageButton) inflater.inflate(R.layout.map_quick_actions_button, container, false);
				container.addView(view);
				buttons.add(new QuickActionButton(activity, view, state));
			}
			actionButtons = buttons;
			mapButtonStates = buttonStates;
		}
	}

	public void refreshLayer() {
		setSelectedButton(null);
		isLayerOn = mapButtonsHelper.hasEnabledButtons();

		for (QuickActionButton button : actionButtons) {
			button.updateVisibility();
			if (isLayerOn) {
				button.setQuickActionButtonMargin();
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
		return isWidgetVisible() && selectedButton != null && Algorithms.stringsEqual(selectedButton.getId(), button.getId());
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
			actionButton.update(nightMode, false, false);
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
			button.update(nightMode, false, false);
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
		setSelectedButton(selectedButton);
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
}
