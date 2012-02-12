package net.osmand.plus.activities;

import net.osmand.plus.R;
import android.app.ExpandableListActivity;
import android.widget.ExpandableListView;

public abstract class OsmandExpandableListActivity extends
		ExpandableListActivity {

	@Override
	protected void onResume() {
		super.onResume();
		
		ExpandableListView view = getExpandableListView();
		view.setCacheColorHint(getResources().getColor(R.color.activity_background));
		view.setDivider(getResources().getDrawable(R.drawable.tab_text_separator));
	}
}
