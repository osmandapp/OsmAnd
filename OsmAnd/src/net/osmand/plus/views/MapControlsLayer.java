package net.osmand.plus.views;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorCompat;
import android.support.v4.view.ViewPropertyAnimatorListener;
import android.support.v7.app.AlertDialog;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.Location;
import net.osmand.core.android.MapRendererContext;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmAndAppCustomization;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmAndLocationSimulation;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.CommonPreference;
import net.osmand.plus.OsmandSettings.LayerTransparencySeekbarMode;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.TargetPointsHelper.TargetPoint;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.MapActivity.ShowQuickSearchMode;
import net.osmand.plus.base.ContextMenuFragment.MenuState;
import net.osmand.plus.dashboard.DashboardOnMap.DashboardType;
import net.osmand.plus.dialogs.DirectionsDialogs;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.rastermaps.OsmandRasterMapsPlugin;
import net.osmand.plus.routepreparationmenu.MapRouteInfoMenu;
import net.osmand.plus.routepreparationmenu.MapRouteInfoMenu.PointType;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.routing.TransportRoutingHelper;
import net.osmand.plus.search.QuickSearchDialogFragment.QuickSearchType;
import net.osmand.plus.views.corenative.NativeCoreContext;

import java.util.ArrayList;
import java.util.List;

import gnu.trove.list.array.TIntArrayList;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.BACK_TO_LOC_HUD_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.COMPASS_HUD_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.LAYERS_HUD_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MENU_HUD_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.QUICK_SEARCH_HUD_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.ROUTE_PLANNING_HUD_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.ZOOM_IN_HUD_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.ZOOM_OUT_HUD_ID;

public class MapControlsLayer extends OsmandMapLayer {

	private static final int TIMEOUT_TO_SHOW_BUTTONS = 7000;
	private static final int REQUEST_LOCATION_FOR_NAVIGATION_PERMISSION = 200;
	private static final int REQUEST_LOCATION_FOR_NAVIGATION_FAB_PERMISSION = 201;
	private static final int REQUEST_LOCATION_FOR_ADD_DESTINATION_PERMISSION = 202;

	private static final int COMPASS_PRESSED_TIME_INTERVAL_MS = 5000;

	public MapHudButton createHudButton(View iv, int resId, String id) {
		MapHudButton mc = new MapHudButton();
		mc.iv = iv;
		mc.resId = resId;
		mc.id = id;
		return mc;
	}

	private List<MapHudButton> controls = new ArrayList<>();
	private final MapActivity mapActivity;
	// private RulerControl rulerControl;
	// private List<MapControls> allControls = new ArrayList<MapControls>();

	private SeekBar transparencyBar;
	private LinearLayout transparencyBarLayout;
	private static CommonPreference<Integer> transparencySetting;
	private boolean isTransparencyBarEnabled;
	private OsmandSettings settings;

	private MapRouteInfoMenu mapRouteInfoMenu;
	private MapHudButton backToLocationControl;
	private MapHudButton menuControl;
	private MapHudButton compassHud;
	private MapHudButton quickSearchHud;
	private float cachedRotate = 0;
	private TextView zoomText;
	private OsmandMapTileView mapView;
	private OsmandApplication app;
	private OsmAndAppCustomization appCustomization;
	private MapHudButton routePlanningBtn;
	private long touchEvent;
	private MapHudButton mapZoomOut;
	private MapHudButton mapZoomIn;
	private MapHudButton layersHud;
	private long lastZoom;
	private boolean hasTargets;
	private ContextMenuLayer contextMenuLayer;
	private MapQuickActionLayer mapQuickActionLayer;
	private boolean forceShowCompass;
	private LatLon requestedLatLon;
	private long compassPressed;

	public MapControlsLayer(MapActivity activity) {
		this.mapActivity = activity;
		app = activity.getMyApplication();
		appCustomization = app.getAppCustomization();
		settings = activity.getMyApplication().getSettings();
		mapView = mapActivity.getMapView();
		contextMenuLayer = mapActivity.getMapLayers().getContextMenuLayer();
	}

	@Override
	public boolean drawInScreenPixels() {
		return true;
	}

	@Override
	public void initLayer(final OsmandMapTileView view) {
		initTopControls();
		initTransparencyBar();
		initZooms();
		initDasboardRelatedControls();
		updateControls(view.getCurrentRotatedTileBox(), null);
	}

	public void initDasboardRelatedControls() {
		initControls();
		initRouteControls();
	}

	public View moveCompassButton(ViewGroup destLayout, ViewGroup.LayoutParams layoutParams, boolean night) {
		View compassView = compassHud.iv;
		ViewGroup parent = (ViewGroup) compassView.getParent();
		if (parent != null) {
			compassHud.cancelHideAnimation();
			compassHud.compassOutside = true;
			forceShowCompass = true;
			parent.removeView(compassView);
			compassView.setLayoutParams(layoutParams);
			destLayout.addView(compassView);
			updateCompass(night);
			return compassView;
		}
		return null;
	}

	public void restoreCompassButton(boolean night) {
		View compassView = compassHud.iv;
		ViewGroup parent = (ViewGroup) compassView.getParent();
		if (parent != null) {
			compassHud.compassOutside = false;
			forceShowCompass = false;
			parent.removeView(compassView);
			LinearLayout mapCompassContainer = (LinearLayout) mapActivity.findViewById(R.id.layers_compass_layout);
			if (mapCompassContainer != null) {
				int buttonSizePx = mapActivity.getResources().getDimensionPixelSize(R.dimen.map_small_button_size);
				int topMarginPx = mapActivity.getResources().getDimensionPixelSize(R.dimen.map_small_button_margin);
				LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(buttonSizePx, buttonSizePx);
				params.topMargin = topMarginPx;
				compassView.setLayoutParams(params);
				mapCompassContainer.addView(compassView);
				updateCompass(night);
			}
		}
	}

