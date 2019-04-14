package net.osmand.plus.views.mapwidgets;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.SwitchCompat;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.osmand.Location;
import net.osmand.binary.RouteDataObject;
import net.osmand.data.LatLon;
import net.osmand.plus.CurrentPositionHelper;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmAndLocationProvider.GPSInfo;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.RulerMode;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.actions.StartGPSStatus;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.WaypointDialogHelper;
import net.osmand.plus.helpers.WaypointHelper;
import net.osmand.plus.helpers.WaypointHelper.LocationPointWrapper;
import net.osmand.plus.routepreparationmenu.MapRouteInfoMenu;
import net.osmand.plus.routepreparationmenu.ShowAlongTheRouteBottomSheet;
import net.osmand.plus.routing.RouteDirectionInfo;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.views.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.RulerControlLayer;
import net.osmand.plus.views.mapwidgets.NextTurnInfoWidget.TurnDrawable;
import net.osmand.router.TurnType;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.Iterator;
import java.util.LinkedList;

public class MapInfoWidgetsFactory {
	public enum TopToolbarControllerType {
		QUICK_SEARCH,
		CONTEXT_MENU,
		TRACK_DETAILS,
		DISCOUNT,
		MEASUREMENT_TOOL,
		POI_FILTER
	}

	public TextInfoWidget createAltitudeControl(final MapActivity map) {
		final TextInfoWidget altitudeControl = new TextInfoWidget(map) {
			private int cachedAlt = 0;

			@Override
			public boolean updateInfo(DrawSettings d) {
				// draw speed
				Location loc = map.getMyApplication().getLocationProvider().getLastKnownLocation();
				if (loc != null && loc.hasAltitude()) {
					double compAlt = loc.getAltitude();
					if (cachedAlt != (int) compAlt) {
						cachedAlt = (int) compAlt;
						String ds = OsmAndFormatter.getFormattedAlt(cachedAlt, map.getMyApplication());
						int ls = ds.lastIndexOf(' ');
						if (ls == -1) {
							setText(ds, null);
						} else {
							setText(ds.substring(0, ls), ds.substring(ls + 1));
						}
						return true;
					}
				} else if (cachedAlt != 0) {
					cachedAlt = 0;
					setText(null, null);
					return true;
				}
				return false;
			}
		};
		altitudeControl.setText(null, null);
		altitudeControl.setIcons(R.drawable.widget_altitude_day, R.drawable.widget_altitude_night);
		return altitudeControl;
	}

