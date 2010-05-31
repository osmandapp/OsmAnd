/**
 * 
 */
package com.osmand.activities.search;

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
//
//		setContentView(R.layout.search);
//
//		searchPOIButton = (Button) findViewById(R.id.SearchPOIButton);
//		searchPOIButton.setOnClickListener(new OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				final Intent search = new Intent(SearchActivity.this, SearchPOIListActivity.class);
//				search.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
//				startActivity(search);
//
//			}
//		});
        TabHost host = getTabHost();  
        host.addTab(host.newTabSpec("Search_POI").setIndicator("Search POI").setContent(new Intent(this, SearchPOIListActivity.class)));  
        host.addTab(host.newTabSpec("Search_Adress").setIndicator("Search Address").setContent(new Intent(this, SearchAddress.class)));
	}

}
