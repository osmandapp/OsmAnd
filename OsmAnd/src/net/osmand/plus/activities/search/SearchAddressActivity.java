package net.osmand.plus.activities.search;

import net.osmand.plus.OsmandApplication;
import android.content.pm.ActivityInfo;
import android.os.Bundle;

import com.actionbarsherlock.app.SherlockFragmentActivity;

public class SearchAddressActivity extends SherlockFragmentActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		((OsmandApplication) getApplication()).applyTheme(this);
		super.onCreate(savedInstanceState);
		getSherlock().setUiOptions(ActivityInfo.UIOPTION_SPLIT_ACTION_BAR_WHEN_NARROW);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		if (savedInstanceState == null) {
			// During initial setup, plug in the details fragment.
			SearchAddressFragment details = new SearchAddressFragment();
			details.setArguments(getIntent().getExtras());
			getSupportFragmentManager().beginTransaction().add(android.R.id.content, details).commit();
		}
	}
	
	@Override
	public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
		int itemId = item.getItemId();
		switch (itemId) {
		case android.R.id.home:
			finish();
			return true;

		}
		return false;
	}
}