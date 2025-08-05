package net.osmand.plus.track.helpers;

import android.os.AsyncTask.Status;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.CallbackWithObject;
import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmAndTaskManager;
import net.osmand.shared.gpx.GpxDataItem;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.GpxParameter;
import net.osmand.shared.gpx.primitives.Route;
import net.osmand.shared.gpx.primitives.Track;
import net.osmand.shared.gpx.primitives.TrkSegment;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.track.GpxSplitParams;
import net.osmand.plus.track.SplitTrackAsyncTask;
import net.osmand.plus.track.SplitTrackAsyncTask.SplitTrackListener;
import net.osmand.shared.io.KFile;
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

	@NonNull
	public List<GpxDisplayGroup> collectDisplayGroups(@Nullable SelectedGpxFile selectedGpxFile,
	                                                  @NonNull GpxFile gpxFile, boolean processTrack,
	                                                  boolean useCachedGroups) {
		if (selectedGpxFile == null) {
			selectedGpxFile = app.getSelectedGpxHelper().getSelectedFileByPath(gpxFile.getPath());
		}
		List<GpxDisplayGroup> displayGroups = null;
		if (selectedGpxFile != null && useCachedGroups) {
			displayGroups = selectedGpxFile.getSplitGroups(app);
		}
		if (displayGroups == null) {
			displayGroups = collectDisplayGroups(gpxFile, processTrack);
		}
		return displayGroups;
	}

	@NonNull
	private List<GpxDisplayGroup> collectDisplayGroups(@NonNull GpxFile gpxFile, boolean processTrack) {
		List<GpxDisplayGroup> displayGroups = new ArrayList<>();
		String name = getGroupName(app, gpxFile);
		if (gpxFile.getTracks().size() > 0) {
			for (int i = 0; i < gpxFile.getTracks().size(); i++) {
				TrackDisplayGroup group = buildTrackDisplayGroup(gpxFile, i, name);
				if (processTrack) {
					GpxDataItem dataItem = !Algorithms.isEmpty(gpxFile.getPath())
							? app.getGpxDbHelper().getItem(new KFile(gpxFile.getPath())) : null;
					boolean joinSegments = dataItem != null ? dataItem.getParameter(GpxParameter.JOIN_SEGMENTS) : false;
					SplitTrackAsyncTask.processGroupTrack(app, group, null, joinSegments);
				}
				if (!Algorithms.isEmpty(group.getDisplayItems()) || !processTrack) {
					displayGroups.add(group);
				}
			}
		}
		if (gpxFile.getRoutes().size() > 0) {
			for (int i = 0; i < gpxFile.getRoutes().size(); i++) {
				GpxDisplayGroup group = buildRouteDisplayGroup(gpxFile, i, name);
				displayGroups.add(group);
			}
		}
		if (!gpxFile.isPointsEmpty()) {
			GpxDisplayGroup group = buildPointsDisplayGroup(gpxFile, gpxFile.getPointsList(), name);
			displayGroups.add(group);
		}
		return displayGroups;
	}

	@NonNull
	public TrackDisplayGroup buildTrackDisplayGroup(@NonNull GpxFile gpxFile) {
		return buildTrackDisplayGroup(gpxFile, 0, "");
	}

	@NonNull
	private TrackDisplayGroup buildTrackDisplayGroup(@NonNull GpxFile gpxFile, int trackIndex, @NonNull String name) {
		Track track = gpxFile.getTracks().get(trackIndex);
		TrackDisplayGroup group = new TrackDisplayGroup(gpxFile, track, track.getGeneralTrack(), trackIndex);
		group.applyName(app, name);
		group.setColor(track.getColor(gpxFile.getColor(0)));
		String description = "";
		if (track.getName() != null && !track.getName().isEmpty()) {
			description = track.getName() + " " + description;
		}
		group.setDescription(description);
		return group;
	}

	private GpxDisplayGroup buildRouteDisplayGroup(@NonNull GpxFile gpxFile, int routeIndex, @NonNull String name) {
		Route route = gpxFile.getRoutes().get(routeIndex);
		GpxDisplayGroup group = new RouteDisplayGroup(gpxFile, routeIndex);
		group.applyName(app, name);
		group.setDescription(route.getName());

		List<GpxDisplayItem> displayItems = new ArrayList<>();
		int i = 0;
		for (WptPt point : route.getPoints()) {
			GpxDisplayItem item = new GpxDisplayItem(null);
			item.group = group;
			item.description = point.getDesc();
			item.expanded = true;
			item.name = point.getName();
			i++;
			if (Algorithms.isEmpty(item.name)) {
				item.name = getString(R.string.gpx_selection_point, String.valueOf(i));
			}
			item.locationStart = point;
			item.locationEnd = point;
			displayItems.add(item);
		}
		group.addDisplayItems(displayItems);
		return group;
	}

	public GpxDisplayGroup buildPointsDisplayGroup(@NonNull GpxFile gpxFile, @NonNull List<WptPt> points, @NonNull String name) {
		GpxDisplayGroup group = new PointsDisplayGroup(gpxFile);
		group.applyName(app, name);
		group.setDescription(getString(R.string.gpx_selection_number_of_points, String.valueOf(gpxFile.getPointsSize())));
		List<GpxDisplayItem> displayItems = new ArrayList<>();
		int k = 0;
		for (WptPt wptPt : points) {
			GpxDisplayItem item = new GpxDisplayItem(null);
			item.group = group;
			item.description = wptPt.getDesc();
			item.name = wptPt.getName();
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

	public void updateDisplayGroupsNames(@NonNull SelectedGpxFile selectedGpxFile) {
		GpxFile gpxFile = selectedGpxFile.getGpxFile();
		List<GpxDisplayGroup> displayGroups = selectedGpxFile.getSplitGroups(app);
		if (displayGroups != null) {
			String name = getGroupName(app, gpxFile);
			for (GpxDisplayGroup group : displayGroups) {
				group.applyName(app, name);
			}
		}
	}

	@NonNull
	public static String getGroupName(@NonNull OsmandApplication app, @NonNull GpxFile gpxFile) {
		String name = gpxFile.getPath();
		if (gpxFile.isShowCurrentTrack()) {
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

	public void processSplitAsync(@NonNull SelectedGpxFile selectedGpxFile, @Nullable CallbackWithObject<Boolean> callback) {
		if (!app.isApplicationInitializing()) {
			splitTrackAsync(selectedGpxFile, callback);
		} else if (callback != null) {
			callback.processResult(false);
		}
	}

	private final ExecutorService splitTrackSingleThreadExecutor = Executors.newSingleThreadExecutor();

	@NonNull
	public List<GpxDisplayGroup> processSplitSync(@NonNull GpxFile gpxFile, @NonNull GpxDataItem dataItem) {
		GpxSplitParams params = new GpxSplitParams(app, dataItem);
		List<GpxDisplayGroup> groups = collectDisplayGroups(gpxFile, false);
		SplitTrackAsyncTask splitTask = new SplitTrackAsyncTask(app, params, groups, null);
		try {
			OsmAndTaskManager.executeTask(splitTask, splitTrackSingleThreadExecutor).get();
		} catch (ExecutionException | InterruptedException e) {
			log.error(e);
		}
		return groups;
	}

	private void splitTrackAsync(@NonNull SelectedGpxFile selectedGpxFile, @Nullable CallbackWithObject<Boolean> callback) {
		GpxFile gpxFile = selectedGpxFile.getGpxFile();
		GpxDataItem dataItem = app.getGpxDbHelper().getItem(new KFile(gpxFile.getPath()));
		if (!isSplittingTrack(selectedGpxFile) && dataItem != null) {
			GpxSplitParams params = new GpxSplitParams(app, dataItem);
			List<GpxDisplayGroup> groups = collectDisplayGroups(gpxFile, false);
			SplitTrackListener listener = getSplitTrackListener(selectedGpxFile, groups, callback);

			splitTrackAsync(selectedGpxFile, groups, params, listener);
		} else if (callback != null) {
			callback.processResult(false);
		}
	}

	public void splitTrackAsync(@NonNull SelectedGpxFile selectedGpxFile, @NonNull List<GpxDisplayGroup> groups,
	                            @NonNull GpxSplitParams splitParams, @Nullable SplitTrackListener listener) {
		boolean splittingTrack = isSplittingTrack(selectedGpxFile);
		boolean paramsChanged = splitParamsChanged(selectedGpxFile, splitParams);
		if (paramsChanged) {
			cancelTrackSplitting(selectedGpxFile);
		}
		if (paramsChanged || !splittingTrack) {
			String path = selectedGpxFile.getGpxFile().getPath();
			SplitTrackAsyncTask splitTask = new SplitTrackAsyncTask(app, splitParams, groups, new SplitTrackListener() {
				@Override
				public void trackSplittingStarted() {
					if (listener != null) {
						listener.trackSplittingStarted();
					}
				}

				@Override
				public void trackSplittingFinished(boolean success) {
					if (listener != null) {
						listener.trackSplittingFinished(success);
					}
					splitTrackTasks.remove(path);
				}
			});
			splitTrackTasks.put(path, splitTask);
			OsmAndTaskManager.executeTask(splitTask, splitTrackSingleThreadExecutor);
		}
	}

	private boolean splitParamsChanged(@NonNull SelectedGpxFile selectedGpxFile, @NonNull GpxSplitParams splitParams) {
		GpxFile gpxFile = selectedGpxFile.getGpxFile();
		SplitTrackAsyncTask splitTask = splitTrackTasks.get(gpxFile.getPath());
		if (splitTask != null) {
			return !Algorithms.objectEquals(splitParams, splitTask.getSplitParams());
		}
		return false;
	}

	public boolean isSplittingTrack(@NonNull SelectedGpxFile selectedGpxFile) {
		return splitTrackTasks.containsKey(selectedGpxFile.getGpxFile().getPath());
	}

	public void cancelTrackSplitting(@NonNull SelectedGpxFile selectedGpxFile) {
		SplitTrackAsyncTask splitTask = splitTrackTasks.get(selectedGpxFile.getGpxFile().getPath());
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
					selectedGpxFile.setSplitGroups(groups, app);
					app.getOsmandMap().getMapView().refreshMap();
				}
				if (callback != null) {
					callback.processResult(success);
				}
			}
		};
	}

	@NonNull
	public static String buildTrackSegmentName(GpxFile gpxFile, Track track, TrkSegment segment, OsmandApplication app) {
		String trackTitle = getTrackTitle(gpxFile, track, app);
		String segmentTitle = getSegmentTitle(segment, track.getSegments().indexOf(segment), app);

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
	private static String getTrackTitle(GpxFile gpxFile, Track track, OsmandApplication app) {
		String trackName;
		if (Algorithms.isBlank(track.getName())) {
			int trackIdx = gpxFile.getTracks().indexOf(track);
			trackName = String.valueOf(trackIdx + 1);
		} else {
			trackName = track.getName();
		}
		String trackString = app.getString(R.string.shared_string_gpx_track);
		return app.getString(R.string.ltr_or_rtl_combine_via_colon, trackString, trackName);
	}

	@NonNull
	private static String getSegmentTitle(@NonNull TrkSegment segment, int segmentIdx, OsmandApplication app) {
		String segmentName = Algorithms.isBlank(segment.getName()) ? String.valueOf(segmentIdx + 1) : segment.getName();
		String segmentString = app.getString(R.string.gpx_selection_segment_title);
		return app.getString(R.string.ltr_or_rtl_combine_via_colon, segmentString, segmentName);
	}

	@NonNull
	public static String getRouteTitle(@NonNull Route route, int index, OsmandApplication app) {
		String segmentName = Algorithms.isBlank(route.getName()) ? String.valueOf(index + 1) : route.getName();
		String segmentString = app.getString(R.string.layer_route);
		return app.getString(R.string.ltr_or_rtl_combine_via_colon, segmentString, segmentName);
	}

	private String getString(int resId, Object... formatArgs) {
		return app.getString(resId, formatArgs);
	}
}
