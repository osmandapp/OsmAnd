package net.osmand.plus.dashboard;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings.CommonPreference;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.audionotes.DashAudioVideoNotesFragment;
import net.osmand.plus.development.DashSimulateFragment;
import net.osmand.plus.development.OsmandDevelopmentPlugin;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.helpers.ScreenOrientationHelper;
import net.osmand.plus.monitoring.DashTrackFragment;
import net.osmand.plus.osmedit.DashOsmEditsFragment;
import net.osmand.plus.osmo.DashOsmoFragment;
import net.osmand.plus.parkingpoint.DashParkingFragment;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.views.DownloadedRegionsLayer;
import net.osmand.plus.views.OsmandMapTileView;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.TranslateAnimation;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ScrollView;

import com.software.shell.fab.ActionButton;

/**
 * Created by Denis
 * on 03.03.15.
 */
public class DashboardOnMap {

	public static boolean staticVisible = false;
	private MapActivity mapActivity;
	private ActionButton actionButton;
	private FrameLayout dashboardView;
	private ArrayAdapter<?> listAdapter;
	private OnItemClickListener listAdapterOnClickListener;

	
	private boolean visible = false;
	private boolean landscape;
	private List<WeakReference<DashBaseFragment>> fragList = new LinkedList<WeakReference<DashBaseFragment>>();
	private net.osmand.Location myLocation;
	private LatLon mapViewLocation;
	private float heading;
	private boolean mapLinkedToLocation;
	private float mapRotation;
	private boolean inLocationUpdate = false;
	private boolean saveBackAction;
	private ImageView switchButton;
	private NotifyingScrollView scrollView;
	private View listViewLayout;
	private ListView listView;
	

	public DashboardOnMap(MapActivity ma) {
		this.mapActivity = ma;
	}


