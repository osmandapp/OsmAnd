package net.osmand.plus.track;


import android.os.AsyncTask;

import net.osmand.IProgress;
import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.gpx.GPXTrackAnalysis.TrackPointsAnalyser;
import net.osmand.gpx.GPXUtilities.TrkSegment;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.settings.backend.backup.AbstractProgress;
import net.osmand.plus.settings.enums.MetricsConstants;
import net.osmand.plus.track.helpers.GpxDisplayGroup;
import net.osmand.plus.track.helpers.GpxDisplayItem;
import net.osmand.plus.track.helpers.GpxUiHelper;
import net.osmand.plus.track.helpers.TrackDisplayGroup;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import static net.osmand.plus.track.helpers.GpxDisplayGroup.getTrackDisplayGroup;
import static net.osmand.plus.track.helpers.GpxDisplayHelper.buildTrackSegmentName;

public class SplitTrackAsyncTask extends AsyncTask<Void, Void, Void> {

	private static final int ELEVATION_THRESHOLD = 3;

	private final OsmandApplication app;
	private final GpxSplitParams splitParams;
	private final List<GpxDisplayGroup> groups;
	private final SplitTrackListener listener;

	public SplitTrackAsyncTask(@NonNull OsmandApplication app,
	                           @NonNull GpxSplitParams splitParams,
	                           @NonNull List<GpxDisplayGroup> groups,
	                           @Nullable SplitTrackListener listener) {
		this.app = app;
		this.splitParams = splitParams;
		this.groups = groups;
		this.listener = listener;
	}

	@NonNull
	public GpxSplitParams getSplitParams() {
		return splitParams;
	}

	@Override
	protected void onPreExecute() {
		if (listener != null) {
			listener.trackSplittingStarted();
		}
	}

	@Override
	protected Void doInBackground(Void... params) {
		IProgress progress = getProgress();
		for (GpxDisplayGroup model : groups) {
			if (isCancelled()) {
				return null;
			}
			TrackDisplayGroup trackGroup = getTrackDisplayGroup(model);
			if (trackGroup != null) {
				trackGroup.updateSplit(splitParams);
				processGroupTrack(app, trackGroup, progress, splitParams.joinSegments);
			}
		}
		return null;
	}

	@NonNull
	private IProgress getProgress() {
		return new AbstractProgress() {

			@Override
			public void progress(int deltaWork) {

			}

			@Override
			public boolean isInterrupted() {
				return isCancelled();
			}
		};
	}

	@Override
	protected void onCancelled() {
		if (listener != null) {
			listener.trackSplittingFinished(false);
		}
	}

	@Override
	protected void onPostExecute(Void result) {
		if (listener != null) {
			listener.trackSplittingFinished(true);
		}
	}

	public static void processGroupTrack(@NonNull OsmandApplication app, @NonNull TrackDisplayGroup group,
	                                     @Nullable IProgress progress, boolean joinSegments) {
		if (group.getTrack() == null) {
			return;
		}

		List<GpxDisplayItem> displayItems = new ArrayList<>();

		for (int segmentIdx = 0; segmentIdx < group.getTrack().segments.size(); segmentIdx++) {
			if (progress != null && progress.isInterrupted()) {
				return;
			}
			TrkSegment segment = group.getTrack().segments.get(segmentIdx);
			if (!Algorithms.isEmpty(segment.points)) {
				int splitTime = group.getSplitTime();
				double splitDistance = group.getSplitDistance();

				for (GPXTrackAnalysis analysis : getTrackAnalysis(segment, splitTime, splitDistance, joinSegments)) {
					if (progress != null && progress.isInterrupted()) {
						return;
					}
					displayItems.add(createGpxDisplayItem(app, group, segment, analysis));
				}
			}
		}
		group.addDisplayItems(displayItems);
	}

	@NonNull
	public static GpxDisplayItem createGpxDisplayItem(@NonNull OsmandApplication app, @NonNull TrackDisplayGroup group,
	                                                  @NonNull TrkSegment segment, @NonNull GPXTrackAnalysis analysis) {
		GpxDisplayItem item = new GpxDisplayItem(analysis);
		item.group = group;
		item.name = getItemName(app, group, analysis);
		item.description = GpxUiHelper.getDescription(app, analysis, true);
		item.locationStart = analysis.locationStart;
		item.locationEnd = analysis.locationEnd;

		if (group.getSplitTime() > 0 || group.getSplitDistance() > 0) {
			item.splitMetric = analysis.metricEnd;
			item.secondarySplitMetric = analysis.secondaryMetricEnd;
			item.splitName = formatSplitName(analysis.metricEnd, group, app);
			item.splitName += " (" + formatSecondarySplitName(analysis.secondaryMetricEnd, group, app) + ") ";
		} else if (!group.isGeneralTrack()) {
			item.trackSegmentName = buildTrackSegmentName(group.getGpxFile(), group.getTrack(), segment, app);
		}
		return item;
	}

