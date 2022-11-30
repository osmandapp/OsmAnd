package net.osmand.plus.backup.ui.status;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.R;
import net.osmand.plus.backup.ui.ChangesFragment.ChangesTabType;

public class ListHeaderViewHolder extends RecyclerView.ViewHolder {
	final TextView title;
	final TextView countTextView;

	public ListHeaderViewHolder(@NonNull View itemView) {
		super(itemView);
		title = itemView.findViewById(R.id.title);
		countTextView = itemView.findViewById(R.id.count);
	}

	public void bindView(@NonNull ChangesTabType tabType, int count) {
		if (tabType.equals(ChangesTabType.LOCAL_CHANGES)) {
			title.setText(R.string.shared_string_changes);
		} else if (tabType.equals(ChangesTabType.CLOUD_CHANGES)) {
			title.setText(R.string.download_tab_updates);
		} else if (tabType.equals(ChangesTabType.CONFLICTS)) {
			title.setText(R.string.shared_string_unsynced);
		}
		countTextView.setText(String.valueOf(count));
	}
}
