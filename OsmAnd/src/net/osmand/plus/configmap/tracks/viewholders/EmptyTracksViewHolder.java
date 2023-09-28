package net.osmand.plus.configmap.tracks.viewholders;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.widgets.dialogbutton.DialogButton;

public class EmptyTracksViewHolder extends RecyclerView.ViewHolder {

	protected final OsmandApplication app;
	protected final TextView title;
	protected final TextView description;
	protected final ImageView icon;
	protected final DialogButton button;

	public EmptyTracksViewHolder(@NonNull View view, @Nullable EmptyTracksListener listener) {
		super(view);
		app = (OsmandApplication) itemView.getContext().getApplicationContext().getApplicationContext();

		title = view.findViewById(R.id.title);
		description = view.findViewById(R.id.description);
		icon = view.findViewById(R.id.icon);
		button = view.findViewById(R.id.action_button);
		button.setOnClickListener(v -> {
			if (listener != null) {
				listener.importTracks();
			}
		});
	}

	public void bindView() {
		title.setText(R.string.empty_tracks);
		description.setText(R.string.empty_tracks_description);
		icon.setImageDrawable(app.getUIUtilities().getThemedIcon(R.drawable.ic_action_folder_open));
		button.setTitleId(R.string.shared_string_import);
	}

	public interface EmptyTracksListener {
		void importTracks();
	}
}
