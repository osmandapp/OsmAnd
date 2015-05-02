package net.osmand.plus.dashboard;


import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.osmand.data.LatLon;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuAdapter.OnContextMenuClick;
import net.osmand.plus.ContextMenuAdapter.OnRowItemClick;
import net.osmand.plus.IconsCache;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.IntermediatePointsDialog;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.audionotes.DashAudioVideoNotesFragment;
import net.osmand.plus.development.DashSimulateFragment;
import net.osmand.plus.development.OsmandDevelopmentPlugin;
import net.osmand.plus.dialogs.ConfigureMapMenu;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.WaypointDialogHelper;
import net.osmand.plus.helpers.WaypointHelper.LocationPointWrapper;
import net.osmand.plus.monitoring.DashTrackFragment;
import net.osmand.plus.osmedit.DashOsmEditsFragment;
import net.osmand.plus.osmo.DashOsMoFragment;
import net.osmand.plus.parkingpoint.DashParkingFragment;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.views.DownloadedRegionsLayer;
import net.osmand.plus.views.OsmandMapTileView;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
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

/**
 */
public class DashboardOnMap implements ObservableScrollViewCallbacks {

	public static boolean staticVisible = false;
	public static DashboardType staticVisibleType = DashboardType.DASHBOARD;
	
	private MapActivity mapActivity;
	private ImageView actionButton;
	private FrameLayout dashboardView;
	
