package net.osmand.plus.views.mapwidgets;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.google.android.material.snackbar.Snackbar;
import com.jwetherell.openmap.common.LatLonPoint;
import com.jwetherell.openmap.common.MGRSPoint;
import com.jwetherell.openmap.common.UTMPoint;

import net.osmand.AndroidUtils;
import net.osmand.Location;
import net.osmand.LocationConvert;
import net.osmand.PlatformUtil;
import net.osmand.binary.BinaryMapRouteReaderAdapter;
import net.osmand.binary.RouteDataObject;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.CurrentPositionHelper;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmAndLocationProvider.GPSInfo;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.routing.CurrentStreetName;
import net.osmand.plus.routing.RoutingHelperUtils;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.actions.StartGPSStatus;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.WaypointDialogHelper;
import net.osmand.plus.helpers.WaypointHelper;
import net.osmand.plus.helpers.WaypointHelper.LocationPointWrapper;
import net.osmand.plus.render.OsmandRenderer;
import net.osmand.plus.render.TextRenderer;
import net.osmand.plus.routepreparationmenu.MapRouteInfoMenu;
import net.osmand.plus.routepreparationmenu.ShowAlongTheRouteBottomSheet;
import net.osmand.plus.routing.RouteCalculationResult;
import net.osmand.plus.routing.RouteDirectionInfo;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.views.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.layers.RadiusRulerControlLayer.RadiusRulerMode;
import net.osmand.plus.views.mapwidgets.widgets.TextInfoWidget;
import net.osmand.render.RenderingRuleSearchRequest;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.util.Iterator;
import java.util.LinkedList;

