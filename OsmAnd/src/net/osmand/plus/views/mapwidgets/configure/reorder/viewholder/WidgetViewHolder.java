package net.osmand.plus.views.mapwidgets.configure.reorder.viewholder;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.R;
import net.osmand.plus.views.controls.ReorderItemTouchHelperCallback;

public class WidgetViewHolder extends RecyclerView.ViewHolder
		implements ReorderItemTouchHelperCallback.UnmovableItem {

	public TextView title;
	public ImageView icon;
	public ImageView moveIcon;

	private final CheckItemIsMovable checkItemIsMovable;

	public WidgetViewHolder(View itemView, CheckItemIsMovable checkItemIsMovable) {
		super(itemView);
		this.checkItemIsMovable = checkItemIsMovable;
		title = itemView.findViewById(R.id.title);
		icon = itemView.findViewById(R.id.icon);
		moveIcon = itemView.findViewById(R.id.move_icon);
	}

	@Override
	public boolean isMovingDisabled() {
		int position = getAdapterPosition();
		if (position != RecyclerView.NO_POSITION && checkItemIsMovable != null) {
			return !checkItemIsMovable.isListItemMovable(position);
		}
		return false;
	}


	public static class WidgetUiInfo {
		public String title;
		public int iconId;
		public int order;
		public boolean isActive;

		public void toggleActive() {
			isActive = !isActive;
		}
	}

	public interface CheckItemIsMovable {
		boolean isListItemMovable(int position);
	}
}
