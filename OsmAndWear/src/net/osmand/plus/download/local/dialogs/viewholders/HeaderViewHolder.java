package net.osmand.plus.download.local.dialogs.viewholders;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.R;
import net.osmand.plus.download.local.LocalItemUtils;
import net.osmand.plus.download.local.dialogs.HeaderGroup;

public class HeaderViewHolder extends RecyclerView.ViewHolder {

	private final TextView title;
	private final TextView count;

	public HeaderViewHolder(@NonNull View itemView) {
		super(itemView);
		title = itemView.findViewById(R.id.title);
		count = itemView.findViewById(R.id.count);
	}

	public void bindView(@NonNull HeaderGroup group) {
		title.setText(group.getName());
		count.setText(LocalItemUtils.getSizeDescription(itemView.getContext(), group.getItems()));
	}
}