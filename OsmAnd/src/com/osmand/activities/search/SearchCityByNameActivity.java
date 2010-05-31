package com.osmand.activities.search;

import com.osmand.R;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;

public class SearchCityByNameActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.search_by_name);
	}
}