	private ArrayAdapter<?> listAdapter;
	private OnItemClickListener listAdapterOnClickListener;

	
	private boolean visible = false;
	private DashboardType visibleType;
	private DashboardType previousVisibleType;
	private boolean landscape;
	private List<WeakReference<DashBaseFragment>> fragList = new LinkedList<WeakReference<DashBaseFragment>>();
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
	private final int[] running = new int[] { -1 };
	private List<LocationPointWrapper> deletedPoints = new ArrayList<LocationPointWrapper>();
	private Drawable gradientToolbar;
	
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
		baseColor =  mapActivity.getResources().getColor(R.color.osmand_orange) & 0x00ffffff;
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
		if (waypointsVisible || waypointsEdit){
			tv.setText(R.string.waypoints);
		} else if(visibleType == DashboardType.CONFIGURE_MAP){
			tv.setText(R.string.configure_map);
		} else if(visibleType == DashboardType.CONFIGURE_SCREEN){
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
		ImageView settings = (ImageView) dashboardView.findViewById(R.id.toolbar_settings);
		settings.setVisibility(View.GONE);
		IconsCache iconsCache = mapActivity.getMyApplication().getIconsCache();
		ImageView lst = (ImageView) dashboardView.findViewById(R.id.toolbar_list);
		lst.setVisibility(View.GONE);
		ImageView back = (ImageView) dashboardView.findViewById(R.id.toolbar_back);
		back.setImageDrawable(
				((OsmandApplication)getMyApplication()).getIconsCache().getIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha));
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
		if(waypointsEdit) {
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
			settings.setVisibility(View.VISIBLE);
			settings.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					Class<? extends Activity> sta = mapActivity.getMyApplication().getAppCustomization()
							.getSettingsActivity();
					hideDashboard(false);
					mapActivity.startActivity(new Intent(mapActivity, sta));
				}
			});
			lst.setVisibility(View.VISIBLE);
			if (visibleType == DashboardType.DASHBOARD) {
				lst.setImageDrawable(iconsCache.getIcon(R.drawable.ic_navigation_drawer));
			} else if (visibleType == DashboardType.LIST_MENU) {
				lst.setImageDrawable(iconsCache.getIcon(R.drawable.ic_dashboard_dark));
			}
			lst.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (visibleType == DashboardType.DASHBOARD) {
						setDashboardVisibility(true, DashboardType.LIST_MENU, null, true);
					} else {
						setDashboardVisibility(true, DashboardType.DASHBOARD, null, true);
					}
				}
			});
		}
	}


	private void initActionButton() {
		actionButton = new ImageView(mapActivity);
		int btnSize = (int) mapActivity.getResources().getDimension(R.dimen.map_button_size);
		int topPad = (int) mapActivity.getResources().getDimension(R.dimen.dashboard_map_top_padding);
		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
				btnSize,btnSize
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


	public static int convertPixelsToDp(float dp, Context context){
		DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
		return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
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

	public void refreshDashboardFragments(){
		addOrUpdateDashboardFragments();
	}

	public void setDashboardVisibility(boolean visible, DashboardType type, DashboardType prevItem, boolean animation) {
		if(visible == this.visible && type == visibleType) {
			return;
		}
		this.previousVisibleType = prevItem;
		this.visible = visible;
		boolean refresh = this.visibleType == type;
		this.visibleType = type;
		DashboardOnMap.staticVisible = visible;
		DashboardOnMap.staticVisibleType = type;
		if (visible) {
			mapViewLocation = mapActivity.getMapLocation();
			mapRotation = mapActivity.getMapRotate();
			mapLinkedToLocation = mapActivity.getMapViewTrackingUtilities().isMapLinkedToLocation();
			myLocation = mapActivity.getMyApplication().getLocationProvider().getLastKnownLocation();
			mapActivity.getMapViewTrackingUtilities().setDashboard(this);
			dashboardView.setVisibility(View.VISIBLE);
			if(isActionButtonVisible()) {
				actionButton.setVisibility(View.VISIBLE);
			}
			updateDownloadBtn();
			View listViewLayout = dashboardView.findViewById(R.id.dash_list_view_layout);
			ScrollView scrollView = (ScrollView) dashboardView.findViewById(R.id.main_scroll);
			if(visibleType == DashboardType.DASHBOARD) {
				addOrUpdateDashboardFragments();
				scrollView.setVisibility(View.VISIBLE);
				listViewLayout.setVisibility(View.GONE);
				onScrollChanged(scrollView.getScrollY(), false, false);
			} else {
				scrollView.setVisibility(View.GONE);
				listViewLayout.setVisibility(View.VISIBLE);
				if (listView instanceof ObservableListView) {
					onScrollChanged(((ObservableListView) listView).getScrollY(), false, false);
				}
				if(refresh) {
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
			} else if(DashboardType.CONFIGURE_MAP == visibleType) {
				cm = new ConfigureMapMenu().createListAdapter(mapActivity);
			} else if(DashboardType.LIST_MENU == visibleType) {
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
		if(visibleType == DashboardType.WAYPOINTS || force) {
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
				if(click instanceof OnRowItemClick) {
					boolean cl = ((OnRowItemClick) click).onRowItemClick(listAdapter, view, cm.getElementId(which), which);
					if(cl) {
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
				intent.putExtra(DownloadActivity.FILTER_KEY, f.toString());
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
				if(isVisible()) {
					updateDownloadBtn();
				}
			}
		}, 4000);
	}


	public void navigationAction() {
		RoutingHelper routingHelper = mapActivity.getRoutingHelper();
		if(!routingHelper.isFollowingMode() && !routingHelper.isRoutePlanningMode()) {
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
		if(!animation) {
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
		boolean firstTime = getMyApplication().getAppInitializer().isFirstTime(mapActivity);
//		boolean showCards = mapActivity.getMyApplication().getSettings().USE_DASHBOARD_INSTEAD_OF_DRAWER.get();
		boolean showCards = !firstTime;
		
		FragmentManager manager = mapActivity.getSupportFragmentManager();
		FragmentTransaction fragmentTransaction = manager.beginTransaction();

		showFragment(manager, fragmentTransaction, DashFirstTimeFragment.TAG, DashFirstTimeFragment.class, firstTime);
		showFragment(manager, fragmentTransaction, DashChooseAppDirFragment.TAG, DashChooseAppDirFragment.class, 
				DashChooseAppDirFragment.isDashNeeded(getMyApplication().getSettings()));

		showFragment(manager, fragmentTransaction, DashErrorFragment.TAG, DashErrorFragment.class,
				mapActivity.getMyApplication().getAppInitializer().checkPreviousRunsForExceptions(mapActivity) && showCards);
		showFragment(manager, fragmentTransaction, DashNavigationFragment.TAG, DashNavigationFragment.class, showCards);
		showFragment(manager, fragmentTransaction, DashParkingFragment.TAG, DashParkingFragment.class, showCards);
		showFragment(manager, fragmentTransaction, DashWaypointsFragment.TAG, DashWaypointsFragment.class, showCards);
		showFragment(manager, fragmentTransaction, DashSearchFragment.TAG, DashSearchFragment.class, showCards);
		showFragment(manager, fragmentTransaction, DashRecentsFragment.TAG, DashRecentsFragment.class, showCards);
		showFragment(manager, fragmentTransaction, DashFavoritesFragment.TAG, DashFavoritesFragment.class, showCards);
		showFragment(manager, fragmentTransaction, DashAudioVideoNotesFragment.TAG, DashAudioVideoNotesFragment.class, showCards);
		showFragment(manager, fragmentTransaction, DashTrackFragment.TAG, DashTrackFragment.class, showCards);
		showFragment(manager, fragmentTransaction, DashOsMoFragment.TAG, DashOsMoFragment.class, showCards);
		showFragment(manager, fragmentTransaction, DashOsmEditsFragment.TAG, DashOsmEditsFragment.class, showCards);
//		showFragment(manager, fragmentTransaction, DashUpdatesFragment.TAG, DashUpdatesFragment.class, showCards);
		showFragment(manager, fragmentTransaction, DashPluginsFragment.TAG, DashPluginsFragment.class, showCards);
		showFragment(manager, fragmentTransaction, DashSimulateFragment.TAG, DashSimulateFragment.class,
				OsmandPlugin.getEnabledPlugin(OsmandDevelopmentPlugin.class) != null && showCards);
		
		fragmentTransaction.commit();
	}



	private <T extends Fragment> void showFragment(FragmentManager manager, FragmentTransaction fragmentTransaction,
			String tag, Class<T> cl, boolean cond) {
		try {
			Fragment frag = manager.findFragmentByTag(tag);
			if (manager.findFragmentByTag(tag) == null ) {
				if(cond) {
					T ni = cl.newInstance();
					fragmentTransaction.add(R.id.content, ni, tag);
				}
			} else {
				if(!cond) {
					fragmentTransaction.remove(manager.findFragmentByTag(tag));
 				} else if(frag instanceof DashBaseFragment){
 					if(((DashBaseFragment) frag).getView() != null) {
 						((DashBaseFragment) frag).onOpenDash();
 					}
 				}
			}
		} catch (Exception e) {
			getMyApplication().showToastMessage("Error showing dashboard " + tag);
			e.printStackTrace();
		}
	}

	public boolean isVisible() {
		return visible;
	}

	public void onDetach(DashBaseFragment dashBaseFragment) {
		Iterator<WeakReference<DashBaseFragment>> it = fragList.iterator();
		while(it.hasNext()) {
			WeakReference<DashBaseFragment> wr = it.next();
			if(wr.get() == dashBaseFragment) {
				it.remove();
			}
		}
	}
	
	
	public void updateLocation(final boolean centerChanged, final boolean locationChanged, final boolean compassChanged){
		if(inLocationUpdate) {
			return ;
		}
		inLocationUpdate = true;
		mapActivity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				inLocationUpdate = false;
				for (WeakReference<DashBaseFragment> df : fragList) {
					if (df.get() instanceof DashLocationFragment) {
						((DashLocationFragment)df.get()).updateLocation(centerChanged, locationChanged, compassChanged);
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
		fragList.add(new WeakReference<DashBaseFragment>(dashBaseFragment));
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
		if(previousVisibleType != visibleType && previousVisibleType != null) {
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
			if(listBackgroundView != null) {
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
		return visibleType == DashboardType.DASHBOARD || visibleType == DashboardType.LIST_MENU;
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
				((Toolbar) dashboardView.findViewById(R.id.toolbar)).setBackgroundDrawable(gradientToolbar);
			} else {
				((Toolbar) dashboardView.findViewById(R.id.toolbar)).setBackgroundColor(0xff000000 | baseColor);
			}
		}
	}
	
	private void updateListAdapter(ArrayAdapter<?> listAdapter, OnItemClickListener listener) {
		this.listAdapter = listAdapter;
		this.listAdapterOnClickListener = listener;
		if (this.listView != null) {
			listView.setAdapter(listAdapter);
			if(!portrait) {
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
			int colr = (((int) alpha ) << 24) | clr;
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
		for(WeakReference<DashBaseFragment> f: fragList) {
			DashBaseFragment b = f.get();
			if(b != null && !b.isDetached() && class1.isInstance(b)) {
				return (T) b;
			}
		}
		return null;
	}


	

}
