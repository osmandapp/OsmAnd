package net.osmand.plus.mapmarkers;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import net.osmand.FileUtils;
import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXExtensionsReader;
import net.osmand.GPXUtilities.GPXExtensionsWriter;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.Version;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;

import static net.osmand.GPXUtilities.readText;
import static net.osmand.GPXUtilities.writeNotNullText;
import static net.osmand.plus.FavouritesDbHelper.backup;
import static net.osmand.util.MapUtils.createShortLinkString;

public class ItineraryDataHelper {

	private static final Log log = PlatformUtil.getLog(ItineraryDataHelper.class);

	private static final String VISITED_DATE = "visited_date";
	private static final String CREATION_DATE = "creation_date";

	private static final String CATEGORIES_SPLIT = ",";
	private static final String FILE_TO_SAVE = "itinerary.gpx";
	private static final String FILE_TO_BACKUP = "itinerary_bak.gpx";
	private static final String ITINERARY_ID = "itinerary_id";
	private static final String ITINERARY_GROUP = "itinerary_group";
	private static final String GPX_KEY = "gpx";
	private static final String FAVOURITES_KEY = "favourites_group";

	private static final SimpleDateFormat GPX_TIME_FORMAT = new SimpleDateFormat(GPXUtilities.GPX_TIME_FORMAT, Locale.US);

	static {
		GPX_TIME_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	private final OsmandApplication app;
	private final MapMarkersHelper mapMarkersHelper;

	public ItineraryDataHelper(OsmandApplication app, MapMarkersHelper mapMarkersHelper) {
		this.app = app;
		this.mapMarkersHelper = mapMarkersHelper;
	}

	public long getLastModifiedTime() {
		File internalFile = getInternalFile();
		File externalFile = getExternalFile();
		if (externalFile.exists()) {
			return externalFile.lastModified();
		} else if (internalFile.exists()) {
			return internalFile.lastModified();
		}
		return 0;
	}

	public void setLastModifiedTime(long lastModifiedTime) {
		File internalFile = getInternalFile();
		File externalFile = getExternalFile();
		if (externalFile.exists()) {
			externalFile.setLastModified(lastModifiedTime);
		} else if (internalFile.exists()) {
			internalFile.setLastModified(lastModifiedTime);
		}
	}

	private File getInternalFile() {
		return app.getFileStreamPath(FILE_TO_BACKUP);
	}

	public File getExternalFile() {
		return new File(app.getAppPath(null), FILE_TO_SAVE);
	}

	public File getBackupFile() {
		return FavouritesDbHelper.getBackupFile(app, "itinerary_bak_");
	}

	public List<MapMarkersGroup> loadGroups() {
		Map<String, MapMarkersGroup> groups = new LinkedHashMap<>();
		Map<String, MapMarkersGroup> extGroups = new LinkedHashMap<>();

		File internalFile = getInternalFile();
		if (!internalFile.exists()) {
			groups.put(ItineraryType.MARKERS.getTypeName(), new MapMarkersGroup());
		}
		loadGPXFile(internalFile, groups);
		loadGPXFile(getExternalFile(), extGroups);

		boolean changed = merge(extGroups, groups);
		List<MapMarkersGroup> markersGroups = new ArrayList<>(groups.values());
		if (changed || !getExternalFile().exists()) {
			saveGroups(markersGroups);
		}
		return markersGroups;
	}

	private boolean loadGPXFile(File file, Map<String, MapMarkersGroup> groups) {
		if (!file.exists()) {
			return false;
		}
		List<ItineraryGroupInfo> groupInfos = new ArrayList<>();
		GPXFile gpxFile = loadGPXFile(file, groupInfos);
		if (gpxFile.error != null) {
			return false;
		}
		collectMarkersGroups(gpxFile, groups, groupInfos);
		return true;
	}

	public void collectMarkersGroups(GPXFile gpxFile, Map<String, MapMarkersGroup> groups, List<ItineraryGroupInfo> groupInfos) {
		for (ItineraryGroupInfo groupInfo : groupInfos) {
			MapMarkersGroup group = ItineraryGroupInfo.createGroup(groupInfo);
			groups.put(groupInfo.alias, group);
		}
		for (WptPt point : gpxFile.getPoints()) {
			String itineraryId = point.getExtensionsToRead().get(ITINERARY_ID);
			if (!Algorithms.isEmpty(itineraryId)) {
				for (Entry<String, MapMarkersGroup> entry : groups.entrySet()) {
					String alias = entry.getKey() + ":";
					if (itineraryId.startsWith(alias)) {
						MapMarkersGroup group = entry.getValue();
						MapMarker marker = fromWpt(point, app, false);
						marker.groupKey = group.getId();
						marker.groupName = group.getName();

						if (group.getType() == ItineraryType.MARKERS) {
							marker.id = itineraryId.substring(alias.length());
						} else {
							marker.id = group.getId() + itineraryId.substring(alias.length());
						}

						entry.getValue().getMarkers().add(marker);
						break;
					}
				}
			}
		}
	}

	private boolean merge(Map<String, MapMarkersGroup> source, Map<String, MapMarkersGroup> destination) {
		boolean changed = false;
		for (Map.Entry<String, MapMarkersGroup> entry : source.entrySet()) {
			String ks = entry.getKey();
			if (!destination.containsKey(ks)) {
				changed = true;
				destination.put(ks, entry.getValue());
			}
		}
		return changed;
	}

	public void saveGroups(List<MapMarkersGroup> groups) {
		try {
			saveFile(getInternalFile(), groups);
			saveFile(getExternalFile(), groups);
			backup(getBackupFile(), getExternalFile());
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	public Exception saveFile(File file, List<MapMarkersGroup> groups) {
		GPXFile gpxFile = generateGpx(groups);
		return GPXUtilities.writeGpxFile(file, gpxFile);
	}

	private void assignRouteExtensionWriter(GPXFile gpxFile, final List<ItineraryGroupInfo> groups) {
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
		return GPXUtilities.loadGPXFile(file, getGPXExtensionsReader(groupInfos));
	}

	public GPXExtensionsReader getGPXExtensionsReader(final List<ItineraryGroupInfo> groupInfos) {
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
					wpt.getExtensionsToWrite().put(CREATION_DATE, GPX_TIME_FORMAT.format(new Date(marker.creationDate)));
				}
				if (marker.visitedDate != 0) {
					wpt.getExtensionsToWrite().put(VISITED_DATE, GPX_TIME_FORMAT.format(new Date(marker.visitedDate)));
				}
			}
			gpxFile.addPoint(wpt);
		}
		return gpxFile;
	}

