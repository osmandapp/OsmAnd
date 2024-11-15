package net.osmand.plus.mapmarkers;

import static net.osmand.data.PointDescription.POINT_TYPE_MAP_MARKER;
import static net.osmand.util.MapUtils.createShortLinkString;

import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.plus.shared.SharedUtil;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.Version;
import net.osmand.plus.utils.FileUtils;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.GpxUtilities;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.util.Algorithms;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.logging.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

public class ItineraryDataHelper {

	static final Log LOG = PlatformUtil.getLog(ItineraryDataHelper.class);

	public static final String VISITED_DATE = "visited_date";
	public static final String CREATION_DATE = "creation_date";
	public static final String FILE_TO_SAVE = "itinerary.gpx";

	static final String CATEGORIES_SPLIT = ",";
	static final String ITINERARY_ID = "itinerary_id";
	static final String ITINERARY_GROUP = "itinerary_group";
	static final String GPX_KEY = "gpx";
	static final String FAVOURITES_KEY = "favourites_group";

	private final OsmandApplication app;
	private final MapMarkersHelper mapMarkersHelper;

	public ItineraryDataHelper(OsmandApplication app, MapMarkersHelper mapMarkersHelper) {
		this.app = app;
		this.mapMarkersHelper = mapMarkersHelper;
	}

	public long getLastModifiedTime() {
		File externalFile = getExternalFile();
		if (externalFile.exists()) {
			return externalFile.lastModified();
		}
		return 0;
	}

	public void setLastModifiedTime(long lastModifiedTime) {
		File externalFile = getExternalFile();
		if (externalFile.exists()) {
			externalFile.setLastModified(lastModifiedTime);
		}
	}

	public File getExternalFile() {
		return new File(app.getAppPath(null), FILE_TO_SAVE);
	}

	public Pair<Map<String, MapMarkersGroup>, Map<String, MapMarker>> loadGroupsAndOrder() {
		Map<String, MapMarkersGroup> groups = new LinkedHashMap<>();
		Map<String, MapMarker> sortedMarkers = new LinkedHashMap<>();

		File externalFile = getExternalFile();
		if (!externalFile.exists()) {
			groups.put(ItineraryType.MARKERS.getTypeName(), new MapMarkersGroup());
		}
		loadGPXFile(externalFile, groups, sortedMarkers);

		List<MapMarker> mapMarkers = new ArrayList<>(sortedMarkers.values());
		List<MapMarkersGroup> markersGroups = new ArrayList<>(groups.values());
		if (!getExternalFile().exists()) {
			saveGroups(markersGroups, mapMarkers);
		}
		return new Pair<>(groups, sortedMarkers);
	}

	private boolean loadGPXFile(File file, Map<String, MapMarkersGroup> groups, Map<String, MapMarker> sortedMarkers) {
		if (!file.exists()) {
			return false;
		}
		List<ItineraryGroupInfo> groupInfos = new ArrayList<>();
		GpxFile gpxFile = loadGPXFile(file, groupInfos);
		if (gpxFile.getError() != null) {
			return false;
		}
		collectMarkersGroups(gpxFile, groups, groupInfos, sortedMarkers);
		return true;
	}

	public void collectMarkersGroups(GpxFile gpxFile, Map<String, MapMarkersGroup> groups,
	                                 List<ItineraryGroupInfo> groupInfos, Map<String, MapMarker> sortedMarkers) {
		for (ItineraryGroupInfo groupInfo : groupInfos) {
			MapMarkersGroup group = ItineraryGroupInfo.createGroup(groupInfo);
			groups.put(groupInfo.alias, group);
		}
		for (WptPt point : gpxFile.getPointsList()) {
			String itineraryId = point.getExtensionsToRead().get(ITINERARY_ID);
			Entry<String, MapMarkersGroup> entry = getMapMarkersGroupForItineraryId(groups, itineraryId);
			if (entry != null) {
				MapMarkersGroup group = entry.getValue();
				MapMarker marker = fromWpt(app, point, group);
				marker.groupKey = group.getId();
				marker.groupName = group.getName();

				String alias = entry.getKey() + ":";
				if (group.getType() == ItineraryType.MARKERS) {
					marker.id = itineraryId.substring(alias.length());
				} else {
					marker.id = group.getId() + itineraryId.substring(alias.length());
				}
				group.getMarkers().add(marker);
				sortedMarkers.put(marker.id, marker);
			}
		}
	}

	private Entry<String, MapMarkersGroup> getMapMarkersGroupForItineraryId(Map<String, MapMarkersGroup> groups, String itineraryId) {
		if (!Algorithms.isEmpty(itineraryId)) {
			for (Entry<String, MapMarkersGroup> entry : groups.entrySet()) {
				String alias = entry.getKey() + ":";
				if (itineraryId.startsWith(alias)) {
					return entry;
				}
			}
		}
		return null;
	}

