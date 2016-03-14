package net.osmand.plus.views;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import net.londatiga.android.ActionItem;
import net.londatiga.android.QuickAction;
import net.osmand.AndroidUtils;
import net.osmand.core.android.MapRendererContext;
import net.osmand.data.LatLon;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.CommonPreference;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.TargetPointsHelper.TargetPoint;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.search.SearchAddressFragment;
import net.osmand.plus.dashboard.DashboardOnMap.DashboardType;
import net.osmand.plus.mapcontextmenu.other.MapRouteInfoMenu;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.views.corenative.NativeCoreContext;

import java.util.ArrayList;
import java.util.List;

import gnu.trove.list.array.TIntArrayList;

public class MapControlsLayer extends OsmandMapLayer {

	private static final int TIMEOUT_TO_SHOW_BUTTONS = 7000;
	public static final int REQUEST_ADDRESS_SELECT = 2;
	private static final int REQUEST_LOCATION_FOR_NAVIGATION_PERMISSION = 200;

	public MapHudButton createHudButton(View iv, int resId) {
		MapHudButton mc = new MapHudButton();
		mc.iv = iv;
		mc.resId = resId;
		return mc;
	}

	private List<MapHudButton> controls = new ArrayList<>();
	private final MapActivity mapActivity;
	private int shadowColor = -1;
	// private RulerControl rulerControl;
	// private List<MapControls> allControls = new ArrayList<MapControls>();

	private SeekBar transparencyBar;
	private LinearLayout transparencyBarLayout;
	private static CommonPreference<Integer> settingsToTransparency;
	private boolean isTransparencyBarEnabled = true;
	private OsmandSettings settings;

	private MapRouteInfoMenu mapRouteInfoMenu;
	private MapHudButton backToLocationControl;
	private MapHudButton menuControl;
	private MapHudButton compassHud;
	private float cachedRotate = 0;
	private ImageView appModeIcon;
	private TextView zoomText;
	private OsmandMapTileView mapView;
	private OsmandApplication app;
	private View mapAppModeShadow;
	private MapHudButton routePlanningBtn;
	private long touchEvent;
	private MapHudButton mapZoomOut;
	private MapHudButton mapZoomIn;
	private MapHudButton layersHud;
	private long lastZoom;
	private boolean hasTargets;

	public MapControlsLayer(MapActivity activity) {
		this.mapActivity = activity;
		app = activity.getMyApplication();
		settings = activity.getMyApplication().getSettings();
		mapView = mapActivity.getMapView();
	}

