package net.osmand.plus.activities;

import net.osmand.plus.R;
import android.app.ListActivity;
import android.widget.ListView;

public abstract class OsmandListActivity extends ListActivity {

	@Override
	protected void onResume() {
		super.onResume();
		
		ListView view = getListView();
		view.setCacheColorHint(getResources().getColor(R.color.activity_background));
		view.setDivider(getResources().getDrawable(R.drawable.tab_text_separator));
	}
}
