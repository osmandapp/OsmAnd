package net.osmand.plus.settings.fragments.configureitems.viewholders;


import android.content.Context;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.FontCache;
import net.osmand.plus.views.controls.ReorderItemTouchHelperCallback.UnmovableItem;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;

public class RearrangeCategoryHolder extends ViewHolder implements UnmovableItem {

	private final TextView title;

	public RearrangeCategoryHolder(@NonNull View itemView) {
		super(itemView);
		Context context = itemView.getContext();

		title = itemView.findViewById(R.id.title);
		title.setTypeface(FontCache.getMediumFont());
		title.setTextSize(TypedValue.COMPLEX_UNIT_PX, context.getResources().getDimension(R.dimen.default_list_text_size));

		AndroidUiHelper.updateVisibility(itemView.findViewById(R.id.divider), true);
		AndroidUiHelper.updateVisibility(itemView.findViewById(R.id.icon), false);
		AndroidUiHelper.updateVisibility(itemView.findViewById(R.id.action_icon), false);
		AndroidUiHelper.updateVisibility(itemView.findViewById(R.id.move_button), false);
		AndroidUiHelper.updateVisibility(itemView.findViewById(R.id.description), false);
	}

	public void bindView(@NonNull ContextMenuItem item) {
		title.setText(item.getTitle());
	}

	@Override
	public boolean isMovingDisabled() {
		return false;
	}
}