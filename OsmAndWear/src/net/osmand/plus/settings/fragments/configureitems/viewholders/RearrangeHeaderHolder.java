package net.osmand.plus.settings.fragments.configureitems.viewholders;


import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.fragments.configureitems.RearrangeHeaderItem;
import net.osmand.plus.utils.FontCache;
import net.osmand.plus.views.controls.ReorderItemTouchHelperCallback.UnmovableItem;

public class RearrangeHeaderHolder extends ViewHolder implements UnmovableItem {

	private final OsmandApplication app;

	private final TextView title;
	private final TextView description;
	private final ImageView moveIcon;
	private boolean movable = true;

	public RearrangeHeaderHolder(@NonNull View itemView) {
		super(itemView);
		app = (OsmandApplication) itemView.getContext().getApplicationContext();

		title = itemView.findViewById(R.id.title);
		description = itemView.findViewById(R.id.summary);
		moveIcon = itemView.findViewById(R.id.move_icon);
	}


	public void bindView(@NonNull RearrangeHeaderItem item) {
		title.setTypeface(FontCache.getMediumFont());
		title.setTextSize(TypedValue.COMPLEX_UNIT_PX, app.getResources().getDimension(R.dimen.default_list_text_size));
		title.setText(item.titleId);
		description.setText(item.description);

		moveIcon.setVisibility(View.GONE);
		movable = item.titleId == R.string.additional_actions;
	}

	@Override
	public boolean isMovingDisabled() {
		return !movable;
	}
}
