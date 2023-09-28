package net.osmand.plus.views.layers;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.BACK_TO_LOC_HUD_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_3D_HUD_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.ZOOM_IN_HUD_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.ZOOM_OUT_HUD_ID;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.slider.Slider;

import net.osmand.core.android.MapRendererContext;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmAndLocationSimulation;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.auto.NavigationSession;
import net.osmand.plus.base.ContextMenuFragment.MenuState;
import net.osmand.plus.dialogs.DirectionsDialogs;
import net.osmand.plus.helpers.TargetPointsHelper;
import net.osmand.plus.helpers.TargetPointsHelper.TargetPoint;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.rastermaps.LayerTransparencySeekbarMode;
import net.osmand.plus.plugins.rastermaps.OsmandRasterMapsPlugin;
import net.osmand.plus.routepreparationmenu.MapRouteInfoMenu;
import net.osmand.plus.routepreparationmenu.MapRouteInfoMenu.PointType;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.search.QuickSearchDialogFragment.QuickSearchType;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.MapActions;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.controls.maphudbuttons.CompassButton;
import net.osmand.plus.views.controls.maphudbuttons.ConfigureMapButton;
import net.osmand.plus.views.controls.maphudbuttons.DrawerMenuButton;
import net.osmand.plus.views.controls.maphudbuttons.Map3DButton;
import net.osmand.plus.views.controls.maphudbuttons.MapButton;
import net.osmand.plus.views.controls.maphudbuttons.MyLocationButton;
import net.osmand.plus.views.controls.maphudbuttons.NavigationMenuButton;
import net.osmand.plus.views.controls.maphudbuttons.QuickSearchButton;
import net.osmand.plus.views.controls.maphudbuttons.ZoomInButton;
import net.osmand.plus.views.controls.maphudbuttons.ZoomOutButton;
import net.osmand.plus.views.corenative.NativeCoreContext;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.plus.views.mapwidgets.WidgetsVisibilityHelper;
import net.osmand.gpx.GPXFile;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import gnu.trove.list.array.TIntArrayList;

public class MapControlsLayer extends OsmandMapLayer {

	private static final int TIMEOUT_TO_SHOW_BUTTONS = 7000;
	private static final int REQUEST_LOCATION_FOR_NAVIGATION_PERMISSION = 200;
	private static final int REQUEST_LOCATION_FOR_NAVIGATION_FAB_PERMISSION = 201;
	private static final int REQUEST_LOCATION_FOR_ADD_DESTINATION_PERMISSION = 202;

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final OsmandMapTileView mapView;

	private List<MapButton> mapButtons = new ArrayList<>();
	private CompassButton compassButton;
	private Map3DButton map3DButton;

	private LinearLayout transparencyBarLayout;
	private Slider transparencySlider;
	private static CommonPreference<Integer> transparencySetting;

	private LinearLayout parameterBarLayout;
	private Slider parameterSlider;
	private static CommonPreference<Float> parameterMinSetting;
	private static CommonPreference<Float> parameterMaxSetting;
	private static CommonPreference<Float> parameterStepSetting;
	private static CommonPreference<Float> parameterValueSetting;

	private MapRouteInfoMenu mapRouteInfoMenu;
	private TextView zoomText;
	private long touchEvent;
	private LatLon requestedLatLon;
	private final Set<String> themeInfoProviderTags = new HashSet<>();
	private WidgetsVisibilityHelper vh;

	public MapControlsLayer(@NonNull Context context) {
		super(context);
		app = getApplication();
		settings = app.getSettings();
		mapView = app.getOsmandMap().getMapView();
	}

	@Override
	public boolean drawInScreenPixels() {
		return true;
	}

	@Override
	public void initLayer(@NonNull OsmandMapTileView view) {
		super.initLayer(view);
	}

	@Override
	public void setMapActivity(@Nullable MapActivity mapActivity) {
		super.setMapActivity(mapActivity);
		if (mapActivity != null) {
			mapRouteInfoMenu = mapActivity.getMapRouteInfoMenu();
			vh = mapActivity.getWidgetsVisibilityHelper();
			initTopControls();
			initFabButtons(mapActivity);
			initTransparencyBar();
			initZooms();
			initDashboardRelatedControls();
			updateControls(mapView.getCurrentRotatedTileBox(), null);
		} else {
			mapButtons = new ArrayList<>();
			transparencySlider = null;
			transparencyBarLayout = null;
			parameterSlider = null;
			parameterBarLayout = null;
			mapRouteInfoMenu = null;
			compassButton = null;
			zoomText = null;
			if (map3DButton != null) {
				map3DButton.onDestroyButton();
			}
			map3DButton = null;
		}
	}

	public View moveCompassButton(ViewGroup destLayout, ViewGroup.LayoutParams layoutParams) {
		return compassButton.moveToSpecialPosition(destLayout, layoutParams);
	}

