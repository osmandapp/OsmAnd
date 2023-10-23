package net.osmand.plus.backup.ui.trash.viewholder;

import static net.osmand.plus.backup.ui.trash.CloudTrashController.DAYS_FOR_TRASH_CLEARING;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.R;
import net.osmand.plus.backup.ui.trash.CloudTrashController;

public class AlertViewHolder extends RecyclerView.ViewHolder {

	public AlertViewHolder(@NonNull View itemView, @NonNull CloudTrashController controller) {
		super(itemView);

		Context context = itemView.getContext();
		TextView title = itemView.findViewById(R.id.title);
		title.setText(context.getString(R.string.trash_alert_card_desc, String.valueOf(DAYS_FOR_TRASH_CLEARING)));
		itemView.findViewById(R.id.action_button).setOnClickListener(v -> controller.showClearConfirmationDialog());
	}
}