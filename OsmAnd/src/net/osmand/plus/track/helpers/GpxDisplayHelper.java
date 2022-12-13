package net.osmand.plus.track.helpers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.GPXTrackAnalysis;
import net.osmand.GPXUtilities.Route;
import net.osmand.GPXUtilities.Track;
import net.osmand.GPXUtilities.TrkSegment;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.IndexConstants;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.settings.enums.MetricsConstants;
import net.osmand.plus.track.GpxSplitType;
import net.osmand.plus.track.helpers.GPXDatabase.GpxDataItem;
import net.osmand.plus.track.helpers.GpxSelectionHelper.GpxDisplayItemType;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class GpxDisplayHelper {

	private final OsmandApplication app;

	public GpxDisplayHelper(@NonNull OsmandApplication app) {
		this.app = app;
	}

	private String getString(int resId, Object... formatArgs) {
		return app.getString(resId, formatArgs);
	}

	public GpxDisplayGroup buildGeneralGpxDisplayGroup(GPXFile gpxFile, Track track) {
		GpxDisplayGroup group = new GpxDisplayGroup(gpxFile);
		String name = getGroupName(gpxFile);
		group.setGpxName(name);
		group.setColor(track.getColor(gpxFile.getColor(0)));
		group.setType(GpxDisplayItemType.TRACK_SEGMENT);
		group.setTrack(track);
		group.setName(getString(R.string.gpx_selection_track, name, ""));
		String d = "";
		if (track.name != null && track.name.length() > 0) {
			d = track.name + " " + d;
		}
		group.setDescription(d);
		group.setGeneralTrack(true);
		processGroupTrack(app, group);
		return group;
	}

	public GpxDisplayGroup buildGpxDisplayGroup(@NonNull GPXFile gpxFile, int trackIndex, String name) {
		Track t = gpxFile.tracks.get(trackIndex);
		GpxDisplayGroup group = new GpxDisplayGroup(gpxFile);
		group.setGpxName(name);
		group.setColor(t.getColor(gpxFile.getColor(0)));
		group.setType(GpxDisplayItemType.TRACK_SEGMENT);
		group.setTrack(t);
		String ks = (trackIndex + 1) + "";
		group.setName(getString(R.string.gpx_selection_track, name, gpxFile.tracks.size() == 1 ? "" : ks));
		String d = "";
		if (t.name != null && t.name.length() > 0) {
			d = t.name + " " + d;
		}
		group.setDescription(d);
		group.setGeneralTrack(t.generalTrack);
		processGroupTrack(app, group);
		return group;
	}

	public GpxDisplayGroup buildPointsDisplayGroup(@NonNull GPXFile gpxFile, @NonNull List<WptPt> points, String name) {
		GpxDisplayGroup group = new GpxDisplayGroup(gpxFile);
		group.setGpxName(name);
		group.setType(GpxDisplayItemType.TRACK_POINTS);
		group.setDescription(getString(R.string.gpx_selection_number_of_points, gpxFile.getPointsSize()));
		group.setName(getString(R.string.gpx_selection_points, name));
		List<GpxDisplayItem> list = group.getModifiableList();
		int k = 0;
		for (WptPt wptPt : points) {
			GpxDisplayItem item = new GpxDisplayItem();
			item.group = group;
			item.description = wptPt.desc;
			item.name = wptPt.name;
			k++;
			if (Algorithms.isEmpty(item.name)) {
				item.name = getString(R.string.gpx_selection_point, k + "");
			}
			item.expanded = true;
			item.locationStart = wptPt;
			item.locationEnd = wptPt;
			list.add(item);
		}
		return group;
	}

	public String getGroupName(GPXFile g) {
		String name = g.path;
		if (g.showCurrentTrack) {
			name = getString(R.string.shared_string_currently_recording_track);
		} else if (Algorithms.isEmpty(name)) {
			name = getString(R.string.current_route);
		} else {
			int i = name.lastIndexOf('/');
			if (i >= 0) {
				name = name.substring(i + 1);
			}
			i = name.lastIndexOf('\\');
			if (i >= 0) {
				name = name.substring(i + 1);
			}
			if (name.toLowerCase().endsWith(IndexConstants.GPX_FILE_EXT)) {
				name = name.substring(0, name.length() - 4);
			}
			name = name.replace('_', ' ');
		}
		return name;
	}

	public List<GpxDisplayGroup> collectDisplayGroups(GPXFile gpxFile) {
		List<GpxDisplayGroup> dg = new ArrayList<>();
		String name = getGroupName(gpxFile);
		if (gpxFile.tracks.size() > 0) {
			for (int i = 0; i < gpxFile.tracks.size(); i++) {
				GpxDisplayGroup group = buildGpxDisplayGroup(gpxFile, i, name);
				if (group.getModifiableList().size() > 0) {
					dg.add(group);
				}
			}
		}
		if (gpxFile.routes.size() > 0) {
			int k = 0;
			for (Route route : gpxFile.routes) {
				GpxDisplayGroup group = new GpxDisplayGroup(gpxFile);
				group.setGpxName(name);
				group.setType(GpxDisplayItemType.TRACK_ROUTE_POINTS);
				String d = getString(R.string.gpx_selection_number_of_points, name, route.points.size());
				if (route.name != null && route.name.length() > 0) {
					d = route.name + " " + d;
				}
				group.setDescription(d);
				String ks = (k++) + "";
				group.setName(getString(R.string.gpx_selection_route_points, name, gpxFile.routes.size() == 1 ? "" : ks));
				dg.add(group);
				List<GpxDisplayItem> list = group.getModifiableList();
				int t = 0;
				for (WptPt r : route.points) {
					GpxDisplayItem item = new GpxDisplayItem();
					item.group = group;
					item.description = r.desc;
					item.expanded = true;
					item.name = r.name;
					t++;
					if (Algorithms.isEmpty(item.name)) {
						item.name = getString(R.string.gpx_selection_point, t + "");
					}
					item.locationStart = r;
					item.locationEnd = r;
					list.add(item);
				}
			}
		}
		if (!gpxFile.isPointsEmpty()) {
			GpxDisplayGroup group = buildPointsDisplayGroup(gpxFile, gpxFile.getPoints(), name);
			dg.add(group);
		}
		return dg;
	}

	public static boolean processSplit(@NonNull OsmandApplication app, @Nullable SelectedGpxFile fileToProcess) {
		if (app.isApplicationInitializing()) {
			return false;
		}

		List<GpxDataItem> items = app.getGpxDbHelper().getSplitItems();

		for (GpxDataItem dataItem : items) {
			String path = dataItem.getFile().getAbsolutePath();
			SelectedGpxFile selectedGpxFile;

			GPXFile gpxFileToProcess = fileToProcess == null ? null : fileToProcess.getGpxFile();
			if (gpxFileToProcess != null && path.equals(gpxFileToProcess.path)) {
				selectedGpxFile = fileToProcess;
			} else {
				selectedGpxFile = app.getSelectedGpxHelper().getSelectedFileByPath(path);
			}

			if (selectedGpxFile == null || fileToProcess != null && !fileToProcess.equals(selectedGpxFile)) {
				continue;
			}

			if (selectedGpxFile.getGpxFile() != null) {
				GPXFile gpxFile = selectedGpxFile.getGpxFile();
				List<GpxDisplayGroup> displayGroups = processSplit(app, dataItem, gpxFile);
				selectedGpxFile.setDisplayGroups(displayGroups, app);
			}
		}
		return fileToProcess == null || fileToProcess.splitProcessed;
	}

	@NonNull
	public static List<GpxDisplayGroup> processSplit(@NonNull OsmandApplication app,
	                                                 @NonNull GpxDataItem dataItem,
	                                                 @NonNull GPXFile gpxFile) {
		List<GpxDisplayGroup> groups = app.getGpxDisplayHelper().collectDisplayGroups(gpxFile);

		GpxSplitType splitType = GpxSplitType.getSplitTypeByTypeId(dataItem.getSplitType());
		if (splitType == GpxSplitType.NO_SPLIT) {
			for (GpxDisplayGroup model : groups) {
				model.noSplit(app);
			}
		} else if (splitType == GpxSplitType.DISTANCE) {
			for (GpxDisplayGroup model : groups) {
				model.splitByDistance(app, dataItem.getSplitInterval(), dataItem.isJoinSegments());
			}
		} else if (splitType == GpxSplitType.TIME) {
			for (GpxDisplayGroup model : groups) {
				model.splitByTime(app, (int) dataItem.getSplitInterval(), dataItem.isJoinSegments());
			}
		}

		return groups;
	}

	protected static void processGroupTrack(@NonNull OsmandApplication app, @NonNull GpxDisplayGroup group) {
		processGroupTrack(app, group, false);
	}

	protected static void processGroupTrack(@NonNull OsmandApplication app, @NonNull GpxDisplayGroup group, boolean joinSegments) {
		if (group.getTrack() == null) {
			return;
		}

		List<GpxDisplayItem> list = group.getModifiableList();
		String timeSpanClr = Algorithms.colorToString(ContextCompat.getColor(app, R.color.gpx_time_span_color));
		String speedClr = Algorithms.colorToString(ContextCompat.getColor(app, R.color.gpx_speed));
		String ascClr = Algorithms.colorToString(ContextCompat.getColor(app, R.color.gpx_altitude_asc));
		String descClr = Algorithms.colorToString(ContextCompat.getColor(app, R.color.gpx_altitude_desc));
		String distanceClr = Algorithms.colorToString(ContextCompat.getColor(app, R.color.gpx_distance_color));
		final float eleThreshold = 3;

		for (int segmentIdx = 0; segmentIdx < group.getTrack().segments.size(); segmentIdx++) {
			TrkSegment segment = group.getTrack().segments.get(segmentIdx);

			if (segment.points.size() == 0) {
				continue;
			}
			GPXTrackAnalysis[] as;
			boolean split = true;
			if (group.getSplitDistance() > 0) {
				List<GPXTrackAnalysis> trackSegments = segment.splitByDistance(group.getSplitDistance(), joinSegments);
				as = trackSegments.toArray(new GPXTrackAnalysis[0]);
			} else if (group.getSplitTime() > 0) {
				List<GPXTrackAnalysis> trackSegments = segment.splitByTime(group.getSplitTime(), joinSegments);
				as = trackSegments.toArray(new GPXTrackAnalysis[0]);
			} else {
				split = false;
				as = new GPXTrackAnalysis[] {GPXTrackAnalysis.segment(0, segment)};
			}
			for (GPXTrackAnalysis analysis : as) {
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
					if (name.length() != 0)
						name += ", ";
					name += GpxUiHelper.getColorValue(timeSpanClr, Algorithms.formatDuration((int) (tm / 1000), app.accessibilityEnabled()));
				}
				if (analysis.isSpeedSpecified()) {
					if (name.length() != 0)
						name += ", ";
					name += GpxUiHelper.getColorValue(speedClr, OsmAndFormatter.getFormattedSpeed(analysis.avgSpeed, app));
				}
// add min/max elevation data to split track analysis to facilitate easier track/segment identification
				if (analysis.isElevationSpecified()) {
					if (name.length() != 0)
						name += ", ";
					name += GpxUiHelper.getColorValue(descClr, OsmAndFormatter.getFormattedAlt(analysis.minElevation, app));
					name += " - ";
					name += GpxUiHelper.getColorValue(ascClr, OsmAndFormatter.getFormattedAlt(analysis.maxElevation, app));
				}
				if (analysis.isElevationSpecified() && (analysis.diffElevationUp > eleThreshold ||
						analysis.diffElevationDown > eleThreshold)) {
					if (name.length() != 0)
						name += ", ";
					if (analysis.diffElevationDown > eleThreshold) {
						name += GpxUiHelper.getColorValue(descClr, " \u2193 " +
								OsmAndFormatter.getFormattedAlt(analysis.diffElevationDown, app));
					}
					if (analysis.diffElevationUp > eleThreshold) {
						name += GpxUiHelper.getColorValue(ascClr, " \u2191 " +
								OsmAndFormatter.getFormattedAlt(analysis.diffElevationUp, app));
					}
				}
				item.name = name;
				item.locationStart = analysis.locationStart;
				item.locationEnd = analysis.locationEnd;
				list.add(item);
			}
		}
	}

	private static String formatSecondarySplitName(double metricEnd, GpxDisplayGroup group, OsmandApplication app) {
		if (group.isSplitDistance()) {
			return Algorithms.formatDuration((int) metricEnd, app.accessibilityEnabled());
		} else {
			return OsmAndFormatter.getFormattedDistance((float) metricEnd, app);
		}
	}

	@NonNull
	public static String buildTrackSegmentName(GPXFile gpxFile, Track track, TrkSegment segment, OsmandApplication app) {
		String trackTitle = getTrackTitle(gpxFile, track, app);
		String segmentTitle = getSegmentTitle(segment, track.segments.indexOf(segment), app);

		boolean oneSegmentPerTrack =
				gpxFile.getNonEmptySegmentsCount() == gpxFile.getNonEmptyTracksCount();
		boolean oneOriginalTrack = gpxFile.hasGeneralTrack() && gpxFile.getNonEmptyTracksCount() == 2
				|| !gpxFile.hasGeneralTrack() && gpxFile.getNonEmptyTracksCount() == 1;

		if (oneSegmentPerTrack) {
			return trackTitle;
		} else if (oneOriginalTrack) {
			return segmentTitle;
		} else {
			return app.getString(R.string.ltr_or_rtl_combine_via_dash, trackTitle, segmentTitle);
		}
	}

	@NonNull
	private static String getTrackTitle(GPXFile gpxFile, Track track, OsmandApplication app) {
		String trackName;
		if (Algorithms.isBlank(track.name)) {
			int trackIdx = gpxFile.tracks.indexOf(track);
			trackName = String.valueOf(trackIdx + 1);
		} else {
			trackName = track.name;
		}
		String trackString = app.getString(R.string.shared_string_gpx_track);
		return app.getString(R.string.ltr_or_rtl_combine_via_colon, trackString, trackName);
	}

	@NonNull
	private static String getSegmentTitle(@NonNull TrkSegment segment, int segmentIdx, OsmandApplication app) {
		String segmentName = Algorithms.isBlank(segment.name) ? String.valueOf(segmentIdx + 1) : segment.name;
		String segmentString = app.getString(R.string.gpx_selection_segment_title);
		return app.getString(R.string.ltr_or_rtl_combine_via_colon, segmentString, segmentName);
	}

	@NonNull
	public static String getRouteTitle(@NonNull Route route, int index, OsmandApplication app) {
		String segmentName = Algorithms.isBlank(route.name) ? String.valueOf(index + 1) : route.name;
		String segmentString = app.getString(R.string.layer_route);
		return app.getString(R.string.ltr_or_rtl_combine_via_colon, segmentString, segmentName);
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
}
