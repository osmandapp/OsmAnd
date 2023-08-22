package net.osmand.plus.track;


import static net.osmand.plus.track.helpers.GpxDisplayHelper.buildTrackSegmentName;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

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
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class SplitTrackAsyncTask extends AsyncTask<Void, Void, Void> {

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
			model.updateSplit(splitParams);
			processGroupTrack(app, model, progress, splitParams.joinSegments);
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

	public static void processGroupTrack(@NonNull OsmandApplication app, @NonNull GpxDisplayGroup group,
	                                     @Nullable IProgress progress, boolean joinSegments) {
		if (group.getTrack() == null) {
			return;
		}

		List<GpxDisplayItem> displayItems = new ArrayList<>();
		String timeSpanClr = Algorithms.colorToString(ContextCompat.getColor(app, R.color.gpx_time_span_color));
		String speedClr = Algorithms.colorToString(ContextCompat.getColor(app, R.color.gpx_speed));
		String ascClr = Algorithms.colorToString(ContextCompat.getColor(app, R.color.gpx_altitude_asc));
		String descClr = Algorithms.colorToString(ContextCompat.getColor(app, R.color.gpx_altitude_desc));
		String distanceClr = Algorithms.colorToString(ContextCompat.getColor(app, R.color.gpx_distance_color));
		final float eleThreshold = 3;

		for (int segmentIdx = 0; segmentIdx < group.getTrack().segments.size(); segmentIdx++) {
			if (progress != null && progress.isInterrupted()) {
				return;
			}
			TrkSegment segment = group.getTrack().segments.get(segmentIdx);
			if (segment.points.isEmpty()) {
				continue;
			}

			int splitTime = group.getSplitTime();
			double splitDistance = group.getSplitDistance();
			boolean split = splitTime > 0 || splitDistance > 0;
			GPXTrackAnalysis[] trackAnalysis = getTrackAnalysis(segment, splitTime, splitDistance, joinSegments);

			for (GPXTrackAnalysis analysis : trackAnalysis) {
				if (progress != null && progress.isInterrupted()) {
					return;
				}
				GpxDisplayItem item = new GpxDisplayItem();
				item.group = group;
				if (split) {
					item.splitMetric = analysis.metricEnd;
					item.secondarySplitMetric = analysis.secondaryMetricEnd;
					item.splitName = formatSplitName(analysis.metricEnd, group, app);
					item.splitName += " (" + formatSecondarySplitName(analysis.secondaryMetricEnd, group, app) + ") ";
				}

				if (!group.isGeneralTrack() && !split) {
					item.trackSegmentName = buildTrackSegmentName(group.getGpxFile(), group.getTrack(), segment, app);
				}

				item.description = GpxUiHelper.getDescription(app, analysis, true);
				item.analysis = analysis;
				String name = "";
				if (!group.isSplitDistance()) {
					name += GpxUiHelper.getColorValue(distanceClr, OsmAndFormatter.getFormattedDistance(analysis.totalDistance, app));
				}
				if ((analysis.timeSpan > 0 || analysis.timeMoving > 0) && !group.isSplitTime()) {
					long tm = analysis.timeMoving;
					if (tm == 0) {
						tm = analysis.timeSpan;
					}
					if (!name.isEmpty())
						name += ", ";
					name += GpxUiHelper.getColorValue(timeSpanClr, Algorithms.formatDuration((int) (tm / 1000), app.accessibilityEnabled()));
				}
				if (analysis.isSpeedSpecified()) {
					if (!name.isEmpty())
						name += ", ";
					name += GpxUiHelper.getColorValue(speedClr, OsmAndFormatter.getFormattedSpeed(analysis.avgSpeed, app));
				}
				// add min/max elevation data to split track analysis to facilitate easier track/segment identification
				if (analysis.isElevationSpecified()) {
					if (!name.isEmpty())
						name += ", ";
					name += GpxUiHelper.getColorValue(descClr, OsmAndFormatter.getFormattedAlt(analysis.minElevation, app));
					name += " - ";
					name += GpxUiHelper.getColorValue(ascClr, OsmAndFormatter.getFormattedAlt(analysis.maxElevation, app));
				}
				if (analysis.isElevationSpecified() && (analysis.diffElevationUp > eleThreshold ||
						analysis.diffElevationDown > eleThreshold)) {
					if (!name.isEmpty())
						name += ", ";
					if (analysis.diffElevationDown > eleThreshold) {
						name += GpxUiHelper.getColorValue(descClr, " ↓ " +
								OsmAndFormatter.getFormattedAlt(analysis.diffElevationDown, app));
					}
					if (analysis.diffElevationUp > eleThreshold) {
						name += GpxUiHelper.getColorValue(ascClr, " ↑ " +
								OsmAndFormatter.getFormattedAlt(analysis.diffElevationUp, app));
					}
				}
				item.name = name;
				item.locationStart = analysis.locationStart;
				item.locationEnd = analysis.locationEnd;
				displayItems.add(item);
			}
		}
		group.addDisplayItems(displayItems);
	}

	@NonNull
	private static TrackPointsAnalyser getTrackPointsAnalyser() {
		return PluginsHelper::onAnalysePoint;
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

	private static String formatSecondarySplitName(double metricEnd, GpxDisplayGroup group, OsmandApplication app) {
		if (group.isSplitDistance()) {
			return Algorithms.formatDuration((int) metricEnd, app.accessibilityEnabled());
		} else {
			return OsmAndFormatter.getFormattedDistance((float) metricEnd, app);
		}
	}

	private static String formatSplitName(double metricEnd, GpxDisplayGroup group, OsmandApplication app) {
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