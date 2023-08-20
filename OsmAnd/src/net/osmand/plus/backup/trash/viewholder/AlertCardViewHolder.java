package net.osmand.plus.backup.trash.viewholder;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.R;

public class AlertCardViewHolder extends RecyclerView.ViewHolder {

	public View actionButton;

	public AlertCardViewHolder(@NonNull View itemView) {
		super(itemView);
		actionButton = itemView.findViewById(R.id.action_button);
	}

}
