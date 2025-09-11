package net.osmand.plus.views.mapwidgets;

import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.ENABLED_MODE;
import static net.osmand.plus.views.mapwidgets.WidgetsVisibilityHelper.VisibleElements.*;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.exploreplaces.ExplorePlacesFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.MapFragmentsHelper;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.mapcontextmenu.MapContextMenuFragment;
import net.osmand.plus.mapcontextmenu.other.MapMultiSelectionMenu;
import net.osmand.plus.measurementtool.GpxApproximationFragment;
import net.osmand.plus.measurementtool.SnapTrackWarningFragment;
import net.osmand.plus.routepreparationmenu.MapRouteInfoMenu;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.track.fragments.TrackMenuFragment;
import net.osmand.plus.views.MapLayers;
import net.osmand.plus.views.layers.MapQuickActionLayer;
import net.osmand.util.CollectionUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class WidgetsVisibilityHelper {

	private final OsmandApplication app;
	private final RoutingHelper routingHelper;
	private final MapFragmentsHelper fragmentsHelper;

	private final MapActivity mapActivity;
	private final MapLayers mapLayers;

	public WidgetsVisibilityHelper(@NonNull MapActivity mapActivity) {
		this.mapActivity = mapActivity;
		app = mapActivity.getApp();
		routingHelper = app.getRoutingHelper();
		mapLayers = app.getOsmandMap().getMapLayers();
		fragmentsHelper = mapActivity.getFragmentsHelper();
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
				&& !isInTrackMenuMode()
				&& !isInRouteLineAppearanceMode()
				&& !isMapRouteInfoMenuVisible()
				&& !isInChoosingRoutesMode()
				&& !isInWaypointsChoosingMode()
				&& !isInFollowTrackMode()
				&& !isContextMenuFragmentVisible()
				&& !isMultiSelectionMenuFragmentVisible()
				&& shouldShowElementOnActiveScreen(FAB_BUTTON);
	}

	public boolean shouldShowTopCoordinatesWidget() {
		return !mapActivity.shouldHideTopControls()
				&& mapActivity.getMapRouteInfoMenu().shouldShowTopControls()
				&& !mapActivity.isTopToolbarActive()
				&& !isInRouteLineAppearanceMode()
				&& !isInChoosingRoutesMode()
				&& !isInWaypointsChoosingMode()
				&& !isInFollowTrackMode()
				&& !isInConfigureMapOptionMode()
				&& shouldShowElementOnActiveScreen(TOP_COORDINATES_WIDGET);
	}

	public boolean shouldShowBottomWidgets() {
		return isExplorePlacesPresentHidden() && !isContextMenuFragmentVisible();
	}

	public boolean shouldHideVerticalWidgets() {
		return isMapRouteInfoMenuVisible()
				|| isExplorePlacesMode()
				|| mapActivity.isTopToolbarActive()
				|| mapActivity.shouldHideTopControls()
				|| isInRouteLineAppearanceMode()
				|| isInConfigureMapOptionMode()
				|| !shouldShowElementOnActiveScreen(VERTICAL_WIDGETS);
	}

	public boolean shouldHideBottomWidgets() {
		return shouldHideVerticalWidgets()
				|| isContextMenuFragmentVisible()
				|| isInTrackMenuMode();
	}

	public boolean shouldShowBottomMenuButtons() {
		return !mapActivity.shouldHideTopControls()
				&& !isInMovingMarkerMode()
				&& !isInGpxDetailsMode()
				&& !isInChoosingRoutesMode()
				&& !isInWaypointsChoosingMode()
				&& !isInFollowTrackMode()
				&& !isInRouteLineAppearanceMode()
				&& !isInConfigureMapOptionMode()
				&& !isContextMenuFragmentVisible()
				&& shouldShowElementOnActiveScreen(BOTTOM_MENU_BUTTONS);
	}

	public boolean shouldShowZoomButtons() {
		boolean screensAllowed = shouldShowElementOnActiveScreen(ZOOM_BUTTONS);
		boolean additionalDialogsHide = !isInGpxApproximationMode()
				&& !isInChoosingRoutesMode()
				&& !isInWaypointsChoosingMode()
				&& !isInRouteLineAppearanceMode()
				&& !isContextMenuFragmentVisible()
				&& screensAllowed;
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
				|| isInChoosingRoutesMode()
				|| isInWaypointsChoosingMode()
				|| isInFollowTrackMode()
				|| isInRouteLineAppearanceMode()
				|| !shouldShowElementOnActiveScreen(COMPASS);
	}

	public boolean shouldShowTopButtons() {
		return !mapActivity.shouldHideTopControls()
				&& !isInAttachToRoads()
				&& !isTrackDetailsMenuOpened()
				&& !isInChoosingRoutesMode()
				&& !isInWaypointsChoosingMode()
				&& !isInFollowTrackMode()
				&& !isInRouteLineAppearanceMode()
				&& !isInConfigureMapOptionMode()
				&& shouldShowElementOnActiveScreen(TOP_BUTTONS);
	}

	public boolean shouldShowBackToLocationButton() {
		boolean screensAllowed = shouldShowElementOnActiveScreen(BACK_TO_LOCATION_BUTTON);
		boolean additionalDialogsHide = !isInGpxApproximationMode()
				&& !isInChoosingRoutesMode()
				&& !isInWaypointsChoosingMode()
				&& !isInFollowTrackMode()
				&& !isInRouteLineAppearanceMode()
				&& !isContextMenuFragmentVisible()
				&& screensAllowed;
		boolean showTopControls = !mapActivity.shouldHideTopControls()
				|| (isInTrackMenuMode() && !isPortrait());
		return showTopControls
				&& !isInConfigureMapOptionMode()
				&& !(isMapLinkedToLocation() && routingHelper.isFollowingMode())
				&& (additionalDialogsHide || !isPortrait());
	}

	public boolean shouldShowElevationProfileWidget() {
		return isRouteCalculated() && WidgetType.ELEVATION_PROFILE.isPurchased(app)
				&& !isInChangeMarkerPositionMode()
				&& !isInChoosingRoutesMode()
				&& !isInWaypointsChoosingMode()
				&& !isTrackDetailsMenuOpened()
				&& shouldShowElementOnActiveScreen(ELEVATION_PROFILE_WIDGET);
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

	public boolean shouldShowSuggestMapBanner() {
		return !isInRouteLineAppearanceMode()
				&& !isInConfigureMapOptionMode()
				&& shouldShowElementOnActiveScreen(SUGGEST_MAP_BANNER);
	}

	public boolean shouldShowSpeedometer() {
		return shouldShowElementOnActiveScreen(SPEEDOMETER);
	}

	public static boolean isWidgetEnabled(@NonNull MapActivity activity,
			@NonNull WidgetsPanel panel, @NonNull String... widgetsIds) {
		OsmandApplication app = activity.getApp();
		ApplicationMode appMode = app.getSettings().getApplicationMode();

		MapWidgetRegistry widgetRegistry = app.getOsmandMap().getMapLayers().getMapWidgetRegistry();
		Set<MapWidgetInfo> enabledWidgets = widgetRegistry.getWidgetsForPanel(activity, appMode, ENABLED_MODE, Collections.singletonList(panel));

		for (MapWidgetInfo widgetInfo : enabledWidgets) {
			if (CollectionUtils.containsAny(widgetInfo.key, widgetsIds)) {
				return true;
			}
		}
		return false;
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
		TrackMenuFragment fragment = fragmentsHelper.getTrackMenuFragment();
		return fragment != null && fragment.isVisible();
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
		return fragmentsHelper.getGpsFilterFragment() != null;
	}

	private boolean isExplorePlacesMode() {
		ExplorePlacesFragment fragment = fragmentsHelper.getExplorePlacesFragment();
		return fragment != null && !fragment.isListHidden();
	}

	private boolean isExplorePlacesPresentHidden() {
		ExplorePlacesFragment fragment = fragmentsHelper.getExplorePlacesFragment();
		return fragment != null && (fragment.isListHidden() || fragment.isHidden());
	}

	private boolean isSelectMapLocationMode() {
		return fragmentsHelper.getSelectMapLocationFragment() != null;
	}

	public boolean isInConfigureMapOptionMode() {
		return fragmentsHelper.getConfigureMapOptionFragment() != null;
	}

	private boolean isInWeatherForecastMode() {
		return fragmentsHelper.getWeatherForecastFragment() != null;
	}

	private boolean isSelectingTilesZone() {
		return fragmentsHelper.getDownloadTilesFragment() != null;
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

	public void hideWidgets() {
		updateWidgetsVisibility(false);
	}

	public void showWidgets() {
		updateWidgetsVisibility(true);
	}

	public void updateControlsVisibility(boolean topControlsVisible,
			boolean bottomControlsVisible) {
		updateWidgetsVisibility(topControlsVisible);
		updateBottomControlsVisibility(bottomControlsVisible);
	}

	public void updateWidgetsVisibility(boolean visible) {
		int visibility = visible ? View.VISIBLE : View.GONE;
		AndroidUiHelper.setVisibility(mapActivity, visibility,
				R.id.map_center_info,
				R.id.map_left_widgets_panel,
				R.id.map_right_widgets_panel);
	}

	public void updateBottomControlsVisibility(boolean visible) {
		int bottomControlsVisibility = visible ? View.VISIBLE : View.GONE;
		AndroidUiHelper.setVisibility(mapActivity, bottomControlsVisibility,
				R.id.bottom_controls_container);
	}

	private boolean shouldShowElementOnActiveScreen(VisibleElements element) {
		for (VisibilityScreens screen : VisibilityScreens.values()) {
			if (screen.isVisibleInMode(this)) {
				return screen.visibleElements.contains(element);
			}
		}
		return true;
	}

	public enum VisibleElements {
		QUICK_ACTION_BUTTONS,
		MAP_3D_BUTTON,
		TOP_COORDINATES_WIDGET,
		VERTICAL_WIDGETS,
		BOTTOM_MENU_BUTTONS,
		ZOOM_BUTTONS,
		COMPASS,
		TOP_BUTTONS,
		BACK_TO_LOCATION_BUTTON,
		ELEVATION_PROFILE_WIDGET,
		SUGGEST_MAP_BANNER,
		FAB_BUTTON,
		SPEEDOMETER
	}

	public enum VisibilityScreens {
		EXPLORE_PLACES(),
		WEATHER_FORECAST(ZOOM_BUTTONS, BACK_TO_LOCATION_BUTTON),
		MEASUREMENT_MODE(ZOOM_BUTTONS, BACK_TO_LOCATION_BUTTON, SUGGEST_MAP_BANNER, TOP_BUTTONS, COMPASS),
		PLAN_ROUTE_MODE(TOP_COORDINATES_WIDGET, SUGGEST_MAP_BANNER),
		TRACK_APPEARANCE_MODE(SUGGEST_MAP_BANNER),
		SELECTING_TILES_ZONE_MODE(),
		GPS_FILTERING_MODE(),
		SELECT_MAP_LOCATION(ZOOM_BUTTONS, BACK_TO_LOCATION_BUTTON);

		public final List<VisibleElements> visibleElements = new ArrayList<>();

		VisibilityScreens(@NonNull VisibleElements... visibleElements) {
			this.visibleElements.addAll(Arrays.asList(visibleElements));
		}

		boolean isVisibleInMode(@NonNull WidgetsVisibilityHelper helper) {
			return switch (this) {
				case WEATHER_FORECAST -> helper.isInWeatherForecastMode();
				case MEASUREMENT_MODE -> helper.isInMeasurementToolMode();
				case PLAN_ROUTE_MODE -> helper.isInPlanRouteMode();
				case TRACK_APPEARANCE_MODE -> helper.isInTrackAppearanceMode();
				case SELECTING_TILES_ZONE_MODE -> helper.isSelectingTilesZone();
				case GPS_FILTERING_MODE -> helper.isInGpsFilteringMode();
				case EXPLORE_PLACES -> helper.isExplorePlacesMode();
				case SELECT_MAP_LOCATION -> helper.isSelectMapLocationMode();
			};
		}
	}
}
