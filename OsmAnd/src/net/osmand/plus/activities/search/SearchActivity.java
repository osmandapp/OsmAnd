package net.osmand.plus.activities.search;


import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;

import androidx.appcompat.app.ActionBar.OnNavigationListener;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager.widget.ViewPager.OnPageChangeListener;

import net.osmand.access.AccessibilityAssistant;
import net.osmand.access.NavigationInfo;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmAndLocationProvider.OsmAndLocationListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.FavoritesListActivity;
import net.osmand.plus.activities.FavoritesListFragment;
import net.osmand.plus.activities.NavigatePointFragment;
import net.osmand.plus.activities.TabActivity;
import net.osmand.plus.views.controls.PagerSlidingTabStrip;
import net.osmand.util.Algorithms;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;

public class SearchActivity extends TabActivity implements OsmAndLocationListener {
	public static final int POI_TAB_INDEX = 0;
	public static final int ADDRESS_TAB_INDEX = 1;
	public static final int LOCATION_TAB_INDEX = 2;
	public static final int FAVORITES_TAB_INDEX = 3;
	public static final int HISTORY_TAB_INDEX = 4;
	
	protected static final int POSITION_CURRENT_LOCATION = 1;
	protected static final int POSITION_LAST_MAP_VIEW = 2;
	protected static final int POSITION_FAVORITES = 3;
	protected static final int POSITION_ADDRESS = 4;
	
	private static final int REQUEST_FAVORITE_SELECT = 1;
	private static final int REQUEST_ADDRESS_SELECT = 2;
	
	public static final String SEARCH_NEARBY = "net.osmand.search_nearby"; //$NON-NLS-1$
	public static final String SEARCH_LAT = "net.osmand.search_lat"; //$NON-NLS-1$
	public static final String SEARCH_LON = "net.osmand.search_lon"; //$NON-NLS-1$
	public static final String SHOW_ONLY_ONE_TAB = "SHOW_ONLY_ONE_TAB"; //$NON-NLS-1$

	Button searchPOIButton;
	private LatLon searchPoint = null;
	private LatLon reqSearchPoint = null;
	private boolean searchAroundCurrentLocation = false;

	private static boolean searchOnLine = false;
	private ArrayAdapter<String> spinnerAdapter;
	private OsmandSettings settings;
	List<WeakReference<Fragment>> fragList = new ArrayList<WeakReference<Fragment>>();
	private boolean showOnlyOneTab;
	
	private AccessibilityAssistant accessibilityAssistant;
	private NavigationInfo navigationInfo;
	private View spinnerView;
	
	public interface SearchActivityChild {
		
		public void locationUpdate(LatLon l);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		((OsmandApplication) getApplication()).applyTheme(this);
		super.onCreate(savedInstanceState);
		getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		long t = System.currentTimeMillis();
 		setContentView(R.layout.tab_content);
		settings = ((OsmandApplication) getApplication()).getSettings();
		accessibilityAssistant = new AccessibilityAssistant(this);
		navigationInfo = new NavigationInfo((OsmandApplication)getApplication());
		
		showOnlyOneTab = getIntent() != null && getIntent().getBooleanExtra(SHOW_ONLY_ONE_TAB, false);
		getSupportActionBar().setTitle("");
		getSupportActionBar().setElevation(0);
		Integer tab = settings.SEARCH_TAB.get();
		if (!showOnlyOneTab) {
			ViewPager mViewPager = (ViewPager) findViewById(R.id.pager);
			PagerSlidingTabStrip mSlidingTabLayout = (PagerSlidingTabStrip) findViewById(R.id.sliding_tabs);
			List<TabItem> mTabs = new ArrayList<TabItem>();
			mTabs.add(getTabIndicator(R.string.poi, getFragment(POI_TAB_INDEX)));
			mTabs.add(getTabIndicator(R.string.address, getFragment(ADDRESS_TAB_INDEX)));
			mTabs.add(getTabIndicator(R.string.shared_string_location, getFragment(LOCATION_TAB_INDEX)));
			mTabs.add(getTabIndicator(R.string.favorite, getFragment(FAVORITES_TAB_INDEX)));
			mTabs.add(getTabIndicator(R.string.shared_string_history, getFragment(HISTORY_TAB_INDEX)));

			
			setViewPagerAdapter(mViewPager, mTabs);
			mSlidingTabLayout.setViewPager(mViewPager);
			
			mViewPager.setCurrentItem(Math.min(tab, HISTORY_TAB_INDEX));
			mSlidingTabLayout.setOnPageChangeListener(new OnPageChangeListener() {

				@Override
				public void onPageSelected(int arg0) {
					settings.SEARCH_TAB.set(arg0);
				}

				@Override
				public void onPageScrolled(int arg0, float arg1, int arg2) {
				}

				@Override
				public void onPageScrollStateChanged(int arg0) {

				}
			});
		} else {
			setContentView(R.layout.search_activity_single);
			Class<?> cl = getFragment(tab);
			try {
				getSupportFragmentManager().beginTransaction().replace(R.id.layout, (Fragment) cl.newInstance()).commit();
			} catch (InstantiationException e) {
				throw new IllegalStateException(e);
			} catch (IllegalAccessException e) {
				throw new IllegalStateException(e);
			}
        }
        setTopSpinner();
		
