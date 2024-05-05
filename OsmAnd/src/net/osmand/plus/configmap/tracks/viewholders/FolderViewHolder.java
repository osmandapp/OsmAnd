package net.osmand.plus.configmap.tracks.viewholders;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.track.data.TrackFolder;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.OsmAndFormatter;

import java.text.DateFormat;
import java.util.Date;

public class FolderViewHolder extends RecyclerView.ViewHolder {

	private final OsmandApplication app;
	private final TrackViewHolder.TrackSelectionListener listener;
	private final boolean nightMode;

	private final TextView title;
	private final TextView description;
	private final ImageView imageView;
	private final View menuButton;
	private final View divider;
	private final ImageView directionIcon;

	public FolderViewHolder(@NonNull View itemView, @Nullable TrackViewHolder.TrackSelectionListener listener, boolean nightMode) {
		super(itemView);
		this.app = (OsmandApplication) itemView.getContext().getApplicationContext();

		this.listener = listener;
		this.nightMode = nightMode;

		title = itemView.findViewById(R.id.title);
		description = itemView.findViewById(R.id.description);
		directionIcon = itemView.findViewById(R.id.direction_icon);
		menuButton = itemView.findViewById(R.id.menu_button);
		imageView = itemView.findViewById(R.id.icon);
		divider = itemView.findViewById(R.id.divider);
	}

	public void bindView(@NonNull TrackFolder trackFolder, boolean showDivider) {
		title.setText(trackFolder.getName(app));
		imageView.setImageDrawable(app.getUIUtilities().getPaintedIcon(R.drawable.ic_action_folder, ColorUtilities.getDefaultIconColor(app, nightMode)));

		int margin = app.getResources().getDimensionPixelSize(R.dimen.content_padding);
		ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) imageView.getLayoutParams();
		AndroidUtils.setMargins(params, margin, 0, margin, 0);

		itemView.setOnClickListener(v -> {
			if (listener != null) {
				listener.onTrackFolderSelected(trackFolder);
			}
		});

		long lastModified = trackFolder.getLastModified();
		if (lastModified > 0) {
			DateFormat format = OsmAndFormatter.getDateFormat(app);
			description.setText(format.format(new Date(lastModified)));
		}

		AndroidUiHelper.updateVisibility(divider, showDivider);
		AndroidUiHelper.updateVisibility(menuButton, false);
		AndroidUiHelper.updateVisibility(directionIcon, false);
		AndroidUiHelper.updateVisibility(itemView.findViewById(R.id.checkbox_container), false);
	}
}
