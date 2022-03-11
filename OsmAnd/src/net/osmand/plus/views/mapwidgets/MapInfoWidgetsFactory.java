package net.osmand.plus.views.mapwidgets;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmAndLocationProvider.GPSInfo;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.actions.StartGPSStatus;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.MapRouteInfoMenu;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.layers.RadiusRulerControlLayer.RadiusRulerMode;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.widgets.TextInfoWidget;
import net.osmand.util.MapUtils;

import java.util.Iterator;
import java.util.LinkedList;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

public class MapInfoWidgetsFactory {

	private final OsmandApplication app;
	private final OsmAndLocationProvider locationProvider;

	public MapInfoWidgetsFactory(@NonNull OsmandApplication app) {
		this.app = app;
		this.locationProvider = app.getLocationProvider();
	}

	public enum TopToolbarControllerType {
		QUICK_SEARCH,
		CONTEXT_MENU,
		TRACK_DETAILS,
		DISCOUNT,
		MEASUREMENT_TOOL,
		POI_FILTER,
		DOWNLOAD_MAP
	}

	public TextInfoWidget createAltitudeControl(@NonNull MapActivity mapActivity) {
		TextInfoWidget altitudeControl = new TextInfoWidget(mapActivity) {
			private int cachedAlt = 0;

			@Override
			public void updateInfo(@Nullable DrawSettings drawSettings) {
				Location loc = locationProvider.getLastKnownLocation();
				if (loc != null && loc.hasAltitude()) {
					double compAlt = loc.getAltitude();
					if (isUpdateNeeded() || cachedAlt != (int) compAlt) {
						cachedAlt = (int) compAlt;
						String ds = OsmAndFormatter.getFormattedAlt(cachedAlt, app);
						int ls = ds.lastIndexOf(' ');
						if (ls == -1) {
							setText(ds, null);
						} else {
							setText(ds.substring(0, ls), ds.substring(ls + 1));
						}
					}
				} else if (cachedAlt != 0) {
					cachedAlt = 0;
					setText(null, null);
				}
			}

			@Override
			public boolean isMetricSystemDepended() {
				return true;
			}
		};
		altitudeControl.setText(null, null);
		altitudeControl.setIcons(R.drawable.widget_altitude_day, R.drawable.widget_altitude_night);
		return altitudeControl;
	}

	public TextInfoWidget createGPSInfoControl(@NonNull MapActivity mapActivity) {
		TextInfoWidget gpsInfoControl = new TextInfoWidget(mapActivity) {
			private int usedSatellites = -1;
			private int foundSatellites = -1;

			@Override
			public void updateInfo(@Nullable DrawSettings drawSettings) {
				GPSInfo gpsInfo = locationProvider.getGPSInfo();
				if (isUpdateNeeded()
						|| gpsInfo.usedSatellites != usedSatellites
						|| gpsInfo.foundSatellites != foundSatellites) {
					usedSatellites = gpsInfo.usedSatellites;
					foundSatellites = gpsInfo.foundSatellites;
					setText(gpsInfo.usedSatellites + "/" + gpsInfo.foundSatellites, "");
				}
			}
		};
		gpsInfoControl.setIcons(R.drawable.widget_gps_info_day, R.drawable.widget_gps_info_night);
		gpsInfoControl.setText(null, null);
		gpsInfoControl.setOnClickListener(view -> new StartGPSStatus(mapActivity).run());
		return gpsInfoControl;
	}

