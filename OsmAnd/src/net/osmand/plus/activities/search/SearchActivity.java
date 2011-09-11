package net.osmand.plus.activities.search;

import net.osmand.osm.LatLon;
import net.osmand.plus.R;
import net.osmand.plus.activities.NavigatePointActivity;
import android.app.Activity;
import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;


public class SearchActivity extends TabActivity {
	public static final int POI_TAB_INDEX = 0;
	public static final int ADDRESS_TAB_INDEX = 1;
	public static final int LOCATION_TAB_INDEX = 2;
	public static final int TRANSPORT_TAB_INDEX = 3;
	public static final int HISTORY_TAB_INDEX = 4;
	public static final String TAB_INDEX_EXTRA = "TAB_INDEX_EXTRA";
	
	public static final String SEARCH_LAT = "net.osmand.search_lat"; //$NON-NLS-1$
	public static final String SEARCH_LON = "net.osmand.search_lon"; //$NON-NLS-1$

	Button searchPOIButton;
	private TabSpec addressSpec;
	private LatLon searchPoint = null;

	private static boolean searchOnLine = false;
	 
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		Intent intent = getIntent();
		int tabIndex = 0;
		if(intent != null){
			tabIndex = intent.getIntExtra(TAB_INDEX_EXTRA, POI_TAB_INDEX);
			float lat = intent.getFloatExtra(SEARCH_LAT, 0);
			float lon = intent.getFloatExtra(SEARCH_LON, 0);
			if(lat != 0 || lon != 0){
				searchPoint = new LatLon(lat, lon);
			}
		}
		
        TabHost host = getTabHost();  
        // TODO investigate proper intent with lat/lon ?
        host.addTab(host.newTabSpec("Search_POI").setIndicator(getString(R.string.poi)).setContent(new Intent(this, SearchPoiFilterActivity.class)));   //$NON-NLS-1$
        
        addressSpec = host.newTabSpec("Search_Address").setIndicator(getString(R.string.address));
        setAddressSpecContent(searchPoint);
        
        host.addTab(addressSpec); 
        host.addTab(host.newTabSpec("Search_Location").setIndicator(getString(R.string.search_tabs_location)).setContent(createIntent(NavigatePointActivity.class))); //$NON-NLS-1$
		if (searchPoint != null) {
			host.addTab(host.newTabSpec("Search_Transport").setIndicator(getString(R.string.transport)).setContent(createIntent(SearchTransportActivity.class))); //$NON-NLS-1$
		}
        host.addTab(host.newTabSpec("Search_History").setIndicator(getString(R.string.history)).setContent(createIntent(SearchHistoryActivity.class))); //$NON-NLS-1$
        host.setCurrentTab(tabIndex);
	}
	
	private Intent createIntent(Class<? extends Activity> cl){
		Intent intent = new Intent(this, cl);
		if(searchPoint != null){
			intent.putExtra(SearchActivity.SEARCH_LAT, searchPoint.getLatitude());
			intent.putExtra(SearchActivity.SEARCH_LON, searchPoint.getLongitude());
		}
		return intent;
	}
	
	public void startSearchAddressOffline(){
		searchOnLine = false;
		getTabHost().setCurrentTab(0);
		setAddressSpecContent(searchPoint);
		getTabHost().setCurrentTab(1);
	}
	
	public void startSearchAddressOnline(){
		searchOnLine = true;
		getTabHost().setCurrentTab(0);
		setAddressSpecContent(searchPoint);
		getTabHost().setCurrentTab(1);
	}
	
	public void setAddressSpecContent(LatLon latLon) {
	     if (searchOnLine) {
	        addressSpec.setContent(createIntent(SearchAddressOnlineActivity.class));
	     } else {
	        addressSpec.setContent(createIntent(SearchAddressOnlineActivity.class));
	     }
	}

}
