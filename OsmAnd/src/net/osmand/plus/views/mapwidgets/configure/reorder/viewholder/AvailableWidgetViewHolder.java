package net.osmand.plus.views.mapwidgets.configure.reorder.viewholder;

import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.plus.R;
import net.osmand.plus.views.controls.ReorderItemTouchHelperCallback.UnmovableItem;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.configure.reorder.viewholder.AddedWidgetViewHolder.AddedWidgetUiInfo;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

public class AvailableWidgetViewHolder extends ViewHolder implements UnmovableItem {

	public final ImageButton addWidgetButton;
	public final ImageView icon;
	public final TextView title;
	public final View bottomDivider;

	public AvailableWidgetViewHolder(@NonNull View itemView) {
		super(itemView);
		addWidgetButton = itemView.findViewById(R.id.add_widget_button);
		icon = itemView.findViewById(R.id.icon);
		title = itemView.findViewById(R.id.title);
		bottomDivider = itemView.findViewById(R.id.bottom_divider);
	}

	@Override
	public boolean isMovingDisabled() {
		return true;
	}

	public static class AvailableWidgetUiInfo {

		public String key;
		public String title;
		public MapWidgetInfo info;
		public int order;
		public int iconId;

		public AvailableWidgetUiInfo() {
		}

		public AvailableWidgetUiInfo(@NonNull AddedWidgetUiInfo addedWidgetUiInfo, int order) {
			this.key = addedWidgetUiInfo.key;
			this.title = addedWidgetUiInfo.title;
			this.info = addedWidgetUiInfo.info;
			this.order = order;
			this.iconId = addedWidgetUiInfo.iconId;
		}
	}
}