	public void saveGroups(@NonNull List<MapMarkersGroup> groups, @Nullable List<MapMarker> sortedMarkers) {
		try {
			saveFile(getExternalFile(), groups, sortedMarkers);
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
	}

	public Exception saveFile(@NonNull File file, @NonNull List<MapMarkersGroup> groups, @Nullable List<MapMarker> sortedMarkers) {
		long lastModifiedTime = getLastModifiedTime();
		GpxFile gpxFile = generateGpx(groups, sortedMarkers);
		Exception exception = SharedUtil.writeGpxFile(file, gpxFile);
		if (exception == null) {
			FileInputStream is = null;
			try {
				is = new FileInputStream(file);
				String md5 = new String(Hex.encodeHex(DigestUtils.md5(is)));
				String lastMd5 = app.getSettings().ITINERARY_LAST_CALCULATED_MD5.get();
				if (!md5.equals(lastMd5)) {
					app.getSettings().ITINERARY_LAST_CALCULATED_MD5.set(md5);
				} else {
					setLastModifiedTime(lastModifiedTime);
				}
			} catch (IOException e) {
				app.getSettings().ITINERARY_LAST_CALCULATED_MD5.set("");
			} finally {
				Algorithms.closeStream(is);
			}
		}
		return exception;
	}

	private GpxFile loadGPXFile(File file, List<ItineraryGroupInfo> groupInfos) {
		return SharedUtil.loadGpxFile(file, ItineraryDataHelperKt.getGpxExtensionsReader(groupInfos), false);
	}

	public String saveMarkersToFile(String fileName) {
		GpxFile gpxFile = generateGpx();
		String dirName = IndexConstants.GPX_INDEX_DIR + IndexConstants.MAP_MARKERS_INDEX_DIR;
		File dir = app.getAppPath(dirName);
		if (!dir.exists()) {
			dir.mkdirs();
		}
		String uniqueFileName = FileUtils.createUniqueFileName(app, fileName, dirName, IndexConstants.GPX_FILE_EXT);
		File fout = new File(dir, uniqueFileName + IndexConstants.GPX_FILE_EXT);
		SharedUtil.writeGpxFile(fout, gpxFile);

		return fout.getAbsolutePath();
	}

	public GpxFile generateGpx() {
		return generateGpx(mapMarkersHelper.getMapMarkers(), false);
	}

	public GpxFile generateGpx(List<MapMarker> markers, boolean completeBackup) {
		GpxFile gpxFile = new GpxFile(Version.getFullVersion(app));
		for (MapMarker marker : markers) {
			WptPt wpt = toWpt(marker);
			wpt.setColor(ContextCompat.getColor(app, MapMarker.getColorId(marker.colorIndex)));
			if (completeBackup) {
				if (marker.creationDate != 0) {
					wpt.getExtensionsToWrite().put(CREATION_DATE, GpxUtilities.INSTANCE.formatTime(marker.creationDate));
				}
				if (marker.visitedDate != 0) {
					wpt.getExtensionsToWrite().put(VISITED_DATE, GpxUtilities.INSTANCE.formatTime(marker.visitedDate));
				}
			}
			gpxFile.addPoint(wpt);
		}
		return gpxFile;
	}

	public GpxFile generateGpx(@NonNull List<MapMarkersGroup> mapMarkersGroups, @Nullable List<MapMarker> sortedMarkers) {
		GpxFile gpxFile = new GpxFile(Version.getFullVersion(app));
		Map<String, ItineraryGroupInfo> groups = new HashMap<>();

		List<MapMarker> markers = new ArrayList<>();
		for (MapMarkersGroup group : mapMarkersGroups) {
			markers.addAll(group.getMarkers());
			groups.put(group.getId(), ItineraryGroupInfo.createGroupInfo(app, group));
		}
		addMarkersToGpx(gpxFile, groups, sortedMarkers != null ? sortedMarkers : markers);
		ItineraryDataHelperKt.assignExtensionWriter(gpxFile, groups.values());
		return gpxFile;
	}

	private void addMarkersToGpx(@NonNull GpxFile gpxFile, @NonNull Map<String, ItineraryGroupInfo> groups, @NonNull List<MapMarker> markers) {
		for (MapMarker marker : markers) {
			WptPt wptPt = toWpt(marker);
			gpxFile.addPoint(wptPt);

			ItineraryGroupInfo groupInfo = groups.get(marker.groupKey);
			if (groupInfo != null) {
				Map<String, String> extensions = wptPt.getExtensionsToWrite();
				if (Algorithms.stringsEqual(groupInfo.type, ItineraryType.MARKERS.getTypeName())) {
					extensions.put(ITINERARY_ID, groupInfo.alias + ":" + marker.id);
				} else {
					String itineraryId = marker.getName(app) + createShortLinkString(wptPt.getLat(), wptPt.getLon(), 15);
					extensions.put(ITINERARY_ID, groupInfo.alias + ":" + itineraryId);
				}
				if (Algorithms.stringsEqual(groupInfo.type, ItineraryType.TRACK.getTypeName())) {
					extensions.put(GPX_KEY, groupInfo.path);
				} else if (Algorithms.stringsEqual(groupInfo.type, ItineraryType.FAVOURITES.getTypeName())
						&& !Algorithms.isEmpty(groupInfo.name)) {
					extensions.put(FAVOURITES_KEY, groupInfo.name);
				}
			}
		}
	}

	@NonNull
	public List<MapMarker> readMarkersFromGpx(@NonNull GpxFile gpxFile, boolean history) {
		List<MapMarker> mapMarkers = new ArrayList<>();
		for (WptPt point : gpxFile.getPointsList()) {
			MapMarker marker = fromWpt(app, point, null);
			marker.history = history;
			mapMarkers.add(marker);
		}
		return mapMarkers;
	}

	public static MapMarker fromFavourite(@NonNull OsmandApplication app, @NonNull FavouritePoint point, @Nullable MapMarkersGroup group) {
		int colorIndex = MapMarker.getColorIndex(app, point.getColor());
		LatLon latLon = new LatLon(point.getLatitude(), point.getLongitude());
		PointDescription name = new PointDescription(POINT_TYPE_MAP_MARKER, point.getName());
		MapMarker marker = new MapMarker(latLon, name, colorIndex);

		marker.id = getMarkerId(app, marker, group);
		marker.favouritePoint = point;
		marker.creationDate = point.getTimestamp();
		marker.visitedDate = point.getVisitedDate();
		marker.history = marker.visitedDate != 0;

		if (group != null) {
			marker.groupKey = group.getId();
			marker.groupName = group.getName();
		}

		return marker;
	}

	public static MapMarker fromWpt(@NonNull OsmandApplication app, @NonNull WptPt point, @Nullable MapMarkersGroup group) {
		Map<String, String> extensions = point.getExtensionsToRead();
		String creationDate = extensions.get(CREATION_DATE);
		String visitedDate = extensions.get(VISITED_DATE);

		int colorIndex = MapMarker.getColorIndex(app, point.getColor());
		PointDescription name = new PointDescription(POINT_TYPE_MAP_MARKER, point.getName());
		MapMarker marker = new MapMarker(new LatLon(point.getLat(), point.getLon()), name, colorIndex);

		marker.id = getMarkerId(app, marker, group);
		marker.wptPt = point;
		if (!Algorithms.isEmpty(creationDate)) {
			marker.creationDate = GpxUtilities.INSTANCE.parseTime(creationDate);
		}
		if (!Algorithms.isEmpty(visitedDate)) {
			marker.visitedDate = GpxUtilities.INSTANCE.parseTime(visitedDate);
		}
		marker.history = marker.visitedDate != 0;

		if (group != null) {
			marker.groupKey = group.getId();
			marker.groupName = group.getName();
		}

		return marker;
	}

	public static WptPt toWpt(@NonNull MapMarker marker) {
		WptPt wpt = new WptPt();
		wpt.setLat(marker.getLatitude());
		wpt.setLon(marker.getLongitude());
		wpt.setName(marker.getOnlyName());
		wpt.setTime(marker.creationDate);
		return wpt;
	}

	public static String getMarkerId(@NonNull OsmandApplication app, @NonNull MapMarker marker, @Nullable MapMarkersGroup group) {
		if (group == null) {
			return UUID.randomUUID().toString();
		} else {
			String shortLink = createShortLinkString(marker.point.getLatitude(), marker.point.getLongitude(), 15);
			return group.getId() + marker.getName(app) + shortLink;
		}
	}

	public static class ItineraryGroupInfo {

		public String name;
		public String type;
		public String path;
		public String alias;
		public String categories;

		public static ItineraryGroupInfo createGroupInfo(OsmandApplication app, MapMarkersGroup group) {
			ItineraryGroupInfo groupInfo = new ItineraryGroupInfo();
			groupInfo.name = group.getName();
			groupInfo.type = group.getType().getTypeName();

			Set<String> wptCategories = group.getWptCategories();
			if (!Algorithms.isEmpty(wptCategories)) {
				groupInfo.categories = Algorithms.encodeCollection(wptCategories, CATEGORIES_SPLIT);
			}
			if (group.getType() == ItineraryType.TRACK) {
				String path = group.getId();
				String gpxDir = app.getAppPath(IndexConstants.GPX_INDEX_DIR).getAbsolutePath();
				int index = path.indexOf(gpxDir);
				if (index != -1) {
					path = path.substring(gpxDir.length() + 1);
				}
				groupInfo.path = path;
				groupInfo.alias = groupInfo.type + ":" + path.replace(IndexConstants.GPX_FILE_EXT, "");
			} else {
				groupInfo.alias = groupInfo.type + (group.getId() == null ? "" : ":" + group.getId().replace(":", "_"));
			}
			return groupInfo;
		}

		public static MapMarkersGroup createGroup(ItineraryGroupInfo groupInfo) {
			ItineraryType type = ItineraryType.findTypeForName(groupInfo.type);

			if (type == ItineraryType.FAVOURITES && groupInfo.name == null) {
				groupInfo.name = "";
			}

			String groupId;
			if (type == ItineraryType.TRACK) {
				groupId = groupInfo.path;
			} else {
				groupId = groupInfo.name;
			}

			MapMarkersGroup group = new MapMarkersGroup(groupId, groupInfo.name, type);
			group.setWptCategories(Algorithms.decodeStringSet(groupInfo.categories, CATEGORIES_SPLIT));
			return group;
		}
	}
}
