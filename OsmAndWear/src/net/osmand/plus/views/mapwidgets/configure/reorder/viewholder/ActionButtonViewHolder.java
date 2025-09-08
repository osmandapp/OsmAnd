package net.osmand.plus.views.mapwidgets.configure.reorder.viewholder;

import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.plus.R;
import net.osmand.plus.views.controls.ReorderItemTouchHelperCallback.UnmovableItem;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

public class ActionButtonViewHolder extends ViewHolder implements UnmovableItem {

	public View buttonView;
	public ImageView icon;
	public TextView title;

	public ActionButtonViewHolder(@NonNull View itemView) {
		super(itemView);
		buttonView = itemView.findViewById(R.id.container);
		icon = itemView.findViewById(R.id.icon);
		title = itemView.findViewById(R.id.title);
	}

	@Override
	public boolean isMovingDisabled() {
		return true;
	}

	public static class ActionButtonInfo {

		public final String title;
		public final int iconRes;
		public final OnClickListener listener;

		public ActionButtonInfo(@NonNull String title, int iconRes,
		                        @NonNull OnClickListener listener) {
			this.title = title;
			this.iconRes = iconRes;
			this.listener = listener;
		}
	}
}