	public void createDashboardView() {
		landscape = !ScreenOrientationHelper.isOrientationPortrait(mapActivity);
//		dashboardView = (FrameLayout) mapActivity.getLayoutInflater().inflate(R.layout.dashboard_over_map, null, false);
//		dashboardView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
//				ViewGroup.LayoutParams.MATCH_PARENT));
//		((FrameLayout) mapActivity.findViewById(R.id.MapHudButtonsOverlay)).addView(dashboardView);
		dashboardView = (FrameLayout) mapActivity.findViewById(R.id.dashboard);
		View.OnClickListener listener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				setDashboardVisibility(false);
			}
		};
		scrollView = ((NotifyingScrollView) dashboardView.findViewById(R.id.main_scroll));
		listViewLayout = dashboardView.findViewById(R.id.dash_list_view_layout);
		listView = (ListView) dashboardView.findViewById(R.id.dash_list_view);
		dashboardView.findViewById(R.id.animateContent).setOnClickListener(listener);
		dashboardView.setOnClickListener(listener);

		actionButton = new ActionButton(mapActivity);
		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		int marginRight = convertPixelsToDp(16, mapActivity);
		params.setMargins(0, landscape ? 0 : convertPixelsToDp(164, mapActivity), marginRight, landscape ? marginRight : 0);

		params.gravity = landscape ? Gravity.BOTTOM | Gravity.RIGHT : Gravity.TOP | Gravity.RIGHT;
		actionButton.setLayoutParams(params);
		actionButton.setImageDrawable(mapActivity.getResources().getDrawable(R.drawable.ic_action_get_my_location));
		actionButton.setButtonColor(mapActivity.getResources().getColor(R.color.color_myloc_distance));
		actionButton.hide();
		actionButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (getMyApplication().accessibilityEnabled()) {
					mapActivity.getMapActions().whereAmIDialog();
				} else {
					mapActivity.getMapViewTrackingUtilities().backToLocationImpl();
				}
				setDashboardVisibility(false);
			}
		});
		dashboardView.addView(actionButton);

		
		if (ScreenOrientationHelper.isOrientationPortrait(mapActivity)) {
			scrollView.setOnScrollChangedListener(onScrollChangedListener);
			listView.setOnScrollListener(new OnScrollListener() {
				
				private TIntObjectHashMap<Integer> listViewItemHeights = new TIntObjectHashMap<Integer>();

				@Override
				public void onScrollStateChanged(AbsListView view, int scrollState) {
				}
				
				@Override
				public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
//					if(listView.getChildCount() == 0) {
//						return;
//					}
//					View c = listView.getChildAt(0);
//					int sy = -c.getTop();
//				    listViewItemHeights.put(listView.getFirstVisiblePosition(), c.getHeight());
//				    for (int i = 0; i < listView.getFirstVisiblePosition(); ++i) {
//				        if (listViewItemHeights.get(i) != null) // (this is a sanity check)
//				            sy += listViewItemHeights.get(i); //add all heights of the views that are gone
//				    }
//					double scale = view.getContext().getResources().getDisplayMetrics().density;
//					MarginLayoutParams lp = (MarginLayoutParams) actionButton.getLayoutParams();
//					lp.topMargin = (int) Math.max(30 * scale, 160 * scale - sy);
//					((FrameLayout) actionButton.getParent()).updateViewLayout(actionButton, lp);
//					MarginLayoutParams llp = (MarginLayoutParams) listView.getLayoutParams();
//					llp.topMargin = (int) Math.max(5 * scale, 160 * scale - sy);
//					listView.setLayoutParams(llp);
//					listView.getParent().requestLayout();
				}
			});
		}

	}
	
	private void switchBtnAction() {
		setDashboardVisibility(false);
		CommonPreference<Boolean> st = mapActivity.getMyApplication().getSettings().USE_DASHBOARD_INSTEAD_OF_DRAWER;
		st.set(!st.get());
		setDashboardVisibility(true);
//		mapActivity.getMapActions().toggleDrawer();
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
	
	public void setListAdapter(ArrayAdapter<?> listAdapter, OnItemClickListener optionsMenuOnClickListener) {
		if(!isVisible()) {
			mapActivity.getMyApplication().getSettings().USE_DASHBOARD_INSTEAD_OF_DRAWER.set(false);
		}
		this.listAdapter = listAdapter;
		this.listAdapterOnClickListener = optionsMenuOnClickListener;
		if(this.listView != null) {
			listView.setAdapter(listAdapter);
			listView.setOnItemClickListener(optionsMenuOnClickListener);
		}
		setDashboardVisibility(true);
	}

	public void setDashboardVisibility(boolean visible) {
		if(visible == this.visible) {
			return;
		}
		this.visible = visible;
		DashboardOnMap.staticVisible = visible;
		if (visible) {
			mapViewLocation = mapActivity.getMapLocation();
			mapRotation = mapActivity.getMapRotate();
			mapLinkedToLocation = mapActivity.getMapViewTrackingUtilities().isMapLinkedToLocation();
			myLocation = mapActivity.getMyApplication().getLocationProvider().getLastKnownLocation();
			mapActivity.getMapViewTrackingUtilities().setDashboard(this);
			dashboardView.setVisibility(View.VISIBLE);
			actionButton.show();
			updateDownloadBtn();
			if(mapActivity.getMyApplication().getSettings().USE_DASHBOARD_INSTEAD_OF_DRAWER.get()) {
				addOrUpdateDashboardFragments();
				scrollView.setVisibility(View.VISIBLE);
				listViewLayout.setVisibility(View.GONE);
				switchButton = (ImageView) scrollView.findViewById(R.id.map_menu_button);
				if(switchButton == null) {
					switchButton = (ImageView) dashboardView.findViewById(R.id.map_menu_button);
				}
				switchButton.setImageDrawable(mapActivity.getMyApplication().getIconsCache().getIcon(R.drawable.ic_navigation_drawer,
						R.color.icon_color_light));

			} else {
				scrollView.setVisibility(View.GONE);
				switchButton = (ImageView) listViewLayout.findViewById(R.id.map_menu_button);
				if(switchButton == null) {
					switchButton = (ImageView) dashboardView.findViewById(R.id.map_menu_button);
				}
				listViewLayout.setVisibility(View.VISIBLE);
				switchButton.setImageDrawable(mapActivity.getMyApplication().getIconsCache().getIcon(R.drawable.ic_dashboard_dark,
						R.color.icon_color_light));
			}
			switchButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					switchBtnAction();				
				}
			});

			//fabButton.showFloatingActionButton();
			open(dashboardView.findViewById(R.id.animateContent));
			
			mapActivity.getMapActions().disableDrawer();
			mapActivity.findViewById(R.id.MapHudButtonsOverlay).setVisibility(View.INVISIBLE);
			updateLocation(true, true, false);
			
		} else {
			mapActivity.getMapActions().enableDrawer();
			mapActivity.getMapViewTrackingUtilities().setDashboard(null);
			hide(dashboardView.findViewById(R.id.animateContent));
			mapActivity.findViewById(R.id.MapHudButtonsOverlay).setVisibility(View.VISIBLE);
			actionButton.hide();
			for (WeakReference<DashBaseFragment> df : fragList) {
				if (df.get() != null) {
					df.get().onCloseDash();
				}
			}
			
		}
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
				setDashboardVisibility(false);
				final Intent intent = new Intent(mapActivity, mapActivity.getMyApplication().getAppCustomization()
						.getDownloadIndexActivity());
				intent.putExtra(DownloadActivity.FILTER_KEY, f.toString());
				intent.putExtra(DownloadActivity.TAB_TO_OPEN, DownloadActivity.DOWNLOAD_TAB);
				mapActivity.startActivity(intent);
			}
		});
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
	}


	// To animate view slide out from right to left
	private void open(View view){
		TranslateAnimation animate = new TranslateAnimation(-mapActivity.findViewById(R.id.MapHudButtonsOverlay).getWidth(),0,0,0);
		animate.setDuration(500);
		animate.setFillAfter(true);
		view.startAnimation(animate);
		view.setVisibility(View.VISIBLE);
	}

	private void hide(View view) {
		TranslateAnimation animate = new TranslateAnimation(0, -mapActivity.findViewById(R.id.MapHudButtonsOverlay).getWidth(), 0, 0);
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
		view.setVisibility(View.GONE);
	}
	

	private void addOrUpdateDashboardFragments() {
		
//		boolean showCards = mapActivity.getMyApplication().getSettings().USE_DASHBOARD_INSTEAD_OF_DRAWER.get();
		boolean showCards = true;
		
		FragmentManager manager = mapActivity.getSupportFragmentManager();
		FragmentTransaction fragmentTransaction = manager.beginTransaction();
		//showFragment(manager, fragmentTransaction, DashOsmEditsFragment.TAG, DashOsmEditsFragment.class);
		
		
		showFragment(manager, fragmentTransaction, DashErrorFragment.TAG, DashErrorFragment.class,
				mapActivity.getMyApplication().getAppInitializer().checkPreviousRunsForExceptions(mapActivity) && showCards);
		showFragment(manager, fragmentTransaction, DashParkingFragment.TAG, DashParkingFragment.class, showCards);
		showFragment(manager, fragmentTransaction, DashWaypointsFragment.TAG, DashWaypointsFragment.class, showCards);
		showFragment(manager, fragmentTransaction, DashSearchFragment.TAG, DashSearchFragment.class, showCards);
		showFragment(manager, fragmentTransaction, DashRecentsFragment.TAG, DashRecentsFragment.class, showCards);
		showFragment(manager, fragmentTransaction, DashFavoritesFragment.TAG, DashFavoritesFragment.class, showCards);
		showFragment(manager, fragmentTransaction, DashAudioVideoNotesFragment.TAG, DashAudioVideoNotesFragment.class, showCards);
		showFragment(manager, fragmentTransaction, DashTrackFragment.TAG, DashTrackFragment.class, showCards);
		showFragment(manager, fragmentTransaction, DashOsmoFragment.TAG, DashOsmoFragment.class, showCards);
		//showFragment(manager, fragmentTransaction, DashOsmEditsFragment.TAG, DashOsmEditsFragment.class, showCards);
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
 					((DashBaseFragment) frag).onOpenDash();
 				}
			}
		} catch (Exception e) {
			getMyApplication().showToastMessage("Error showing dashboard");
			e.printStackTrace();
		}
	}




	private NotifyingScrollView.OnScrollChangedListener onScrollChangedListener = new NotifyingScrollView.OnScrollChangedListener() {
		public void onScrollChanged(ScrollView who, int l, int t, int oldl, int oldt) {
			int sy = who.getScrollY();
			double scale = who.getContext().getResources().getDisplayMetrics().density;
			FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) actionButton.getLayoutParams();
			lp.topMargin = (int) Math.max(30 * scale, 160 * scale - sy);
			((FrameLayout) actionButton.getParent()).updateViewLayout(actionButton, lp);
		}
	};
	

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


	public void saveBackAction() {
		saveBackAction = true;
	}
	
	public boolean clearBackAction() {
		if(saveBackAction) {
			saveBackAction = false;
			return true;
		}
		return false;
	}


	public void onMenuPressed() {
		if (!isVisible()) {
			setDashboardVisibility(true);
		} else {
			setDashboardVisibility(false);
		}
	}


	public boolean onBackPressed() {
		if (isVisible()) {
			setDashboardVisibility(false);
			return true;
		}
		return false;
	}


	
}
