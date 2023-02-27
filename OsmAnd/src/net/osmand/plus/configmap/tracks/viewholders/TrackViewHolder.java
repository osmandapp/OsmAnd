package net.osmand.plus.configmap.tracks.viewholders;

import static net.osmand.plus.configmap.tracks.TracksSortMode.DATE_ASCENDING;
import static net.osmand.plus.configmap.tracks.TracksSortMode.DATE_DESCENDING;
import static net.osmand.plus.configmap.tracks.TracksSortMode.DISTANCE_ASCENDING;
import static net.osmand.plus.configmap.tracks.TracksSortMode.DISTANCE_DESCENDING;
import static net.osmand.plus.configmap.tracks.TracksSortMode.DURATION_ASCENDING;
import static net.osmand.plus.configmap.tracks.TracksSortMode.DURATION_DESCENDING;
import static net.osmand.plus.configmap.tracks.TracksSortMode.NAME_ASCENDING;
import static net.osmand.plus.configmap.tracks.TracksSortMode.NAME_DESCENDING;
import static net.osmand.plus.track.fragments.TrackAppearanceFragment.getTrackIcon;
import static net.osmand.plus.utils.ColorUtilities.getSecondaryTextColor;

import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
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
import net.osmand.plus.configmap.tracks.TrackTab;
import net.osmand.plus.configmap.tracks.TracksFragment;
import net.osmand.plus.configmap.tracks.TracksSortMode;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.track.GpxAppearanceAdapter;
import net.osmand.plus.track.helpers.GPXDatabase.GpxDataItem;
import net.osmand.plus.track.helpers.GPXInfo;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.style.CustomTypefaceSpan;
import net.osmand.util.Algorithms;

import java.io.File;
import java.text.DateFormat;
import java.util.Collections;
import java.util.Date;

public class TrackViewHolder extends RecyclerView.ViewHolder {

	private final OsmandApplication app;
	private final TracksFragment fragment;
	private final boolean nightMode;

	private final TextView title;
	private final TextView description;
	private final ImageView imageView;
	private final CompoundButton checkbox;
	private final View divider;

	public TrackViewHolder(@NonNull View itemView, @NonNull TracksFragment fragment, boolean nightMode) {
		super(itemView);
		this.fragment = fragment;
		this.nightMode = nightMode;
		app = (OsmandApplication) itemView.getContext().getApplicationContext();

		title = itemView.findViewById(R.id.title);
		description = itemView.findViewById(R.id.description);
		checkbox = itemView.findViewById(R.id.checkbox);
		imageView = itemView.findViewById(R.id.track_icon);
		divider = itemView.findViewById(R.id.divider);
	}

	public void bindView(@NonNull GPXInfo gpxInfo, boolean lastItem) {
		title.setText(gpxInfo.getName());

		SelectedTracksHelper helper = fragment.getSelectedTracksHelper();
		boolean selected = helper.getSelectedTracks().contains(gpxInfo);

		checkbox.setChecked(selected);
		int activeColor = app.getSettings().getApplicationMode().getProfileColor(nightMode);
		UiUtilities.setupCompoundButton(selected, activeColor, checkbox);
		itemView.setOnClickListener(v -> fragment.onGpxInfosSelected(Collections.singleton(gpxInfo), !selected));

		AndroidUiHelper.updateVisibility(divider, !lastItem);
	}

	public void bindInfoRow(@NonNull TrackTab trackTab, @NonNull GPXInfo gpxInfo) {
		setupIcon(gpxInfo);

		TracksSortMode sortMode = trackTab.getSortMode();
		GPXTrackAnalysis analysis = gpxInfo.getDataItem().getAnalysis();
		if (analysis != null) {
			SpannableStringBuilder builder = new SpannableStringBuilder();
			if (sortMode == NAME_ASCENDING || sortMode == NAME_DESCENDING) {
				appendNameDescription(builder, trackTab, gpxInfo, analysis);
			} else if (sortMode == DATE_ASCENDING || sortMode == DATE_DESCENDING) {
				appendDateDescription(builder, trackTab, gpxInfo, analysis);
			} else if (sortMode == DISTANCE_ASCENDING || sortMode == DISTANCE_DESCENDING) {
				appendDistanceDescription(builder, trackTab, gpxInfo, analysis);
			} else if (sortMode == DURATION_ASCENDING || sortMode == DURATION_DESCENDING) {
				appendDurationDescription(builder, trackTab, gpxInfo, analysis);
			}
			description.setText(builder);
		}
	}

