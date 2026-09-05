package net.osmand.plus.backup.ui.status;

import static net.osmand.plus.widgets.dialogbutton.DialogButtonType.SECONDARY;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.backup.ui.ChangesFragment.RecentChangesType;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.UiUtilities;

public class EmptyStateViewHolder extends RecyclerView.ViewHolder {

	private final TextView title;
	private final TextView description;
	private final ImageView imageView;
	private final View button;

	private final RecentChangesType tabType;

	public EmptyStateViewHolder(@NonNull View itemView, @NonNull RecentChangesType tabType) {
		super(itemView);
		this.tabType = tabType;
		title = itemView.findViewById(R.id.title);
		description = itemView.findViewById(R.id.description);
		imageView = itemView.findViewById(R.id.icon);
		button = itemView.findViewById(R.id.button);
	}

	public void bindView(boolean nightMode) {
		title.setText(getTitle());
		description.setText(R.string.cloud_all_changes_uploaded_descr);

		OsmandApplication app = (OsmandApplication) imageView.getContext().getApplicationContext();
		imageView.setImageDrawable(app.getUIUtilities().getIcon(R.drawable.ic_action_cloud_smile_face_colored));

		UiUtilities.setupDialogButton(nightMode, button, SECONDARY, R.string.check_for_updates);
		AndroidUiHelper.updateVisibility(button, false);
	}

	@Nullable
	private String getTitle() {
		Context context = itemView.getContext();
		switch (tabType) {
			case RECENT_CHANGES_LOCAL:
				return context.getString(R.string.cloud_all_changes_uploaded);
			case RECENT_CHANGES_REMOTE:
				return context.getString(R.string.cloud_all_changes_downloaded);
			case RECENT_CHANGES_CONFLICTS:
				return context.getString(R.string.cloud_no_conflicts);
			default:
				return null;
		}
	}
}