	private class CompassDrawable extends Drawable {

		private Drawable original;

		public CompassDrawable(Drawable original) {
			this.original = original;
		}

		@Override
		public void draw(Canvas canvas) {
			canvas.save();
			canvas.rotate(cachedRotate, getIntrinsicWidth() / 2, getIntrinsicHeight() / 2);
			original.draw(canvas);
			canvas.restore();
		}

		@Override
		public int getMinimumHeight() {
			return original.getMinimumHeight();
		}

		@Override
		public int getMinimumWidth() {
			return original.getMinimumWidth();
		}

		@Override
		public int getIntrinsicHeight() {
			return original.getIntrinsicHeight();
		}

		@Override
		public int getIntrinsicWidth() {
			return original.getIntrinsicWidth();
		}

		@Override
		public void setChangingConfigurations(int configs) {
			super.setChangingConfigurations(configs);
			original.setChangingConfigurations(configs);
		}

		@Override
		public void setBounds(int left, int top, int right, int bottom) {
			super.setBounds(left, top, right, bottom);
			original.setBounds(left, top, right, bottom);
		}

		@Override
		public void setAlpha(int alpha) {
			original.setAlpha(alpha);
		}

		@Override
		public void setColorFilter(ColorFilter cf) {
			original.setColorFilter(cf);
		}

		@Override
		public int getOpacity() {
			return original.getOpacity();
		}
	}

