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

import net.osmand.gpx.GPXTrackAnalysis;
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
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.track.GpxAppearanceAdapter;
import net.osmand.plus.track.helpers.GPXDatabase.GpxDataItem;
import net.osmand.plus.track.helpers.GpxDbHelper;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.utils.UpdateLocationUtils;
import net.osmand.plus.utils.UpdateLocationUtils.UpdateLocationInfo;
import net.osmand.plus.utils.UpdateLocationUtils.UpdateLocationViewCache;
import net.osmand.plus.widgets.style.CustomTypefaceSpan;
import net.osmand.util.Algorithms;

import java.io.File;
import java.text.DateFormat;
import java.util.Collections;
import java.util.Date;

public class TrackViewHolder extends RecyclerView.ViewHolder {

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final GpxDbHelper gpxDbHelper;

	private final UpdateLocationViewCache locationViewCache;
	private final TracksFragment fragment;
	private final boolean nightMode;

	private final TextView title;
	private final TextView description;
	private final ImageView imageView;
	private final CompoundButton checkbox;
	private final View divider;
	private final ImageView directionIcon;

	public TrackViewHolder(@NonNull View itemView, @NonNull TracksFragment fragment,
	                       @NonNull UpdateLocationViewCache viewCache, boolean nightMode) {
		super(itemView);
		this.app = (OsmandApplication) itemView.getContext().getApplicationContext();
		this.settings = app.getSettings();
		this.gpxDbHelper = app.getGpxDbHelper();
		this.locationViewCache = viewCache;
		this.fragment = fragment;
		this.nightMode = nightMode;

		title = itemView.findViewById(R.id.title);
		description = itemView.findViewById(R.id.description);
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
		int activeColor = ColorUtilities.getActiveColor(app, nightMode);
		UiUtilities.setupCompoundButton(nightMode, activeColor, checkbox);
		itemView.setOnClickListener(v -> fragment.onTrackItemsSelected(Collections.singleton(trackItem), !selected));

		AndroidUiHelper.updateVisibility(divider, showDivider);
		bindInfoRow(adapter, trackItem);
	}

	public void bindInfoRow(@NonNull TracksAdapter adapter, @NonNull TrackItem trackItem) {
		File file = trackItem.getFile();
		GpxDataItem item = trackItem.getDataItem();
		if (item != null) {
			bindInfoRow(adapter, trackItem, item);
		} else if (file != null) {
			item = gpxDbHelper.getItem(file, dataItem -> {
				trackItem.setDataItem(dataItem);
				if (fragment.isAdded()) {
					adapter.notifyItemChanged(getAdapterPosition());
				}
			});
			if (item != null) {
				trackItem.setDataItem(item);
				bindInfoRow(adapter, trackItem, item);
			}
		} else if (trackItem.isShowCurrentTrack()) {
			String width = settings.CURRENT_TRACK_WIDTH.get();
			boolean showArrows = settings.CURRENT_TRACK_SHOW_ARROWS.get();
			int color = settings.CURRENT_TRACK_COLOR.get();
			setupIcon(color, width, showArrows);

			SelectedGpxFile selectedGpxFile = app.getSavingTrackHelper().getCurrentTrack();
			GPXTrackAnalysis analysis = selectedGpxFile.getTrackAnalysis(app);
			buildDescriptionRow(adapter, trackItem, analysis, null);
		}
	}

	public void bindInfoRow(@NonNull TracksAdapter adapter, @NonNull TrackItem trackItem, @NonNull GpxDataItem dataItem) {
		setupIcon(dataItem);
		GPXTrackAnalysis analysis = dataItem.getAnalysis();
		String cityName = dataItem.getNearestCityName();
		buildDescriptionRow(adapter, trackItem, analysis, cityName);
	}

	private void buildDescriptionRow(@NonNull TracksAdapter adapter, @NonNull TrackItem item,
	                                 @Nullable GPXTrackAnalysis analysis, @Nullable String cityName) {
		TrackTab trackTab = adapter.getTrackTab();
		TracksSortMode sortMode = trackTab.getSortMode();
		if (analysis != null) {
			SpannableStringBuilder builder = new SpannableStringBuilder();
			if (sortMode == NAME_ASCENDING || sortMode == NAME_DESCENDING) {
				appendNameDescription(builder, trackTab, item, analysis);
			} else if (sortMode == DATE_ASCENDING || sortMode == DATE_DESCENDING) {
				appendCreationTimeDescription(builder, analysis);
			} else if (sortMode == DISTANCE_ASCENDING || sortMode == DISTANCE_DESCENDING) {
				appendDistanceDescription(builder, trackTab, item, analysis);
			} else if (sortMode == DURATION_ASCENDING || sortMode == DURATION_DESCENDING) {
				appendDurationDescription(builder, trackTab, item, analysis);
			} else if (sortMode == NEAREST) {
				appendNearestDescription(builder, analysis, cityName);
			} else if (sortMode == LAST_MODIFIED) {
				appendLastModifiedDescription(builder, item, analysis);
			}
			description.setText(builder);
		}
		boolean showDirection = sortMode == NEAREST && analysis != null && analysis.latLonStart != null;
		AndroidUiHelper.updateVisibility(directionIcon, showDirection);
	}

