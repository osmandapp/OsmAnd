package net.osmand.plus.myplaces.tracks.dialogs;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.track.data.TrackFolder;
import net.osmand.plus.track.helpers.GpxUiHelper;

public class TrackFolderViewHolder extends RecyclerView.ViewHolder {

	private final OsmandApplication app;

	private final FolderSelectionListener listener;
	private final boolean nightMode;

	private final TextView title;
	private final TextView description;
	private final ImageView icon;
	private final View selectionButton;
	private final View actionsButton;

	public TrackFolderViewHolder(@NonNull View view, @NonNull FolderSelectionListener listener, boolean nightMode) {
		super(view);
		this.listener = listener;
		this.nightMode = nightMode;
		app = (OsmandApplication) itemView.getContext().getApplicationContext().getApplicationContext();

		title = view.findViewById(R.id.title);
		description = view.findViewById(R.id.description);
		icon = view.findViewById(R.id.icon);
		selectionButton = view.findViewById(R.id.selection_button);
		actionsButton = view.findViewById(R.id.actions_button);
	}

	public void bindView(@NonNull TrackFolder folder) {
		title.setText(folder.getName());
		description.setText(GpxUiHelper.getFolderDescription(app, folder));

		itemView.setOnClickListener(v -> listener.onFolderSelected(folder));
		actionsButton.setOnClickListener(v -> listener.onFolderOptionsSelected(folder));

		AndroidUiHelper.updateVisibility(selectionButton, false);
	}

	public interface FolderSelectionListener {
		void onFolderSelected(@NonNull TrackFolder folder);

		void onFolderOptionsSelected(@NonNull TrackFolder folder);
	}
}
