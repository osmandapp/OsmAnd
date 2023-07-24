package net.osmand.plus.views.mapwidgets;

import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.ENABLED_MODE;
import static net.osmand.plus.views.mapwidgets.WidgetType.MARKERS_TOP_BAR;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.mapcontextmenu.MapContextMenuFragment;
import net.osmand.plus.mapcontextmenu.other.MapMultiSelectionMenu;
import net.osmand.plus.measurementtool.GpxApproximationFragment;
import net.osmand.plus.measurementtool.SnapTrackWarningFragment;
import net.osmand.plus.routepreparationmenu.MapRouteInfoMenu;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.views.MapLayers;
import net.osmand.plus.views.layers.MapQuickActionLayer;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Set;

public class WidgetsVisibilityHelper {

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final RoutingHelper routingHelper;

	private final MapActivity mapActivity;
	private final MapLayers mapLayers;

	public WidgetsVisibilityHelper(@NonNull MapActivity mapActivity) {
		this.mapActivity = mapActivity;
		app = mapActivity.getMyApplication();
		settings = app.getSettings();
		routingHelper = app.getRoutingHelper();
		mapLayers = app.getOsmandMap().getMapLayers();
	}

	public boolean shouldShowQuickActionButton() {
		return isQuickActionLayerOn()
				&& !isInConfigureMapOptionMode()
				&& shouldShowFabButton();
	}

	public boolean shouldShowMap3DButton() {
		return isInConfigureMapOptionMode()
				|| shouldShowFabButton();
	}

	public boolean shouldShowFabButton() {
		return !isInChangeMarkerPositionMode()
				&& !isInGpxDetailsMode()
				&& !isInMeasurementToolMode()
				&& !isInPlanRouteMode()
				&& !isInTrackAppearanceMode()
				&& !isInTrackMenuMode()
				&& !isInRouteLineAppearanceMode()
				&& !isMapRouteInfoMenuVisible()
				&& !isInChoosingRoutesMode()
				&& !isInWaypointsChoosingMode()
				&& !isInFollowTrackMode()
				&& !isContextMenuFragmentVisible()
				&& !isMultiSelectionMenuFragmentVisible()
				&& !isInGpsFilteringMode()
				&& !isInWeatherForecastMode()
				&& !isSelectingTilesZone();
	}

	public boolean shouldShowTopMapCenterCoordinatesWidget() {
		return (settings.SHOW_MAP_CENTER_COORDINATES_WIDGET.get())
				&& shouldShowTopCoordinatesWidget();
	}

	public boolean shouldShowTopCurrentLocationCoordinatesWidget() {
		return settings.SHOW_CURRENT_LOCATION_COORDINATES_WIDGET.get()
				&& shouldShowTopCoordinatesWidget();
	}

	private boolean shouldShowTopCoordinatesWidget() {
		return !mapActivity.shouldHideTopControls()
				&& mapActivity.getMapRouteInfoMenu().shouldShowTopControls()
				&& !mapActivity.isTopToolbarActive()
				&& !isInTrackAppearanceMode()
				&& !isInRouteLineAppearanceMode()
				&& !isInChoosingRoutesMode()
				&& !isInWaypointsChoosingMode()
				&& !isInFollowTrackMode()
				&& !isInGpsFilteringMode()
				&& !isInWeatherForecastMode()
				&& !isSelectingTilesZone()
				&& !isInConfigureMapOptionMode();
	}

	public boolean shouldHideMapMarkersWidget() {
		return isMapRouteInfoMenuVisible()
				|| mapActivity.isTopToolbarActive()
				|| mapActivity.shouldHideTopControls()
				|| isInTrackAppearanceMode()
				|| isInPlanRouteMode()
				|| isInRouteLineAppearanceMode()
				|| isInGpsFilteringMode()
				|| isInWeatherForecastMode()
				|| isSelectingTilesZone()
				|| isInConfigureMapOptionMode();
	}

	public static boolean isMapMarkerBarWidgetEnabled(@NonNull MapActivity mapActivity) {
		OsmandApplication app = mapActivity.getMyApplication();
		ApplicationMode appMode = app.getSettings().getApplicationMode();
		List<WidgetsPanel> panels = WidgetsPanel.TOP.getMergedPanels();

		MapWidgetRegistry widgetRegistry = app.getOsmandMap().getMapLayers().getMapWidgetRegistry();
		Set<MapWidgetInfo> enabledWidgets = widgetRegistry.getWidgetsForPanel(mapActivity, appMode, ENABLED_MODE, panels);

		for (MapWidgetInfo widgetInfo : enabledWidgets) {
			if (widgetInfo.key.contains(MARKERS_TOP_BAR.id)) {
				return true;
			}
		}
		return false;
	}

