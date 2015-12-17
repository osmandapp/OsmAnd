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
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
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

import com.github.ksoichiro.android.observablescrollview.ObservableListView;
import com.github.ksoichiro.android.observablescrollview.ObservableScrollView;
import com.github.ksoichiro.android.observablescrollview.ObservableScrollViewCallbacks;
import com.github.ksoichiro.android.observablescrollview.ScrollState;

import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuAdapter.OnContextMenuClick;
import net.osmand.plus.ContextMenuAdapter.OnRowItemClick;
import net.osmand.plus.IconsCache;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.IntermediatePointsDialog;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dashboard.tools.DashFragmentData;
import net.osmand.plus.dashboard.tools.DashboardSettingsDialogFragment;
import net.osmand.plus.dashboard.tools.TransactionBuilder;
import net.osmand.plus.dialogs.ConfigureMapMenu;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.WaypointDialogHelper;
import net.osmand.plus.helpers.WaypointHelper.LocationPointWrapper;
import net.osmand.plus.mapcontextmenu.other.RoutePreferencesMenu;
import net.osmand.plus.mapcontextmenu.other.RoutePreferencesMenu.LocalRoutingParameter;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.views.DownloadedRegionsLayer;
import net.osmand.plus.views.OsmandMapTileView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static android.util.TypedValue.COMPLEX_UNIT_DIP;

/**
 */
