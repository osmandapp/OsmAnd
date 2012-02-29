package net.osmand.plus.activities.search;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.Locale;

import net.osmand.Algoritms;
import net.osmand.FavouritePoint;
import net.osmand.osm.LatLon;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.FavouritesListActivity;
import net.osmand.plus.activities.NavigatePointActivity;
import android.app.Activity;
import android.app.TabActivity;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.TabWidget;
import android.widget.TextView;


public class SearchActivity extends TabActivity {
	public static final int POI_TAB_INDEX = 0;
	public static final int ADDRESS_TAB_INDEX = 1;
	public static final int LOCATION_TAB_INDEX = 2;
	public static final int TRANSPORT_TAB_INDEX = 3;
	public static final int HISTORY_TAB_INDEX = 4;
	public static final String TAB_INDEX_EXTRA = "TAB_INDEX_EXTRA";
	
	protected static final int POSITION_CURRENT_LOCATION = 1;
	protected static final int POSITION_LAST_MAP_VIEW = 2;
	protected static final int POSITION_FAVORITES = 3;
	protected static final int POSITION_ADDRESS = 4;
	
	private static final int GPS_TIMEOUT_REQUEST = 1000;
	private static final int GPS_DIST_REQUEST = 5;
	private static final int GPS_ACCURACY = 50; 
	
	private static final int REQUEST_FAVORITE_SELECT = 1;
	private static final int REQUEST_ADDRESS_SELECT = 2;
	
	public static final String SEARCH_LAT = "net.osmand.search_lat"; //$NON-NLS-1$
	public static final String SEARCH_LON = "net.osmand.search_lon"; //$NON-NLS-1$

	Button searchPOIButton;
	private TabSpec addressSpec;
	private LatLon searchPoint = null;
	private LatLon reqSearchPoint = null;
	private boolean searchAroundCurrentLocation = false;

	private static boolean searchOnLine = false;
	private LocationListener locationListener = null;
	private ArrayAdapter<String> spinnerAdapter;
	private Spinner spinner;
	
	
	public interface SearchActivityChild {
		
		public void locationUpdate(LatLon l);
	}
	
	private View getTabIndicator(int imageId){
		View r = getLayoutInflater().inflate(R.layout.search_main_tab_header, getTabHost(), false);
		ImageView tabImage = (ImageView)r.findViewById(R.id.TabImage);
		tabImage.setImageResource(imageId);
		tabImage.setBackgroundResource(R.drawable.tab_icon_background);
		return r;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE); 
		setContentView(R.layout.search_main);
		