	public GPXFile generateGpx(List<MapMarkersGroup> mapMarkersGroups) {
		GPXFile gpxFile = new GPXFile(Version.getFullVersion(app));
		List<ItineraryGroupInfo> groups = new ArrayList<>();
		for (MapMarkersGroup group : mapMarkersGroups) {
			ItineraryGroupInfo groupInfo = ItineraryGroupInfo.createGroupInfo(app, group);

			for (MapMarker marker : group.getMarkers()) {
				WptPt wptPt = toWpt(marker);

				Map<String, String> extensions = wptPt.getExtensionsToWrite();

				if (group.getType() == ItineraryType.MARKERS) {
					extensions.put(ITINERARY_ID, groupInfo.alias + ":" + marker.id);
				} else {
					String itineraryId = marker.getName(app) + createShortLinkString(wptPt.lat, wptPt.lon, 15);
					extensions.put(ITINERARY_ID, groupInfo.alias + ":" + itineraryId);
				}
				if (group.getType() == ItineraryType.TRACK) {
					extensions.put(GPX_KEY, groupInfo.path);
				} else if (group.getType() == ItineraryType.FAVOURITES && !Algorithms.isEmpty(groupInfo.name)) {
					extensions.put(FAVOURITES_KEY, groupInfo.name);
				}
				gpxFile.addPoint(wptPt);
			}
			groups.add(groupInfo);
		}
		assignRouteExtensionWriter(gpxFile, groups);
		return gpxFile;
	}

	public List<MapMarker> readMarkersFromGpx(GPXFile gpxFile, boolean history) {
		List<MapMarker> mapMarkers = new ArrayList<>();
		for (WptPt point : gpxFile.getPoints()) {
			MapMarker marker = fromWpt(point, app, history);
			mapMarkers.add(marker);
		}
		return mapMarkers;
	}

	public static MapMarker fromWpt(@NonNull WptPt point, @NonNull Context ctx, boolean history) {
		LatLon latLon = new LatLon(point.lat, point.lon);
		int colorIndex = MapMarker.getColorIndex(ctx, point.getColor());
		PointDescription name = new PointDescription(PointDescription.POINT_TYPE_LOCATION, point.name);

		MapMarker marker = new MapMarker(latLon, name, colorIndex, false, 0);

		String visitedDateStr = point.getExtensionsToRead().get(VISITED_DATE);
		String creationDateStr = point.getExtensionsToRead().get(CREATION_DATE);
		marker.visitedDate = parseTime(visitedDateStr);
		marker.creationDate = parseTime(creationDateStr);
		marker.history = history;
		marker.nextKey = history ? MapMarkersDbHelper.HISTORY_NEXT_VALUE : MapMarkersDbHelper.TAIL_NEXT_VALUE;

		return marker;
	}

	public static WptPt toWpt(@NonNull MapMarker marker) {
		WptPt wpt = new WptPt();
		wpt.lat = marker.getLatitude();
		wpt.lon = marker.getLongitude();
		wpt.name = marker.getOnlyName();
		return wpt;
	}

	private static long parseTime(String text) {
		long time = 0;
		if (text != null) {
			try {
				time = GPX_TIME_FORMAT.parse(text).getTime();
			} catch (ParseException e) {
				log.error(e);
			}
		}
		return time;
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
				groupInfo.categories = Algorithms.encodeStringSet(wptCategories, CATEGORIES_SPLIT);
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
