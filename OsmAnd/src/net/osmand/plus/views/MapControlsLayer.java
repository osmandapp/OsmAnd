package net.osmand.plus.views;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import net.osmand.PlatformUtil;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.CommonPreference;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.ScreenOrientationHelper;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.helpers.WaypointDialogHelper;
import net.osmand.plus.views.controls.MapRoutePlanControl;
import net.osmand.plus.views.controls.MapRoutePreferencesControl;
import net.osmand.plus.views.controls.MapCancelControl;
import net.osmand.plus.views.controls.MapControls;
import net.osmand.plus.views.controls.MapRouteInfoControl;
import net.osmand.plus.views.controls.MapMenuControls;
import net.osmand.plus.views.controls.MapNavigateControl;
import net.osmand.plus.views.controls.MapZoomControls;
import net.osmand.plus.views.controls.RulerControl;
import net.osmand.plus.views.controls.SmallMapMenuControls;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;

public class MapControlsLayer extends OsmandMapLayer {

	private static final int TIMEOUT_TO_SHOW_BUTTONS = 5000;
	private static class MapHudButton {
		ImageView iv;
		int bgDark;
		int bgLight;
		int resId;
		int resLight = R.color.icon_color_light;
		int resDark = 0;
				
		boolean nightMode = false;
		boolean f = true;
		
		
		public MapHudButton setRoundTransparent() {
			setBg(R.drawable.btn_circle_trans);
			return this;
		}
		
		public MapHudButton setBg(int dayBg, int nightBg) {
			bgDark = nightBg;
			bgLight = dayBg;
			f = true;
			return this;

		}
		
		public MapHudButton setBg(int bg) {
			bgDark = bg;
			bgLight = bg;
			f = true;
			return this;
		}
		
		public static MapHudButton create(ImageView iv, int resId) {
			MapHudButton mc = new MapHudButton();
			mc.iv = iv;
			mc.resId = resId;
			mc.setBg(R.drawable.btn_circle);
			return mc;
		}
		
		public boolean setIconResId(int resId) {
			if(this.resId == resId) {
				return false;
			}
			this.resId = resId;
			f = true;
			return true;
		}

		public boolean setIconColorId(int clr) {
			if(resLight == clr && resDark == clr) {
				return false;
			}
			resLight = clr;
			resDark = clr;
			f = true;
			return true;
		}
		
		public void update(OsmandApplication ctx, boolean night) {
			if (nightMode == night && !f) {
				return;
			}
			f = false;
			nightMode = night;
			if(bgDark != 0 && bgLight != 0) {
				iv.setBackgroundDrawable(ctx.getResources().getDrawable(night ? bgDark : bgLight));
			}
			iv.setImageDrawable(ctx.getIconsCache().getIcon(resId, nightMode ? resDark : resLight));
		}

		
		
	}
	
	private List<MapHudButton> controls = new ArrayList<MapControlsLayer.MapHudButton>(); 
	private final MapActivity mapActivity;
	private int shadowColor = -1;
//	private RulerControl rulerControl;
//	private List<MapControls> allControls = new ArrayList<MapControls>();
	
	private float scaleCoefficient;

	private SeekBar transparencyBar;
	private LinearLayout transparencyBarLayout;
	private static CommonPreference<Integer> settingsToTransparency;
	private OsmandSettings settings;

	private MapHudButton backToLocationControl;
	private MapHudButton menuControl;
	private View routePreparationLayout;
	private MapRouteInfoControl mapRouteInfoControl;
	private MapHudButton optionsRouteControl;


	private MapRoutePreferencesControl optionsRouteControlDialog;


	public MapControlsLayer(MapActivity activity){
		this.mapActivity = activity;
		settings = activity.getMyApplication().getSettings();
	}
	
	
	


	@Override
	public boolean drawInScreenPixels() {
		return true;
	}

	@Override
	public void initLayer(final OsmandMapTileView view) {
		scaleCoefficient = view.getScaleCoefficient();
		FrameLayout parent = getParent();
		initNewControls();
		// TODO
//		rulerControl = init(new RulerControl(zoomControls, mapActivity, showUIHandler, scaleCoefficient), parent, 
//				rightGravity);
		initTransparencyBar(view, parent);
	}

