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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        TabHost host = getTabHost();  
        host.addTab(host.newTabSpec("Search_POI").setIndicator(getString(R.string.poi)).setContent(new Intent(this, SearchPoiFilterActivity.class)));   //$NON-NLS-1$
        addressSpec = host.newTabSpec("Search_Address").setIndicator(getString(R.string.address)).setContent(new Intent(this, SearchAddressActivity.class));//$NON-NLS-1$
        host.addTab(addressSpec); 
        host.addTab(host.newTabSpec("Search_Location").setIndicator(getString(R.string.search_tabs_location)).setContent(new Intent(this, NavigatePointActivity.class))); //$NON-NLS-1$
//        host.addTab(host.newTabSpec("Search_Transport").setIndicator(getString(R.string.transport)).setContent(new Intent(this, SearchTransportActivity.class))); //$NON-NLS-1$
        host.addTab(host.newTabSpec("Search_History").setIndicator(getString(R.string.history)).setContent(new Intent(this, SearchHistoryActivity.class))); //$NON-NLS-1$
	}
	
	public void startSearchAddressOffline(){
		getTabHost().setCurrentTab(0);
		addressSpec.setContent(new Intent(this, SearchAddressActivity.class));
		getTabHost().setCurrentTab(1);
	}
	
	public void startSearchAddressOnline(){
		getTabHost().setCurrentTab(0);
		addressSpec.setContent(new Intent(this, SearchAddressOnlineActivity.class));
		getTabHost().setCurrentTab(1);
	}

}
