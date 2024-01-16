package net.osmand.plus.configmap.tracks;

import static net.osmand.plus.configmap.tracks.TracksAdapter.TYPE_NO_TRACKS;
import static net.osmand.plus.configmap.tracks.TracksAdapter.TYPE_NO_VISIBLE_TRACKS;
import static net.osmand.plus.configmap.tracks.TracksAdapter.TYPE_RECENTLY_VISIBLE_TRACKS;
import static net.osmand.plus.configmap.tracks.TracksAdapter.TYPE_SORT_TRACKS;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.LatLon;
import net.osmand.gpx.GPXFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.myplaces.tracks.ItemsSelectionHelper;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.monitoring.OsmandMonitoringPlugin;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.enums.TracksSortMode;
import net.osmand.plus.track.data.SmartFolder;
import net.osmand.plus.track.data.TrackFolder;
import net.osmand.plus.track.helpers.GpxSelectionHelper;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class TrackTabsHelper {

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final GpxSelectionHelper gpxSelectionHelper;
	private final ItemsSelectionHelper<TrackItem> itemsSelectionHelper;

	private final Set<TrackItem> recentlyVisibleTrackItem = new HashSet<>();
	private final Map<String, TrackTab> trackTabs = new LinkedHashMap<>();


	public TrackTabsHelper(@NonNull OsmandApplication app) {
		this.app = app;
		this.settings = app.getSettings();
		this.gpxSelectionHelper = app.getSelectedGpxHelper();
		this.itemsSelectionHelper = new ItemsSelectionHelper<>();
	}

	@NonNull
	public ItemsSelectionHelper<TrackItem> getItemsSelectionHelper() {
		return itemsSelectionHelper;
	}

	@NonNull
	public Map<String, TrackTab> getTrackTabs() {
		return trackTabs;
	}

	@NonNull
	public Set<TrackItem> getRecentlyVisibleTracks() {
		return new HashSet<>(recentlyVisibleTrackItem);
	}

	public void updateTrackItems(@NonNull List<TrackItem> trackItems) {
		List<TrackItem> allTrackItems = new ArrayList<>(trackItems);
		if (settings.SAVE_GLOBAL_TRACK_TO_GPX.get() || gpxSelectionHelper.getSelectedCurrentRecordingTrack() != null) {
			SelectedGpxFile selectedGpxFile = app.getSavingTrackHelper().getCurrentTrack();
			TrackItem trackItem = new TrackItem(app, selectedGpxFile.getGpxFile());
			allTrackItems.add(trackItem);
		}
		itemsSelectionHelper.setAllItems(allTrackItems);
		Map<String, TrackTab> trackTabs = new LinkedHashMap<>();
		for (TrackItem item : trackItems) {
			addTrackItem(trackTabs, item);
		}
		updateTrackTabs(trackTabs);
	}

	public void updateItems(@NonNull TrackFolder folder){
		List<TrackItem> allTrackItems = new ArrayList<>(folder.getFlattenedTrackItems());
		if (settings.SAVE_GLOBAL_TRACK_TO_GPX.get() || gpxSelectionHelper.getSelectedCurrentRecordingTrack() != null) {
			SelectedGpxFile selectedGpxFile = app.getSavingTrackHelper().getCurrentTrack();
			TrackItem trackItem = new TrackItem(app, selectedGpxFile.getGpxFile());
			allTrackItems.add(trackItem);
		}
		itemsSelectionHelper.setAllItems(allTrackItems);
		updateSelectTrackTabs(folder);
	}

	private void updateTrackTabs(@NonNull Map<String, TrackTab> folderTabs) {
		processVisibleTracks();
		processRecentlyVisibleTracks();
		trackTabs.clear();
		trackTabs.put(TrackTabType.ON_MAP.name(), getTracksOnMapTab());
		trackTabs.put(TrackTabType.ALL.name(), getAllTracksTab());
		trackTabs.putAll(getAllSmartFoldersTabs());
		trackTabs.putAll(folderTabs);
		loadTabsSortModes();
		sortTrackTabs();
	}

	private void updateSelectTrackTabs(@NonNull TrackFolder folder) {
		processVisibleTracks();
		processRecentlyVisibleTracks();
		trackTabs.clear();
		trackTabs.put(app.getString(R.string.shared_string_visible), getTracksOnMapTab());
		trackTabs.put(app.getString(R.string.shared_string_all_tracks), getAllTracksTab());
		trackTabs.put(app.getString(R.string.shared_string_folders), getFoldersTab(folder));
		loadTabsSortModes();
		sortTrackTabs();
	}

	@NonNull
	private TrackTab getTracksOnMapTab() {
		TrackTab trackTab = new TrackTab(TrackTabType.ON_MAP);
		trackTab.items.addAll(getOnMapTabItems());
		return trackTab;
	}

	public void updateTracksOnMap() {
		TrackTab onMapTab = getTracksOnMapTab();
		trackTabs.put(TrackTabType.ON_MAP.name(), onMapTab);
		sortTrackTab(onMapTab);
	}

	@NonNull
	private TrackTab getAllTracksTab() {
		TrackTab trackTab = new TrackTab(TrackTabType.ALL);
		trackTab.items.addAll(getAllTabItems());
		return trackTab;
	}

	@NonNull
	private List<Object> getOnMapTabItems() {
		List<Object> items = new ArrayList<>();
		items.add(TYPE_SORT_TRACKS);
		items.addAll(getVisibleItems());
		items.addAll(getRecentlyVisibleItems());
		return items;
	}

	@NonNull
	private List<Object> getAllTabItems() {
		List<Object> items = new ArrayList<>();
		items.add(TYPE_SORT_TRACKS);

		Set<TrackItem> allTrackItems = itemsSelectionHelper.getAllItems();
		if (Algorithms.isEmpty(allTrackItems)) {
			items.add(TYPE_NO_TRACKS);
		} else {
			items.addAll(allTrackItems);
		}
		return items;
	}

	@NonNull
	private TrackTab getFoldersTab(@NonNull TrackFolder folder) {
		TrackTab trackTab = new TrackTab(TrackTabType.FOLDERS);
		trackTab.items.add(TYPE_SORT_TRACKS);
		trackTab.items.addAll(folder.getSubFolders());
		trackTab.items.addAll(folder.getTrackItems());
		return trackTab;
	}

	@NonNull
	private List<Object> getRecentlyVisibleItems() {
		List<Object> items = new ArrayList<>();
		if (!Algorithms.isEmpty(recentlyVisibleTrackItem)) {
			items.add(TYPE_RECENTLY_VISIBLE_TRACKS);
			items.addAll(recentlyVisibleTrackItem);
		}
		return items;
	}

	@NonNull
	private List<Object> getVisibleItems() {
		List<Object> items = new ArrayList<>();
		Set<TrackItem> originalSelectedItems = itemsSelectionHelper.getOriginalSelectedItems();
		if (!Algorithms.isEmpty(originalSelectedItems)) {
			items.addAll(originalSelectedItems);
		} else if (Algorithms.isEmpty(itemsSelectionHelper.getAllItems())) {
			items.add(TYPE_NO_TRACKS);
		} else {
			items.add(TYPE_NO_VISIBLE_TRACKS);
		}
		return items;
	}

	public void processRecentlyVisibleTracks() {
		recentlyVisibleTrackItem.clear();
		boolean monitoringActive = PluginsHelper.isActive(OsmandMonitoringPlugin.class);
		for (GPXFile gpxFile : gpxSelectionHelper.getSelectedGpxFilesBackUp().keySet()) {
			SelectedGpxFile selectedGpxFile = gpxSelectionHelper.getSelectedFileByPath(gpxFile.path);
			if (selectedGpxFile == null && (!gpxFile.showCurrentTrack || monitoringActive)) {
				recentlyVisibleTrackItem.add(new TrackItem(app, gpxFile));
			}
		}
	}

	public void processVisibleTracks() {
		List<TrackItem> selectedItems = new ArrayList<>();
		if (gpxSelectionHelper.isAnyGpxFileSelected()) {
			for (TrackItem info : itemsSelectionHelper.getAllItems()) {
				SelectedGpxFile selectedGpxFile = gpxSelectionHelper.getSelectedFileByPath(info.getPath());
				if (selectedGpxFile != null) {
					selectedItems.add(info);
				}
			}
		}
		itemsSelectionHelper.setSelectedItems(selectedItems);
		itemsSelectionHelper.setOriginalSelectedItems(selectedItems);
	}

	@NonNull
	private Map<String, TrackTab> getAllSmartFoldersTabs() {
		Map<String, TrackTab> smartFoldersTabs = new LinkedHashMap<>();
		for (SmartFolder folder : app.getSmartFolderHelper().getSmartFolders()) {
			TrackTab folderTab = new TrackTab(folder);
			folderTab.items.add(TYPE_SORT_TRACKS);
			folderTab.items.addAll(folder.getTrackItems());
			smartFoldersTabs.put(folderTab.getTypeName(), folderTab);
		}
		return smartFoldersTabs;
	}

	@Nullable
	private TrackTab addTrackItem(@NonNull Map<String, TrackTab> trackTabs, @NonNull TrackItem item) {
		File file = item.getFile();
		if (file != null && file.getParentFile() != null) {
			File dir = file.getParentFile();
			TrackTab trackTab = trackTabs.get(dir.getName());
			if (trackTab == null) {
				trackTab = new TrackTab(dir);
				trackTab.items.add(TYPE_SORT_TRACKS);
				trackTabs.put(trackTab.getTypeName(), trackTab);
			}
			trackTab.items.add(item);
			return trackTab;
		}
		return null;
	}

	public void addTrackItem(@NonNull TrackItem item) {
		itemsSelectionHelper.addItemToAll(item);

		TrackTab onMapTab = trackTabs.get(TrackTabType.ON_MAP.name());
		if (onMapTab != null) {
			onMapTab.items.clear();
			onMapTab.items.addAll(getOnMapTabItems());
			sortTrackTab(onMapTab);
		}
		TrackTab allTab = trackTabs.get(TrackTabType.ALL.name());
		if (allTab != null) {
			allTab.items.clear();
			allTab.items.addAll(getAllTabItems());
			sortTrackTab(allTab);
		}
		TrackTab folderTab = addTrackItem(trackTabs, item);
		if (folderTab != null) {
			sortTrackTab(folderTab);
		}
	}

	public void saveTracksVisibility() {
		gpxSelectionHelper.saveTracksVisibility(itemsSelectionHelper.getSelectedItems());
	}

	private void sortTrackTabs() {
		for (TrackTab trackTab : trackTabs.values()) {
			sortTrackTab(trackTab);
		}
	}

	public void sortTrackTab(@NonNull TrackTab trackTab) {
		LatLon latLon = app.getMapViewTrackingUtilities().getDefaultLocation();
		if (trackTab.type == TrackTabType.ON_MAP) {
			List<Object> visibleItems = getVisibleItems();
			List<Object> recentlyVisibleItems = getRecentlyVisibleItems();

			TracksComparator comparator = new TracksComparator(trackTab, latLon);
			Collections.sort(visibleItems, comparator);
			Collections.sort(recentlyVisibleItems, comparator);

			trackTab.items.clear();
			trackTab.items.add(TYPE_SORT_TRACKS);
			trackTab.items.addAll(visibleItems);
			trackTab.items.addAll(recentlyVisibleItems);
		} else {
			Collections.sort(trackTab.items, new TracksComparator(trackTab, latLon));
		}
	}

	public void loadTabsSortModes() {
		Map<String, String> tabsSortModes = settings.getTrackSortModes();
		if (!Algorithms.isEmpty(tabsSortModes)) {
			for (Entry<String, String> entry : tabsSortModes.entrySet()) {
				TrackTab trackTab = trackTabs.get(entry.getKey());
				if (trackTab != null) {
					trackTab.setSortMode(TracksSortMode.getByValue(entry.getValue()));
				}
			}
		}
	}

	public void saveTabsSortModes() {
		Map<String, String> tabsSortModes = new HashMap<>();
		for (TrackTab trackTab : trackTabs.values()) {
			String name = trackTab.getTypeName();
			String sortType = trackTab.getSortMode().name();
			tabsSortModes.put(name, sortType);
		}
		settings.saveTabsSortModes(tabsSortModes);
	}
}