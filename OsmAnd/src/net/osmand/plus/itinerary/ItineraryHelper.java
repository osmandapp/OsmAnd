package net.osmand.plus.itinerary;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXExtensionsReader;
import net.osmand.GPXUtilities.GPXExtensionsWriter;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.FavouritesDbHelper.FavoriteGroup;
import net.osmand.plus.GPXDatabase.GpxDataItem;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.itinerary.ItineraryGroup.ItineraryGroupInfo;
import net.osmand.plus.mapmarkers.CategoriesSubHeader;
import net.osmand.plus.mapmarkers.GroupHeader;
import net.osmand.plus.mapmarkers.MapMarker;
import net.osmand.plus.mapmarkers.MapMarkersDbHelper;
import net.osmand.plus.mapmarkers.MapMarkersGroup;
import net.osmand.plus.mapmarkers.MapMarkersHelper;
import net.osmand.plus.mapmarkers.MapMarkersHelper.OnGroupSyncedListener;
import net.osmand.plus.mapmarkers.ShowHideHistoryButton;
import net.osmand.plus.wikivoyage.data.TravelArticle;
import net.osmand.plus.wikivoyage.data.TravelHelper;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static net.osmand.GPXUtilities.readText;
import static net.osmand.GPXUtilities.writeNotNullText;
import static net.osmand.plus.FavouritesDbHelper.backup;
import static net.osmand.plus.mapmarkers.MapMarkersHelper.BY_DATE_ADDED_DESC;
import static net.osmand.util.Algorithms.getFileNameWithoutExtension;

public class ItineraryHelper {

	private static final Log log = PlatformUtil.getLog(ItineraryHelper.class);

	protected static final String CATEGORIES_SPLIT = ",";
	private static final String FILE_TO_SAVE = "itinerary.gpx";
	private static final String FILE_TO_BACKUP = "itinerary_bak.gpx";
	private static final String ITINERARY_ID = "itinerary_id";
	private static final String ITINERARY_GROUP = "itinerary_group";

	private OsmandApplication app;

	private MapMarkersHelper markersHelper;
	private MapMarkersDbHelper markersDbHelper;

	private ExecutorService executorService = Executors.newSingleThreadExecutor();

	private List<ItineraryGroup> itineraryGroups = new ArrayList<>();
	private List<MapMarkersGroup> mapMarkersGroups = new ArrayList<>();
	private Set<OnGroupSyncedListener> syncListeners = new HashSet<>();

	public ItineraryHelper(@NonNull OsmandApplication app) {
		this.app = app;
		markersHelper = app.getMapMarkersHelper();
		markersDbHelper = app.getMapMarkersDbHelper();
		loadMarkersGroups();
	}

	public List<ItineraryGroup> getItineraryGroups() {
		return itineraryGroups;
	}

	public List<MapMarkersGroup> getMapMarkersGroups() {
		return mapMarkersGroups;
	}

	public void syncAllGroups() {
		itineraryGroups.clear();
	}

	private boolean merge(Map<Object, ItineraryGroup> source, Map<Object, ItineraryGroup> destination) {
		boolean changed = false;
		for (Map.Entry<Object, ItineraryGroup> entry : source.entrySet()) {
			Object ks = entry.getKey();
			if (!destination.containsKey(ks)) {
				changed = true;
				destination.put(ks, entry.getValue());
			}
		}
		return changed;
	}

	private boolean loadGPXFile(File file, Map<Object, ItineraryGroup> groups) {
		if (!file.exists()) {
			return false;
		}
		List<ItineraryGroupInfo> groupInfos = new ArrayList<>();
		GPXFile gpxFile = loadGPXFile(file, groupInfos);
		if (gpxFile.error != null) {
			return false;
		}
		for (ItineraryGroupInfo groupInfo : groupInfos) {
			ItineraryGroup group = ItineraryGroup.createGroup(groupInfo);
			Object key = group.name != null ? group.name : group.type;
			groups.put(key, group);
		}
		return true;
	}

