package net.osmand.plus.download.local.dialogs.viewholders;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.R;
import net.osmand.plus.download.local.LocalCategory;
import net.osmand.plus.utils.AndroidUtils;

public class CategoryViewHolder extends RecyclerView.ViewHolder {

	private final TextView title;
	private final TextView count;

	public CategoryViewHolder(@NonNull View itemView) {
		super(itemView);
		title = itemView.findViewById(R.id.title);
		count = itemView.findViewById(R.id.count);
	}

	public void bindView(@NonNull LocalCategory category) {
		title.setText(category.getName(itemView.getContext()));
		count.setText(AndroidUtils.formatSize(itemView.getContext(), category.getSize()));
	}
}