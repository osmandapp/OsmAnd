package net.osmand.plus.activities.search;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;

import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmAndLocationProvider.OsmAndLocationListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.FavouritesListActivity;
import net.osmand.plus.activities.FavouritesListFragment;
import net.osmand.plus.activities.NavigatePointFragment;
import net.osmand.util.Algorithms;
import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.TabWidget;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar.OnNavigationListener;
import com.actionbarsherlock.app.SherlockFragmentActivity;


public class SearchActivity extends SherlockFragmentActivity implements OsmAndLocationListener {
	private static final String SEARCH_HISTORY = "Search_History";
	private static final String SEARCH_FAVORITES = "Search_Favorites";
	private static final String SEARCH_TRANSPORT = "Search_Transport";
	private static final String SEARCH_LOCATION = "Search_Location";
	private static final String SEARCH_ADDRESS = "Search_Address";
	private static final String SEARCH_POI = "Search_POI";
	public static final int POI_TAB_INDEX = 0;
	public static final int ADDRESS_TAB_INDEX = 1;
	public static final int LOCATION_TAB_INDEX = 2;
	public static final int HISTORY_TAB_INDEX = 3;
	public static final int TRANSPORT_TAB_INDEX = 4;
	
	public static final String TAB_INDEX_EXTRA = "TAB_INDEX_EXTRA";
	
	protected static final int POSITION_CURRENT_LOCATION = 1;
	protected static final int POSITION_LAST_MAP_VIEW = 2;
	protected static final int POSITION_FAVORITES = 3;
	protected static final int POSITION_ADDRESS = 4;
	
	private static final int REQUEST_FAVORITE_SELECT = 1;
	private static final int REQUEST_ADDRESS_SELECT = 2;
	
	public static final String SEARCH_LAT = "net.osmand.search_lat"; //$NON-NLS-1$
	public static final String SEARCH_LON = "net.osmand.search_lon"; //$NON-NLS-1$

	Button searchPOIButton;
	private LatLon searchPoint = null;
	private LatLon reqSearchPoint = null;
	private boolean searchAroundCurrentLocation = false;

	private static boolean searchOnLine = false;
	private ArrayAdapter<String> spinnerAdapter;
	private OsmandSettings settings;
	private TabsAdapter mTabsAdapter;
	List<WeakReference<Fragment>> fragList = new ArrayList<WeakReference<Fragment>>();
	

	
	
	public interface SearchActivityChild {
		
		public void locationUpdate(LatLon l);
	}
	
