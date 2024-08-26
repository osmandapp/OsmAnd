package net.osmand.plus.dashboard;

import static net.osmand.plus.dashboard.DashboardOnMap.DashboardType.CONFIGURE_MAP;
import static net.osmand.plus.dashboard.DashboardOnMap.DashboardType.CONTOUR_LINES;
import static net.osmand.plus.dashboard.DashboardOnMap.DashboardType.CYCLE_ROUTES;
import static net.osmand.plus.dashboard.DashboardOnMap.DashboardType.DASHBOARD;
import static net.osmand.plus.dashboard.DashboardOnMap.DashboardType.ALPINE_HIKING;
import static net.osmand.plus.dashboard.DashboardOnMap.DashboardType.HIKING_ROUTES;
import static net.osmand.plus.dashboard.DashboardOnMap.DashboardType.MAPILLARY;
import static net.osmand.plus.dashboard.DashboardOnMap.DashboardType.MTB_ROUTES;
import static net.osmand.plus.dashboard.DashboardOnMap.DashboardType.NAUTICAL_DEPTH;
import static net.osmand.plus.dashboard.DashboardOnMap.DashboardType.OSM_NOTES;
import static net.osmand.plus.dashboard.DashboardOnMap.DashboardType.OVERLAY_MAP;
import static net.osmand.plus.dashboard.DashboardOnMap.DashboardType.RELIEF_3D;
import static net.osmand.plus.dashboard.DashboardOnMap.DashboardType.TERRAIN;
import static net.osmand.plus.dashboard.DashboardOnMap.DashboardType.TRANSPORT_LINES;
import static net.osmand.plus.dashboard.DashboardOnMap.DashboardType.TRAVEL_ROUTES;
import static net.osmand.plus.dashboard.DashboardOnMap.DashboardType.UNDERLAY_MAP;
import static net.osmand.plus.dashboard.DashboardOnMap.DashboardType.WEATHER;
import static net.osmand.plus.dashboard.DashboardOnMap.DashboardType.WEATHER_CONTOURS;
import static net.osmand.plus.dashboard.DashboardOnMap.DashboardType.WEATHER_LAYER;
import static net.osmand.plus.dashboard.DashboardOnMap.DashboardType.WIKIPEDIA;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.github.ksoichiro.android.observablescrollview.ObservableListView;
import com.github.ksoichiro.android.observablescrollview.ObservableScrollView;
import com.github.ksoichiro.android.observablescrollview.ObservableScrollViewCallbacks;

