package net.osmand.plus.backup.trash.viewholder;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.R;

public class TrashItemViewHolder extends RecyclerView.ViewHolder {

	public View buttonView;
	public ImageView icon;
	public TextView title;
	public TextView description;
	public View cloudLabel;
	public View divider;

	public TrashItemViewHolder(@NonNull View itemView) {
		super(itemView);
		buttonView = itemView.findViewById(R.id.selectable_list_item);
		icon = itemView.findViewById(R.id.icon);
		title = itemView.findViewById(R.id.title);
		description = itemView.findViewById(R.id.description);
		cloudLabel = itemView.findViewById(R.id.cloud_label);
		divider = itemView.findViewById(R.id.bottom_divider);
	}

}
