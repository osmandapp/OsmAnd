package net.osmand.plus.track.helpers;

import android.os.AsyncTask.Status;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.CallbackWithObject;
import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXUtilities.Route;
import net.osmand.gpx.GPXUtilities.Track;
import net.osmand.gpx.GPXUtilities.TrkSegment;
import net.osmand.gpx.GPXUtilities.WptPt;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.track.GpxSplitParams;
import net.osmand.plus.track.SplitTrackAsyncTask;
import net.osmand.plus.track.SplitTrackAsyncTask.SplitTrackListener;
import net.osmand.plus.track.helpers.GPXDatabase.GpxDataItem;
import net.osmand.plus.track.helpers.GpxSelectionHelper.GpxDisplayItemType;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GpxDisplayHelper {

	private static final Log log = PlatformUtil.getLog(GpxDisplayHelper.class);

	private final OsmandApplication app;
	private final Map<String, SplitTrackAsyncTask> splitTrackTasks = new ConcurrentHashMap<>();

	public GpxDisplayHelper(@NonNull OsmandApplication app) {
		this.app = app;
	}

	private String getString(int resId, Object... formatArgs) {
		return app.getString(resId, formatArgs);
	}

	@NonNull
	public GpxDisplayGroup buildGpxDisplayGroup(@NonNull GPXFile gpxFile, int trackIndex, String name) {
		Track t = gpxFile.tracks.get(trackIndex);
		GpxDisplayGroup group = new GpxDisplayGroup(gpxFile);
		group.setGpxName(name);
		group.setColor(t.getColor(gpxFile.getColor(0)));
		group.setType(GpxDisplayItemType.TRACK_SEGMENT);
		group.setTrack(t);
		String ks = (trackIndex + 1) + "";
		group.setName(getString(R.string.gpx_selection_track, name, gpxFile.tracks.size() == 1 ? "" : ks));
		String description = "";
		if (t.name != null && !t.name.isEmpty()) {
			description = t.name + " " + description;
		}
		group.setDescription(description);
		group.setGeneralTrack(t.generalTrack);

		return group;
	}

	public GpxDisplayGroup buildPointsDisplayGroup(@NonNull GPXFile gpxFile, @NonNull List<WptPt> points, String name) {
		GpxDisplayGroup group = new GpxDisplayGroup(gpxFile);
		group.setGpxName(name);
		group.setType(GpxDisplayItemType.TRACK_POINTS);
		group.setDescription(getString(R.string.gpx_selection_number_of_points, gpxFile.getPointsSize()));
		group.setName(getString(R.string.gpx_selection_points, name));
		List<GpxDisplayItem> displayItems = new ArrayList<>();
		int k = 0;
		for (WptPt wptPt : points) {
			GpxDisplayItem item = new GpxDisplayItem();
			item.group = group;
			item.description = wptPt.desc;
			item.name = wptPt.name;
			k++;
			if (Algorithms.isEmpty(item.name)) {
				item.name = getString(R.string.gpx_selection_point, String.valueOf(k));
			}
			item.expanded = true;
			item.locationStart = wptPt;
			item.locationEnd = wptPt;
			displayItems.add(item);
		}
		group.addDisplayItems(displayItems);

		return group;
	}

	@NonNull
	public static String getGroupName(@NonNull OsmandApplication app, @NonNull GPXFile gpxFile) {
		String name = gpxFile.path;
		if (gpxFile.showCurrentTrack) {
			name = app.getString(R.string.shared_string_currently_recording_track);
		} else if (Algorithms.isEmpty(name)) {
			name = app.getString(R.string.current_route);
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

	@NonNull
	public List<GpxDisplayGroup> collectDisplayGroups(@NonNull GPXFile gpxFile, boolean processTrack) {
		List<GpxDisplayGroup> dg = new ArrayList<>();
		String name = getGroupName(app, gpxFile);
		if (gpxFile.tracks.size() > 0) {
			for (int i = 0; i < gpxFile.tracks.size(); i++) {
				GpxDisplayGroup group = buildGpxDisplayGroup(gpxFile, i, name);

				if (processTrack) {
					SplitTrackAsyncTask.processGroupTrack(app, group, null, false);
				}
				if (!Algorithms.isEmpty(group.getDisplayItems()) || !processTrack) {
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
				List<GpxDisplayItem> displayItems = new ArrayList<>();
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
					displayItems.add(item);
				}
				group.addDisplayItems(displayItems);
			}
		}
		if (!gpxFile.isPointsEmpty()) {
			GpxDisplayGroup group = buildPointsDisplayGroup(gpxFile, gpxFile.getPoints(), name);
			dg.add(group);
		}
		return dg;
	}

	public void processSplitAsync(@NonNull SelectedGpxFile selectedGpxFile, @Nullable CallbackWithObject<Boolean> callback) {
		if (!app.isApplicationInitializing()) {
			splitTrackAsync(selectedGpxFile, callback);
		} else if (callback != null) {
			callback.processResult(false);
		}
	}

	private final ExecutorService splitTrackSingleThreadExecutor = Executors.newSingleThreadExecutor();

	@NonNull
	public List<GpxDisplayGroup> processSplitSync(@NonNull GPXFile gpxFile, @NonNull GpxDataItem dataItem) {
		GpxSplitParams params = new GpxSplitParams(dataItem);
		List<GpxDisplayGroup> groups = collectDisplayGroups(gpxFile, false);
		SplitTrackAsyncTask splitTask = new SplitTrackAsyncTask(app, params, groups, null);
		try {
			splitTask.executeOnExecutor(splitTrackSingleThreadExecutor).get();
		} catch (ExecutionException | InterruptedException e) {
			log.error(e);
		}
		return groups;
	}

	private void splitTrackAsync(@NonNull SelectedGpxFile selectedGpxFile, @Nullable CallbackWithObject<Boolean> callback) {
		GPXFile gpxFile = selectedGpxFile.getGpxFile();
		GpxDataItem dataItem = app.getGpxDbHelper().getItem(new File(gpxFile.path));
		if (!isSplittingTrack(selectedGpxFile) && dataItem != null) {
			GpxSplitParams params = new GpxSplitParams(dataItem);
			List<GpxDisplayGroup> groups = collectDisplayGroups(gpxFile, false);
			SplitTrackListener listener = getSplitTrackListener(selectedGpxFile, groups, callback);

			splitTrackAsync(selectedGpxFile, groups, params, listener);
		} else if (callback != null) {
			callback.processResult(false);
		}
	}

	private boolean splitParamsChanged(@NonNull SelectedGpxFile selectedGpxFile, @NonNull GpxSplitParams splitParams) {
		GPXFile gpxFile = selectedGpxFile.getGpxFile();
		SplitTrackAsyncTask splitTask = splitTrackTasks.get(gpxFile.path);
		if (splitTask != null) {
			return !Algorithms.objectEquals(splitParams, splitTask.getSplitParams());
		}
		return false;
	}

	public void splitTrackAsync(@NonNull SelectedGpxFile selectedGpxFile, @NonNull List<GpxDisplayGroup> groups,
	                            @NonNull GpxSplitParams splitParams, @Nullable SplitTrackListener listener) {
		boolean splittingTrack = isSplittingTrack(selectedGpxFile);
		boolean paramsChanged = splitParamsChanged(selectedGpxFile, splitParams);
		if (paramsChanged) {
			cancelTrackSplitting(selectedGpxFile);
		}
		if (paramsChanged || !splittingTrack) {
			SplitTrackAsyncTask splitTask = new SplitTrackAsyncTask(app, splitParams, groups, listener);
			splitTrackTasks.put(selectedGpxFile.getGpxFile().path, splitTask);
			splitTask.executeOnExecutor(splitTrackSingleThreadExecutor);
		}
	}

	public boolean isSplittingTrack(@NonNull SelectedGpxFile selectedGpxFile) {
		return splitTrackTasks.containsKey(selectedGpxFile.getGpxFile().path);
	}

	public void cancelTrackSplitting(@NonNull SelectedGpxFile selectedGpxFile) {
		SplitTrackAsyncTask splitTask = splitTrackTasks.get(selectedGpxFile.getGpxFile().path);
		if (splitTask != null && splitTask.getStatus() == Status.RUNNING) {
			splitTask.cancel(false);
		}
	}

	@NonNull
	private SplitTrackListener getSplitTrackListener(@NonNull SelectedGpxFile selectedGpxFile,
	                                                 @NonNull List<GpxDisplayGroup> groups,
	                                                 @Nullable CallbackWithObject<Boolean> callback) {
		return new SplitTrackListener() {
			@Override
			public void trackSplittingFinished(boolean success) {
				if (success) {
					selectedGpxFile.setDisplayGroups(groups, app);
					app.getOsmandMap().getMapView().refreshMap();
				}
				if (callback != null) {
					callback.processResult(success);
				}
				splitTrackTasks.remove(selectedGpxFile.getGpxFile().path);
			}
		};
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
}
