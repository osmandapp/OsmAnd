package net.osmand.plus.track;


import static net.osmand.plus.track.helpers.GpxDisplayGroup.getTrackDisplayGroup;
import static net.osmand.plus.track.helpers.GpxDisplayHelper.buildTrackSegmentName;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import net.osmand.IProgress;
import net.osmand.shared.gpx.GpxTrackAnalysis;
import net.osmand.shared.gpx.GpxTrackAnalysis.TrackPointsAnalyser;
import net.osmand.shared.gpx.primitives.TrkSegment;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.settings.backend.backup.AbstractProgress;
import net.osmand.shared.settings.enums.MetricsConstants;
import net.osmand.plus.track.helpers.GpxDisplayGroup;
import net.osmand.plus.track.helpers.GpxDisplayItem;
import net.osmand.plus.track.helpers.GpxUiHelper;
import net.osmand.plus.track.helpers.TrackDisplayGroup;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

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

		for (int segmentIdx = 0; segmentIdx < group.getTrack().getSegments().size(); segmentIdx++) {
			if (progress != null && progress.isInterrupted()) {
				return;
			}
			TrkSegment segment = group.getTrack().getSegments().get(segmentIdx);
			if (!Algorithms.isEmpty(segment.getPoints())) {
				int splitTime = group.getSplitTime();
				double splitDistance = group.getSplitDistance();
				boolean uphillDownhill = group.isSplitUphillDownhill();

				for (GpxTrackAnalysis analysis : getTrackAnalysis(segment, splitTime, splitDistance, uphillDownhill, joinSegments)) {
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
	                                                  @NonNull TrkSegment segment, @NonNull GpxTrackAnalysis analysis) {
		GpxDisplayItem item = new GpxDisplayItem(analysis);
		item.group = group;
		item.name = getItemName(app, group, analysis);
		item.description = GpxUiHelper.getDescription(app, analysis, true);
		item.locationStart = analysis.getLocationStart();
		item.locationEnd = analysis.getLocationEnd();

		if (group.isSplitUphillDownhill()) {
			item.splitMetric = analysis.getMetricEnd();
			item.splitName = formatSplitName(analysis.getMetricEnd(), group, app);
			if (group.getSplitTime() > 0) {
				item.secondarySplitMetric = analysis.getSecondaryMetricEnd();
				item.splitName += " (" + formatSecondarySplitName(analysis.getSecondaryMetricEnd(), group, app) + ") ";
			}
		} else if (group.getSplitTime() > 0 || group.getSplitDistance() > 0) {
			item.splitMetric = analysis.getMetricEnd();
			item.secondarySplitMetric = analysis.getSecondaryMetricEnd();
			item.splitName = formatSplitName(analysis.getMetricEnd(), group, app);
			item.splitName += " (" + formatSecondarySplitName(analysis.getSecondaryMetricEnd(), group, app) + ") ";
		} else if (!group.isGeneralTrack()) {
			item.trackSegmentName = buildTrackSegmentName(group.getGpxFile(), group.getTrack(), segment, app);
		}
		return item;
	}

	@NonNull
	public static String getItemName(@NonNull OsmandApplication app, @NonNull TrackDisplayGroup group, @NonNull GpxTrackAnalysis analysis) {
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
	private static GpxTrackAnalysis[] getTrackAnalysis(@NonNull TrkSegment segment, int splitTime,
	                                                   double splitDistance, boolean upDownHills, boolean joinSegments) {
		TrackPointsAnalyser pointsAnalyser = getTrackPointsAnalyser();
		if (upDownHills) {
			List<GpxTrackAnalysis> trackAnalyses = segment.splitByUpDownHills(pointsAnalyser);
			return trackAnalyses.toArray(new GpxTrackAnalysis[0]);
		} else if (splitDistance > 0) {
			List<GpxTrackAnalysis> trackAnalyses = segment.splitByDistance(splitDistance, joinSegments, pointsAnalyser);
			return trackAnalyses.toArray(new GpxTrackAnalysis[0]);
		} else if (splitTime > 0) {
			List<GpxTrackAnalysis> trackAnalyses = segment.splitByTime(splitTime, joinSegments, pointsAnalyser);
			return trackAnalyses.toArray(new GpxTrackAnalysis[0]);
		} else {
			return new GpxTrackAnalysis[] {
					GpxTrackAnalysis.Companion.prepareInformation(0, joinSegments, pointsAnalyser, segment)
			};
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
		if (group.isSplitDistance() || group.isSplitUphillDownhill()) {
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