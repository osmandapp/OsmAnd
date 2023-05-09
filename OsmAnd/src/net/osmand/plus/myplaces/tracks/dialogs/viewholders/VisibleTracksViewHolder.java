package net.osmand.plus.myplaces.tracks.dialogs.viewholders;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.R;
import net.osmand.plus.track.helpers.GpxSelectionHelper;

public class VisibleTracksViewHolder extends TracksGroupViewHolder {

	private final GpxSelectionHelper selectedGpxHelper;
	private final VisibleTracksListener listener;

	public VisibleTracksViewHolder(@NonNull View view, @Nullable VisibleTracksListener listener,
	                               boolean nightMode, boolean selectionMode) {
		super(view, nightMode, selectionMode);
		this.listener = listener;
		selectedGpxHelper = app.getSelectedGpxHelper();
	}

	public void bindView() {
		title.setText(R.string.shared_string_visible_on_map);
		icon.setImageDrawable(uiUtilities.getActiveIcon(R.drawable.ic_show_on_map, nightMode));

		int count = selectedGpxHelper.getSelectedGPXFiles().size();
		String tracks = app.getString(R.string.shared_string_gpx_tracks);
		description.setText(app.getString(R.string.ltr_or_rtl_combine_via_space, tracks, String.valueOf(count)));

		itemView.setOnClickListener(v -> {
			if (listener != null) {
				listener.showTracksDialog();
			}
		});
	}

	public interface VisibleTracksListener {

		void showTracksDialog();

	}
}
