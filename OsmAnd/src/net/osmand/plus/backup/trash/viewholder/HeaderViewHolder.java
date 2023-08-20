package net.osmand.plus.backup.trash.viewholder;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;

public class HeaderViewHolder extends RecyclerView.ViewHolder {

	public TextView title;

	public HeaderViewHolder(@NonNull View itemView) {
		super(itemView);
		title = itemView.findViewById(R.id.title);
		AndroidUiHelper.updateVisibility(itemView.findViewById(R.id.count), false);
	}

}
