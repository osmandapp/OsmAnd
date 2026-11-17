package net.osmand.plus.myplaces.tracks.dialogs.viewholders;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.R;
import net.osmand.plus.configmap.tracks.viewholders.EmptyTracksViewHolder;

public class EmptyFolderViewHolder extends EmptyTracksViewHolder {

	public EmptyFolderViewHolder(@NonNull View view, @Nullable EmptyTracksListener listener) {
		super(view, listener);
	}

	@Override
	public void bindView() {
		super.bindView();
		title.setText(R.string.tracks_empty_folder);
		description.setText(R.string.tracks_empty_folder_description);
	}
}