	public boolean shouldShowBottomMenuButtons() {
		return !mapActivity.shouldHideTopControls()
				&& !isInMovingMarkerMode()
				&& !isInGpxDetailsMode()
				&& !isInMeasurementToolMode()
				&& !isInPlanRouteMode()
				&& !isInChoosingRoutesMode()
				&& !isInWaypointsChoosingMode()
				&& !isInFollowTrackMode()
				&& !isInTrackAppearanceMode()
				&& !isInRouteLineAppearanceMode()
				&& !isInGpsFilteringMode()
				&& !isInWeatherForecastMode()
				&& !isSelectingTilesZone()
				&& !isInConfigureMapOptionMode();
	}

	public boolean shouldShowZoomButtons() {
		boolean additionalDialogsHide = !isInGpxApproximationMode()
				&& !isInTrackAppearanceMode()
				&& !isInChoosingRoutesMode()
				&& !isInWaypointsChoosingMode()
				&& !isInRouteLineAppearanceMode()
				&& !isInGpsFilteringMode()
				&& !isSelectingTilesZone();
		boolean showTopControls = !mapActivity.shouldHideTopControls()
				|| (isInTrackMenuMode() && !isPortrait());
		return showTopControls
				&& !isInFollowTrackMode()
				&& !isInConfigureMapOptionMode()
				&& (additionalDialogsHide || !isPortrait());
	}

	public boolean shouldHideCompass() {
		return mapActivity.shouldHideTopControls()
				|| isTrackDetailsMenuOpened()
				|| isInPlanRouteMode()
				|| isInChoosingRoutesMode()
				|| isInTrackAppearanceMode()
				|| isInWaypointsChoosingMode()
				|| isInFollowTrackMode()
				|| isInRouteLineAppearanceMode()
				|| isInGpsFilteringMode()
				|| isSelectingTilesZone();
	}

	public boolean shouldShowTopButtons() {
		return !mapActivity.shouldHideTopControls()
				&& !isInAttachToRoads()
				&& !isTrackDetailsMenuOpened()
				&& !isInPlanRouteMode()
				&& !isInChoosingRoutesMode()
				&& !isInTrackAppearanceMode()
				&& !isInWaypointsChoosingMode()
				&& !isInFollowTrackMode()
				&& !isInRouteLineAppearanceMode()
				&& !isInGpsFilteringMode()
				&& !isInWeatherForecastMode()
				&& !isSelectingTilesZone()
				&& !isInConfigureMapOptionMode();
	}

	public boolean shouldShowBackToLocationButton() {
		boolean additionalDialogsHide = !isInTrackAppearanceMode()
				&& !isInGpxApproximationMode()
				&& !isInChoosingRoutesMode()
				&& !isInWaypointsChoosingMode()
				&& !isInFollowTrackMode()
				&& !isInRouteLineAppearanceMode()
				&& !isInGpsFilteringMode()
				&& !isSelectingTilesZone();
		boolean showTopControls = !mapActivity.shouldHideTopControls()
				|| (isInTrackMenuMode() && !isPortrait());
		return showTopControls
				&& !isInPlanRouteMode()
				&& !isInConfigureMapOptionMode()
				&& !(isMapLinkedToLocation() && routingHelper.isFollowingMode())
				&& (additionalDialogsHide || !isPortrait());
	}

	public boolean shouldShowElevationProfileWidget() {
		return isRouteCalculated() && WidgetType.ELEVATION_PROFILE.isPurchased(app)
				&& !isInChangeMarkerPositionMode()
				&& !isInMeasurementToolMode()
				&& !isInChoosingRoutesMode()
				&& !isInWaypointsChoosingMode()
				&& !isInPlanRouteMode()
				&& !isSelectingTilesZone()
				&& !isTrackDetailsMenuOpened();
		/*
				&& !isDashboardVisible()
				&& !isInGpxDetailsMode()
				&& !isInTrackAppearanceMode()
				&& !isInTrackMenuMode()
				&& !isMapRouteInfoMenuVisible()
				&& !isInRouteLineAppearanceMode()
				&& !isInFollowTrackMode()
				&& !isContextMenuFragmentVisible()
				&& !isMultiSelectionMenuFragmentVisible();
		 */
	}

	public boolean shouldShowDownloadMapWidget() {
		return !isInRouteLineAppearanceMode()
				&& !isInGpsFilteringMode()
				&& !isInWeatherForecastMode()
				&& !isSelectingTilesZone()
				&& !isInConfigureMapOptionMode();
	}

	private boolean isQuickActionLayerOn() {
		return mapLayers.getMapQuickActionLayer().isLayerOn();
	}

	private boolean isMapRouteInfoMenuVisible() {
		return mapActivity.getMapRouteInfoMenu().isVisible();
	}

	private boolean isInMovingMarkerMode() {
		MapQuickActionLayer quickActionLayer = mapLayers.getMapQuickActionLayer();
		boolean isInMovingMarkerMode = quickActionLayer != null && quickActionLayer.isInMovingMarkerMode();
		return isInMovingMarkerMode || isInChangeMarkerPositionMode() || isInAddGpxPointMode();
	}