	private GPXFile loadGPXFile(File file, final List<ItineraryGroupInfo> groupInfos) {
		return GPXUtilities.loadGPXFile(file, new GPXExtensionsReader() {
			@Override
			public boolean readExtensions(GPXFile res, XmlPullParser parser) throws IOException, XmlPullParserException {
				if (ITINERARY_GROUP.equalsIgnoreCase(parser.getName())) {
					ItineraryGroupInfo groupInfo = new ItineraryGroupInfo();

					int tok;
					while ((tok = parser.next()) != XmlPullParser.END_DOCUMENT) {
						if (tok == XmlPullParser.START_TAG) {
							String tagName = parser.getName().toLowerCase();
							if ("name".equalsIgnoreCase(tagName)) {
								groupInfo.name = readText(parser, tagName);
							} else if ("type".equalsIgnoreCase(tagName)) {
								groupInfo.type = readText(parser, tagName);
							} else if ("path".equalsIgnoreCase(tagName)) {
								groupInfo.path = readText(parser, tagName);
							} else if ("alias".equalsIgnoreCase(tagName)) {
								groupInfo.alias = readText(parser, tagName);
							} else if ("categories".equalsIgnoreCase(tagName)) {
								groupInfo.categories = readText(parser, tagName);
							}
						} else if (tok == XmlPullParser.END_TAG) {
							if (ITINERARY_GROUP.equalsIgnoreCase(parser.getName())) {
								groupInfos.add(groupInfo);
								return true;
							}
						}
					}
				}
				return false;
			}
		});
	}

