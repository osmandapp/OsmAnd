package net.osmand.plus.activities.search;

import net.osmand.osm.LatLon;
import net.osmand.plus.R;
import net.osmand.plus.activities.NavigatePointActivity;
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

	Button searchPOIButton;
	private TabSpec addressSpec;

	private static boolean searchOnLine = false;
	 
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();
//		LatLon latLon = null;
		int tabIndex = 0;
		if(intent != null){
			tabIndex = intent.getIntExtra(TAB_INDEX_EXTRA, POI_TAB_INDEX);
		}
		
        TabHost host = getTabHost();  
        host.addTab(host.newTabSpec("Search_POI").setIndicator(getString(R.string.poi)).setContent(new Intent(this, SearchPoiFilterActivity.class)));   //$NON-NLS-1$
        
        addressSpec = host.newTabSpec("Search_Address").setIndicator(getString(R.string.address));
        setAddressSpecContent();
        
        host.addTab(addressSpec); 
        host.addTab(host.newTabSpec("Search_Location").setIndicator(getString(R.string.search_tabs_location)).setContent(new Intent(this, NavigatePointActivity.class))); //$NON-NLS-1$
//        host.addTab(host.newTabSpec("Search_Transport").setIndicator(getString(R.string.transport)).setContent(new Intent(this, SearchTransportActivity.class))); //$NON-NLS-1$
        host.addTab(host.newTabSpec("Search_History").setIndicator(getString(R.string.history)).setContent(new Intent(this, SearchHistoryActivity.class))); //$NON-NLS-1$
        host.setCurrentTab(tabIndex);
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
	        addressSpec.setContent(new Intent(this, SearchAddressOnlineActivity.class));
	     } else {
	        addressSpec.setContent(new Intent(this, SearchAddressActivity.class));
	     }
	}

}
