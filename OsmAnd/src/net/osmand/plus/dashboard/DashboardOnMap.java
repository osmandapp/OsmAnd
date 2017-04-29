package net.osmand.plus.dashboard;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.TranslateAnimation;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.github.ksoichiro.android.observablescrollview.ObservableScrollView;
import com.github.ksoichiro.android.observablescrollview.ObservableScrollViewCallbacks;
import com.github.ksoichiro.android.observablescrollview.ScrollState;

import net.osmand.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.ValueHolder;
import net.osmand.data.LatLon;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuAdapter.OnRowItemClick;
import net.osmand.plus.ContextMenuItem;
import net.osmand.plus.IconsCache;
import net.osmand.plus.MapMarkersHelper;
import net.osmand.plus.MapMarkersHelper.MapMarker;
import net.osmand.plus.MapMarkersHelper.MapMarkerChangedListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.TargetPointsHelper.TargetPoint;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dashboard.tools.DashFragmentData;
import net.osmand.plus.dashboard.tools.DashboardSettingsDialogFragment;
import net.osmand.plus.dashboard.tools.TransactionBuilder;
import net.osmand.plus.dialogs.ConfigureMapMenu;
import net.osmand.plus.dialogs.RasterMapMenu;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadIndexesThread;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.MapMarkerDialogHelper;
import net.osmand.plus.helpers.MapMarkerDialogHelper.MapMarkersDialogHelperCallbacks;
import net.osmand.plus.helpers.WaypointDialogHelper;
import net.osmand.plus.helpers.WaypointDialogHelper.WaypointDialogHelperCallbacks;
import net.osmand.plus.helpers.WaypointHelper.LocationPointWrapper;
import net.osmand.plus.mapcontextmenu.other.MapRouteInfoMenu;
import net.osmand.plus.mapcontextmenu.other.RoutePreferencesMenu;
import net.osmand.plus.mapcontextmenu.other.RoutePreferencesMenu.LocalRoutingParameter;
import net.osmand.plus.mapillary.MapillaryPlugin;
import net.osmand.plus.mapillary.MapillaryPlugin.MapillaryFirstDialogFragment;
import net.osmand.plus.rastermaps.OsmandRasterMapsPlugin;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.routing.RoutingHelper.IRouteInformationListener;
import net.osmand.plus.srtmplugin.ContourLinesMenu;
import net.osmand.plus.srtmplugin.HillshadeMenu;
import net.osmand.plus.srtmplugin.SRTMPlugin;
import net.osmand.plus.views.DownloadedRegionsLayer;
import net.osmand.plus.views.MapInfoLayer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.controls.DynamicListView;
import net.osmand.plus.views.controls.DynamicListViewCallbacks;
import net.osmand.plus.views.controls.StableArrayAdapter;
import net.osmand.plus.views.controls.SwipeDismissListViewTouchListener;
import net.osmand.plus.views.controls.SwipeDismissListViewTouchListener.DismissCallbacks;
import net.osmand.plus.views.controls.SwipeDismissListViewTouchListener.Undoable;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static android.util.TypedValue.COMPLEX_UNIT_DIP;

