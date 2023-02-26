package net.osmand.plus.configmap.tracks;

import static net.osmand.plus.configmap.tracks.TracksAdapter.TYPE_NO_TRACKS;
import static net.osmand.plus.configmap.tracks.TracksAdapter.TYPE_NO_VISIBLE_TRACKS;
import static net.osmand.plus.configmap.tracks.TracksAdapter.TYPE_RECENTLY_VISIBLE_TRACKS;
import static net.osmand.plus.configmap.tracks.TracksAdapter.TYPE_SORT_TRACKS;

import androidx.annotation.NonNull;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.gpx.GPXFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.track.helpers.GPXInfo;
import net.osmand.plus.track.helpers.GpxSelectionHelper;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SelectedTracksHelper {

	private final OsmandApplication app;
	private final GpxSelectionHelper selectionHelper;

	private final Set<GPXInfo> selectedGpxInfo = new HashSet<>();
	private final Set<GPXInfo> originalSelectedGpxInfo = new HashSet<>();
	private final Set<GPXInfo> recentlyVisibleGpxInfo = new HashSet<>();
	private final Map<String, TrackTab> trackTabs = new LinkedHashMap<>();


	public SelectedTracksHelper(@NonNull OsmandApplication app) {
		this.app = app;
		this.selectionHelper = app.getSelectedGpxHelper();
	}

	@NonNull
	public Set<GPXInfo> getSelectedTracks() {
		return new HashSet<>(selectedGpxInfo);
	}

	@NonNull
	public Set<GPXInfo> getRecentlyVisibleTracks() {
		return new HashSet<>(recentlyVisibleGpxInfo);
	}

	@NonNull
	public Map<String, TrackTab> getTrackTabs() {
		return trackTabs;
	}

	public boolean hasItemsToApply() {
		return !Algorithms.objectEquals(selectedGpxInfo, originalSelectedGpxInfo);
	}

	public boolean hasSelectedTracks() {
		return !selectedGpxInfo.isEmpty();
	}

	public boolean hasRecentlyVisibleTracks() {
		return !recentlyVisibleGpxInfo.isEmpty();
	}

	public void onGpxInfosSelected(@NonNull Set<GPXInfo> gpxInfos, boolean selected) {
		if (selected) {
			selectedGpxInfo.addAll(gpxInfos);
		} else {
			selectedGpxInfo.removeAll(gpxInfos);
		}
	}

	public void updateTrackTabs(@NonNull Map<String, TrackTab> folderTabs, @NonNull List<GPXInfo> gpxInfos) {
		trackTabs.clear();

		TrackTab allTab = getAllTracksTab(gpxInfos);
		TrackTab mapTab = getTracksOnMapTab(gpxInfos);

		trackTabs.put(mapTab.name, mapTab);
		trackTabs.put(allTab.name, allTab);
		trackTabs.putAll(folderTabs);
		sortTracks();
	}

	private TrackTab getAllTracksTab(@NonNull List<GPXInfo> gpxInfos) {
		TrackTab trackTab = new TrackTab(app.getString(R.string.shared_string_all), TrackTabType.ALL);
		trackTab.items.add(TYPE_SORT_TRACKS);
		if (Algorithms.isEmpty(gpxInfos)) {
			trackTab.items.add(TYPE_NO_TRACKS);
		} else {
			trackTab.items.addAll(gpxInfos);
		}
		return trackTab;
	}

	private TrackTab getTracksOnMapTab(@NonNull List<GPXInfo> gpxInfos) {
		TrackTab trackTab = new TrackTab(app.getString(R.string.shared_string_on_map), TrackTabType.ON_MAP);
		trackTab.items.add(TYPE_SORT_TRACKS);

		if (selectionHelper.isAnyGpxFileSelected()) {
			for (GPXInfo info : gpxInfos) {
				SelectedGpxFile selectedGpxFile = selectionHelper.getSelectedFileByName(info.getFileName());
				if (selectedGpxFile != null) {
					trackTab.items.add(info);
					selectedGpxInfo.add(info);
					originalSelectedGpxInfo.add(info);
				}
			}
		} else if (Algorithms.isEmpty(gpxInfos)) {
			trackTab.items.add(TYPE_NO_TRACKS);
		} else {
			trackTab.items.add(TYPE_NO_VISIBLE_TRACKS);
		}
		Map<GPXFile, Long> selectedGpxFilesBackUp = selectionHelper.getSelectedGpxFilesBackUp();
		if (!selectedGpxFilesBackUp.isEmpty()) {
			trackTab.items.add(TYPE_RECENTLY_VISIBLE_TRACKS);
			for (GPXFile gpxFile : selectedGpxFilesBackUp.keySet()) {
				File file = new File(gpxFile.path);
				GPXInfo info = new GPXInfo(file.getName(), file);
				info.setGpxFile(gpxFile);
				trackTab.items.add(info);
				recentlyVisibleGpxInfo.add(info);
			}
		}
		return trackTab;
	}

	public void addLocalIndexInfo(@NonNull Map<String, TrackTab> trackTabs, @NonNull GPXInfo info) {
		String name = info.isCurrentRecordingTrack() ? info.getName() : info.subfolder;
		if (Algorithms.isEmpty(name)) {
			name = app.getString(R.string.shared_string_tracks);
		}

		TrackTab trackTab = trackTabs.get(name);
		if (trackTab == null) {
			trackTab = new TrackTab(name, TrackTabType.FOLDER);
			trackTab.items.add(TYPE_SORT_TRACKS);
			trackTabs.put(name, trackTab);
		}
		trackTab.items.add(info);
	}

	private void sortTracks() {
		for (TrackTab trackTab : trackTabs.values()) {
			sortTracks(trackTab);
		}
	}

	public void sortTracks(@NonNull TrackTab trackTab) {
		LatLon latLon = getCurrentLocation();
		TracksSortMode sortMode = trackTab.getSortMode();
		Collections.sort(trackTab.items, new TracksComparator(sortMode, latLon));
	}

	private LatLon getCurrentLocation() {
		Location location = app.getLocationProvider().getLastKnownLocation();
		if (location == null) {
			location = app.getLocationProvider().getLastStaleKnownLocation();
		}
		if (location != null) {
			return new LatLon(location.getLatitude(), location.getLongitude());
		}
		return app.getMapViewTrackingUtilities().getMapLocation();
	}
}