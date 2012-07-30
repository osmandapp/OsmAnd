package net.osmand.plus.activities;

import net.osmand.plus.R;
import android.view.View;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;

public abstract class OsmandBaseExpandableListAdapter extends BaseExpandableListAdapter {

	protected void adjustIndicator(int groupPosition, boolean isExpanded, View row) {
		ImageView indicator = (ImageView) row.findViewById(R.id.explist_indicator);
		if (!isExpanded) {
			if (getChildrenCount(groupPosition) == 0) {
				indicator.setImageResource(R.drawable.expandable_category_empty);
			} else {
				indicator.setImageResource(R.drawable.expandable_category_unpushed);
			}
		} else {
			indicator.setImageResource(R.drawable.expandable_category_pushed);
		}
	}

}
