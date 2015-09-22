package net.osmand.plus.dashboard;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.Toolbar;
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
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.views.DownloadedRegionsLayer;
import net.osmand.plus.views.OsmandMapTileView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 */
public class DashboardOnMap implements ObservableScrollViewCallbacks {
	private static final org.apache.commons.logging.Log LOG =
			PlatformUtil.getLog(DashboardOnMap.class);
	private static final String TAG = "DashboardOnMap";
	public static boolean staticVisible = false;
	public static DashboardType staticVisibleType = DashboardType.DASHBOARD;
	public static final String SHOULD_SHOW = "should_show";

	private static final DashFragmentData.ShouldShowFunction rateUsShouldShow = new DashRateUsFragment.RateUsShouldShow();
	private static final DefaultShouldShow defaultShouldShow = new DefaultShouldShow();
	private static final DashFragmentData.ShouldShowFunction errorShouldShow = new ErrorShouldShow();
	private static final DashFragmentData.ShouldShowFunction firstTimeShouldShow = new FirstTimeShouldShow();
	private static final DashFragmentData.ShouldShowFunction chooseAppDirShouldShow = new ChooseAppDirShouldShow();

	private static final DashFragmentData[] fragmentsData = new DashFragmentData[]{
			new DashFragmentData(DashRateUsFragment.TAG, DashRateUsFragment.class,
					-1, rateUsShouldShow, true, 0),
			new DashFragmentData(DashFirstTimeFragment.TAG, DashFirstTimeFragment.class,
					-1, firstTimeShouldShow, true, 1),
			new DashFragmentData(DashChooseAppDirFragment.TAG, DashChooseAppDirFragment.class,
					-1, chooseAppDirShouldShow, true, 2),
			new DashFragmentData(DashErrorFragment.TAG, DashErrorFragment.class,
					-1, errorShouldShow, true, 3),
			new DashFragmentData(DashNavigationFragment.TAG, DashNavigationFragment.class,
					R.string.tip_navigation, 4),
			new DashFragmentData(DashWaypointsFragment.TAG, DashWaypointsFragment.class,
					R.string.waypoints, 6),
			new DashFragmentData(DashSearchFragment.TAG, DashSearchFragment.class,
					R.string.shared_string_search, 7),
			new DashFragmentData(DashRecentsFragment.TAG, DashRecentsFragment.class,
					R.string.recent_places, 8),
			new DashFragmentData(DashFavoritesFragment.TAG, DashFavoritesFragment.class,
					R.string.favourites, defaultShouldShow, false, 9, DashFavoritesFragment.ROW_NUMBER_TAG),
			new DashFragmentData(DashPluginsFragment.TAG, DashPluginsFragment.class,
					R.string.plugin_settings, 14)
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
	private List<LocationPointWrapper> deletedPoints = new ArrayList<LocationPointWrapper>();
	private Drawable gradientToolbar;

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
		DASHBOARD
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

		initActionButton();
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


	private void initActionButton() {
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
		actionButton.setImageDrawable(mapActivity.getResources().getDrawable(R.drawable.map_my_location));

		actionButton.setBackgroundDrawable(mapActivity.getResources().getDrawable(R.drawable.btn_circle_blue));
		hideActionButton();
		actionButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (getMyApplication().accessibilityEnabled()) {
					mapActivity.getMapActions().whereAmIDialog();
				} else {
					mapActivity.getMapViewTrackingUtilities().backToLocationImpl();
				}
				hideDashboard();
			}
		});
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

	private void updateListAdapter() {
		ContextMenuAdapter cm = null;
		if (DashboardType.WAYPOINTS == visibleType || DashboardType.WAYPOINTS_FLAT == visibleType) {
			ArrayAdapter<Object> listAdapter = waypointDialogHelper.getWaypointsDrawerAdapter(false, deletedPoints, mapActivity, running,
					DashboardType.WAYPOINTS_FLAT == visibleType);
			OnItemClickListener listener = waypointDialogHelper.getDrawerItemClickListener(mapActivity, running,
					listAdapter);
			updateListAdapter(listAdapter, listener);
		} else if (DashboardType.WAYPOINTS_EDIT == visibleType) {
			deletedPoints.clear();
			ArrayAdapter<Object> listAdapter = waypointDialogHelper.getWaypointsDrawerAdapter(true, deletedPoints, mapActivity, running,
					DashboardType.WAYPOINTS_FLAT == visibleType);
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
			}
			if (cm != null) {
				updateListAdapter(cm);
			}
		}
	}

	public void updateListAdapter(ContextMenuAdapter cm) {
		final ArrayAdapter<?> listAdapter = cm.createListAdapter(mapActivity, getMyApplication().getSettings()
				.isLightContent());
		OnItemClickListener listener = getOptionsMenuOnClickListener(cm, listAdapter);
		updateListAdapter(listAdapter, listener);
	}

	public void refreshContent(boolean force) {
		if (visibleType == DashboardType.WAYPOINTS || force) {
			updateListAdapter();
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
				intent.putExtra(DownloadActivity.FILTER_KEY, f);
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
			mapActivity.getMapActions().enterRoutePlanningMode(null, null, false);
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
		return visibleType == DashboardType.DASHBOARD || visibleType == DashboardType.LIST_MENU || visibleType == DashboardType.CONFIGURE_SCREEN;
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
			int malpha = t == 1 ? alpha = 0 : alpha;
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
		int colr = (((int) alpha) << 24) | clr;
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

	public boolean hasCriticalMessages() {
		final OsmandSettings settings = getMyApplication().getSettings();
		return rateUsShouldShow.shouldShow(settings, mapActivity, DashRateUsFragment.TAG)
				|| errorShouldShow.shouldShow(null, mapActivity, null);
	}

	View getParentView() {
		return dashboardView;
	}

	public static class SettingsShouldShow implements DashFragmentData.ShouldShowFunction {
		@Override
		public boolean shouldShow(OsmandSettings settings, MapActivity activity, String tag) {
			return settings.registerBooleanPreference(SHOULD_SHOW + tag, true)
					.makeGlobal().get();
		}
	}

	public static class DefaultShouldShow extends SettingsShouldShow {
		@Override
		public boolean shouldShow(OsmandSettings settings, MapActivity activity, String tag) {
			return !activity.getMyApplication().getAppInitializer().isFirstTime(activity)
					&& super.shouldShow(settings, activity, tag);
		}
	}

	private static class ErrorShouldShow implements DashFragmentData.ShouldShowFunction {
		// If settings null. No changes in setting will be made.
		@Override
		public boolean shouldShow(OsmandSettings settings, MapActivity activity, String tag) {
			return activity.getMyApplication().getAppInitializer()
					.checkPreviousRunsForExceptions(activity, settings != null);
		}
	}

	private static class FirstTimeShouldShow extends SettingsShouldShow {
		@Override
		public boolean shouldShow(OsmandSettings settings, MapActivity activity, String tag) {
			return activity.getMyApplication().getAppInitializer().isFirstTime(activity)
					&& super.shouldShow(settings, activity, tag);
		}
	}

	private static class ChooseAppDirShouldShow extends SettingsShouldShow {
		public boolean shouldShow(OsmandSettings settings, MapActivity activity, String tag) {
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
				return false;
			}
			return !settings.isExternalStorageDirectorySpecifiedV19()
					&& super.shouldShow(settings, activity, tag);
		}
	}
}
