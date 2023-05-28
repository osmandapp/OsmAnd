package net.osmand.plus.myplaces.tracks.dialogs.viewholders;

import android.text.SpannableStringBuilder;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.track.data.TrackFolder.FolderStats;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.util.Algorithms;

public class FolderStatsViewHolder extends RecyclerView.ViewHolder {
	protected final OsmandApplication app;
	protected final TextView stats;
	protected final boolean nightMode;

	public FolderStatsViewHolder(@NonNull View view, boolean nightMode) {
		super(view);
		this.nightMode = nightMode;
		app = (OsmandApplication) itemView.getContext().getApplicationContext().getApplicationContext();
		stats = view.findViewById(R.id.stats);
	}

	public void bindView(@NonNull FolderStats folderStats) {
		stats.setText(getFormattedStats(folderStats));
	}

	private String getFormattedStats(FolderStats stats) {
		SpannableStringBuilder builder = new SpannableStringBuilder(app.getString(R.string.shared_string_tracks) + " - " + stats.tracksCount + ", ");
		appendField(builder, app.getString(R.string.distance), OsmAndFormatter.getFormattedDistance(stats.totalDistance, app), false);
		appendField(builder, app.getString(R.string.shared_string_uphill), OsmAndFormatter.getFormattedAlt(stats.diffElevationUp, app), false);
		appendField(builder, app.getString(R.string.shared_string_downhill), OsmAndFormatter.getFormattedAlt(stats.diffElevationDown, app), false);
		appendField(builder, app.getString(R.string.duration), Algorithms.formatDuration(stats.duration, app.accessibilityEnabled()), false);
		appendField(builder, app.getString(R.string.shared_string_size), AndroidUtils.formatSize(app, stats.fileSize), true);
		return builder.toString();
	}

	private void appendField(@NonNull SpannableStringBuilder builder, @NonNull String field, @NonNull String value, boolean lastItem) {
		builder.append(field.toLowerCase()).append(" - ").append(value);
		if (lastItem) {
			builder.append(".");
		} else {
			builder.append(", ");
		}
	}
}