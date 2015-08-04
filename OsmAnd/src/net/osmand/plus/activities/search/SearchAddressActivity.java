package net.osmand.plus.activities.search;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.OsmandActionBarActivity;
import android.os.Bundle;
import android.view.MenuItem;


public class SearchAddressActivity extends OsmandActionBarActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		((OsmandApplication) getApplication()).applyTheme(this);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.single_fragment_layout);
		if (savedInstanceState == null) {
			// During initial setup, plug in the details fragment.
			SearchAddressFragment details = new SearchAddressFragment();
			details.setArguments(getIntent().getExtras());
			getSupportFragmentManager().beginTransaction().add(android.R.id.content, details).commit();
		}
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		switch (itemId) {
		case android.R.id.home:
			finish();
			return true;

		}
		return false;
	}
}