package net.osmand.plus.settings.fragments.configureitems.viewholders;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.fragments.configureitems.RearrangeButtonItem;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.controls.ReorderItemTouchHelperCallback.UnmovableItem;

public class RearrangeButtonHolder extends ViewHolder implements UnmovableItem {

	private final OsmandApplication app;
	private final UiUtilities uiUtilities;
	private final int activeColorRes;

	private final ImageView icon;
	private final TextView title;

	public RearrangeButtonHolder(@NonNull View itemView, boolean nightMode) {
		super(itemView);
		app = (OsmandApplication) itemView.getContext().getApplicationContext();
		uiUtilities = app.getUIUtilities();
		activeColorRes = ColorUtilities.getActiveColorId(nightMode);

		icon = itemView.findViewById(android.R.id.icon);
		title = itemView.findViewById(android.R.id.title);
	}


	public void bindView(@NonNull RearrangeButtonItem item) {
		title.setText(app.getString(item.titleId));
		icon.setImageDrawable(uiUtilities.getIcon(item.iconId, activeColorRes));
		itemView.setOnClickListener(item.listener);
		Drawable drawable = UiUtilities.getColoredSelectableDrawable(app, ContextCompat.getColor(app, activeColorRes), 0.3f);
		AndroidUtils.setBackground(itemView, drawable);
	}

	@Override
	public boolean isMovingDisabled() {
		return true;
	}
}
