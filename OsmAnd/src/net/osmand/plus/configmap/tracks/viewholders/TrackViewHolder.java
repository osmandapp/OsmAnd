package net.osmand.plus.configmap.tracks.viewholders;

import static net.osmand.plus.configmap.tracks.TracksSortMode.DATE_ASCENDING;
import static net.osmand.plus.configmap.tracks.TracksSortMode.DATE_DESCENDING;
import static net.osmand.plus.configmap.tracks.TracksSortMode.DISTANCE_ASCENDING;
import static net.osmand.plus.configmap.tracks.TracksSortMode.DISTANCE_DESCENDING;
import static net.osmand.plus.configmap.tracks.TracksSortMode.DURATION_ASCENDING;
import static net.osmand.plus.configmap.tracks.TracksSortMode.DURATION_DESCENDING;
import static net.osmand.plus.configmap.tracks.TracksSortMode.LAST_MODIFIED;
import static net.osmand.plus.configmap.tracks.TracksSortMode.NAME_ASCENDING;
import static net.osmand.plus.configmap.tracks.TracksSortMode.NAME_DESCENDING;
import static net.osmand.plus.configmap.tracks.TracksSortMode.NEAREST;
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

import net.osmand.data.LatLon;
import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.gpx.GPXUtilities.WptPt;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.configmap.tracks.SelectedTracksHelper;
import net.osmand.plus.configmap.tracks.TrackItem;
import net.osmand.plus.configmap.tracks.TrackTab;
import net.osmand.plus.configmap.tracks.TracksAdapter;
import net.osmand.plus.configmap.tracks.TracksFragment;
import net.osmand.plus.configmap.tracks.TracksSortMode;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.track.GpxAppearanceAdapter;
import net.osmand.plus.track.helpers.GPXDatabase.GpxDataItem;
import net.osmand.plus.track.helpers.GpxDbHelper;
import net.osmand.plus.track.helpers.GpxDbHelper.GpxDataItemCallback;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.utils.UiUtilities.UpdateLocationViewCache;
import net.osmand.plus.widgets.style.CustomTypefaceSpan;
import net.osmand.util.Algorithms;

import java.io.File;
import java.text.DateFormat;
import java.util.Collections;
import java.util.Date;

public class TrackViewHolder extends RecyclerView.ViewHolder {

	private final OsmandApplication app;
	private final UiUtilities uiUtilities;
	private final GpxDbHelper gpxDbHelper;

	private final UpdateLocationViewCache locationViewCache;
	private final TracksFragment fragment;
	private final boolean nightMode;

	private final TextView title;
	private final TextView description;
	private final TextView distanceTv;
	private final ImageView imageView;
	private final CompoundButton checkbox;
	private final View divider;
	private final ImageView directionIcon;

	public TrackViewHolder(@NonNull View itemView, @NonNull TracksFragment fragment,
	                       @NonNull UpdateLocationViewCache viewCache, boolean nightMode) {
		super(itemView);
		this.app = (OsmandApplication) itemView.getContext().getApplicationContext();
		this.gpxDbHelper = app.getGpxDbHelper();
		this.uiUtilities = app.getUIUtilities();
		this.locationViewCache = viewCache;
		this.fragment = fragment;
		this.nightMode = nightMode;

		title = itemView.findViewById(R.id.title);
		description = itemView.findViewById(R.id.description);
		distanceTv = itemView.findViewById(R.id.distance);
		directionIcon = itemView.findViewById(R.id.direction_icon);
		checkbox = itemView.findViewById(R.id.checkbox);
		imageView = itemView.findViewById(R.id.track_icon);
		divider = itemView.findViewById(R.id.divider);
	}

	public void bindView(@NonNull TracksAdapter adapter, @NonNull TrackItem trackItem, boolean showDivider) {
		title.setText(trackItem.getName());

		SelectedTracksHelper helper = fragment.getSelectedTracksHelper();
		boolean selected = helper.getSelectedTracks().contains(trackItem);

		checkbox.setChecked(selected);
		int activeColor = app.getSettings().getApplicationMode().getProfileColor(nightMode);
		UiUtilities.setupCompoundButton(selected, activeColor, checkbox);
		itemView.setOnClickListener(v -> fragment.onTrackItemsSelected(Collections.singleton(trackItem), !selected));

		AndroidUiHelper.updateVisibility(divider, showDivider);
		bindInfoRow(adapter, trackItem);
	}

	public void bindInfoRow(@NonNull TracksAdapter adapter, @NonNull TrackItem trackItem) {
		TrackTab trackTab = adapter.getTrackTab();
		File file = trackItem.getFile();
		GpxDataItem dataItem = trackItem.getDataItem();
		if (dataItem != null) {
			buildDescriptionRow(trackTab, dataItem, trackItem);
		} else if (file != null) {
			dataItem = gpxDbHelper.getItem(file, new GpxDataItemCallback() {
				@Override
				public void onGpxDataItemReady(GpxDataItem item) {
					trackItem.setDataItem(item);
					if (item != null && fragment.isAdded()) {
						adapter.notifyItemChanged(getAdapterPosition());
					}
				}
			});
			if (dataItem != null) {
				trackItem.setDataItem(dataItem);
				buildDescriptionRow(trackTab, dataItem, trackItem);
			}
		}
	}