	public TextInfoWidget createRadiusRulerControl(@NonNull MapActivity mapActivity) {
		final String title = "—";
		final TextInfoWidget radiusRulerControl = new TextInfoWidget(mapActivity) {

			@Override
			public void updateInfo(@Nullable DrawSettings drawSettings) {
				Location currentLoc = locationProvider.getLastKnownLocation();
				LatLon centerLoc = mapActivity.getMapLocation();

				if (currentLoc != null && centerLoc != null) {
					if (mapActivity.getMapViewTrackingUtilities().isMapLinkedToLocation()) {
						setDistanceText(0);
					} else {
						setDistanceText(currentLoc.getLatitude(), currentLoc.getLongitude(),
								centerLoc.getLatitude(), centerLoc.getLongitude());
					}
				} else {
					setText(title, null);
				}
			}

			private void setDistanceText(float dist) {
				calculateAndSetText(dist);
			}

			private void setDistanceText(double firstLat, double firstLon, double secondLat, double secondLon) {
				float dist = (float) MapUtils.getDistance(firstLat, firstLon, secondLat, secondLon);
				calculateAndSetText(dist);
			}

			private void calculateAndSetText(float dist) {
				String distance = OsmAndFormatter.getFormattedDistance(dist, app);
				int ls = distance.lastIndexOf(' ');
				setText(distance.substring(0, ls), distance.substring(ls + 1));
			}
		};

		radiusRulerControl.setText(title, null);
		setRulerControlIcon(radiusRulerControl, app.getSettings().RADIUS_RULER_MODE.get());
		radiusRulerControl.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				final RadiusRulerMode mode = app.getSettings().RADIUS_RULER_MODE.get();
				RadiusRulerMode newMode = RadiusRulerMode.FIRST;
				if (mode == RadiusRulerMode.FIRST) {
					newMode = RadiusRulerMode.SECOND;
				} else if (mode == RadiusRulerMode.SECOND) {
					newMode = RadiusRulerMode.EMPTY;
				}
				setRulerControlIcon(radiusRulerControl, newMode);
				app.getSettings().RADIUS_RULER_MODE.set(newMode);
				mapActivity.refreshMap();
			}
		});

		return radiusRulerControl;
	}

	private void setRulerControlIcon(TextInfoWidget rulerControl, RadiusRulerMode mode) {
		if (mode == RadiusRulerMode.FIRST || mode == RadiusRulerMode.SECOND) {
			rulerControl.setIcons(R.drawable.widget_ruler_circle_day, R.drawable.widget_ruler_circle_night);
		} else {
			rulerControl.setIcons(R.drawable.widget_hidden_day, R.drawable.widget_hidden_night);
		}
	}

	public static class TopToolbarController {

		public static final int NO_COLOR = -1;

		private TopToolbarControllerType type;

		@ColorRes
		int bgLightId = R.color.list_background_color_light;
		@ColorRes
		int bgDarkId = R.color.list_background_color_dark;
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
		int backBtnIconClrLightId = R.color.icon_color_default_light;
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
		int closeBtnIconClrLightId = R.color.icon_color_default_light;
		@ColorRes
		int closeBtnIconClrDarkId = 0;
		boolean closeBtnVisible = true;

		@DrawableRes
		int refreshBtnIconLightId = R.drawable.ic_action_refresh_dark;
		@DrawableRes
		int refreshBtnIconDarkId = R.drawable.ic_action_refresh_dark;
		@ColorRes
		int refreshBtnIconClrLightId = R.color.icon_color_default_light;
		@ColorRes
		int refreshBtnIconClrDarkId = 0;

		boolean refreshBtnVisible = false;
		boolean saveViewVisible = false;
		boolean textBtnVisible = false;
		protected boolean topBarSwitchVisible = false;
		protected boolean topBarSwitchChecked = false;

		@ColorRes
		int titleTextClrLightId = R.color.text_color_primary_light;
		@ColorRes
		int titleTextClrDarkId = R.color.text_color_primary_dark;
		@ColorRes
		int descrTextClrLightId = R.color.text_color_primary_light;
		@ColorRes
		int descrTextClrDarkId = R.color.text_color_primary_dark;
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
		boolean topViewVisible = true;
		boolean shadowViewVisible = true;

		private boolean bottomViewAdded = false;

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

		public boolean isTopViewVisible() {
			return topViewVisible;
		}

		public void setTopViewVisible(boolean topViewVisible) {
			this.topViewVisible = topViewVisible;
		}

		public boolean isShadowViewVisible() {
			return shadowViewVisible;
		}

		public void setShadowViewVisible(boolean shadowViewVisible) {
			this.shadowViewVisible = shadowViewVisible;
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
				AndroidUiHelper.updateVisibility(titleView, true);
			} else {
				AndroidUiHelper.updateVisibility(titleView, false);
			}
			if (description != null) {
				descrView.setText(description);
				AndroidUiHelper.updateVisibility(descrView, true);
			} else {
				AndroidUiHelper.updateVisibility(descrView, false);
			}
			if (bottomView != null) {
				if (!bottomViewAdded) {
					bottomViewLayout.removeAllViews();
					bottomViewLayout.addView(bottomView);
					bottomViewLayout.setVisibility(View.VISIBLE);
					bottomViewAdded = true;
				}
			} else {
				bottomViewLayout.setVisibility(View.GONE);
			}
			AndroidUiHelper.updateVisibility(switchCompat, topBarSwitchVisible);
			if (topBarSwitchVisible) {
				switchCompat.setChecked(topBarSwitchChecked);
				if (topBarSwitchChecked) {
					DrawableCompat.setTint(switchCompat.getTrackDrawable(), ContextCompat.getColor(switchCompat.getContext(), R.color.map_toolbar_switch_track_color));
				}
			}
			View shadowView = view.getShadowView();
			if (shadowView != null) {
				AndroidUiHelper.updateVisibility(shadowView, isShadowViewVisible());
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
			AndroidUiHelper.updateVisibility(topbar, false);
		}

		public MapActivity getMap() {
			return map;
		}

		public View getTopbar() {
			return topbar;
		}

		public boolean isTopToolbarViewVisible() {
			return topbar.getVisibility() == View.VISIBLE;
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
			map.getMapLayers().getMapMarkersLayer().getMarkersWidgetsHelper().setCustomLatLon(null);
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
			boolean updated = AndroidUiHelper.updateVisibility(topbar, controller != null && !MapRouteInfoMenu.chooseRoutesVisible && !MapRouteInfoMenu.waypointsVisible &&
					(!map.getContextMenu().isVisible() || controller.getType() == TopToolbarControllerType.CONTEXT_MENU));
			if (updated) {
				map.updateStatusBarColor();
			}
		}

		public void updateColors(TopToolbarController controller) {
			UiUtilities uiUtils = map.getMyApplication().getUIUtilities();
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

			if (controller.isTopViewVisible()) {
				if (bg != null) {
					topBarLayout.setBackgroundDrawable(bg);
				} else {
					topBarLayout.setBackgroundResource(bgId);
				}
				topBarLayout.setVisibility(View.VISIBLE);
			} else {
				topBarLayout.setVisibility(View.GONE);
			}

			if (backBtnIconId == 0) {
				backButton.setImageDrawable(null);
			} else {
				if (backBtnIconClr != -1) {
					backButton.setImageDrawable(uiUtils.getPaintedIcon(backBtnIconId, backBtnIconClr));
				} else {
					backButton.setImageDrawable(uiUtils.getIcon(backBtnIconId, backBtnIconClrId));
				}
			}
			if (closeBtnIconId == 0) {
				closeButton.setImageDrawable(null);
			} else {
				closeButton.setImageDrawable(uiUtils.getIcon(closeBtnIconId, closeBtnIconClrId));
			}
			if (refreshBtnIconId == 0) {
				refreshButton.setImageDrawable(null);
			} else {
				refreshButton.setImageDrawable(uiUtils.getIcon(refreshBtnIconId, refreshBtnIconClrId));
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
}