		Log.i("net.osmand", "Start on create " + (System.currentTimeMillis() - t ));
		
		Intent intent = getIntent();
		OsmandSettings settings = ((OsmandApplication) getApplication()).getSettings();
		LatLon last = settings.getLastKnownMapLocation();
		if (intent != null) {
			double lat = intent.getDoubleExtra(SEARCH_LAT, 0);
			double lon = intent.getDoubleExtra(SEARCH_LON, 0);
			if (lat != 0 || lon != 0) {
				LatLon l = new LatLon(lat, lon);
				if(!Algorithms.objectEquals(reqSearchPoint, l)){
					reqSearchPoint = l;
					if ((Math.abs(lat - last.getLatitude()) < 0.00001) && (Math.abs(lon - last.getLongitude()) < 0.00001)) {
						updateSearchPoint(reqSearchPoint, getString(R.string.select_search_position) + " " + getString(R.string.search_position_map_view), false);
					} else {
						updateSearchPoint(reqSearchPoint, getString(R.string.select_search_position) + " ", true);
					}
				}
			}
		}
		if(searchPoint == null){
			if(!Algorithms.objectEquals(reqSearchPoint, last)){
				reqSearchPoint = last;
				updateSearchPoint(last, getString(R.string.select_search_position) + " " + getString(R.string.search_position_map_view), false);
			}
		}
    }

	protected Class<?> getFragment(int tab) {
		if(tab == POI_TAB_INDEX) {
			return SearchPoiFilterFragment.class;
		} else if(tab == ADDRESS_TAB_INDEX) {
			return SearchAddressFragment.class;
		} else if(tab == LOCATION_TAB_INDEX) {
			return NavigatePointFragment.class;
		} else if(tab == HISTORY_TAB_INDEX) {
			return SearchHistoryFragment.class;
		} else if(tab == FAVORITES_TAB_INDEX) {
			return FavoritesListFragment.class;
		}
		return SearchPoiFilterFragment.class;
	}

	public AccessibilityAssistant getAccessibilityAssistant() {
		return accessibilityAssistant;
	}

	public NavigationInfo getNavigationInfo() {
		return navigationInfo;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		switch (itemId) {
		case android.R.id.home:
			finish();
			return true;

		}
		return false;
	}

