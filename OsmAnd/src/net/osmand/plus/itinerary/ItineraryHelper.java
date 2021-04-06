package net.osmand.plus.itinerary;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXExtensionsWriter;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.ItineraryGroupItem;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.binary.StringBundle;
import net.osmand.binary.StringBundleWriter;
import net.osmand.binary.StringBundleXmlWriter;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.FavouritesDbHelper.FavoriteGroup;
import net.osmand.plus.GPXDatabase.GpxDataItem;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.itinerary.ItineraryGroup.ItineraryType;
import net.osmand.plus.mapmarkers.CategoriesSubHeader;
import net.osmand.plus.mapmarkers.GroupHeader;
import net.osmand.plus.mapmarkers.MapMarker;
import net.osmand.plus.mapmarkers.MapMarkersDbHelper;
import net.osmand.plus.mapmarkers.MapMarkersHelper;
import net.osmand.plus.mapmarkers.MapMarkersHelper.OnGroupSyncedListener;
import net.osmand.plus.mapmarkers.ShowHideHistoryButton;
import net.osmand.plus.wikivoyage.data.TravelArticle;
import net.osmand.plus.wikivoyage.data.TravelHelper;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static net.osmand.plus.FavouritesDbHelper.backup;
import static net.osmand.plus.mapmarkers.MapMarkersDbHelper.DB_NAME;
import static net.osmand.plus.mapmarkers.MapMarkersHelper.BY_DATE_ADDED_DESC;

public class ItineraryHelper {

	private static final Log log = PlatformUtil.getLog(ItineraryHelper.class);

	private static final String CATEGORIES_SPLIT = ",";
	private static final String FILE_TO_SAVE = "itinerary.gpx";
	private static final String FILE_TO_BACKUP = "itinerary_bak.gpx";

	private OsmandApplication app;

	private MapMarkersHelper markersHelper;
	private MapMarkersDbHelper markersDbHelper;

	private ExecutorService executorService = Executors.newSingleThreadExecutor();

	private List<ItineraryGroup> itineraryGroups = new ArrayList<>();
	private Set<OnGroupSyncedListener> syncListeners = new HashSet<>();

	public ItineraryHelper(@NonNull OsmandApplication app) {
		this.app = app;
		markersHelper = app.getMapMarkersHelper();
		markersDbHelper = app.getMapMarkersDbHelper();
		loadGroups();
	}

	public void loadGroups() {
		itineraryGroups.clear();

		File internalFile = getInternalFile();
		if (!internalFile.exists()) {
			File dbPath = app.getDatabasePath(DB_NAME);
			if (dbPath.exists()) {
				loadAndCheckDatabasePoints();
				removeNonExistingItems();
				saveCurrentPointsIntoFile();
			}
		}
		Map<String, ItineraryGroup> points = new LinkedHashMap<>();
		Map<String, ItineraryGroup> extPoints = new LinkedHashMap<>();
		loadGPXFile(internalFile, points);
		loadGPXFile(getExternalFile(), extPoints);
		boolean changed = merge(extPoints, points);

		itineraryGroups = new ArrayList<>(points.values());

		sortGroups();
		removeNonExistingItems();
		if (changed || !getExternalFile().exists()) {
			saveCurrentPointsIntoFile();
		}
		for (ItineraryGroup group : itineraryGroups) {
			updateGroup(group);
		}
	}

	private void loadAndCheckDatabasePoints() {
		Map<String, ItineraryGroup> groupsMap = markersDbHelper.getAllGroupsMap();
		List<MapMarker> mapMarkers = new ArrayList<>(markersHelper.getMapMarkers());

		ItineraryGroup noGroup = null;
		for (MapMarker marker : mapMarkers) {
			ItineraryGroup group = groupsMap.get(marker.groupKey);
			if (group == null) {
				if (noGroup == null) {
					noGroup = new ItineraryGroup();
					noGroup.setCreationDate(Long.MAX_VALUE);
				}
				noGroup.getMarkers().add(marker);
			} else {
				if (marker.creationDate < group.getCreationDate()) {
					group.setCreationDate(marker.creationDate);
				}
				group.getMarkers().add(marker);
			}
		}
		itineraryGroups = new ArrayList<>(groupsMap.values());
		if (noGroup != null) {
			markersHelper.sortMarkers(noGroup.getMarkers(), false, BY_DATE_ADDED_DESC);
			addToGroupsList(noGroup);
		}
		sortGroups();
	}