	private View getTabIndicator(TabHost tabHost, int imageId, int stringId){
		View r = getLayoutInflater().inflate(R.layout.search_main_tab_header, tabHost, false);
		ImageView tabImage = (ImageView)r.findViewById(R.id.TabImage);
		tabImage.setImageResource(imageId);
		tabImage.setBackgroundResource(R.drawable.tab_icon_background);
		tabImage.setContentDescription(getString(stringId));
		return r;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		((OsmandApplication) getApplication()).applyTheme(this);
		super.onCreate(savedInstanceState);
		long t = System.currentTimeMillis();
		getSherlock().setUiOptions(ActivityInfo.UIOPTION_SPLIT_ACTION_BAR_WHEN_NARROW);
		getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		setContentView(R.layout.search_main);
		settings = ((OsmandApplication) getApplication()).getSettings();
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setTitle("");
//		getSupportActionBar().setTitle(R.string.select_search_position);
		
		final TextView tabinfo  = (TextView) findViewById(R.id.textViewADesc);

		TabWidget tabs = (TabWidget) findViewById(android.R.id.tabs);
		tabs.setBackgroundResource(R.drawable.tab_icon_background);
		
        TabHost tabHost = (TabHost)findViewById(android.R.id.tabhost);
        tabHost.setup();

        ViewPager mViewPager = (ViewPager)findViewById(R.id.pager);
        mTabsAdapter = new TabsAdapter(this, tabHost, tabinfo, mViewPager);
        TabSpec poiTab = tabHost.newTabSpec(SEARCH_POI).setIndicator(getTabIndicator(tabHost, R.drawable.tab_search_poi_icon, R.string.poi));
		mTabsAdapter.addTab(poiTab, SearchPoiFilterActivity.class, null);

		TabSpec addressSpec = tabHost.newTabSpec(SEARCH_ADDRESS).setIndicator(
				getTabIndicator(tabHost, R.drawable.tab_search_address_icon, R.string.address));
		mTabsAdapter.addTab(addressSpec, searchOnLine? SearchAddressOnlineFragment.class :   SearchAddressFragment.class, null);
		// mTabsAdapter.addTab(addressSpec, SearchAddressOnlineActivity.class, null);
		TabSpec locationTab = tabHost.newTabSpec(SEARCH_LOCATION).setIndicator(getTabIndicator(tabHost, R.drawable.tab_search_location_icon, R.string.search_tabs_location));
		mTabsAdapter.addTab(locationTab, NavigatePointFragment.class, null); 
		TabSpec favoriteTab = tabHost.newTabSpec(SEARCH_FAVORITES).setIndicator(getTabIndicator(tabHost, R.drawable.tab_search_favorites_icon, R.string.favorite));
		mTabsAdapter.addTab(favoriteTab, FavouritesListFragment.class, null); 
		TabSpec historyTab = tabHost.newTabSpec(SEARCH_HISTORY).setIndicator(getTabIndicator(tabHost, R.drawable.tab_search_history_icon, R.string.history));
		mTabsAdapter.addTab(historyTab, SearchHistoryFragment.class, null);
		TabSpec transportTab = tabHost.newTabSpec(SEARCH_TRANSPORT).setIndicator(getTabIndicator(tabHost, R.drawable.tab_search_transport_icon, R.string.transport));
		mTabsAdapter.addTab(transportTab, SearchTransportFragment.class, null);
		tabHost.setCurrentTab(POI_TAB_INDEX);
        if (savedInstanceState != null) {
            tabHost.setCurrentTabByTag(savedInstanceState.getString("tab"));
        }
        
        setTopSpinner();
		
		Log.i("net.osmand", "Start on create " + (System.currentTimeMillis() - t ));

    }
	
