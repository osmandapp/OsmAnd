package net.osmand.plus.download.local.dialogs.viewholders;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.R;
import net.osmand.plus.download.local.BaseLocalItem;
import net.osmand.plus.download.local.dialogs.HeaderGroup;
import net.osmand.plus.utils.AndroidUtils;

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
		count.setText(AndroidUtils.formatSize(itemView.getContext(), getSize(group)));
	}

	public long getSize(@NonNull HeaderGroup group) {
		long size = 0;
		for (BaseLocalItem item : group.getItems()) {
			size += item.getSize();
		}
		return size;
	}
}