	private void initNewControls() {
		initZooms();
		initControls();
		initRouteControls();
	}

	
	private void initRouteControls() {
		routePreparationLayout = mapActivity.findViewById(R.id.map_route_preparation_layout);
		View cancelRouteButton = mapActivity.findViewById(R.id.map_cancel_route_button);
		controls.add(MapHudButton.create((ImageView) cancelRouteButton, R.drawable.ic_action_remove_dark));
		
		cancelRouteButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				notifyClicked();
				if(mapActivity.getRoutingHelper().isFollowingMode()) {
					mapActivity.getMapActions().stopNavigationActionConfirm(mapActivity.getMapView());
				} else {
					mapActivity.getMapActions().stopNavigationWithoutConfirm();
				}
			}
		});
		mapRouteInfoControl = new MapRouteInfoControl(mapActivity.getMapLayers().getContextMenuLayer(),
				mapActivity);
		
		View waypointsButton = mapActivity.findViewById(R.id.map_waypoints_route_button);
		controls.add(MapHudButton.create((ImageView) waypointsButton, R.drawable.ic_action_flage_dark));
		waypointsButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				notifyClicked();
				mapRouteInfoControl.showHideDialog();
			}
		});
		
		View optionsRouteButton = mapActivity.findViewById(R.id.map_options_route_button);
		optionsRouteControl = MapHudButton.create((ImageView) optionsRouteButton, settings.getApplicationMode().getSmallIcon(false));
		optionsRouteControlDialog = new MapRoutePreferencesControl(mapActivity);
		controls.add(optionsRouteControl);
		optionsRouteButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				notifyClicked();
				optionsRouteControlDialog.showAndHideDialog();
			}
		});
	}
	
	public void showDialog(){
		mapRouteInfoControl.setShowDialog();
	}

	private void initControls() {
		View backToLocation = mapActivity.findViewById(R.id.map_my_location_button);
		backToLocationControl = MapHudButton.create((ImageView) backToLocation, R.drawable.ic_action_get_my_location).setBg(R.drawable.btn_circle_blue)
				;
		controls.add(backToLocationControl);
		
		backToLocation.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mapActivity.getMapViewTrackingUtilities().backToLocationImpl();
			}
		});		
		
		View backToMenuButton = mapActivity.findViewById(R.id.map_menu_button);
		menuControl = MapHudButton.create((ImageView) backToMenuButton, R.drawable.ic_navigation_drawer).
		setBg(R.drawable.btn_round, R.drawable.btn_round_night);
		controls.add(menuControl);
		backToMenuButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// double lat = activity.getMapView().getLatitude();
				// double lon = activity.getMapView().getLongitude();
				// MainMenuActivity.backToMainMenuDialog(activity, new LatLon(lat, lon));
				notifyClicked();
				if(mapActivity.getMyApplication().getSettings().USE_DASHBOARD_INSTEAD_OF_DRAWER.get()) {
					mapActivity.getDashboard().setDashboardVisibility(true);
				} else {
					mapActivity.getMapActions().onDrawerBack();
					mapActivity.getMapActions().toggleDrawer();
				}
			}
		});
		
		View routePlanButton = mapActivity.findViewById(R.id.map_route_info_button);
		controls.add(MapHudButton.create((ImageView) routePlanButton, R.drawable.ic_action_info_dark).
				setBg(R.drawable.btn_round, R.drawable.btn_round_night));
		routePlanButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				notifyClicked();
				mapActivity.getRoutingHelper().setRoutePlanningMode(true);
				mapActivity.getMapViewTrackingUtilities().switchToRoutePlanningMode();
				mapActivity.refreshMap();
			}
		});
	}


	private void initZooms() {
		final OsmandMapTileView view = mapActivity.getMapView();
		View zoomInButton = mapActivity.findViewById(R.id.map_zoom_in_button);
		controls.add(MapHudButton.create((ImageView) zoomInButton, R.drawable.ic_action_zoom_in).setRoundTransparent()
				);
		zoomInButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				notifyClicked();
				if (view.isZooming()) {
					mapActivity.changeZoom(2, System.currentTimeMillis());
				} else {
					mapActivity.changeZoom(1, System.currentTimeMillis());
				}

			}
		});
		final View.OnLongClickListener listener = MapZoomControls.getOnClickMagnifierListener(view);
		zoomInButton.setOnLongClickListener(listener);
		View zoomOutButton = mapActivity.findViewById(R.id.map_zoom_out_button);
		controls.add(MapHudButton.create((ImageView) zoomOutButton, R.drawable.ic_action_zoom_out).setRoundTransparent()
				);
		zoomOutButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				notifyClicked();
				mapActivity.changeZoom(-1, System.currentTimeMillis());
			}
		});
		zoomOutButton.setOnLongClickListener(listener);		
	}


	public void startNavigation() {
		// TODO
//		if (mapNavigationControl == null) {
//			return;
//		}
//		mapNavigationControl.startNavigation();
	}


	

	protected void notifyClicked(MapControls m) {
		notifyClicked();
	}
	
	protected void notifyClicked() {
		// TODO
//		if(mapNavigationControl != null) {
//			mapNavigationControl.stopCounter();
//		}
	}

	@Override
	public void destroyLayer() {
		controls.clear();
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings nightMode) {
		boolean isNight = nightMode != null && nightMode.isNightMode();
		int shadw = isNight ? Color.TRANSPARENT : Color.WHITE;
		int textColor = isNight ? mapActivity.getResources().getColor(R.color.widgettext_night) : Color.BLACK;
		if (shadowColor != shadw) {
			shadowColor = shadw;
			// TODO
//			updatextColor(textColor, shadw, rulerControl, zoomControls, mapMenuControls);
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
		boolean showRouteCalculationControls = routePlanningMode;

		boolean showDefaultButtons = !routePlanningMode
				&& (!routeFollowingMode || settings.SHOW_ZOOM_BUTTONS_NAVIGATION.get());
		// /////////////////////////////////////////////
		// new update
		boolean enabled = mapActivity.getMyApplication().getLocationProvider().getLastKnownLocation() != null;
		boolean tracked = mapActivity.getMapViewTrackingUtilities().isMapLinkedToLocation();
		if (!enabled) {
			backToLocationControl.setIconColorId(R.color.icon_color_light);
		} else if (tracked) {
			backToLocationControl.setIconColorId(R.color.color_distance);
		} else {
			backToLocationControl.setIconColorId(R.color.color_white);
		}
		menuControl.setIconResId(mapActivity.getMyApplication().getSettings().USE_DASHBOARD_INSTEAD_OF_DRAWER.get() ? R.drawable.ic_dashboard_dark
						: R.drawable.ic_navigation_drawer);
		optionsRouteControl.setIconResId(settings.getApplicationMode().getSmallIcon(false));
		int vis = showRouteCalculationControls? View.VISIBLE : View.GONE;
		if(routePreparationLayout.getVisibility() != vis) {
			routePreparationLayout.setVisibility(vis);
			mapRouteInfoControl.setVisible(showRouteCalculationControls);
		}

		for (MapHudButton mc : controls) {
			mc.update(mapActivity.getMyApplication(), nightMode == null ? false : nightMode.isNightMode());
		}

	}
	


	private void updatextColor(int textColor, int shadowColor, MapControls... mc) {
		for(MapControls m : mc) {
			m.updateTextColor(textColor, shadowColor);
		}
	}
	private FrameLayout getParent() {
		return (FrameLayout) mapActivity.findViewById(R.id.MapButtons);
	}


	@Override
	public boolean onSingleTap(PointF point, RotatedTileBox tileBox) {
		// TODO stop counter & show buttons
//		for(MapControls m : allControls) {
//			if(m.isVisible() && m.onSingleTap(point, tileBox)){
//				return true;
//			}
//		}
		return false;
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event, RotatedTileBox tileBox) {
		// TODO stop counter & show buttons
//		if(!mapActivity.getRoutingHelper().isRoutePlanningMode() && mapActivity.getRoutingHelper().isFollowingMode()) {
//			if(!settings.SHOW_ZOOM_BUTTONS_NAVIGATION.get()) {
//				zoomControls.showWithDelay(getParent(), TIMEOUT_TO_SHOW_BUTTONS);
//				mapMenuControls.showWithDelay(getParent(), TIMEOUT_TO_SHOW_BUTTONS);
//			}
//			mapRoutePlanControl.showWithDelay(getParent(), TIMEOUT_TO_SHOW_BUTTONS);
//		}
		return false;
	}


	/////////////////  Transparency bar /////////////////////////
	private void initTransparencyBar(final OsmandMapTileView view, FrameLayout parent) {
		int minimumHeight = view.getResources().getDrawable(R.drawable.map_zoom_in).getMinimumHeight();
		android.widget.FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
				Gravity.BOTTOM | Gravity.CENTER);
		params.setMargins(0, 0, 0, minimumHeight + 3);
		transparencyBarLayout = new LinearLayout(view.getContext());
		transparencyBarLayout.setVisibility(settingsToTransparency != null ? View.VISIBLE : View.GONE);
		parent.addView(transparencyBarLayout, params);

		transparencyBar = new SeekBar(view.getContext());
		transparencyBar.setMax(255);
		if(settingsToTransparency != null) {
			transparencyBar.setProgress(settingsToTransparency.get());
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
		android.widget.LinearLayout.LayoutParams prms = new LinearLayout.LayoutParams((int) (scaleCoefficient * 100),
				LayoutParams.WRAP_CONTENT);
		transparencyBarLayout.addView(transparencyBar, prms);
		ImageButton imageButton = new ImageButton(view.getContext());
		prms = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		prms.setMargins((int) (2 * scaleCoefficient), (int) (2 * scaleCoefficient), 0, 0);
		imageButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				transparencyBarLayout.setVisibility(View.GONE);
				hideTransparencyBar(settingsToTransparency);
			}
		});
		imageButton.setContentDescription(view.getContext().getString(R.string.shared_string_close));
		imageButton.setBackgroundResource(R.drawable.headliner_close);
		transparencyBarLayout.addView(imageButton, prms);
	}
	
	public void showTransparencyBar(CommonPreference<Integer> transparenPreference) {
		MapControlsLayer.settingsToTransparency = transparenPreference;
		transparencyBarLayout.setVisibility(View.VISIBLE);
		transparencyBar.setProgress(transparenPreference.get());
	}
	
	public void hideTransparencyBar(CommonPreference<Integer> transparentPreference) {
		if(settingsToTransparency == transparentPreference) {
			transparencyBarLayout.setVisibility(View.GONE);
			settingsToTransparency = null;
		}
	}

	public void shiftLayout(int height) {
		// TODO
	}

	


}
