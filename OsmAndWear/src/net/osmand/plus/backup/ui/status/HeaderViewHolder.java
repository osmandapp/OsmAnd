package net.osmand.plus.backup.ui.status;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.R;
import net.osmand.plus.backup.ui.ChangesFragment.RecentChangesType;

public class HeaderViewHolder extends RecyclerView.ViewHolder {

	private final TextView title;
	private final TextView countTextView;

	public HeaderViewHolder(@NonNull View itemView) {
		super(itemView);
		title = itemView.findViewById(R.id.title);
		countTextView = itemView.findViewById(R.id.count);
	}

	public void bindView(@NonNull RecentChangesType tabType, int count) {
		title.setText(getTitle(tabType));
		countTextView.setText(String.valueOf(count));
	}

	@Nullable
	private String getTitle(@NonNull RecentChangesType tabType) {
		Context context = itemView.getContext();
		switch (tabType) {
			case RECENT_CHANGES_LOCAL:
				return context.getString(R.string.cloud_recent_changes);
			case RECENT_CHANGES_REMOTE:
				return context.getString(R.string.download_tab_updates);
			case RECENT_CHANGES_CONFLICTS:
				return context.getString(R.string.shared_string_unsynced);
			default:
				return null;
		}
	}
}