	public void saveGroupsIntoFile() {
		try {
			saveFile(getInternalFile());
			saveFile(getExternalFile());
			backup(getBackupFile(), getExternalFile());
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	public Exception saveFile(File file) {
		GPXFile gpxFile = asGpxFile();
		return GPXUtilities.writeGpxFile(file, gpxFile);
	}

	public GPXFile asGpxFile() {
		GPXFile gpxFile = new GPXFile(Version.getFullVersion(app));
		List<ItineraryGroupInfo> groups = new ArrayList<>();
		for (ItineraryGroup group : itineraryGroups) {
			ItineraryGroupInfo groupInfo = group.convertToGroupInfo();

			for (ItineraryItem item : group.itineraryItems) {
				WptPt wptPt = new WptPt();
				wptPt.lat = item.latLon.getLatitude();
				wptPt.lon = item.latLon.getLongitude();
				wptPt.getExtensionsToWrite().put(ITINERARY_ID, groupInfo.alias + ":" + item.id);
				gpxFile.addPoint(wptPt);
			}
			groups.add(groupInfo);
		}
		assignRouteExtensionWriter(gpxFile, groups);
		return gpxFile;
	}

	private void assignRouteExtensionWriter(GPXFile gpxFile, final List<ItineraryGroupInfo> groups) {
		if (gpxFile.getExtensionsWriter() == null) {
			gpxFile.setExtensionsWriter(new GPXExtensionsWriter() {
				@Override
				public void writeExtensions(XmlSerializer serializer) {
					for (ItineraryGroupInfo group : groups) {
						try {
							serializer.startTag(null, "osmand:itinerary_group");

							writeNotNullText(serializer, "osmand:name", group.name);
							writeNotNullText(serializer, "osmand:type", group.type);
							writeNotNullText(serializer, "osmand:path", group.path);
							writeNotNullText(serializer, "osmand:alias", group.alias);
							writeNotNullText(serializer, "osmand:categories", group.categories);

							serializer.endTag(null, "osmand:itinerary_group");
						} catch (IOException e) {
							log.error(e);
						}
					}
				}
			});
		}
	}

	public void syncMarkersGroups() {
		for (MapMarkersGroup group : mapMarkersGroups) {
			if (group.getId() != null && group.getName() != null) {
				runGroupSynchronization(group);
			}
		}
	}

	private List<ItineraryGroup> convertToItineraryGroups() {
		List<ItineraryGroup> groups = new ArrayList<>();
		for (MapMarkersGroup markersGroup : mapMarkersGroups) {
			groups.add(ItineraryGroup.createGroup(markersGroup));
		}
		return groups;
	}

	public void syncItineraryGroups() {
		syncItineraryGroups(convertToItineraryGroups());
	}

	public void syncItineraryGroups(List<ItineraryGroup> itineraryGroups) {
		for (ItineraryGroup group : itineraryGroups) {
			if (group.type == ItineraryType.FAVOURITES) {
				syncFavoriteGroup(group);
			} else if (group.type == ItineraryType.TRACK) {
				syncTrackGroup(group);
			} else if (group.type == ItineraryType.MARKERS) {
				syncMarkersGroup(group);
			}
		}
	}

	private void syncFavoriteGroup(ItineraryGroup group) {
		FavoriteGroup favoriteGroup = app.getFavorites().getGroup(group.id);
		if (favoriteGroup != null && favoriteGroup.isVisible()) {
			for (FavouritePoint point : favoriteGroup.getPoints()) {
				if (point.getPassedTimestamp() == 0) {
					ItineraryItem item = new ItineraryItem(group, point, ItineraryType.FAVOURITES);
					group.itineraryItems.add(item);
				}
			}
			itineraryGroups.add(group);
		}
	}

	private void syncTrackGroup(ItineraryGroup group) {
		SelectedGpxFile selectedGpxFile = app.getSelectedGpxHelper().getSelectedFileByPath(group.id);
		if (selectedGpxFile != null) {
			for (WptPt wptPt : selectedGpxFile.getGpxFile().getPoints()) {
				if (shouldAddWpt(wptPt, group.wptCategories)) {
					ItineraryItem item = new ItineraryItem(group, wptPt, ItineraryType.TRACK);
					group.itineraryItems.add(item);
				}
			}
			itineraryGroups.add(group);
		}
	}

	private void syncMarkersGroup(ItineraryGroup group) {
		ItineraryGroup pointsGroup = new ItineraryGroup(group);
		ItineraryGroup markersGroup = new ItineraryGroup(group);

		pointsGroup.type = ItineraryType.POINTS;
		markersGroup.type = ItineraryType.MARKERS;

		List<MapMarker> allMarkers = new ArrayList<>(markersHelper.getMapMarkers());
		for (MapMarker marker : allMarkers) {
			PointDescription description = marker.getOriginalPointDescription();
			if (description.isFavorite()) {
				FavouritePoint point = app.getFavorites().getVisibleFavByLatLon(marker.point);
				if (point != null && point.getPassedTimestamp() == 0) {
					ItineraryItem item = new ItineraryItem(pointsGroup, point, ItineraryType.FAVOURITES);
					pointsGroup.itineraryItems.add(item);
				}
			} else if (description.isWpt()) {
				WptPt wptPt = app.getSelectedGpxHelper().getVisibleWayPointByLatLon(marker.point);
				if (wptPt != null && shouldAddWpt(wptPt, null)) {
					ItineraryItem item = new ItineraryItem(pointsGroup, wptPt, ItineraryType.TRACK);
					pointsGroup.itineraryItems.add(item);
				}
			} else if (!marker.history) {
				ItineraryItem item = new ItineraryItem(markersGroup, marker, ItineraryType.MARKERS);
				markersGroup.itineraryItems.add(item);
			}
		}
		itineraryGroups.add(pointsGroup);
		itineraryGroups.add(markersGroup);
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

	public void updateGroupWptCategories(@NonNull MapMarkersGroup group, Set<String> wptCategories) {
		String id = group.getId();
		if (id != null) {
			group.setWptCategories(wptCategories);
			if (wptCategories != null) {
				markersDbHelper.updateGroupCategories(id, group.getWptCategoriesString());
			}
		}
	}

	public void enableGroup(@NonNull MapMarkersGroup gr) {
		// check if group doesn't exist internally
		if (!mapMarkersGroups.contains(gr)) {
			addGroupInternally(gr);
		}
		if (gr.isDisabled()) {
			updateGroupDisabled(gr, false);
		}
		runSynchronizationAsync(gr);
	}

	public void updateGroups() {
		for (MapMarkersGroup group : mapMarkersGroups) {
			updateGroup(group);
		}
	}

	public void updateGroupDisabled(@NonNull MapMarkersGroup group, boolean disabled) {
		String id = group.getId();
		if (id != null) {
			markersDbHelper.updateGroupDisabled(id, disabled);
			group.setDisabled(disabled);
		}
	}

	public List<MapMarker> getMapMarkersFromDefaultGroups(boolean history) {
		List<MapMarker> mapMarkers = new ArrayList<>();
		for (MapMarkersGroup group : mapMarkersGroups) {
			if (group.getType() == ItineraryType.MARKERS) {
				for (MapMarker marker : group.getMarkers()) {
					if (history && marker.history || !history && !marker.history) {
						mapMarkers.add(marker);
					}
				}
			}
		}
		return mapMarkers;
	}

	private void loadMarkersGroups() {
		Map<String, MapMarkersGroup> groupsMap = markersDbHelper.getAllGroupsMap();
		List<MapMarker> allMarkers = new ArrayList<>(markersHelper.getMapMarkers());
		allMarkers.addAll(markersHelper.getMapMarkersHistory());

		Iterator<Entry<String, MapMarkersGroup>> iterator = groupsMap.entrySet().iterator();
		while (iterator.hasNext()) {
			MapMarkersGroup group = iterator.next().getValue();
			if (group.getType() == ItineraryType.TRACK && !new File(group.getId()).exists()) {
				markersDbHelper.removeMarkersGroup(group.getId());
				iterator.remove();
			}
		}

		MapMarkersGroup noGroup = null;
		for (MapMarker marker : allMarkers) {
			MapMarkersGroup group = groupsMap.get(marker.groupKey);
			if (group == null) {
				if (noGroup == null) {
					noGroup = new MapMarkersGroup();
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

		mapMarkersGroups = new ArrayList<>(groupsMap.values());
		if (noGroup != null) {
			markersHelper.sortMarkers(noGroup.getMarkers(), false, BY_DATE_ADDED_DESC);
			addToGroupsList(noGroup);
		}

		sortGroups();

		for (MapMarkersGroup group : mapMarkersGroups) {
			updateGroup(group);
		}
	}

	public void addGroupInternally(MapMarkersGroup group) {
		markersDbHelper.addGroup(group);
		markersHelper.addHistoryMarkersToGroup(group);
		addToGroupsList(group);
	}

	public void updateGpxShowAsMarkers(File file) {
		GpxDataItem dataItem = app.getGpxDbHelper().getItem(file);
		if (dataItem != null) {
			app.getGpxDbHelper().updateShowAsMarkers(dataItem, true);
			dataItem.setShowAsMarkers(true);
		}
	}

	public void addToGroupsList(MapMarkersGroup group) {
		List<MapMarkersGroup> copyList = new ArrayList<>(mapMarkersGroups);
		copyList.add(group);
		mapMarkersGroups = copyList;
	}

	public void removeFromGroupsList(MapMarkersGroup group) {
		List<MapMarkersGroup> copyList = new ArrayList<>(mapMarkersGroups);
		copyList.remove(group);
		mapMarkersGroups = copyList;
	}

	public void addSyncListener(OnGroupSyncedListener listener) {
		syncListeners.add(listener);
	}

	public void removeSyncListener(OnGroupSyncedListener listener) {
		syncListeners.remove(listener);
	}

	public void runSynchronizationAsync(final @NonNull MapMarkersGroup group) {
		app.runInUIThread(new Runnable() {
			@Override
			public void run() {
				new SyncGroupTask(group).executeOnExecutor(executorService);
			}
		});
	}

	public void runSynchronizationAsync(@NonNull FavoriteGroup favoriteGroup) {
		MapMarkersGroup group = getMarkersGroup(favoriteGroup);
		if (group != null) {
			runSynchronizationAsync(group);
		}
	}

	public void runSynchronizationAsync(@NonNull GPXFile gpxFile) {
		MapMarkersGroup group = getMarkersGroup(gpxFile);
		if (group != null) {
			runSynchronizationAsync(group);
		}
	}

	public void addMarkerToGroup(MapMarker marker) {
		MapMarkersGroup mapMarkersGroup = app.getItineraryHelper().getMapMarkerGroupById(marker.groupKey, marker.getType());
		if (mapMarkersGroup != null) {
			mapMarkersGroup.getMarkers().add(marker);
			updateGroup(mapMarkersGroup);
			if (mapMarkersGroup.getName() == null) {
				markersHelper.sortMarkers(mapMarkersGroup.getMarkers(), false, BY_DATE_ADDED_DESC);
			}
		} else {
			mapMarkersGroup = new MapMarkersGroup(marker.groupKey, marker.groupName, ItineraryType.MARKERS);
			mapMarkersGroup.setCreationDate(Long.MAX_VALUE);
			mapMarkersGroup.getMarkers().add(marker);
			addToGroupsList(mapMarkersGroup);
			sortGroups();
			updateGroup(mapMarkersGroup);
		}
	}

	public void updateGroup(MapMarkersGroup mapMarkersGroup) {
		if (mapMarkersGroup.getId() == null || mapMarkersGroup.getName() == null) {
			return;
		}
		createHeadersInGroup(mapMarkersGroup);
		int historyMarkersCount = mapMarkersGroup.getHistoryMarkers().size();
		ShowHideHistoryButton showHideHistoryButton = mapMarkersGroup.getShowHideHistoryButton();
		if (showHideHistoryButton != null) {
			if (historyMarkersCount == 0) {
				mapMarkersGroup.setShowHideHistoryButton(null);
			}
		} else if (historyMarkersCount > 0) {
			showHideHistoryButton = new ShowHideHistoryButton();
			showHideHistoryButton.showHistory = false;
			mapMarkersGroup.setShowHideHistoryButton(showHideHistoryButton);
		}
	}

	public void createHeadersInGroup(@NonNull MapMarkersGroup group) {
		ItineraryType type = group.getType();
		int headerIconId = 0;
		int subHeaderIconId = 0;
		if (type != ItineraryType.MARKERS) {
			headerIconId = type == ItineraryType.FAVOURITES
					? R.drawable.ic_action_favorite : R.drawable.ic_action_polygom_dark;
			subHeaderIconId = R.drawable.ic_action_filter;
		}
		GroupHeader header = new GroupHeader(headerIconId, group);
		CategoriesSubHeader categoriesSubHeader = new CategoriesSubHeader(subHeaderIconId, group);

		group.setHeader(header);
		group.setCategoriesSubHeader(categoriesSubHeader);
	}

	public MapMarkersGroup getMarkersGroup(GPXFile gpx) {
		if (gpx == null || gpx.path == null) {
			return null;
		}
		return getMapMarkerGroupById(getMarkerGroupId(new File(gpx.path)), ItineraryType.TRACK);
	}

	public MapMarkersGroup getMarkersGroup(FavoriteGroup favGroup) {
		return getMapMarkerGroupById(getMarkerGroupId(favGroup), ItineraryType.FAVOURITES);
	}

	public MapMarkersGroup addOrEnableGpxGroup(@NonNull File file) {
		updateGpxShowAsMarkers(file);
		MapMarkersGroup gr = getMapMarkerGroupById(getMarkerGroupId(file), ItineraryType.TRACK);
		if (gr == null) {
			gr = createGPXMarkerGroup(file);
			addGroupInternally(gr);
		}
		enableGroup(gr);
		return gr;
	}

	public MapMarkersGroup addOrEnableGroup(@NonNull GPXFile file) {
		updateGpxShowAsMarkers(new File(file.path));
		MapMarkersGroup gr = getMarkersGroup(file);
		if (gr == null) {
			gr = createGPXMarkerGroup(new File(file.path));
			addGroupInternally(gr);
		}
		enableGroup(gr);
		return gr;
	}

	public MapMarkersGroup addOrEnableGroup(@NonNull FavoriteGroup group) {
		MapMarkersGroup gr = getMarkersGroup(group);
		if (gr == null) {
			gr = createFavMarkerGroup(group);
			addGroupInternally(gr);
		}
		enableGroup(gr);
		return gr;
	}

	private MapMarkersGroup createGPXMarkerGroup(File fl) {
		return new MapMarkersGroup(getMarkerGroupId(fl), getFileNameWithoutExtension(fl), ItineraryType.TRACK);
	}

	private MapMarkersGroup createFavMarkerGroup(FavoriteGroup favGroup) {
		return new MapMarkersGroup(favGroup.getName(), favGroup.getName(), ItineraryType.FAVOURITES);
	}

	private String getMarkerGroupId(File gpx) {
		return gpx.getAbsolutePath();
	}

	private String getMarkerGroupId(FavoriteGroup group) {
		return group.getName();
	}

	public void removeMarkerFromGroup(MapMarker marker) {
		if (marker != null) {
			MapMarkersGroup mapMarkersGroup = getMapMarkerGroupById(marker.groupKey, marker.getType());
			if (mapMarkersGroup != null) {
				mapMarkersGroup.getMarkers().remove(marker);
				updateGroup(mapMarkersGroup);
			}
		}
	}

	public void sortGroups() {
		if (mapMarkersGroups.size() > 0) {
			Collections.sort(mapMarkersGroups, new Comparator<MapMarkersGroup>() {
				@Override
				public int compare(MapMarkersGroup group1, MapMarkersGroup group2) {
					long t1 = group1.getCreationDate();
					long t2 = group2.getCreationDate();
					return (t1 > t2) ? -1 : ((t1 == t2) ? 0 : 1);
				}
			});
		}
	}

	@Nullable
	public ItineraryItem getItineraryItem(@NonNull Object object) {
		for (ItineraryGroup group : itineraryGroups) {
			for (ItineraryItem item : group.itineraryItems) {
				if (Algorithms.objectEquals(item.object, object)) {
					return item;
				}
			}
		}
		return null;
	}

	@Nullable
	public ItineraryGroup getItineraryGroupById(String id, ItineraryType type) {
		for (ItineraryGroup group : itineraryGroups) {
			if ((id == null && group.id == null)
					|| (group.id != null && group.id.equals(id))) {
				if (type == ItineraryType.POINTS || type == group.type) {
					return group;
				}
			}
		}
		return null;
	}

	@Nullable
	public MapMarkersGroup getMapMarkerGroupById(String id, ItineraryType type) {
		for (MapMarkersGroup group : mapMarkersGroups) {
			if ((id == null && group.getId() == null)
					|| (group.getId() != null && group.getId().equals(id))) {
				if (type == ItineraryType.MARKERS || type == group.getType()) {
					return group;
				}
			}
		}
		return null;
	}

	@NonNull
	public List<MapMarkersGroup> getGroupsForDisplayedGpx() {
		List<MapMarkersGroup> res = new ArrayList<>();
		List<SelectedGpxFile> selectedGpxFiles = app.getSelectedGpxHelper().getSelectedGPXFiles();
		for (SelectedGpxFile selected : selectedGpxFiles) {
			MapMarkersGroup search = getMarkersGroup(selected.getGpxFile());
			if (search == null && selected.getGpxFile() != null && selected.getGpxFile().path != null) {
				MapMarkersGroup group = createGPXMarkerGroup(new File(selected.getGpxFile().path));
				group.setDisabled(true);
				createHeadersInGroup(group);
				res.add(group);
			}
		}
		return res;
	}

	@NonNull
	public List<MapMarkersGroup> getGroupsForSavedArticlesTravelBook() {
		List<MapMarkersGroup> res = new ArrayList<>();
		TravelHelper travelHelper = app.getTravelHelper();
		if (travelHelper.isAnyTravelBookPresent()) {
			List<TravelArticle> savedArticles = travelHelper.getBookmarksHelper().getSavedArticles();
			for (TravelArticle art : savedArticles) {
				String gpxName = travelHelper.getGPXName(art);
				File path = app.getAppPath(IndexConstants.GPX_TRAVEL_DIR + gpxName);
				MapMarkersGroup search = getMapMarkerGroupById(getMarkerGroupId(path), ItineraryType.TRACK);
				if (search == null) {
					MapMarkersGroup group = createGPXMarkerGroup(path);
					group.setDisabled(true);
					createHeadersInGroup(group);
					res.add(group);
				}
			}
		}
		return res;
	}

	public void removeMarkersGroup(MapMarkersGroup group) {
		if (group != null) {
			markersDbHelper.removeMarkersGroup(group.getId());
			markersHelper.removeGroupActiveMarkers(group, false);
			removeFromGroupsList(group);
		}
	}

	private void runGroupSynchronization(MapMarkersGroup group) {
		List<MapMarker> groupMarkers = new ArrayList<>(group.getMarkers());
		if (group.getType() == ItineraryType.FAVOURITES) {
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
		} else if (group.getType() == ItineraryType.TRACK) {
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

			List<WptPt> gpxPoints = new ArrayList<>(gpx.getPoints());
			for (WptPt pt : gpxPoints) {
				if (shouldAddWpt(pt, group.getWptCategories())) {
					markersHelper.addNewMarkerIfNeeded(group, groupMarkers, new LatLon(pt.lat, pt.lon), pt.name, null, pt);
				}
			}
		}
		markersHelper.removeOldMarkersIfPresent(groupMarkers);
	}

	private boolean shouldAddWpt(WptPt wptPt, Set<String> wptCategories) {
		boolean addAll = wptCategories == null || wptCategories.isEmpty();
		return addAll || wptCategories.contains(wptPt.category)
				|| wptPt.category == null && wptCategories.contains("");
	}

	private class SyncGroupTask extends AsyncTask<Void, Void, Void> {

		private MapMarkersGroup group;

		SyncGroupTask(MapMarkersGroup group) {
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
			runGroupSynchronization(group);
			return null;
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