	private void setupIcon(@NonNull GpxDataItem item) {
		setupIcon(item.getColor(), item.getWidth(), item.isShowArrows());
	}

	private void setupIcon(int color, String width, boolean showArrows) {
		int trackColor = color;
		if (trackColor == 0) {
			trackColor = GpxAppearanceAdapter.getTrackColor(app);
		}
		imageView.setImageDrawable(getTrackIcon(app, width, showArrows, trackColor));
	}

	private void appendNameDescription(@NonNull SpannableStringBuilder builder, @NonNull TrackTab trackTab, @NonNull TrackItem trackItem, @NonNull GPXTrackAnalysis analysis) {
		builder.append(OsmAndFormatter.getFormattedDistance(analysis.totalDistance, app));
		if (analysis.isTimeSpecified()) {
			builder.append(" • ");
			appendDuration(builder, analysis);
		}
		appendPoints(builder, analysis);
		appendFolderName(builder, trackTab, trackItem);
	}

	private void appendCreationTimeDescription(@NonNull SpannableStringBuilder builder, @NonNull GPXTrackAnalysis analysis) {
		if (analysis.startTime > 0) {
			DateFormat format = OsmAndFormatter.getDateFormat(app);
			builder.append(format.format(new Date(analysis.startTime)));
			setupTextSpan(builder);
			builder.append(" | ");
		}
		builder.append(OsmAndFormatter.getFormattedDistance(analysis.totalDistance, app));
		if (analysis.isTimeSpecified()) {
			builder.append(" • ");
			appendDuration(builder, analysis);
		}
		appendPoints(builder, analysis);
	}

	private void appendLastModifiedDescription(@NonNull SpannableStringBuilder builder, @NonNull TrackItem trackItem, @NonNull GPXTrackAnalysis analysis) {
		long lastModified = trackItem.getLastModified();
		if (lastModified > 0) {
			DateFormat format = OsmAndFormatter.getDateFormat(app);
			builder.append(format.format(new Date(lastModified)));
			setupTextSpan(builder);
			builder.append(" | ");
		}
		builder.append(OsmAndFormatter.getFormattedDistance(analysis.totalDistance, app));
		if (analysis.isTimeSpecified()) {
			builder.append(" • ");
			appendDuration(builder, analysis);
		}
		appendPoints(builder, analysis);
	}

	private void appendDistanceDescription(@NonNull SpannableStringBuilder builder, @NonNull TrackTab trackTab, @NonNull TrackItem trackItem, @NonNull GPXTrackAnalysis analysis) {
		builder.append(OsmAndFormatter.getFormattedDistance(analysis.totalDistance, app));
		setupTextSpan(builder);

		if (analysis.isTimeSpecified()) {
			builder.append(" • ");
			appendDuration(builder, analysis);
		}
		appendPoints(builder, analysis);
		appendFolderName(builder, trackTab, trackItem);
	}

	private void appendDurationDescription(@NonNull SpannableStringBuilder builder, @NonNull TrackTab trackTab, @NonNull TrackItem trackItem, @NonNull GPXTrackAnalysis analysis) {
		if (analysis.isTimeSpecified()) {
			appendDuration(builder, analysis);
			setupTextSpan(builder);
			builder.append(" • ");
		}
		builder.append(OsmAndFormatter.getFormattedDistance(analysis.totalDistance, app));

		appendPoints(builder, analysis);
		appendFolderName(builder, trackTab, trackItem);
	}

	private void appendNearestDescription(@NonNull SpannableStringBuilder builder,
	                                      @NonNull GPXTrackAnalysis analysis,
	                                      @Nullable String cityName) {
		if (analysis.latLonStart != null) {
			UpdateLocationInfo locationInfo = new UpdateLocationInfo(app, null, analysis.latLonStart);
			builder.append(UpdateLocationUtils.getFormattedDistance(app, locationInfo, locationViewCache));

			if (!Algorithms.isEmpty(cityName)) {
				builder.append(", ").append(cityName);
			}
			builder.append(" | ");
			UpdateLocationUtils.updateDirectionDrawable(app, directionIcon, locationInfo, locationViewCache);
		}
		builder.append(OsmAndFormatter.getFormattedDistance(analysis.totalDistance, app));
		if (analysis.isTimeSpecified()) {
			builder.append(" • ");
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
			builder.append(" • ");
			builder.append(String.valueOf(analysis.wptPoints));
		}
	}

	private void appendFolderName(@NonNull SpannableStringBuilder builder, @NonNull TrackTab trackTab, @NonNull TrackItem trackItem) {
		String folderName = getFolderName(trackTab, trackItem);
		if (!Algorithms.isEmpty(folderName)) {
			builder.append(" | ");
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
