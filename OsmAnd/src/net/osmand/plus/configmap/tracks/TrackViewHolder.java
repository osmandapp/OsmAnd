package net.osmand.plus.configmap.tracks;

import static net.osmand.plus.track.fragments.TrackAppearanceFragment.getTrackIcon;

import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.configmap.tracks.TracksAdapter.TracksVisibilityListener;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.track.GpxAppearanceAdapter;
import net.osmand.plus.track.helpers.GPXDatabase.GpxDataItem;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.Algorithms;

import java.io.File;
import java.text.DateFormat;
import java.util.Date;

class TrackViewHolder extends RecyclerView.ViewHolder {

	private final OsmandApplication app;

	private final TextView title;
	private final TextView distance;
	private final TextView time;
	private final TextView pointsCount;
	private final TextView folder;
	private final ImageView trackIcon;
	private final CompoundButton checkbox;
	private final View divider;
	private final View folderDivider;
	private final boolean nightMode;

	public TrackViewHolder(@NonNull View itemView, boolean nightMode) {
		super(itemView);
		this.nightMode = nightMode;
		app = (OsmandApplication) itemView.getContext().getApplicationContext();

		title = itemView.findViewById(R.id.title);
		distance = itemView.findViewById(R.id.distance);
		time = itemView.findViewById(R.id.time);
		pointsCount = itemView.findViewById(R.id.points_count);
		folder = itemView.findViewById(R.id.folder);
		trackIcon = itemView.findViewById(R.id.track_icon);
		checkbox = itemView.findViewById(R.id.checkbox);
		divider = itemView.findViewById(R.id.divider);
		folderDivider = itemView.findViewById(R.id.folder_divider);
	}

	public void bindView(@NonNull TracksVisibilityListener listener, @NonNull GPXInfo gpxInfo,
	                     @Nullable String folderName, boolean lastItem) {
		title.setText(gpxInfo.getName());
		folder.setText(folderName);

		File file = gpxInfo.getFile();
		GpxDataItem dataItem = app.getGpxDbHelper().getItem(file, null);
		GPXTrackAnalysis analysis = dataItem != null ? dataItem.getAnalysis() : null;

		if (dataItem != null) {
			String width = dataItem.getWidth();
			boolean showArrows = dataItem.isShowArrows();
			int color = dataItem.getColor();
			if (color == 0) {
				color = GpxAppearanceAdapter.getTrackColor(app);
			}
			trackIcon.setImageDrawable(getTrackIcon(app, width, showArrows, color));
		}

		boolean selected = listener.isTrackSelected(gpxInfo);

		if (analysis == null) {
			String date = "";
			String size = "";
			if (gpxInfo.getFileSize() >= 0) {
				size = AndroidUtils.formatSize(app, gpxInfo.getFileSize());
			}
			DateFormat df = app.getResourceManager().getDateFormat();
			long fd = gpxInfo.getLastModified();
			if (fd > 0) {
				date = (df.format(new Date(fd)));
			}
//			TextView sizeText = v.findViewById(R.id.date_and_size_details);
//			sizeText.setText(date + " \u2022 " + size);
		} else {
			pointsCount.setText(analysis.wptPoints + "");
			distance.setText(OsmAndFormatter.getFormattedDistance(analysis.totalDistance, app));

			if (analysis.isTimeSpecified()) {
				time.setText(Algorithms.formatDuration((int) (analysis.timeSpan / 1000.0f + 0.5), app.accessibilityEnabled()) + "");
			} else {
				time.setText("");
			}
		}
		checkbox.setChecked(selected);
		int activeColor = app.getSettings().getApplicationMode().getProfileColor(nightMode);
		UiUtilities.setupCompoundButton(selected, activeColor, checkbox);
		itemView.setOnClickListener(v -> listener.onTrackItemSelected(gpxInfo, !selected));

		AndroidUiHelper.updateVisibility(divider, !lastItem);
		AndroidUiHelper.updateVisibility(folder, folderName != null);
		AndroidUiHelper.updateVisibility(folderDivider, folderName != null);
	}
}
