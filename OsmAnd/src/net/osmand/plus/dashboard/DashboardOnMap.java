package net.osmand.plus.dashboard;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.audionotes.DashAudioVideoNotesFragment;
import net.osmand.plus.development.DashSimulateFragment;
import net.osmand.plus.development.OsmandDevelopmentPlugin;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.helpers.ScreenOrientationHelper;
import net.osmand.plus.monitoring.DashTrackFragment;
import net.osmand.plus.parkingpoint.DashParkingFragment;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.views.DownloadedRegionsLayer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.controls.FloatingActionButton;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ScrollView;

/**
 * Created by Denis
 * on 03.03.15.
 */
public class DashboardOnMap {

	public static boolean staticVisible = false;
	private static final int LIST_ID = 1;
	private static final int DIRECTIONS_ID = 2;
	private static final int CONFIGURE_SCREEN_ID = 3;
	private static final int SETTINGS_ID = 4;
	private MapActivity mapActivity;
	FloatingActionButton fabButton;
	boolean floatingButtonVisible = true;
	private FrameLayout dashboardView;
	
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
		dashboardView.findViewById(R.id.animateContent).setOnClickListener(listener);
		dashboardView.setOnClickListener(listener);
		
		
		switchButton =  (ImageView) dashboardView.findViewById(R.id.map_layers_button);
		switchButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				setDashboardVisibility(false);
				mapActivity.getMyApplication().getSettings().USE_DASHBOARD_INSTEAD_OF_DRAWER.set(false);
				mapActivity.getMapActions().toggleDrawer();				
			}
		});
		
		
		

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			fabButton = new FloatingActionButton.Builder(mapActivity)
					.withDrawable(mapActivity.getResources().getDrawable(R.drawable.ic_action_get_my_location))
					.withButtonColor(mapActivity.getResources().getColor(R.color.color_myloc_distance))
					.withGravity(landscape ? Gravity.BOTTOM | Gravity.RIGHT : Gravity.TOP | Gravity.RIGHT)
					.withMargins(0, landscape ? 0 : 160, 16, landscape ? 16 : 0).create();
			fabButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					if (getMyApplication().accessibilityEnabled()) {
						mapActivity.getMapActions().whereAmIDialog();
					} else {
						mapActivity.getMapViewTrackingUtilities().backToLocationImpl();
					}
					setDashboardVisibility(false);
				}
			});
			fabButton.hideFloatingActionButton();
		}

		if (ScreenOrientationHelper.isOrientationPortrait(mapActivity)) {
			((NotifyingScrollView) dashboardView.findViewById(R.id.main_scroll))
					.setOnScrollChangedListener(onScrollChangedListener);
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

	public void setDashboardVisibility(boolean visible) {
		this.visible = visible;
		DashboardOnMap.staticVisible = visible;
		if (visible) {
			mapViewLocation = mapActivity.getMapLocation();
			mapRotation = mapActivity.getMapRotate();
			mapLinkedToLocation = mapActivity.getMapViewTrackingUtilities().isMapLinkedToLocation();
			myLocation = mapActivity.getMyApplication().getLocationProvider().getLastKnownLocation();
			mapActivity.getMapViewTrackingUtilities().setDashboard(this);
			addOrUpdateDashboardFragments();
			setupActionBar();
			updateDownloadBtn();
			dashboardView.setVisibility(View.VISIBLE);
			fabButton.showFloatingActionButton();
			open(dashboardView.findViewById(R.id.animateContent));
			switchButton.setImageDrawable(mapActivity.getMyApplication().getIconsCache().getIcon(R.drawable.ic_navigation_drawer,
					R.color.icon_color_light));
			
			mapActivity.getMapActions().disableDrawer();
			mapActivity.findViewById(R.id.MapHudButtonsOverlay).setVisibility(View.GONE);
			updateLocation(true, true, false);
			
		} else {
			mapActivity.getMapActions().enableDrawer();
			mapActivity.getMapViewTrackingUtilities().setDashboard(null);
			hide(dashboardView.findViewById(R.id.animateContent));
			mapActivity.findViewById(R.id.MapHudButtonsOverlay).setVisibility(View.VISIBLE);
			fabButton.hideFloatingActionButton();
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


	private void setupActionBar() {
		final Toolbar tb = (Toolbar) mapActivity.findViewById(R.id.bottomControls);
		tb.setTitle(null);
		tb.getMenu().clear();
		Menu menu = tb.getMenu();
		createMenuItem(menu, LIST_ID, R.string.drawer, 
				R.drawable.ic_dashboard_dark, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
		createMenuItem(menu, DIRECTIONS_ID, R.string.get_directions, 
				R.drawable.ic_action_gdirections_dark, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
		createMenuItem(menu, CONFIGURE_SCREEN_ID, R.string.layer_map_appearance,
				R.drawable.ic_configure_screen_dark, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
		createMenuItem(menu, SETTINGS_ID, R.string.shared_string_settings, 
				R.drawable.ic_action_settings_enabled_dark, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
	}
	
	public MenuItem createMenuItem(Menu m, int id, int titleRes, int icon, int menuItemType) {
		int r = icon;
		MenuItem menuItem = m.add(0, id, 0, titleRes);
		if (r != 0) {
			menuItem.setIcon(r);
		}
		menuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				return onOptionsItemSelected(item);
			}
		});
		MenuItemCompat.setShowAsAction(menuItem, menuItemType);
		return menuItem;
	}


	protected boolean onOptionsItemSelected(MenuItem item) {
		setDashboardVisibility(false);
		if(item.getItemId() == LIST_ID) {
			// temporarily disable drawer
//			getMyApplication().getSettings().USE_DASHBOARD_INSTEAD_OF_DRAWER.set(false);
			mapActivity.getMapActions().toggleDrawer();
		} else if(item.getItemId() == DIRECTIONS_ID) {
			navigationAction();
		} else if(item.getItemId() == CONFIGURE_SCREEN_ID) {
			mapActivity.getMapActions().prepareConfigureScreen();
			mapActivity.getMapActions().toggleDrawer();
			return false;	
		} else if(item.getItemId() == SETTINGS_ID) {
			final Intent settings = new Intent(mapActivity, getMyApplication().getAppCustomization().getSettingsActivity());
			mapActivity.startActivity(settings);
		} else {
			return false;
		}
		return true;
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
		Iterator<WeakReference<DashBaseFragment>> it = fragList.iterator();
		while(it.hasNext()) {
			WeakReference<DashBaseFragment> df = it.next();
			if(df.get() != null) {
				if(df.get().getView() != null) {
					df.get().onOpenDash();
				}
			} else {
				it.remove();
			}
		}
		FragmentManager manager = mapActivity.getSupportFragmentManager();
		FragmentTransaction fragmentTransaction = manager.beginTransaction();
		showFragment(manager, fragmentTransaction, DashErrorFragment.TAG, DashErrorFragment.class,
				mapActivity.getMyApplication().getAppInitializer().checkPreviousRunsForExceptions(mapActivity));
		showFragment(manager, fragmentTransaction, DashParkingFragment.TAG, DashParkingFragment.class);
		showFragment(manager, fragmentTransaction, DashWaypointsFragment.TAG, DashWaypointsFragment.class);
		showFragment(manager, fragmentTransaction, DashSearchFragment.TAG, DashSearchFragment.class);
		showFragment(manager, fragmentTransaction, DashRecentsFragment.TAG, DashRecentsFragment.class);
		showFragment(manager, fragmentTransaction, DashFavoritesFragment.TAG, DashFavoritesFragment.class);
		showFragment(manager, fragmentTransaction, DashAudioVideoNotesFragment.TAG, DashAudioVideoNotesFragment.class);
		showFragment(manager, fragmentTransaction, DashTrackFragment.TAG, DashTrackFragment.class);
//		showFragment(manager, fragmentTransaction, DashUpdatesFragment.TAG, DashUpdatesFragment.class);
		showFragment(manager, fragmentTransaction, DashPluginsFragment.TAG, DashPluginsFragment.class);
		
		showFragment(manager, fragmentTransaction, DashSimulateFragment.TAG, DashSimulateFragment.class,
				OsmandPlugin.getEnabledPlugin(OsmandDevelopmentPlugin.class) != null);
		fragmentTransaction.commit();
	}



	private <T extends Fragment> void showFragment(FragmentManager manager, FragmentTransaction fragmentTransaction,
			String tag, Class<T> cl) {
		showFragment(manager, fragmentTransaction, tag, cl, true);
	}

	private <T extends Fragment> void showFragment(FragmentManager manager, FragmentTransaction fragmentTransaction,
			String tag, Class<T> cl, boolean cond) {
		try {
			if (manager.findFragmentByTag(tag) == null && cond) {
				T ni = cl.newInstance();
				fragmentTransaction.add(R.id.content, ni, tag);
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
			FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) fabButton.getLayoutParams();
			lp.topMargin = (int) Math.max(30 * scale, 160 * scale - sy);
			((FrameLayout) fabButton.getParent()).updateViewLayout(fabButton, lp);
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

	
}