import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.data.ValueHolder;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.configmap.ConfigureMapFragment;
import net.osmand.plus.configmap.CycleRoutesFragment;
import net.osmand.plus.configmap.AlpineHikingScaleFragment;
import net.osmand.plus.configmap.HikingRoutesFragment;
import net.osmand.plus.configmap.MtbRoutesFragment;
import net.osmand.plus.configmap.TravelRoutesFragment;
import net.osmand.plus.dashboard.tools.DashFragmentData;
import net.osmand.plus.dashboard.tools.DashboardSettingsDialogFragment;
import net.osmand.plus.dashboard.tools.TransactionBuilder;
import net.osmand.plus.dialogs.RasterMapMenu;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadIndexesThread;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.WaypointDialogHelper;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.mapillary.MapillaryFiltersFragment;
import net.osmand.plus.plugins.mapillary.MapillaryFirstDialogFragment;
import net.osmand.plus.plugins.mapillary.MapillaryPlugin;
import net.osmand.plus.plugins.openseamaps.NauticalDepthContourFragment;
import net.osmand.plus.plugins.osmedit.menu.OsmNotesMenu;
import net.osmand.plus.plugins.rastermaps.OsmandRasterMapsPlugin;
import net.osmand.plus.plugins.srtm.ContourLinesMenu;
import net.osmand.plus.plugins.srtm.Relief3DFragment;
import net.osmand.plus.plugins.srtm.TerrainFragment;
import net.osmand.plus.plugins.weather.WeatherBand;
import net.osmand.plus.plugins.weather.WeatherPlugin;
import net.osmand.plus.plugins.weather.dialogs.WeatherContoursFragment;
import net.osmand.plus.plugins.weather.dialogs.WeatherLayerFragment;
import net.osmand.plus.plugins.weather.dialogs.WeatherMainFragment;
import net.osmand.plus.routing.IRouteInformationListener;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.track.fragments.TrackMenuFragment;
import net.osmand.plus.transport.TransportLinesFragment;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.DownloadedRegionsLayer;
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter;
import net.osmand.plus.widgets.ctxmenu.ContextMenuListAdapter;
import net.osmand.plus.widgets.ctxmenu.ViewCreator;
import net.osmand.plus.widgets.ctxmenu.callback.ItemClickListener;
import net.osmand.plus.widgets.ctxmenu.callback.OnRowItemClick;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;
import net.osmand.plus.wikipedia.WikipediaPoiMenu;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public class DashboardOnMap implements ObservableScrollViewCallbacks, IRouteInformationListener {

	private static final org.apache.commons.logging.Log LOG = PlatformUtil.getLog(DashboardOnMap.class);
	private static final String TAG = "DashboardOnMap";

	public static boolean staticVisible;
	public static DashboardType staticVisibleType = DASHBOARD;
	public static final String SHOULD_SHOW = "should_show";


	private final DashFragmentData[] fragmentsData = {
			new DashFragmentData(DashDashboardOrDrawerFragment.TAG, DashDashboardOrDrawerFragment.class,
					DashDashboardOrDrawerFragment.SHOULD_SHOW_FUNCTION, 5, null),
			new DashFragmentData(DashErrorFragment.TAG, DashErrorFragment.class,
					DashErrorFragment.SHOULD_SHOW_FUNCTION, 30, null),
			new DashFragmentData(DashSearchFragment.TAG, DashSearchFragment.class,
					DashSearchFragment.SHOULD_SHOW_FUNCTION, 35, null),
			new DashFragmentData(DashNavigationFragment.TAG, DashNavigationFragment.class,
					DashNavigationFragment.SHOULD_SHOW_FUNCTION, 40, null),
			new DashFragmentData(DashWaypointsFragment.TAG, DashWaypointsFragment.class,
					DashWaypointsFragment.SHOULD_SHOW_FUNCTION, 60, null),
			DashRecentsFragment.FRAGMENT_DATA,
			DashFavoritesFragment.FRAGMENT_DATA,
			new DashFragmentData(DashPluginsFragment.TAG, DashPluginsFragment.class,
					DashPluginsFragment.SHOULD_SHOW_FUNCTION, 140, null)
	};

	private final MapActivity mapActivity;
	private ImageView actionButton;
	private View compassButton;
	private FrameLayout dashboardView;

	private ArrayAdapter<?> listAdapter;
	private OnItemClickListener adapterClickListener;

	private boolean visible;
	private final DashboardVisibilityStack visibleTypes = new DashboardVisibilityStack();
	private final Map<DashboardType, Integer> lastKnownScrolls = new HashMap<>();
	private ApplicationMode previousAppMode;
	private boolean landscape;
	private final List<WeakReference<DashBaseFragment>> fragList = new LinkedList<>();
	private LatLon mapViewLocation;
	private float heading;
	private boolean mapLinkedToLocation;
	private float mapRotation;
	private boolean inLocationUpdate;
	private ObservableListView listView;
	private View listBackgroundView;
	private Toolbar toolbar;
	private View paddingView;
	private int mFlexibleSpaceImageHeight;
	private int mFlexibleBlurSpaceHeight;
	private boolean portrait;
	private int[] animationCoordinates;
	private ProgressBar planRouteProgressBar;

	int baseColor;

	private WaypointDialogHelper waypointDialogHelper;
	private Drawable gradientToolbar;
	boolean nightMode;

	public DashFragmentData[] getFragmentsData() {
		return fragmentsData;
	}

	public enum DashboardType {
		CONFIGURE_MAP,
		DASHBOARD,
		OVERLAY_MAP,
		UNDERLAY_MAP,
		MAPILLARY,
		CONTOUR_LINES,
		OSM_NOTES,
		WIKIPEDIA,
		TERRAIN,
		RELIEF_3D,
		CYCLE_ROUTES,
		HIKING_ROUTES,
		TRAVEL_ROUTES,
		TRANSPORT_LINES,
		WEATHER,
		WEATHER_LAYER,
		WEATHER_CONTOURS,
		NAUTICAL_DEPTH,
		MTB_ROUTES,
		ALPINE_HIKING
	}

	private final Map<DashboardActionButtonType, DashboardActionButton> actionButtons = new HashMap<>();

	public enum DashboardActionButtonType {
		MY_LOCATION,
		NAVIGATE,
		ROUTE
	}

	private static class DashboardActionButton {
		private Drawable icon;
		private String text;
		private OnClickListener onClickListener;
	}

	public DashboardOnMap(@NonNull MapActivity activity) {
		this.mapActivity = activity;
	}

	public WaypointDialogHelper getWaypointDialogHelper() {
		return waypointDialogHelper;
	}

	public void updateRouteCalculationProgress(int progress) {
		if (planRouteProgressBar != null) {
			if (planRouteProgressBar.getVisibility() != View.VISIBLE) {
				planRouteProgressBar.setVisibility(View.VISIBLE);
			}
			planRouteProgressBar.setProgress(progress);
		}
	}

	public void routeCalculationFinished() {
		if (planRouteProgressBar != null) {
			planRouteProgressBar.setVisibility(View.GONE);
		}
	}

	public void createDashboardView() {
		baseColor = ContextCompat.getColor(mapActivity, R.color.osmand_orange) & 0x00ffffff;
		waypointDialogHelper = new WaypointDialogHelper(mapActivity);
		landscape = !AndroidUiHelper.isOrientationPortrait(mapActivity);
		dashboardView = mapActivity.findViewById(R.id.dashboard);
		AndroidUtils.addStatusBarPadding21v(mapActivity, dashboardView);
		OnClickListener listener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				hideDashboard();
				mapActivity.getFragmentsHelper().dismissSettingsScreens();
			}
		};
		toolbar = dashboardView.findViewById(R.id.toolbar);
		ObservableScrollView scrollView = dashboardView.findViewById(R.id.main_scroll);
		listView = dashboardView.findViewById(R.id.dash_list_view);
		//listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		listView.setDrawSelectorOnTop(true);
		listView.setScrollViewCallbacks(this);
		gradientToolbar = AppCompatResources.getDrawable(mapActivity, R.drawable.gradient_toolbar).mutate();
		if (AndroidUiHelper.isOrientationPortrait(mapActivity)) {
			this.portrait = true;
			scrollView.setScrollViewCallbacks(this);
			listView.setScrollViewCallbacks(this);
			mFlexibleSpaceImageHeight = mapActivity.getResources().getDimensionPixelSize(
					R.dimen.dashboard_map_top_padding);
			mFlexibleBlurSpaceHeight = mapActivity.getResources().getDimensionPixelSize(
					R.dimen.dashboard_map_toolbar);
			// Set padding view for ListView. This is the flexible space.
			paddingView = new FrameLayout(mapActivity);
			AbsListView.LayoutParams lp = new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT,
					mFlexibleSpaceImageHeight);
			paddingView.setLayoutParams(lp);
			// This is required to disable header's list selector effect
			paddingView.setClickable(true);
			paddingView.setOnClickListener(listener);

			FrameLayout shadowContainer = new FrameLayout(mapActivity);
			FrameLayout.LayoutParams fl = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
					FrameLayout.LayoutParams.MATCH_PARENT);
			fl.gravity = Gravity.BOTTOM;
			shadowContainer.setLayoutParams(fl);
			ImageView shadow = new ImageView(mapActivity);
			shadow.setImageDrawable(AppCompatResources.getDrawable(mapActivity, R.drawable.bg_shadow_onmap));
			shadow.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
					FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));
			shadow.setScaleType(ScaleType.FIT_XY);
			shadowContainer.addView(shadow);
			((FrameLayout) paddingView).addView(shadowContainer);
			listView.addHeaderView(paddingView);
			listBackgroundView = mapActivity.findViewById(R.id.dash_list_background);
		}
		dashboardView.findViewById(R.id.animateContent).setOnClickListener(listener);
		dashboardView.findViewById(R.id.map_part_dashboard).setOnClickListener(listener);

		View pbContainer = LayoutInflater.from(mapActivity).inflate(R.layout.plan_route_progress_bar, null);
		planRouteProgressBar = pbContainer.findViewById(R.id.progress_bar);
		listView.addHeaderView(pbContainer);

		initActionButtons();
		dashboardView.addView(actionButton);
	}

	private void updateListBackgroundHeight() {
		if (listBackgroundView != null) {
			View contentView = mapActivity.getWindow().getDecorView().findViewById(android.R.id.content);
			ViewTreeObserver vto = contentView.getViewTreeObserver();
			vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

				@Override
				public void onGlobalLayout() {
					ViewTreeObserver obs = contentView.getViewTreeObserver();
					obs.removeOnGlobalLayoutListener(this);
					listBackgroundView.getLayoutParams().height = contentView.getHeight();
				}
			});
		}
	}

	private void updateToolbarActions() {
		TextView tv = dashboardView.findViewById(R.id.toolbar_text);
		tv.setText("");
		if (isCurrentType(CONFIGURE_MAP)) {
			tv.setText(R.string.configure_map);
		} else if (isCurrentType(UNDERLAY_MAP)) {
			tv.setText(R.string.map_underlay);
		} else if (isCurrentType(OVERLAY_MAP)) {
			tv.setText(R.string.map_overlay);
		} else if (isCurrentType(MAPILLARY)) {
			tv.setText(R.string.street_level_imagery);
		} else if (isCurrentType(CONTOUR_LINES)) {
			tv.setText(R.string.download_srtm_maps);
		} else if (isCurrentType(OSM_NOTES)) {
			tv.setText(R.string.osm_notes);
		} else if (isCurrentType(TERRAIN)) {
			tv.setText(R.string.shared_string_terrain);
		}else if (isCurrentType(RELIEF_3D)) {
			tv.setText(R.string.relief_3d);
		} else if (isCurrentType(WIKIPEDIA)) {
			tv.setText(R.string.shared_string_wikipedia);
		} else if (isCurrentType(CYCLE_ROUTES)) {
			tv.setText(R.string.rendering_attr_showCycleRoutes_name);
		} else if (isCurrentType(HIKING_ROUTES)) {
			tv.setText(R.string.rendering_attr_hikingRoutesOSMC_name);
		} else if (isCurrentType(TRAVEL_ROUTES)) {
			tv.setText(R.string.travel_routes);
		} else if (isCurrentType(TRANSPORT_LINES)) {
			tv.setText(R.string.rendering_category_transport);
		} else if (isCurrentType(WEATHER)) {
			tv.setText(R.string.shared_string_weather);
		} else if (isCurrentType(WEATHER_LAYER)) {
			WeatherPlugin plugin = PluginsHelper.getPlugin(WeatherPlugin.class);
			if (plugin != null) {
				WeatherBand weatherBand = getMyApplication().getWeatherHelper().getWeatherBand(plugin.getCurrentConfigureBand());
				if (weatherBand != null) {
					tv.setText(weatherBand.getMeasurementName());
				}
			}
		} else if (isCurrentType(WEATHER_CONTOURS)) {
			tv.setText(R.string.shared_string_contours);
		} else if (isCurrentType(NAUTICAL_DEPTH)) {
			tv.setText(R.string.nautical_depth);
		} else if (isCurrentType(MTB_ROUTES)) {
			tv.setText(R.string.app_mode_mountain_bicycle);
		} else if (isCurrentType(ALPINE_HIKING)) {
			tv.setText(R.string.rendering_attr_alpineHiking_name);
		}
		ImageView edit = dashboardView.findViewById(R.id.toolbar_edit);
		edit.setVisibility(View.GONE);
		ImageView sort = dashboardView.findViewById(R.id.toolbar_sort);
		sort.setVisibility(View.GONE);
		ImageView ok = dashboardView.findViewById(R.id.toolbar_ok);
		ok.setVisibility(View.GONE);
		ImageView flat = dashboardView.findViewById(R.id.toolbar_flat);
		flat.setVisibility(View.GONE);
		ImageView settingsButton = dashboardView.findViewById(R.id.toolbar_settings);
		settingsButton.setVisibility(View.GONE);
		ImageView lst = dashboardView.findViewById(R.id.toolbar_list);
		lst.setVisibility(View.GONE);
		ImageButton back = dashboardView.findViewById(R.id.toolbar_back);
		Drawable icBack = getMyApplication().getUIUtilities().getIcon(AndroidUtils.getNavigationIconResId(mapActivity));
		back.setImageDrawable(icBack);
		back.setOnClickListener(v -> backPressed());

		if (isCurrentType(DASHBOARD)) {
			settingsButton.setVisibility(View.VISIBLE);
			settingsButton.setOnClickListener(v -> {
				DashboardSettingsDialogFragment fragment = new DashboardSettingsDialogFragment();
				fragment.show(mapActivity.getSupportFragmentManager(), "dashboard_settings");
			});
			lst.setVisibility(View.VISIBLE);
			lst.setOnClickListener(v -> {
				hideDashboard(false);
				mapActivity.openDrawer();
			});
		}

		toolbar.getMenu().clear();
	}

	private FrameLayout.LayoutParams getActionButtonLayoutParams(int btnSizePx) {
		int topPadPx = mapActivity.getResources().getDimensionPixelSize(R.dimen.dashboard_map_top_padding);
		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(btnSizePx, btnSizePx);
		int marginEnd = btnSizePx / 4;
		AndroidUtils.setMargins(params, 0, landscape ? 0 : topPadPx - 2 * btnSizePx, marginEnd, landscape ? marginEnd : 0);
		params.gravity = landscape ? Gravity.BOTTOM | Gravity.END : Gravity.TOP | Gravity.END;
		return params;
	}

	private void initActionButtons() {
		actionButton = new ImageView(mapActivity);
		int btnSizePx = mapActivity.getResources().getDimensionPixelSize(R.dimen.map_button_size);
		actionButton.setLayoutParams(getActionButtonLayoutParams(btnSizePx));
		actionButton.setBackgroundResource(R.drawable.btn_circle_blue);
		int iconSizePx = mapActivity.getResources().getDimensionPixelSize(R.dimen.map_widget_icon);
		int iconPadding = (btnSizePx - iconSizePx) / 2;
		actionButton.setPadding(iconPadding, iconPadding, iconPadding, iconPadding);
		hideActionButton();


		DashboardActionButton myLocationButton = new DashboardActionButton();
		myLocationButton.icon = AppCompatResources.getDrawable(mapActivity, R.drawable.ic_my_location);
		myLocationButton.text = mapActivity.getString(R.string.map_widget_back_to_loc);
		myLocationButton.onClickListener = v -> {
			if (getMyApplication().accessibilityEnabled()) {
				mapActivity.getMapActions().whereAmIDialog();
			} else {
				mapActivity.getMapViewTrackingUtilities().backToLocationImpl();
			}
			hideDashboard();
		};

		DashboardActionButton navigateButton = new DashboardActionButton();
		navigateButton.icon = AppCompatResources.getDrawable(mapActivity, R.drawable.ic_action_start_navigation);
		navigateButton.text = mapActivity.getString(R.string.follow);
		navigateButton.onClickListener = v -> {
			mapActivity.getMapLayers().getMapActionsHelper().doNavigate();
			hideDashboard();
		};

		DashboardActionButton routeButton = new DashboardActionButton();
		routeButton.icon = AppCompatResources.getDrawable(mapActivity, R.drawable.ic_action_gdirections_dark);
		routeButton.text = mapActivity.getString(R.string.layer_route);
		routeButton.onClickListener = v -> {
			hideDashboard();
			mapActivity.getMapLayers().getMapActionsHelper().doRoute();
		};

		actionButtons.put(DashboardActionButtonType.MY_LOCATION, myLocationButton);
		actionButtons.put(DashboardActionButtonType.NAVIGATE, navigateButton);
		actionButtons.put(DashboardActionButtonType.ROUTE, routeButton);
	}

	private void setActionButton(DashboardType type) {
		DashboardActionButton button = null;

		if (type == DASHBOARD) {
			button = actionButtons.get(DashboardActionButtonType.MY_LOCATION);
		}

		if (button != null) {
			actionButton.setImageDrawable(button.icon);
			actionButton.setContentDescription(button.text);
			actionButton.setOnClickListener(button.onClickListener);
		}
	}

	private void hideActionButton() {
		actionButton.setVisibility(View.GONE);
		if (compassButton != null) {
			compassButton.setVisibility(View.GONE);
		}
	}

	public LatLon getMapViewLocation() {
		return mapViewLocation;
	}

	public float getHeading() {
		return heading;
	}

	public float getMapRotation() {
		return mapRotation;
	}

	public boolean isMapLinkedToLocation() {
		return mapLinkedToLocation;
	}

	protected OsmandApplication getMyApplication() {
		return mapActivity.getMyApplication();
	}

	public ArrayAdapter<?> getListAdapter() {
		return listAdapter;
	}

	public void hideDashboard() {
		setDashboardVisibility(false, visibleTypes.getCurrent());
	}

	public void hideDashboard(boolean animation) {
		setDashboardVisibility(false, visibleTypes.getCurrent(), animation);
	}

	public void setDashboardVisibility(boolean visible, DashboardType type) {
		setDashboardVisibility(visible, type, null);
	}

	public void setDashboardVisibility(boolean visible, DashboardType type, int[] animationCoordinates) {
		boolean animate = !getMyApplication().getSettings().DO_NOT_USE_ANIMATIONS.get();
		setDashboardVisibility(visible, type, animate, animationCoordinates);
	}

	public void setDashboardVisibility(boolean visible, DashboardType type, boolean animation) {
		setDashboardVisibility(visible, type, animation, null);
	}

	public void refreshDashboardFragments() {
		addOrUpdateDashboardFragments();
	}

	@ColorRes
	public int getStatusBarColor() {
		return R.color.status_bar_transparent_gradient;
	}

	public void setDashboardVisibility(boolean visible, DashboardType type, boolean animation, int[] animationCoordinates) {
		boolean currentType = isCurrentType(type);
		if (visible == this.visible && currentType || !AndroidUtils.isActivityNotDestroyed(mapActivity)) {
			return;
		}
		mapActivity.getRoutingHelper().removeListener(this);
		nightMode = getMyApplication().getDaynightHelper().isNightModeForMapControls();
		this.visible = visible;
		updateVisibilityStack(type, visible);

		ApplicationMode currentAppMode = getMyApplication().getSettings().APPLICATION_MODE.get();
		boolean appModeChanged = currentAppMode != previousAppMode;
		boolean refresh = currentType && !appModeChanged;
		previousAppMode = currentAppMode;
		staticVisible = visible;
		staticVisibleType = type;
		mapActivity.enableDrawer();
		removeFragment(ConfigureMapFragment.TAG);
		removeFragment(MapillaryFiltersFragment.TAG);
		removeFragment(TerrainFragment.TAG);
		removeFragment(TransportLinesFragment.TAG);
		removeFragment(WeatherMainFragment.TAG);

		if (visible) {
			mapActivity.getFragmentsHelper().dismissCardDialog();
			mapActivity.getFragmentsHelper().dismissFragment(TrackMenuFragment.TAG);
			mapActivity.getContextMenu().hideMenus();
			mapViewLocation = mapActivity.getMapLocation();
			mapRotation = mapActivity.getMapRotate();
			mapLinkedToLocation = mapActivity.getMapViewTrackingUtilities().isMapLinkedToLocation();
			mapActivity.getMapViewTrackingUtilities().setDashboard(this);
			mapActivity.disableDrawer();
			dashboardView.setVisibility(View.VISIBLE);
			if (isActionButtonVisible()) {
				setActionButton(visibleTypes.getCurrent());
				actionButton.setVisibility(View.VISIBLE);
			} else {
				hideActionButton();
				if (isCurrentType(CONFIGURE_MAP)) {
					int btnSizePx = mapActivity.getResources().getDimensionPixelSize(R.dimen.map_small_button_size);
					compassButton = mapActivity.getMapLayers().getMapControlsLayer()
							.moveCompassButton(dashboardView, getActionButtonLayoutParams(btnSizePx));
				}
			}
			updateDownloadBtn();
			View listViewLayout = dashboardView.findViewById(R.id.dash_list_view_layout);
			ScrollView scrollView = dashboardView.findViewById(R.id.main_scroll);
			if (isCurrentType(DASHBOARD) || isCurrentTypeHasIndividualFragment()) {
				FragmentManager fragmentManager = mapActivity.getSupportFragmentManager();
				if (isCurrentType(DASHBOARD)) {
					addOrUpdateDashboardFragments();
				} else if (isCurrentType(CONFIGURE_MAP)) {
					ConfigureMapFragment.showInstance(fragmentManager);
				} else if (isCurrentType(MAPILLARY)) {
					MapillaryFiltersFragment.showInstance(fragmentManager);
				} else if (isCurrentType(CYCLE_ROUTES)) {
					CycleRoutesFragment.showInstance(fragmentManager);
				} else if (isCurrentType(HIKING_ROUTES)) {
					HikingRoutesFragment.showInstance(fragmentManager);
				} else if (isCurrentType(TRAVEL_ROUTES)) {
					TravelRoutesFragment.showInstance(fragmentManager);
				} else if (isCurrentType(TRANSPORT_LINES)) {
					TransportLinesFragment.showInstance(fragmentManager);
				} else if (isCurrentType(NAUTICAL_DEPTH)) {
					NauticalDepthContourFragment.showInstance(fragmentManager);
				} else if (isCurrentType(TERRAIN)) {
					TerrainFragment.showInstance(fragmentManager);
				}else if (isCurrentType(RELIEF_3D)) {
					Relief3DFragment.showInstance(fragmentManager);
				} else if (isCurrentType(WEATHER)) {
					WeatherMainFragment.showInstance(fragmentManager);
				} else if (isCurrentType(WEATHER_LAYER)) {
					WeatherLayerFragment.showInstance(fragmentManager);
				} else if (isCurrentType(WEATHER_CONTOURS)) {
					WeatherContoursFragment.showInstance(fragmentManager);
				} else if (isCurrentType(MTB_ROUTES)) {
					MtbRoutesFragment.showInstance(fragmentManager);
				} else if (isCurrentType(ALPINE_HIKING)) {
					AlpineHikingScaleFragment.showInstance(fragmentManager);
				}
				scrollView.setVisibility(View.VISIBLE);
				listViewLayout.setVisibility(View.GONE);
				applyScrollPosition(scrollView);
			} else {
				scrollView.setVisibility(View.GONE);
				listViewLayout.setVisibility(View.VISIBLE);
				if (refresh) {
					refreshContent(false);
				} else {
					listView.scrollTo(0, 0);
					listView.clearParams();
					onScrollChangedImpl(listView.getScrollY());
					updateListAdapter();
				}
				updateListBackgroundHeight();
				applyDayNightMode();
			}
			mapActivity.findViewById(R.id.toolbar_back).setVisibility(isBackButtonVisible() ? View.VISIBLE : View.GONE);
			mapActivity.getMapLayers().getMapControlsLayer().hideMapControls();

			updateToolbarActions();
			open(animation, animationCoordinates);
			updateLocation(true, true, false);
			mapActivity.getRoutingHelper().addListener(this);
		} else {
			mapActivity.getMapViewTrackingUtilities().setDashboard(null);
			hide(animation);
			mapActivity.getMapLayers().getMapControlsLayer().showMapControlsIfHidden();
			hideActionButton();
			for (WeakReference<DashBaseFragment> df : fragList) {
				if (df.get() != null) {
					df.get().onCloseDash();
				}
			}

			MapillaryPlugin plugin = PluginsHelper.getPlugin(MapillaryPlugin.class);
			if (plugin != null && plugin.SHOW_MAPILLARY.get() && !plugin.MAPILLARY_FIRST_DIALOG_SHOWN.get()) {
				MapillaryFirstDialogFragment fragment = new MapillaryFirstDialogFragment();
				fragment.show(mapActivity.getSupportFragmentManager(), MapillaryFirstDialogFragment.TAG);
				plugin.MAPILLARY_FIRST_DIALOG_SHOWN.set(true);
			}
		}
		mapActivity.updateStatusBarColor();
	}

	private void updateVisibilityStack(@NonNull DashboardType type, boolean visible) {
		if (visible) {
			visibleTypes.add(type);
		} else {
			visibleTypes.clear();
		}
	}

	private void applyDayNightMode() {
		int backgroundColor;
		backgroundColor = ColorUtilities.getActivityBgColor(mapActivity, nightMode);
		Drawable dividerDrawable = new ColorDrawable(ColorUtilities.getDividerColor(mapActivity, nightMode));

		if (listBackgroundView != null) {
			listBackgroundView.setBackgroundColor(backgroundColor);
		} else {
			listView.setBackgroundColor(backgroundColor);
		}
		if (isNoCurrentType(CONFIGURE_MAP, CONTOUR_LINES, TERRAIN, CYCLE_ROUTES, HIKING_ROUTES,
				TRAVEL_ROUTES, OSM_NOTES, WIKIPEDIA, TRANSPORT_LINES, WEATHER, WEATHER_LAYER,
				WEATHER_CONTOURS, NAUTICAL_DEPTH, MTB_ROUTES, ALPINE_HIKING)) {
			listView.setDivider(dividerDrawable);
			listView.setDividerHeight(AndroidUtils.dpToPx(mapActivity, 1f));
		} else {
			listView.setDivider(null);
		}

		if (planRouteProgressBar != null) {
			mapActivity.setupRouteCalculationProgressBar(planRouteProgressBar);
		}
	}

	private void updateListAdapter() {
		listView.setEmptyView(null);
		ContextMenuAdapter cm = null;
		if (isCurrentType(UNDERLAY_MAP)) {
			cm = RasterMapMenu.createListAdapter(mapActivity, OsmandRasterMapsPlugin.RasterMapType.UNDERLAY);
		} else if (isCurrentType(OVERLAY_MAP)) {
			cm = RasterMapMenu.createListAdapter(mapActivity, OsmandRasterMapsPlugin.RasterMapType.OVERLAY);
		} else if (isCurrentType(CONTOUR_LINES)) {
			cm = ContourLinesMenu.createListAdapter(mapActivity);
		} else if (isCurrentType(OSM_NOTES)) {
			cm = OsmNotesMenu.createListAdapter(mapActivity);
		} else if (isCurrentType(WIKIPEDIA)) {
			cm = WikipediaPoiMenu.createListAdapter(mapActivity);
		}
		if (cm != null) {
			updateListAdapter(cm);
		}
	}

	public void updateListAdapter(ContextMenuAdapter cm) {
		OsmandApplication app = mapActivity.getMyApplication();
		OsmandSettings settings = app.getSettings();
		ApplicationMode appMode = settings.getApplicationMode();

		boolean nightMode = app.getDaynightHelper().isNightModeForMapControls();
		if (this.nightMode != nightMode) {
			this.nightMode = nightMode;
			applyDayNightMode();
		}

		ViewCreator viewCreator = new ViewCreator(mapActivity, nightMode);
		int profileColor = appMode.getProfileColor(nightMode);
		if (isCurrentType(WIKIPEDIA)) {
			viewCreator.setDefaultLayoutId(R.layout.dash_item_with_description_72dp);
			viewCreator.setCustomControlsColor(profileColor);
		} else {
			viewCreator.setDefaultLayoutId(R.layout.list_item_icon_and_menu);
			viewCreator.setCustomControlsColor(profileColor);
		}

		ContextMenuListAdapter listAdapter = cm.toListAdapter(mapActivity, viewCreator);
		OnItemClickListener listener = getOptionsMenuOnClickListener(cm, listAdapter);
		updateListAdapter(listAdapter, listener);
	}

	public void onNewDownloadIndexes() {
		if (isCurrentType(CONTOUR_LINES, WIKIPEDIA)) {
			refreshContent(true);
		}
	}

	@SuppressWarnings("unchecked")
	public void onDownloadInProgress() {
		if (isCurrentType(CONTOUR_LINES, WIKIPEDIA)) {
			DownloadIndexesThread downloadThread = getMyApplication().getDownloadThread();
			IndexItem downloadIndexItem = downloadThread.getCurrentDownloadingItem();
			if (downloadIndexItem != null && listAdapter != null) {
				int downloadProgress = (int) downloadThread.getCurrentDownloadProgress();
				ArrayAdapter<ContextMenuItem> adapter = (ArrayAdapter<ContextMenuItem>) listAdapter;
				for (int i = 0; i < adapter.getCount(); i++) {
					ContextMenuItem item = adapter.getItem(i);
					if (item != null && item.getProgressListener() != null) {
						item.getProgressListener().onProgressChanged(
								downloadIndexItem, downloadProgress, adapter, (int) adapter.getItemId(i), i);
					}
				}
			}
		}
	}

	public void onDownloadHasFinished() {
		if (isCurrentType(CONTOUR_LINES, WIKIPEDIA)) {
			refreshContent(true);
			if (isCurrentType(CONTOUR_LINES)) {
				mapActivity.refreshMapComplete();
			}
		}
	}

	public void refreshContent() {
		refreshContent(!isCurrentTypeHasIndividualFragment());
	}

	public void refreshContent(boolean force) {
		if (force) {
			listView.clearParams();
			updateListAdapter();
		} else if (isCurrentType(CONFIGURE_MAP)) {
			ConfigureMapFragment cm = ConfigureMapFragment.getVisibleInstance(mapActivity);
			if (cm != null) {
				cm.onDataSetInvalidated();
			}
		} else if (isCurrentType(MAPILLARY)) {
			refreshFragment(MapillaryFiltersFragment.TAG);
		} else if (isCurrentType(TERRAIN)) {
			refreshFragment(TerrainFragment.TAG);
		}else if (isCurrentType(RELIEF_3D)) {
			refreshFragment(Relief3DFragment.TAG);
		} else if (isCurrentType(CYCLE_ROUTES)) {
			refreshFragment(CycleRoutesFragment.TAG);
		} else if (isCurrentType(HIKING_ROUTES)) {
			refreshFragment(HikingRoutesFragment.TAG);
		} else if (isCurrentType(TRAVEL_ROUTES)) {
			refreshFragment(TravelRoutesFragment.TAG);
		} else if (isCurrentType(TRANSPORT_LINES)) {
			refreshFragment(TransportLinesFragment.TAG);
		} else if (isCurrentType(WEATHER)) {
			refreshFragment(WeatherMainFragment.TAG);
		} else if (isCurrentType(WEATHER_LAYER)) {
			refreshFragment(WeatherLayerFragment.TAG);
		} else if (isCurrentType(WEATHER_CONTOURS)) {
			refreshFragment(WeatherContoursFragment.TAG);
		} else if (isCurrentType(NAUTICAL_DEPTH)) {
			refreshFragment(NauticalDepthContourFragment.TAG);
		} else if (isCurrentType(MTB_ROUTES)) {
			refreshFragment(MtbRoutesFragment.TAG);
		} else if (isCurrentType(ALPINE_HIKING)) {
			refreshFragment(AlpineHikingScaleFragment.TAG);
		} else if (listAdapter != null) {
			listAdapter.notifyDataSetChanged();
		}
	}

	private void refreshFragment(@NonNull String tag) {
		FragmentManager fragmentManager = mapActivity.getSupportFragmentManager();
		Fragment fragment = fragmentManager.findFragmentByTag(tag);
		if (fragment != null && AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			fragmentManager.beginTransaction()
					.detach(fragment)
					.attach(fragment)
					.commitAllowingStateLoss();
		}
	}

	private OnItemClickListener getOptionsMenuOnClickListener(ContextMenuAdapter adapter,
	                                                          ContextMenuListAdapter listAdapter) {
		return (parent, view, position, id) -> {
			int size = adapter.getItems().size();
			if (position < 0 || position >= size) {
				LOG.warn("Tried to select item " + position + " items in list: " + size);
				return;
			}
			ContextMenuItem item = adapter.getItem(position);
			ItemClickListener click = item.getItemClickListener();
			if (click instanceof OnRowItemClick) {
				boolean cl = ((OnRowItemClick) click).onRowItemClick(listAdapter, view, item);
				if (cl) {
					hideDashboard();
				}
			} else if (click != null) {
				CompoundButton btn = view.findViewById(R.id.toggle_item);
				if (btn != null && btn.getVisibility() == View.VISIBLE) {
					btn.setChecked(!btn.isChecked());
				} else if (click.onContextMenuClick(listAdapter, view, item, false)) {
					hideDashboard();
				}
			} else if (!item.isCategory()) {
				hideDashboard();
			}
		};
	}

	private void updateDownloadBtn() {
		Button btn = dashboardView.findViewById(R.id.map_download_button);
		String filter = null;
		String txt = "";
		OsmandMapTileView mv = mapActivity.getMapView();
		if (!mapActivity.getMyApplication().isApplicationInitializing()) {
			if (mv.getZoom() < 11 && !mapActivity.getMyApplication().getResourceManager().containsBasemap()) {
				filter = "basemap";
				txt = mapActivity.getString(R.string.shared_string_download) + " "
						+ mapActivity.getString(R.string.base_world_map);
			} else {
				DownloadedRegionsLayer dl = mv.getLayerByClass(DownloadedRegionsLayer.class);
				if (dl != null) {
					StringBuilder btnName = new StringBuilder();
					filter = dl.getFilter(btnName);
					txt = btnName.toString();
				}
			}
		}

		btn.setText(txt);
		btn.setVisibility(filter == null ? View.GONE : View.VISIBLE);
		String f = filter;
		btn.setOnClickListener(v -> {
			hideDashboard(false);
			Intent intent = new Intent(mapActivity, mapActivity.getMyApplication().getAppCustomization()
					.getDownloadIndexActivity());
			if (f != null && !f.equals("basemap")) {
				intent.putExtra(DownloadActivity.FILTER_KEY, f);
			}
			intent.putExtra(DownloadActivity.TAB_TO_OPEN, DownloadActivity.DOWNLOAD_TAB);
			mapActivity.startActivity(intent);
		});
		scheduleDownloadButtonCheck();
	}

	private void scheduleDownloadButtonCheck() {
		mapActivity.getMyApplication().runInUIThread(() -> {
			if (isVisible()) {
				updateDownloadBtn();
			}
		}, 4000);
	}

	void navigationAction() {
		RoutingHelper routingHelper = mapActivity.getRoutingHelper();
		if (!routingHelper.isFollowingMode() && !routingHelper.isRoutePlanningMode()) {
			mapActivity.getMapActions().enterRoutePlanningMode(null, null);
		} else {
			mapActivity.getRoutingHelper().setRoutePlanningMode(true);
			mapActivity.getMapViewTrackingUtilities().switchRoutePlanningMode();
			mapActivity.refreshMap();
		}
		boolean animate = !getMyApplication().getSettings().DO_NOT_USE_ANIMATIONS.get();
		hideDashboard(animate);
	}

	// To bounce animate view
	private void open(boolean animation, int[] animationCoordinates) {
		if (animation) {
			this.animationCoordinates = animationCoordinates;
			animateDashboard(true);
		} else {
			dashboardView.findViewById(R.id.animateContent).setVisibility(View.VISIBLE);
			dashboardView.findViewById(R.id.toolbar).setVisibility(View.VISIBLE);
		}
	}

	private void animateDashboard(boolean show) {
		View content = dashboardView.findViewById(R.id.animateContent);
		View toolbar = dashboardView.findViewById(R.id.toolbar);
		AnimatorSet set = new AnimatorSet();
		List<Animator> animators = new ArrayList<>();
		if (animationCoordinates != null) {
			float screenHeight = mapActivity.getResources().getDisplayMetrics().heightPixels;
			float screenWidth = mapActivity.getResources().getDisplayMetrics().widthPixels;
			float initialValueX = show ? animationCoordinates[0] - screenWidth / 2 : 0;
			float finalValueX = show ? 0 : animationCoordinates[0] - screenWidth / 2;
			float initialValueY = show ? animationCoordinates[1] - screenHeight / 2 : 0;
			float finalValueY = show ? 0 : animationCoordinates[1] - screenHeight / 2;
			animators.add(ObjectAnimator.ofFloat(content, View.TRANSLATION_X, initialValueX, finalValueX));
			animators.add(ObjectAnimator.ofFloat(content, View.TRANSLATION_Y, initialValueY, finalValueY));
		}
		if (ViewCompat.isAttachedToWindow(content)) {
			int centerX = content.getMeasuredWidth() / 2;
			int centerY = content.getMeasuredHeight() / 2;
			float initialRadius = show ? 0 : (float) Math.sqrt(Math.pow((float) content.getWidth() / 2, 2) + Math.pow((float) content.getHeight() / 2, 2));
			float finalRadius = show ? (float) Math.sqrt(Math.pow((float) content.getWidth() / 2, 2) + Math.pow((float) content.getHeight() / 2, 2)) : 0;
			Animator circleAnimator = ViewAnimationUtils.createCircularReveal(content, centerX, centerY, initialRadius, finalRadius);
			animators.add(circleAnimator);
		}
		float initialValueScale = show ? 0f : 1f;
		float finalValueScale = show ? 1f : 0f;
		animators.add(ObjectAnimator.ofFloat(content, View.SCALE_X, initialValueScale, finalValueScale));
		animators.add(ObjectAnimator.ofFloat(content, View.SCALE_Y, initialValueScale, finalValueScale));
		float initialToolbarTransY = show ? -toolbar.getHeight() : 0;
		float finalToolbarTransY = show ? 0 : -toolbar.getHeight();
		animators.add(ObjectAnimator.ofFloat(toolbar, View.TRANSLATION_Y, initialToolbarTransY, finalToolbarTransY));
		for (Animator animator : animators) {
			animator.setDuration(300);
		}
		set.playTogether(animators);
		set.addListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationStart(Animator animation) {
				super.onAnimationStart(animation);
				listView.setVerticalScrollBarEnabled(false);
				if (show) {
					content.setVisibility(View.VISIBLE);
					toolbar.setVisibility(View.VISIBLE);
				}
			}

			@Override
			public void onAnimationEnd(Animator animation) {
				super.onAnimationEnd(animation);
				listView.setVerticalScrollBarEnabled(true);
				if (!show) {
					dashboardView.setVisibility(View.GONE);
					content.setVisibility(View.GONE);
					toolbar.setVisibility(View.GONE);
					content.setTranslationX(0);
					content.setTranslationY(0);
					toolbar.setTranslationY(0);
					content.setScaleX(initialValueScale);
					content.setScaleY(initialValueScale);
				}
			}
		});
		set.start();
	}

	private void hide(boolean animation) {
		if (compassButton != null) {
			mapActivity.getMapLayers().getMapControlsLayer().restoreCompassButton();
			compassButton = null;
		}
		if (!animation) {
			dashboardView.setVisibility(View.GONE);
			dashboardView.findViewById(R.id.animateContent).setVisibility(View.GONE);
			dashboardView.findViewById(R.id.toolbar).setVisibility(View.GONE);
		} else {
			animateDashboard(false);
		}
		animationCoordinates = null;
	}

	private void addOrUpdateDashboardFragments() {
		OsmandSettings settings = getMyApplication().getSettings();
		TransactionBuilder builder =
				new TransactionBuilder(mapActivity.getSupportFragmentManager(), settings, mapActivity);
		builder.addFragmentsData(fragmentsData)
				.addFragmentsData(PluginsHelper.getPluginsCardsList())
				.getFragmentTransaction().commitAllowingStateLoss();
	}

	private void removeFragment(String tag) {
		FragmentManager manager = mapActivity.getSupportFragmentManager();
		Fragment fragment = manager.findFragmentByTag(tag);
		if (fragment != null) {
			OsmandSettings settings = getMyApplication().getSettings();
			TransactionBuilder builder = new TransactionBuilder(manager, settings, mapActivity);
			builder.getFragmentTransaction()
					.remove(fragment)
					.commitAllowingStateLoss();
		}
	}

	public boolean isVisible() {
		return visible;
	}

	public boolean isCurrentType(@NonNull DashboardType... types) {
		for (DashboardType type : types) {
			if (visibleTypes.getCurrent() == type) {
				return true;
			}
		}
		return false;
	}

	public boolean isNoCurrentType(@NonNull DashboardType... types) {
		return !isCurrentType(types);
	}

	public boolean isCurrentTypeHasIndividualFragment() {
		return isCurrentType(
				CONFIGURE_MAP, MAPILLARY, TERRAIN, RELIEF_3D, CYCLE_ROUTES, HIKING_ROUTES,
				TRAVEL_ROUTES, TRANSPORT_LINES, WEATHER, WEATHER_LAYER,
				WEATHER_CONTOURS, NAUTICAL_DEPTH, MTB_ROUTES, ALPINE_HIKING
		);
	}

	void onDetach(DashBaseFragment dashBaseFragment) {
		Iterator<WeakReference<DashBaseFragment>> it = fragList.iterator();
		while (it.hasNext()) {
			WeakReference<DashBaseFragment> wr = it.next();
			if (wr.get() == dashBaseFragment) {
				it.remove();
			}
		}
	}

	public void onAppModeChanged() {
		if (isCurrentType(CONFIGURE_MAP)) {
			refreshContent(false);
		}
	}

	public void updateLocation(boolean centerChanged, boolean locationChanged,
	                           boolean compassChanged) {
		if (inLocationUpdate) {
			return;
		}
		inLocationUpdate = true;
		mapActivity.runOnUiThread(() -> {
			inLocationUpdate = false;
			for (WeakReference<DashBaseFragment> df : fragList) {
				if (df.get() instanceof DashLocationFragment) {
					((DashLocationFragment) df.get()).updateLocation(centerChanged, locationChanged, compassChanged);
				}
			}
		});

	}

	public void updateMyLocation(net.osmand.Location location) {
		updateLocation(false, true, false);
	}

	public void updateCompassValue(double heading) {
		this.heading = (float) heading;
		updateLocation(false, false, true);
	}

	public void onAttach(DashBaseFragment dashBaseFragment) {
		fragList.add(new WeakReference<>(dashBaseFragment));
	}

	public boolean onBackPressed() {
		if (isVisible()) {
			backPressed();
			return true;
		}
		return false;
	}

	private void backPressed() {
		// Remove known scroll when screen closed
		lastKnownScrolls.remove(visibleTypes.getCurrent());

		DashboardType previous = visibleTypes.getPrevious();
		if (previous != null) {
			if (isCurrentType(MAPILLARY)) {
				hideKeyboard();
			}
			visibleTypes.pop(); // Remove current visible type.
			visibleTypes.pop(); // Also remove previous type. It will be add later.
			setDashboardVisibility(true, previous);
		} else {
			hideDashboard();
			mapActivity.getFragmentsHelper().backToConfigureProfileFragment();
		}
	}

	private void hideKeyboard() {
		View currentFocus = mapActivity.getCurrentFocus();
		if (currentFocus != null) {
			InputMethodManager imm = (InputMethodManager) mapActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
			if (imm != null) {
				imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
			}
		}
	}

	private void applyScrollPosition(@NonNull ScrollView scrollView) {
		Integer lastKnownScroll = lastKnownScrolls.get(visibleTypes.getCurrent());
		applyScrollPosition(scrollView, lastKnownScroll != null ? lastKnownScroll : 0);
	}

	private void applyScrollPosition(@NonNull ScrollView scrollView, int scrollYPos) {
		scrollView.postDelayed(() -> {
			scrollView.scrollTo(0, scrollYPos);
			onScrollChangedImpl(scrollYPos);
		}, 100);
	}

	@Override
	public void onScrollChanged(int scrollY, boolean firstScroll, boolean dragging) {
		lastKnownScrolls.put(visibleTypes.getCurrent(), scrollY);
		onScrollChangedImpl(scrollY);
	}

	private void onScrollChangedImpl(int scrollY) {
		// Translate list background
		if (portrait) {
			if (listBackgroundView != null) {
				setTranslationY(listBackgroundView, Math.max(0, -scrollY + mFlexibleSpaceImageHeight));
			}
		}
		if (portrait && toolbar.getVisibility() == View.VISIBLE) {
			setTranslationY(toolbar, Math.min(0, -scrollY + mFlexibleSpaceImageHeight - mFlexibleBlurSpaceHeight));
		}
		updateColorOfToolbar(scrollY);
		updateTopButton(scrollY);
		updateMapShadow(scrollY);
	}

	private boolean isActionButtonVisible() {
		return isCurrentType(DASHBOARD);
	}

	private boolean isBackButtonVisible() {
		return isNoCurrentType(DASHBOARD);
	}

	private void updateMapShadow(int scrollY) {
		View shadowOnMap = dashboardView.findViewById(R.id.shadow_on_map);
		if (shadowOnMap != null) {
			int minTop = dashboardView.findViewById(R.id.map_part_dashboard).getHeight() - toolbar.getHeight();
			if (scrollY >= minTop) {
				shadowOnMap.setVisibility(View.GONE);
			} else {
				shadowOnMap.setVisibility(View.VISIBLE);
			}
		}
	}

	private void updateTopButton(int scrollY) {
		if (actionButton != null && portrait && isActionButtonVisible()) {
			double scale = mapActivity.getResources().getDisplayMetrics().density;
			int originalPosition = mFlexibleSpaceImageHeight - (int) (80 * scale);
			int minTop = mFlexibleBlurSpaceHeight + (int) (5 * scale);
			FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) actionButton.getLayoutParams();
			if (minTop > originalPosition - scrollY) {
				hideActionButton();
			} else {
				actionButton.setVisibility(View.VISIBLE);
				lp.topMargin = originalPosition - scrollY;
				((FrameLayout) actionButton.getParent()).updateViewLayout(actionButton, lp);
			}
		} else if (compassButton != null) {
			double scale = mapActivity.getResources().getDisplayMetrics().density;
			int originalPosition = mFlexibleSpaceImageHeight - (int) (64 * scale);
			int minTop = mFlexibleBlurSpaceHeight + (int) (5 * scale);
			MarginLayoutParams lp = (MarginLayoutParams) compassButton.getLayoutParams();
			if (minTop > originalPosition - scrollY) {
				hideActionButton();
			} else {
				compassButton.setVisibility(View.VISIBLE);
				lp.topMargin = originalPosition - scrollY;
				((ViewGroup) compassButton.getParent()).updateViewLayout(compassButton, lp);
			}
		}
	}

	private void updateColorOfToolbar(int scrollY) {
		if (portrait) {
			float sh = mFlexibleSpaceImageHeight - mFlexibleBlurSpaceHeight;
			float t = sh == 0 ? 1 : (1 - Math.max(0, -scrollY + sh) / sh);
			t = Math.max(0, t);

			int alpha = (int) (t * 255);
			// in order to have proper fast scroll down
			int malpha = t == 1 ? 0 : alpha;
			setAlpha(paddingView, malpha, baseColor);
			setAlpha(dashboardView.findViewById(R.id.map_part_dashboard), malpha, baseColor);
			gradientToolbar.setAlpha((int) ((1 - t) * 255));
			setAlpha(dashboardView, (int) (t * 128), 0);
			View toolbar = dashboardView.findViewById(R.id.toolbar);
			updateMapShadowColor(malpha);
			if (t < 1) {
				//noinspection deprecation
				toolbar.setBackground(gradientToolbar);
			} else {
				toolbar.setBackgroundColor(0xff000000 | baseColor);
			}
		}
	}

	private void updateMapShadowColor(int alpha) {
		View shadowOnMap = dashboardView.findViewById(R.id.shadow_on_map);
		if (shadowOnMap != null) {
			setAlpha(shadowOnMap, alpha, baseColor);
		}
	}

	private void updateListAdapter(ArrayAdapter<?> listAdapter, OnItemClickListener listener) {
		this.listAdapter = listAdapter;
		adapterClickListener = listener;
		if (listView != null) {
			listView.setAdapter(listAdapter);
			if (adapterClickListener != null) {
				listView.setOnItemClickListener((parent, view, position, id) ->
						adapterClickListener.onItemClick(parent, view, position - listView.getHeaderViewsCount(), id));
			} else {
				listView.setOnItemClickListener(null);
			}
		}
	}

	private void setTranslationY(View v, int y) {
		v.setTranslationY(y);
	}

	@SuppressLint("NewApi")
	private void setAlpha(View v, int alpha, int color) {
		v.setBackgroundColor((alpha << 24) | color);
	}

	<T extends DashBaseFragment> T getFragmentByClass(Class<T> class1) {
		for (WeakReference<DashBaseFragment> f : fragList) {
			DashBaseFragment b = f.get();
			if (b != null && !b.isDetached() && class1.isInstance(b)) {
				//noinspection unchecked
				return (T) b;
			}
		}
		return null;
	}

	void blacklistFragmentByTag(String tag) {
		hideFragmentByTag(tag);
		getMyApplication().getSettings().registerBooleanPreference(SHOULD_SHOW + tag, true)
				.makeGlobal().set(false);
	}

	void hideFragmentByTag(String tag) {
		FragmentManager manager = mapActivity.getSupportFragmentManager();
		Fragment fragment = manager.findFragmentByTag(tag);
		if (fragment != null) {
			manager.beginTransaction()
					.hide(fragment)
					.commitAllowingStateLoss();
		}
	}

	void unblacklistFragmentClass(String tag) {
		unhideFragmentByTag(tag);
		getMyApplication().getSettings().registerBooleanPreference(SHOULD_SHOW + tag, true)
				.makeGlobal().set(true);
	}

	void unhideFragmentByTag(String tag) {
		FragmentManager manager = mapActivity.getSupportFragmentManager();
		Fragment fragment = manager.findFragmentByTag(tag);
		if (fragment != null) {
			manager.beginTransaction()
					.show(fragment)
					.commitAllowingStateLoss();
		}
	}

	View getParentView() {
		return dashboardView;
	}

	public static <T> void handleNumberOfRows(List<T> list, OsmandSettings settings,
	                                          String rowNumberTag) {
		int numberOfRows = settings.registerIntPreference(rowNumberTag, 3)
				.makeGlobal().get();
		if (list.size() > numberOfRows) {
			while (list.size() != numberOfRows) {
				list.remove(numberOfRows);
			}
		}
	}

	public static class DefaultShouldShow extends DashFragmentData.ShouldShowFunction {
		public boolean shouldShow(OsmandSettings settings, MapActivity activity, String tag) {
			return settings.registerBooleanPreference(SHOULD_SHOW + tag, true).makeGlobal().get();
		}
	}

	@Override
	public void newRouteIsCalculated(boolean newRoute, ValueHolder<Boolean> showToast) {
	}

	@Override
	public void routeWasCancelled() {
	}

	@Override
	public void routeWasFinished() {
	}
}
