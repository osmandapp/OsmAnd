package net.osmand.plus.views.mapwidgets.configure.reorder.viewholder;

import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import net.osmand.plus.R;
import net.osmand.plus.views.controls.ReorderItemTouchHelperCallback.UnmovableItem;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;

public class AddedWidgetViewHolder extends ViewHolder implements UnmovableItem {

	public final ImageButton deleteWidgetButton;
	public final TextView title;
	public final ImageView icon;
	public final ImageView moveIcon;

	private final ItemMovableCallback itemMovableCallback;

	public AddedWidgetViewHolder(@NonNull View itemView, @Nullable ItemMovableCallback callback) {
		super(itemView);
		this.itemMovableCallback = callback;

		deleteWidgetButton = itemView.findViewById(R.id.delete_widget_button);
		title = itemView.findViewById(R.id.title);
		icon = itemView.findViewById(R.id.icon);
		moveIcon = itemView.findViewById(R.id.move_icon);
	}

	@Override
	public boolean isMovingDisabled() {
		int position = getAdapterPosition();
		if (position != RecyclerView.NO_POSITION && itemMovableCallback != null) {
			return !itemMovableCallback.isListItemMovable(position);
		}
		return false;
	}

	public static class AddedWidgetUiInfo {

		public String key;
		public String title;
		public MapWidgetInfo info;
		public int iconId;

		public AddedWidgetUiInfo() {
		}

		public AddedWidgetUiInfo(@NonNull String key, @NonNull String title, @NonNull MapWidgetInfo info, int iconId) {
			this.key = key;
			this.title = title;
			this.info = info;
			this.iconId = iconId;
		}
	}

	public interface ItemMovableCallback {
		boolean isListItemMovable(int position);
	}
}