	private boolean isInGpxDetailsMode() {
		return mapLayers.getContextMenuLayer().isInGpxDetailsMode();
	}

	private boolean isInAddGpxPointMode() {
		return mapLayers.getContextMenuLayer().isInAddGpxPointMode();
	}

	private boolean isInChangeMarkerPositionMode() {
		return mapLayers.getContextMenuLayer().isInChangeMarkerPositionMode();
	}

	private boolean isInMeasurementToolMode() {
		return mapLayers.getMeasurementToolLayer().isInMeasurementMode();
	}

	private boolean isInAttachToRoads() {
		if (isInMeasurementToolMode()) {
			for (Fragment fragment : mapActivity.getSupportFragmentManager().getFragments()) {
				if (fragment instanceof SnapTrackWarningFragment || fragment instanceof GpxApproximationFragment) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean isInPlanRouteMode() {
		return mapLayers.getMapMarkersLayer().isInPlanRouteMode();
	}

	private boolean isInTrackAppearanceMode() {
		return mapLayers.getGpxLayer().isInTrackAppearanceMode();
	}

	private boolean isInGpxApproximationMode() {
		return mapLayers.getMeasurementToolLayer().isTapsDisabled();
	}

	public boolean isInTrackMenuMode() {
		return mapActivity.getTrackMenuFragment() != null && mapActivity.getTrackMenuFragment().isVisible();
	}

	private boolean isInChoosingRoutesMode() {
		return MapRouteInfoMenu.chooseRoutesVisible;
	}

	private boolean isInWaypointsChoosingMode() {
		return MapRouteInfoMenu.waypointsVisible;
	}

	private boolean isInRouteLineAppearanceMode() {
		return mapLayers.getRouteLayer().isPreviewRouteLineVisible();
	}

	private boolean isInFollowTrackMode() {
		return MapRouteInfoMenu.followTrackVisible;
	}

	public boolean isDashboardVisible() {
		return mapActivity.getDashboard().isVisible();
	}

	private boolean isContextMenuFragmentVisible() {
		MapContextMenu contextMenu = mapActivity.getContextMenu();
		WeakReference<MapContextMenuFragment> contextMenuMenuFragmentRef = contextMenu.findMenuFragment();
		MapContextMenuFragment contextMenuMenuFragment = contextMenuMenuFragmentRef != null ? contextMenuMenuFragmentRef.get() : null;
		if (contextMenuMenuFragment != null && contextMenu.isVisible()) {
			return !contextMenuMenuFragment.isRemoving() || contextMenuMenuFragment.isAdded();
		}
		return false;
	}

	private boolean isMultiSelectionMenuFragmentVisible() {
		MapContextMenu contextMenu = mapActivity.getContextMenu();
		MapMultiSelectionMenu multiSelectionMenu = contextMenu.getMultiSelectionMenu();
		Fragment multiMenuFragment = multiSelectionMenu.getFragmentByTag();
		if (multiMenuFragment != null && multiSelectionMenu.isVisible()) {
			return multiMenuFragment.isAdded() || !multiMenuFragment.isRemoving();
		}
		return false;
	}

	private boolean isInGpsFilteringMode() {
		return mapActivity.getGpsFilterFragment() != null;
	}

	public boolean isInConfigureMapOptionMode() {
		return mapActivity.getConfigureMapOptionFragment() != null;
	}

	private boolean isInWeatherForecastMode() {
		return mapActivity.getWeatherForecastFragment() != null;
	}

	private boolean isSelectingTilesZone() {
		return mapActivity.getDownloadTilesFragment() != null;
	}

	private boolean isMapLinkedToLocation() {
		return mapActivity.getMapViewTrackingUtilities().isMapLinkedToLocation();
	}

	private boolean isTrackDetailsMenuOpened() {
		return mapActivity.getTrackDetailsMenu().isVisible();
	}

	private boolean isRouteCalculated() {
		return mapActivity.getRoutingHelper().isRouteCalculated();
	}

	private boolean isPortrait() {
		return AndroidUiHelper.isOrientationPortrait(mapActivity);
	}

	public void updateControlsVisibility(boolean topControlsVisible, boolean bottomControlsVisible) {
		int topControlsVisibility = topControlsVisible ? View.VISIBLE : View.GONE;
		AndroidUiHelper.setVisibility(mapActivity, topControlsVisibility,
				R.id.map_center_info,
				R.id.map_left_widgets_panel,
				R.id.map_right_widgets_panel);
		int bottomControlsVisibility = bottomControlsVisible ? View.VISIBLE : View.GONE;
		AndroidUiHelper.setVisibility(mapActivity, bottomControlsVisibility,
				R.id.bottom_controls_container);
	}
}