public class DashboardOnMap implements ObservableScrollViewCallbacks {
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
			new DashFragmentData(DashNavigationFragment.TAG, DashNavigationFragment.class,
					DashNavigationFragment.SHOULD_SHOW_FUNCTION, 40, null),
			new DashFragmentData(DashWaypointsFragment.TAG, DashWaypointsFragment.class,
					DashWaypointsFragment.SHOULD_SHOW_FUNCTION, 60, null),
			new DashFragmentData(DashSearchFragment.TAG, DashSearchFragment.class,
					DashSearchFragment.SHOULD_SHOW_FUNCTION, 70, null),
			DashRecentsFragment.FRAGMENT_DATA,
			DashFavoritesFragment.FRAGMENT_DATA,
			new DashFragmentData(DashPluginsFragment.TAG, DashPluginsFragment.class,
					DashPluginsFragment.SHOULD_SHOW_FUNCTION, 140, null)
	};

	private MapActivity mapActivity;
	private ImageView actionButton;
	private FrameLayout dashboardView;

	private ArrayAdapter<?> listAdapter;
	private OnItemClickListener listAdapterOnClickListener;


	private boolean visible = false;
	private DashboardType visibleType;
	private DashboardType previousVisibleType;
	private boolean landscape;
	private List<WeakReference<DashBaseFragment>> fragList = new LinkedList<>();
	private net.osmand.Location myLocation;
	private LatLon mapViewLocation;
	private float heading;
	private boolean mapLinkedToLocation;
	private float mapRotation;
	private boolean inLocationUpdate = false;
	private ListView listView;
	private View listBackgroundView;
	private Toolbar toolbar;
	private View paddingView;
	private int mFlexibleSpaceImageHeight;
	private int mFlexibleBlurSpaceHeight;
	private boolean portrait;

	int baseColor;

	private WaypointDialogHelper waypointDialogHelper;
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
		WAYPOINTS_EDIT,
		CONFIGURE_SCREEN,
		CONFIGURE_MAP,
		LIST_MENU,
		ROUTE_PREFERENCES,
		DASHBOARD
	}

	private Map<DashboardActionButtonType, DashboardActionButton> actionButtons = new HashMap<>();

	public enum DashboardActionButtonType {
		MY_LOCATION,
		NAVIGATE
	}

	private class DashboardActionButton {
		private Drawable icon;
		private View.OnClickListener onClickListener;
	}

	public DashboardOnMap(MapActivity ma) {
		this.mapActivity = ma;
	}


	public void createDashboardView() {
		baseColor = mapActivity.getResources().getColor(R.color.osmand_orange) & 0x00ffffff;
		waypointDialogHelper = new WaypointDialogHelper(mapActivity);
		landscape = !AndroidUiHelper.isOrientationPortrait(mapActivity);
		dashboardView = (FrameLayout) mapActivity.findViewById(R.id.dashboard);
		View.OnClickListener listener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				hideDashboard();
			}
		};
		toolbar = ((Toolbar) dashboardView.findViewById(R.id.toolbar));
		ObservableScrollView scrollView = ((ObservableScrollView) dashboardView.findViewById(R.id.main_scroll));
		listView = (ListView) dashboardView.findViewById(R.id.dash_list_view);
		gradientToolbar = mapActivity.getResources().getDrawable(R.drawable.gradient_toolbar).mutate();
		if (AndroidUiHelper.isOrientationPortrait(mapActivity)) {
			this.portrait = true;
			scrollView.setScrollViewCallbacks(this);
			((ObservableListView) listView).setScrollViewCallbacks(this);
			mFlexibleSpaceImageHeight = mapActivity.getResources().getDimensionPixelSize(
					R.dimen.dashboard_map_top_padding);
			mFlexibleBlurSpaceHeight = mapActivity.getResources().getDimensionPixelSize(
					R.dimen.dashboard_map_toolbar);
			// Set padding view for ListView. This is the flexible space.
			paddingView = new View(mapActivity);
			AbsListView.LayoutParams lp = new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT,
					mFlexibleSpaceImageHeight);
			paddingView.setLayoutParams(lp);
			// This is required to disable header's list selector effect
			paddingView.setClickable(true);
			paddingView.setOnClickListener(listener);
			listView.addHeaderView(paddingView);
			listBackgroundView = mapActivity.findViewById(R.id.dash_list_background);
		}
		dashboardView.findViewById(R.id.animateContent).setOnClickListener(listener);
		dashboardView.findViewById(R.id.map_part_dashboard).setOnClickListener(listener);

		initActionButtons();
		dashboardView.addView(actionButton);
	}


	private void updateListBackgroundHeight() {

		if (listBackgroundView == null || listBackgroundView.getHeight() > 0) {
			return;
		}
		final View contentView = mapActivity.getWindow().getDecorView().findViewById(android.R.id.content);
		if (contentView.getHeight() > 0) {
			listBackgroundView.getLayoutParams().height = contentView.getHeight();
		} else {
			contentView.post(new Runnable() {
				@Override
				public void run() {
					// mListBackgroundView's should fill its parent vertically
					// but the height of the content view is 0 on 'onCreate'.
					// So we should get it with post().
					listBackgroundView.getLayoutParams().height = contentView.getHeight();
				}
			});
		}
	}

	private void updateToolbarActions() {
		TextView tv = (TextView) dashboardView.findViewById(R.id.toolbar_text);
		tv.setText("");
		boolean waypointsVisible = visibleType == DashboardType.WAYPOINTS || visibleType == DashboardType.WAYPOINTS_FLAT;
		boolean waypointsEdit = visibleType == DashboardType.WAYPOINTS_EDIT;
		if (waypointsVisible || waypointsEdit) {
			tv.setText(R.string.waypoints);
		} else if (visibleType == DashboardType.CONFIGURE_MAP) {
			tv.setText(R.string.configure_map);
		} else if (visibleType == DashboardType.CONFIGURE_SCREEN) {
			tv.setText(R.string.layer_map_appearance);
		} else if (visibleType == DashboardType.ROUTE_PREFERENCES) {
			tv.setText(R.string.shared_string_settings);
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
			if (mapActivity.getMyApplication().getTargetPointsHelper().getIntermediatePoints().size() > 0) {
				sort.setVisibility(View.VISIBLE);
				sort.setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						hideDashboard();
						IntermediatePointsDialog.openIntermediatePointsDialog(mapActivity, getMyApplication(), true);
					}
				});
			}
			edit.setVisibility(View.VISIBLE);
			edit.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					setDashboardVisibility(true, DashboardType.WAYPOINTS_EDIT);
				}
			});
			if (getMyApplication().getWaypointHelper().isRouteCalculated()) {
				flat.setVisibility(View.VISIBLE);
				final boolean flatNow = visibleType == DashboardType.WAYPOINTS_FLAT;
				flat.setImageDrawable(iconsCache.getIcon(flatNow ? R.drawable.ic_tree_list_dark
						: R.drawable.ic_flat_list_dark));
				flat.setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						setDashboardVisibility(true, flatNow ? DashboardType.WAYPOINTS : DashboardType.WAYPOINTS_FLAT,
								previousVisibleType, false);
					}
				});
			}
		}
		if (waypointsEdit) {
			ok.setVisibility(View.VISIBLE);
			ok.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					mapActivity.getMyApplication().getWaypointHelper().removeVisibleLocationPoint(deletedPoints);
					hideDashboard();
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
	}


	private void initActionButtons() {
		actionButton = new ImageView(mapActivity);
		int btnSize = (int) mapActivity.getResources().getDimension(R.dimen.map_button_size);
		int topPad = (int) mapActivity.getResources().getDimension(R.dimen.dashboard_map_top_padding);
		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
				btnSize, btnSize
		);
		int marginRight = btnSize / 4;
		params.setMargins(0, landscape ? 0 : topPad - 2 * btnSize,
				marginRight, landscape ? marginRight : 0);
		params.gravity = landscape ? Gravity.BOTTOM | Gravity.RIGHT : Gravity.TOP | Gravity.RIGHT;
		actionButton.setLayoutParams(params);
		actionButton.setScaleType(ScaleType.CENTER);
		actionButton.setBackgroundDrawable(mapActivity.getResources().getDrawable(R.drawable.btn_circle_blue));
		hideActionButton();


		DashboardActionButton myLocationButton = new DashboardActionButton();
		myLocationButton.icon = mapActivity.getResources().getDrawable(R.drawable.map_my_location);
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
		navigateButton.icon = mapActivity.getResources().getDrawable(R.drawable.map_start_navigation);
		navigateButton.onClickListener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mapActivity.getMapLayers().getMapControlsLayer().doNavigate();
				hideDashboard();
			}
		};

		actionButtons.put(DashboardActionButtonType.MY_LOCATION, myLocationButton);
		actionButtons.put(DashboardActionButtonType.NAVIGATE, navigateButton);
	}

	private void setActionButton(DashboardType type) {
		DashboardActionButton button = null;

		if (type == DashboardType.DASHBOARD
				|| type == DashboardType.LIST_MENU
				|| type == DashboardType.CONFIGURE_SCREEN) {
			button = actionButtons.get(DashboardActionButtonType.MY_LOCATION);
		} else if (type == DashboardType.ROUTE_PREFERENCES) {
			button = actionButtons.get(DashboardActionButtonType.NAVIGATE);
		}

		if (button != null) {
			actionButton.setImageDrawable(button.icon);
			actionButton.setOnClickListener(button.onClickListener);
		}
	}

	private void hideActionButton() {
		actionButton.setVisibility(View.GONE);
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
		nightMode = mapActivity.getMyApplication().getDaynightHelper().isNightModeForMapControls();
		this.previousVisibleType = prevItem;
		this.visible = visible;
		boolean refresh = this.visibleType == type;
		this.visibleType = type;
		DashboardOnMap.staticVisible = visible;
		DashboardOnMap.staticVisibleType = type;
		mapActivity.enableDrawer();
		if (visible) {
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
				if (listView instanceof ObservableListView) {
					onScrollChanged(listView.getScrollY(), false, false);
				}
				if (refresh) {
					refreshContent(false);
				} else {
					updateListAdapter();
					updateListBackgroundHeight();
				}
				applyDayNightMode();
			}
			mapActivity.findViewById(R.id.toolbar_back).setVisibility(isBackButtonVisible() ? View.VISIBLE : View.GONE);
			mapActivity.findViewById(R.id.MapHudButtonsOverlay).setVisibility(View.INVISIBLE);

			updateToolbarActions();
			//fabButton.showFloatingActionButton();
			open(dashboardView.findViewById(R.id.animateContent), animation);
			updateLocation(true, true, false);
//			addOrUpdateDashboardFragments();
		} else {
			mapActivity.getMapViewTrackingUtilities().setDashboard(null);
			hide(dashboardView.findViewById(R.id.animateContent), animation);
			mapActivity.findViewById(R.id.MapHudButtonsOverlay).setVisibility(View.VISIBLE);
			hideActionButton();
			for (WeakReference<DashBaseFragment> df : fragList) {
				if (df.get() != null) {
					df.get().onCloseDash();
				}
			}
		}
	}

	private void applyDayNightMode() {
		if (nightMode) {
			if (listBackgroundView != null) {
				listBackgroundView.setBackgroundColor(mapActivity.getResources().getColor(R.color.bg_color_dark));
			} else {
				listView.setBackgroundColor(mapActivity.getResources().getColor(R.color.bg_color_dark));
			}
			Drawable d = new ColorDrawable(mapActivity.getResources().getColor(R.color.dashboard_divider_dark));
			listView.setDivider(d);
			listView.setDividerHeight(dpToPx(1f));
		} else {
			if (listBackgroundView != null) {
				listBackgroundView.setBackgroundColor(mapActivity.getResources().getColor(R.color.bg_color_light));
			} else {
				listView.setBackgroundColor(mapActivity.getResources().getColor(R.color.bg_color_light));
			}
			Drawable d = new ColorDrawable(mapActivity.getResources().getColor(R.color.dashboard_divider_light));
			listView.setDivider(d);
			listView.setDividerHeight(dpToPx(1f));
		}
	}

	private int dpToPx(float dp) {
		Resources r = mapActivity.getResources();
		return (int) TypedValue.applyDimension(
				COMPLEX_UNIT_DIP,
				dp,
				r.getDisplayMetrics()
		);
	}

	private void updateListAdapter() {
		ContextMenuAdapter cm = null;
		if (DashboardType.WAYPOINTS == visibleType || DashboardType.WAYPOINTS_FLAT == visibleType) {
			ArrayAdapter<Object> listAdapter = waypointDialogHelper.getWaypointsDrawerAdapter(false, deletedPoints, mapActivity, running,
					DashboardType.WAYPOINTS_FLAT == visibleType, nightMode);
			OnItemClickListener listener = waypointDialogHelper.getDrawerItemClickListener(mapActivity, running,
					listAdapter);
			updateListAdapter(listAdapter, listener);
		} else if (DashboardType.WAYPOINTS_EDIT == visibleType) {
			deletedPoints.clear();
			ArrayAdapter<Object> listAdapter = waypointDialogHelper.getWaypointsDrawerAdapter(true, deletedPoints, mapActivity, running,
					DashboardType.WAYPOINTS_FLAT == visibleType, nightMode);
			OnItemClickListener listener = waypointDialogHelper.getDrawerItemClickListener(mapActivity, running,
					listAdapter);
			updateListAdapter(listAdapter, listener);

		} else {
			if (DashboardType.CONFIGURE_SCREEN == visibleType) {
				cm = mapActivity.getMapLayers().getMapWidgetRegistry().getViewConfigureMenuAdapter(mapActivity);
			} else if (DashboardType.CONFIGURE_MAP == visibleType) {
				cm = new ConfigureMapMenu().createListAdapter(mapActivity);
			} else if (DashboardType.LIST_MENU == visibleType) {
				cm = mapActivity.getMapActions().createMainOptionsMenu();
			} else if (DashboardType.ROUTE_PREFERENCES == visibleType) {
				RoutePreferencesMenu routePreferencesMenu = new RoutePreferencesMenu(mapActivity);
				ArrayAdapter<LocalRoutingParameter> listAdapter = routePreferencesMenu.getRoutePreferencesDrawerAdapter(nightMode);
				OnItemClickListener listener = routePreferencesMenu.getItemClickListener(listAdapter);
				updateListAdapter(listAdapter, listener);
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
		final ArrayAdapter<?> listAdapter = cm.createListAdapter(mapActivity, !nightMode);
		OnItemClickListener listener = getOptionsMenuOnClickListener(cm, listAdapter);
		updateListAdapter(listAdapter, listener);
	}

	public void refreshContent(boolean force) {
		if (visibleType == DashboardType.WAYPOINTS || visibleType == DashboardType.WAYPOINTS_EDIT
				|| force) {
			updateListAdapter();
		} else if (visibleType == DashboardType.CONFIGURE_MAP || visibleType == DashboardType.ROUTE_PREFERENCES) {
			int index = listView.getFirstVisiblePosition();
			View v = listView.getChildAt(0);
			int top = (v == null) ? 0 : (v.getTop() - listView.getPaddingTop());
			updateListAdapter();
			listView.setSelectionFromTop(index, top);
		} else {
			listAdapter.notifyDataSetChanged();
		}
	}


	private OnItemClickListener getOptionsMenuOnClickListener(final ContextMenuAdapter cm,
															  final ArrayAdapter<?> listAdapter) {
		return new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int which, long id) {
				OnContextMenuClick click = cm.getClickAdapter(which);
				if (click instanceof OnRowItemClick) {
					boolean cl = ((OnRowItemClick) click).onRowItemClick(listAdapter, view, cm.getElementId(which), which);
					if (cl) {
						hideDashboard();
					}
				} else if (click != null) {
					CompoundButton btn = (CompoundButton) view.findViewById(R.id.check_item);
					if (btn != null && btn.getVisibility() == View.VISIBLE) {
						btn.setChecked(!btn.isChecked());
					} else {
						if (click.onContextMenuClick(listAdapter, cm.getElementId(which), which, false)) {
							hideDashboard();
						}
					}
				} else {
					hideDashboard();
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
				|| visibleType == DashboardType.LIST_MENU
				|| visibleType == DashboardType.ROUTE_PREFERENCES
				|| visibleType == DashboardType.CONFIGURE_SCREEN;
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
			if (t < 1) {
				dashboardView.findViewById(R.id.toolbar).setBackgroundDrawable(gradientToolbar);
			} else {
				dashboardView.findViewById(R.id.toolbar).setBackgroundColor(0xff000000 | baseColor);
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

	@SuppressLint("NewApi")
	private void setTranslationY(View v, int y) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			v.setTranslationY(y);
		} else {
			TranslateAnimation anim = new TranslateAnimation(0, 0, y, y);
			anim.setFillAfter(true);
			anim.setDuration(0);
			v.startAnimation(anim);
		}
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
//		 ActionBar ab = getSupportActionBar();
//	        if (scrollState == ScrollState.UP) {
//	            if (ab.isShowing()) {
//	                ab.hide();
//	            }
//	        } else if (scrollState == ScrollState.DOWN) {
//	            if (!ab.isShowing()) {
//	                ab.show();
//	            }
//	        }		
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
}