	public void moveMap3DButton(ViewGroup destLayout, ViewGroup.LayoutParams layoutParams) {
		if (map3DButton != null) {
			map3DButton.moveToSpecialPosition(destLayout, layoutParams);
		}
	}

	public void restoreCompassButton() {
		compassButton.moveToDefaultPosition();
	}

	public void restoreMap3DButton() {
		if (map3DButton != null) {
			map3DButton.restoreSavedPosition();
		}
	}

	private void initTopControls() {
		MapActivity mapActivity = requireMapActivity();

		mapButtons.add(new ConfigureMapButton(mapActivity));
		mapButtons.add(new QuickSearchButton(mapActivity));

		compassButton = new CompassButton(mapActivity);
		mapButtons.add(compassButton);
	}

	private void initFabButtons(MapActivity mapActivity) {
		ImageView map3DButtonView = mapActivity.findViewById(R.id.map_3d_button);
		if (map3DButton != null) {
			map3DButton.onDestroyButton();
		}
		map3DButton = new Map3DButton(mapActivity, map3DButtonView, MAP_3D_HUD_ID);
		mapButtons.add(map3DButton);
	}

	public void setControlsClickable(boolean clickable) {
		for (MapButton mapButton : mapButtons) {
			mapButton.view.setClickable(clickable);
		}
	}

	private TargetPointsHelper getTargets() {
		return app.getTargetPointsHelper();
	}