public class MapInfoWidgetsFactory {
	public enum TopToolbarControllerType {
		QUICK_SEARCH,
		CONTEXT_MENU,
		TRACK_DETAILS,
		DISCOUNT,
		MEASUREMENT_TOOL,
		POI_FILTER,
		DOWNLOAD_MAP
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
					if (isUpdateNeeded() || cachedAlt != (int) compAlt) {
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

			@Override
			public boolean isMetricSystemDepended() {
				return true;
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
				if (isUpdateNeeded() || gpsInfo.usedSatellites != u || gpsInfo.foundSatellites != f) {
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

	public TextInfoWidget createRadiusRulerControl(final MapActivity map) {
		final String title = "—";
		final TextInfoWidget radiusRulerControl = new TextInfoWidget(map) {

			@Override
			public boolean updateInfo(DrawSettings drawSettings) {
				Location currentLoc = map.getMyApplication().getLocationProvider().getLastKnownLocation();
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

		radiusRulerControl.setText(title, null);
		setRulerControlIcon(radiusRulerControl, map.getMyApplication().getSettings().RADIUS_RULER_MODE.get());
		radiusRulerControl.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				final RadiusRulerMode mode = map.getMyApplication().getSettings().RADIUS_RULER_MODE.get();
				RadiusRulerMode newMode = RadiusRulerMode.FIRST;
				if (mode == RadiusRulerMode.FIRST) {
					newMode = RadiusRulerMode.SECOND;
				} else if (mode == RadiusRulerMode.SECOND) {
					newMode = RadiusRulerMode.EMPTY;
				}
				setRulerControlIcon(radiusRulerControl, newMode);
				map.getMyApplication().getSettings().RADIUS_RULER_MODE.set(newMode);
				map.refreshMap();
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
			map.getMapLayers().getMapMarkersLayer().getWidgetsFactory().updateInfo(null, map.getMapView().getZoom());
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

	public static class TopTextView {
		private final RoutingHelper routingHelper;
		private final MapActivity map;
		private final View topBar;
		private final TextView addressText;
		private final TextView addressTextShadow;
		private final TextView exitRefText;
		private final ImageView shieldIcon;
		private final ImageView turnIcon;
		private final OsmAndLocationProvider locationProvider;
		private WaypointHelper waypointHelper;
		private OsmandSettings settings;
		private View waypointInfoBar;
		private LocationPointWrapper lastPoint;
		private TurnDrawable turnDrawable;
		private int shadowRad;
		RouteCalculationResult.NextDirectionInfo calc1;

		private static final Log LOG = PlatformUtil.getLog(TopTextView.class);
		private boolean showMarker;

		public TopTextView(OsmandApplication app, MapActivity map) {
			turnDrawable = new TurnDrawable(map, true);
			topBar = map.findViewById(R.id.map_top_bar);
			addressText = (TextView) map.findViewById(R.id.map_address_text);
			addressTextShadow = (TextView) map.findViewById(R.id.map_address_text_shadow);
			waypointInfoBar = map.findViewById(R.id.waypoint_info_bar);
			exitRefText = map.findViewById(R.id.map_exit_ref);
			shieldIcon = map.findViewById(R.id.map_shield_icon);
			turnIcon = map.findViewById(R.id.map_turn_icon);
			this.routingHelper = app.getRoutingHelper();
			locationProvider = app.getLocationProvider();
			this.map = map;
			settings = app.getSettings();
			waypointHelper = app.getWaypointHelper();
			updateVisibility(false);
			calc1 = new RouteCalculationResult.NextDirectionInfo();
		}

		public boolean updateVisibility(boolean visible) {
			boolean res = AndroidUiHelper.updateVisibility(topBar, visible);
			if (res) {
				map.updateStatusBarColor();
			}
			return res;
		}

		public void updateTextColor(boolean nightMode, int textColor, int textShadowColor, boolean bold, int rad) {
			this.shadowRad = rad;
			TextInfoWidget.updateTextColor(addressText, addressTextShadow, textColor, textShadowColor, bold, rad);
			TextInfoWidget.updateTextColor((TextView) waypointInfoBar.findViewById(R.id.waypoint_text),
					(TextView) waypointInfoBar.findViewById(R.id.waypoint_text_shadow),
					textColor, textShadowColor, bold, rad / 2);
			exitRefText.setTextColor(nightMode ? map.getResources().getColor(R.color.text_color_primary_dark) :
					map.getResources().getColor(R.color.color_white));

			ImageView all = (ImageView) waypointInfoBar.findViewById(R.id.waypoint_more);
			ImageView remove = (ImageView) waypointInfoBar.findViewById(R.id.waypoint_close);
			all.setImageDrawable(map.getMyApplication().getUIUtilities()
					.getIcon(R.drawable.ic_overflow_menu_white, !nightMode));
			remove.setImageDrawable(map.getMyApplication().getUIUtilities()
					.getIcon(R.drawable.ic_action_remove_dark, !nightMode));
		}


		public boolean updateInfo(DrawSettings d) {
			CurrentStreetName streetName = null;
			boolean showClosestWaypointFirstInAddress = true;
			if (routingHelper != null && routingHelper.isRouteCalculated() && !routingHelper.isDeviatedFromRoute()) {
				if (routingHelper.isFollowingMode()) {
					if (settings.SHOW_STREET_NAME.get()) {
						RouteCalculationResult.NextDirectionInfo nextDirInfo = routingHelper.getNextRouteDirectionInfo(calc1, true);
						streetName = routingHelper.getCurrentName(nextDirInfo);
						turnDrawable.setColor(R.color.nav_arrow);
					}
				} else {
					int di = MapRouteInfoMenu.getDirectionInfo();
					if (di >= 0 && map.getMapRouteInfoMenu().isVisible() && di < routingHelper.getRouteDirections().size()) {
						showClosestWaypointFirstInAddress = false;
						RouteDirectionInfo next = routingHelper.getRouteDirections().get(di);
						streetName = routingHelper.getCurrentName(routingHelper.getNextRouteDirectionInfo(calc1, true));
						turnDrawable.setColor(R.color.nav_arrow_distant);
					}
				}
			} else if (map.getMapViewTrackingUtilities().isMapLinkedToLocation() &&
					settings.SHOW_STREET_NAME.get()) {
				streetName = new CurrentStreetName();
				RouteDataObject rt = locationProvider.getLastKnownRouteSegment();
				if (rt != null) {
					Location lastKnownLocation = locationProvider.getLastKnownLocation();
					streetName.text = RoutingHelperUtils.formatStreetName(
							rt.getName(settings.MAP_PREFERRED_LOCALE.get(), settings.MAP_TRANSLITERATE_NAMES.get()),
							rt.getRef(settings.MAP_PREFERRED_LOCALE.get(), settings.MAP_TRANSLITERATE_NAMES.get(), rt.bearingVsRouteDirection(lastKnownLocation)),
							rt.getDestinationName(settings.MAP_PREFERRED_LOCALE.get(), settings.MAP_TRANSLITERATE_NAMES.get(), rt.bearingVsRouteDirection(lastKnownLocation)),
							"»");
					if (!Algorithms.isEmpty(streetName.text) && lastKnownLocation != null) {
						double dist = CurrentPositionHelper.getOrthogonalDistance(rt, lastKnownLocation);
						if (dist < 50) {
							streetName.showMarker = true;
						} else {
							streetName.text = map.getResources().getString(R.string.shared_string_near) + " " + streetName.text;
						}
					}
				}
			}
			if (map.isTopToolbarActive() || map.shouldHideTopControls() || MapRouteInfoMenu.chooseRoutesVisible || MapRouteInfoMenu.waypointsVisible) {
				updateVisibility(false);
			} else if (showClosestWaypointFirstInAddress && updateWaypoint()) {
				updateVisibility(true);
				AndroidUiHelper.updateVisibility(addressText, false);
				AndroidUiHelper.updateVisibility(addressTextShadow, false);
				AndroidUiHelper.updateVisibility(turnIcon, false);
				AndroidUiHelper.updateVisibility(shieldIcon, false);
				AndroidUiHelper.updateVisibility(exitRefText, false);
			} else if (streetName == null) {
				updateVisibility(false);
			} else {
				updateVisibility(true);
				AndroidUiHelper.updateVisibility(waypointInfoBar, false);
				AndroidUiHelper.updateVisibility(addressText, true);
				AndroidUiHelper.updateVisibility(addressTextShadow, shadowRad > 0);

				if (streetName.shieldObject != null && streetName.shieldObject.nameIds != null
						&& setRoadShield(shieldIcon, streetName.shieldObject)) {
					AndroidUiHelper.updateVisibility(shieldIcon, true);
				} else {
					AndroidUiHelper.updateVisibility(shieldIcon, false);
				}

				if (!Algorithms.isEmpty(streetName.exitRef)) {
					exitRefText.setText(streetName.exitRef);
					AndroidUiHelper.updateVisibility(exitRefText, true);
				} else {
					AndroidUiHelper.updateVisibility(exitRefText, false);
				}
				if (turnDrawable.setTurnType(streetName.turnType) || streetName.showMarker != this.showMarker) {
					this.showMarker = streetName.showMarker;
					if (streetName.turnType != null) {
						turnIcon.invalidateDrawable(turnDrawable);
						turnIcon.setImageDrawable(turnDrawable);
						AndroidUiHelper.updateVisibility(turnIcon, true);
					} else if (streetName.showMarker) {
						Drawable marker = map.getMyApplication().getUIUtilities().getIcon(R.drawable.ic_action_start_navigation, R.color.color_myloc_distance);
						turnIcon.setImageDrawable(marker);
						AndroidUiHelper.updateVisibility(turnIcon, true);
					} else {
						AndroidUiHelper.updateVisibility(turnIcon, false);
					}
				}
				if (streetName.text == null || streetName.text.isEmpty()) {
					addressTextShadow.setText("");
					addressText.setText("");
				} else if (!streetName.text.equals(addressText.getText().toString())) {
					addressTextShadow.setText(streetName.text);
					addressText.setText(streetName.text);
					return true;
				}
			}
			return false;
		}

		private boolean setRoadShield(ImageView view, RouteDataObject object) {
			StringBuilder additional = new StringBuilder();
			for (int i = 0; i < object.nameIds.length; i++) {
				String key = object.region.routeEncodingRules.get(object.nameIds[i]).getTag();
				String val = object.names.get(object.nameIds[i]);
				if (!key.startsWith("road_ref")) {
					additional.append(key).append("=").append(val).append(";");
				}
			}
			for (int i = 0; i < object.nameIds.length; i++) {
				String key = object.region.routeEncodingRules.get(object.nameIds[i]).getTag();
				String val = object.names.get(object.nameIds[i]);
				if (key.startsWith("road_ref")) {
					boolean visible = setRoadShield(view, object, key, val, additional);
					if (visible) {
						return true;
					}
				}
			}
			return false;
		}

		private boolean setRoadShield(ImageView view, RouteDataObject object, String nameTag, String name, StringBuilder additional) {

			Context context = topBar.getContext();
			int[] tps = object.getTypes();
			OsmandApplication app = ((OsmandApplication) context.getApplicationContext());
			RenderingRulesStorage storage = app.getRendererRegistry().getCurrentSelectedRenderer();
			boolean nightMode = app.getDaynightHelper().isNightMode();
			RenderingRuleSearchRequest rreq = map.getMyApplication().getResourceManager()
					.getRenderer().getSearchRequestWithAppliedCustomRules(storage, nightMode);

			for (int i : tps) {
				BinaryMapRouteReaderAdapter.RouteTypeRule tp = object.region.quickGetEncodingRule(i);
				if (tp.getTag().equals("highway") || tp.getTag().equals("route")) {
					rreq.setInitialTagValueZoom(tp.getTag(), tp.getValue(), 13, null);
				} else {
					additional.append(tp.getTag()).append("=").append(tp.getValue()).append(";");
				}
			}

			rreq.setIntFilter(rreq.ALL.R_TEXT_LENGTH, name.length());
			rreq.setStringFilter(rreq.ALL.R_NAME_TAG, nameTag);
			rreq.setStringFilter(rreq.ALL.R_ADDITIONAL, additional.toString());
			rreq.search(RenderingRulesStorage.TEXT_RULES);

			OsmandRenderer.RenderingContext rc = new OsmandRenderer.RenderingContext(context);

			TextRenderer textRenderer = new TextRenderer(context);
			TextRenderer.TextDrawInfo text = new TextRenderer.TextDrawInfo(name);


			Paint p = textRenderer.getPaintText();
			p.setTypeface(Typeface.create("Droid Serif", Typeface.BOLD));

			int shieldRes = -1;

			if (rreq.isSpecified(rreq.ALL.R_TEXT_SHIELD)) {
				text.setShieldResIcon(rreq.getStringPropertyValue(rreq.ALL.R_TEXT_SHIELD));
				shieldRes = app.getResources().getIdentifier("h_" + text.getShieldResIcon(),
						"drawable", app.getPackageName());
			}

			if (rreq.isSpecified(rreq.ALL.R_TEXT_COLOR)) {
				p.setColor(rreq.getIntPropertyValue(rreq.ALL.R_TEXT_COLOR));
			}

			if (rreq.isSpecified(rreq.ALL.R_TEXT_SIZE)) {
				float ts = rreq.getFloatPropertyValue(rreq.ALL.R_TEXT_SIZE);
				textRenderer.getPaintText().setTextSize(
						TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, ts,
								app.getResources().getDisplayMetrics()));
			}

			if (shieldRes != -1) {
				Drawable shield = AppCompatResources.getDrawable(view.getContext(), shieldRes);
				if (shield == null) {
					return false;
				}
				float xSize = shield.getIntrinsicWidth();
				float ySize = shield.getIntrinsicHeight();
				float xyRatio = xSize / ySize;
				//setting view propotions (height is fixed by toolbar size - 48dp);
				int viewHeightPx = AndroidUtils.dpToPx(context, 48);
				int viewWidthPx = (int) (viewHeightPx * xyRatio);

				ViewGroup.LayoutParams params = view.getLayoutParams();
				params.width = viewWidthPx;
				view.setLayoutParams(params);

				//creating bitmap according to size of resource
				Bitmap bitmap = Bitmap.createBitmap((int) xSize, (int) ySize, Bitmap.Config.ARGB_8888);
				Canvas canvas = new Canvas(bitmap);
				text.fillProperties(rc, rreq, xSize / 2f, ySize / 2f - p.getFontMetrics().ascent / 2f);
				textRenderer.drawShieldIcon(rc, canvas, text, text.getShieldResIcon());
				textRenderer.drawWrappedText(canvas, text, 20f);

				view.setImageBitmap(bitmap);
				return true;
			}
			return false;
		}

		public boolean updateWaypoint() {
			final LocationPointWrapper pnt = waypointHelper.getMostImportantLocationPoint(null);
			boolean changed = this.lastPoint != pnt;
			this.lastPoint = pnt;
			if (pnt == null) {
				topBar.setOnClickListener(null);
				AndroidUiHelper.updateVisibility(waypointInfoBar, false);
				return false;
			} else {
				AndroidUiHelper.updateVisibility(addressText, false);
				AndroidUiHelper.updateVisibility(addressTextShadow, false);
				boolean updated = AndroidUiHelper.updateVisibility(waypointInfoBar, true);
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
							fragment.setUsedOnMap(true);
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

	public static class TopCoordinatesView {
		private final MapActivity map;
		private OsmandSettings settings;
		private UiUtilities iconsCache;

		private OsmAndLocationProvider locationProvider;
		private View topBar;
		private View coordinatesRow;
		private View latCoordinatesContainer;
		private View lonCoordinatesContainer;
		private TextView latitudeText;
		private TextView longitudeText;
		private ImageView latitudeIcon;
		private ImageView longitudeIcon;
		private View coordinatesDivider;

		private Location lastKnownLocation;
		private boolean nightMode;

		public TopCoordinatesView(OsmandApplication app, MapActivity map) {
			topBar = map.findViewById(R.id.coordinates_top_bar);
			coordinatesRow = (LinearLayout) map.findViewById(R.id.coordinates_row);
			latCoordinatesContainer = (LinearLayout) map.findViewById(R.id.lat_coordinates_container);
			lonCoordinatesContainer = (LinearLayout) map.findViewById(R.id.lon_coordinates_container);
			latitudeText = (TextView) map.findViewById(R.id.lat_coordinates);
			longitudeText = (TextView) map.findViewById(R.id.lon_coordinates);
			latitudeIcon = (ImageView) map.findViewById(R.id.lat_icon);
			longitudeIcon = (ImageView) map.findViewById(R.id.lon_icon);
			coordinatesDivider = map.findViewById(R.id.coordinates_divider);
			this.map = map;
			settings = app.getSettings();
			iconsCache = app.getUIUtilities();
			locationProvider = app.getLocationProvider();
			updateVisibility(false);
			coordinatesRow.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (lastKnownLocation != null) {
						String coordinates = latitudeText.getText().toString();
						if (lonCoordinatesContainer.getVisibility() == View.VISIBLE) {
							coordinates += ", " + longitudeText.getText().toString();
						}
						copyToClipboard(coordinates);
					}
				}
			});
			AndroidUtils.setTextDirection(latitudeText, false);
			AndroidUtils.setTextDirection(longitudeText, false);
		}

		@SuppressLint("SetTextI18n")
		public boolean updateInfo() {
			boolean visible = map.getWidgetsVisibilityHelper().shouldShowTopCoordinatesWidget();
			updateVisibility(visible);
			if (visible) {
				lastKnownLocation = locationProvider.getLastKnownLocation();
				if (lastKnownLocation != null) {
					int f = settings.COORDINATES_FORMAT.get();
					double lat = lastKnownLocation.getLatitude();
					double lon = lastKnownLocation.getLongitude();

					if (f == PointDescription.UTM_FORMAT) {
						AndroidUiHelper.updateVisibility(lonCoordinatesContainer, false);
						AndroidUiHelper.updateVisibility(coordinatesDivider, false);
						AndroidUiHelper.updateVisibility(latitudeIcon, true);
						latitudeIcon.setImageDrawable(iconsCache.getIcon(nightMode ? R.drawable.widget_coordinates_utm_night : R.drawable.widget_coordinates_utm_day));
						UTMPoint pnt = new UTMPoint(new LatLonPoint(lat, lon));
						String utmLocation = pnt.zone_number + "" + pnt.zone_letter + " " + ((long) pnt.easting) + " " + ((long) pnt.northing);
						latitudeText.setText(utmLocation);
					} else if (f == PointDescription.MGRS_FORMAT) {
						AndroidUiHelper.updateVisibility(lonCoordinatesContainer, false);
						AndroidUiHelper.updateVisibility(coordinatesDivider, false);
						AndroidUiHelper.updateVisibility(latitudeIcon, true);
						latitudeIcon.setImageDrawable(iconsCache.getIcon(nightMode ? R.drawable.widget_coordinates_utm_night : R.drawable.widget_coordinates_utm_day));
						MGRSPoint pnt = new MGRSPoint(new LatLonPoint(lat, lon));
						latitudeText.setText(pnt.toFlavoredString(5));
					} else if (f == PointDescription.OLC_FORMAT) {
						AndroidUiHelper.updateVisibility(lonCoordinatesContainer, false);
						AndroidUiHelper.updateVisibility(coordinatesDivider, false);
						AndroidUiHelper.updateVisibility(latitudeIcon, true);
						latitudeIcon.setImageDrawable(iconsCache.getIcon(nightMode ? R.drawable.widget_coordinates_utm_night : R.drawable.widget_coordinates_utm_day));
						String olcLocation;
						try {
							olcLocation = PointDescription.getLocationOlcName(lat, lon);
						} catch (RuntimeException e) {
							e.printStackTrace();
							olcLocation = "0, 0";
						}
						latitudeText.setText(olcLocation);
					} else {
						AndroidUiHelper.updateVisibility(lonCoordinatesContainer, true);
						AndroidUiHelper.updateVisibility(coordinatesDivider, true);
						AndroidUiHelper.updateVisibility(latitudeIcon, true);
						String latitude = "";
						String longitude = "";
						try {
							latitude = LocationConvert.convertLatitude(lat, f, true);
							longitude = LocationConvert.convertLongitude(lon, f, true);
						} catch (RuntimeException e) {
							e.printStackTrace();
						}
						int latDayImgId = lat >= 0 ? R.drawable.widget_coordinates_latitude_north_day : R.drawable.widget_coordinates_latitude_south_day;
						int latNightImgId = lat >= 0 ? R.drawable.widget_coordinates_latitude_north_night : R.drawable.widget_coordinates_latitude_south_night;
						int lonDayImgId = lon >= 0 ? R.drawable.widget_coordinates_longitude_east_day : R.drawable.widget_coordinates_longitude_west_day;
						int lonNightImgId = lon >= 0 ? R.drawable.widget_coordinates_longitude_east_night : R.drawable.widget_coordinates_longitude_west_night;
						latitudeIcon.setImageDrawable(iconsCache.getIcon(nightMode ? latDayImgId : latNightImgId));
						longitudeIcon.setImageDrawable(iconsCache.getIcon(nightMode ? lonDayImgId : lonNightImgId));
						latitudeText.setText(latitude);
						longitudeText.setText(longitude);
					}
				} else {
					AndroidUiHelper.updateVisibility(lonCoordinatesContainer, false);
					AndroidUiHelper.updateVisibility(coordinatesDivider, false);
					AndroidUiHelper.updateVisibility(latitudeIcon, false);
					GPSInfo gpsInfo = locationProvider.getGPSInfo();
					latitudeText.setText(map.getString(R.string.searching_gps) + "…" + gpsInfo.usedSatellites + "/" + gpsInfo.foundSatellites);
				}
			}
			return false;
		}

		private void copyToClipboard(@NonNull String text) {
			Object systemService = map.getSystemService(Activity.CLIPBOARD_SERVICE);
			if (systemService instanceof ClipboardManager) {
				((ClipboardManager) systemService).setText(text);
				showShareSnackbar(text, map);
			}
		}

		private void showShareSnackbar(@NonNull final String text, @NonNull final Context ctx) {
			Snackbar snackbar = Snackbar.make(map.getLayout(), ctx.getResources().getString(R.string.copied_to_clipboard) + ":\n" + text, Snackbar.LENGTH_LONG)
					.setAction(R.string.shared_string_share, new View.OnClickListener() {
						@Override
						public void onClick(View view) {
							Intent intent = new Intent(Intent.ACTION_SEND);
							intent.setAction(Intent.ACTION_SEND);
							intent.putExtra(Intent.EXTRA_TEXT, text);
							intent.setType("text/plain");
							ctx.startActivity(Intent.createChooser(intent, ctx.getString(R.string.send_location)));
						}
					});
			UiUtilities.setupSnackbar(snackbar, nightMode, 5);
			snackbar.show();
		}

		public boolean updateVisibility(boolean visible) {
			boolean res = AndroidUiHelper.updateVisibility(topBar, visible);
			if (res) {
				map.updateStatusBarColor();
			}
			return res;
		}

		public void updateColors(boolean nightMode, boolean bold) {
			this.nightMode = nightMode;
			topBar.setBackgroundColor(ContextCompat.getColor(map, nightMode ? R.color.activity_background_dark : R.color.activity_background_dark));
			int textColor = ContextCompat.getColor(map, nightMode ? R.color.activity_background_light : R.color.activity_background_light);
			latitudeText.setTextColor(textColor);
			longitudeText.setTextColor(textColor);
			coordinatesDivider.setBackgroundColor(ContextCompat.getColor(map, nightMode ? R.color.divider_color_dark : R.color.divider_color_dark));
			latitudeText.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
			longitudeText.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
		}
	}
}
