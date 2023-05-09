package net.osmand.plus.myplaces.tracks.dialogs.viewholders;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.R;
import net.osmand.plus.track.data.TrackFolder;
import net.osmand.plus.track.helpers.GpxUiHelper;

public class TrackFolderViewHolder extends TracksGroupViewHolder {

	private final FolderSelectionListener listener;

	public TrackFolderViewHolder(@NonNull View view, @Nullable FolderSelectionListener listener,
	                             boolean nightMode, boolean selectionMode) {
		super(view, nightMode, selectionMode);
		this.listener = listener;
	}

	public void bindView(@NonNull TrackFolder folder) {
		title.setText(folder.getName());
		description.setText(GpxUiHelper.getFolderDescription(app, folder));
		icon.setImageDrawable(uiUtilities.getPaintedIcon(R.drawable.ic_action_folder, folder.getColor()));

		boolean selected = listener != null && listener.isFolderSelected(folder);
		checkbox.setChecked(selected);

		itemView.setOnClickListener(v -> {
			if (listener != null) {
				listener.onFolderSelected(folder);
			}
		});
	}

	public interface FolderSelectionListener {

		default boolean isFolderSelected(@NonNull TrackFolder folder) {
			return false;
		}


		default void onFolderSelected(@NonNull TrackFolder folder) {

		}
	}
}
