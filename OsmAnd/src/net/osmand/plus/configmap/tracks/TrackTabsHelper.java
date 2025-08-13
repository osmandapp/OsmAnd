package net.osmand.plus.configmap.tracks;

import static net.osmand.plus.configmap.tracks.TracksAdapter.TYPE_NO_TRACKS;
import static net.osmand.plus.configmap.tracks.TracksAdapter.TYPE_NO_VISIBLE_TRACKS;
import static net.osmand.plus.configmap.tracks.TracksAdapter.TYPE_RECENTLY_VISIBLE_TRACKS;
import static net.osmand.plus.configmap.tracks.TracksAdapter.TYPE_SORT_TRACKS;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.shared.SharedUtil;
import net.osmand.data.LatLon;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.myplaces.tracks.ItemsSelectionHelper;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.monitoring.OsmandMonitoringPlugin;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.enums.TracksSortMode;
import net.osmand.shared.gpx.data.SmartFolder;
import net.osmand.shared.gpx.data.TrackFolder;
import net.osmand.plus.track.helpers.GpxSelectionHelper;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.shared.gpx.TrackItem;
import net.osmand.shared.io.KFile;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
	protected final Map<String, TrackTab> trackTabs = new LinkedHashMap<>();

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
	public List<TrackTab> getSortedTrackTabs(boolean useSubdirs) {
		List<TrackTab> result = getTrackTabs();
		result.sort(new TracksComparator(getRootSortMode(), getDefaultLocation(), useSubdirs));
		return result;
	}

	@NonNull
	public List<TrackTab> getTrackTabs() {
		return new ArrayList<>(trackTabs.values());
	}

	@Nullable
	public TrackTab getTrackTab(@NonNull String id) {
		return trackTabs.get(id);
	}

	@NonNull
	public Set<TrackItem> getRecentlyVisibleTracks() {
		return new HashSet<>(recentlyVisibleTrackItem);
	}

	public void updateTrackItems(@NonNull TrackFolder rootFolder) {
		List<TrackItem> allTrackItems = new ArrayList<>(rootFolder.getFlattenedTrackItems());
		addCurrentTrackItemIfPresent(allTrackItems);
		itemsSelectionHelper.setAllItems(allTrackItems);

		processVisibleTracks();
		processRecentlyVisibleTracks();

		updateTrackTabs(rootFolder);
		loadTabsSortModes();
		sortTrackTabsContent();
	}

	protected void updateTrackTabs(@NonNull TrackFolder rootFolder) {
		trackTabs.clear();
		trackTabs.put(TrackTabType.ON_MAP.name(), getTracksOnMapTab());
		trackTabs.put(TrackTabType.ALL.name(), getAllTracksTab());
		for (TrackTab tab : getAllSmartFoldersTabs()) {
			trackTabs.put(tab.getId(), tab);
		}
		for (TrackTab tab : getAllTrackFoldersTabs(rootFolder)) {
			trackTabs.put(tab.getId(), tab);
		}
	}

	private void addCurrentTrackItemIfPresent(@NonNull List<TrackItem> trackItems) {
		if (settings.SAVE_GLOBAL_TRACK_TO_GPX.get() || gpxSelectionHelper.getSelectedCurrentRecordingTrack() != null) {
			SelectedGpxFile selectedGpxFile = app.getSavingTrackHelper().getCurrentTrack();
			TrackItem trackItem = new TrackItem(selectedGpxFile.getGpxFile());
			trackItems.add(trackItem);
		}
	}

	@NonNull
	private Collection<TrackTab> getAllTrackFoldersTabs(@NonNull TrackFolder rootFolder) {
		List<TrackItem> trackItems = rootFolder.getFlattenedTrackItems();
		Map<String, TrackTab> trackFolderTabs = new LinkedHashMap<>();
		for (TrackItem item : trackItems) {
			addTrackItem(trackFolderTabs, item);
		}
		return trackFolderTabs.values();
	}

	@NonNull
	protected TrackTab getTracksOnMapTab() {
		TrackTab trackTab = new TrackTab(app, TrackTabType.ON_MAP);
		trackTab.items.addAll(getOnMapTabItems());
		return trackTab;
	}

	public void updateTracksOnMap() {
		TrackTab onMapTab = getTracksOnMapTab();
		trackTabs.put(TrackTabType.ON_MAP.name(), onMapTab);
		sortTrackTab(onMapTab);
	}

	@NonNull
	protected TrackTab getAllTracksTab() {
		TrackTab trackTab = new TrackTab(app, TrackTabType.ALL);
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
	protected TrackTab getFoldersTab(@NonNull TrackFolder folder) {
		TrackTab trackTab = new TrackTab(app, TrackTabType.FOLDERS);
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
		for (GpxFile gpxFile : gpxSelectionHelper.getSelectedGpxFilesBackUp().keySet()) {
			SelectedGpxFile selectedGpxFile = gpxSelectionHelper.getSelectedFileByPath(gpxFile.getPath());
			if (selectedGpxFile == null && (!gpxFile.isShowCurrentTrack() || monitoringActive)) {
				recentlyVisibleTrackItem.add(new TrackItem(gpxFile));
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
	private List<TrackTab> getAllSmartFoldersTabs() {
		List<TrackTab> smartFoldersTabs = new ArrayList<>();
		for (SmartFolder folder : app.getSmartFolderHelper().getSmartFolders()) {
			TrackTab folderTab = new TrackTab(app, folder);
			folderTab.items.add(TYPE_SORT_TRACKS);
			folderTab.items.addAll(folder.getTrackItems());
			smartFoldersTabs.add(folderTab);
		}
		return smartFoldersTabs;
	}

	@Nullable
	private TrackTab addTrackItem(@NonNull Map<String, TrackTab> trackTabs, @NonNull TrackItem item) {
		KFile file = item.getFile();
		if (file != null && file.getParentFile() != null) {
			KFile dir = file.getParentFile();
			if (dir != null) {
				String folderId = TrackSortModesHelper.getFolderId(dir.absolutePath());
				TrackTab trackTab = trackTabs.get(folderId);
				if (trackTab == null) {
					trackTab = new TrackTab(app, SharedUtil.jFile(dir));
					trackTab.items.add(TYPE_SORT_TRACKS);
					trackTabs.put(folderId, trackTab);
				}
				trackTab.items.add(item);
				return trackTab;
			}
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

	private void sortTrackTabsContent() {
		for (TrackTab trackTab : trackTabs.values()) {
			sortTrackTab(trackTab);
		}
	}

	public void sortTrackTab(@NonNull TrackTab trackTab) {
		LatLon latLon = getDefaultLocation();
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
		TrackSortModesHelper sortModesHelper = app.getTrackSortModesHelper();
		for (TrackTab trackTab : trackTabs.values()) {
			TracksSortMode sortMode = sortModesHelper.getSortMode(trackTab.getId());
			if (sortMode != null) {
				trackTab.setSortMode(sortMode);
			}
		}
	}

	public void saveTabSortMode(@NonNull TrackTab trackTab) {
		TrackSortModesHelper sortModesHelper = app.getTrackSortModesHelper();
		sortModesHelper.setSortMode(trackTab.getId(), trackTab.getSortMode());
		sortModesHelper.syncSettings();
	}

	public void saveTabsSortModes() {
		TrackSortModesHelper sortModesHelper = app.getTrackSortModesHelper();
		for (TrackTab trackTab : trackTabs.values()) {
			sortModesHelper.setSortMode(trackTab.getId(), trackTab.getSortMode());
		}
		sortModesHelper.syncSettings();
	}

	@NonNull
	private TracksSortMode getRootSortMode() {
		return app.getTrackSortModesHelper().getRootFolderSortMode();
	}

	@NonNull
	private LatLon getDefaultLocation() {
		return app.getMapViewTrackingUtilities().getDefaultLocation();
	}
}