	private void setTopSpinner() {
		spinnerAdapter = new ArrayAdapter<String>(getSupportActionBar().getThemedContext(), R.layout.spinner_item,
				new ArrayList<String>(Arrays.asList(new String[]{
						getString(R.string.shared_string_undefined),
						getString(R.string.shared_string_my_location) + getString(R.string.shared_string_ellipsis),
						getString(R.string.search_position_map_view),
						getString(R.string.search_position_favorites),
						getString(R.string.search_position_address)
				}))
				) {
					@Override
					public View getDropDownView(int position, View convertView, ViewGroup parent) {
						View itemView = super.getDropDownView(position, convertView, parent);
						ViewCompat.setAccessibilityDelegate(itemView, accessibilityAssistant);
						return itemView;
					}
				};
		spinnerAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
		spinnerView = LayoutInflater.from(spinnerAdapter.getContext()).inflate(R.layout.spinner_item, null);
        getSupportActionBar().setListNavigationCallbacks(spinnerAdapter, new OnNavigationListener() {
			
			@Override
			public boolean onNavigationItemSelected(int position, long itemId) {
				if (position != 0) {
					if (position == POSITION_CURRENT_LOCATION) {
						net.osmand.Location loc = getLocationProvider().getLastKnownLocation();
						searchAroundCurrentLocation = true;
						if(loc != null && System.currentTimeMillis() - loc.getTime() < 10000) {
							updateLocation(loc);
						}
						startSearchCurrentLocation();
					} else {
						searchAroundCurrentLocation = false;
						endSearchCurrentLocation();
						if (position == POSITION_LAST_MAP_VIEW) {
							updateSearchPoint(settings.getLastKnownMapLocation(), getString(R.string.select_search_position) + " " + getString(R.string.search_position_map_view), false);
						} else if (position == POSITION_FAVORITES) {
							Intent intent = new Intent(SearchActivity.this, FavoritesListActivity.class);
							intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
							intent.putExtra(FavoritesListFragment.SELECT_FAVORITE_POINT_INTENT_KEY, (Serializable) null);
							startActivityForResult(intent, REQUEST_FAVORITE_SELECT);
							getSupportActionBar().setSelectedNavigationItem(0);
						} else if (position == POSITION_ADDRESS) {
							Intent intent = new Intent(SearchActivity.this, SearchAddressActivity.class);
							intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
							intent.putExtra(SearchAddressFragment.SELECT_ADDRESS_POINT_INTENT_KEY, (String) null);
							startActivityForResult(intent, REQUEST_ADDRESS_SELECT);
							getSupportActionBar().setSelectedNavigationItem(0);
						}
					}
				}
				return true;
			}
		});
	}

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

		
		
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if(requestCode == REQUEST_FAVORITE_SELECT && resultCode == FavoritesListFragment.SELECT_FAVORITE_POINT_RESULT_OK){
			FavouritePoint p = (FavouritePoint) data.getSerializableExtra(FavoritesListFragment.SELECT_FAVORITE_POINT_INTENT_KEY);
			if (p != null) {
				LatLon latLon = new LatLon(p.getLatitude(), p.getLongitude());
				updateSearchPoint(latLon, getString(R.string.select_search_position) + " " + p.getName(), false);
			}
		} else if(requestCode == REQUEST_ADDRESS_SELECT && resultCode == SearchAddressFragment.SELECT_ADDRESS_POINT_RESULT_OK){
			String name = data.getStringExtra(SearchAddressFragment.SELECT_ADDRESS_POINT_INTENT_KEY);
			LatLon latLon = new LatLon(
					data.getDoubleExtra(SearchAddressFragment.SELECT_ADDRESS_POINT_LAT, 0), 
					data.getDoubleExtra(SearchAddressFragment.SELECT_ADDRESS_POINT_LON, 0));
			if(name != null){
				updateSearchPoint(latLon, getString(R.string.select_search_position) + " " + name, false);
			} else {
				updateSearchPoint(latLon, getString(R.string.select_search_position) + " ", true);
			}
		}
	}
	
	public Toolbar getClearToolbar(boolean visible) {
		final Toolbar tb = (Toolbar) findViewById(R.id.bottomControls);
		tb.setTitle(null);
		tb.getMenu().clear();
		tb.setVisibility(visible? View.VISIBLE : View.GONE);
		return tb;
	}


	public void updateLocation(net.osmand.Location location){
		if (location != null) {
			navigationInfo.updateLocation(location);
			updateSearchPoint(new LatLon(location.getLatitude(), location.getLongitude()),
					getString(R.string.select_search_position) + " " + getString(R.string.search_position_current_location_found), false);
			// don't stop in case we want to see updates 
//			if (location.getAccuracy() < 20) {
//				endSearchCurrentLocation();
//			}
		}
	}
	public void startSearchCurrentLocation(){
		getLocationProvider().resumeAllUpdates();
		getLocationProvider().addLocationListener(this);
		updateSearchPoint(null,
				getString(R.string.search_position_current_location_search), false);
	}

	private OsmAndLocationProvider getLocationProvider() {
		return ((OsmandApplication) getApplication()).getLocationProvider();
	}
	
	public void endSearchCurrentLocation(){
		getLocationProvider().pauseAllUpdates();
		getLocationProvider().removeLocationListener(this);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if (!showOnlyOneTab) {
			Integer tab = settings.SEARCH_TAB.get();
			ViewPager mViewPager = (ViewPager) findViewById(R.id.pager);
			mViewPager.setCurrentItem(Math.min(tab, HISTORY_TAB_INDEX));
		}
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		endSearchCurrentLocation();
	}
	
	private String formatLatLon(LatLon searchPoint){
		return new Formatter(Locale.US).format(" %.2f;%.2f", searchPoint.getLatitude(), searchPoint.getLongitude()).toString();
	}
	
	@Override
	public void onAttachFragment (Fragment fragment) {
	    fragList.add(new WeakReference<Fragment>(fragment));
	}
	
	public void updateSearchPoint(LatLon searchPoint, String message, boolean showLoc){
		String oldState = spinnerAdapter.getItem(0);
                String newState = message;
		if(showLoc && searchPoint != null){
			newState += formatLatLon(searchPoint);
		}
		accessibilityAssistant.lockEvents();
		if (!oldState.equals(newState)) {
			spinnerAdapter.remove(oldState);
			spinnerAdapter.insert(newState, 0);
		}
		this.searchPoint = searchPoint;
		for(WeakReference<Fragment> ref : fragList) {
	        Fragment f = ref.get();
	        if(f instanceof SearchActivityChild) {
	            if(!f.isDetached()) {
	            	((SearchActivityChild) f).locationUpdate(searchPoint);
	            }
	        }
	    }
		getSupportActionBar().setSelectedNavigationItem(0);
		accessibilityAssistant.unlockEvents();
	}
	
	public LatLon getSearchPoint() {
		return searchPoint;
	}
	

	public boolean isSearchAroundCurrentLocation() {
		return searchAroundCurrentLocation;
	}
	
	public void startSearchAddressOffline(){
		searchOnLine = false;
		setAddressSpecContent();
	}
	
	public void startSearchAddressOnline(){
		searchOnLine = true;
		setAddressSpecContent();
	}
	
	public void setAddressSpecContent() {
		Intent intent = getIntent();
		finish();
		startActivity(intent);
	}
	



}
