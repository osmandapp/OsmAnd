package net.osmand.plus.configmap.tracks.viewholders;

import static net.osmand.gpx.GpxParameter.COLOR;
import static net.osmand.gpx.GpxParameter.FILE_CREATION_TIME;
import static net.osmand.gpx.GpxParameter.NEAREST_CITY_NAME;
import static net.osmand.gpx.GpxParameter.SHOW_ARROWS;
import static net.osmand.gpx.GpxParameter.WIDTH;
import static net.osmand.plus.settings.enums.TracksSortMode.DATE_ASCENDING;
import static net.osmand.plus.settings.enums.TracksSortMode.DATE_DESCENDING;
import static net.osmand.plus.settings.enums.TracksSortMode.DISTANCE_ASCENDING;
import static net.osmand.plus.settings.enums.TracksSortMode.DISTANCE_DESCENDING;
import static net.osmand.plus.settings.enums.TracksSortMode.DURATION_ASCENDING;
import static net.osmand.plus.settings.enums.TracksSortMode.DURATION_DESCENDING;
import static net.osmand.plus.settings.enums.TracksSortMode.LAST_MODIFIED;
import static net.osmand.plus.settings.enums.TracksSortMode.NAME_ASCENDING;
import static net.osmand.plus.settings.enums.TracksSortMode.NAME_DESCENDING;
import static net.osmand.plus.settings.enums.TracksSortMode.NEAREST;
import static net.osmand.plus.track.fragments.TrackAppearanceFragment.getTrackIcon;
import static net.osmand.plus.utils.ColorUtilities.getSecondaryTextColor;

import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.data.LatLon;
import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.configmap.tracks.TrackItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.enums.TracksSortMode;
import net.osmand.plus.track.GpxAppearanceAdapter;
import net.osmand.plus.track.data.TrackFolder;
import net.osmand.plus.track.helpers.GpxDataItem;
import net.osmand.plus.track.helpers.GpxDbHelper;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.utils.AndroidUtils;
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
import java.util.Set;

public class TrackViewHolder extends RecyclerView.ViewHolder {

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final GpxDbHelper gpxDbHelper;

	private final UpdateLocationViewCache locationViewCache;
	private final TrackSelectionListener listener;
	private final boolean nightMode;

	private final TextView title;
	private final TextView description;
	private final ImageView imageView;
	private final CompoundButton checkbox;
	private final View menuButton;
	private final View divider;
	private final ImageView directionIcon;

	public TrackViewHolder(@NonNull View itemView, @Nullable TrackSelectionListener listener,
	                       @NonNull UpdateLocationViewCache viewCache, boolean nightMode) {
		super(itemView);
		this.app = (OsmandApplication) itemView.getContext().getApplicationContext();
		this.settings = app.getSettings();
		this.gpxDbHelper = app.getGpxDbHelper();
		this.locationViewCache = viewCache;
		this.listener = listener;
		this.nightMode = nightMode;

		title = itemView.findViewById(R.id.title);
		description = itemView.findViewById(R.id.description);
		directionIcon = itemView.findViewById(R.id.direction_icon);
		checkbox = itemView.findViewById(R.id.checkbox);
		menuButton = itemView.findViewById(R.id.menu_button);
		imageView = itemView.findViewById(R.id.icon);
		divider = itemView.findViewById(R.id.divider);
	}

	public void bindView(@NonNull TracksSortMode sortMode, @NonNull TrackItem trackItem,
	                     boolean showDivider, boolean shouldShowFolder, boolean selectionMode) {
		bindView(sortMode, trackItem, showDivider, shouldShowFolder, selectionMode, false);
	}

	public void bindView(@NonNull TracksSortMode sortMode, @NonNull TrackItem trackItem,
	                     boolean showDivider, boolean shouldShowFolder, boolean selectionMode, boolean hideOptionsButton) {
		title.setText(trackItem.getName());

		boolean selected = listener != null && listener.isTrackItemSelected(trackItem);
		checkbox.setChecked(selected);
		UiUtilities.setupCompoundButton(nightMode, ColorUtilities.getActiveColor(app, nightMode), checkbox);
		AndroidUiHelper.updateVisibility(itemView.findViewById(R.id.checkbox_container), selectionMode);
		AndroidUiHelper.updateVisibility(menuButton, !selectionMode && !hideOptionsButton);

		int margin = app.getResources().getDimensionPixelSize(R.dimen.content_padding);
		ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) imageView.getLayoutParams();
		AndroidUtils.setMargins(params, margin, 0, selectionMode ? 0 : margin, 0);

