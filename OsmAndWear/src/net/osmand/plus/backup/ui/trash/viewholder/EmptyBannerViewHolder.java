package net.osmand.plus.backup.ui.trash.viewholder;

import static net.osmand.plus.backup.ui.trash.CloudTrashController.DAYS_FOR_TRASH_CLEARING;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.R;

public class EmptyBannerViewHolder extends RecyclerView.ViewHolder {

	public EmptyBannerViewHolder(@NonNull View itemView) {
		super(itemView);

		Context context = itemView.getContext();
		TextView description = itemView.findViewById(R.id.description);
		description.setText(context.getString(R.string.trash_is_empty_banner_desc, String.valueOf(DAYS_FOR_TRASH_CLEARING)));
	}
}
