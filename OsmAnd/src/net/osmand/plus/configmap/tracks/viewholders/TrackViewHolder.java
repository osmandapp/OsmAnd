package net.osmand.plus.configmap.tracks.viewholders;

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
import net.osmand.plus.configmap.tracks.SelectedTracksHelper;
import net.osmand.plus.configmap.tracks.TracksFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.track.GpxAppearanceAdapter;
import net.osmand.plus.track.helpers.GPXDatabase.GpxDataItem;
import net.osmand.plus.track.helpers.GPXInfo;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.Algorithms;

import java.text.DateFormat;
import java.util.Collections;
import java.util.Date;

public class TrackViewHolder extends RecyclerView.ViewHolder {

	private final OsmandApplication app;
	private final TracksFragment fragment;
	private final boolean nightMode;

	private final TextView title;

	private final TextView folder;
	private final ImageView trackIcon;
	private final CompoundButton checkbox;
	private final View analysisRow;
	private final TextView dateSizeDetails;
	private final View divider;
	private final View folderDivider;

	public TrackViewHolder(@NonNull View itemView, @NonNull TracksFragment fragment, boolean nightMode) {
		super(itemView);
		this.fragment = fragment;
		this.nightMode = nightMode;
		app = (OsmandApplication) itemView.getContext().getApplicationContext();

		title = itemView.findViewById(R.id.title);
		checkbox = itemView.findViewById(R.id.checkbox);
		trackIcon = itemView.findViewById(R.id.track_icon);
		folder = itemView.findViewById(R.id.folder);
		analysisRow = itemView.findViewById(R.id.analysis_row);
		dateSizeDetails = itemView.findViewById(R.id.date_and_size_details);
		divider = itemView.findViewById(R.id.divider);
		folderDivider = itemView.findViewById(R.id.folder_divider);
	}

	public void bindView(@NonNull GPXInfo gpxInfo, @Nullable String folderName, boolean lastItem) {
		title.setText(gpxInfo.getName());
		folder.setText(Algorithms.capitalizeFirstLetter(folderName));

		SelectedTracksHelper helper = fragment.getSelectedTracksHelper();
		boolean selected = helper.getSelectedTracks().contains(gpxInfo);

		checkbox.setChecked(selected);
		int activeColor = app.getSettings().getApplicationMode().getProfileColor(nightMode);
		UiUtilities.setupCompoundButton(selected, activeColor, checkbox);
		itemView.setOnClickListener(v -> fragment.onGpxInfosSelected(Collections.singleton(gpxInfo), !selected));

		AndroidUiHelper.updateVisibility(divider, !lastItem);
		AndroidUiHelper.updateVisibility(folder, folderName != null);
		AndroidUiHelper.updateVisibility(folderDivider, folderName != null);
	}

	public void bindInfoRow(@NonNull GPXInfo gpxInfo, @NonNull GpxDataItem dataItem) {
		int color = dataItem.getColor();
		if (color == 0) {
			color = GpxAppearanceAdapter.getTrackColor(app);
		}
		trackIcon.setImageDrawable(getTrackIcon(app, dataItem.getWidth(), dataItem.isShowArrows(), color));

		GPXTrackAnalysis analysis = dataItem.getAnalysis();
		if (analysis == null) {
			String date = "";
			String size = "";
			long fileSize = gpxInfo.getFileSize();
			if (fileSize >= 0) {
				size = AndroidUtils.formatSize(app, fileSize);
			}
			DateFormat format = app.getResourceManager().getDateFormat();
			long lastModified = gpxInfo.getLastModified();
			if (lastModified > 0) {
				date = (format.format(new Date(lastModified)));
			}
			dateSizeDetails.setText(date + " \u2022 " + size);
		} else {
			TextView distance = itemView.findViewById(R.id.distance);
			TextView time = itemView.findViewById(R.id.time);
			TextView pointsCount = itemView.findViewById(R.id.points_count);

			pointsCount.setText(String.valueOf(analysis.wptPoints));
			distance.setText(OsmAndFormatter.getFormattedDistance(analysis.totalDistance, app));

			int duration = (int) (analysis.timeSpan / 1000.0f + 0.5);
			time.setText(analysis.isTimeSpecified() ? Algorithms.formatDuration(duration, app.accessibilityEnabled()) : null);
		}
		AndroidUiHelper.updateVisibility(analysisRow, analysis != null);
		AndroidUiHelper.updateVisibility(dateSizeDetails, analysis == null);
	}
}