		Button backButton = (Button) findViewById(R.id.search_back_button);
		backButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				SearchActivity.this.finish();
			}
		});
		
		spinner = (Spinner) findViewById(R.id.SpinnerLocation);
		spinnerAdapter = new ArrayAdapter<String>(this, R.layout.my_spinner_text, 
				new ArrayList<String>(Arrays.asList(new String[]{
						getString(R.string.search_position_undefined),
						getString(R.string.search_position_current_location),
						getString(R.string.search_position_map_view),
						getString(R.string.search_position_favorites),
						getString(R.string.search_position_address)
					}))
				) {
			@Override
			public View getDropDownView(int position, View convertView,
					ViewGroup parent) {
				View dropDownView = super.getDropDownView(position,
						convertView, parent);
				if (dropDownView instanceof TextView) {
					((TextView) dropDownView).setTextColor(getResources()
							.getColor(R.color.color_black));
				}
				return dropDownView;
			}
		};
		spinnerAdapter.setDropDownViewResource(R.layout.my_spinner_text);
		
		
		TabWidget tabs = (TabWidget) findViewById(android.R.id.tabs);
		tabs.setBackgroundResource(R.drawable.tab_icon_background);
		TabHost host = getTabHost(); 
		host.addTab(host.newTabSpec("Search_POI").setIndicator(getTabIndicator(R.drawable.tab_search_poi_icon)).
				setContent(new Intent(this, SearchPoiFilterActivity.class))); //$NON-NLS-1$
		
		addressSpec = host.newTabSpec("Search_Address").
				setIndicator(getTabIndicator(R.drawable.tab_search_address_icon));
		
		setAddressSpecContent();

		host.addTab(addressSpec);
		host.addTab(host.newTabSpec("Search_Location").setIndicator(getTabIndicator(R.drawable.tab_search_location_icon)).setContent(createIntent(NavigatePointActivity.class))); //$NON-NLS-1$
		TabSpec transportTab = host.newTabSpec("Search_Transport").setIndicator(getTabIndicator(R.drawable.tab_search_transport_icon)).setContent(createIntent(SearchTransportActivity.class));
		host.addTab(transportTab); //$NON-NLS-1$
		host.addTab(host.newTabSpec("Search_Favorites").setIndicator(getTabIndicator(R.drawable.tab_search_favorites_icon)).setContent(createIntent(FavouritesListActivity.class))); //$NON-NLS-1$
		host.addTab(host.newTabSpec("Search_History").setIndicator(getTabIndicator(R.drawable.tab_search_history_icon)).setContent(createIntent(SearchHistoryActivity.class))); //$NON-NLS-1$
		host.setCurrentTab(POI_TAB_INDEX);
		
		
		spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(spinnerAdapter);
		spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				if (position != 0) {
					if (position == POSITION_CURRENT_LOCATION) {
						startSearchCurrentLocation();
						searchAroundCurrentLocation = true;
					} else {
						searchAroundCurrentLocation = false;
						endSearchCurrentLocation();
						if (position == POSITION_LAST_MAP_VIEW) {
							OsmandSettings settings = OsmandSettings.getOsmandSettings(SearchActivity.this);
							updateSearchPoint(settings.getLastKnownMapLocation(), getString(R.string.search_position_fixed), true);
						} else if (position == POSITION_FAVORITES) {
							Intent intent = new Intent(SearchActivity.this, FavouritesListActivity.class);
							intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
							intent.putExtra(FavouritesListActivity.SELECT_FAVORITE_POINT_INTENT_KEY, (Serializable) null);
							startActivityForResult(intent, REQUEST_FAVORITE_SELECT);
							spinner.setSelection(0);
						} else if (position == POSITION_ADDRESS) {
							Intent intent = new Intent(SearchActivity.this, SearchAddressActivity.class);
							intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
							intent.putExtra(SearchAddressActivity.SELECT_ADDRESS_POINT_INTENT_KEY, (String) null);
							startActivityForResult(intent, REQUEST_ADDRESS_SELECT);
							spinner.setSelection(0);
						}
					}
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if(requestCode == REQUEST_FAVORITE_SELECT && resultCode == FavouritesListActivity.SELECT_FAVORITE_POINT_RESULT_OK){
			FavouritePoint p = (FavouritePoint) data.getSerializableExtra(FavouritesListActivity.SELECT_FAVORITE_POINT_INTENT_KEY);
			if (p != null) {
				LatLon latLon = new LatLon(p.getLatitude(), p.getLongitude());
				updateSearchPoint(latLon, p.getName(), false);
			}
		} else if(requestCode == REQUEST_ADDRESS_SELECT && resultCode == SearchAddressActivity.SELECT_ADDRESS_POINT_RESULT_OK){
			String name = data.getStringExtra(SearchAddressActivity.SELECT_ADDRESS_POINT_INTENT_KEY);
			LatLon latLon = new LatLon(
					data.getDoubleExtra(SearchAddressActivity.SELECT_ADDRESS_POINT_LAT, 0), 
					data.getDoubleExtra(SearchAddressActivity.SELECT_ADDRESS_POINT_LON, 0));
			if(name != null){
				updateSearchPoint(latLon, name, false);
			} else {
				updateSearchPoint(latLon, getString(R.string.search_position_fixed), true);
			}
		}
	}
	
	
	
	public void startSearchCurrentLocation(){
		if(locationListener == null){
			locationListener = new LocationListener() {
				@Override
				public void onStatusChanged(String provider, int status, Bundle extras) {}
				@Override
				public void onProviderEnabled(String provider) {}
				@Override
				public void onProviderDisabled(String provider) {}
				@Override
				public void onLocationChanged(Location location) {
					if(location != null){
						updateSearchPoint(new LatLon(location.getLatitude(), location.getLongitude()),
								getString(R.string.search_position_current_location_found), false);
						if(location.getAccuracy() < GPS_ACCURACY){
							endSearchCurrentLocation();
						}
					}
					
				}
			};
			LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
			for(String provider : locationManager.getAllProviders()){
				locationManager.requestLocationUpdates(provider, GPS_TIMEOUT_REQUEST, GPS_DIST_REQUEST, locationListener);
			}
		}
		updateSearchPoint(null,
				getString(R.string.search_position_current_location_search), false);
	}
	
	public void endSearchCurrentLocation(){
		if (locationListener != null) {
			LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
			locationManager.removeUpdates(locationListener);
			locationListener = null;
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		TabHost host = getTabHost();
		Intent intent = getIntent();
		int tabIndex = 0;
		if (intent != null) {
			if(intent.hasExtra(TAB_INDEX_EXTRA)){
				tabIndex = intent.getIntExtra(TAB_INDEX_EXTRA, POI_TAB_INDEX);
				host.setCurrentTab(tabIndex);
			}
			double lat = intent.getDoubleExtra(SEARCH_LAT, 0);
			double lon = intent.getDoubleExtra(SEARCH_LON, 0);
			if (lat != 0 || lon != 0) {
				LatLon l = new LatLon(lat, lon);
				if(!Algoritms.objectEquals(reqSearchPoint, l)){
					reqSearchPoint = l;
					updateSearchPoint(reqSearchPoint, getString(R.string.search_position_fixed), true);
				}
			}
		}
		
		if(searchPoint == null){
			LatLon last = OsmandSettings.getOsmandSettings(this).getLastKnownMapLocation();
			if(!Algoritms.objectEquals(reqSearchPoint, last)){
				reqSearchPoint = last;
				updateSearchPoint(last, getString(R.string.search_position_fixed), true);
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
	
	public void updateSearchPoint(LatLon searchPoint, String message, boolean showLoc){
		spinnerAdapter.remove(spinnerAdapter.getItem(0));
		String suffix = "";
		if(showLoc && searchPoint != null){
			suffix = formatLatLon(searchPoint);
		}
		spinnerAdapter.insert(message + suffix, 0);
		this.searchPoint = searchPoint;
		
		Activity currentActivity = getLocalActivityManager().getCurrentActivity();
		if(currentActivity instanceof SearchActivityChild){
			((SearchActivityChild) currentActivity).locationUpdate(searchPoint);
		}
		spinner.setSelection(0);
	}
	
	public LatLon getSearchPoint() {
		return searchPoint;
	}
	

	public boolean isSearchAroundCurrentLocation() {
		return searchAroundCurrentLocation;
	}
	
	private Intent createIntent(Class<? extends Activity> cl){
		Intent intent = new Intent(this, cl);
		return intent;
	}
	
	public void startSearchAddressOffline(){
		searchOnLine = false;
		getTabHost().setCurrentTab(0);
		setAddressSpecContent();
		getTabHost().setCurrentTab(1);
	}
	
	public void startSearchAddressOnline(){
		searchOnLine = true;
		getTabHost().setCurrentTab(0);
		setAddressSpecContent();
		getTabHost().setCurrentTab(1);
	}
	
	public void setAddressSpecContent() {
	     if (searchOnLine) {
	        addressSpec.setContent(createIntent(SearchAddressOnlineActivity.class));
	     } else {
	        addressSpec.setContent(createIntent(SearchAddressActivity.class));
	     }
	}

}