	public TextInfoWidget createGPSInfoControl(final MapActivity map) {
		final OsmandApplication app = map.getMyApplication();
		final OsmAndLocationProvider loc = app.getLocationProvider();
		final TextInfoWidget gpsInfoControl = new TextInfoWidget(map) {
			private int u = -1;
			private int f = -1;

			@Override
			public boolean updateInfo(DrawSettings d) {
				GPSInfo gpsInfo = loc.getGPSInfo();
				if (gpsInfo.usedSatellites != u || gpsInfo.foundSatellites != f) {
					u = gpsInfo.usedSatellites;
					f = gpsInfo.foundSatellites;
					setText(gpsInfo.usedSatellites + "/" + gpsInfo.foundSatellites, "");
					return true;
				}
				return false;
			}
		};
		gpsInfoControl.setIcons(R.drawable.widget_gps_info_day, R.drawable.widget_gps_info_night);
		gpsInfoControl.setText(null, null);
		gpsInfoControl.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				new StartGPSStatus(map).run();
			}
		});
		return gpsInfoControl;
	}

	public TextInfoWidget createRulerControl(final MapActivity map) {
		final String title = "—";
		final TextInfoWidget rulerControl = new TextInfoWidget(map) {
			RulerControlLayer rulerLayer = map.getMapLayers().getRulerControlLayer();
			LatLon cacheFirstTouchPoint = new LatLon(0, 0);
			LatLon cacheSecondTouchPoint = new LatLon(0, 0);
			LatLon cacheSingleTouchPoint = new LatLon(0, 0);
			boolean fingerAndLocDistWasShown;

			@Override
			public boolean updateInfo(DrawSettings drawSettings) {
				OsmandMapTileView view = map.getMapView();
				Location currentLoc = map.getMyApplication().getLocationProvider().getLastKnownLocation();

				if (rulerLayer.isShowDistBetweenFingerAndLocation() && currentLoc != null) {
					if (!cacheSingleTouchPoint.equals(rulerLayer.getTouchPointLatLon())) {
						cacheSingleTouchPoint = rulerLayer.getTouchPointLatLon();
						setDistanceText(cacheSingleTouchPoint.getLatitude(), cacheSingleTouchPoint.getLongitude(),
								currentLoc.getLatitude(), currentLoc.getLongitude());
						fingerAndLocDistWasShown = true;
					}
				} else if (rulerLayer.isShowTwoFingersDistance()) {
					if (!cacheFirstTouchPoint.equals(view.getFirstTouchPointLatLon()) ||
							!cacheSecondTouchPoint.equals(view.getSecondTouchPointLatLon()) ||
							fingerAndLocDistWasShown) {
						cacheFirstTouchPoint = view.getFirstTouchPointLatLon();
						cacheSecondTouchPoint = view.getSecondTouchPointLatLon();
						setDistanceText(cacheFirstTouchPoint.getLatitude(), cacheFirstTouchPoint.getLongitude(),
								cacheSecondTouchPoint.getLatitude(), cacheSecondTouchPoint.getLongitude());
						fingerAndLocDistWasShown = false;
					}
				} else {
					LatLon centerLoc = map.getMapLocation();

					if (currentLoc != null && centerLoc != null) {
						if (map.getMapViewTrackingUtilities().isMapLinkedToLocation()) {
							setDistanceText(0);
						} else {
							setDistanceText(currentLoc.getLatitude(), currentLoc.getLongitude(),
									centerLoc.getLatitude(), centerLoc.getLongitude());
						}
					} else {
						setText(title, null);
					}
				}
				return true;
			}

			private void setDistanceText(float dist) {
				calculateAndSetText(dist);
			}

			private void setDistanceText(double firstLat, double firstLon, double secondLat, double secondLon) {
				float dist = (float) MapUtils.getDistance(firstLat, firstLon, secondLat, secondLon);
				calculateAndSetText(dist);
			}

			private void calculateAndSetText(float dist) {
				String distance = OsmAndFormatter.getFormattedDistance(dist, map.getMyApplication());
				int ls = distance.lastIndexOf(' ');
				setText(distance.substring(0, ls), distance.substring(ls + 1));
			}
		};

		rulerControl.setText(title, null);
		setRulerControlIcon(rulerControl, map.getMyApplication().getSettings().RULER_MODE.get());
		rulerControl.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				final RulerMode mode = map.getMyApplication().getSettings().RULER_MODE.get();
				RulerMode newMode = RulerMode.FIRST;
				if (mode == RulerMode.FIRST) {
					newMode = RulerMode.SECOND;
				} else if (mode == RulerMode.SECOND) {
					newMode = RulerMode.EMPTY;
				}
				setRulerControlIcon(rulerControl, newMode);
				map.getMyApplication().getSettings().RULER_MODE.set(newMode);
				map.refreshMap();
			}
		});

		return rulerControl;
	}

	private void setRulerControlIcon(TextInfoWidget rulerControl, RulerMode mode) {
		if (mode == RulerMode.FIRST || mode == RulerMode.SECOND) {
			rulerControl.setIcons(R.drawable.widget_ruler_circle_day, R.drawable.widget_ruler_circle_night);
		} else {
			rulerControl.setIcons(R.drawable.widget_hidden_day, R.drawable.widget_hidden_night);
		}
	}

	public static class TopToolbarController {

		public static final int NO_COLOR = -1;

		private TopToolbarControllerType type;

		@ColorRes
		int bgLightId = R.color.bg_color_light;
		@ColorRes
		int bgDarkId = R.color.bg_color_dark;
		@DrawableRes
		int bgLightLandId = R.drawable.btn_round;
		@DrawableRes
		int bgDarkLandId = R.drawable.btn_round_night;
		@Nullable
		Drawable bgLight = null;
		@Nullable
		Drawable bgDark = null;
		@Nullable
		Drawable bgLightLand = null;
		@Nullable
		Drawable bgDarkLand = null;

		@DrawableRes
		int backBtnIconLightId = R.drawable.ic_arrow_back;
		@DrawableRes
		int backBtnIconDarkId = R.drawable.ic_arrow_back;
		@ColorRes
		int backBtnIconClrLightId = R.color.icon_color;
		@ColorRes
		int backBtnIconClrDarkId = 0;
		@ColorInt
		int backBtnIconClrLight = -1;
		@ColorInt
		int backBtnIconClrDark = -1;

		@DrawableRes
		int closeBtnIconLightId = R.drawable.ic_action_remove_dark;
		@DrawableRes
		int closeBtnIconDarkId = R.drawable.ic_action_remove_dark;
		@ColorRes
		int closeBtnIconClrLightId = R.color.icon_color;
		@ColorRes
		int closeBtnIconClrDarkId = 0;
		boolean closeBtnVisible = true;

		@DrawableRes
		int refreshBtnIconLightId = R.drawable.ic_action_refresh_dark;
		@DrawableRes
		int refreshBtnIconDarkId = R.drawable.ic_action_refresh_dark;
		@ColorRes
		int refreshBtnIconClrLightId = R.color.icon_color;
		@ColorRes
		int refreshBtnIconClrDarkId = 0;

		boolean refreshBtnVisible = false;
		boolean saveViewVisible = false;
		boolean textBtnVisible = false;
		protected boolean topBarSwitchVisible = false;
		protected boolean topBarSwitchChecked = false;

		@ColorRes
		int titleTextClrLightId = R.color.primary_text_light;
		@ColorRes
		int titleTextClrDarkId = R.color.primary_text_dark;
		@ColorRes
		int descrTextClrLightId = R.color.primary_text_light;
		@ColorRes
		int descrTextClrDarkId = R.color.primary_text_dark;
		@ColorInt
		int titleTextClrLight = -1;
		@ColorInt
		int titleTextClrDark = -1;
		@ColorInt
		int descrTextClrLight = -1;
		@ColorInt
		int descrTextClrDark = -1;
		@ColorInt
		int textBtnTitleClrLight = -1;
		@ColorInt
		int textBtnTitleClrDark = -1;

		boolean singleLineTitle = true;

		boolean nightMode = false;

		String title = "";
		String description = null;
		String textBtnTitle = null;

		int saveViewTextId = -1;

		OnClickListener onBackButtonClickListener;
		OnClickListener onTitleClickListener;
		OnClickListener onCloseButtonClickListener;
		OnClickListener onRefreshButtonClickListener;
		OnClickListener onSaveViewClickListener;
		OnClickListener onTextBtnClickListener;
		OnCheckedChangeListener onSwitchCheckedChangeListener;

		Runnable onCloseToolbarListener;

		View bottomView = null;

		public TopToolbarController(TopToolbarControllerType type) {
			this.type = type;
		}

		public TopToolbarControllerType getType() {
			return type;
		}

		@ColorInt
		public int getStatusBarColor(Context context, boolean night) {
			return ContextCompat.getColor(context, night ? R.color.status_bar_route_dark : R.color.status_bar_route_light);
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public String getTitle() {
			return title;
		}

		public void setBottomView(View bottomView) {
			this.bottomView = bottomView;
		}

		public void setSingleLineTitle(boolean singleLineTitle) {
			this.singleLineTitle = singleLineTitle;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public void setBgIds(int bgLightId, int bgDarkId, int bgLightLandId, int bgDarkLandId) {
			this.bgLightId = bgLightId;
			this.bgDarkId = bgDarkId;
			this.bgLightLandId = bgLightLandId;
			this.bgDarkLandId = bgDarkLandId;
		}

		public void setBgs(Drawable bgLight, Drawable bgDark, Drawable bgLightLand, Drawable bgDarkLand) {
			this.bgLight = bgLight;
			this.bgDark = bgDark;
			this.bgLightLand = bgLightLand;
			this.bgDarkLand = bgDarkLand;
		}

		public void setBackBtnIconIds(int backBtnIconLightId, int backBtnIconDarkId) {
			this.backBtnIconLightId = backBtnIconLightId;
			this.backBtnIconDarkId = backBtnIconDarkId;
		}

		public void setBackBtnIconClrIds(int backBtnIconClrLightId, int backBtnIconClrDarkId) {
			this.backBtnIconClrLightId = backBtnIconClrLightId;
			this.backBtnIconClrDarkId = backBtnIconClrDarkId;
		}

		public void setBackBtnIconClrs(int backBtnIconClrLight, int backBtnIconClrDark) {
			this.backBtnIconClrLight = backBtnIconClrLight;
			this.backBtnIconClrDark = backBtnIconClrDark;
		}

		public void setCloseBtnIconIds(int closeBtnIconLightId, int closeBtnIconDarkId) {
			this.closeBtnIconLightId = closeBtnIconLightId;
			this.closeBtnIconDarkId = closeBtnIconDarkId;
		}

		public void setCloseBtnIconClrIds(int closeBtnIconClrLightId, int closeBtnIconClrDarkId) {
			this.closeBtnIconClrLightId = closeBtnIconClrLightId;
			this.closeBtnIconClrDarkId = closeBtnIconClrDarkId;
		}

		public void setRefreshBtnIconIds(int refreshBtnIconLightId, int refreshBtnIconDarkId) {
			this.refreshBtnIconLightId = refreshBtnIconLightId;
			this.refreshBtnIconDarkId = refreshBtnIconDarkId;
		}

		public void setRefreshBtnIconClrIds(int refreshBtnIconClrLightId, int refreshBtnIconClrDarkId) {
			this.refreshBtnIconClrLightId = refreshBtnIconClrLightId;
			this.refreshBtnIconClrDarkId = refreshBtnIconClrDarkId;
		}

		public void setCloseBtnVisible(boolean closeBtnVisible) {
			this.closeBtnVisible = closeBtnVisible;
		}

		public void setRefreshBtnVisible(boolean visible) {
			this.refreshBtnVisible = visible;
		}

		public void setSaveViewVisible(boolean visible) {
			this.saveViewVisible = visible;
		}

		public void setSaveViewTextId(int id) {
			this.saveViewTextId = id;
		}

		public void setTextBtnVisible(boolean visible) {
			this.textBtnVisible = visible;
		}

		public void setTextBtnTitle(String title) {
			this.textBtnTitle = title;
		}

		public void setTopBarSwitchVisible(boolean visible) {
			this.topBarSwitchVisible = visible;
		}

		public void setTopBarSwitchChecked(boolean checked) {
			this.topBarSwitchChecked = checked;
		}

		public void setTitleTextClrIds(int titleTextClrLightId, int titleTextClrDarkId) {
			this.titleTextClrLightId = titleTextClrLightId;
			this.titleTextClrDarkId = titleTextClrDarkId;
		}

		public void setTitleTextClrs(int titleTextClrLight, int titleTextClrDark) {
			this.titleTextClrLight = titleTextClrLight;
			this.titleTextClrDark = titleTextClrDark;
		}

		public void setDescrTextClrIds(int descrTextClrLightId, int descrTextClrDarkId) {
			this.descrTextClrLightId = descrTextClrLightId;
			this.descrTextClrDarkId = descrTextClrDarkId;
		}

		public void setDescrTextClrs(int descrTextClrLight, int descrTextClrDark) {
			this.descrTextClrLight = descrTextClrLight;
			this.descrTextClrDark = descrTextClrDark;
		}

		public void setTextBtnTitleClrs(int textBtnTitleClrLight, int textBtnTitleClrDark) {
			this.textBtnTitleClrLight = textBtnTitleClrLight;
			this.textBtnTitleClrDark = textBtnTitleClrDark;
		}

		public void setOnBackButtonClickListener(OnClickListener onBackButtonClickListener) {
			this.onBackButtonClickListener = onBackButtonClickListener;
		}

		public void setOnTitleClickListener(OnClickListener onTitleClickListener) {
			this.onTitleClickListener = onTitleClickListener;
		}

		public void setOnCloseButtonClickListener(OnClickListener onCloseButtonClickListener) {
			this.onCloseButtonClickListener = onCloseButtonClickListener;
		}

		public void setOnSaveViewClickListener(OnClickListener onSaveViewClickListener) {
			this.onSaveViewClickListener = onSaveViewClickListener;
		}

		public void setOnTextBtnClickListener(OnClickListener onTextBtnClickListener) {
			this.onTextBtnClickListener = onTextBtnClickListener;
		}

		public void setOnSwitchCheckedChangeListener(OnCheckedChangeListener onSwitchCheckedChangeListener) {
			this.onSwitchCheckedChangeListener = onSwitchCheckedChangeListener;
		}

		public void setOnRefreshButtonClickListener(OnClickListener onRefreshButtonClickListener) {
			this.onRefreshButtonClickListener = onRefreshButtonClickListener;
		}

		public void setOnCloseToolbarListener(Runnable onCloseToolbarListener) {
			this.onCloseToolbarListener = onCloseToolbarListener;
		}

		public void updateToolbar(TopToolbarView view) {
			TextView titleView = view.getTitleView();
			TextView descrView = view.getDescrView();
			LinearLayout bottomViewLayout = view.getBottomViewLayout();
			SwitchCompat switchCompat = view.getTopBarSwitch();
			if (title != null) {
				titleView.setText(title);
				view.updateVisibility(titleView, true);
			} else {
				view.updateVisibility(titleView, false);
			}
			if (description != null) {
				descrView.setText(description);
				view.updateVisibility(descrView, true);
			} else {
				view.updateVisibility(descrView, false);
			}
			if (bottomView != null) {
				bottomViewLayout.removeAllViews();
				bottomViewLayout.addView(bottomView);
				view.updateVisibility(bottomViewLayout, true);
			} else {
				view.updateVisibility(bottomViewLayout, false);
			}
			view.updateVisibility(switchCompat, topBarSwitchVisible);
			if (topBarSwitchVisible) {
				switchCompat.setChecked(topBarSwitchChecked);
				if (topBarSwitchChecked) {
					DrawableCompat.setTint(switchCompat.getTrackDrawable(), ContextCompat.getColor(switchCompat.getContext(),R.color.map_toolbar_switch_track_color));
				}
			}
			if (view.getShadowView() != null) {
				view.getShadowView().setVisibility(View.VISIBLE);
			}
		}
	}

	public static class TopToolbarView {
		private final MapActivity map;
		private LinkedList<TopToolbarController> controllers = new LinkedList<>();
		private TopToolbarController defaultController = new TopToolbarController(TopToolbarControllerType.CONTEXT_MENU);
		private View topbar;
		private View topBarLayout;
		private View topBarBottomView;
		private View topBarTitleLayout;
		private ImageButton backButton;
		private TextView titleView;
		private TextView descrView;
		private ImageButton refreshButton;
		private ImageButton closeButton;
		private TextView saveView;
		private TextView textBtn;
		private SwitchCompat topBarSwitch;
		private View shadowView;
		private boolean nightMode;

		public TopToolbarView(final MapActivity map) {
			this.map = map;

			topbar = map.findViewById(R.id.widget_top_bar);
			topBarLayout = map.findViewById(R.id.widget_top_bar_layout);
			topBarBottomView = map.findViewById(R.id.widget_top_bar_bottom_view);
			topBarTitleLayout = map.findViewById(R.id.widget_top_bar_title_layout);
			backButton = (ImageButton) map.findViewById(R.id.widget_top_bar_back_button);
			refreshButton = (ImageButton) map.findViewById(R.id.widget_top_bar_refresh_button);
			closeButton = (ImageButton) map.findViewById(R.id.widget_top_bar_close_button);
			titleView = (TextView) map.findViewById(R.id.widget_top_bar_title);
			saveView = (TextView) map.findViewById(R.id.widget_top_bar_save);
			textBtn = (TextView) map.findViewById(R.id.widget_top_bar_text_btn);
			descrView = (TextView) map.findViewById(R.id.widget_top_bar_description);
			topBarSwitch = (SwitchCompat) map.findViewById(R.id.widget_top_bar_switch);
			shadowView = map.findViewById(R.id.widget_top_bar_shadow);
			updateVisibility(false);
		}

		public MapActivity getMap() {
			return map;
		}

		public View getTopbar() {
			return topbar;
		}

		public View getTopBarLayout() {
			return topBarLayout;
		}

		public ImageButton getBackButton() {
			return backButton;
		}

		public TextView getTitleView() {
			return titleView;
		}

		public LinearLayout getBottomViewLayout() {
			return (LinearLayout) topBarBottomView;
		}

		public TextView getDescrView() {
			return descrView;
		}

		public ImageButton getCloseButton() {
			return closeButton;
		}

		public TextView getSaveView() {
			return saveView;
		}

		public SwitchCompat getTopBarSwitch() {
			return topBarSwitch;
		}

		public ImageButton getRefreshButton() {
			return refreshButton;
		}

		public View getShadowView() {
			return shadowView;
		}

		public TopToolbarController getTopController() {
			if (controllers.size() > 0) {
				return controllers.get(controllers.size() - 1);
			} else {
				return null;
			}
		}

		public TopToolbarController getController(TopToolbarControllerType type) {
			for (TopToolbarController controller : controllers) {
				if (controller.getType() == type) {
					return controller;
				}
			}
			return null;
		}

		public void addController(TopToolbarController controller) {
			for (Iterator ctrlIter = controllers.iterator(); ctrlIter.hasNext(); ) {
				TopToolbarController ctrl = (TopToolbarController) ctrlIter.next();
				if (ctrl.getType() == controller.getType()) {
					if (controller.onCloseToolbarListener != null) {
						controller.onCloseToolbarListener.run();
					}
					ctrlIter.remove();
				}
			}
			controllers.add(controller);
			updateColors();
			updateInfo();
		}

		public void removeController(TopToolbarController controller) {
			if (controller.onCloseToolbarListener != null) {
				controller.onCloseToolbarListener.run();
			}
			controllers.remove(controller);
			updateColors();
			updateInfo();
		}

		public boolean updateVisibility(boolean visible) {
			return updateVisibility(topbar, visible);
		}

		public boolean updateVisibility(View v, boolean visible) {
			if (visible != (v.getVisibility() == View.VISIBLE)) {
				if (visible) {
					v.setVisibility(View.VISIBLE);
				} else {
					v.setVisibility(View.GONE);
				}
				v.invalidate();
				return true;
			}
			return false;
		}

		private void initToolbar(TopToolbarController controller) {
			backButton.setOnClickListener(controller.onBackButtonClickListener);
			topBarTitleLayout.setOnClickListener(controller.onTitleClickListener);
			closeButton.setOnClickListener(controller.onCloseButtonClickListener);
			refreshButton.setOnClickListener(controller.onRefreshButtonClickListener);
			saveView.setOnClickListener(controller.onSaveViewClickListener);
			textBtn.setOnClickListener(controller.onTextBtnClickListener);
			topBarSwitch.setOnCheckedChangeListener(controller.onSwitchCheckedChangeListener);
		}

		public void updateInfo() {
			TopToolbarController controller = getTopController();
			if (controller != null) {
				initToolbar(controller);
				controller.updateToolbar(this);
			} else {
				initToolbar(defaultController);
				defaultController.updateToolbar(this);
			}
			updateVisibility(controller != null && !MapRouteInfoMenu.chooseRoutesVisible && !MapRouteInfoMenu.waypointsVisible &&
					(!map.getContextMenu().isVisible() || controller.getType() == TopToolbarControllerType.CONTEXT_MENU));
		}

		public void updateColors(TopToolbarController controller) {
			OsmandApplication app = map.getMyApplication();
			controller.nightMode = nightMode;

			boolean portrait = AndroidUiHelper.isOrientationPortrait(map);
			int bgId = portrait
					? nightMode ? controller.bgDarkId : controller.bgLightId
					: nightMode ? controller.bgDarkLandId : controller.bgLightLandId;
			Drawable bg = portrait
					? nightMode ? controller.bgDark : controller.bgLight
					: nightMode ? controller.bgDarkLand : controller.bgLightLand;
			int backBtnIconId = nightMode ? controller.backBtnIconDarkId : controller.backBtnIconLightId;
			int backBtnIconClr = nightMode ? controller.backBtnIconClrDark : controller.backBtnIconClrLight;
			int backBtnIconClrId = nightMode ? controller.backBtnIconClrDarkId : controller.backBtnIconClrLightId;
			int closeBtnIconId = nightMode ? controller.closeBtnIconDarkId : controller.closeBtnIconLightId;
			int closeBtnIconClrId = nightMode ? controller.closeBtnIconClrDarkId : controller.closeBtnIconClrLightId;
			int refreshBtnIconId = nightMode ? controller.refreshBtnIconDarkId : controller.refreshBtnIconLightId;
			int refreshBtnIconClrId = nightMode ? controller.refreshBtnIconClrDarkId : controller.refreshBtnIconClrLightId;
			int titleTextClr = nightMode ? controller.titleTextClrDark : controller.titleTextClrLight;
			int titleTextClrId = nightMode ? controller.titleTextClrDarkId : controller.titleTextClrLightId;
			int descrTextClr = nightMode ? controller.descrTextClrDark : controller.descrTextClrLight;
			int descrTextClrId = nightMode ? controller.descrTextClrDarkId : controller.descrTextClrLightId;
			int textBtnTitleClr = nightMode ? controller.textBtnTitleClrDark : controller.textBtnTitleClrLight;

			if (bg != null) {
				topBarLayout.setBackgroundDrawable(bg);
			} else {
				topBarLayout.setBackgroundResource(bgId);
			}
			if (backBtnIconId == 0) {
				backButton.setImageDrawable(null);
			} else {
				if (backBtnIconClr != -1) {
					backButton.setImageDrawable(app.getUIUtilities().getPaintedIcon(backBtnIconId, backBtnIconClr));
				} else {
					backButton.setImageDrawable(app.getUIUtilities().getIcon(backBtnIconId, backBtnIconClrId));
				}
			}
			if (closeBtnIconId == 0) {
				closeButton.setImageDrawable(null);
			} else {
				closeButton.setImageDrawable(app.getUIUtilities().getIcon(closeBtnIconId, closeBtnIconClrId));
			}
			if (refreshBtnIconId == 0) {
				refreshButton.setImageDrawable(null);
			} else {
				refreshButton.setImageDrawable(app.getUIUtilities().getIcon(refreshBtnIconId, refreshBtnIconClrId));
			}
			int titleColor = titleTextClr != -1 ? titleTextClr : map.getResources().getColor(titleTextClrId);
			int descrColor = descrTextClr != -1 ? descrTextClr : map.getResources().getColor(descrTextClrId);
			titleView.setTextColor(titleColor);
			descrView.setTextColor(descrColor);
			saveView.setTextColor(titleColor);
			if (textBtnTitleClr != -1) {
				textBtn.setTextColor(textBtnTitleClr);
			}

			titleView.setSingleLine(controller.singleLineTitle);

			if (controller.closeBtnVisible) {
				if (closeButton.getVisibility() == View.GONE) {
					closeButton.setVisibility(View.VISIBLE);
				}
			} else if (closeButton.getVisibility() == View.VISIBLE) {
				closeButton.setVisibility(View.GONE);
			}
			if (controller.refreshBtnVisible) {
				if (refreshButton.getVisibility() == View.GONE) {
					refreshButton.setVisibility(View.VISIBLE);
				}
			} else if (refreshButton.getVisibility() == View.VISIBLE) {
				refreshButton.setVisibility(View.GONE);
			}
			if (controller.saveViewVisible) {
				if (controller.saveViewTextId != -1) {
					saveView.setText(map.getString(controller.saveViewTextId));
					saveView.setContentDescription(map.getString(controller.saveViewTextId));
				}
				if (saveView.getVisibility() == View.GONE) {
					saveView.setVisibility(View.VISIBLE);
				}
			} else if (saveView.getVisibility() == View.VISIBLE) {
				saveView.setVisibility(View.GONE);
			}
			if (controller.textBtnVisible) {
				textBtn.setText(controller.textBtnTitle);
				textBtn.setContentDescription(controller.textBtnTitle);
				if (textBtn.getVisibility() == View.GONE) {
					textBtn.setVisibility(View.VISIBLE);
				}
			} else if (textBtn.getVisibility() == View.VISIBLE) {
				textBtn.setVisibility(View.GONE);
			}
		}

		public void updateColors() {
			TopToolbarController controller = getTopController();
			if (controller != null) {
				updateColors(controller);
			} else {
				updateColors(defaultController);
			}
		}

		public void updateColors(boolean nightMode) {
			this.nightMode = nightMode;
			for (TopToolbarController controller : controllers) {
				controller.nightMode = nightMode;
			}
			updateColors();
		}
	}

	public static class TopTextView {
		private final RoutingHelper routingHelper;
		private final MapActivity map;
		private View topBar;
		private TextView addressText;
		private TextView addressTextShadow;
		private OsmAndLocationProvider locationProvider;
		private WaypointHelper waypointHelper;
		private OsmandSettings settings;
		private View waypointInfoBar;
		private LocationPointWrapper lastPoint;
		private TurnDrawable turnDrawable;
		private boolean showMarker;
		private int shadowRad;

		public TopTextView(OsmandApplication app, MapActivity map) {
			topBar = map.findViewById(R.id.map_top_bar);
			addressText = (TextView) map.findViewById(R.id.map_address_text);
			addressTextShadow = (TextView) map.findViewById(R.id.map_address_text_shadow);
			waypointInfoBar = map.findViewById(R.id.waypoint_info_bar);
			this.routingHelper = app.getRoutingHelper();
			locationProvider = app.getLocationProvider();
			this.map = map;
			settings = app.getSettings();
			waypointHelper = app.getWaypointHelper();
			updateVisibility(false);
			turnDrawable = new NextTurnInfoWidget.TurnDrawable(map, true);
		}

		public boolean updateVisibility(boolean visible) {
			boolean res = updateVisibility(topBar, visible);
			if (res) {
				map.updateStatusBarColor();
			}
			return res;
		}

		public boolean updateVisibility(View v, boolean visible) {
			if (visible != (v.getVisibility() == View.VISIBLE)) {
				if (visible) {
					v.setVisibility(View.VISIBLE);
				} else {
					v.setVisibility(View.GONE);
				}
				v.invalidate();
				return true;
			}
			return false;
		}

		public void updateTextColor(boolean nightMode, int textColor, int textShadowColor, boolean bold, int rad) {
			this.shadowRad = rad;
			TextInfoWidget.updateTextColor(addressText, addressTextShadow, textColor, textShadowColor, bold, rad);
			TextInfoWidget.updateTextColor((TextView) waypointInfoBar.findViewById(R.id.waypoint_text),
					(TextView) waypointInfoBar.findViewById(R.id.waypoint_text_shadow),
					textColor, textShadowColor, bold, rad / 2);

			ImageView all = (ImageView) waypointInfoBar.findViewById(R.id.waypoint_more);
			ImageView remove = (ImageView) waypointInfoBar.findViewById(R.id.waypoint_close);
			all.setImageDrawable(map.getMyApplication().getUIUtilities()
					.getIcon(R.drawable.ic_overflow_menu_white, !nightMode));
			remove.setImageDrawable(map.getMyApplication().getUIUtilities()
					.getIcon(R.drawable.ic_action_remove_dark, !nightMode));
		}


		public boolean updateInfo(DrawSettings d) {
			String text = null;
			TurnType[] type = new TurnType[1];
			boolean showNextTurn = false;
			boolean showMarker = this.showMarker;
			if (routingHelper != null && routingHelper.isRouteCalculated() && !routingHelper.isDeviatedFromRoute()) {
				if (routingHelper.isFollowingMode()) {
					if (settings.SHOW_STREET_NAME.get()) {
						text = routingHelper.getCurrentName(type);
						if (text == null) {
							text = "";
						} else {
							if (type[0] == null) {
								showMarker = true;
							} else {
								turnDrawable.setColor(R.color.nav_arrow);
							}
						}
					}
				} else {
					int di = MapRouteInfoMenu.getDirectionInfo();
					if (di >= 0 && map.getMapRouteInfoMenu().isVisible() &&
							di < routingHelper.getRouteDirections().size()) {
						showNextTurn = true;
						RouteDirectionInfo next = routingHelper.getRouteDirections().get(di);
						type[0] = next.getTurnType();
						turnDrawable.setColor(R.color.nav_arrow_distant);
						text = RoutingHelper.formatStreetName(next.getStreetName(), next.getRef(), next.getDestinationName(), "»");
//						if (next.distance > 0) {
//							text += " " + OsmAndFormatter.getFormattedDistance(next.distance, map.getMyApplication());
//						}
						if (text == null) {
							text = "";
						}
					} else {
						text = null;
					}
				}
			} else if (map.getMapViewTrackingUtilities().isMapLinkedToLocation() &&
					settings.SHOW_STREET_NAME.get()) {
				RouteDataObject rt = locationProvider.getLastKnownRouteSegment();
				if (rt != null) {
					Location lastKnownLocation = locationProvider.getLastKnownLocation();
					text = RoutingHelper.formatStreetName(
							rt.getName(settings.MAP_PREFERRED_LOCALE.get(), settings.MAP_TRANSLITERATE_NAMES.get()),
							rt.getRef(settings.MAP_PREFERRED_LOCALE.get(), settings.MAP_TRANSLITERATE_NAMES.get(), rt.bearingVsRouteDirection(lastKnownLocation)),
							rt.getDestinationName(settings.MAP_PREFERRED_LOCALE.get(), settings.MAP_TRANSLITERATE_NAMES.get(), rt.bearingVsRouteDirection(lastKnownLocation)),
							"»");
				}
				if (text == null) {
					text = "";
				} else {
					Location lastKnownLocation = locationProvider.getLastKnownLocation();
					if (!Algorithms.isEmpty(text) && lastKnownLocation != null) {
						double dist =
								CurrentPositionHelper.getOrthogonalDistance(rt, lastKnownLocation);
						if (dist < 50) {
							showMarker = true;
						} else {
							text = map.getResources().getString(R.string.shared_string_near) + " " + text;
						}
					}
				}
			}
			if (map.isTopToolbarActive() || !map.getContextMenu().shouldShowTopControls() || MapRouteInfoMenu.chooseRoutesVisible || MapRouteInfoMenu.waypointsVisible) {
				updateVisibility(false);
			} else if (!showNextTurn && updateWaypoint()) {
				updateVisibility(true);
				updateVisibility(addressText, false);
				updateVisibility(addressTextShadow, false);
			} else if (text == null) {
				updateVisibility(false);
			} else {
				updateVisibility(true);
				updateVisibility(waypointInfoBar, false);
				updateVisibility(addressText, true);
				updateVisibility(addressTextShadow, shadowRad > 0);
				boolean update = turnDrawable.setTurnType(type[0]) || showMarker != this.showMarker;
				this.showMarker = showMarker;
				int h = addressText.getHeight() / 4 * 3;
				if (h != turnDrawable.getBounds().bottom) {
					turnDrawable.setBounds(0, 0, h, h);
				}
				if (update) {
					if (type[0] != null) {
						addressTextShadow.setCompoundDrawables(turnDrawable, null, null, null);
						addressTextShadow.setCompoundDrawablePadding(4);
						addressText.setCompoundDrawables(turnDrawable, null, null, null);
						addressText.setCompoundDrawablePadding(4);
					} else if (showMarker) {
						Drawable marker = map.getMyApplication().getUIUtilities().getIcon(R.drawable.ic_action_start_navigation, R.color.color_myloc_distance);
						addressTextShadow.setCompoundDrawablesWithIntrinsicBounds(marker, null, null, null);
						addressTextShadow.setCompoundDrawablePadding(4);
						addressText.setCompoundDrawablesWithIntrinsicBounds(marker, null, null, null);
						addressText.setCompoundDrawablePadding(4);
					} else {
						addressTextShadow.setCompoundDrawables(null, null, null, null);
						addressText.setCompoundDrawables(null, null, null, null);
					}
				}
				if (!text.equals(addressText.getText().toString())) {
					addressTextShadow.setText(text);
					addressText.setText(text);
					return true;
				}
			}
			return false;
		}

		public boolean updateWaypoint() {
			final LocationPointWrapper pnt = waypointHelper.getMostImportantLocationPoint(null);
			boolean changed = this.lastPoint != pnt;
			this.lastPoint = pnt;
			if (pnt == null) {
				topBar.setOnClickListener(null);
				updateVisibility(waypointInfoBar, false);
				return false;
			} else {
				updateVisibility(addressText, false);
				updateVisibility(addressTextShadow, false);
				boolean updated = updateVisibility(waypointInfoBar, true);
				// pass top bar to make it clickable
				WaypointDialogHelper.updatePointInfoView(map.getMyApplication(), map, topBar, pnt, true,
						map.getMyApplication().getDaynightHelper().isNightModeForMapControls(), false, true);
				if (updated || changed) {
					ImageView all = (ImageView) waypointInfoBar.findViewById(R.id.waypoint_more);
					ImageView remove = (ImageView) waypointInfoBar.findViewById(R.id.waypoint_close);
					all.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View view) {
							map.hideContextAndRouteInfoMenues();
							ShowAlongTheRouteBottomSheet fragment = new ShowAlongTheRouteBottomSheet();
							Bundle args = new Bundle();
							args.putInt(ShowAlongTheRouteBottomSheet.EXPAND_TYPE_KEY, pnt.type);
							fragment.setArguments(args);
							fragment.setUsedOnMap(false);
							fragment.show(map.getSupportFragmentManager(), ShowAlongTheRouteBottomSheet.TAG);
						}
					});
					remove.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View view) {
							waypointHelper.removeVisibleLocationPoint(pnt);
							map.refreshMap();
						}
					});
				}
				return true;
			}
		}

		public void setBackgroundResource(int boxTop) {
			topBar.setBackgroundResource(boxTop);
		}

	}
}