	private void buildDescriptionRow(@NonNull TrackTab trackTab, @NonNull GpxDataItem dataItem, @NonNull TrackItem trackItem) {
		setupIcon(dataItem);

		TracksSortMode sortMode = trackTab.getSortMode();
		GPXTrackAnalysis analysis = dataItem != null ? dataItem.getAnalysis() : null;
		if (analysis != null) {
			SpannableStringBuilder builder = new SpannableStringBuilder();
			if (sortMode == NAME_ASCENDING || sortMode == NAME_DESCENDING) {
				appendNameDescription(builder, trackTab, trackItem, analysis);
			} else if (sortMode == DATE_ASCENDING || sortMode == DATE_DESCENDING) {
				appendCreationTimeDescription(builder, trackTab, trackItem, analysis);
			} else if (sortMode == DISTANCE_ASCENDING || sortMode == DISTANCE_DESCENDING) {
				appendDistanceDescription(builder, trackTab, trackItem, analysis);
			} else if (sortMode == DURATION_ASCENDING || sortMode == DURATION_DESCENDING) {
				appendDurationDescription(builder, trackTab, trackItem, analysis);
			} else if (sortMode == NEAREST) {
				appendNearestDescription(builder, trackTab, trackItem, analysis);
			} else if (sortMode == LAST_MODIFIED) {
				appendLastModifiedDescription(builder, trackTab, trackItem, analysis);
			}
			description.setText(builder);
		}
		boolean showDistance = sortMode == NEAREST && trackItem.getNearestPoint() != null;
		AndroidUiHelper.updateVisibility(distanceTv, showDistance);
		AndroidUiHelper.updateVisibility(directionIcon, showDistance);
	}

	private void setupIcon(@NonNull GpxDataItem dataItem) {
		int color = dataItem.getColor();
		if (color == 0) {
			color = GpxAppearanceAdapter.getTrackColor(app);
		}
		imageView.setImageDrawable(getTrackIcon(app, dataItem.getWidth(), dataItem.isShowArrows(), color));
	}

	private void appendNameDescription(@NonNull SpannableStringBuilder builder, @NonNull TrackTab trackTab, @NonNull TrackItem trackItem, @NonNull GPXTrackAnalysis analysis) {
		builder.append(OsmAndFormatter.getFormattedDistance(analysis.totalDistance, app));
		if (analysis.isTimeSpecified()) {
			builder.append(" \u2022 ");
			appendDuration(builder, analysis);
		}
		appendPoints(builder, analysis);
		appendFolderName(builder, trackTab, trackItem);
	}

	private void appendCreationTimeDescription(@NonNull SpannableStringBuilder builder, @NonNull TrackTab trackTab, @NonNull TrackItem trackItem, @NonNull GPXTrackAnalysis analysis) {
		long creationTime = trackItem.getCreationTime();
		if (creationTime > 0) {
			DateFormat format = app.getResourceManager().getDateFormat();
			builder.append(format.format(new Date(creationTime)));
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

	private void appendLastModifiedDescription(@NonNull SpannableStringBuilder builder, @NonNull TrackTab trackTab, @NonNull TrackItem trackItem, @NonNull GPXTrackAnalysis analysis) {
		long lastModified = trackItem.getLastModified();
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

	private void appendDistanceDescription(@NonNull SpannableStringBuilder builder, @NonNull TrackTab trackTab, @NonNull TrackItem trackItem, @NonNull GPXTrackAnalysis analysis) {
		builder.append(OsmAndFormatter.getFormattedDistance(analysis.totalDistance, app));
		setupTextSpan(builder);

		if (analysis.isTimeSpecified()) {
			builder.append(" \u2022 ");
			appendDuration(builder, analysis);
		}
		appendPoints(builder, analysis);
		appendFolderName(builder, trackTab, trackItem);
	}

	private void appendDurationDescription(@NonNull SpannableStringBuilder builder, @NonNull TrackTab trackTab, @NonNull TrackItem trackItem, @NonNull GPXTrackAnalysis analysis) {
		if (analysis.isTimeSpecified()) {
			appendDuration(builder, analysis);
			setupTextSpan(builder);
			builder.append(" \u2022 ");
		}
		builder.append(OsmAndFormatter.getFormattedDistance(analysis.totalDistance, app));

		appendPoints(builder, analysis);
		appendFolderName(builder, trackTab, trackItem);
	}

	private void appendNearestDescription(@NonNull SpannableStringBuilder builder, @NonNull TrackTab trackTab, @NonNull TrackItem trackItem, @NonNull GPXTrackAnalysis analysis) {
		WptPt wptPt = trackItem.getNearestPoint();
		if (wptPt != null) {
			builder.append(" \u007C ");
			uiUtilities.updateLocationView(locationViewCache, directionIcon, distanceTv, new LatLon(wptPt.lat, wptPt.lon));
		}
		builder.append(OsmAndFormatter.getFormattedDistance(analysis.totalDistance, app));
		if (analysis.isTimeSpecified()) {
			builder.append(" \u2022 ");
			appendDuration(builder, analysis);
		}
		appendPoints(builder, analysis);
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

	private void appendFolderName(@NonNull SpannableStringBuilder builder, @NonNull TrackTab trackTab, @NonNull TrackItem trackItem) {
		String folderName = getFolderName(trackTab, trackItem);
		if (!Algorithms.isEmpty(folderName)) {
			builder.append(" \u007C ");
			builder.append(Algorithms.capitalizeFirstLetter(folderName));
		}
	}

	@Nullable
	private String getFolderName(@NonNull TrackTab trackTab, @NonNull TrackItem trackItem) {
		String folderName = null;
		File file = trackItem.getFile();
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
