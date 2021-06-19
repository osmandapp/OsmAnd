package net.osmand.plus.activities;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;

public abstract class OsmandBaseExpandableListAdapter extends BaseExpandableListAdapter {

	protected void adjustIndicator(OsmandApplication app, int groupPosition, boolean isExpanded, View row, boolean light) {
		ImageView indicator = (ImageView) row.findViewById(R.id.explicit_indicator);
		if (!isExpanded) {
			indicator.setImageDrawable(app.getUIUtilities().getIcon(R.drawable.ic_action_arrow_down, light));
			indicator.setContentDescription(row.getContext().getString(R.string.access_collapsed_list));
		} else {
			indicator.setImageDrawable(app.getUIUtilities().getIcon(R.drawable.ic_action_arrow_up, light));
			indicator.setContentDescription(row.getContext().getString(R.string.access_expanded_list));
		}
		indicator.setVisibility(getChildrenCount(groupPosition) > 0 ? View.VISIBLE : View.GONE);
	}

	protected void setCategoryIcon(OsmandApplication app, int resId, int groupPosition, boolean isExpanded, View row, boolean light) {
		ImageView icon = (ImageView) row.findViewById(R.id.category_icon);
		if (resId == 0) {
			icon.setImageDrawable(app.getUIUtilities().getIcon(R.drawable.ic_action_folder_stroke, light));
		} else {
			icon.setImageDrawable(app.getUIUtilities().getIcon(resId, light));
		}
	}

	protected void setCategoryIcon(OsmandApplication app, Drawable res, int groupPosition, boolean isExpanded, View row, boolean light) {
		ImageView icon = (ImageView) row.findViewById(R.id.category_icon);
		icon.setImageDrawable(res);
	}
}
