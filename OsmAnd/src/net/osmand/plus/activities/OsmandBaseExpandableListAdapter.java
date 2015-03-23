package net.osmand.plus.activities;

import android.view.View;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;

import net.osmand.plus.R;

public abstract class OsmandBaseExpandableListAdapter extends BaseExpandableListAdapter {

	protected void adjustIndicator(int groupPosition, boolean isExpanded, View row, boolean light) {
		ImageView indicator = (ImageView) row.findViewById(R.id.explist_indicator);
		if (!isExpanded) {
			if (getChildrenCount(groupPosition) == 0) {
				indicator.setImageResource(light ? R.drawable.expandable_category_empty_light : R.drawable.expandable_category_empty_dark);
			} else {
				indicator.setImageResource(light ? R.drawable.expandable_category_unpushed_light : R.drawable.expandable_category_unpushed_dark);
			}
		} else {
			indicator.setImageResource(light ? R.drawable.expandable_category_pushed_light : R.drawable.expandable_category_pushed_dark);
		}
	}

}