	public MapRouteInfoMenu getMapRouteInfoMenu() {
		return mapRouteInfoMenu;
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
		layersHud = createHudButton(configureMap, R.drawable.map_layer_dark)
				.setIconsId(R.drawable.map_layer_dark, R.drawable.map_layer_night)
				.setBg(R.drawable.btn_inset_circle_trans, R.drawable.btn_inset_circle_night);
		controls.add(layersHud);
		configureMap.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				MapActivity.clearPrevActivityIntent();
				mapActivity.getDashboard().setDashboardVisibility(true, DashboardType.CONFIGURE_MAP);
			}
		});

		View compass = mapActivity.findViewById(R.id.map_compass_button);
		compassHud = createHudButton(compass, R.drawable.map_compass).setIconColorId(0).
				setBg(R.drawable.btn_inset_circle_trans, R.drawable.btn_inset_circle_night);
		compassHud.compass = true;
		controls.add(compassHud);
		compass.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mapActivity.getMapViewTrackingUtilities().switchRotateMapMode();
			}
		});

	}

	private void initRouteControls() {
		mapRouteInfoMenu = new MapRouteInfoMenu(mapActivity, this);
	}

	public void updateRouteButtons(View main, boolean routeInfo) {
		boolean nightMode = mapActivity.getMyApplication().getDaynightHelper().isNightModeForMapControls();
		ImageView cancelRouteButton = (ImageView) main.findViewById(R.id.map_cancel_route_button);
		cancelRouteButton.setImageDrawable(app.getIconsCache().getContentIcon(R.drawable.map_action_cancel, !nightMode));
		AndroidUtils.setBackground(mapActivity, cancelRouteButton, nightMode, R.drawable.dashboard_button_light, R.drawable.dashboard_button_dark);
		cancelRouteButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				clickRouteCancel();
			}
		});

		ImageView waypointsButton = (ImageView) main.findViewById(R.id.map_waypoints_route_button);
		waypointsButton.setImageDrawable(app.getIconsCache().getContentIcon(R.drawable.map_action_waypoint, !nightMode));
		AndroidUtils.setBackground(mapActivity, waypointsButton, nightMode, R.drawable.dashboard_button_light, R.drawable.dashboard_button_dark);
		waypointsButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				clickRouteWaypoints();
			}
		});

		ImageView options = (ImageView) main.findViewById(R.id.map_options_route_button);
		options.setImageDrawable(!routeInfo ? app.getIconsCache().getIcon(R.drawable.map_action_settings,
				R.color.osmand_orange) : app.getIconsCache().getContentIcon(R.drawable.map_action_settings, !nightMode));
		AndroidUtils.setBackground(mapActivity, options, nightMode, R.drawable.dashboard_button_light, R.drawable.dashboard_button_dark);
		options.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				clickRouteParams();
			}
		});

		TextView routeGoButton = (TextView) main.findViewById(R.id.map_go_route_button);
		routeGoButton.setCompoundDrawablesWithIntrinsicBounds(app.getIconsCache().getIcon(R.drawable.map_start_navigation, R.color.color_myloc_distance), null, null, null);
		routeGoButton.setText(mapActivity.getString(R.string.shared_string_go));
		AndroidUtils.setTextSecondaryColor(mapActivity, routeGoButton, nightMode);
		AndroidUtils.setBackground(mapActivity, routeGoButton, nightMode, R.drawable.dashboard_button_light, R.drawable.dashboard_button_dark);
		routeGoButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				clickRouteGo();
			}
		});
	}

	public void setControlsClickable(boolean clickable) {
		for (MapHudButton mb : controls) {
			mb.iv.setClickable(clickable);
		}
	}

	private TargetPointsHelper getTargets() {
		return mapActivity.getMyApplication().getTargetPointsHelper();
	}

	protected void clickRouteParams() {
		mapActivity.getMapActions().openRoutePreferencesDialog();
	}

	protected void clickRouteWaypoints() {
		if (getTargets().checkPointToNavigateShort()) {
			mapActivity.getMapActions().openIntermediatePointsDialog();
		}
	}

	protected void clickRouteCancel() {
		mapRouteInfoMenu.hide();
		if (mapActivity.getRoutingHelper().isFollowingMode()) {
			mapActivity.getMapActions().stopNavigationActionConfirm();
		} else {
			mapActivity.getMapActions().stopNavigationWithoutConfirm();
		}
	}

	protected void clickRouteGo() {
		if (app.getTargetPointsHelper().getPointToNavigate() != null) {
			mapRouteInfoMenu.hide();
		}
		startNavigation();
	}

	public void showRouteInfoControlDialog() {
		mapRouteInfoMenu.showHideMenu();
	}

	public void showDialog() {
		mapRouteInfoMenu.setShowMenu();
	}

	private void initControls() {
		View backToLocation = mapActivity.findViewById(R.id.map_my_location_button);
		backToLocationControl = createHudButton(backToLocation, R.drawable.map_my_location)
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
		controls.add(createHudButton(mapActivity.findViewById(R.id.map_app_mode_shadow), 0).setBg(
				R.drawable.btn_round_trans, R.drawable.btn_round_transparent));
		View backToMenuButton = mapActivity.findViewById(R.id.map_menu_button);

		final boolean dash = settings.SHOW_DASHBOARD_ON_MAP_SCREEN.get();
		menuControl = createHudButton(backToMenuButton,
				!dash ? R.drawable.map_drawer : R.drawable.map_dashboard).setBg(
				R.drawable.btn_round, R.drawable.btn_round_night);
		controls.add(menuControl);
		backToMenuButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				MapActivity.clearPrevActivityIntent();
				if (dash) {
					mapActivity.getDashboard().setDashboardVisibility(true, DashboardType.DASHBOARD);
				} else {
					mapActivity.openDrawer();
				}
			}
		});
		mapAppModeShadow = mapActivity.findViewById(R.id.map_app_mode_shadow);
		mapAppModeShadow.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				onApplicationModePress(v);
			}
		});
		appModeIcon = (ImageView) mapActivity.findViewById(R.id.map_app_mode_icon);
		zoomText = (TextView) mapActivity.findViewById(R.id.map_app_mode_text);

		View routePlanButton = mapActivity.findViewById(R.id.map_route_info_button);
		routePlanningBtn = createHudButton(routePlanButton, R.drawable.map_directions).setBg(
				R.drawable.btn_round, R.drawable.btn_round_night);
		controls.add(routePlanningBtn);
		routePlanButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				doRoute(false);
			}
		});
	}

	public void doRoute(boolean hasTargets) {
		this.hasTargets = hasTargets;
		if (OsmAndLocationProvider.isLocationPermissionAvailable(mapActivity)) {
			onNavigationClick();
		} else {
			ActivityCompat.requestPermissions(mapActivity,
					new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
					REQUEST_LOCATION_FOR_NAVIGATION_PERMISSION);
		}
	}

	public void doNavigate() {
		mapRouteInfoMenu.hide();
		startNavigation();
	}

	private void onNavigationClick() {
		MapActivity.clearPrevActivityIntent();
		RoutingHelper routingHelper = mapActivity.getRoutingHelper();
		if (!routingHelper.isFollowingMode() && !routingHelper.isRoutePlanningMode()) {
			if (settings.USE_MAP_MARKERS.get() && !hasTargets) {
				getTargets().restoreTargetPoints(false);
				if (getTargets().getPointToNavigate() == null) {
					mapActivity.getMapActions().setFirstMapMarkerAsTarget();
				}
			}
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
		mapZoomIn = createHudButton(zoomInButton, R.drawable.map_zoom_in).
				setIconsId(R.drawable.map_zoom_in, R.drawable.map_zoom_in_night).setRoundTransparent();
		controls.add(mapZoomIn);
		zoomInButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
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
		mapZoomOut = createHudButton(zoomOutButton, R.drawable.map_zoom_out).
				setIconsId(R.drawable.map_zoom_out, R.drawable.map_zoom_out_night).setRoundTransparent();
		controls.add(mapZoomOut);
		zoomOutButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mapActivity.changeZoom(-1, System.currentTimeMillis());
			}
		});
		zoomOutButton.setOnLongClickListener(listener);
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
				app.getSettings().APPLICATION_MODE.set(routingHelper.getAppMode());
				mapActivity.getMapViewTrackingUtilities().backToLocationImpl();
				app.getSettings().FOLLOW_THE_ROUTE.set(true);
				routingHelper.setFollowingMode(true);
				routingHelper.setRoutePlanningMode(false);
				mapActivity.getMapViewTrackingUtilities().switchToRoutePlanningMode();
				app.getRoutingHelper().notifyIfRouteIsCalculated();
				routingHelper.setCurrentLocation(app.getLocationProvider().getLastKnownLocation(), false);
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

	@SuppressWarnings("deprecation")
	private void updateControls(@NonNull RotatedTileBox tileBox, DrawSettings nightMode) {
		boolean isNight = nightMode != null && nightMode.isNightMode();
		int shadw = isNight ? Color.TRANSPARENT : Color.WHITE;
		int textColor = isNight ? mapActivity.getResources().getColor(R.color.widgettext_night) : Color.BLACK;
		if (shadowColor != shadw) {
			shadowColor = shadw;
			// TODOnightMode
			// updatextColor(textColor, shadw, rulerControl, zoomControls, mapMenuControls);
		}
		// default buttons
		boolean routePlanningMode = false;
		RoutingHelper rh = mapActivity.getRoutingHelper();
		if (rh.isRoutePlanningMode()) {
			routePlanningMode = true;
		} else if ((rh.isRouteCalculated() || rh.isRouteBeingCalculated()) && !rh.isFollowingMode()) {
			routePlanningMode = true;
		}
		boolean routeFollowingMode = !routePlanningMode && rh.isFollowingMode();
		boolean dialogOpened = mapRouteInfoMenu.isVisible();
		boolean showRouteCalculationControls = routePlanningMode ||
				((System.currentTimeMillis() - touchEvent < TIMEOUT_TO_SHOW_BUTTONS) && routeFollowingMode);
		updateMyLocation(rh, dialogOpened);
		boolean showButtons = (showRouteCalculationControls || !routeFollowingMode);
		//routePlanningBtn.setIconResId(routeFollowingMode ? R.drawable.ic_action_gabout_dark : R.drawable.map_directions);
		if (rh.isFollowingMode()) {
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

		mapZoomIn.updateVisibility(!dialogOpened);
		mapZoomOut.updateVisibility(!dialogOpened);
		compassHud.updateVisibility(!dialogOpened);
		layersHud.updateVisibility(!dialogOpened);

		if (routePlanningMode || routeFollowingMode) {
			mapAppModeShadow.setVisibility(View.GONE);
		} else {
			if (mapView.isZooming()) {
				lastZoom = System.currentTimeMillis();
			}
			mapAppModeShadow.setVisibility(View.VISIBLE);
			//if (!mapView.isZooming() || !OsmandPlugin.isDevelopment()) {
			if ((System.currentTimeMillis() - lastZoom > 1000) || !OsmandPlugin.isDevelopment()) {
				zoomText.setVisibility(View.GONE);
				appModeIcon.setVisibility(View.VISIBLE);
				appModeIcon.setImageDrawable(
						app.getIconsCache().getIcon(
								settings.getApplicationMode().getSmallIconDark(), !isNight));
			} else {
				appModeIcon.setVisibility(View.GONE);
				zoomText.setVisibility(View.VISIBLE);
				zoomText.setTextColor(textColor);
				zoomText.setText(getZoomLevel(tileBox));
			}
		}

		mapRouteInfoMenu.setVisible(showRouteCalculationControls);
		updateCompass(isNight);

		for (MapHudButton mc : controls) {
			mc.update(mapActivity.getMyApplication(), isNight);
		}
	}

	private void updateCompass(boolean isNight) {
		float mapRotate = mapActivity.getMapView().getRotate();
		if (mapRotate != cachedRotate) {
			cachedRotate = mapRotate;
			// Aply animation to image view
			compassHud.iv.invalidate();
		}
		if (settings.ROTATE_MAP.get() == OsmandSettings.ROTATE_MAP_NONE) {
			compassHud.setIconResId(isNight ? R.drawable.map_compass_niu_white : R.drawable.map_compass_niu);
		} else if (settings.ROTATE_MAP.get() == OsmandSettings.ROTATE_MAP_BEARING) {
			compassHud.setIconResId(isNight ? R.drawable.map_compass_bearing_white : R.drawable.map_compass_bearing);
		} else {
			compassHud.setIconResId(isNight ? R.drawable.map_compass_white : R.drawable.map_compass);
		}
	}

	private void updateMyLocation(RoutingHelper rh, boolean dialogOpened) {
		boolean enabled = mapActivity.getMyApplication().getLocationProvider().getLastKnownLocation() != null;
		boolean tracked = mapActivity.getMapViewTrackingUtilities().isMapLinkedToLocation();

		if (!enabled) {
			backToLocationControl.setBg(R.drawable.btn_circle, R.drawable.btn_circle_night);
			backToLocationControl.setIconColorId(R.color.icon_color, 0);
		} else if (tracked) {
			backToLocationControl.setBg(R.drawable.btn_circle, R.drawable.btn_circle_night);
			backToLocationControl.setIconColorId(R.color.color_myloc_distance);
		} else {
			backToLocationControl.setIconColorId(0);
			backToLocationControl.setBg(R.drawable.btn_circle_blue);
		}
		boolean visible = !(tracked && rh.isFollowingMode());
		backToLocationControl.updateVisibility(visible && !dialogOpened);
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
		if (settingsToTransparency != null) {
			transparencyBar.setProgress(settingsToTransparency.get());
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
				if (settingsToTransparency != null) {
					settingsToTransparency.set(progress);
					mapActivity.getMapView().refreshMap();
				}
			}
		});
		ImageButton imageButton = (ImageButton) mapActivity.findViewById(R.id.map_transparency_hide);
		imageButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				transparencyBarLayout.setVisibility(View.GONE);
				settings.SHOW_LAYER_TRANSPARENCY_SEEKBAR.set(false);
				hideTransparencyBar(settingsToTransparency);
			}
		});
	}

	public void showTransparencyBar(CommonPreference<Integer> transparenPreference) {
		if (MapControlsLayer.settingsToTransparency != transparenPreference) {
			MapControlsLayer.settingsToTransparency = transparenPreference;
			if (isTransparencyBarEnabled) {
				transparencyBarLayout.setVisibility(View.VISIBLE);
			}
			transparencyBar.setProgress(transparenPreference.get());
		}
	}

	public void hideTransparencyBar(CommonPreference<Integer> transparentPreference) {
		if (settingsToTransparency == transparentPreference) {
			transparencyBarLayout.setVisibility(View.GONE);
			settingsToTransparency = null;
		}
	}

	public void setTransparencyBarEnabled(boolean isTransparencyBarEnabled) {
		this.isTransparencyBarEnabled = isTransparencyBarEnabled;
		if (settingsToTransparency != null) {
			if(isTransparencyBarEnabled) {
				transparencyBarLayout.setVisibility(View.VISIBLE);
			} else {
				transparencyBarLayout.setVisibility(View.GONE);
			}
		}
	}

	public boolean isTransparencyBarInitialized() {
		return settingsToTransparency != null;
	}

	private class MapHudButton {
		View iv;
		int bgDark;
		int bgLight;
		int resId;
		int resLightId;
		int resDarkId;
		int resClrLight = R.color.icon_color;
		int resClrDark = 0;


		boolean nightMode = false;
		boolean f = true;
		boolean compass;

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

		public boolean updateVisibility(boolean visible) {
			if (visible != (iv.getVisibility() == View.VISIBLE)) {
				if (visible) {
					iv.setVisibility(View.VISIBLE);
				} else {
					iv.setVisibility(View.GONE);
				}
				iv.invalidate();
				return true;
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
			if (resClrLight == R.color.icon_color && resClrDark == 0) {
				return false;
			}
			resClrLight = R.color.icon_color;
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
				d = ctx.getIconsCache().getIcon(resDarkId);
			} else if (resLightId != 0 && !nightMode) {
				d = ctx.getIconsCache().getIcon(resLightId);
			} else if (resId != 0) {
				d = ctx.getIconsCache().getIcon(resId, nightMode ? resClrDark : resClrLight);
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

	private void onApplicationModePress(View v) {
		final QuickAction mQuickAction = new QuickAction(v);
		mQuickAction.setOnAnchorOnTop(true);
		List<ApplicationMode> vls = ApplicationMode.values(mapActivity.getMyApplication().getSettings());
		final ApplicationMode[] modes = vls.toArray(new ApplicationMode[vls.size()]);
		Drawable[] icons = new Drawable[vls.size()];
		int[] values = new int[vls.size()];
		for (int k = 0; k < modes.length; k++) {
			icons[k] = app.getIconsCache().getIcon(modes[k].getSmallIconDark(), R.color.icon_color);
			values[k] = modes[k].getStringResource();
		}
		for (int i = 0; i < modes.length; i++) {
			final ActionItem action = new ActionItem();
			action.setTitle(mapActivity.getResources().getString(values[i]));
			action.setIcon(icons[i]);
			final int j = i;
			action.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					mapActivity.getMyApplication().getSettings().APPLICATION_MODE.set(modes[j]);
					mQuickAction.dismiss();
				}
			});
			mQuickAction.addActionItem(action);
		}
		mQuickAction.setAnimStyle(QuickAction.ANIM_AUTO);
		mQuickAction.show();
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

	public static View.OnLongClickListener getOnClickMagnifierListener(final OsmandMapTileView view) {
		return new View.OnLongClickListener() {

			@Override
			public boolean onLongClick(View notUseCouldBeNull) {
				final OsmandSettings.OsmandPreference<Float> mapDensity = view.getSettings().MAP_DENSITY;
				final AlertDialog.Builder bld = new AlertDialog.Builder(view.getContext());
				int p = (int) (mapDensity.get() * 100);
				final TIntArrayList tlist = new TIntArrayList(new int[]{20, 25, 33, 50, 75, 100, 150, 200, 300, 400});
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

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_ADDRESS_SELECT && resultCode == SearchAddressFragment.SELECT_ADDRESS_POINT_RESULT_OK) {
			String name = data.getStringExtra(SearchAddressFragment.SELECT_ADDRESS_POINT_INTENT_KEY);
			boolean target = data.getBooleanExtra(MapRouteInfoMenu.TARGET_SELECT, true);
			LatLon latLon = new LatLon(
					data.getDoubleExtra(SearchAddressFragment.SELECT_ADDRESS_POINT_LAT, 0),
					data.getDoubleExtra(SearchAddressFragment.SELECT_ADDRESS_POINT_LON, 0));
			if (name != null) {
				mapRouteInfoMenu.selectAddress(name, latLon, target);
			} else {
				mapRouteInfoMenu.selectAddress("", latLon, target);
			}
		}
	}

	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		if (requestCode == REQUEST_LOCATION_FOR_NAVIGATION_PERMISSION
				&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {
			onNavigationClick();
		}
	}
}