	private void setupIcon(@NonNull GPXInfo gpxInfo) {
		GpxDataItem dataItem = gpxInfo.getDataItem();
		int color = dataItem.getColor();
		if (color == 0) {
			color = GpxAppearanceAdapter.getTrackColor(app);
		}
		imageView.setImageDrawable(getTrackIcon(app, dataItem.getWidth(), dataItem.isShowArrows(), color));
	}

	private void appendNameDescription(@NonNull SpannableStringBuilder builder, @NonNull TrackTab trackTab, @NonNull GPXInfo gpxInfo, @NonNull GPXTrackAnalysis analysis) {
		builder.append(OsmAndFormatter.getFormattedDistance(analysis.totalDistance, app));
		if (analysis.isTimeSpecified()) {
			builder.append(" \u2022 ");
			appendDuration(builder, analysis);
		}
		appendPoints(builder, analysis);
		appendFolderName(builder, trackTab, gpxInfo);
	}

	private void appendDateDescription(@NonNull SpannableStringBuilder builder, @NonNull TrackTab trackTab, @NonNull GPXInfo gpxInfo, @NonNull GPXTrackAnalysis analysis) {
		long lastModified = gpxInfo.getLastModified();
		if (lastModified > 0) {
			DateFormat format = app.getResourceManager().getDateFormat();
			builder.append(format.format(new Date(lastModified)));
			setupTextSpan(builder);
			builder.append(" \u007C ");
		}
		builder.append(OsmAndFormatter.getFormattedDistance(analysis.totalDistance, app));
		if (analysis.isTimeSpecified()) {
			builder.append(" \u2022 ");
			appendDuration(builder, analysis);
		}
		appendPoints(builder, analysis);
	}

	private void appendDistanceDescription(@NonNull SpannableStringBuilder builder, @NonNull TrackTab trackTab, @NonNull GPXInfo gpxInfo, @NonNull GPXTrackAnalysis analysis) {
		builder.append(OsmAndFormatter.getFormattedDistance(analysis.totalDistance, app));
		setupTextSpan(builder);

		if (analysis.isTimeSpecified()) {
			builder.append(" \u2022 ");
			appendDuration(builder, analysis);
		}
		appendPoints(builder, analysis);
		appendFolderName(builder, trackTab, gpxInfo);
	}

	private void appendDurationDescription(@NonNull SpannableStringBuilder builder, @NonNull TrackTab trackTab, @NonNull GPXInfo gpxInfo, @NonNull GPXTrackAnalysis analysis) {
		if (analysis.isTimeSpecified()) {
			appendDuration(builder, analysis);
			setupTextSpan(builder);
			builder.append(" \u2022 ");
		}
		builder.append(OsmAndFormatter.getFormattedDistance(analysis.totalDistance, app));

		appendPoints(builder, analysis);
		appendFolderName(builder, trackTab, gpxInfo);
	}

	private void appendDuration(@NonNull SpannableStringBuilder builder, @NonNull GPXTrackAnalysis analysis) {
		if (analysis.isTimeSpecified()) {
			int duration = (int) (analysis.timeSpan / 1000.0f + 0.5);
			builder.append(Algorithms.formatDuration(duration, app.accessibilityEnabled()));
		}
	}

	private void appendPoints(@NonNull SpannableStringBuilder builder, @NonNull GPXTrackAnalysis analysis) {
		if (analysis.wptPoints > 0) {
			builder.append(" \u2022 ");
			builder.append(String.valueOf(analysis.wptPoints));
		}
	}

	private void appendFolderName(@NonNull SpannableStringBuilder builder, @NonNull TrackTab trackTab, @NonNull GPXInfo gpxInfo) {
		String folderName = getFolderName(trackTab, gpxInfo);
		if (!Algorithms.isEmpty(folderName)) {
			builder.append(" \u007C ");
			builder.append(Algorithms.capitalizeFirstLetter(folderName));
		}
	}

	@Nullable
	private String getFolderName(@NonNull TrackTab trackTab, @NonNull GPXInfo gpxInfo) {
		String folderName = null;
		File file = gpxInfo.getFile();
		if (trackTab.type.shouldShowFolder() && file != null) {
			String[] path = file.getAbsolutePath().split(File.separator);
			folderName = path.length > 1 ? path[path.length - 2] : null;
		}
		return folderName;
	}

	private void setupTextSpan(@NonNull SpannableStringBuilder builder) {
		int length = builder.length();
		builder.setSpan(new ForegroundColorSpan(getSecondaryTextColor(app, nightMode)), 0, length, 0);
		builder.setSpan(new CustomTypefaceSpan(FontCache.getRobotoMedium(app)), 0, length, 0);
	}
}