	public void stopNavigation() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapRouteInfoMenu.hide();
			if (app.getRoutingHelper().isFollowingMode()) {
				mapActivity.getMapActions().stopNavigationActionConfirm(null);
			} else {
				mapActivity.getMapActions().stopNavigationWithoutConfirm();
			}
		} else {
			app.getOsmandMap().getMapActions().stopNavigationWithoutConfirm();
		}
	}

	public void stopNavigationWithoutConfirm() {
		MapActivity mapActivity = requireMapActivity();
		mapRouteInfoMenu.hide();
		mapActivity.getMapActions().stopNavigationWithoutConfirm();
	}

	public void showRouteInfoControlDialog() {
		mapRouteInfoMenu.showHideMenu();
	}

	public void showRouteInfoMenu() {
		mapRouteInfoMenu.setShowMenu(MapRouteInfoMenu.DEFAULT_MENU_STATE);
	}

	public void initDashboardRelatedControls() {
		MapActivity mapActivity = requireMapActivity();
		ImageView backToLocation = mapActivity.findViewById(R.id.map_my_location_button);

		mapButtons.add(new DrawerMenuButton(mapActivity));
		mapButtons.add(new NavigationMenuButton(mapActivity));
		mapButtons.add(new MyLocationButton(mapActivity, backToLocation, BACK_TO_LOC_HUD_ID, true));

		zoomText = mapActivity.findViewById(R.id.map_app_mode_text);
	}

	public void doRoute() {
		onNavigationClick();
	}

	public void doNavigate() {
		mapRouteInfoMenu.hide();
		startNavigation();
	}

	private void onNavigationClick() {
		MapActivity mapActivity = requireMapActivity();
		if (mapRouteInfoMenu != null) {
			mapRouteInfoMenu.cancelSelectionFromMap();
		}
		MapActivity.clearPrevActivityIntent();
		RoutingHelper routingHelper = app.getRoutingHelper();
		if (!routingHelper.isFollowingMode() && !routingHelper.isRoutePlanningMode()) {
			mapRouteInfoMenu.clearSuggestedMissingMaps();
			TargetPoint start = getTargets().getPointToStart();
			if (start != null) {
				mapActivity.getMapActions().enterRoutePlanningMode(
						new LatLon(start.getLatitude(), start.getLongitude()), start.getOriginalPointDescription());
			} else {
				mapActivity.getMapActions().enterRoutePlanningMode(null, null);
			}
		} else {
			showRouteInfoControlDialog();
		}
	}

	public void navigateButton() {
		MapActivity mapActivity = requireMapActivity();
		if (!OsmAndLocationProvider.isLocationPermissionAvailable(mapActivity)) {
			ActivityCompat.requestPermissions(mapActivity,
					new String[] {Manifest.permission.ACCESS_FINE_LOCATION,
							Manifest.permission.ACCESS_COARSE_LOCATION},
					REQUEST_LOCATION_FOR_NAVIGATION_FAB_PERMISSION);
		} else {
			MapContextMenu menu = mapActivity.getContextMenu();
			LatLon latLon = menu.getLatLon();
			PointDescription pointDescription = menu.getPointDescriptionForTarget();
			menu.hide();

			TargetPointsHelper targets = app.getTargetPointsHelper();
			RoutingHelper routingHelper = app.getRoutingHelper();
			if (routingHelper.isFollowingMode() || routingHelper.isRoutePlanningMode()) {
				DirectionsDialogs.addWaypointDialogAndLaunchMap(mapActivity, latLon.getLatitude(),
						latLon.getLongitude(), pointDescription);
			} else if (targets.getIntermediatePoints().isEmpty()) {
				startRoutePlanningWithDestination(latLon, pointDescription, targets);
				menu.close();
			} else {
				AlertDialog.Builder bld = new AlertDialog.Builder(mapActivity);
				bld.setTitle(R.string.new_directions_point_dialog);
				int[] defaultVls = {0};
				bld.setSingleChoiceItems(new String[] {
						mapActivity.getString(R.string.clear_intermediate_points),
						mapActivity.getString(R.string.keep_intermediate_points)
				}, 0, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						defaultVls[0] = which;
					}
				});
				bld.setPositiveButton(R.string.shared_string_ok, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (defaultVls[0] == 0) {
							targets.removeAllWayPoints(false, true);
						}
						targets.navigateToPoint(latLon, true, -1, pointDescription);
						mapActivity.getMapActions().enterRoutePlanningModeGivenGpx(null, null, null, true, true, MenuState.HEADER_ONLY);
						menu.close();
					}
				});
				bld.setNegativeButton(R.string.shared_string_cancel, null);
				bld.show();
			}
		}
	}

	private void startRoutePlanningWithDestination(LatLon latLon, PointDescription pointDescription, TargetPointsHelper targets) {
		MapActivity mapActivity = getMapActivity();
		MapActions mapActions = mapActivity != null ? mapActivity.getMapActions() : app.getOsmandMap().getMapActions();
		boolean hasPointToStart = settings.restorePointToStart();
		targets.navigateToPoint(latLon, true, -1, pointDescription);
		if (!hasPointToStart) {
			mapActions.enterRoutePlanningModeGivenGpx(null, null, null, true, true, MenuState.HEADER_ONLY);
		} else {
			TargetPoint start = targets.getPointToStart();
			if (start != null) {
				mapActions.enterRoutePlanningModeGivenGpx(null, start.point, start.getOriginalPointDescription(), true, true, MenuState.HEADER_ONLY);
			} else {
				mapActions.enterRoutePlanningModeGivenGpx(null, null, null, true, true, MenuState.HEADER_ONLY);
			}
		}
	}

	public void buildRouteByGivenGpx(GPXFile gpxFile) {
		MapActivity mapActivity = getMapActivity();
		MapActions mapActions = mapActivity != null ? mapActivity.getMapActions() : app.getOsmandMap().getMapActions();
		mapActions.enterRoutePlanningModeGivenGpx(gpxFile, null, null, true, true, MenuState.HEADER_ONLY);
	}

	private PointDescription getPointDescriptionForTarget(@NonNull LatLon latLon) {
		return getPointDescriptionForTarget(latLon, null);
	}

	private PointDescription getPointDescriptionForTarget(@NonNull LatLon latLon, @Nullable String name) {
		MapActivity mapActivity = getMapActivity();
		MapContextMenu menu = mapActivity != null ? mapActivity.getContextMenu() : null;
		return menu != null && menu.isActive() && latLon.equals(menu.getLatLon())
				? menu.getPointDescriptionForTarget()
				: new PointDescription(PointDescription.POINT_TYPE_LOCATION, Algorithms.isEmpty(name) ? "" : name);
	}

	public void addDestination(@NonNull LatLon latLon) {
		addDestination(latLon, null);
	}

	public void addDestination(@NonNull LatLon latLon, @Nullable PointDescription pointDescription) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null && !OsmAndLocationProvider.isLocationPermissionAvailable(mapActivity)) {
			requestedLatLon = latLon;
			ActivityCompat.requestPermissions(mapActivity,
					new String[] {Manifest.permission.ACCESS_FINE_LOCATION,
							Manifest.permission.ACCESS_COARSE_LOCATION},
					REQUEST_LOCATION_FOR_ADD_DESTINATION_PERMISSION);
		} else {
			if (pointDescription == null) {
				pointDescription = getPointDescriptionForTarget(latLon, null);
			}
			if (mapActivity != null) {
				mapActivity.getContextMenu().close();
			}
			TargetPointsHelper targets = app.getTargetPointsHelper();
			RoutingHelper routingHelper = app.getRoutingHelper();
			if (routingHelper.isFollowingMode() || routingHelper.isRoutePlanningMode()) {
				targets.navigateToPoint(latLon, true, targets.getIntermediatePoints().size() + 1, pointDescription);
			} else if (targets.getIntermediatePoints().isEmpty()) {
				startRoutePlanningWithDestination(latLon, pointDescription, targets);
			}
		}
	}

	public void addFirstIntermediate(LatLon latLon) {
		MapActivity mapActivity = requireMapActivity();
		if (latLon != null) {
			RoutingHelper routingHelper = app.getRoutingHelper();
			if (routingHelper.isFollowingMode() || routingHelper.isRoutePlanningMode()) {
				PointDescription pointDescription = getPointDescriptionForTarget(latLon);
				mapActivity.getContextMenu().close();
				TargetPointsHelper targets = app.getTargetPointsHelper();
				if (routingHelper.isFollowingMode() || routingHelper.isRoutePlanningMode()) {
					targets.navigateToPoint(latLon, true, 0, pointDescription);
				} else if (targets.getIntermediatePoints().isEmpty()) {
					startRoutePlanningWithDestination(latLon, pointDescription, targets);
				}
			} else {
				addDestination(latLon);
			}
		}
	}

	public void replaceDestination(@NonNull LatLon latLon) {
		replaceDestination(latLon, null);
	}

	public void replaceDestination(@NonNull LatLon latLon, @Nullable PointDescription pointDescription) {
		RoutingHelper routingHelper = app.getRoutingHelper();
		if (routingHelper.isFollowingMode() || routingHelper.isRoutePlanningMode()) {
			if (pointDescription == null) {
				pointDescription = getPointDescriptionForTarget(latLon, null);
			}
			MapActivity mapActivity = getMapActivity();
			if (mapActivity != null) {
				mapActivity.getContextMenu().close();
			}
			TargetPointsHelper targets = app.getTargetPointsHelper();
			targets.navigateToPoint(latLon, true, -1, pointDescription);
		} else {
			addDestination(latLon, pointDescription);
		}
	}

	public void switchToRouteFollowingLayout() {
		touchEvent = 0;
		app.getRoutingHelper().setRoutePlanningMode(false);
		app.getMapViewTrackingUtilities().switchRoutePlanningMode();
		mapView.refreshMap();
	}

	public boolean switchToRoutePlanningLayout() {
		RoutingHelper routingHelper = app.getRoutingHelper();
		if (!routingHelper.isRoutePlanningMode() && routingHelper.isFollowingMode()) {
			routingHelper.setRoutePlanningMode(true);
			app.getMapViewTrackingUtilities().switchRoutePlanningMode();
			mapView.refreshMap();
			return true;
		}
		return false;
	}

	private void initZooms() {
		MapActivity mapActivity = requireMapActivity();

		ImageView zoomInButton = mapActivity.findViewById(R.id.map_zoom_in_button);
		ImageView zoomOutButton = mapActivity.findViewById(R.id.map_zoom_out_button);

		addMapButton(new ZoomInButton(mapActivity, zoomInButton, ZOOM_IN_HUD_ID));
		addMapButton(new ZoomOutButton(mapActivity, zoomOutButton, ZOOM_OUT_HUD_ID));
	}

	public void addMapButton(@NonNull MapButton mapButton) {
		mapButtons.add(mapButton);
	}

	public void removeMapButtons(@NonNull List<String> buttonIds) {
		List<MapButton> mapButtons = new ArrayList<>(this.mapButtons);
		for (Iterator<MapButton> iterator = mapButtons.iterator(); iterator.hasNext(); ) {
			MapButton mapButton = iterator.next();
			if (buttonIds.contains(mapButton.id)) {
				iterator.remove();
			}
		}
		this.mapButtons = mapButtons;
	}

	public void showMapControlsIfHidden() {
		if (!isMapControlsVisible()) {
			showMapControls();
		}
	}

	private void showMapControls() {
		MapActivity mapActivity = requireMapActivity();
		if (settings.DO_NOT_USE_ANIMATIONS.get()) {
			mapActivity.findViewById(R.id.MapHudButtonsOverlay).setVisibility(View.VISIBLE);
		} else {
			animateMapControls(true);
		}
		AndroidUtils.showNavBar(mapActivity);
	}

	public void hideMapControls() {
		MapActivity mapActivity = requireMapActivity();
		if (settings.DO_NOT_USE_ANIMATIONS.get()) {
			mapActivity.findViewById(R.id.MapHudButtonsOverlay).setVisibility(View.INVISIBLE);
		} else {
			animateMapControls(false);
		}
	}

	private void animateMapControls(boolean show) {
		MapActivity mapActivity = requireMapActivity();
		View mapHudButtonsOverlay = mapActivity.findViewById(R.id.MapHudButtonsOverlay);
		View mapHudButtonsTop = mapActivity.findViewById(R.id.MapHudButtonsOverlayTop);
		View mapHudButtonsBottom = mapActivity.findViewById(R.id.MapHudButtonsOverlayBottom);
		View mapHudButtonsQuickActions = mapActivity.findViewById(R.id.MapHudButtonsOverlayQuickActions);

		float transTopInitial = show ? -mapHudButtonsTop.getHeight() : 0;
		float transBottomInitial = show ? mapHudButtonsBottom.getHeight() : 0;
		float alphaInitial = show ? 0f : 1f;

		float transTopFinal = show ? 0 : -mapHudButtonsTop.getHeight();
		float transBottomFinal = show ? 0 : mapHudButtonsBottom.getHeight();
		float alphaFinal = show ? 1f : 0f;

		AnimatorSet set = new AnimatorSet();
		set.setDuration(300).playTogether(
				ObjectAnimator.ofFloat(mapHudButtonsTop, View.TRANSLATION_Y, transTopInitial, transTopFinal),
				ObjectAnimator.ofFloat(mapHudButtonsBottom, View.TRANSLATION_Y, transBottomInitial, transBottomFinal),
				ObjectAnimator.ofFloat(mapHudButtonsQuickActions, View.ALPHA, alphaInitial, alphaFinal)
		);
		set.addListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationStart(Animator animation) {
				super.onAnimationStart(animation);
				if (show) {
					mapHudButtonsOverlay.setVisibility(View.VISIBLE);
				}
			}

			@Override
			public void onAnimationEnd(Animator animation) {
				super.onAnimationEnd(animation);
				if (!show) {
					mapHudButtonsOverlay.setVisibility(View.INVISIBLE);
					mapHudButtonsTop.setTranslationY(transTopInitial);
					mapHudButtonsBottom.setTranslationY(transBottomInitial);
					mapHudButtonsQuickActions.setAlpha(alphaInitial);
				}
				mapActivity.updateStatusBarColor();
			}
		});
		set.start();
	}

	public boolean isMapControlsVisible() {
		MapActivity mapActivity = requireMapActivity();
		return mapActivity.findViewById(R.id.MapHudButtonsOverlay).getVisibility() == View.VISIBLE;
	}

	public void switchMapControlsVisibility(boolean switchNavBarVisibility) {
		MapActivity mapActivity = requireMapActivity();
		if (app.getRoutingHelper().isFollowingMode() || app.getRoutingHelper().isPauseNavigation()
				|| mapActivity.getMeasurementToolFragment() != null
				|| mapActivity.getPlanRouteFragment() != null
				|| mapActivity.getMapLayers().getDistanceRulerControlLayer().rulerModeOn()) {
			return;
		}
		if (isMapControlsVisible()) {
			hideMapControls();
			if (switchNavBarVisibility) {
				AndroidUtils.hideNavBar(mapActivity);
			}
		} else {
			showMapControls();
		}
		mapActivity.updateStatusBarColor();
	}

	public void startNavigation() {
		RoutingHelper routingHelper = app.getRoutingHelper();
		if (routingHelper.isFollowingMode()) {
			switchToRouteFollowingLayout();
//			if (settings.getApplicationMode() != routingHelper.getAppMode()) {
//				settings.setApplicationMode(routingHelper.getAppMode(), false);
//			}
		} else {
			if (!app.getTargetPointsHelper().checkPointToNavigateShort()) {
				mapRouteInfoMenu.show();
			} else {
				touchEvent = 0;
				app.logEvent("start_navigation");
//				settings.setApplicationMode(routingHelper.getAppMode(), false);
				app.getMapViewTrackingUtilities().backToLocationImpl(17, true);
				settings.FOLLOW_THE_ROUTE.set(true);
				routingHelper.setFollowingMode(true);
				routingHelper.setRoutePlanningMode(false);
				app.getMapViewTrackingUtilities().switchRoutePlanningMode();
				routingHelper.notifyIfRouteIsCalculated();
				if (!settings.simulateNavigation) {
					routingHelper.setCurrentLocation(app.getLocationProvider().getLastKnownLocation(), false);
				} else if (routingHelper.isRouteCalculated() && !routingHelper.isRouteBeingCalculated()) {
					OsmAndLocationSimulation sim = app.getLocationProvider().getLocationSimulation();
					if (!sim.isRouteAnimating()) {
						sim.startStopRouteAnimation(getMapActivity());
					}
				}
			}
		}
	}

	@Override
	public void destroyLayer() {
		super.destroyLayer();
		destroyButtons();
	}

	private void destroyButtons() {
		for (MapButton button : mapButtons) {
			button.onDestroyButton();
		}
		mapButtons.clear();
	}

	public void refreshButtons() {
		for (MapButton button : mapButtons) {
			button.refresh();
		}
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings nightMode) {
		updateControls(tileBox, nightMode);
	}

	private void updateControls(@NonNull RotatedTileBox tileBox, DrawSettings drawSettings) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return;
		}
		boolean isNight = isNightModeForMapControls(drawSettings);

		boolean isRoutePlanningMode = isInRoutePlanningMode();
		boolean isRouteFollowingMode = !isRoutePlanningMode && app.getRoutingHelper().isFollowingMode();
		boolean isTimeToShowButtons = System.currentTimeMillis() - touchEvent < TIMEOUT_TO_SHOW_BUTTONS;
		boolean shouldShowRouteCalculationControls = isRoutePlanningMode || ((app.accessibilityEnabled() || isTimeToShowButtons) && isRouteFollowingMode);
		boolean isRouteDialogOpened = mapRouteInfoMenu.isVisible() || (shouldShowRouteCalculationControls && mapRouteInfoMenu.needShowMenu());

		NavigationSession carNavigationSession = app.getCarNavigationSession();
		boolean androidAutoAttached = carNavigationSession != null && carNavigationSession.hasStarted();
		boolean showBottomMenuButtons = vh.shouldShowBottomMenuButtons()
				&& (shouldShowRouteCalculationControls || !isRouteFollowingMode || androidAutoAttached);

		updateTransparencySliderUi();

		long lastZoomTime = mapView.isZooming()
				? System.currentTimeMillis()
				: app.getMapViewTrackingUtilities().getLastManualZoomTime();
		if ((System.currentTimeMillis() - lastZoomTime > 1000) || !PluginsHelper.isDevelopment()) {
			zoomText.setVisibility(View.GONE);
		} else {
			int textColor = ContextCompat.getColor(mapActivity, isNight ? R.color.widgettext_night : R.color.widgettext_day);
			zoomText.setVisibility(View.VISIBLE);
			zoomText.setTextColor(textColor);
			zoomText.setText(getZoomInfo(tileBox));
		}

		mapRouteInfoMenu.setVisible(shouldShowRouteCalculationControls);

		for (MapButton mapButton : mapButtons) {
			mapButton.update(isNight, isRouteDialogOpened, showBottomMenuButtons);
		}
	}

	private boolean isNightModeForMapControls(DrawSettings drawSettings) {
		MapControlsThemeInfoProvider themeInfoProvider = getThemeInfoProvider();
		if (themeInfoProvider != null) {
			return themeInfoProvider.isNightModeForMapControls();
		}
		return drawSettings != null && drawSettings.isNightMode();
	}

	private MapControlsThemeInfoProvider getThemeInfoProvider() {
		MapActivity mapActivity = requireMapActivity();
		FragmentManager fm = mapActivity.getSupportFragmentManager();
		for (String tag : themeInfoProviderTags) {
			Fragment f = fm.findFragmentByTag(tag);
			if (f instanceof MapControlsThemeInfoProvider) {
				return (MapControlsThemeInfoProvider) f;
			}
		}
		return null;
	}

	public boolean onSingleTap(@NonNull PointF point, @NonNull RotatedTileBox tileBox) {
		return mapRouteInfoMenu != null && mapRouteInfoMenu.onSingleTap(point, tileBox);
	}

	@Override
	public boolean onTouchEvent(@NonNull MotionEvent event, @NonNull RotatedTileBox tileBox) {
		touchEvent = System.currentTimeMillis();
		RoutingHelper rh = app.getRoutingHelper();
		if (rh.isFollowingMode()) {
			mapView.refreshMap();
		}
		return false;
	}

	// /////////////// Transparency bar /////////////////////////
	private void initTransparencyBar() {
		MapActivity mapActivity = requireMapActivity();
		transparencyBarLayout = mapActivity.findViewById(R.id.map_transparency_layout);
		transparencySlider = mapActivity.findViewById(R.id.map_transparency_slider);
		parameterBarLayout = mapActivity.findViewById(R.id.layer_param_layout);
		parameterSlider = mapActivity.findViewById(R.id.layer_param_slider);
		transparencySlider.setValueTo(255);
		if (transparencySetting != null) {
			transparencySlider.setValue(transparencySetting.get());
			transparencyBarLayout.setVisibility(View.VISIBLE);
		} else {
			transparencyBarLayout.setVisibility(View.GONE);
		}
		transparencySlider.addOnChangeListener((slider, value, fromUser) -> {
			if (transparencySetting != null) {
				transparencySetting.set((int) value);
				mapActivity.refreshMap();
			}
		});
		boolean showParameterSlider = false;
		if (parameterMinSetting != null && parameterMaxSetting != null
				&& parameterStepSetting != null && parameterValueSetting != null) {
			float paramMin = parameterMinSetting.get();
			float paramMax = parameterMaxSetting.get();
			float paramStep = parameterStepSetting.get();
			float paramValue = parameterValueSetting.get();
			if (paramMin < paramMax && paramStep < Math.abs(paramMax - paramMin) && paramStep > 0
					&& paramValue >= paramMin && paramValue <= paramMax) {
				parameterSlider.setValueFrom(paramMin);
				parameterSlider.setValueTo(paramMax);
				parameterSlider.setStepSize(paramStep);
				parameterSlider.setValue(paramValue);
				showParameterSlider = true;
			}
		}
		parameterSlider.addOnChangeListener((slider, value, fromUser) -> {
			if (parameterValueSetting != null) {
				parameterValueSetting.set(value);
				mapActivity.refreshMap();
			}
		});

		LayerTransparencySeekbarMode seekbarMode = settings.LAYER_TRANSPARENCY_SEEKBAR_MODE.get();
		if (PluginsHelper.isActive(OsmandRasterMapsPlugin.class)) {
			if (seekbarMode == LayerTransparencySeekbarMode.OVERLAY && settings.MAP_OVERLAY.get() != null) {
				if (showParameterSlider) {
					hideTransparencyBar();
					parameterBarLayout.setVisibility(View.VISIBLE);
					updateParameterSliderUi();
				} else {
					showTransparencyBar(settings.MAP_OVERLAY_TRANSPARENCY);
				}
			} else if (seekbarMode == LayerTransparencySeekbarMode.UNDERLAY && settings.MAP_UNDERLAY.get() != null) {
				showTransparencyBar(settings.MAP_TRANSPARENCY);
			}
		}
	}

	public void updateTransparencySliderValue() {
		LayerTransparencySeekbarMode seekbarMode = settings.LAYER_TRANSPARENCY_SEEKBAR_MODE.get();
		if (PluginsHelper.isActive(OsmandRasterMapsPlugin.class)) {
			if (seekbarMode == LayerTransparencySeekbarMode.OVERLAY && settings.MAP_OVERLAY.get() != null) {
				transparencySlider.setValue(settings.MAP_OVERLAY_TRANSPARENCY.get());
			} else if (seekbarMode == LayerTransparencySeekbarMode.UNDERLAY && settings.MAP_UNDERLAY.get() != null) {
				transparencySlider.setValue(settings.MAP_TRANSPARENCY.get());
			}
		}
	}

	public void showTransparencyBar(@NonNull CommonPreference<Integer> transparentPreference) {
		hideParameterBar();
		transparencySetting = transparentPreference;
		transparencyBarLayout.setVisibility(View.VISIBLE);
		transparencySlider.setValue(transparentPreference.get());
		updateTransparencySliderUi();
	}

	private void updateTransparencySliderUi() {
		ApplicationMode appMode = app.getSettings().getApplicationMode();
		boolean nightMode = app.getDaynightHelper().isNightModeForMapControls();
		int selectedModeColor = appMode.getProfileColor(nightMode);
		UiUtilities.setupSlider(transparencySlider, nightMode, selectedModeColor);
	}

	public void hideTransparencyBar() {
		transparencyBarLayout.setVisibility(View.GONE);
		transparencySetting = null;
	}

	public void showParameterBar(@NonNull MapTileLayer layer) {
		CommonPreference<Float> paramMinPref = layer.getParamMinPref();
		CommonPreference<Float> paramMaxPref = layer.getParamMaxPref();
		CommonPreference<Float> paramStepPref = layer.getParamStepPref();
		CommonPreference<Float> paramValuePref = layer.getParamValuePref();
		parameterMinSetting = paramMinPref;
		parameterMaxSetting = paramMaxPref;
		parameterStepSetting = paramStepPref;
		parameterValueSetting = paramValuePref;
		if (paramMinPref != null && paramMaxPref != null && paramStepPref != null && paramValuePref != null) {
			float paramMin = paramMinPref.get();
			float paramMax = paramMaxPref.get();
			float paramStep = paramStepPref.get();
			float paramValue = paramValuePref.get();
			if (paramMin < paramMax && paramStep < Math.abs(paramMax - paramMin) && paramStep > 0
					&& paramValue >= paramMin && paramValue <= paramMax) {
				hideTransparencyBar();
				parameterBarLayout.setVisibility(View.VISIBLE);
				parameterSlider.setValueFrom(paramMin);
				parameterSlider.setValueTo(paramMax);
				parameterSlider.setStepSize(paramStep);
				parameterSlider.setValue(paramValue);
				updateParameterSliderUi();
				layer.setupParameterListener();
			}
		}
	}

	private void updateParameterSliderUi() {
		ApplicationMode appMode = app.getSettings().getApplicationMode();
		boolean nightMode = app.getDaynightHelper().isNightModeForMapControls();
		int selectedModeColor = appMode.getProfileColor(nightMode);
		UiUtilities.setupSlider(parameterSlider, nightMode, selectedModeColor);
	}

	public void hideParameterBar() {
		parameterBarLayout.setVisibility(View.GONE);
		parameterMinSetting = null;
		parameterMaxSetting = null;
		parameterStepSetting = null;
		parameterValueSetting = null;
	}

	@NonNull
	private String getZoomInfo(@NonNull RotatedTileBox tb) {
		StringBuilder zoomInfo = new StringBuilder();
		zoomInfo.append(tb.getZoom());

		double zoomFloatPart = tb.getZoomFloatPart() + tb.getZoomAnimation();
		int formattedZoomFloatPart = (int) Math.abs(Math.round(zoomFloatPart * 100));
		boolean addLeadingZero = formattedZoomFloatPart < 10;
		zoomInfo.append(" ")
				.append(zoomFloatPart < 0 ? "-" : "+")
				.append(".")
				.append(addLeadingZero ? "0" : "")
				.append(formattedZoomFloatPart);

		double mapDensity = Math.abs(tb.getMapDensity());
		if (mapDensity != 0) {
			int mapDensity10 = (int) (mapDensity * 10);
			zoomInfo.append(" ").append(mapDensity10 / 10);
			if (mapDensity10 % 10 != 0) {
				zoomInfo.append(".").append(mapDensity10 % 10);
			}
		}

		return zoomInfo.toString();
	}

	private boolean isInRoutePlanningMode() {
		RoutingHelper routingHelper = app.getRoutingHelper();
		return routingHelper.isRoutePlanningMode()
				|| ((routingHelper.isRouteCalculated() || routingHelper.isRouteBeingCalculated())
				&& !routingHelper.isFollowingMode());
	}

	public static OnLongClickListener getOnClickMagnifierListener(@NonNull OsmandMapTileView view) {
		return new OnLongClickListener() {

			@Override
			public boolean onLongClick(View notUseCouldBeNull) {
				OsmandPreference<Float> mapDensity = view.getSettings().MAP_DENSITY;
				AlertDialog.Builder bld = new AlertDialog.Builder(view.requireMapActivity());
				int p = (int) (mapDensity.get() * 100);
				TIntArrayList tlist = new TIntArrayList(new int[] {25, 33, 50, 75, 100, 125, 150, 200, 300, 400});
				List<String> values = new ArrayList<>();
				int i = -1;
				for (int k = 0; k <= tlist.size(); k++) {
					boolean end = k == tlist.size();
					if (i == -1) {
						if ((end || p < tlist.get(k))) {
							values.add(p + " %");
							i = k;
						} else if (p == tlist.get(k)) {
							i = k;
						}

					}
					if (k < tlist.size()) {
						values.add(tlist.get(k) + " %");
					}
				}
				if (values.size() != tlist.size()) {
					tlist.insert(i, p);
				}

				bld.setTitle(R.string.map_magnifier);
				bld.setSingleChoiceItems(values.toArray(new String[0]), i,
						(dialog, which) -> {
							int p1 = tlist.get(which);
							mapDensity.set(p1 / 100.0f);
							view.setComplexZoom(view.getZoom(), view.getSettingsMapDensity());
							MapRendererContext mapContext = NativeCoreContext.getMapRendererContext();
							if (mapContext != null) {
								mapContext.updateMapSettings();
							}
							dialog.dismiss();
						});
				bld.show();
				return true;
			}
		};
	}

	public void selectAddress(String name, double latitude, double longitude, QuickSearchType searchType) {
		PointType pointType = null;
		switch (searchType) {
			case START_POINT:
				pointType = PointType.START;
				break;
			case DESTINATION:
			case DESTINATION_AND_START:
				pointType = PointType.TARGET;
				break;
			case INTERMEDIATE:
				pointType = PointType.INTERMEDIATE;
				break;
			case HOME_POINT:
				pointType = PointType.HOME;
				break;
			case WORK_POINT:
				pointType = PointType.WORK;
				break;
		}
		if (pointType != null) {
			mapRouteInfoMenu.selectAddress(name, new LatLon(latitude, longitude), pointType);
		}
	}

	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		if ((requestCode == REQUEST_LOCATION_FOR_NAVIGATION_PERMISSION
				|| requestCode == REQUEST_LOCATION_FOR_NAVIGATION_FAB_PERMISSION
				|| requestCode == REQUEST_LOCATION_FOR_ADD_DESTINATION_PERMISSION)) {
			if (grantResults.length > 0) {
				if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					switch (requestCode) {
						case REQUEST_LOCATION_FOR_NAVIGATION_PERMISSION:
							onNavigationClick();
							break;
						case REQUEST_LOCATION_FOR_NAVIGATION_FAB_PERMISSION:
							navigateButton();
							break;
						case REQUEST_LOCATION_FOR_ADD_DESTINATION_PERMISSION:
							if (requestedLatLon != null) {
								addDestination(requestedLatLon);
							}
							break;
					}
				} else if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
					app.showToastMessage(R.string.ask_for_location_permission);
				}
			}
		}
	}

	public void addThemeInfoProviderTag(String tag) {
		themeInfoProviderTags.add(tag);
	}

	public void removeThemeInfoProviderTag(String tag) {
		themeInfoProviderTags.remove(tag);
	}

	public interface MapControlsThemeInfoProvider {
		boolean isNightModeForMapControls();
	}
}
