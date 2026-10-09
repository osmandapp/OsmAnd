package net.osmand.plus.base;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;

public abstract class OsmandBaseExpandableListAdapter extends BaseExpandableListAdapter {

	protected void adjustIndicator(OsmandApplication app, int groupPosition, boolean expanded, View row, boolean light) {
		adjustIndicator(app, row, expanded, !light);
		ImageView indicator = row.findViewById(R.id.explicit_indicator);
		indicator.setVisibility(getChildrenCount(groupPosition) > 0 ? View.VISIBLE : View.GONE);
	}

	public static void adjustIndicator(@NonNull OsmandApplication app, @NonNull View row, boolean expanded, boolean nightMode) { // todo use for icon
		ImageView indicator = row.findViewById(R.id.explicit_indicator);
		if (!expanded) {
			indicator.setImageDrawable(app.getUIUtilities().getIcon(R.drawable.ic_action_arrow_down, !nightMode));
			indicator.setContentDescription(row.getContext().getString(R.string.access_collapsed_list));
		} else {
			indicator.setImageDrawable(app.getUIUtilities().getIcon(R.drawable.ic_action_arrow_up, !nightMode));
			indicator.setContentDescription(row.getContext().getString(R.string.access_expanded_list));
		}
	}

	protected void setCategoryIcon(OsmandApplication app, int resId, View row, boolean light) {
		ImageView icon = row.findViewById(R.id.category_icon);
		if (resId == 0) {
			icon.setImageDrawable(app.getUIUtilities().getIcon(R.drawable.ic_action_folder_stroke, light));
		} else {
			icon.setImageDrawable(app.getUIUtilities().getIcon(resId, light));
		}
	}

	protected void setCategoryIcon(Drawable res, View row) {
		ImageView icon = row.findViewById(R.id.category_icon);
		icon.setImageDrawable(res);
	}
}
