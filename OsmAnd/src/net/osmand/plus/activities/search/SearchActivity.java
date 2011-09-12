package net.osmand.plus.activities.search;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import net.osmand.osm.LatLon;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.NavigatePointActivity;
import android.app.Activity;
import android.app.TabActivity;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;


public class SearchActivity extends TabActivity {
	public static final int POI_TAB_INDEX = 0;
	public static final int ADDRESS_TAB_INDEX = 1;
	public static final int LOCATION_TAB_INDEX = 2;
	public static final int TRANSPORT_TAB_INDEX = 3;
	public static final int HISTORY_TAB_INDEX = 4;
	public static final String TAB_INDEX_EXTRA = "TAB_INDEX_EXTRA";
	
	protected static final int POSITION_CURRENT_LOCATION = 1;
	protected static final int POSITION_LAST_MAP_VIEW = 2;
	protected static final int POSITION_ADDRESS = 3;
	protected static final int POSITION_FAVORITES = 4;
	
	private static final int GPS_TIMEOUT_REQUEST = 1000;
	private static final int GPS_DIST_REQUEST = 5;
	private static final int GPS_ACCURACY = 50; 
	
	public static final String SEARCH_LAT = "net.osmand.search_lat"; //$NON-NLS-1$
	public static final String SEARCH_LON = "net.osmand.search_lon"; //$NON-NLS-1$

	Button searchPOIButton;
	private TabSpec addressSpec;
	private LatLon searchPoint = null;
	private boolean searchAroundCurrentLocation = false;

	private static boolean searchOnLine = false;
	private LocationListener locationListener = null;
	private ArrayAdapter<String> spinnerAdapter;
	private Spinner spinner;
	
	
	public interface SearchActivityChild {
		
		public void locationUpdate(LatLon l);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE); 
		setContentView(R.layout.search_main);
		
		
		spinner = (Spinner) findViewById(R.id.SpinnerLocation);
		spinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, 
				new ArrayList<String>(Arrays.asList(new String[]{
						getString(R.string.search_position_undefined),
						getString(R.string.search_position_current_location),
						getString(R.string.search_position_map_view),
						getString(R.string.search_position_favorites),
						getString(R.string.search_position_address)
					}))
				);
		
		
		TabHost host = getTabHost();
		host.addTab(host.newTabSpec("Search_POI").setIndicator(getString(R.string.poi)).setContent(new Intent(this, SearchPoiFilterActivity.class))); //$NON-NLS-1$

		addressSpec = host.newTabSpec("Search_Address").setIndicator(getString(R.string.address));
		setAddressSpecContent();

		host.addTab(addressSpec);
		host.addTab(host.newTabSpec("Search_Location").setIndicator(getString(R.string.search_tabs_location)).setContent(createIntent(NavigatePointActivity.class))); //$NON-NLS-1$
		TabSpec transportTab = host.newTabSpec("Search_Transport").setIndicator(getString(R.string.transport)).setContent(createIntent(SearchTransportActivity.class));
		//if (searchPoint != null) {
			host.addTab(transportTab); //$NON-NLS-1$
		//}
		host.addTab(host.newTabSpec("Search_History").setIndicator(getString(R.string.history)).setContent(createIntent(SearchHistoryActivity.class))); //$NON-NLS-1$
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
						}
					}
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
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
				LatLon searchPoint = new LatLon(lat, lon);
				updateSearchPoint(searchPoint, getString(R.string.search_position_fixed), true);
			}
		}
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		endSearchCurrentLocation();
	}
	
	public void updateSearchPoint(LatLon searchPoint, String message, boolean showLoc){
		spinnerAdapter.remove(spinnerAdapter.getItem(0));
		String suffix = "";
		if(showLoc && searchPoint != null){
			MessageFormat format = new MessageFormat(" ({0,number,#.##};{1,number,#.##})", Locale.US);
			suffix = format.format(new Object[]{searchPoint.getLatitude(), searchPoint.getLongitude()});
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
