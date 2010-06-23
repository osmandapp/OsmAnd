/**
 * 
 */
package com.osmand.activities.search;

import com.osmand.R;
import com.osmand.activities.NavigatePointActivity;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TabHost;

/**
 * @author Maxim Frolov
 * 
 */
public class SearchActivity extends TabActivity {

	Button searchPOIButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        TabHost host = getTabHost();  
        host.addTab(host.newTabSpec("Search_POI").setIndicator(getString(R.string.poi)).setContent(new Intent(this, SearchPoiFilterActivity.class)));   //$NON-NLS-1$
        host.addTab(host.newTabSpec("Search_Address").setIndicator(getString(R.string.address)).setContent(new Intent(this, SearchAddressActivity.class))); //$NON-NLS-1$
        host.addTab(host.newTabSpec("Search_Location").setIndicator(getString(R.string.search_tabs_location)).setContent(new Intent(this, NavigatePointActivity.class))); //$NON-NLS-1$
	}

}