	private void initTopControls() {
		View configureMap = mapActivity.findViewById(R.id.map_layers_button);
		layersHud = createHudButton(configureMap, R.drawable.map_world_globe_dark, LAYERS_HUD_ID)
				.setIconColorId(R.color.on_map_icon_color, 0)
				.setBg(R.drawable.btn_inset_circle_trans, R.drawable.btn_inset_circle_night);
		controls.add(layersHud);
		configureMap.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				MapActivity.clearPrevActivityIntent();
				mapActivity.getDashboard().setDashboardVisibility(true, DashboardType.CONFIGURE_MAP, AndroidUtils.getCenterViewCoordinates(v));
			}
		});

		View compass = mapActivity.findViewById(R.id.map_compass_button);
		compassHud = createHudButton(compass, R.drawable.map_compass, COMPASS_HUD_ID).setIconColorId(0).
				setBg(R.drawable.btn_inset_circle_trans, R.drawable.btn_inset_circle_night);
		compassHud.compass = true;
		controls.add(compassHud);
		compass.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				boolean followingMode = app.getRoutingHelper().isFollowingMode();

				if (followingMode) {
					if (compassPressed + COMPASS_PRESSED_TIME_INTERVAL_MS > System.currentTimeMillis()) {
						compassPressed = 0;
						mapActivity.getMapViewTrackingUtilities().switchRotateMapMode();
					} else {
						compassPressed = System.currentTimeMillis();
						app.showShortToastMessage(app.getString(R.string.press_again_to_change_the_map_orientation));
					}
				} else {
					compassPressed = 0;
					mapActivity.getMapViewTrackingUtilities().switchRotateMapMode();
				}
			}
		});

		View search = mapActivity.findViewById(R.id.map_search_button);
		quickSearchHud = createHudButton(search, R.drawable.map_search_dark, QUICK_SEARCH_HUD_ID)
				.setIconsId(R.drawable.map_search_dark, R.drawable.map_search_night)
				.setIconColorId(0)
				.setBg(R.drawable.btn_inset_circle_trans, R.drawable.btn_inset_circle_night);
		controls.add(quickSearchHud);
		search.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mapActivity.dismissCardDialog();
				mapActivity.showQuickSearch(ShowQuickSearchMode.NEW_IF_EXPIRED, false);
			}
		});

	}

	private void initRouteControls() {
		mapRouteInfoMenu = mapActivity.getMapRouteInfoMenu();
	}

	public void setControlsClickable(boolean clickable) {
		for (MapHudButton mb : controls) {
			mb.iv.setClickable(clickable);
		}
	}

	private TargetPointsHelper getTargets() {
		return mapActivity.getMyApplication().getTargetPointsHelper();
	}

	public void stopNavigation() {
		mapRouteInfoMenu.hide();
		if (mapActivity.getRoutingHelper().isFollowingMode()) {
			mapActivity.getMapActions().stopNavigationActionConfirm();
		} else {
			mapActivity.getMapActions().stopNavigationWithoutConfirm();
		}
	}

	public void stopNavigationWithoutConfirm() {
		mapRouteInfoMenu.hide();
		mapActivity.getMapActions().stopNavigationWithoutConfirm();
	}

	public void showRouteInfoControlDialog() {
		mapRouteInfoMenu.showHideMenu();
	}

	public void showRouteInfoMenu() {
		mapRouteInfoMenu.setShowMenu(MapRouteInfoMenu.DEFAULT_MENU_STATE);
	}

	public void showRouteInfoMenu(int menuState) {
		mapRouteInfoMenu.setShowMenu(menuState);
	}

	private void initControls() {
		View backToLocation = mapActivity.findViewById(R.id.map_my_location_button);
		backToLocationControl = createHudButton(backToLocation, R.drawable.map_my_location, BACK_TO_LOC_HUD_ID)
				.setBg(R.drawable.btn_circle_blue);
		controls.add(backToLocationControl);

		backToLocation.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (OsmAndLocationProvider.isLocationPermissionAvailable(mapActivity)) {
					mapActivity.getMapViewTrackingUtilities().backToLocationImpl();
				} else {
					ActivityCompat.requestPermissions(mapActivity,
							new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
							OsmAndLocationProvider.REQUEST_LOCATION_PERMISSION);
				}
			}
		});
		View backToMenuButton = mapActivity.findViewById(R.id.map_menu_button);

		final boolean dash = settings.SHOW_DASHBOARD_ON_MAP_SCREEN.get();
		menuControl = createHudButton(backToMenuButton,
				!dash ? R.drawable.map_drawer : R.drawable.map_dashboard, MENU_HUD_ID).setBg(
				R.drawable.btn_round, R.drawable.btn_round_night);
		controls.add(menuControl);
		backToMenuButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				MapActivity.clearPrevActivityIntent();
				if (dash) {
					mapActivity.getDashboard().setDashboardVisibility(true, DashboardType.DASHBOARD, AndroidUtils.getCenterViewCoordinates(v));
				} else {
					mapActivity.openDrawer();
				}
			}
		});
		zoomText = (TextView) mapActivity.findViewById(R.id.map_app_mode_text);

		View routePlanButton = mapActivity.findViewById(R.id.map_route_info_button);
		routePlanningBtn = createHudButton(routePlanButton, R.drawable.map_directions, ROUTE_PLANNING_HUD_ID).setBg(
				R.drawable.btn_round, R.drawable.btn_round_night);
		controls.add(routePlanningBtn);
		routePlanButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mapActivity.dismissCardDialog();
				doRoute(false);
			}
		});
	}

	public void doRoute(boolean hasTargets) {
		this.hasTargets = hasTargets;
		onNavigationClick();
	}

	public void doNavigate() {
		mapRouteInfoMenu.hide();
		startNavigation();
	}

	private void onNavigationClick() {
		if (mapRouteInfoMenu != null) {
			mapRouteInfoMenu.cancelSelectionFromMap();
		}
		MapActivity.clearPrevActivityIntent();
		RoutingHelper routingHelper = mapActivity.getRoutingHelper();
		if (!routingHelper.isFollowingMode() && !routingHelper.isRoutePlanningMode()) {
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
		hasTargets = false;
	}

	public void navigateButton() {
		if (!OsmAndLocationProvider.isLocationPermissionAvailable(mapActivity)) {
			ActivityCompat.requestPermissions(mapActivity,
					new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
					REQUEST_LOCATION_FOR_NAVIGATION_FAB_PERMISSION);
		} else {
			final MapContextMenu menu = mapActivity.getContextMenu();
			final LatLon latLon = menu.getLatLon();
			final PointDescription pointDescription = menu.getPointDescriptionForTarget();
			menu.hide();
			final TargetPointsHelper targets = mapActivity.getMyApplication().getTargetPointsHelper();
			RoutingHelper routingHelper = mapActivity.getMyApplication().getRoutingHelper();
			if (routingHelper.isFollowingMode() || routingHelper.isRoutePlanningMode()) {
				DirectionsDialogs.addWaypointDialogAndLaunchMap(mapActivity, latLon.getLatitude(),
						latLon.getLongitude(), pointDescription);
			} else if (targets.getIntermediatePoints().isEmpty()) {
				startRoutePlanningWithDestination(latLon, pointDescription, targets);
				menu.close();
			} else {
				AlertDialog.Builder bld = new AlertDialog.Builder(mapActivity);
				bld.setTitle(R.string.new_directions_point_dialog);
				final int[] defaultVls = new int[]{0};
				bld.setSingleChoiceItems(new String[]{
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
							targets.navigateToPoint(latLon, true, -1, pointDescription);
							mapActivity.getMapActions().enterRoutePlanningModeGivenGpx(null, null, null, true, true, MenuState.HEADER_ONLY);
							menu.close();
						} else {
							targets.navigateToPoint(latLon, true, -1, pointDescription);
							mapActivity.getMapActions().enterRoutePlanningModeGivenGpx(null, null, null, true, true, MenuState.HEADER_ONLY);
							menu.close();
						}
					}
				});
				bld.setNegativeButton(R.string.shared_string_cancel, null);
				bld.show();
			}
		}
	}

	private void startRoutePlanningWithDestination(LatLon latLon, PointDescription pointDescription, TargetPointsHelper targets) {
		boolean hasPointToStart = settings.restorePointToStart();
		targets.navigateToPoint(latLon, true, -1, pointDescription);
		if (!hasPointToStart) {
			mapActivity.getMapActions().enterRoutePlanningModeGivenGpx(null, null, null, true, true, MenuState.HEADER_ONLY);
		} else {
			TargetPoint start = targets.getPointToStart();
			if (start != null) {
				mapActivity.getMapActions().enterRoutePlanningModeGivenGpx(null, start.point, start.getOriginalPointDescription(), true, true, MenuState.HEADER_ONLY);
			} else {
				mapActivity.getMapActions().enterRoutePlanningModeGivenGpx(null, null, null, true, true, MenuState.HEADER_ONLY);
			}
		}
	}

	private PointDescription getPointDescriptionForTarget(LatLon latLon) {
		final MapContextMenu menu = mapActivity.getContextMenu();
		PointDescription pointDescription;
		if (menu.isActive() && latLon.equals(menu.getLatLon())) {
			pointDescription = menu.getPointDescriptionForTarget();
		} else {
			pointDescription = new PointDescription(PointDescription.POINT_TYPE_LOCATION, "");
		}
		return pointDescription;
	}

	public void addDestination(LatLon latLon) {
		if (latLon != null) {
			if (!OsmAndLocationProvider.isLocationPermissionAvailable(mapActivity)) {
				requestedLatLon = latLon;
				ActivityCompat.requestPermissions(mapActivity,
						new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
						REQUEST_LOCATION_FOR_ADD_DESTINATION_PERMISSION);
			} else {
				PointDescription pointDescription = getPointDescriptionForTarget(latLon);
				mapActivity.getContextMenu().close();
				final TargetPointsHelper targets = mapActivity.getMyApplication().getTargetPointsHelper();
				RoutingHelper routingHelper = mapActivity.getMyApplication().getRoutingHelper();
				if (routingHelper.isFollowingMode() || routingHelper.isRoutePlanningMode()) {
					targets.navigateToPoint(latLon, true, targets.getIntermediatePoints().size() + 1, pointDescription);
				} else if (targets.getIntermediatePoints().isEmpty()) {
					startRoutePlanningWithDestination(latLon, pointDescription, targets);
				}
			}
		}
	}

	public void addFirstIntermediate(LatLon latLon) {
		if (latLon != null) {
			RoutingHelper routingHelper = mapActivity.getMyApplication().getRoutingHelper();
			if (routingHelper.isFollowingMode() || routingHelper.isRoutePlanningMode()) {
				PointDescription pointDescription = getPointDescriptionForTarget(latLon);
				mapActivity.getContextMenu().close();
				final TargetPointsHelper targets = mapActivity.getMyApplication().getTargetPointsHelper();
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

	public void replaceDestination(LatLon latLon) {
		RoutingHelper routingHelper = mapActivity.getMyApplication().getRoutingHelper();
		if (latLon != null) {
			if (routingHelper.isFollowingMode() || routingHelper.isRoutePlanningMode()) {
				PointDescription pointDescription = getPointDescriptionForTarget(latLon);
				mapActivity.getContextMenu().close();
				final TargetPointsHelper targets = mapActivity.getMyApplication().getTargetPointsHelper();
				targets.navigateToPoint(latLon, true, -1, pointDescription);
			} else {
				addDestination(latLon);
			}
		}
	}

	public void switchToRouteFollowingLayout() {
		touchEvent = 0;
		mapActivity.getMyApplication().getRoutingHelper().setRoutePlanningMode(false);
		mapActivity.getMapViewTrackingUtilities().switchToRoutePlanningMode();
		mapActivity.refreshMap();
	}

	public boolean switchToRoutePlanningLayout() {
		if (!mapActivity.getRoutingHelper().isRoutePlanningMode() && mapActivity.getRoutingHelper().isFollowingMode()) {
			mapActivity.getRoutingHelper().setRoutePlanningMode(true);
			mapActivity.getMapViewTrackingUtilities().switchToRoutePlanningMode();
			mapActivity.refreshMap();
			return true;
		}
		return false;
	}

	private void initZooms() {
		final OsmandMapTileView view = mapActivity.getMapView();
		View zoomInButton = mapActivity.findViewById(R.id.map_zoom_in_button);
		mapZoomIn = createHudButton(zoomInButton, R.drawable.map_zoom_in, ZOOM_IN_HUD_ID).
				setIconsId(R.drawable.map_zoom_in, R.drawable.map_zoom_in_night).setRoundTransparent();
		controls.add(mapZoomIn);
		zoomInButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mapActivity.getContextMenu().zoomInPressed()) {
					return;
				}
				if (view.isZooming()) {
					mapActivity.changeZoom(2, System.currentTimeMillis());
				} else {
					mapActivity.changeZoom(1, System.currentTimeMillis());
				}
			}
		});
		final View.OnLongClickListener listener = MapControlsLayer.getOnClickMagnifierListener(view);
		zoomInButton.setOnLongClickListener(listener);
		View zoomOutButton = mapActivity.findViewById(R.id.map_zoom_out_button);
		mapZoomOut = createHudButton(zoomOutButton, R.drawable.map_zoom_out, ZOOM_OUT_HUD_ID).
				setIconsId(R.drawable.map_zoom_out, R.drawable.map_zoom_out_night).setRoundTransparent();
		controls.add(mapZoomOut);
		zoomOutButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mapActivity.getContextMenu().zoomOutPressed()) {
					return;
				}
				mapActivity.changeZoom(-1, System.currentTimeMillis());
			}
		});
		zoomOutButton.setOnLongClickListener(listener);
	}

	public void showMapControlsIfHidden() {
		if (!isMapControlsVisible()) {
			showMapControls();
		}
	}

	private void showMapControls() {
		if (settings.DO_NOT_USE_ANIMATIONS.get()) {
			mapActivity.findViewById(R.id.MapHudButtonsOverlay).setVisibility(View.VISIBLE);
		} else {
			animateMapControls(true);
		}
		AndroidUtils.showNavBar(mapActivity);
	}

	public void hideMapControls() {
		if (settings.DO_NOT_USE_ANIMATIONS.get()) {
			mapActivity.findViewById(R.id.MapHudButtonsOverlay).setVisibility(View.INVISIBLE);
		} else {
			animateMapControls(false);
		}
	}

	private void animateMapControls(final boolean show) {
		final View mapHudButtonsOverlay = mapActivity.findViewById(R.id.MapHudButtonsOverlay);
		View mapHudButtonsTop = mapActivity.findViewById(R.id.MapHudButtonsOverlayTop);
		View mapHudButtonsBottom = mapActivity.findViewById(R.id.MapHudButtonsOverlayBottom);
		View mapHudButtonsQuickActions = mapActivity.findViewById(R.id.MapHudButtonsOverlayQuickActions);
		AnimatorSet set = new AnimatorSet();
		float transTopInitial = show ? -mapHudButtonsTop.getHeight() : 0;
		float transTopFinal = show ? 0 : -mapHudButtonsTop.getHeight();
		float transBottomInitial = show ? mapHudButtonsBottom.getHeight() : 0;
		float transBottomFinal = show ? 0 : mapHudButtonsBottom.getHeight();
		float alphaInitial = show ? 0f : 1f;
		float alphaFinal = show ? 1f : 0f;
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
				}
				mapActivity.updateStatusBarColor();
			}
		});
		set.start();
	}

	public void setMapControlsVisibility(boolean visible) {
		View mapHudButtonsOverlay = mapActivity.findViewById(R.id.MapHudButtonsOverlay);
		mapHudButtonsOverlay.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
	}

	public boolean isMapControlsVisible() {
		return mapActivity.findViewById(R.id.MapHudButtonsOverlay).getVisibility() == View.VISIBLE;
	}

	public void switchMapControlsVisibility(boolean switchNavBarVisibility) {
		if (app.getRoutingHelper().isFollowingMode() || app.getRoutingHelper().isPauseNavigation()
				|| mapActivity.getMeasurementToolFragment() != null
				|| mapActivity.getPlanRouteFragment() != null
				|| mapActivity.getMapLayers().getRulerControlLayer().rulerModeOn()) {
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
		OsmandApplication app = mapActivity.getMyApplication();
		RoutingHelper routingHelper = app.getRoutingHelper();
		if (routingHelper.isFollowingMode()) {
			switchToRouteFollowingLayout();
			if (app.getSettings().APPLICATION_MODE.get() != routingHelper.getAppMode()) {
				app.getSettings().APPLICATION_MODE.set(routingHelper.getAppMode());
			}
		} else {
			if (!app.getTargetPointsHelper().checkPointToNavigateShort()) {
				mapRouteInfoMenu.show();
			} else {
				touchEvent = 0;
				app.logEvent("start_navigation");
				app.getSettings().APPLICATION_MODE.set(routingHelper.getAppMode());
				mapActivity.getMapViewTrackingUtilities().backToLocationImpl(17, true);
				app.getSettings().FOLLOW_THE_ROUTE.set(true);
				routingHelper.setFollowingMode(true);
				routingHelper.setRoutePlanningMode(false);
				mapActivity.getMapViewTrackingUtilities().switchToRoutePlanningMode();
				app.getRoutingHelper().notifyIfRouteIsCalculated();
				if (!app.getSettings().simulateNavigation) {
					routingHelper.setCurrentLocation(app.getLocationProvider().getLastKnownLocation(), false);
				} else if (routingHelper.isRouteCalculated() && !routingHelper.isRouteBeingCalculated()) {
					OsmAndLocationSimulation sim = app.getLocationProvider().getLocationSimulation();
					if (!sim.isRouteAnimating()) {
						sim.startStopRouteAnimation(mapActivity);
					}
				}
			}
		}
	}

	@Override
	public void destroyLayer() {
		controls.clear();
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings nightMode) {
		updateControls(tileBox, nightMode);
	}

	public boolean isPotrait() {
		return AndroidUiHelper.isOrientationPortrait(mapActivity);
	}

	@SuppressWarnings("deprecation")
	private void updateControls(@NonNull RotatedTileBox tileBox, DrawSettings drawSettings) {
		boolean isNight = drawSettings != null && drawSettings.isNightMode();
		boolean portrait = isPotrait();
//		int shadw = isNight ? mapActivity.getResources().getColor(R.color.widgettext_shadow_night) :
//				mapActivity.getResources().getColor(R.color.widgettext_shadow_day);
		int textColor = isNight ? mapActivity.getResources().getColor(R.color.widgettext_night) :
				mapActivity.getResources().getColor(R.color.widgettext_day);
		// TODOnightMode
		// updatextColor(textColor, shadw, rulerControl, zoomControls, mapMenuControls);
		// default buttons
		boolean routePlanningMode = false;
		RoutingHelper rh = mapActivity.getRoutingHelper();
		TransportRoutingHelper trh = mapActivity.getRoutingHelper().getTransportRoutingHelper();
		if (rh.isRoutePlanningMode()) {
			routePlanningMode = true;
		} else if ((rh.isRouteCalculated() || rh.isRouteBeingCalculated()) && !rh.isFollowingMode()) {
			routePlanningMode = true;
		}
		boolean routeFollowingMode = !routePlanningMode && rh.isFollowingMode();
		boolean trackDialogOpened = mapActivity.getTrackDetailsMenu().isVisible();
		boolean contextMenuOpened = !mapActivity.getContextMenu().shouldShowTopControls();
		boolean showRouteCalculationControls = routePlanningMode ||
				((app.accessibilityEnabled() || (System.currentTimeMillis() - touchEvent < TIMEOUT_TO_SHOW_BUTTONS)) && routeFollowingMode);
		boolean routeDialogOpened = mapRouteInfoMenu.isVisible() || (showRouteCalculationControls && mapRouteInfoMenu.needShowMenu());
		updateMyLocation(rh, routeDialogOpened || trackDialogOpened || contextMenuOpened);
		boolean showButtons = (showRouteCalculationControls || !routeFollowingMode)
				&& !isInMovingMarkerMode() && !isInGpxDetailsMode() && !isInMeasurementToolMode() && !isInPlanRouteMode() && !contextMenuOpened && !isInChoosingRoutesMode() && !isInWaypointsChoosingMode();
		//routePlanningBtn.setIconResId(routeFollowingMode ? R.drawable.ic_action_gabout_dark : R.drawable.map_directions);
		int routePlanningBtnImage = mapRouteInfoMenu.getRoutePlanningBtnImage();
		if (routePlanningBtnImage != 0) {
			routePlanningBtn.setIconResId(routePlanningBtnImage);
			routePlanningBtn.setIconColorId(R.color.color_myloc_distance);
		} else if (rh.isFollowingMode()) {
			routePlanningBtn.setIconResId(R.drawable.map_start_navigation);
			routePlanningBtn.setIconColorId(R.color.color_myloc_distance);
		} else if (routePlanningMode) {
			routePlanningBtn.setIconResId(R.drawable.map_directions);
			routePlanningBtn.setIconColorId(R.color.color_myloc_distance);
		} else {
			routePlanningBtn.setIconResId(R.drawable.map_directions);
			routePlanningBtn.resetIconColors();
		}
		routePlanningBtn.updateVisibility(showButtons);
		menuControl.updateVisibility(showButtons);


		mapZoomIn.updateVisibility(!routeDialogOpened && !contextMenuOpened && (!isInChoosingRoutesMode() || !isInWaypointsChoosingMode() || !portrait));
		mapZoomOut.updateVisibility(!routeDialogOpened && !contextMenuOpened && (!isInChoosingRoutesMode() || !isInWaypointsChoosingMode() || !portrait));
		boolean forceHideCompass = routeDialogOpened || trackDialogOpened
				|| isInMeasurementToolMode() || isInPlanRouteMode() || contextMenuOpened || isInChoosingRoutesMode() || isInWaypointsChoosingMode();
		compassHud.forceHideCompass = forceHideCompass;
		compassHud.updateVisibility(!forceHideCompass && shouldShowCompass());

		if (layersHud.setIconResId(settings.getApplicationMode().getMapIconRes())) {
			layersHud.update(app, isNight);
		}
		layersHud.updateVisibility(!routeDialogOpened && !trackDialogOpened && !isInMeasurementToolMode() && !isInPlanRouteMode()
				&& !contextMenuOpened && !isInChoosingRoutesMode() && !isInWaypointsChoosingMode());
		quickSearchHud.updateVisibility(!routeDialogOpened && !trackDialogOpened && !isInMeasurementToolMode() && !isInPlanRouteMode()
				&& !contextMenuOpened && !isInChoosingRoutesMode() && !isInWaypointsChoosingMode());

		if (!routePlanningMode && !routeFollowingMode) {
			if (mapView.isZooming()) {
				lastZoom = System.currentTimeMillis();
			}
			//if (!mapView.isZooming() || !OsmandPlugin.isDevelopment()) {
			if ((System.currentTimeMillis() - lastZoom > 1000) || !OsmandPlugin.isDevelopment()) {
				zoomText.setVisibility(View.GONE);
			} else {
				zoomText.setVisibility(View.VISIBLE);
				zoomText.setTextColor(textColor);
				zoomText.setText(getZoomLevel(tileBox));
			}
		}

		mapRouteInfoMenu.setVisible(showRouteCalculationControls);
		if (!forceHideCompass) {
			updateCompass(isNight);
		}

		for (MapHudButton mc : controls) {
			mc.update(mapActivity.getMyApplication(), isNight);
		}
	}

	public void updateCompass(boolean isNight) {
		float mapRotate = mapActivity.getMapView().getRotate();
		boolean showCompass = shouldShowCompass();
		if (mapRotate != cachedRotate) {
			cachedRotate = mapRotate;
			// Apply animation to image view
			compassHud.iv.invalidate();
			compassHud.updateVisibility(showCompass);
		}
		if (settings.ROTATE_MAP.get() == OsmandSettings.ROTATE_MAP_NONE) {
			compassHud.setIconResId(isNight ? R.drawable.map_compass_niu_white : R.drawable.map_compass_niu);
			compassHud.iv.setContentDescription(mapActivity.getString(R.string.rotate_map_none_opt));
			compassHud.updateVisibility(showCompass);
		} else if (settings.ROTATE_MAP.get() == OsmandSettings.ROTATE_MAP_BEARING) {
			compassHud.setIconResId(isNight ? R.drawable.map_compass_bearing_white : R.drawable.map_compass_bearing);
			compassHud.iv.setContentDescription(mapActivity.getString(R.string.rotate_map_bearing_opt));
			compassHud.updateVisibility(true);
		} else {
			compassHud.setIconResId(isNight ? R.drawable.map_compass_white : R.drawable.map_compass);
			compassHud.iv.setContentDescription(mapActivity.getString(R.string.rotate_map_compass_opt));
			compassHud.updateVisibility(true);
		}
	}

	private boolean shouldShowCompass() {
		float mapRotate = mapActivity.getMapView().getRotate();
		return forceShowCompass || mapRotate != 0
				|| settings.ROTATE_MAP.get() != OsmandSettings.ROTATE_MAP_NONE
				|| mapActivity.getMapLayers().getMapInfoLayer().getMapInfoControls().isVisible("compass");
	}

	public CompassDrawable getCompassDrawable(Drawable originalDrawable) {
		return new CompassDrawable(originalDrawable);
	}

	private void updateMyLocation(RoutingHelper rh, boolean dialogOpened) {
		boolean portrait = isPotrait();
		Location lastKnownLocation = mapActivity.getMyApplication().getLocationProvider().getLastKnownLocation();
		boolean enabled = lastKnownLocation != null;
		boolean tracked = mapActivity.getMapViewTrackingUtilities().isMapLinkedToLocation();

		if (!enabled) {
			backToLocationControl.setBg(R.drawable.btn_circle, R.drawable.btn_circle_night);
			backToLocationControl.setIconColorId(R.color.icon_color_default_light, 0);
			backToLocationControl.iv.setContentDescription(mapActivity.getString(R.string.unknown_location));
		} else if (tracked) {
			backToLocationControl.setBg(R.drawable.btn_circle, R.drawable.btn_circle_night);
			backToLocationControl.setIconColorId(R.color.color_myloc_distance);
			backToLocationControl.iv.setContentDescription(mapActivity.getString(R.string.access_map_linked_to_location));
		} else {
			backToLocationControl.setIconColorId(0);
			backToLocationControl.setBg(R.drawable.btn_circle_blue);
			backToLocationControl.iv.setContentDescription(mapActivity.getString(R.string.map_widget_back_to_loc));
		}
		boolean visible = !(tracked && rh.isFollowingMode());
		backToLocationControl.updateVisibility(visible && !dialogOpened && !isInPlanRouteMode() && (!isInChoosingRoutesMode() || !isInWaypointsChoosingMode() || !portrait));
		if (app.accessibilityEnabled()) {
			backToLocationControl.iv.setClickable(enabled && visible);
		}
	}


	public boolean onSingleTap(PointF point, RotatedTileBox tileBox) {
		return mapRouteInfoMenu.onSingleTap(point, tileBox);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event, RotatedTileBox tileBox) {
		touchEvent = System.currentTimeMillis();
		RoutingHelper rh = mapActivity.getRoutingHelper();
		if (rh.isFollowingMode()) {
			mapActivity.refreshMap();
		}
		return false;
	}

	// /////////////// Transparency bar /////////////////////////
	private void initTransparencyBar() {
		transparencyBarLayout = (LinearLayout) mapActivity.findViewById(R.id.map_transparency_layout);
		transparencyBar = (SeekBar) mapActivity.findViewById(R.id.map_transparency_seekbar);
		transparencyBar.setMax(255);
		if (transparencySetting != null) {
			transparencyBar.setProgress(transparencySetting.get());
			transparencyBarLayout.setVisibility(View.VISIBLE);
		} else {
			transparencyBarLayout.setVisibility(View.GONE);
		}
		transparencyBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (transparencySetting != null) {
					transparencySetting.set(progress);
					mapActivity.getMapView().refreshMap();
				}
			}
		});

		LayerTransparencySeekbarMode seekbarMode = settings.LAYER_TRANSPARENCY_SEEKBAR_MODE.get();
		if (OsmandPlugin.getEnabledPlugin(OsmandRasterMapsPlugin.class) != null) {
			if (seekbarMode == LayerTransparencySeekbarMode.OVERLAY && settings.MAP_OVERLAY.get() != null) {
				showTransparencyBar(settings.MAP_OVERLAY_TRANSPARENCY, true);
			} else if (seekbarMode == LayerTransparencySeekbarMode.UNDERLAY && settings.MAP_UNDERLAY.get() != null) {
				showTransparencyBar(settings.MAP_TRANSPARENCY, true);
			}
		}
	}

	public void showTransparencyBar(CommonPreference<Integer> transparenPreference,
									boolean isTransparencyBarEnabled) {
		this.isTransparencyBarEnabled = isTransparencyBarEnabled;
		if (MapControlsLayer.transparencySetting != transparenPreference) {
			MapControlsLayer.transparencySetting = transparenPreference;

		}
		if (transparenPreference != null && isTransparencyBarEnabled) {
			transparencyBarLayout.setVisibility(View.VISIBLE);
			transparencyBar.setProgress(transparenPreference.get());
		} else {
			transparencyBarLayout.setVisibility(View.GONE);
		}
	}

	public void hideTransparencyBar() {
		transparencyBarLayout.setVisibility(View.GONE);
		transparencySetting = null;
	}

	private class MapHudButton {
		View iv;
		int bgDark;
		int bgLight;
		int resId;
		int resLightId;
		int resDarkId;
		int resClrLight = R.color.icon_color_default_light;
		int resClrDark = 0;
		String id;

		boolean nightMode = false;
		boolean f = true;
		boolean compass;
		boolean compassOutside;
		boolean forceHideCompass;
		ViewPropertyAnimatorCompat hideAnimator;

		public MapHudButton setRoundTransparent() {
			setBg(R.drawable.btn_circle_trans, R.drawable.btn_circle_night);
			return this;
		}


		public MapHudButton setBg(int dayBg, int nightBg) {
			if (bgDark == nightBg && dayBg == bgLight) {
				return this;
			}
			bgDark = nightBg;
			bgLight = dayBg;
			f = true;
			return this;
		}

		public void hideDelayed(long msec) {
			if (!compassOutside && (iv.getVisibility() == View.VISIBLE)) {
				if (hideAnimator != null) {
					hideAnimator.cancel();
				}
				hideAnimator = ViewCompat.animate(iv).alpha(0f).setDuration(250).setStartDelay(msec).setListener(new ViewPropertyAnimatorListener() {
					@Override
					public void onAnimationStart(View view) {
					}

					@Override
					public void onAnimationEnd(View view) {
						iv.setVisibility(View.GONE);
						ViewCompat.setAlpha(iv, 1f);
						hideAnimator = null;
					}

					@Override
					public void onAnimationCancel(View view) {
						iv.setVisibility(View.GONE);
						ViewCompat.setAlpha(iv, 1f);
						hideAnimator = null;
					}
				});
				hideAnimator.start();
			}
		}

		public void cancelHideAnimation() {
			if (hideAnimator != null) {
				hideAnimator.cancel();
			}
		}

		public boolean updateVisibility(boolean visible) {
			if (visible) {
				visible = appCustomization.isFeatureEnabled(id);
			}
			if (!compassOutside && visible != (iv.getVisibility() == View.VISIBLE)) {
				if (visible) {
					if (hideAnimator != null) {
						hideAnimator.cancel();
					}
					iv.setVisibility(View.VISIBLE);
					iv.invalidate();
				} else if (hideAnimator == null) {
					if (compass && !forceHideCompass) {
						hideDelayed(5000);
					} else {
						forceHideCompass = false;
						iv.setVisibility(View.GONE);
						iv.invalidate();
					}
				}
				return true;
			} else if (visible && hideAnimator != null) {
				hideAnimator.cancel();
				iv.setVisibility(View.VISIBLE);
				iv.invalidate();
			}
			return false;
		}

		public MapHudButton setBg(int bg) {
			if (bgDark == bg && bg == bgLight) {
				return this;
			}
			bgDark = bg;
			bgLight = bg;
			f = true;
			return this;
		}

		public boolean setIconResId(int resId) {
			if (this.resId == resId) {
				return false;
			}
			this.resId = resId;
			f = true;
			return true;
		}

		public boolean resetIconColors() {
			if (resClrLight == R.color.icon_color_default_light && resClrDark == 0) {
				return false;
			}
			resClrLight = R.color.icon_color_default_light;
			resClrDark = 0;
			f = true;
			return true;
		}

		public MapHudButton setIconColorId(int clr) {
			if (resClrLight == clr && resClrDark == clr) {
				return this;
			}
			resClrLight = clr;
			resClrDark = clr;
			f = true;
			return this;
		}

		public MapHudButton setIconsId(int icnLight, int icnDark) {
			if (resLightId == icnLight && resDarkId == icnDark) {
				return this;
			}
			resLightId = icnLight;
			resDarkId = icnDark;
			f = true;
			return this;
		}

		public MapHudButton setIconColorId(int clrLight, int clrDark) {
			if (resClrLight == clrLight && resClrDark == clrDark) {
				return this;
			}
			resClrLight = clrLight;
			resClrDark = clrDark;
			f = true;
			return this;
		}

		@SuppressLint("NewApi")
		@SuppressWarnings("deprecation")
		public void update(OsmandApplication ctx, boolean night) {
			if (nightMode == night && !f) {
				return;
			}
			f = false;
			nightMode = night;
			if (bgDark != 0 && bgLight != 0) {
				if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
					iv.setBackground(ctx.getResources().getDrawable(night ? bgDark : bgLight,
							mapActivity.getTheme()));
				} else {
					iv.setBackgroundDrawable(ctx.getResources().getDrawable(night ? bgDark : bgLight));
				}
			}
			Drawable d = null;
			if (resDarkId != 0 && nightMode) {
				d = ctx.getUIUtilities().getIcon(resDarkId);
			} else if (resLightId != 0 && !nightMode) {
				d = ctx.getUIUtilities().getIcon(resLightId);
			} else if (resId != 0) {
				d = ctx.getUIUtilities().getIcon(resId, nightMode ? resClrDark : resClrLight);
			}

			if (iv instanceof ImageView) {
				if (compass) {
					((ImageView) iv).setImageDrawable(new CompassDrawable(d));
				} else {
					((ImageView) iv).setImageDrawable(d);
				}
			} else if (iv instanceof TextView) {
				((TextView) iv).setCompoundDrawablesWithIntrinsicBounds(
						d, null, null, null);
			}
		}

	}

	private String getZoomLevel(@NonNull RotatedTileBox tb) {
		String zoomText = tb.getZoom() + "";
		double frac = tb.getMapDensity();
		if (frac != 0) {
			int ifrac = (int) (frac * 10);
			zoomText += " ";
			zoomText += Math.abs(ifrac) / 10;
			if (ifrac % 10 != 0) {
				zoomText += "." + Math.abs(ifrac) % 10;
			}
		}
		return zoomText;
	}

	public void setMapQuickActionLayer(MapQuickActionLayer mapQuickActionLayer) {
		this.mapQuickActionLayer = mapQuickActionLayer;
	}

	private boolean isInMovingMarkerMode() {
		return mapQuickActionLayer == null ? contextMenuLayer.isInChangeMarkerPositionMode() || contextMenuLayer.isInAddGpxPointMode() :
				mapQuickActionLayer.isInMovingMarkerMode() || contextMenuLayer.isInChangeMarkerPositionMode() || contextMenuLayer.isInAddGpxPointMode();
	}

	private boolean isInGpxDetailsMode() {
		return contextMenuLayer.isInGpxDetailsMode();
	}

	private boolean isInMeasurementToolMode() {
		return mapActivity.getMapLayers().getMeasurementToolLayer().isInMeasurementMode();
	}

	private boolean isInPlanRouteMode() {
		return mapActivity.getMapLayers().getMapMarkersLayer().isInPlanRouteMode();
	}

	private boolean isInChoosingRoutesMode() {
		return MapRouteInfoMenu.chooseRoutesVisible;
	}

	private boolean isInWaypointsChoosingMode() {
		return  MapRouteInfoMenu.waypointsVisible;
	}

	public static View.OnLongClickListener getOnClickMagnifierListener(final OsmandMapTileView view) {
		return new View.OnLongClickListener() {

			@Override
			public boolean onLongClick(View notUseCouldBeNull) {
				final OsmandSettings.OsmandPreference<Float> mapDensity = view.getSettings().MAP_DENSITY;
				final AlertDialog.Builder bld = new AlertDialog.Builder(view.getContext());
				int p = (int) (mapDensity.get() * 100);
				final TIntArrayList tlist = new TIntArrayList(new int[]{25, 33, 50, 75, 100, 125, 150, 200, 300, 400});
				final List<String> values = new ArrayList<>();
				int i = -1;
				for (int k = 0; k <= tlist.size(); k++) {
					final boolean end = k == tlist.size();
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
				bld.setSingleChoiceItems(values.toArray(new String[values.size()]), i,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								int p = tlist.get(which);
								mapDensity.set(p / 100.0f);
								view.setComplexZoom(view.getZoom(), view.getSettingsMapDensity());
								MapRendererContext mapContext = NativeCoreContext.getMapRendererContext();
								if (mapContext != null) {
									mapContext.updateMapSettings();
								}
								dialog.dismiss();
							}
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
			mapRouteInfoMenu.selectAddress(name != null ? name : "", new LatLon(latitude, longitude), pointType);
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
							addDestination(requestedLatLon);
							break;
					}
				} else if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
					app.showToastMessage(R.string.ask_for_location_permission);
				}
			}
		}
	}
}