	@NonNull
	public static String getItemName(@NonNull OsmandApplication app, @NonNull TrackDisplayGroup group, @NonNull GPXTrackAnalysis analysis) {
		StringBuilder builder = new StringBuilder();

		if (!group.isSplitDistance()) {
			String color = Algorithms.colorToString(ContextCompat.getColor(app, R.color.gpx_distance_color));
			builder.append(GpxUiHelper.getColorValue(color, OsmAndFormatter.getFormattedDistance(analysis.getTotalDistance(), app)));
		}
		if ((analysis.getTimeSpan() > 0 || analysis.getTimeMoving() > 0) && !group.isSplitTime()) {
			if (!Algorithms.isEmpty(builder)) {
				builder.append(", ");
			}
			long time = analysis.getTimeMoving() != 0 ? analysis.getTimeMoving() : analysis.getTimeSpan();
			String color = Algorithms.colorToString(ContextCompat.getColor(app, R.color.gpx_time_span_color));
			builder.append(GpxUiHelper.getColorValue(color, Algorithms.formatDuration((int) (time / 1000), app.accessibilityEnabled())));
		}
		if (analysis.isSpeedSpecified()) {
			if (!Algorithms.isEmpty(builder)) {
				builder.append(", ");
			}
			String color = Algorithms.colorToString(ContextCompat.getColor(app, R.color.gpx_speed));
			builder.append(GpxUiHelper.getColorValue(color, OsmAndFormatter.getFormattedSpeed(analysis.getAvgSpeed(), app)));
		}
		// add min/max elevation data to split track analysis to facilitate easier track/segment identification
		String ascClr = Algorithms.colorToString(ContextCompat.getColor(app, R.color.gpx_altitude_asc));
		String descClr = Algorithms.colorToString(ContextCompat.getColor(app, R.color.gpx_altitude_desc));
		if (analysis.isElevationSpecified()) {
			if (!Algorithms.isEmpty(builder)) {
				builder.append(", ");
			}
			builder.append(GpxUiHelper.getColorValue(descClr, OsmAndFormatter.getFormattedAlt(analysis.getMinElevation(), app)));
			builder.append(" - ");
			builder.append(GpxUiHelper.getColorValue(ascClr, OsmAndFormatter.getFormattedAlt(analysis.getMaxElevation(), app)));
		}
		if (analysis.isElevationSpecified()
				&& (analysis.getDiffElevationUp() > ELEVATION_THRESHOLD || analysis.getDiffElevationDown() > ELEVATION_THRESHOLD)) {
			if (!Algorithms.isEmpty(builder)) {
				builder.append(", ");
			}
			if (analysis.getDiffElevationDown() > ELEVATION_THRESHOLD) {
				builder.append(GpxUiHelper.getColorValue(descClr, " ↓ " +
						OsmAndFormatter.getFormattedAlt(analysis.getDiffElevationDown(), app)));
			}
			if (analysis.getDiffElevationUp() > ELEVATION_THRESHOLD) {
				builder.append(GpxUiHelper.getColorValue(ascClr, " ↑ " +
						OsmAndFormatter.getFormattedAlt(analysis.getDiffElevationUp(), app)));
			}
		}
		return builder.toString();
	}

	@NonNull
	private static TrackPointsAnalyser getTrackPointsAnalyser() {
		return PluginsHelper.getTrackPointsAnalyser();
	}

	@NonNull
	private static GPXTrackAnalysis[] getTrackAnalysis(@NonNull TrkSegment segment, int splitTime,
	                                                   double splitDistance, boolean joinSegments) {
		TrackPointsAnalyser pointsAnalyser = getTrackPointsAnalyser();
		if (splitDistance > 0) {
			List<GPXTrackAnalysis> trackAnalyses = segment.splitByDistance(splitDistance, joinSegments);
			return trackAnalyses.toArray(new GPXTrackAnalysis[0]);
		} else if (splitTime > 0) {
			List<GPXTrackAnalysis> trackAnalyses = segment.splitByTime(splitTime, joinSegments);
			return trackAnalyses.toArray(new GPXTrackAnalysis[0]);
		} else {
			return new GPXTrackAnalysis[] {GPXTrackAnalysis.prepareInformation(0, pointsAnalyser, segment)};
		}
	}

	private static String formatSecondarySplitName(double metricEnd, @NonNull TrackDisplayGroup group,
	                                               @NonNull OsmandApplication app) {
		if (group.isSplitDistance()) {
			return Algorithms.formatDuration((int) metricEnd, app.accessibilityEnabled());
		} else {
			return OsmAndFormatter.getFormattedDistance((float) metricEnd, app);
		}
	}

	private static String formatSplitName(double metricEnd, @NonNull TrackDisplayGroup group,
	                                      @NonNull OsmandApplication app) {
		if (group.isSplitDistance()) {
			MetricsConstants mc = app.getSettings().METRIC_SYSTEM.get();
			if (mc == MetricsConstants.KILOMETERS_AND_METERS) {
				double sd = group.getSplitDistance();
				int digits = sd < 100 ? 2 : (sd < 1000 ? 1 : 0);
				int rem1000 = (int) (metricEnd + 0.5) % 1000;
				if (rem1000 > 1 && digits < 1) {
					digits = 1;
				}
				int rem100 = (int) (metricEnd + 0.5) % 100;
				if (rem100 > 1 && digits < 2) {
					digits = 2;
				}
				return OsmAndFormatter.getFormattedRoundDistanceKm((float) metricEnd, digits, app);
			} else {
				return OsmAndFormatter.getFormattedDistance((float) metricEnd, app);
			}
		} else {
			return Algorithms.formatDuration((int) metricEnd, app.accessibilityEnabled());
		}
	}

	public interface SplitTrackListener {

		default void trackSplittingStarted() {
		}

		default void trackSplittingFinished(boolean success) {
		}
	}
}