		itemView.setOnClickListener(v -> {
			if (listener != null) {
				listener.onTrackItemsSelected(Collections.singleton(trackItem), !selected);
			}
		});
		menuButton.setOnClickListener(v -> {
			if (listener != null) {
				listener.onTrackItemOptionsSelected(v, trackItem);
			}
		});
		itemView.setOnLongClickListener(view -> {
			if (listener != null) {
				listener.onTrackItemLongClick(view, trackItem);
			}
			return true;
		});
		bindInfoRow(sortMode, trackItem, shouldShowFolder);
		AndroidUiHelper.updateVisibility(divider, showDivider);
	}

	public void bindInfoRow(@NonNull TracksSortMode sortMode, @NonNull TrackItem trackItem, boolean shouldShowFolder) {
		File file = trackItem.getFile();
		GpxDataItem item = trackItem.getDataItem();
		if (item != null) {
			bindInfoRow(sortMode, trackItem, item, shouldShowFolder);
		} else if (file != null) {
			item = gpxDbHelper.getItem(file, trackItem::setDataItem);
			if (item != null) {
				trackItem.setDataItem(item);
				bindInfoRow(sortMode, trackItem, item, shouldShowFolder);
			}
		} else if (trackItem.isShowCurrentTrack()) {
			String width = settings.CURRENT_TRACK_WIDTH.get();
			boolean showArrows = settings.CURRENT_TRACK_SHOW_ARROWS.get();
			int color = settings.CURRENT_TRACK_COLOR.get();
			setupIcon(color, width, showArrows);

			SelectedGpxFile selectedGpxFile = app.getSavingTrackHelper().getCurrentTrack();
			GPXTrackAnalysis analysis = selectedGpxFile.getTrackAnalysis(app);
			buildDescriptionRow(sortMode, trackItem, analysis, null, shouldShowFolder);
		}
	}

	public void bindInfoRow(@NonNull TracksSortMode sortMode, @NonNull TrackItem trackItem,
	                        @NonNull GpxDataItem dataItem, boolean shouldShowFolder) {
		setupIcon(dataItem);
		GPXTrackAnalysis analysis = dataItem.getAnalysis();
		String cityName = dataItem.getParameter(NEAREST_CITY_NAME);
		buildDescriptionRow(sortMode, trackItem, analysis, cityName, shouldShowFolder);
	}

	private void buildDescriptionRow(@NonNull TracksSortMode sortMode, @NonNull TrackItem trackItem,
	                                 @Nullable GPXTrackAnalysis analysis, @Nullable String cityName,
	                                 boolean shouldShowFolder) {
		if (analysis != null) {
			SpannableStringBuilder builder = new SpannableStringBuilder();
			if (sortMode == NAME_ASCENDING || sortMode == NAME_DESCENDING) {
				appendNameDescription(builder, trackItem, analysis, shouldShowFolder);
			} else if (sortMode == DATE_ASCENDING || sortMode == DATE_DESCENDING) {
				appendCreationTimeDescription(builder, trackItem, analysis);
			} else if (sortMode == DISTANCE_ASCENDING || sortMode == DISTANCE_DESCENDING) {
				appendDistanceDescription(builder, trackItem, analysis, shouldShowFolder);
			} else if (sortMode == DURATION_ASCENDING || sortMode == DURATION_DESCENDING) {
				appendDurationDescription(builder, trackItem, analysis, shouldShowFolder);
			} else if (sortMode == NEAREST) {
				appendNearestDescription(builder, analysis, cityName);
			} else if (sortMode == LAST_MODIFIED) {
				appendLastModifiedDescription(builder, trackItem, analysis);
			}
			description.setText(builder);
		}
		boolean showDirection = sortMode == NEAREST && analysis != null && analysis.getLatLonStart() != null;
		AndroidUiHelper.updateVisibility(directionIcon, showDirection);
	}

	private void setupIcon(@NonNull GpxDataItem item) {
		setupIcon(item.getParameter(COLOR), item.getParameter(WIDTH), item.getParameter(SHOW_ARROWS));
	}

	private void setupIcon(int color, String width, boolean showArrows) {
		int trackColor = color;
		if (trackColor == 0) {
			trackColor = GpxAppearanceAdapter.getTrackColor(app);
		}
		imageView.setImageDrawable(getTrackIcon(app, width, showArrows, trackColor));
	}

	private void appendNameDescription(@NonNull SpannableStringBuilder builder, @NonNull TrackItem trackItem,
	                                   @NonNull GPXTrackAnalysis analysis, boolean shouldShowFolder) {
		builder.append(OsmAndFormatter.getFormattedDistance(analysis.getTotalDistance(), app));
		if (analysis.isTimeSpecified()) {
			builder.append(" • ");
			appendDuration(builder, analysis);
		}
		appendPoints(builder, analysis);
		appendFolderName(builder, trackItem, shouldShowFolder);
	}

	private void appendCreationTimeDescription(@NonNull SpannableStringBuilder builder, @NonNull TrackItem trackItem, @NonNull GPXTrackAnalysis analysis) {
		GpxDataItem dataItem = trackItem.getDataItem();
		long creationTime = dataItem != null ? dataItem.getParameter(FILE_CREATION_TIME) : -1;
		if (creationTime > 10) {
			DateFormat format = OsmAndFormatter.getDateFormat(app);
			builder.append(format.format(new Date(creationTime)));
			setupTextSpan(builder);
			builder.append(" | ");
		}
		builder.append(OsmAndFormatter.getFormattedDistance(analysis.getTotalDistance(), app));
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
		builder.append(OsmAndFormatter.getFormattedDistance(analysis.getTotalDistance(), app));
		if (analysis.isTimeSpecified()) {
			builder.append(" • ");
			appendDuration(builder, analysis);
		}
		appendPoints(builder, analysis);
	}

	private void appendDistanceDescription(@NonNull SpannableStringBuilder builder, @NonNull TrackItem trackItem,
	                                       @NonNull GPXTrackAnalysis analysis, boolean shouldShowFolder) {
		builder.append(OsmAndFormatter.getFormattedDistance(analysis.getTotalDistance(), app));
		setupTextSpan(builder);

		if (analysis.isTimeSpecified()) {
			builder.append(" • ");
			appendDuration(builder, analysis);
		}
		appendPoints(builder, analysis);
		appendFolderName(builder, trackItem, shouldShowFolder);
	}

	private void appendDurationDescription(@NonNull SpannableStringBuilder builder, @NonNull TrackItem trackItem,
	                                       @NonNull GPXTrackAnalysis analysis, boolean shouldShowFolder) {
		if (analysis.isTimeSpecified()) {
			appendDuration(builder, analysis);
			setupTextSpan(builder);
			builder.append(" • ");
		}
		builder.append(OsmAndFormatter.getFormattedDistance(analysis.getTotalDistance(), app));

		appendPoints(builder, analysis);
		appendFolderName(builder, trackItem, shouldShowFolder);
	}

	private void appendNearestDescription(@NonNull SpannableStringBuilder builder,
	                                      @NonNull GPXTrackAnalysis analysis,
	                                      @Nullable String cityName) {
		LatLon latLon = analysis.getLatLonStart();
		if (latLon != null) {
			UpdateLocationInfo locationInfo = new UpdateLocationInfo(app, null, latLon);
			builder.append(UpdateLocationUtils.getFormattedDistance(app, locationInfo, locationViewCache));

			if (!Algorithms.isEmpty(cityName)) {
				builder.append(", ").append(cityName);
			}
			builder.append(" | ");
			UpdateLocationUtils.updateDirectionDrawable(app, directionIcon, locationInfo, locationViewCache);
		}
		builder.append(OsmAndFormatter.getFormattedDistance(analysis.getTotalDistance(), app));
		if (analysis.isTimeSpecified()) {
			builder.append(" • ");
			appendDuration(builder, analysis);
		}
		appendPoints(builder, analysis);
	}

	private void appendDuration(@NonNull SpannableStringBuilder builder, @NonNull GPXTrackAnalysis analysis) {
		if (analysis.isTimeSpecified()) {
			int duration = analysis.getDurationInSeconds();
			builder.append(Algorithms.formatDuration(duration, app.accessibilityEnabled()));
		}
	}

	private void appendPoints(@NonNull SpannableStringBuilder builder, @NonNull GPXTrackAnalysis analysis) {
		if (analysis.getWptPoints() > 0) {
			builder.append(" • ");
			builder.append(String.valueOf(analysis.getWptPoints()));
		}
	}

	private void appendFolderName(@NonNull SpannableStringBuilder builder, @NonNull TrackItem trackItem, boolean shouldShowFolder) {
		String folderName = getFolderName(trackItem, shouldShowFolder);
		if (!Algorithms.isEmpty(folderName)) {
			builder.append(" | ");
			builder.append(Algorithms.capitalizeFirstLetter(folderName));
		}
	}

	@Nullable
	private String getFolderName(@NonNull TrackItem trackItem, boolean shouldShowFolder) {
		String folderName = null;
		File file = trackItem.getFile();
		if (shouldShowFolder && file != null) {
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

	public interface TrackSelectionListener {
		default boolean isTrackItemSelected(@NonNull TrackItem trackItem) {
			return false;
		}

		default void onTrackItemsSelected(@NonNull Set<TrackItem> trackItems, boolean selected) {

		}

		default void onTrackFolderSelected(@NonNull TrackFolder trackFolder) {

		}

		default void onTrackItemLongClick(@NonNull View view, @NonNull TrackItem trackItem) {

		}

		default void onTrackItemOptionsSelected(@NonNull View view, @NonNull TrackItem trackItem) {

		}
	}
}
