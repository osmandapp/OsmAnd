package net.osmand.plus.myplaces.tracks.dialogs.viewholders;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.shared.gpx.filters.TrackFolderAnalysis;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.util.Algorithms;

public class FolderStatsViewHolder extends RecyclerView.ViewHolder {

	private final OsmandApplication app;
	private final TextView stats;

	public FolderStatsViewHolder(@NonNull OsmandApplication app, @NonNull View view) {
		super(view);
		this.app = app;
		stats = view.findViewById(R.id.stats);
	}

	public void bindView(@NonNull TrackFolderAnalysis folderAnalysis) {
		stats.setText(getFormattedStats(folderAnalysis));
	}

	@NonNull
	private String getFormattedStats(@NonNull TrackFolderAnalysis analysis) {
		StringBuilder builder = new StringBuilder();
		appendField(builder, app.getString(R.string.shared_string_tracks), String.valueOf(analysis.getTracksCount()));
		appendField(builder, app.getString(R.string.distance), OsmAndFormatter.getFormattedDistance(analysis.getTotalDistance(), app));
		appendField(builder, app.getString(R.string.shared_string_uphill), OsmAndFormatter.getFormattedAlt(analysis.getDiffElevationUp(), app));
		appendField(builder, app.getString(R.string.shared_string_downhill), OsmAndFormatter.getFormattedAlt(analysis.getDiffElevationDown(), app));
		appendField(builder, app.getString(R.string.duration), Algorithms.formatDuration(analysis.getTimeSpan(), app.accessibilityEnabled()));
		appendField(builder, app.getString(R.string.shared_string_size), AndroidUtils.formatSize(app, analysis.getFileSize()));
		return builder.toString();
	}

	private void appendField(@NonNull StringBuilder builder, @NonNull String field, @NonNull String value) {
		if (builder.length() > 0) {
			builder.append(" · ");
		}
		builder.append(app.getString(R.string.ltr_or_rtl_combine_via_colon, field, value));
	}
}