public class DashboardOnMap implements ObservableScrollViewCallbacks, DynamicListViewCallbacks,
		IRouteInformationListener, WaypointDialogHelperCallbacks, MapMarkersDialogHelperCallbacks,
		MapMarkerChangedListener {
	private static final org.apache.commons.logging.Log LOG =
			PlatformUtil.getLog(DashboardOnMap.class);
	private static final String TAG = "DashboardOnMap";
	public static boolean staticVisible = false;
	public static DashboardType staticVisibleType = DashboardType.DASHBOARD;
	public static final String SHOULD_SHOW = "should_show";


	private final DashFragmentData[] fragmentsData = new DashFragmentData[]{
			new DashFragmentData(DashRateUsFragment.TAG, DashRateUsFragment.class,
					DashRateUsFragment.SHOULD_SHOW_FUNCTION, 0, null),
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

	private MapActivity mapActivity;
	private ImageView actionButton;
	private View compassButton;
	private FrameLayout dashboardView;
	private float cachedRotate = 0;

	private ArrayAdapter<?> listAdapter;
	private OnItemClickListener listAdapterOnClickListener;
	private SwipeDismissListViewTouchListener swipeDismissListener;

	private boolean visible = false;
	private DashboardType visibleType;
	private DashboardType previousVisibleType;
	private ApplicationMode previousAppMode;
	private boolean landscape;
	private List<WeakReference<DashBaseFragment>> fragList = new LinkedList<>();
	private net.osmand.Location myLocation;
	private LatLon mapViewLocation;
	private float heading;
	private boolean mapLinkedToLocation;
	private float mapRotation;
	private boolean inLocationUpdate = false;
	private DynamicListView listView;
	private View listBackgroundView;
	private Toolbar toolbar;
	private View paddingView;
	private int mFlexibleSpaceImageHeight;
	private int mFlexibleBlurSpaceHeight;
	private boolean portrait;
	private long lastUpOrCancelMotionEventTime;
	private TextView listEmptyTextView;

	int baseColor;

	private WaypointDialogHelper waypointDialogHelper;
	private MapMarkerDialogHelper mapMarkerDialogHelper;
	private final int[] running = new int[]{-1};
	private List<LocationPointWrapper> deletedPoints = new ArrayList<>();
	private Drawable gradientToolbar;
	boolean nightMode;

	public DashFragmentData[] getFragmentsData() {
		return fragmentsData;
	}

	public enum DashboardType {
		WAYPOINTS,
		WAYPOINTS_FLAT,
		CONFIGURE_SCREEN,
		CONFIGURE_MAP,
		LIST_MENU,
		ROUTE_PREFERENCES,
		DASHBOARD,
		OVERLAY_MAP,
		UNDERLAY_MAP,
		CONTOUR_LINES,
		HILLSHADE,
		MAP_MARKERS,
		MAP_MARKERS_SELECTION
	}

	private Map<DashboardActionButtonType, DashboardActionButton> actionButtons = new HashMap<>();

	public enum DashboardActionButtonType {
		MY_LOCATION,
		NAVIGATE,
		ROUTE,
		MARKERS_SELECTION
	}

	private class DashboardActionButton {
		private Drawable icon;
		private String text;
		private View.OnClickListener onClickListener;
	}

	public DashboardOnMap(MapActivity ma) {
		this.mapActivity = ma;
	}


	public void createDashboardView() {
		baseColor = ContextCompat.getColor(mapActivity, R.color.osmand_orange) & 0x00ffffff;
		waypointDialogHelper = new WaypointDialogHelper(mapActivity);
		waypointDialogHelper.setHelperCallbacks(this);
		mapMarkerDialogHelper = new MapMarkerDialogHelper(mapActivity);
		mapMarkerDialogHelper.setHelperCallbacks(this);
		landscape = !AndroidUiHelper.isOrientationPortrait(mapActivity);
		dashboardView = (FrameLayout) mapActivity.findViewById(R.id.dashboard);
		final View.OnClickListener listener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				hideDashboard();
			}
		};
		toolbar = ((Toolbar) dashboardView.findViewById(R.id.toolbar));
		ObservableScrollView scrollView = ((ObservableScrollView) dashboardView.findViewById(R.id.main_scroll));
		listView = (DynamicListView) dashboardView.findViewById(R.id.dash_list_view);
		//listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		listView.setDrawSelectorOnTop(true);
		listView.setDynamicListViewCallbacks(this);
		listEmptyTextView = (TextView) dashboardView.findViewById(R.id.emptyTextView);

		// Create a ListView-specific touch listener. ListViews are given special treatment because
		// by default they handle touches for their list items... i.e. they're in charge of drawing
		// the pressed state (the list selector), handling list item clicks, etc.
		swipeDismissListener = new SwipeDismissListViewTouchListener(
				mapActivity,
				listView,
				new DismissCallbacks() {

					private List<Object> deletedMarkers = new ArrayList<>();

					@Override
					public boolean canDismiss(int position) {
						boolean res = false;
						if (listAdapter instanceof StableArrayAdapter) {
							if (visibleType == DashboardType.WAYPOINTS || visibleType == DashboardType.WAYPOINTS_FLAT) {
								List<Object> activeObjects = ((StableArrayAdapter) listAdapter).getActiveObjects();
								Object obj = listAdapter.getItem(position);
								res = activeObjects.contains(obj);
							} else if (visibleType == DashboardType.MAP_MARKERS || visibleType == DashboardType.MAP_MARKERS_SELECTION) {
								Object obj = listAdapter.getItem(position);
								res = obj instanceof MapMarker;
							}
						}
						return res;
					}

					@Override
					public Undoable onDismiss(final int position) {
						final Object item;
						final StableArrayAdapter stableAdapter;
						final int activeObjPos;
						if (listAdapter instanceof StableArrayAdapter) {
							stableAdapter = (StableArrayAdapter) listAdapter;
							item = stableAdapter.getItem(position);

							if (visibleType == DashboardType.MAP_MARKERS || visibleType == DashboardType.MAP_MARKERS_SELECTION) {
								if (!((MapMarker) item).history) {
									deletedMarkers.add(item);
								}
							}

							stableAdapter.setNotifyOnChange(false);
							stableAdapter.remove(item);
							stableAdapter.getObjects().remove(item);
							activeObjPos = stableAdapter.getActiveObjects().indexOf(item);
							stableAdapter.getActiveObjects().remove(item);
							stableAdapter.refreshData();
							stableAdapter.notifyDataSetChanged();

						} else {
							item = null;
							stableAdapter = null;
							activeObjPos = 0;
						}
						return new Undoable() {
							@Override
							public void undo() {
								if (item != null) {
									stableAdapter.setNotifyOnChange(false);
									stableAdapter.insert(item, position);
									stableAdapter.getObjects().add(position, item);
									stableAdapter.getActiveObjects().add(activeObjPos, item);
									stableAdapter.refreshData();
									if (visibleType == DashboardType.WAYPOINTS || visibleType == DashboardType.WAYPOINTS_FLAT) {
										onItemsSwapped(stableAdapter.getActiveObjects());
									} else if (visibleType == DashboardType.MAP_MARKERS || visibleType == DashboardType.MAP_MARKERS_SELECTION) {
										deletedMarkers.remove(item);
										updateMapMarkers(stableAdapter.getActiveObjects());
										reloadAdapter();
									}
								}
							}

							@Override
							public String getTitle() {
								if ((visibleType == DashboardType.WAYPOINTS || visibleType == DashboardType.WAYPOINTS_FLAT)
										&& (getMyApplication().getRoutingHelper().isRoutePlanningMode() || getMyApplication().getRoutingHelper().isFollowingMode())
										&& item != null
										&& stableAdapter.getActiveObjects().size() == 0) {
									return mapActivity.getResources().getString(R.string.cancel_navigation);
								} else {
									return null;
								}
							}
						};
					}

					@Override
					public void onHidePopup() {
						if (listAdapter instanceof StableArrayAdapter) {
							StableArrayAdapter stableAdapter = (StableArrayAdapter) listAdapter;
							stableAdapter.refreshData();
							if (visibleType == DashboardType.WAYPOINTS || visibleType == DashboardType.WAYPOINTS_FLAT) {
								onItemsSwapped(stableAdapter.getActiveObjects());
							} else if (visibleType == DashboardType.MAP_MARKERS || visibleType == DashboardType.MAP_MARKERS_SELECTION) {
								updateMapMarkers(stableAdapter.getActiveObjects());
								reloadAdapter();
							}
							if (stableAdapter.getActiveObjects().size() == 0) {
								hideDashboard();
								if (visibleType == DashboardType.WAYPOINTS || visibleType == DashboardType.WAYPOINTS_FLAT) {
									mapActivity.getMapActions().stopNavigationWithoutConfirm();
									if (getMyApplication().getSettings().USE_MAP_MARKERS.get()) {
										getMyApplication().getTargetPointsHelper().removeAllWayPoints(false, true);
									}
									mapActivity.getMapLayers().getMapControlsLayer().getMapRouteInfoMenu().hide();
								}
							}
						}
					}

					private void updateMapMarkers(List<Object> objects) {
						List<MapMarker> markers = new ArrayList<>();
						List<MapMarker> markersHistory = new ArrayList<>();

						for (Object obj : objects) {
							MapMarker marker = (MapMarker) obj;
							if (!marker.history) {
								markers.add(marker);
							} else {
								markersHistory.add(marker);
							}
						}

						for (int i = 0; i <= deletedMarkers.size() - 1; i++) {
							markersHistory.add(0, (MapMarker) deletedMarkers.get(i));
						}
						deletedMarkers.clear();

						getMyApplication().getMapMarkersHelper().saveMapMarkers(markers, markersHistory);
					}
				});

		gradientToolbar = ContextCompat.getDrawable(mapActivity, R.drawable.gradient_toolbar).mutate();
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
			shadow.setImageDrawable(ContextCompat.getDrawable(mapActivity, R.drawable.bg_shadow_onmap));
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

		initActionButtons();
		dashboardView.addView(actionButton);
	}

	@Override
	public void onMapMarkerChanged(MapMarker mapMarker) {
		if (visible && visibleType == DashboardType.MAP_MARKERS || visibleType == DashboardType.MAP_MARKERS_SELECTION) {
			mapMarkerDialogHelper.updateMarkerView(listView, mapMarker);
		}
	}

	@Override
	public void onMapMarkersChanged() {
	}

	private void updateListBackgroundHeight() {
		if (listBackgroundView != null) {
			final View contentView = mapActivity.getWindow().getDecorView().findViewById(android.R.id.content);
			ViewTreeObserver vto = contentView.getViewTreeObserver();
			vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

				@Override
				public void onGlobalLayout() {

					ViewTreeObserver obs = contentView.getViewTreeObserver();
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
						obs.removeOnGlobalLayoutListener(this);
					} else {
						//noinspection deprecation
						obs.removeGlobalOnLayoutListener(this);
					}
					listBackgroundView.getLayoutParams().height = contentView.getHeight();
				}
			});
		}
	}

	private void updateToolbarActions() {
		TextView tv = (TextView) dashboardView.findViewById(R.id.toolbar_text);
		tv.setText("");
		boolean waypointsVisible = visibleType == DashboardType.WAYPOINTS || visibleType == DashboardType.WAYPOINTS_FLAT;
		if (waypointsVisible) {
			tv.setText(R.string.waypoints);
		} else if (visibleType == DashboardType.CONFIGURE_MAP) {
			tv.setText(R.string.configure_map);
		} else if (visibleType == DashboardType.CONFIGURE_SCREEN) {
			tv.setText(R.string.layer_map_appearance);
		} else if (visibleType == DashboardType.ROUTE_PREFERENCES) {
			tv.setText(R.string.shared_string_settings);
		} else if (visibleType == DashboardType.UNDERLAY_MAP) {
			tv.setText(R.string.map_underlay);
		} else if (visibleType == DashboardType.OVERLAY_MAP) {
			tv.setText(R.string.map_overlay);
		} else if (visibleType == DashboardType.MAP_MARKERS) {
			tv.setText(R.string.map_markers);
		} else if (visibleType == DashboardType.MAP_MARKERS_SELECTION) {
			tv.setText(R.string.select_map_markers);
		} else if (visibleType == DashboardType.CONTOUR_LINES) {
			tv.setText(R.string.srtm_plugin_name);
		} else if (visibleType == DashboardType.HILLSHADE) {
			tv.setText(R.string.layer_hillshade);
		}
		ImageView edit = (ImageView) dashboardView.findViewById(R.id.toolbar_edit);
		edit.setVisibility(View.GONE);
		ImageView sort = (ImageView) dashboardView.findViewById(R.id.toolbar_sort);
		sort.setVisibility(View.GONE);
		ImageView ok = (ImageView) dashboardView.findViewById(R.id.toolbar_ok);
		ok.setVisibility(View.GONE);
		ImageView flat = (ImageView) dashboardView.findViewById(R.id.toolbar_flat);
		flat.setVisibility(View.GONE);
		ImageView settingsButton = (ImageView) dashboardView.findViewById(R.id.toolbar_settings);
		settingsButton.setVisibility(View.GONE);
		IconsCache iconsCache = mapActivity.getMyApplication().getIconsCache();
		ImageView lst = (ImageView) dashboardView.findViewById(R.id.toolbar_list);
		lst.setVisibility(View.GONE);
		ImageView back = (ImageView) dashboardView.findViewById(R.id.toolbar_back);
		back.setImageDrawable(
				getMyApplication().getIconsCache().getIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha));
		back.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				backPressed();
			}
		});

		if (waypointsVisible && getMyApplication().getWaypointHelper().getAllPoints().size() > 0) {
			if (getMyApplication().getWaypointHelper().isRouteCalculated()) {
				flat.setVisibility(View.VISIBLE);
				final boolean flatNow = visibleType == DashboardType.WAYPOINTS_FLAT;
				flat.setImageDrawable(iconsCache.getIcon(flatNow ? R.drawable.ic_tree_list_dark
						: R.drawable.ic_flat_list_dark));
				flat.setContentDescription(mapActivity.getString(flatNow ? R.string.access_tree_list : R.string.drawer));
				flat.setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						setDashboardVisibility(true, flatNow ? DashboardType.WAYPOINTS : DashboardType.WAYPOINTS_FLAT,
								previousVisibleType, false);
					}
				});
			}
		}

		if (visibleType == DashboardType.MAP_MARKERS || visibleType == DashboardType.MAP_MARKERS_SELECTION
				&& getMyApplication().getMapMarkersHelper().getMapMarkers().size() > 0) {
			sort.setVisibility(View.VISIBLE);
			sort.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					mapMarkerDialogHelper.setSorted(!mapMarkerDialogHelper.isSorted());
					reloadAdapter();
				}
			});
		}

		if (visibleType == DashboardType.DASHBOARD || visibleType == DashboardType.LIST_MENU) {
			settingsButton.setVisibility(View.VISIBLE);
			settingsButton.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					new DashboardSettingsDialogFragment().show(
							mapActivity.getSupportFragmentManager(), "dashboard_settings");
				}
			});
			lst.setVisibility(View.VISIBLE);
			lst.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					hideDashboard(false);
					mapActivity.openDrawer();
				}
			});
		}

		toolbar.getMenu().clear();
		if (visibleType == DashboardType.CONFIGURE_SCREEN) {
			toolbar.inflateMenu(R.menu.refresh_menu);
			toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem menuItem) {
					if (menuItem.getItemId() == R.id.action_refresh) {
						MapWidgetRegistry registry = mapActivity.getMapLayers().getMapWidgetRegistry();
						registry.resetToDefault();
						MapInfoLayer mil = mapActivity.getMapLayers().getMapInfoLayer();
						if (mil != null) {
							mil.recreateControls();
						}
						updateListAdapter(registry.getViewConfigureMenuAdapter(mapActivity));
					}
					return false;
				}
			});
		}
	}

	private FrameLayout.LayoutParams getActionButtonLayoutParams(int btnSizePx) {
		int topPadPx = mapActivity.getResources().getDimensionPixelSize(R.dimen.dashboard_map_top_padding);
		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(btnSizePx, btnSizePx);
		int marginRight = btnSizePx / 4;
		params.setMargins(0, landscape ? 0 : topPadPx - 2 * btnSizePx, marginRight, landscape ? marginRight : 0);
		params.gravity = landscape ? Gravity.BOTTOM | Gravity.RIGHT : Gravity.TOP | Gravity.RIGHT;
		return params;
	}

	private void initActionButtons() {
		actionButton = new ImageView(mapActivity);
		int btnSizePx = mapActivity.getResources().getDimensionPixelSize(R.dimen.map_button_size);
		actionButton.setLayoutParams(getActionButtonLayoutParams(btnSizePx));
		actionButton.setScaleType(ScaleType.CENTER);
		actionButton.setBackgroundResource(R.drawable.btn_circle_blue);
		hideActionButton();


		DashboardActionButton myLocationButton = new DashboardActionButton();
		myLocationButton.icon = ContextCompat.getDrawable(mapActivity, R.drawable.map_my_location);
		myLocationButton.text = mapActivity.getString(R.string.map_widget_back_to_loc);
		myLocationButton.onClickListener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (getMyApplication().accessibilityEnabled()) {
					mapActivity.getMapActions().whereAmIDialog();
				} else {
					mapActivity.getMapViewTrackingUtilities().backToLocationImpl();
				}
				hideDashboard();
			}
		};

		DashboardActionButton navigateButton = new DashboardActionButton();
		navigateButton.icon = ContextCompat.getDrawable(mapActivity, R.drawable.map_start_navigation);
		navigateButton.text = mapActivity.getString(R.string.follow);
		navigateButton.onClickListener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mapActivity.getMapLayers().getMapControlsLayer().doNavigate();
				hideDashboard();
			}
		};

		DashboardActionButton routeButton = new DashboardActionButton();
		routeButton.icon = ContextCompat.getDrawable(mapActivity, R.drawable.map_directions);
		routeButton.text = mapActivity.getString(R.string.layer_route);
		routeButton.onClickListener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				boolean hasTargets = false;
				if (visibleType == DashboardType.MAP_MARKERS_SELECTION) {
					TargetPointsHelper targetPointsHelper = getMyApplication().getTargetPointsHelper();
					MapMarkersHelper markersHelper = getMyApplication().getMapMarkersHelper();
					List<MapMarker> markers = markersHelper.getSelectedMarkers();
					if (markers.size() > 0) {
						int i = 0;
						if (markersHelper.isStartFromMyLocation()) {
							targetPointsHelper.clearStartPoint(false);
						} else {
							MapMarker m = markers.get(i++);
							targetPointsHelper.setStartPoint(new LatLon(m.getLatitude(), m.getLongitude()),
									false, m.getPointDescription(mapActivity));
						}
						List<TargetPoint> targetPoints = new ArrayList<>();
						for (int k = i; k < markers.size(); k++) {
							MapMarker m = markers.get(k);
							TargetPoint t = new TargetPoint(new LatLon(m.getLatitude(), m.getLongitude()),
									m.getPointDescription(mapActivity));
							targetPoints.add(t);
						}
						RoutingHelper routingHelper = mapActivity.getRoutingHelper();
						boolean updateRoute = routingHelper.isFollowingMode() || routingHelper.isRoutePlanningMode();
						targetPointsHelper.reorderAllTargetPoints(targetPoints, updateRoute);
						hasTargets = true;
					} else {
						targetPointsHelper.clearStartPoint(false);
						targetPointsHelper.clearPointToNavigate(false);
					}
				}
				hideDashboard();
				mapActivity.getMapLayers().getMapControlsLayer().doRoute(hasTargets);
			}
		};

		DashboardActionButton markersSelectionButton = new DashboardActionButton();
		markersSelectionButton.icon = ContextCompat.getDrawable(mapActivity, R.drawable.map_start_navigation);
		markersSelectionButton.text = mapActivity.getString(R.string.map_markers);
		markersSelectionButton.onClickListener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				setDashboardVisibility(true, DashboardType.MAP_MARKERS_SELECTION);
			}
		};

		actionButtons.put(DashboardActionButtonType.MY_LOCATION, myLocationButton);
		actionButtons.put(DashboardActionButtonType.NAVIGATE, navigateButton);
		actionButtons.put(DashboardActionButtonType.ROUTE, routeButton);
		actionButtons.put(DashboardActionButtonType.MARKERS_SELECTION, markersSelectionButton);
	}

	private void setActionButton(DashboardType type) {
		DashboardActionButton button = null;

		if (type == DashboardType.DASHBOARD
				|| type == DashboardType.LIST_MENU
				|| type == DashboardType.CONFIGURE_SCREEN) {
			button = actionButtons.get(DashboardActionButtonType.MY_LOCATION);
		} else if (type == DashboardType.ROUTE_PREFERENCES) {
			button = actionButtons.get(DashboardActionButtonType.NAVIGATE);
		} else if (type == DashboardType.WAYPOINTS || type == DashboardType.WAYPOINTS_FLAT) {
			if (isInRouteOrPlannigMode()) {
				button = actionButtons.get(DashboardActionButtonType.NAVIGATE);
			} else {
				button = actionButtons.get(DashboardActionButtonType.ROUTE);
			}
		} else if (type == DashboardType.MAP_MARKERS) {
			button = actionButtons.get(DashboardActionButtonType.MARKERS_SELECTION);
		} else if (type == DashboardType.MAP_MARKERS_SELECTION) {
			button = actionButtons.get(DashboardActionButtonType.ROUTE);
		}

		if (button != null) {
			actionButton.setImageDrawable(button.icon);
			actionButton.setContentDescription(button.text);
			actionButton.setOnClickListener(button.onClickListener);
		}
	}

	private boolean isInRouteOrPlannigMode() {
		boolean routePlanningMode = false;
		RoutingHelper rh = mapActivity.getRoutingHelper();
		if (rh.isRoutePlanningMode()) {
			routePlanningMode = true;
		} else if ((rh.isRouteCalculated() || rh.isRouteBeingCalculated()) && !rh.isFollowingMode()) {
			routePlanningMode = true;
		}
		boolean routeFollowingMode = !routePlanningMode && rh.isFollowingMode();
		return routePlanningMode || routeFollowingMode;
	}

	private void hideActionButton() {
		actionButton.setVisibility(View.GONE);
		if (compassButton != null) {
			compassButton.setVisibility(View.GONE);
		}
	}

	public net.osmand.Location getMyLocation() {
		return myLocation;
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

	public OnItemClickListener getListAdapterOnClickListener() {
		return listAdapterOnClickListener;
	}

	public void hideDashboard() {
		setDashboardVisibility(false, visibleType);
	}

	public void hideDashboard(boolean animation) {
		setDashboardVisibility(false, visibleType, animation);
	}

	public void setDashboardVisibility(boolean visible, DashboardType type) {
		setDashboardVisibility(visible, type, this.visible ? visibleType : null, true);
	}

	public void setDashboardVisibility(boolean visible, DashboardType type, boolean animation) {
		setDashboardVisibility(visible, type, this.visible ? visibleType : null, animation);
	}

	public void refreshDashboardFragments() {
		addOrUpdateDashboardFragments();
	}

	public void setDashboardVisibility(boolean visible, DashboardType type, DashboardType prevItem, boolean animation) {
		if (visible == this.visible && type == visibleType) {
			return;
		}
		mapActivity.getRoutingHelper().removeListener(this);
		nightMode = mapActivity.getMyApplication().getDaynightHelper().isNightModeForMapControls();
		this.previousVisibleType = prevItem;
		this.visible = visible;
		ApplicationMode currentAppMode = getMyApplication().getSettings().APPLICATION_MODE.get();
		boolean appModeChanged = currentAppMode != previousAppMode;

		boolean refresh = this.visibleType == type && !appModeChanged;
		previousAppMode = currentAppMode;
		this.visibleType = type;
		DashboardOnMap.staticVisible = visible;
		DashboardOnMap.staticVisibleType = type;
		mapActivity.enableDrawer();

		getMyApplication().getMapMarkersHelper().removeListener(this);
		if (mapActivity.getMapLayers().getMapMarkersLayer().clearRoute()) {
			mapActivity.refreshMap();
		}
		if (swipeDismissListener != null) {
			swipeDismissListener.discardUndo();
		}

		if (visible) {
			mapActivity.getContextMenu().hideMenues();
			mapViewLocation = mapActivity.getMapLocation();
			mapRotation = mapActivity.getMapRotate();
			mapLinkedToLocation = mapActivity.getMapViewTrackingUtilities().isMapLinkedToLocation();
			myLocation = mapActivity.getMyApplication().getLocationProvider().getLastKnownLocation();
			mapActivity.getMapViewTrackingUtilities().setDashboard(this);
			mapActivity.disableDrawer();
			dashboardView.setVisibility(View.VISIBLE);
			if (isActionButtonVisible()) {
				setActionButton(visibleType);
				actionButton.setVisibility(View.VISIBLE);
			} else {
				hideActionButton();
				if (visibleType == DashboardType.CONFIGURE_MAP) {
					int btnSizePx = mapActivity.getResources().getDimensionPixelSize(R.dimen.map_small_button_size);
					compassButton = mapActivity.getMapLayers().getMapControlsLayer()
							.moveCompassButton(dashboardView, getActionButtonLayoutParams(btnSizePx), nightMode);
				}
			}
			updateDownloadBtn();
			View listViewLayout = dashboardView.findViewById(R.id.dash_list_view_layout);
			ScrollView scrollView = (ScrollView) dashboardView.findViewById(R.id.main_scroll);
			if (visibleType == DashboardType.DASHBOARD) {
				addOrUpdateDashboardFragments();
				scrollView.setVisibility(View.VISIBLE);
				scrollView.scrollTo(0, 0);
				listViewLayout.setVisibility(View.GONE);
				onScrollChanged(scrollView.getScrollY(), false, false);
			} else {
				scrollView.setVisibility(View.GONE);
				listViewLayout.setVisibility(View.VISIBLE);
				if (refresh) {
					refreshContent(false);
				} else {
					listView.scrollTo(0, 0);
					listView.clearParams();
					onScrollChanged(listView.getScrollY(), false, false);
					updateListAdapter();
				}
				updateListBackgroundHeight();
				applyDayNightMode();

				if (visibleType == DashboardType.MAP_MARKERS || visibleType == DashboardType.MAP_MARKERS_SELECTION) {
					getMyApplication().getMapMarkersHelper().addListener(this);
				}
			}
			mapActivity.findViewById(R.id.toolbar_back).setVisibility(isBackButtonVisible() ? View.VISIBLE : View.GONE);
			mapActivity.findViewById(R.id.MapHudButtonsOverlay).setVisibility(View.INVISIBLE);
			boolean portrait = AndroidUiHelper.isOrientationPortrait(mapActivity);
			if (!portrait) {
				AndroidUiHelper.updateVisibility(mapActivity.findViewById(R.id.map_route_land_left_margin_external), true);
				mapActivity.getMapView().setMapPositionX(1);
				mapActivity.refreshMap();
			}

			updateToolbarActions();
			//fabButton.showFloatingActionButton();
			open(dashboardView.findViewById(R.id.animateContent), animation);
			updateLocation(true, true, false);
//			addOrUpdateDashboardFragments();
			mapActivity.getRoutingHelper().addListener(this);
		} else {
			mapActivity.getMapViewTrackingUtilities().setDashboard(null);
			hide(dashboardView.findViewById(R.id.animateContent), animation);

			if (!MapRouteInfoMenu.isVisible()) {
				AndroidUiHelper.updateVisibility(mapActivity.findViewById(R.id.map_route_land_left_margin_external), false);
				mapActivity.getMapView().setMapPositionX(0);
				mapActivity.getMapView().refreshMap();
			}

			mapActivity.findViewById(R.id.MapHudButtonsOverlay).setVisibility(View.VISIBLE);
			hideActionButton();
			for (WeakReference<DashBaseFragment> df : fragList) {
				if (df.get() != null) {
					df.get().onCloseDash();
				}
			}

			OsmandSettings settings = getMyApplication().getSettings();
			if (settings.SHOW_MAPILLARY.get() && !settings.MAPILLARY_FIRST_DIALOG_SHOWN.get()) {
				MapillaryFirstDialogFragment fragment = new MapillaryFirstDialogFragment();
				fragment.show(mapActivity.getSupportFragmentManager(), MapillaryFirstDialogFragment.TAG);
				settings.MAPILLARY_FIRST_DIALOG_SHOWN.set(true);
			}
		}
	}

	public void updateDashboard() {
		if (visibleType == DashboardType.ROUTE_PREFERENCES) {
			refreshContent(false);
		}
	}

	private void applyDayNightMode() {
		final int backgroundColor;
		backgroundColor = ContextCompat.getColor(mapActivity,
				nightMode ? R.color.ctx_menu_info_view_bg_dark
						: R.color.ctx_menu_info_view_bg_light);
		Drawable dividerDrawable = new ColorDrawable(ContextCompat.getColor(mapActivity,
				nightMode ? R.color.dashboard_divider_dark : R.color.dashboard_divider_light));

		if (listBackgroundView != null) {
			listBackgroundView.setBackgroundColor(backgroundColor);
		} else {
//			listView.setBackgroundColor(backgroundColor);
			listEmptyTextView.setBackgroundColor(backgroundColor);
		}
		if (visibleType != DashboardType.WAYPOINTS
				&& visibleType != DashboardType.MAP_MARKERS
				&& visibleType != DashboardType.MAP_MARKERS_SELECTION
				&& visibleType != DashboardType.CONFIGURE_SCREEN
				&& visibleType != DashboardType.CONFIGURE_MAP
				&& visibleType != DashboardType.CONTOUR_LINES
				&& visibleType != DashboardType.HILLSHADE) {
			listView.setDivider(dividerDrawable);
			listView.setDividerHeight(dpToPx(1f));
		} else {
			listView.setDivider(null);
		}
		AndroidUtils.setTextSecondaryColor(mapActivity, listEmptyTextView, nightMode);
	}

	private int dpToPx(float dp) {
		Resources r = mapActivity.getResources();
		return (int) TypedValue.applyDimension(COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
	}

	private void updateListAdapter() {
		listEmptyTextView.setVisibility(View.GONE);
		listView.setEmptyView(null);
		ContextMenuAdapter cm = null;
		if (DashboardType.WAYPOINTS == visibleType || DashboardType.WAYPOINTS_FLAT == visibleType) {

			StableArrayAdapter listAdapter = waypointDialogHelper.getWaypointsDrawerAdapter(true, deletedPoints, mapActivity, running,
					DashboardType.WAYPOINTS_FLAT == visibleType, nightMode);
			OnItemClickListener listener = waypointDialogHelper.getDrawerItemClickListener(mapActivity, running,
					listAdapter);

			setDynamicListItems(listView, listAdapter);
			updateListAdapter(listAdapter, listener);

			if (listAdapter.getObjects().size() == 0) {
				listEmptyTextView.setText(mapActivity.getString(R.string.no_waypoints_found));
				if (landscape) {
					listView.setEmptyView(listEmptyTextView);
				} else {
					listEmptyTextView.setVisibility(View.VISIBLE);
				}
			}

		} else if (DashboardType.MAP_MARKERS == visibleType || visibleType == DashboardType.MAP_MARKERS_SELECTION) {

			mapMarkerDialogHelper.setSelectionMode(visibleType == DashboardType.MAP_MARKERS_SELECTION);
			mapMarkerDialogHelper.setNightMode(nightMode);
			StableArrayAdapter listAdapter = mapMarkerDialogHelper.getMapMarkersListAdapter();
			OnItemClickListener listener = mapMarkerDialogHelper.getItemClickListener(listAdapter);

			setDynamicListItems(listView, listAdapter);
			updateListAdapter(listAdapter, listener);
			if (visibleType == DashboardType.MAP_MARKERS_SELECTION) {
				showMarkersRouteOnMap();
			}

			if (listAdapter.getObjects().size() == 0) {
				listEmptyTextView.setText(mapActivity.getString(R.string.no_map_markers_found));
				if (landscape) {
					listView.setEmptyView(listEmptyTextView);
				} else {
					listEmptyTextView.setVisibility(View.VISIBLE);
				}
			}

		} else {

			if (visibleType == DashboardType.CONFIGURE_SCREEN) {
				cm = mapActivity.getMapLayers().getMapWidgetRegistry().getViewConfigureMenuAdapter(mapActivity);
			} else if (visibleType == DashboardType.CONFIGURE_MAP) {
				cm = new ConfigureMapMenu().createListAdapter(mapActivity);
			} else if (visibleType == DashboardType.LIST_MENU) {
				cm = mapActivity.getMapActions().createMainOptionsMenu();
			} else if (visibleType == DashboardType.ROUTE_PREFERENCES) {
				RoutePreferencesMenu routePreferencesMenu = new RoutePreferencesMenu(mapActivity);
				ArrayAdapter<LocalRoutingParameter> listAdapter = routePreferencesMenu.getRoutePreferencesDrawerAdapter(nightMode);
				OnItemClickListener listener = routePreferencesMenu.getItemClickListener(listAdapter);
				updateListAdapter(listAdapter, listener);
			} else if (visibleType == DashboardType.UNDERLAY_MAP) {
				cm = RasterMapMenu.createListAdapter(mapActivity, OsmandRasterMapsPlugin.RasterMapType.UNDERLAY);
			} else if (visibleType == DashboardType.OVERLAY_MAP) {
				cm = RasterMapMenu.createListAdapter(mapActivity, OsmandRasterMapsPlugin.RasterMapType.OVERLAY);
			} else if (visibleType == DashboardType.CONTOUR_LINES) {
				cm = ContourLinesMenu.createListAdapter(mapActivity);
			} else if (visibleType == DashboardType.HILLSHADE) {
				cm = HillshadeMenu.createListAdapter(mapActivity);
			}
			if (cm != null) {
				updateListAdapter(cm);
			}
		}
	}

	public void updateListAdapter(ContextMenuAdapter cm) {
		boolean nightMode = mapActivity.getMyApplication().getDaynightHelper().isNightModeForMapControls();
		if (this.nightMode != nightMode) {
			this.nightMode = nightMode;
			applyDayNightMode();
		}
		final ArrayAdapter<ContextMenuItem> listAdapter = cm.createListAdapter(mapActivity, !nightMode);
		OnItemClickListener listener = getOptionsMenuOnClickListener(cm, listAdapter);
		updateListAdapter(listAdapter, listener);
	}

	public void onNewDownloadIndexes() {
		if (visibleType == DashboardType.CONTOUR_LINES || visibleType == DashboardType.HILLSHADE) {
			refreshContent(true);
		}
	}

	@SuppressWarnings("unchecked")
	public void onDownloadInProgress() {
		if (visibleType == DashboardType.CONTOUR_LINES || visibleType == DashboardType.HILLSHADE) {
			DownloadIndexesThread downloadThread = getMyApplication().getDownloadThread();
			IndexItem downloadIndexItem = downloadThread.getCurrentDownloadingItem();
			if (downloadIndexItem != null) {
				int downloadProgress = downloadThread.getCurrentDownloadingItemProgress();
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
		if (visibleType == DashboardType.CONTOUR_LINES || visibleType == DashboardType.HILLSHADE) {
			refreshContent(true);
			if (visibleType == DashboardType.HILLSHADE) {
				SRTMPlugin plugin = OsmandPlugin.getEnabledPlugin(SRTMPlugin.class);
				if (plugin != null && plugin.isHillShadeLayerEnabled()) {
					plugin.registerLayers(mapActivity);
				}
			}
			SRTMPlugin.refreshMapComplete(mapActivity);
		}
	}

	public void refreshContent(boolean force) {
		if (visibleType == DashboardType.WAYPOINTS
				|| visibleType == DashboardType.MAP_MARKERS
				|| visibleType == DashboardType.MAP_MARKERS_SELECTION
				|| visibleType == DashboardType.CONFIGURE_SCREEN
				|| force) {
			updateListAdapter();
		} else if (visibleType == DashboardType.CONFIGURE_MAP
				|| visibleType == DashboardType.ROUTE_PREFERENCES) {
			int index = listView.getFirstVisiblePosition();
			View v = listView.getChildAt(0);
			int top = (v == null) ? 0 : (v.getTop() - listView.getPaddingTop());
			updateListAdapter();
			((ListView) listView).setSelectionFromTop(index, top);
		} else {
			listAdapter.notifyDataSetChanged();
		}
	}

	private void setDynamicListItems(DynamicListView listView, StableArrayAdapter listAdapter) {
		listView.setItemsList(listAdapter.getObjects());

		if (DashboardType.WAYPOINTS == visibleType || DashboardType.WAYPOINTS_FLAT == visibleType) {
			listView.setActiveItemsList(listAdapter.getActiveObjects());
		} else if (DashboardType.MAP_MARKERS == visibleType || visibleType == DashboardType.MAP_MARKERS_SELECTION) {
			List<Object> activeMarkers = new ArrayList<>();
			for (Object obj : listAdapter.getActiveObjects()) {
				if (obj instanceof MapMarker && !((MapMarker) obj).history) {
					activeMarkers.add(obj);
				}
			}
			listView.setActiveItemsList(activeMarkers);
		}
	}

	private OnItemClickListener getOptionsMenuOnClickListener(final ContextMenuAdapter cm,
															  final ArrayAdapter<ContextMenuItem> listAdapter) {
		return new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int which, long id) {
				ContextMenuItem item = cm.getItem(which);
				ContextMenuAdapter.ItemClickListener click = item.getItemClickListener();
				if (click instanceof OnRowItemClick) {
					boolean cl = ((OnRowItemClick) click).onRowItemClick(listAdapter, view, item.getTitleId(), which);
					if (cl) {
						hideDashboard();
					}
				} else if (click != null) {
					CompoundButton btn = (CompoundButton) view.findViewById(R.id.toggle_item);
					if (btn != null && btn.getVisibility() == View.VISIBLE) {
						btn.setChecked(!btn.isChecked());
					} else {
						if (click.onContextMenuClick(listAdapter, item.getTitleId(), which, false)) {
							hideDashboard();
						}
					}
				} else {
					if (!item.isCategory()) {
						hideDashboard();
					}
				}
			}
		};
	}

	private void updateDownloadBtn() {
		Button btn = (Button) dashboardView.findViewById(R.id.map_download_button);
		String filter = null;
		String txt = "";
		OsmandMapTileView mv = mapActivity.getMapView();
		if (mv != null && !mapActivity.getMyApplication().isApplicationInitializing()) {
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
		final String f = filter;
		btn.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				hideDashboard(false);
				final Intent intent = new Intent(mapActivity, mapActivity.getMyApplication().getAppCustomization()
						.getDownloadIndexActivity());
				if (f != null && !f.equals("basemap")) {
					intent.putExtra(DownloadActivity.FILTER_KEY, f);
				}
				intent.putExtra(DownloadActivity.TAB_TO_OPEN, DownloadActivity.DOWNLOAD_TAB);
				mapActivity.startActivity(intent);
			}
		});
		scheduleDownloadButtonCheck();
	}

	private void scheduleDownloadButtonCheck() {
		mapActivity.getMyApplication().runInUIThread(new Runnable() {

			@Override
			public void run() {
				if (isVisible()) {
					updateDownloadBtn();
				}
			}
		}, 4000);
	}


	public void navigationAction() {
		RoutingHelper routingHelper = mapActivity.getRoutingHelper();
		if (!routingHelper.isFollowingMode() && !routingHelper.isRoutePlanningMode()) {
			mapActivity.getMapActions().enterRoutePlanningMode(null, null);
		} else {
			mapActivity.getRoutingHelper().setRoutePlanningMode(true);
			mapActivity.getMapViewTrackingUtilities().switchToRoutePlanningMode();
			mapActivity.refreshMap();
		}
		hideDashboard(true);
	}


	// To animate view slide out from right to left
	private void open(View view, boolean animation) {
		if (animation) {
			TranslateAnimation animate = new TranslateAnimation(-mapActivity.findViewById(R.id.MapHudButtonsOverlay)
					.getWidth(), 0, 0, 0);
			animate.setDuration(500);
			animate.setFillAfter(true);
			view.startAnimation(animate);
			view.setVisibility(View.VISIBLE);
		} else {
			view.setVisibility(View.VISIBLE);
		}
	}

	private void hide(View view, boolean animation) {
		if (compassButton != null) {
			mapActivity.getMapLayers().getMapControlsLayer().restoreCompassButton(nightMode);
			compassButton = null;
		}
		if (!animation) {
			dashboardView.setVisibility(View.GONE);
		} else {
			TranslateAnimation animate = new TranslateAnimation(0, -mapActivity.findViewById(R.id.MapHudButtonsOverlay)
					.getWidth(), 0, 0);
			animate.setDuration(500);
			animate.setFillAfter(true);
			animate.setAnimationListener(new AnimationListener() {

				@Override
				public void onAnimationStart(Animation animation) {

				}

				@Override
				public void onAnimationRepeat(Animation animation) {

				}

				@Override
				public void onAnimationEnd(Animation animation) {
					dashboardView.setVisibility(View.GONE);
				}
			});
			view.startAnimation(animate);
		}
		view.setVisibility(View.GONE);
	}


	private void addOrUpdateDashboardFragments() {
		OsmandSettings settings = getMyApplication().getSettings();
		TransactionBuilder builder =
				new TransactionBuilder(mapActivity.getSupportFragmentManager(), settings, mapActivity);
		builder.addFragmentsData(fragmentsData)
				.addFragmentsData(OsmandPlugin.getPluginsCardsList())
				.getFragmentTransaction().commit();
	}

	public boolean isVisible() {
		return visible;
	}

	public void onDetach(DashBaseFragment dashBaseFragment) {
		Iterator<WeakReference<DashBaseFragment>> it = fragList.iterator();
		while (it.hasNext()) {
			WeakReference<DashBaseFragment> wr = it.next();
			if (wr.get() == dashBaseFragment) {
				it.remove();
			}
		}
	}


	public void updateLocation(final boolean centerChanged, final boolean locationChanged,
							   final boolean compassChanged) {
		if (inLocationUpdate) {
			return;
		}
		inLocationUpdate = true;
		mapActivity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				inLocationUpdate = false;
				for (WeakReference<DashBaseFragment> df : fragList) {
					if (df.get() instanceof DashLocationFragment) {
						((DashLocationFragment) df.get()).updateLocation(centerChanged, locationChanged, compassChanged);
					}
				}
				if ((visibleType == DashboardType.MAP_MARKERS || visibleType == DashboardType.MAP_MARKERS_SELECTION)
						&& !listView.isDragging()
						&& System.currentTimeMillis() - lastUpOrCancelMotionEventTime > 1000) {
					mapMarkerDialogHelper.updateLocation(listView, compassChanged);
				}
			}
		});

	}

	public void updateMyLocation(net.osmand.Location location) {
		myLocation = location;
		updateLocation(false, true, false);
	}

	public void updateCompassValue(double heading) {
		this.heading = (float) heading;
		updateLocation(false, false, true);
	}

	public void onAttach(DashBaseFragment dashBaseFragment) {
		fragList.add(new WeakReference<>(dashBaseFragment));
	}

	public void requestLayout() {
		dashboardView.requestLayout();
	}


	public void onMenuPressed() {
		if (!isVisible()) {
			setDashboardVisibility(true, DashboardType.DASHBOARD);
		} else {
			hideDashboard();
		}
	}


	public boolean onBackPressed() {
		if (isVisible()) {
			backPressed();
			return true;
		}
		return false;
	}


	private void backPressed() {
		if (previousVisibleType != visibleType && previousVisibleType != null) {
			visibleType = null;
			setDashboardVisibility(true, previousVisibleType);
		} else {
			hideDashboard();
		}
	}


	@Override
	public void onScrollChanged(int scrollY, boolean firstScroll, boolean dragging) {
		// Translate list background
		if (portrait) {
			if (listBackgroundView != null) {
				setTranslationY(listBackgroundView, Math.max(0, -scrollY + mFlexibleSpaceImageHeight));
			}
		}
		if (portrait) {
			setTranslationY(toolbar, Math.min(0, -scrollY + mFlexibleSpaceImageHeight - mFlexibleBlurSpaceHeight));
		}
		updateColorOfToolbar(scrollY);
		updateTopButton(scrollY);
	}

	private boolean isActionButtonVisible() {
		return visibleType == DashboardType.DASHBOARD
				|| visibleType == DashboardType.WAYPOINTS
				|| visibleType == DashboardType.WAYPOINTS_FLAT
				|| visibleType == DashboardType.LIST_MENU
				|| visibleType == DashboardType.ROUTE_PREFERENCES
				|| visibleType == DashboardType.CONFIGURE_SCREEN
				|| (visibleType == DashboardType.MAP_MARKERS && mapMarkerDialogHelper.hasActiveMarkers())
				|| (visibleType == DashboardType.MAP_MARKERS_SELECTION && mapMarkerDialogHelper.hasActiveMarkers());
	}

	private boolean isBackButtonVisible() {
		return !(visibleType == DashboardType.DASHBOARD || visibleType == DashboardType.LIST_MENU);
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
			FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) compassButton.getLayoutParams();
			if (minTop > originalPosition - scrollY) {
				hideActionButton();
			} else {
				compassButton.setVisibility(View.VISIBLE);
				lp.topMargin = originalPosition - scrollY;
				((FrameLayout) compassButton.getParent()).updateViewLayout(compassButton, lp);
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
			if (t < 1) {
				//noinspection deprecation
				toolbar.setBackgroundDrawable(gradientToolbar);
			} else {
				toolbar.setBackgroundColor(0xff000000 | baseColor);
			}
		}
	}

	private void updateListAdapter(ArrayAdapter<?> listAdapter, OnItemClickListener listener) {
		this.listAdapter = listAdapter;
		this.listAdapterOnClickListener = listener;
		if (this.listView != null) {
			listView.setAdapter(listAdapter);
			if (!portrait) {
				listView.setOnItemClickListener(this.listAdapterOnClickListener);
			} else if (this.listAdapterOnClickListener != null) {
				listView.setOnItemClickListener(new OnItemClickListener() {

					@Override
					public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
						listAdapterOnClickListener.onItemClick(parent, view, position - 1, id);
					}
				});
			} else {
				listView.setOnItemClickListener(null);
			}
		}
	}

	private void setTranslationY(View v, int y) {
		ViewCompat.setTranslationY(v, y);
	}

	@SuppressLint("NewApi")
	private void setAlpha(View v, int alpha, int clr) {
//		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
//			v.setAlpha(alpha/255.f);
//		} else {
		int colr = (alpha << 24) | clr;
		v.setBackgroundColor(colr);
//		}
	}

	@Override
	public void onDownMotionEvent() {
	}


	@Override
	public void onUpOrCancelMotionEvent(ScrollState scrollState) {
		lastUpOrCancelMotionEventTime = System.currentTimeMillis();
	}


	public <T extends DashBaseFragment> T getFragmentByClass(Class<T> class1) {
		for (WeakReference<DashBaseFragment> f : fragList) {
			DashBaseFragment b = f.get();
			if (b != null && !b.isDetached() && class1.isInstance(b)) {
				//noinspection unchecked
				return (T) b;
			}
		}
		return null;
	}

	public void blacklistFragmentByTag(String tag) {
		hideFragmentByTag(tag);
		getMyApplication().getSettings().registerBooleanPreference(SHOULD_SHOW + tag, true)
				.makeGlobal().set(false);
	}

	public void hideFragmentByTag(String tag) {
		FragmentManager manager = mapActivity.getSupportFragmentManager();
		FragmentTransaction transaction = manager.beginTransaction();
		Fragment frag = manager.findFragmentByTag(tag);
		transaction.hide(frag).commit();
	}

	public void unblacklistFragmentClass(String tag) {
		unhideFragmentByTag(tag);
		getMyApplication().getSettings().registerBooleanPreference(SHOULD_SHOW + tag, true)
				.makeGlobal().set(true);
	}

	public void unhideFragmentByTag(String tag) {
		FragmentManager manager = mapActivity.getSupportFragmentManager();
		FragmentTransaction transaction = manager.beginTransaction();
		Fragment frag = manager.findFragmentByTag(tag);
		transaction.show(frag).commit();
	}

	public void clearDeletedPoints() {
		deletedPoints.clear();
	}

	View getParentView() {
		return dashboardView;
	}

	public static <T> List<T> handleNumberOfRows(List<T> list, OsmandSettings settings,
												 String rowNumberTag) {
		int numberOfRows = settings.registerIntPreference(rowNumberTag, 3)
				.makeGlobal().get();
		if (list.size() > numberOfRows) {
			while (list.size() != numberOfRows) {
				list.remove(numberOfRows);
			}
		}
		return list;
	}

	public static class DefaultShouldShow extends DashFragmentData.ShouldShowFunction {

		public boolean shouldShow(OsmandSettings settings, MapActivity activity, String tag) {
			return settings.registerBooleanPreference(SHOULD_SHOW + tag, true).makeGlobal().get();
		}
	}

	@Override
	public void onItemSwapping(int position) {
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onItemsSwapped(final List<Object> items) {
		getMyApplication().runInUIThread(new Runnable() {
			@Override
			public void run() {
				if (visibleType == DashboardType.WAYPOINTS || visibleType == DashboardType.WAYPOINTS_FLAT) {
					List<TargetPoint> allTargets = new ArrayList<>();
					if (items != null) {
						for (Object obj : items) {
							if (obj instanceof LocationPointWrapper) {
								LocationPointWrapper p = (LocationPointWrapper) obj;
								if (p.getPoint() instanceof TargetPoint) {
									TargetPoint t = (TargetPoint) p.getPoint();
									if (!t.start) {
										t.intermediate = true;
										allTargets.add(t);
									}
								}
							}
						}
						if (allTargets.size() > 0) {
							allTargets.get(allTargets.size() - 1).intermediate = false;
						}
					}
					getMyApplication().getTargetPointsHelper().reorderAllTargetPoints(allTargets, false);
					newRouteIsCalculated(false, new ValueHolder<Boolean>());
					getMyApplication().getTargetPointsHelper().updateRouteAndRefresh(true);

				} else if (visibleType == DashboardType.MAP_MARKERS || visibleType == DashboardType.MAP_MARKERS_SELECTION) {
					List<MapMarker> markers = (List<MapMarker>) (Object) items;
					getMyApplication().getMapMarkersHelper().saveMapMarkers(markers, null);
					reloadAdapter();
				}
			}
		}, 50);
	}

	@Override
	public void newRouteIsCalculated(boolean newRoute, ValueHolder<Boolean> showToast) {
		reloadAdapter();
		showToast.value = false;
	}

	@Override
	public void routeWasCancelled() {
	}

	@Override
	public void routeWasFinished() {
	}

	@Override
	public void onWindowVisibilityChanged(int visibility) {
		if (visibility != View.VISIBLE && swipeDismissListener != null) {
			swipeDismissListener.discardUndo();
		}
	}

	@Override
	public void reloadAdapter() {
		if (listAdapter != null && listAdapter instanceof StableArrayAdapter) {
			StableArrayAdapter stableAdapter = (StableArrayAdapter) listAdapter;
			if (DashboardType.WAYPOINTS == visibleType || DashboardType.WAYPOINTS_FLAT == visibleType) {
				waypointDialogHelper.reloadListAdapter(stableAdapter);
			} else if (DashboardType.MAP_MARKERS == visibleType || visibleType == DashboardType.MAP_MARKERS_SELECTION) {
				mapMarkerDialogHelper.reloadListAdapter(stableAdapter);
				if (visibleType == DashboardType.MAP_MARKERS_SELECTION) {
					showMarkersRouteOnMap();
				}
			}
			setDynamicListItems(listView, stableAdapter);
		}
	}

	private void deleteSwipeItem(int position) {
		if (swipeDismissListener != null) {
			swipeDismissListener.delete(position);
		}
	}

	@Override
	public void deleteWaypoint(int position) {
		deleteSwipeItem(position);
	}

	@Override
	public void exchangeWaypoints(int pos1, int pos2) {
		if (swipeDismissListener != null) {
			swipeDismissListener.discardUndo();
		}
		if (pos1 != -1 && pos2 != -1) {
			StableArrayAdapter stableAdapter = (StableArrayAdapter) listAdapter;
			Object item1 = stableAdapter.getActiveObjects().get(pos1);
			Object item2 = stableAdapter.getActiveObjects().get(pos2);
			stableAdapter.getActiveObjects().set(pos1, item2);
			stableAdapter.getActiveObjects().set(pos2, item1);

			stableAdapter.refreshData();
			onItemsSwapped(stableAdapter.getActiveObjects());
		}
	}

	@Override
	public void deleteMapMarker(int position) {
		deleteSwipeItem(position);
	}

	@Override
	public void showMarkersRouteOnMap() {
		MapMarkersHelper helper = getMyApplication().getMapMarkersHelper();
		List<LatLon> points = helper.getSelectedMarkersLatLon();
		mapActivity.getMapLayers().getMapMarkersLayer().setRoute(points);
		showRouteOnMap(points);
	}

	public void showRouteOnMap(List<LatLon> points) {
		if (points.size() > 0 && mapActivity != null) {
			OsmandMapTileView mapView = mapActivity.getMapView();
			double left = 0, right = 0;
			double top = 0, bottom = 0;
			if (getMyApplication().getMapMarkersHelper().isStartFromMyLocation() && myLocation != null) {
				left = myLocation.getLongitude();
				right = myLocation.getLongitude();
				top = myLocation.getLatitude();
				bottom = myLocation.getLatitude();
			}
			for (LatLon l : points) {
				if (left == 0) {
					left = l.getLongitude();
					right = l.getLongitude();
					top = l.getLatitude();
					bottom = l.getLatitude();
				} else {
					left = Math.min(left, l.getLongitude());
					right = Math.max(right, l.getLongitude());
					top = Math.max(top, l.getLatitude());
					bottom = Math.min(bottom, l.getLatitude());
				}
			}

			RotatedTileBox tb = mapView.getCurrentRotatedTileBox().copy();
			int tileBoxWidthPx = 0;
			int tileBoxHeightPx = 0;

			if (landscape) {
				tileBoxWidthPx = tb.getPixWidth() - dashboardView.getWidth();
			} else if (listBackgroundView != null) {
				tileBoxHeightPx = 3 * (mFlexibleSpaceImageHeight - mFlexibleBlurSpaceHeight) / 4;
			}
			mapView.fitRectToMap(left, right, top, bottom, tileBoxWidthPx, tileBoxHeightPx, mFlexibleBlurSpaceHeight * 3 / 2);
		}
	}
}
