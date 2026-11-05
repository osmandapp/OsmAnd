package net.osmand.plus.myplaces.favorites.dialogs;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.utils.AndroidUtils;

public class FavoriteStatsViewHolder extends RecyclerView.ViewHolder {

	private final OsmandApplication app;
	private final TextView stats;

	public FavoriteStatsViewHolder(@NonNull OsmandApplication app, @NonNull View view) {
		super(view);
		this.app = app;
		stats = view.findViewById(R.id.stats);
	}

	public void bindView(@NonNull FavoriteFolderAnalysis folderAnalysis) {
		stats.setText(getFormattedStats(folderAnalysis));
	}

	@NonNull
	private String getFormattedStats(@NonNull FavoriteFolderAnalysis analysis) {
		StringBuilder builder = new StringBuilder();

		if (analysis.getFoldersCount() > 0) {
			appendField(builder, app.getString(R.string.folder), String.valueOf(analysis.getFoldersCount()));
		}
		appendField(builder, app.getString(R.string.shared_string_gpx_points), String.valueOf(analysis.getPointsCount()));
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