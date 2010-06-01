/**
 * 
 */
package com.osmand.activities.search;

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
        host.addTab(host.newTabSpec("Search_POI").setIndicator("Search POI").setContent(new Intent(this, SearchPOIListActivity.class)));  
        host.addTab(host.newTabSpec("Search_Address").setIndicator("Search Address").setContent(new Intent(this, SearchAddressActivity.class)));
        host.addTab(host.newTabSpec("Search_Location").setIndicator("Search Location").setContent(new Intent(this, NavigatePointActivity.class)));
	}

}
