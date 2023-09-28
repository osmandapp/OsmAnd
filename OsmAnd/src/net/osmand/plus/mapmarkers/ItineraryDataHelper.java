package net.osmand.plus.mapmarkers;

import static net.osmand.gpx.GPXUtilities.readText;
import static net.osmand.gpx.GPXUtilities.writeNotNullText;
import static net.osmand.util.MapUtils.createShortLinkString;

import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import net.osmand.gpx.GPXUtilities;
import net.osmand.gpx.GPXUtilities.GPXExtensionsReader;
import net.osmand.gpx.GPXUtilities.GPXExtensionsWriter;
import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXUtilities.WptPt;
import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.Version;
import net.osmand.plus.utils.FileUtils;
import net.osmand.util.Algorithms;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

public class ItineraryDataHelper {

	private static final Log log = PlatformUtil.getLog(ItineraryDataHelper.class);

	public static final String VISITED_DATE = "visited_date";
	public static final String CREATION_DATE = "creation_date";
	public static final String FILE_TO_SAVE = "itinerary.gpx";

	private static final String CATEGORIES_SPLIT = ",";
	private static final String ITINERARY_ID = "itinerary_id";
	private static final String ITINERARY_GROUP = "itinerary_group";
	private static final String GPX_KEY = "gpx";
	private static final String FAVOURITES_KEY = "favourites_group";

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
		GPXFile gpxFile = loadGPXFile(file, groupInfos);
		if (gpxFile.error != null) {
			return false;
		}
		collectMarkersGroups(gpxFile, groups, groupInfos, sortedMarkers);
		return true;
	}

	public void collectMarkersGroups(GPXFile gpxFile, Map<String, MapMarkersGroup> groups,
	                                 List<ItineraryGroupInfo> groupInfos, Map<String, MapMarker> sortedMarkers) {
		for (ItineraryGroupInfo groupInfo : groupInfos) {
			MapMarkersGroup group = ItineraryGroupInfo.createGroup(groupInfo);
			groups.put(groupInfo.alias, group);
		}
		for (WptPt point : gpxFile.getPoints()) {
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
			log.error(e.getMessage(), e);
		}
	}

	public Exception saveFile(@NonNull File file, @NonNull List<MapMarkersGroup> groups, @Nullable List<MapMarker> sortedMarkers) {
		long lastModifiedTime = getLastModifiedTime();
		GPXFile gpxFile = generateGpx(groups, sortedMarkers);
		Exception exception = GPXUtilities.writeGpxFile(file, gpxFile);
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

	private void assignExtensionWriter(GPXFile gpxFile, Collection<ItineraryGroupInfo> groups) {
		if (gpxFile.getExtensionsWriter() == null) {
			gpxFile.setExtensionsWriter(new GPXExtensionsWriter() {
				@Override
				public void writeExtensions(XmlSerializer serializer) {
					for (ItineraryGroupInfo group : groups) {
						try {
							serializer.startTag(null, "osmand:" + ITINERARY_GROUP);

							writeNotNullText(serializer, "osmand:name", group.name);
							writeNotNullText(serializer, "osmand:type", group.type);
							writeNotNullText(serializer, "osmand:path", group.path);
							writeNotNullText(serializer, "osmand:alias", group.alias);
							writeNotNullText(serializer, "osmand:categories", group.categories);

							serializer.endTag(null, "osmand:" + ITINERARY_GROUP);
						} catch (IOException e) {
							log.error(e);
						}
					}
				}
			});
		}
	}

	private GPXFile loadGPXFile(File file, List<ItineraryGroupInfo> groupInfos) {
		return GPXUtilities.loadGPXFile(file, getGPXExtensionsReader(groupInfos), false);
	}

	public GPXExtensionsReader getGPXExtensionsReader(List<ItineraryGroupInfo> groupInfos) {
		return new GPXExtensionsReader() {
			@Override
			public boolean readExtensions(GPXFile res, XmlPullParser parser) throws IOException, XmlPullParserException {
				if (ITINERARY_GROUP.equalsIgnoreCase(parser.getName())) {
					ItineraryGroupInfo groupInfo = new ItineraryGroupInfo();

					int tok;
					while ((tok = parser.next()) != XmlPullParser.END_DOCUMENT) {
						if (tok == XmlPullParser.START_TAG) {
							String tagName = parser.getName().toLowerCase();
							if ("name".equals(tagName)) {
								groupInfo.name = readText(parser, tagName);
							} else if ("type".equals(tagName)) {
								groupInfo.type = readText(parser, tagName);
							} else if ("path".equals(tagName)) {
								groupInfo.path = readText(parser, tagName);
							} else if ("alias".equals(tagName)) {
								groupInfo.alias = readText(parser, tagName);
							} else if ("categories".equals(tagName)) {
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
		};
	}

	public String saveMarkersToFile(String fileName) {
		GPXFile gpxFile = generateGpx();
		String dirName = IndexConstants.GPX_INDEX_DIR + IndexConstants.MAP_MARKERS_INDEX_DIR;
		File dir = app.getAppPath(dirName);
		if (!dir.exists()) {
			dir.mkdirs();
		}
		String uniqueFileName = FileUtils.createUniqueFileName(app, fileName, dirName, IndexConstants.GPX_FILE_EXT);
		File fout = new File(dir, uniqueFileName + IndexConstants.GPX_FILE_EXT);
		GPXUtilities.writeGpxFile(fout, gpxFile);

		return fout.getAbsolutePath();
	}

	public GPXFile generateGpx() {
		return generateGpx(mapMarkersHelper.getMapMarkers(), false);
	}

	public GPXFile generateGpx(List<MapMarker> markers, boolean completeBackup) {
		GPXFile gpxFile = new GPXFile(Version.getFullVersion(app));
		for (MapMarker marker : markers) {
			WptPt wpt = toWpt(marker);
			wpt.setColor(ContextCompat.getColor(app, MapMarker.getColorId(marker.colorIndex)));
			if (completeBackup) {
				if (marker.creationDate != 0) {
					wpt.getExtensionsToWrite().put(CREATION_DATE, GPXUtilities.formatTime(marker.creationDate));
				}
				if (marker.visitedDate != 0) {
					wpt.getExtensionsToWrite().put(VISITED_DATE, GPXUtilities.formatTime(marker.visitedDate));
				}
			}
			gpxFile.addPoint(wpt);
		}
		return gpxFile;
	}

	public GPXFile generateGpx(@NonNull List<MapMarkersGroup> mapMarkersGroups, @Nullable List<MapMarker> sortedMarkers) {
		GPXFile gpxFile = new GPXFile(Version.getFullVersion(app));
		Map<String, ItineraryGroupInfo> groups = new HashMap<>();

		List<MapMarker> markers = new ArrayList<>();
		for (MapMarkersGroup group : mapMarkersGroups) {
			markers.addAll(group.getMarkers());
			groups.put(group.getId(), ItineraryGroupInfo.createGroupInfo(app, group));
		}
		addMarkersToGpx(gpxFile, groups, sortedMarkers != null ? sortedMarkers : markers);
		assignExtensionWriter(gpxFile, groups.values());
		return gpxFile;
	}

	private void addMarkersToGpx(@NonNull GPXFile gpxFile, @NonNull Map<String, ItineraryGroupInfo> groups, @NonNull List<MapMarker> markers) {
		for (MapMarker marker : markers) {
			WptPt wptPt = toWpt(marker);
			gpxFile.addPoint(wptPt);

			ItineraryGroupInfo groupInfo = groups.get(marker.groupKey);
			if (groupInfo != null) {
				Map<String, String> extensions = wptPt.getExtensionsToWrite();
				if (Algorithms.stringsEqual(groupInfo.type, ItineraryType.MARKERS.getTypeName())) {
					extensions.put(ITINERARY_ID, groupInfo.alias + ":" + marker.id);
				} else {
					String itineraryId = marker.getName(app) + createShortLinkString(wptPt.lat, wptPt.lon, 15);
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
	public List<MapMarker> readMarkersFromGpx(@NonNull GPXFile gpxFile, boolean history) {
		List<MapMarker> mapMarkers = new ArrayList<>();
		for (WptPt point : gpxFile.getPoints()) {
			MapMarker marker = fromWpt(app, point, null);
			marker.history = history;
			mapMarkers.add(marker);
		}
		return mapMarkers;
	}

	public static MapMarker fromFavourite(@NonNull OsmandApplication app, @NonNull FavouritePoint point, @Nullable MapMarkersGroup group) {
		int colorIndex = MapMarker.getColorIndex(app, point.getColor());
		LatLon latLon = new LatLon(point.getLatitude(), point.getLongitude());
		PointDescription name = new PointDescription(PointDescription.POINT_TYPE_MAP_MARKER, point.getName());
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
		int colorIndex = MapMarker.getColorIndex(app, point.getColor());
		PointDescription name = new PointDescription(PointDescription.POINT_TYPE_MAP_MARKER, point.name);
		MapMarker marker = new MapMarker(new LatLon(point.lat, point.lon), name, colorIndex);

		marker.id = getMarkerId(app, marker, group);
		marker.wptPt = point;
		marker.creationDate = GPXUtilities.parseTime(point.getExtensionsToRead().get(CREATION_DATE));
		marker.visitedDate = GPXUtilities.parseTime(point.getExtensionsToRead().get(VISITED_DATE));
		marker.history = marker.visitedDate != 0;

		if (group != null) {
			marker.groupKey = group.getId();
			marker.groupName = group.getName();
		}

		return marker;
	}

	public static WptPt toWpt(@NonNull MapMarker marker) {
		WptPt wpt = new WptPt();
		wpt.lat = marker.getLatitude();
		wpt.lon = marker.getLongitude();
		wpt.name = marker.getOnlyName();
		wpt.time = marker.creationDate;
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