	@Override
	public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
		int itemId = item.getItemId();
		switch (itemId) {
		case android.R.id.home:
			finish();
			return true;

		}
		return false;
	}

	private void setTopSpinner() {
		spinnerAdapter = new ArrayAdapter<String>(getSupportActionBar().getThemedContext(), R.layout.sherlock_spinner_item, 
				new ArrayList<String>(Arrays.asList(new String[]{
						getString(R.string.search_position_undefined),
						getString(R.string.search_position_current_location),
						getString(R.string.search_position_map_view),
						getString(R.string.search_position_favorites),
						getString(R.string.search_position_address)
					}))
				);
		spinnerAdapter.setDropDownViewResource(R.layout.sherlock_spinner_dropdown_item);
        getSupportActionBar().setListNavigationCallbacks(spinnerAdapter, new OnNavigationListener() {
			
			@Override
			public boolean onNavigationItemSelected(int position, long itemId) {
				if (position != 0) {
					if (position == POSITION_CURRENT_LOCATION) {
						net.osmand.Location loc = getLocationProvider().getLastKnownLocation();
						if(loc != null && System.currentTimeMillis() - loc.getTime() < 10000) {
							updateLocation(loc);
						} else {
							startSearchCurrentLocation();
							searchAroundCurrentLocation = true;
						}
					} else {
						searchAroundCurrentLocation = false;
						endSearchCurrentLocation();
						if (position == POSITION_LAST_MAP_VIEW) {
							updateSearchPoint(settings.getLastKnownMapLocation(), getString(R.string.search_position_fixed), true);
						} else if (position == POSITION_FAVORITES) {
							Intent intent = new Intent(SearchActivity.this, FavouritesListActivity.class);
							intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
							intent.putExtra(FavouritesListFragment.SELECT_FAVORITE_POINT_INTENT_KEY, (Serializable) null);
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
        outState.putString("tab", mTabsAdapter.mTabHost.getCurrentTabTag());
    }

		
		
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if(requestCode == REQUEST_FAVORITE_SELECT && resultCode == FavouritesListFragment.SELECT_FAVORITE_POINT_RESULT_OK){
			FavouritePoint p = (FavouritePoint) data.getSerializableExtra(FavouritesListFragment.SELECT_FAVORITE_POINT_INTENT_KEY);
			if (p != null) {
				LatLon latLon = new LatLon(p.getLatitude(), p.getLongitude());
				updateSearchPoint(latLon, p.getName(), false);
			}
		} else if(requestCode == REQUEST_ADDRESS_SELECT && resultCode == SearchAddressFragment.SELECT_ADDRESS_POINT_RESULT_OK){
			String name = data.getStringExtra(SearchAddressFragment.SELECT_ADDRESS_POINT_INTENT_KEY);
			LatLon latLon = new LatLon(
					data.getDoubleExtra(SearchAddressFragment.SELECT_ADDRESS_POINT_LAT, 0), 
					data.getDoubleExtra(SearchAddressFragment.SELECT_ADDRESS_POINT_LON, 0));
			if(name != null){
				updateSearchPoint(latLon, name, false);
			} else {
				updateSearchPoint(latLon, getString(R.string.search_position_fixed), true);
			}
		}
	}
	
	
	public void updateLocation(net.osmand.Location location){
		if (location != null) {
			updateSearchPoint(new LatLon(location.getLatitude(), location.getLongitude()),
					getString(R.string.search_position_current_location_found), false);
			if (location.getAccuracy() < 20) {
				endSearchCurrentLocation();
			}
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
		Intent intent = getIntent();
		int tabIndex = 0;
		if (intent != null) {
			if(intent.hasExtra(TAB_INDEX_EXTRA)){
				tabIndex = intent.getIntExtra(TAB_INDEX_EXTRA, POI_TAB_INDEX);
				mTabsAdapter.mTabHost.setCurrentTab(tabIndex);
			}
			double lat = intent.getDoubleExtra(SEARCH_LAT, 0);
			double lon = intent.getDoubleExtra(SEARCH_LON, 0);
			if (lat != 0 || lon != 0) {
				LatLon l = new LatLon(lat, lon);
				if(!Algorithms.objectEquals(reqSearchPoint, l)){
					reqSearchPoint = l;
					updateSearchPoint(reqSearchPoint, getString(R.string.search_position_fixed), true);
				}
			}
		}
		
		if(searchPoint == null){
			LatLon last = settings.getLastKnownMapLocation();
			if(!Algorithms.objectEquals(reqSearchPoint, last)){
				reqSearchPoint = last;
				updateSearchPoint(last, getString(R.string.select_search_position), true);
			}
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
		spinnerAdapter.remove(spinnerAdapter.getItem(0));
		String suffix = "";
		if(showLoc && searchPoint != null){
			suffix = formatLatLon(searchPoint);
		}
		spinnerAdapter.insert(message + suffix, 0);
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
//		mTabsAdapter.mViewPager.setCurrentItem(0);
//		mTabsAdapter.mTabHost.setCurrentTab(0);
//		if (searchOnLine) {
//			mTabsAdapter.mTabs.get(1).clss = SearchAddressOnlineFragment.class;
//		} else {
//			mTabsAdapter.mTabs.get(1).clss = SearchAddressFragment.class;
//		}
//		mTabsAdapter.notifyDataSetChanged();
//		mTabsAdapter.mViewPager.invalidate();
		Intent intent = getIntent();
		intent.putExtra(TAB_INDEX_EXTRA, ADDRESS_TAB_INDEX);
		finish();
		startActivity(intent);
	}
	

    /**
     * This is a helper class that implements the management of tabs and all
     * details of connecting a ViewPager with associated TabHost.  It relies on a
     * trick.  Normally a tab host has a simple API for supplying a View or
     * Intent that each tab will show.  This is not sufficient for switching
     * between pages.  So instead we make the content part of the tab host
     * 0dp high (it is not shown) and the TabsAdapter supplies its own dummy
     * view to show as the tab content.  It listens to changes in tabs, and takes
     * care of switch to the correct paged in the ViewPager whenever the selected
     * tab changes.
     */
    public static class TabsAdapter extends FragmentPagerAdapter
            implements TabHost.OnTabChangeListener, ViewPager.OnPageChangeListener {
        private final Context mContext;
        private final TabHost mTabHost;
        private final ViewPager mViewPager;
        private final ArrayList<TabInfo> mTabs = new ArrayList<TabInfo>();
		private TextView tabInfo;

        static final class TabInfo {
            private final String tag;
            private Class<?> clss;
            private Bundle args;

            TabInfo(String _tag, Class<?> _class, Bundle _args) {
                tag = _tag;
                clss = _class;
                args = _args;
            }
        }

        static class DummyTabFactory implements TabHost.TabContentFactory {
            private final Context mContext;

            public DummyTabFactory(Context context) {
                mContext = context;
            }

            @Override
            public View createTabContent(String tag) {
                View v = new View(mContext);
                v.setMinimumWidth(0);
                v.setMinimumHeight(0);
                return v;
            }
        }

        public TabsAdapter(FragmentActivity activity, TabHost tabHost, TextView tabinfo, ViewPager pager) {
            super(activity.getSupportFragmentManager());
            mContext = activity;
            mTabHost = tabHost;
			tabInfo = tabinfo;
            mViewPager = pager;
            mTabHost.setOnTabChangedListener(this);
            mViewPager.setAdapter(this);
            mViewPager.setOnPageChangeListener(this);
        }

        public void addTab(TabHost.TabSpec tabSpec, Class<?> clss, Bundle args) {
            tabSpec.setContent(new DummyTabFactory(mContext));
            String tag = tabSpec.getTag();

            TabInfo info = new TabInfo(tag, clss, args);
            mTabs.add(info);
            mTabHost.addTab(tabSpec);
            notifyDataSetChanged();
        }
        

        @Override
        public int getCount() {
            return mTabs.size();
        }
        
        @Override
        public Fragment getItem(int position) {
            TabInfo info = mTabs.get(position);
            return Fragment.instantiate(mContext, info.clss.getName(), info.args);
        }

        @Override
        public void onTabChanged(String tabId) {
            int position = mTabHost.getCurrentTab();
            mViewPager.setCurrentItem(position);
            if (SEARCH_POI.equals(tabId)) {
				tabInfo.setText(R.string.poi_search_desc);
			} else	if (SEARCH_ADDRESS.equals(tabId)) {
				tabInfo.setText(searchOnLine? R.string.search_osm_nominatim :  R.string.address_search_desc);
			} else	if (SEARCH_LOCATION.equals(tabId)) {
				tabInfo.setText(R.string.navpoint_search_desc);
			} else	if (SEARCH_TRANSPORT.equals(tabId)) {
				tabInfo.setText(R.string.transport_search_desc);
			} else	if (SEARCH_FAVORITES.equals(tabId)) {
				tabInfo.setText(R.string.favourites_search_desc);
			} else	if (SEARCH_HISTORY.equals(tabId)) {
				tabInfo.setText(R.string.history_search_desc);
			}
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            // Unfortunately when TabHost changes the current tab, it kindly
            // also takes care of putting focus on it when not in touch mode.
            // The jerk.
            // This hack tries to prevent this from pulling focus out of our
            // ViewPager.
            TabWidget widget = mTabHost.getTabWidget();
            int oldFocusability = widget.getDescendantFocusability();
            widget.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
            mTabHost.setCurrentTab(position);
            widget.setDescendantFocusability(oldFocusability);
        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }
    }

}