	private boolean loadGPXFile(File file, Map<String, ItineraryGroup> groups) {
		if (!file.exists()) {
			return false;
		}
		GPXFile gpxFile = GPXUtilities.loadGPXFile(file);
		if (gpxFile.error != null) {
			return false;
		}
		for (ItineraryGroupItem itinerary : gpxFile.itineraryGroups) {
			ItineraryType type = ItineraryType.findTypeForName(itinerary.type);
			ItineraryGroup group = new ItineraryGroup(itinerary.id, itinerary.name, type);
			group.setDisabled(Boolean.parseBoolean(itinerary.disabled));
			group.setWptCategories(decodeWptCategories(itinerary.categories));

			List<MapMarker> markers = new ArrayList<>();
			for (WptPt point : gpxFile.getPoints()) {
				String alias = point.getExtensionsToRead().get("alias");
				String itineraryId = point.getExtensionsToRead().get("itinerary_id");
				if (Algorithms.stringsEqual(group.getId(), alias) && itineraryId != null) {
					MapMarker marker = markersHelper.getMapMarker(itineraryId);
					if (marker != null && !marker.history) {
						markers.add(marker);
					}
				}
			}
			group.setMarkers(markers);
			groups.put(group.getId(), group);
		}
		return true;
	}

	public void saveCurrentPointsIntoFile() {
		try {
			saveFile(itineraryGroups, getInternalFile());
			saveFile(itineraryGroups, getExternalFile());
			backup(getBackupFile(), getExternalFile());
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	public void removeNonExistingItems() {
		Iterator<ItineraryGroup> iterator = itineraryGroups.iterator();
		while (iterator.hasNext()) {
			ItineraryGroup group = iterator.next();
			if (group.getType() == ItineraryType.TRACK && !new File(group.getId()).exists()) {
				markersDbHelper.removeMarkersGroup(group.getId());
				iterator.remove();
			}
		}
	}

	public static Set<String> decodeWptCategories(String wptCategories) {
		if (Algorithms.isEmpty(wptCategories)) {
			return Collections.emptySet();
		} else if (wptCategories.contains(String.valueOf(0x01))) {
			return Algorithms.decodeStringSet(wptCategories);
		} else {
			return Algorithms.decodeStringSet(wptCategories, CATEGORIES_SPLIT);
		}
	}

	private boolean merge(Map<String, ItineraryGroup> source, Map<String, ItineraryGroup> destination) {
		boolean changed = false;
		for (Map.Entry<String, ItineraryGroup> entry : source.entrySet()) {
			String ks = entry.getKey();
			if (!destination.containsKey(ks)) {
				changed = true;
				destination.put(ks, entry.getValue());
			}
		}
		return changed;
	}

	public File getExternalFile() {
		return new File(app.getAppPath(null), FILE_TO_SAVE);
	}

	private File getInternalFile() {
		return app.getFileStreamPath(FILE_TO_BACKUP);
	}

	public File getBackupFile() {
		return FavouritesDbHelper.getBackupFile(app, "itinerary_bak_");
	}

	public Exception saveFile(List<ItineraryGroup> favoritePoints, File file) {
		GPXFile gpx = asGpxFile(favoritePoints);
		return GPXUtilities.writeGpxFile(file, gpx);
	}

	public GPXFile asGpxFile(List<ItineraryGroup> itineraryGroups) {
		GPXFile gpx = new GPXFile(Version.getFullVersion(app));
		List<ItineraryGroupItem> groups = new ArrayList<>();
		for (ItineraryGroup group : itineraryGroups) {
			ItineraryGroupItem itinerary = new ItineraryGroupItem();
			itinerary.id = group.getId();
			itinerary.name = group.getName();
			itinerary.categories = Algorithms.encodeStringSet(group.getWptCategories(), CATEGORIES_SPLIT);
			itinerary.type = group.getType().getTypeName();
			itinerary.disabled = String.valueOf(group.isDisabled());
			groups.add(itinerary);
			for (MapMarker mapMarker : group.getMarkers()) {
				WptPt wptPt = new WptPt();
				wptPt.getExtensionsToWrite().put("itinerary_id", mapMarker.id);
				wptPt.getExtensionsToWrite().put("alias", group.getId());
				gpx.addPoint(wptPt);
			}
		}
		assignRouteExtensionWriter(gpx, groups);
		return gpx;
	}

	private void assignRouteExtensionWriter(GPXFile gpx, final List<ItineraryGroupItem> groups) {
		if (gpx.getExtensionsWriter() == null) {
			gpx.setExtensionsWriter(new GPXExtensionsWriter() {
				@Override
				public void writeExtensions(XmlSerializer serializer) {
					StringBundle bundle = new StringBundle();
					List<StringBundle> segmentsBundle = new ArrayList<>();
					for (ItineraryGroupItem group : groups) {
						segmentsBundle.add(group.toStringBundle());
					}
					bundle.putBundleList("itinerary_groups", "itinerary_group", segmentsBundle);
					StringBundleWriter bundleWriter = new StringBundleXmlWriter(bundle, serializer);
					bundleWriter.writeBundle();
				}
			});
		}
	}

	public List<ItineraryGroup> getItineraryGroups() {
		return itineraryGroups;
	}

	public void syncAllGroupsAsync() {
		for (ItineraryGroup group : itineraryGroups) {
			if (group.getId() != null && group.getName() != null) {
				runSynchronization(group);
			}
		}
	}

	public void updateGroupWptCategories(@NonNull ItineraryGroup group, Set<String> wptCategories) {
		String id = group.getId();
		if (id != null) {
			group.setWptCategories(wptCategories);
			if (wptCategories != null) {
				markersDbHelper.updateGroupCategories(id, group.getWptCategoriesString());
			}
		}
	}

	public void enableGroup(@NonNull ItineraryGroup gr) {
		// check if group doesn't exist internally
		if (!itineraryGroups.contains(gr)) {
			addGroupInternally(gr);
		}
		if (gr.isDisabled()) {
			updateGroupDisabled(gr, false);
		}
		runSynchronization(gr);
	}

	public void updateGroups() {
		for (ItineraryGroup group : itineraryGroups) {
			markersHelper.updateGroup(group);
		}
	}

	public void updateGroupDisabled(@NonNull ItineraryGroup group, boolean disabled) {
		String id = group.getId();
		if (id != null) {
			markersDbHelper.updateGroupDisabled(id, disabled);
			group.setDisabled(disabled);
		}
	}

	public List<MapMarker> getMapMarkersFromDefaultGroups(boolean history) {
		List<MapMarker> mapMarkers = new ArrayList<>();
		for (ItineraryGroup group : itineraryGroups) {
			if (group.getType() == ItineraryGroup.ANY_TYPE) {
				for (MapMarker marker : group.getMarkers()) {
					if (history && marker.history || !history && !marker.history) {
						mapMarkers.add(marker);
					}
				}
			}
		}
		return mapMarkers;
	}

	public void addGroupInternally(ItineraryGroup gr) {
		markersDbHelper.addGroup(gr);
		markersHelper.addHistoryMarkersToGroup(gr);
		addToGroupsList(gr);
	}

	public void updateGpxShowAsMarkers(File file) {
		GpxDataItem dataItem = app.getGpxDbHelper().getItem(file);
		if (dataItem != null) {
			app.getGpxDbHelper().updateShowAsMarkers(dataItem, true);
			dataItem.setShowAsMarkers(true);
		}
	}

	public void addToGroupsList(ItineraryGroup group) {
		List<ItineraryGroup> copyList = new ArrayList<>(itineraryGroups);
		copyList.add(group);
		itineraryGroups = copyList;
	}

	public void removeFromGroupsList(ItineraryGroup group) {
		List<ItineraryGroup> copyList = new ArrayList<>(itineraryGroups);
		copyList.remove(group);
		itineraryGroups = copyList;
	}

	public void addSyncListener(OnGroupSyncedListener listener) {
		syncListeners.add(listener);
	}

	public void removeSyncListener(OnGroupSyncedListener listener) {
		syncListeners.remove(listener);
	}

	public void runSynchronization(final @NonNull ItineraryGroup group) {
		app.runInUIThread(new Runnable() {
			@Override
			public void run() {
				new SyncGroupTask(group).executeOnExecutor(executorService);
			}
		});
	}

	public ItineraryGroup getMarkersGroup(GPXFile gpx) {
		if (gpx == null || gpx.path == null) {
			return null;
		}
		return getMapMarkerGroupById(getMarkerGroupId(new File(gpx.path)), ItineraryGroup.GPX_TYPE);
	}

	public ItineraryGroup getMarkersGroup(FavoriteGroup favGroup) {
		return getMapMarkerGroupById(getMarkerGroupId(favGroup), ItineraryGroup.FAVORITES_TYPE);
	}

	public ItineraryGroup addOrEnableGpxGroup(@NonNull File file) {
		updateGpxShowAsMarkers(file);
		ItineraryGroup gr = getMapMarkerGroupById(getMarkerGroupId(file), ItineraryGroup.GPX_TYPE);
		if (gr == null) {
			gr = createGPXMarkerGroup(file);
			addGroupInternally(gr);
		}
		enableGroup(gr);
		return gr;
	}

	public ItineraryGroup addOrEnableGroup(@NonNull GPXFile file) {
		updateGpxShowAsMarkers(new File(file.path));
		ItineraryGroup gr = getMarkersGroup(file);
		if (gr == null) {
			gr = createGPXMarkerGroup(new File(file.path));
			addGroupInternally(gr);
		}
		enableGroup(gr);
		return gr;
	}

	public ItineraryGroup addOrEnableGroup(@NonNull FavoriteGroup group) {
		ItineraryGroup gr = getMarkersGroup(group);
		if (gr == null) {
			gr = createFavMarkerGroup(group);
			addGroupInternally(gr);
		}
		enableGroup(gr);
		return gr;
	}

	private ItineraryGroup createGPXMarkerGroup(File fl) {
		return new ItineraryGroup(getMarkerGroupId(fl),
				Algorithms.getFileNameWithoutExtension(fl.getName()),
				ItineraryGroup.GPX_TYPE);
	}

	private ItineraryGroup createFavMarkerGroup(FavoriteGroup favGroup) {
		return new ItineraryGroup(favGroup.getName(), favGroup.getName(), ItineraryGroup.FAVORITES_TYPE);
	}

	private String getMarkerGroupId(File gpx) {
		return gpx.getAbsolutePath();
	}

	private String getMarkerGroupId(FavoriteGroup group) {
		return group.getName();
	}

	public void removeMarkerFromGroup(MapMarker marker) {
		if (marker != null) {
			ItineraryGroup itineraryGroup = getMapMarkerGroupById(marker.groupKey, marker.getType());
			if (itineraryGroup != null) {
				itineraryGroup.getMarkers().remove(marker);
				markersHelper.updateGroup(itineraryGroup);
			}
		}
	}

	public void sortGroups() {
		if (itineraryGroups.size() > 0) {
			Collections.sort(itineraryGroups, new Comparator<ItineraryGroup>() {
				@Override
				public int compare(ItineraryGroup group1, ItineraryGroup group2) {
					long t1 = group1.getCreationDate();
					long t2 = group2.getCreationDate();
					return (t1 > t2) ? -1 : ((t1 == t2) ? 0 : 1);
				}
			});
		}
	}

	@Nullable
	public ItineraryGroup getMapMarkerGroupById(String id, int type) {
		for (ItineraryGroup group : itineraryGroups) {
			if ((id == null && group.getId() == null)
					|| (group.getId() != null && group.getId().equals(id))) {
				if (type == ItineraryGroup.ANY_TYPE || type == group.getType()) {
					return group;
				}
			}
		}
		return null;
	}

	@NonNull
	public List<ItineraryGroup> getGroupsForDisplayedGpx() {
		List<ItineraryGroup> res = new ArrayList<>();
		List<SelectedGpxFile> selectedGpxFiles = app.getSelectedGpxHelper().getSelectedGPXFiles();
		for (SelectedGpxFile selected : selectedGpxFiles) {
			ItineraryGroup search = getMarkersGroup(selected.getGpxFile());
			if (search == null && selected.getGpxFile() != null && selected.getGpxFile().path != null) {
				ItineraryGroup group = createGPXMarkerGroup(new File(selected.getGpxFile().path));
				group.setDisabled(true);
				markersHelper.createHeadersInGroup(group);
				res.add(group);
			}
		}
		return res;
	}

	@NonNull
	public List<ItineraryGroup> getGroupsForSavedArticlesTravelBook() {
		List<ItineraryGroup> res = new ArrayList<>();
		TravelHelper travelHelper = app.getTravelHelper();
		if (travelHelper.isAnyTravelBookPresent()) {
			List<TravelArticle> savedArticles = travelHelper.getBookmarksHelper().getSavedArticles();
			for (TravelArticle art : savedArticles) {
				String gpxName = travelHelper.getGPXName(art);
				File path = app.getAppPath(IndexConstants.GPX_TRAVEL_DIR + gpxName);
				ItineraryGroup search = getMapMarkerGroupById(getMarkerGroupId(path), ItineraryGroup.GPX_TYPE);
				if (search == null) {
					ItineraryGroup group = createGPXMarkerGroup(path);
					group.setDisabled(true);
					markersHelper.createHeadersInGroup(group);
					res.add(group);
				}
			}
		}
		return res;
	}

	private class SyncGroupTask extends AsyncTask<Void, Void, Void> {

		private ItineraryGroup group;

		SyncGroupTask(ItineraryGroup group) {
			this.group = group;
		}

		@Override
		protected void onPreExecute() {
			if (!syncListeners.isEmpty()) {
				app.runInUIThread(new Runnable() {
					@Override
					public void run() {
						for (OnGroupSyncedListener listener : syncListeners) {
							listener.onSyncStarted();
						}
					}
				});
			}
		}

		@Override
		protected Void doInBackground(Void... voids) {
			runGroupSynchronization();
			return null;
		}

		// TODO extract method from Asynctask to Helper directly
		private void runGroupSynchronization() {
			List<MapMarker> groupMarkers = new ArrayList<>(group.getMarkers());
			if (group.getType() == ItineraryGroup.FAVORITES_TYPE) {
				FavoriteGroup favGroup = app.getFavorites().getGroup(group.getName());
				if (favGroup == null) {
					return;
				}
				group.setVisible(favGroup.isVisible());
				if (!group.isVisible() || group.isDisabled()) {
					markersHelper.removeGroupActiveMarkers(group, true);
					return;
				}
				List<FavouritePoint> points = new ArrayList<>(favGroup.getPoints());
				for (FavouritePoint fp : points) {
					markersHelper.addNewMarkerIfNeeded(group, groupMarkers, new LatLon(fp.getLatitude(), fp.getLongitude()), fp.getName(), fp, null);
				}
			} else if (group.getType() == ItineraryGroup.GPX_TYPE) {
				GpxSelectionHelper gpxHelper = app.getSelectedGpxHelper();
				File file = new File(group.getId());
				if (!file.exists()) {
					return;
				}

				String gpxPath = group.getId();
				SelectedGpxFile selectedGpxFile = gpxHelper.getSelectedFileByPath(gpxPath);
				GPXFile gpx = selectedGpxFile == null ? null : selectedGpxFile.getGpxFile();
				group.setVisible(gpx != null || group.isVisibleUntilRestart());
				if (gpx == null || group.isDisabled()) {
					markersHelper.removeGroupActiveMarkers(group, true);
					return;
				}

				boolean addAll = group.getWptCategories() == null || group.getWptCategories().isEmpty();
				List<WptPt> gpxPoints = new ArrayList<>(gpx.getPoints());
				for (WptPt pt : gpxPoints) {
					if (addAll || group.getWptCategories().contains(pt.category)
							|| (pt.category == null && group.getWptCategories().contains(""))) {
						markersHelper.addNewMarkerIfNeeded(group, groupMarkers, new LatLon(pt.lat, pt.lon), pt.name, null, pt);
					}
				}
			}
			markersHelper.removeOldMarkersIfPresent(groupMarkers);
		}

		@Override
		protected void onPostExecute(Void aVoid) {
			if (!syncListeners.isEmpty()) {
				app.runInUIThread(new Runnable() {
					@Override
					public void run() {
						for (OnGroupSyncedListener listener : syncListeners) {
							listener.onSyncDone();
						}
					}
				});
			}
		}
	}
}