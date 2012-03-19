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
				indicator.setImageResource(R.drawable.list_activities_dot_marker1_empty);
			} else {
				indicator.setImageResource(R.drawable.list_activities_dot_marker1_content);
			}
		} else {
			indicator.setImageResource(R.drawable.list_activities_dot_marker1_pressed);
		}
	}

}
