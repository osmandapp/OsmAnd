package net.osmand.plus.mapmarkers;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import net.osmand.FileUtils;
import net.osmand.GPXUtilities;
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
import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

import static net.osmand.GPXUtilities.writeNotNullText;
import static net.osmand.plus.FavouritesDbHelper.backup;

public class ItinerarySaveHelper {

	private static final Log log = PlatformUtil.getLog(ItinerarySaveHelper.class);

	private static final String VISITED_DATE = "visited_date";
	private static final String CREATION_DATE = "creation_date";

	private static final String CATEGORIES_SPLIT = ",";
	private static final String FILE_TO_SAVE = "itinerary.gpx";
	private static final String FILE_TO_BACKUP = "itinerary_bak.gpx";
	private static final String ITINERARY_ID = "itinerary_id";
	private static final String ITINERARY_GROUP = "itinerary_group";
	private static final String GPX_ORIGIN = "gpx_origin";
	private static final String FAVOURITES_ORIGIN = "favourites_origin";

	private static final SimpleDateFormat GPX_TIME_FORMAT = new SimpleDateFormat(GPXUtilities.GPX_TIME_FORMAT, Locale.US);

	static {
		GPX_TIME_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	private OsmandApplication app;
	private ItineraryHelper itineraryHelper;

	public ItinerarySaveHelper(OsmandApplication app, ItineraryHelper itineraryHelper) {
		this.app = app;
		this.itineraryHelper = itineraryHelper;
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

	public void saveGroups() {
		try {
			saveFile(getInternalFile());
			saveFile(getExternalFile());
			backup(getBackupFile(), getExternalFile());
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	public Exception saveFile(File file) {
		List<ItineraryGroup> groups = itineraryHelper.getItineraryGroups();
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
		return generateGpx(itineraryHelper.getMapMarkers(), false);
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

	public GPXFile generateGpx(List<ItineraryGroup> itineraryGroups) {
		GPXFile gpxFile = new GPXFile(Version.getFullVersion(app));
		List<ItineraryGroupInfo> groups = new ArrayList<>();
		for (ItineraryGroup group : itineraryGroups) {
			ItineraryGroupInfo groupInfo = ItineraryGroupInfo.createGroupInfo(app, group);

			for (MapMarker marker : group.getMarkers()) {
				WptPt wptPt = toWpt(marker);

				String markerId = marker.id;
				String name = marker.getName(app);
				int index = markerId.indexOf(name);
				if (index != -1) {
					markerId = markerId.substring(index + name.length());
				}
				wptPt.getExtensionsToWrite().put(ITINERARY_ID, groupInfo.type + ":" + markerId);

				if (group.getType() == ItineraryType.TRACK) {
					wptPt.getExtensionsToWrite().put(GPX_ORIGIN, groupInfo.path);
				} else {
					wptPt.getExtensionsToWrite().put(FAVOURITES_ORIGIN, groupInfo.name);
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

		public static ItineraryGroupInfo createGroupInfo(OsmandApplication app, ItineraryGroup group) {
			ItineraryGroupInfo groupInfo = new ItineraryGroupInfo();
			groupInfo.type = group.getType().getTypeName();
			groupInfo.name = !Algorithms.isEmpty(group.getName()) ? group.getName() : null;

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
				groupInfo.alias = groupInfo.type + ":" + path;
			} else {
				groupInfo.alias = groupInfo.type + (Algorithms.isEmpty(groupInfo.name) ? "" : ":" + groupInfo.name);
			}
			return groupInfo;
		}
	}
}
