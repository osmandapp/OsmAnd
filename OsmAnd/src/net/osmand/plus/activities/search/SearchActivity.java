/**
 * 
 */
package net.osmand.plus.activities.search;

import net.osmand.plus.R;
import net.osmand.plus.activities.NavigatePointActivity;
import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;


/**
 * @author Maxim Frolov
 * 
 */
public class SearchActivity extends TabActivity {

	Button searchPOIButton;
	private TabSpec addressSpec;

	private static boolean searchOnLine = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        TabHost host = getTabHost();  
        host.addTab(host.newTabSpec("Search_POI").setIndicator(getString(R.string.poi)).setContent(new Intent(this, SearchPoiFilterActivity.class)));   //$NON-NLS-1$
        
        addressSpec = host.newTabSpec("Search_Address").setIndicator(getString(R.string.address));
        setAddressSpecContent();
        
        host.addTab(addressSpec); 
        host.addTab(host.newTabSpec("Search_Location").setIndicator(getString(R.string.search_tabs_location)).setContent(new Intent(this, NavigatePointActivity.class))); //$NON-NLS-1$
//        host.addTab(host.newTabSpec("Search_Transport").setIndicator(getString(R.string.transport)).setContent(new Intent(this, SearchTransportActivity.class))); //$NON-NLS-1$
        host.addTab(host.newTabSpec("Search_History").setIndicator(getString(R.string.history)).setContent(new Intent(this, SearchHistoryActivity.class))); //$NON-NLS-1$
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
