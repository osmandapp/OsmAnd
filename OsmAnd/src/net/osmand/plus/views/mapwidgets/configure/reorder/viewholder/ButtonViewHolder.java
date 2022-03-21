package net.osmand.plus.views.mapwidgets.configure.reorder.viewholder;

import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import net.osmand.plus.R;
import net.osmand.plus.views.controls.ReorderItemTouchHelperCallback.UnmovableItem;

public class ButtonViewHolder extends ViewHolder implements UnmovableItem {

	public View buttonView;
	public ImageView icon;
	public TextView title;

	public ButtonViewHolder(@NonNull View itemView) {
		super(itemView);
		buttonView = itemView.findViewById(R.id.button_container);
		icon = itemView.findViewById(R.id.icon);
		title = itemView.findViewById(R.id.title);
	}

	@Override
	public boolean isMovingDisabled() {
		return true;
	}

	public static class ButtonInfo {

		public final String title;
		public final int iconRes;
		public final OnClickListener listener;

		public ButtonInfo(@NonNull String title, int iconRes,
		                  @NonNull OnClickListener listener) {
			this.title = title;
			this.iconRes = iconRes;
			this.listener = listener;
		}
	}
}
