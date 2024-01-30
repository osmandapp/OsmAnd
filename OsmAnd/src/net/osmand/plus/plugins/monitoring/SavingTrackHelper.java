package net.osmand.plus.plugins.monitoring;

import static net.osmand.gpx.GpxParameter.COLOR;
import static net.osmand.gpx.GpxParameter.COLORING_TYPE;
import static net.osmand.gpx.GpxParameter.SHOW_ARROWS;
import static net.osmand.gpx.GpxParameter.SHOW_START_FINISH;
import static net.osmand.gpx.GpxParameter.WIDTH;
import static net.osmand.plus.importfiles.tasks.SaveGpxAsyncTask.GPX_FILE_DATE_FORMAT;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.format.DateFormat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.IndexConstants;
import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.gpx.GPXUtilities;
import net.osmand.gpx.GPXUtilities.Track;
import net.osmand.gpx.GPXUtilities.TrkSegment;
import net.osmand.gpx.GPXUtilities.WptPt;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.Version;
import net.osmand.plus.notifications.OsmandNotification.NotificationType;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.development.OsmandDevelopmentPlugin;
import net.osmand.plus.routing.ColoringType;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.simulation.SimulationProvider;
import net.osmand.plus.track.helpers.GpxDataItem;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.utils.AndroidDbUtils;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

public class SavingTrackHelper extends SQLiteOpenHelper {

	private static final Log log = PlatformUtil.getLog(SavingTrackHelper.class);

	private static final int DATABASE_VERSION = 8;
	private static final String DATABASE_NAME = "tracks";

	private static final String TRACK_NAME = "track";
	private static final String TRACK_COL_DATE = "date";
	private static final String TRACK_COL_LAT = "lat";
	private static final String TRACK_COL_LON = "lon";
	private static final String TRACK_COL_ALTITUDE = "altitude";
	private static final String TRACK_COL_SPEED = "speed";
	private static final String TRACK_COL_HDOP = "hdop";
	private static final String TRACK_COL_HEADING = "heading";
	private static final String TRACK_COL_BEARING = "bearing";
	private static final String TRACK_COL_PLUGINS_INFO = "plugins_info";

	private static final String GPXTPX_PREFIX = "gpxtpx:";

	private static final String POINT_NAME = "point";
	private static final String POINT_COL_DATE = "date";
	private static final String POINT_COL_LAT = "lat";
	private static final String POINT_COL_LON = "lon";
	private static final String POINT_COL_NAME = "pname";
	private static final String POINT_COL_CATEGORY = "category";
	private static final String POINT_COL_DESCRIPTION = "description";
	private static final String POINT_COL_COLOR = "color";
	private static final String POINT_COL_ICON = "icon";
	private static final String POINT_COL_BACKGROUND = "background";

	private static final NumberFormat DECIMAL_FORMAT = new DecimalFormat("#.#", new DecimalFormatSymbols(Locale.US));
	private static final long LOCATION_TIME_INTERVAL_MS = 28L * 1000L * 60L * 60L * 24L; // 4 weeks


	private final OsmandApplication app;
	private final OsmandSettings settings;

	private final SelectedGpxFile currentTrack;

	private int currentTrackIndex = 1;
	private LatLon lastPoint;
	private float distance;
	private long duration;
	private int points;
	private int trkPoints;

	private long lastTimeUpdated;
	private long lastTimeFileSaved;

	private ApplicationMode lastRoutingApplicationMode;

	public SavingTrackHelper(@NonNull OsmandApplication app) {
		super(app, DATABASE_NAME, null, DATABASE_VERSION);
		this.app = app;
		this.settings = app.getSettings();
		this.currentTrack = new SelectedGpxFile();
		this.currentTrack.setShowCurrentTrack(true);
		GPXFile gpxFile = new GPXFile(Version.getFullVersion(app));
		gpxFile.showCurrentTrack = true;
		currentTrack.setGpxFile(gpxFile, app);
		prepareCurrentTrackForRecording();
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		createTableForTrack(db);
		createTableForPoints(db);
	}

	private void createTableForTrack(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE " + TRACK_NAME + " (" + TRACK_COL_LAT + " double, " + TRACK_COL_LON + " double, "
				+ TRACK_COL_ALTITUDE + " double, " + TRACK_COL_SPEED + " double, " + TRACK_COL_HDOP + " double, "
				+ TRACK_COL_DATE + " long, " + TRACK_COL_HEADING + " float, " + TRACK_COL_PLUGINS_INFO + " text )");
	}

	private void createTableForPoints(SQLiteDatabase db) {
		try {
			db.execSQL("CREATE TABLE " + POINT_NAME + " (" + POINT_COL_LAT + " double, " + POINT_COL_LON + " double, " //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
					+ POINT_COL_DATE + " long, " + POINT_COL_DESCRIPTION + " text, " + POINT_COL_NAME + " text, "
					+ POINT_COL_CATEGORY + " text, " + POINT_COL_COLOR + " long, " + POINT_COL_ICON + " text, " + POINT_COL_BACKGROUND + " text )"); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (RuntimeException e) {
			// ignore if already exists
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (oldVersion < 2) {
			createTableForPoints(db);
		}
		if (oldVersion < 3) {
			db.execSQL("ALTER TABLE " + TRACK_NAME + " ADD " + TRACK_COL_HDOP + " double");
		}
		if (oldVersion < 4) {
			db.execSQL("ALTER TABLE " + POINT_NAME + " ADD " + POINT_COL_NAME + " text");
			db.execSQL("ALTER TABLE " + POINT_NAME + " ADD " + POINT_COL_CATEGORY + " text");
		}
		if (oldVersion < 5) {
			db.execSQL("ALTER TABLE " + POINT_NAME + " ADD " + POINT_COL_COLOR + " long");
		}
		if (oldVersion < 6) {
			db.execSQL("ALTER TABLE " + TRACK_NAME + " ADD " + TRACK_COL_HEADING + " float");
		}
		if (oldVersion < 7) {
			db.execSQL("ALTER TABLE " + POINT_NAME + " ADD " + POINT_COL_ICON + " text");
			db.execSQL("ALTER TABLE " + POINT_NAME + " ADD " + POINT_COL_BACKGROUND + " text");
		}
		if (oldVersion < 8) {
			db.execSQL("ALTER TABLE " + TRACK_NAME + " ADD " + TRACK_COL_PLUGINS_INFO + " text");
		}
	}

	public long getLastTrackPointTime() {
		long res = 0;
		try {
			SQLiteDatabase db = getWritableDatabase();
			if (db != null) {
				try {
					Cursor query = db.rawQuery("SELECT " + TRACK_COL_DATE + " FROM " + TRACK_NAME + " ORDER BY " + TRACK_COL_DATE + " DESC", null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					if (query.moveToFirst()) {
						res = query.getLong(0);
					}
					query.close();
				} finally {
					db.close();
				}
			}
		} catch (RuntimeException e) {
		}
		return res;
	}

	public synchronized boolean hasDataToSave() {
		try {
			SQLiteDatabase db = getWritableDatabase();
			if (db != null) {
				try {
					Cursor q = db.query(false, TRACK_NAME, new String[0], null, null, null, null, null, null);
					boolean has = q.moveToFirst();
					q.close();
					if (has) {
						return true;
					}
					q = db.query(false, POINT_NAME, new String[] {POINT_COL_LAT, POINT_COL_LON}, null, null, null, null, null, null);
					has = q.moveToFirst();
					while (has) {
						if (q.getDouble(0) != 0 || q.getDouble(1) != 0) {
							break;
						}
						if (!q.moveToNext()) {
							has = false;
							break;
						}
					}
					q.close();
					if (has) {
						return true;
					}
				} finally {
					db.close();
				}
			}
		} catch (RuntimeException e) {
			return false;
		}
		return false;
	}

	/**
	 * @return warnings, gpxFilesByName
	 */
	public synchronized SaveGpxResult saveDataToGpx(@NonNull File dir) {
		List<String> warnings = new ArrayList<>();
		Map<String, GPXFile> gpxFilesByName = new LinkedHashMap<>();
		dir.mkdirs();
		if (dir.getParentFile().canWrite() && dir.exists()) {
			Map<String, GPXFile> data = collectRecordedData();
			for (Map.Entry<String, GPXFile> entry : data.entrySet()) {
				String f = entry.getKey();
				GPXFile gpx = entry.getValue();
				log.debug("Filename: " + f);
				File fout = new File(dir, f + IndexConstants.GPX_FILE_EXT);
				if (!gpx.isEmpty()) {
					WptPt pt = gpx.findPointToShow();
					String fileName = f + "_" + GPX_FILE_DATE_FORMAT.format(new Date(pt.time));
					Integer trackStorageDirectory = app.getSettings().TRACK_STORAGE_DIRECTORY.get();
					if (!OsmandSettings.REC_DIRECTORY.equals(trackStorageDirectory)) {
						SimpleDateFormat dateDirFormat = new SimpleDateFormat("yyyy-MM", Locale.US);
//							if (trackStorageDirectory == OsmandSettings.DAILY_DIRECTORY) {
//								dateDirFormat = new SimpleDateFormat("yyyy-MM-dd");
//							}
						String dateDirName = dateDirFormat.format(new Date(pt.time));
						File dateDir = new File(dir, dateDirName);
						dateDir.mkdirs();
						if (dateDir.exists()) {
							fileName = dateDirName + File.separator + fileName;
						}
					}
					gpxFilesByName.put(fileName, gpx);
					fout = new File(dir, fileName + IndexConstants.GPX_FILE_EXT);
					int ind = 1;
					while (fout.exists()) {
						fout = new File(dir, fileName + "_" + (++ind) + IndexConstants.GPX_FILE_EXT); //$NON-NLS-1$
					}
				}

				Exception warn = GPXUtilities.writeGpxFile(fout, gpx);
				if (warn != null) {
					warnings.add(warn.getMessage());
					return new SaveGpxResult(warnings, new HashMap<>());
				}

				GpxDataItem item = new GpxDataItem(app, fout);
				item.setAnalysis(gpx.getAnalysis(fout.lastModified()));
				app.getGpxDbHelper().add(item);
				lastTimeFileSaved = fout.lastModified();
				saveTrackAppearance(item);
			}
			clearRecordedData(warnings.isEmpty());
		}
		return new SaveGpxResult(warnings, gpxFilesByName);
	}

	private void saveTrackAppearance(@NonNull GpxDataItem item) {
		ColoringType coloringType = settings.CURRENT_TRACK_COLORING_TYPE.get();
		String routeInfoAttribute = settings.CURRENT_TRACK_ROUTE_INFO_ATTRIBUTE.get();

		item.setParameter(COLOR, settings.CURRENT_TRACK_COLOR.get());
		item.setParameter(WIDTH, settings.CURRENT_TRACK_WIDTH.get());
		item.setParameter(SHOW_ARROWS, settings.CURRENT_TRACK_SHOW_ARROWS.get());
		item.setParameter(SHOW_START_FINISH, settings.CURRENT_TRACK_SHOW_START_FINISH.get());
		item.setParameter(COLORING_TYPE, coloringType.getName(routeInfoAttribute));

		app.getGpxDbHelper().updateDataItem(item);
	}

	public void clearRecordedData(boolean isWarningEmpty) {
		long time = System.currentTimeMillis();
		if (isWarningEmpty) {
			SQLiteDatabase db = getWritableDatabase();
			if (db != null) {
				try {
					if (db.isOpen()) {
						db.execSQL("DELETE FROM " + TRACK_NAME + " WHERE " + TRACK_COL_DATE + " <= ?", new Object[] {time});
						db.execSQL("DELETE FROM " + POINT_NAME + " WHERE " + POINT_COL_DATE + " <= ?", new Object[] {time});
					}
				} finally {
					db.close();
				}
			}
		}
		distance = 0;
		points = 0;
		duration = 0;
		trkPoints = 0;
		currentTrackIndex++;
		app.getSelectedGpxHelper().clearPoints(currentTrack.getModifiableGpxFile());
		currentTrack.getModifiableGpxFile().tracks.clear();
		currentTrack.clearSegmentsToDisplay();
		currentTrack.getModifiableGpxFile().modifiedTime = time;
		currentTrack.getModifiableGpxFile().pointsModifiedTime = time;
		prepareCurrentTrackForRecording();
	}

	public Map<String, GPXFile> collectRecordedData() {
		Map<String, GPXFile> data = new LinkedHashMap<String, GPXFile>();
		SQLiteDatabase db = getReadableDatabase();
		if (db != null && db.isOpen()) {
			try {
				collectDBPoints(db, data);
				collectDBTracks(db, data);
			} finally {
				db.close();
			}
		}
		return data;
	}

	private void collectDBPoints(@NonNull SQLiteDatabase db, @NonNull Map<String, GPXFile> dataTracks) {
		Cursor query = db.rawQuery("SELECT " + POINT_COL_LAT + "," + POINT_COL_LON + "," + POINT_COL_DATE + "," //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				+ POINT_COL_DESCRIPTION + "," + POINT_COL_NAME + "," + POINT_COL_CATEGORY + "," + POINT_COL_COLOR + ","
				+ POINT_COL_ICON + "," + POINT_COL_BACKGROUND + " FROM " + POINT_NAME + " ORDER BY " + POINT_COL_DATE + " ASC", null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		if (query.moveToFirst()) {
			do {
				WptPt pt = new WptPt();
				pt.lat = query.getDouble(0);
				pt.lon = query.getDouble(1);
				long time = query.getLong(2);
				pt.time = time;
				pt.desc = query.getString(3);
				pt.name = query.getString(4);
				pt.category = query.getString(5);
				int color = query.getInt(6);
				if (color != 0) {
					pt.setColor(color);
				}
				pt.setIconName(query.getString(7));
				pt.setBackgroundType(query.getString(8));

				// check if name is extension (needed for audio/video plugin & josm integration)
				if (pt.name != null && pt.name.length() > 4 && pt.name.charAt(pt.name.length() - 4) == '.') {
					pt.link = pt.name;
				}

				String date = DateFormat.format("yyyy-MM-dd", time).toString(); //$NON-NLS-1$
				GPXFile gpx;
				if (dataTracks.containsKey(date)) {
					gpx = dataTracks.get(date);
				} else {
					gpx = new GPXFile(Version.getFullVersion(app));
					dataTracks.put(date, gpx);
				}
				app.getSelectedGpxHelper().addPoint(pt, gpx);
			} while (query.moveToNext());
		}
		query.close();
	}

	private void collectDBTracks(@NonNull SQLiteDatabase db, @NonNull Map<String, GPXFile> dataTracks) {
		Cursor query = db.rawQuery("SELECT " + TRACK_COL_LAT + "," + TRACK_COL_LON + "," + TRACK_COL_ALTITUDE + ","
				+ TRACK_COL_SPEED + "," + TRACK_COL_HDOP + "," + TRACK_COL_DATE + "," + TRACK_COL_HEADING + "," + TRACK_COL_PLUGINS_INFO
				+ " FROM " + TRACK_NAME + " ORDER BY " + TRACK_COL_DATE + " ASC", null);
		long previousTime = 0;
		long previousInterval = 0;
		TrkSegment segment = null;
		Track track = null;
		if (query.moveToFirst()) {
			do {
				WptPt pt = new WptPt();
				pt.lat = query.getDouble(0);
				pt.lon = query.getDouble(1);
				pt.ele = query.getDouble(2);
				pt.speed = query.getDouble(3);
				pt.hdop = query.getDouble(4);
				pt.time = query.getLong(5);
				pt.heading = query.isNull(6) ? Float.NaN : query.getFloat(6);

				Map<String, String> extensions = getPluginsExtensions(query.getString(7));
				if (!Algorithms.isEmpty(extensions)) {
					assignExtensionWriter(pt, extensions);
				}

				boolean newInterval = pt.lat == 0 && pt.lon == 0;
				long currentInterval = Math.abs(pt.time - previousTime);
				if (track != null && !newInterval && (!settings.AUTO_SPLIT_RECORDING.get()
						|| currentInterval < 6 * 60 * 1000 || currentInterval < 10 * previousInterval)) {
					// 6 minute - same segment
					segment.points.add(pt);
				} else if (track != null && (settings.AUTO_SPLIT_RECORDING.get()
						&& currentInterval < 2 * 60 * 60 * 1000)) {
					// 2 hour - same track
					segment = new TrkSegment();
					if (!newInterval) {
						segment.points.add(pt);
					}
					track.segments.add(segment);
				} else {
					track = new Track();
					segment = new TrkSegment();
					track.segments.add(segment);
					if (!newInterval) {
						segment.points.add(pt);
					}
					// check if date the same - new track otherwise new file
					String date = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date(pt.time));
					if (dataTracks.containsKey(date)) {
						GPXFile gpx = dataTracks.get(date);
						gpx.tracks.add(track);
					} else {
						GPXFile file = new GPXFile(Version.getFullVersion(app));
						file.tracks.add(track);
						dataTracks.put(date, file);
					}
				}
				previousInterval = currentInterval;
				previousTime = pt.time;
			} while (query.moveToNext());
		}
		query.close();
		dropEmptyTracks(dataTracks);
	}

	private void assignExtensionWriter(@NonNull WptPt wptPt, @NonNull Map<String, String> pluginsExtensions) {
		if (wptPt.getExtensionsWriter() == null) {
			HashMap<String, String> regularExtensions = new HashMap<>();
			HashMap<String, String> gpxtpxExtensions = new HashMap<>();

			for (Entry<String, String> entry : pluginsExtensions.entrySet()) {
				if (entry.getKey().startsWith(GPXTPX_PREFIX)) {
					gpxtpxExtensions.put(entry.getKey(), entry.getValue());
				} else {
					regularExtensions.put(entry.getKey(), entry.getValue());
				}
			}
			wptPt.setExtensionsWriter(createExtensionsWriter(regularExtensions));
			wptPt.setAdditionalExtensionsWriter(createExtensionsWriter(gpxtpxExtensions));
		}
	}

	private GPXUtilities.GPXExtensionsWriter createExtensionsWriter(@NonNull Map<String, String> pluginsExtensions) {
		return serializer -> {
			for (Entry<String, String> entry : pluginsExtensions.entrySet()) {
				try {
					GPXUtilities.writeNotNullText(serializer, entry.getKey(), entry.getValue());
				} catch (IOException e) {
					log.error(e);
				}
			}
		};
	}

	@NonNull
	private Map<String, String> getPluginsExtensions(@Nullable String pluginsInfo) {
		if (!Algorithms.isEmpty(pluginsInfo)) {
			try {
				Map<String, String> extensions = new HashMap<>();
				JSONObject json = new JSONObject(pluginsInfo);
				for (Iterator<String> iterator = json.keys(); iterator.hasNext(); ) {
					String key = iterator.next();
					extensions.put(key, json.optString(key));
				}
				return extensions;
			} catch (JSONException e) {
				log.error(e.getMessage(), e);
			}
		}
		return Collections.emptyMap();
	}

	private void dropEmptyTracks(@NonNull Map<String, GPXFile> dataTracks) {
		List<String> datesToRemove = new ArrayList<>();
		for (Map.Entry<String, GPXFile> entry : dataTracks.entrySet()) {
			GPXFile file = entry.getValue();
			Iterator<Track> it = file.tracks.iterator();
			while (it.hasNext()) {
				Track t = it.next();
				Iterator<TrkSegment> its = t.segments.iterator();
				while (its.hasNext()) {
					if (its.next().points.size() == 0) {
						its.remove();
					}
				}
				if (t.segments.size() == 0) {
					it.remove();
				}
			}
			if (file.isEmpty()) {
				datesToRemove.add(entry.getKey());
			}
		}
		for (String date : datesToRemove) {
			dataTracks.remove(date);
		}
	}

	public void startNewSegment() {
		lastTimeUpdated = 0;
		lastPoint = null;
		executeInsertTrackQuery(0, 0, 0, 0, 0, System.currentTimeMillis(), Float.NaN, null);
		addTrackPoint(null, true, System.currentTimeMillis());
	}

	public void updateLocation(@Nullable Location location, @Nullable Float heading) {
		// use because there is a bug on some devices with location.getTime() see #18642
		long time = System.currentTimeMillis();
		if (location != null) {
			long locationTime = location.getTime();
			if (Math.abs(time - locationTime) < LOCATION_TIME_INTERVAL_MS) {
				time = locationTime;
			}
		}
		if (app.getRoutingHelper().isFollowingMode()) {
			lastRoutingApplicationMode = settings.getApplicationMode();
		} else if (settings.getApplicationMode() == settings.DEFAULT_APPLICATION_MODE.get()) {
			lastRoutingApplicationMode = null;
		}
		boolean record = shouldRecordLocation(location, time);
		if (record) {
			heading = getAdjustedHeading(heading);

			WptPt wptPt = new WptPt(location.getLatitude(), location.getLongitude(), time,
					location.getAltitude(), location.getSpeed(), location.getAccuracy(), heading);

			String pluginsInfo = getPluginsInfo(location);
			Map<String, String> extensions = getPluginsExtensions(pluginsInfo);
			if (!Algorithms.isEmpty(extensions)) {
				assignExtensionWriter(wptPt, extensions);
			}

			insertData(wptPt, pluginsInfo);
			app.getNotificationHelper().refreshNotification(NotificationType.GPX);
		}
	}

	private boolean shouldRecordLocation(@Nullable Location location, long locationTime) {
		boolean record = false;
		if (location != null && SimulationProvider.isNotSimulatedLocation(location)
				&& PluginsHelper.isActive(OsmandMonitoringPlugin.class)) {
			if (isRecordingAutomatically() && locationTime - lastTimeUpdated > settings.SAVE_TRACK_INTERVAL.get()) {
				record = true;
			} else if (settings.SAVE_GLOBAL_TRACK_TO_GPX.get()
					&& locationTime - lastTimeUpdated > settings.SAVE_GLOBAL_TRACK_INTERVAL.get()) {
				record = true;
			}
			float minDistance = settings.SAVE_TRACK_MIN_DISTANCE.get();
			if (minDistance > 0 && lastPoint != null
					&& MapUtils.getDistance(lastPoint, location.getLatitude(), location.getLongitude()) < minDistance) {
				record = false;
			}
			float precision = settings.SAVE_TRACK_PRECISION.get();
			if (precision > 0 && (!location.hasAccuracy() || location.getAccuracy() > precision)) {
				record = false;
			}
			float minSpeed = settings.SAVE_TRACK_MIN_SPEED.get();
			if (minSpeed > 0 && (!location.hasSpeed() || location.getSpeed() < minSpeed)) {
				record = false;
			}
		}
		return record;
	}

	private float getAdjustedHeading(@Nullable Float heading) {
		OsmandDevelopmentPlugin plugin = PluginsHelper.getEnabledPlugin(OsmandDevelopmentPlugin.class);
		boolean writeHeading = plugin != null && plugin.SAVE_HEADING_TO_GPX.get();
		return heading != null && writeHeading ? MapUtils.normalizeDegrees360(heading) : Float.NaN;
	}

	@Nullable
	private String getPluginsInfo(@NonNull net.osmand.Location location) {
		JSONObject json = new JSONObject();
		PluginsHelper.attachAdditionalInfoToRecordedTrack(location, json);

		OsmandDevelopmentPlugin plugin = PluginsHelper.getEnabledPlugin(OsmandDevelopmentPlugin.class);
		boolean writeBearing = plugin != null && plugin.SAVE_BEARING_TO_GPX.get();
		if (writeBearing && location.hasBearing()) {
			try {
				json.put(TRACK_COL_BEARING, DECIMAL_FORMAT.format(location.getBearing()));
			} catch (JSONException e) {
				log.error(e.getMessage(), e);
			}
		}
		return json.length() > 0 ? json.toString() : null;
	}

	private void insertData(@NonNull WptPt wptPt, @Nullable String pluginsInfo) {
		executeInsertTrackQuery(wptPt.lat, wptPt.lon, wptPt.ele, wptPt.speed, wptPt.hdop, wptPt.time, wptPt.heading, pluginsInfo);
		boolean newSegment = false;
		if (lastPoint == null || (wptPt.time - lastTimeUpdated) > 180 * 1000) {
			lastPoint = new LatLon(wptPt.lat, wptPt.lon);
			newSegment = true;
		} else {
			float[] lastInterval = new float[1];
			net.osmand.Location.distanceBetween(wptPt.lat, wptPt.lon, lastPoint.getLatitude(), lastPoint.getLongitude(), lastInterval);
			if (lastTimeUpdated > 0 && wptPt.time > lastTimeUpdated) {
				duration += wptPt.time - lastTimeUpdated;
			}
			distance += lastInterval[0];
			lastPoint = new LatLon(wptPt.lat, wptPt.lon);
		}
		lastTimeUpdated = wptPt.time;
		addTrackPoint(wptPt, newSegment, wptPt.time);
		trkPoints++;
	}

	private void addTrackPoint(WptPt pt, boolean newSegment, long time) {
		Track track = currentTrack.getModifiableGpxFile().tracks.get(0);
		if (newSegment) {
			currentTrack.addEmptySegmentToDisplay();
		}
		boolean segmentAdded = false;
		if (track.segments.size() == 0 || newSegment) {
			track.segments.add(new TrkSegment());
			segmentAdded = true;
		}
		if (pt != null) {
			currentTrack.appendTrackPointToDisplay(pt, app);
			TrkSegment lt = track.segments.get(track.segments.size() - 1);
			lt.points.add(pt);
		}
		if (segmentAdded) {
			currentTrack.processPoints(app);
		}
		currentTrack.getModifiableGpxFile().modifiedTime = time;
	}

	public WptPt insertPointData(double lat, double lon, String description, String name, String category, int color) {
		return insertPointData(lat, lon, description, name, category, color, null, null);
	}

	public WptPt insertPointData(double lat, double lon, String description, String name,
	                             String category, int color, String iconName, String backgroundName) {
		long time = System.currentTimeMillis();
		WptPt pt = new WptPt(lat, lon, time, Double.NaN, 0, Double.NaN);
		pt.name = name;
		pt.category = category;
		pt.desc = description;
		if (color != 0) {
			pt.setColor(color);
		}
		pt.setIconName(iconName);
		pt.setBackgroundType(backgroundName);
		app.getSelectedGpxHelper().addPoint(pt, currentTrack.getModifiableGpxFile());
		currentTrack.getModifiableGpxFile().modifiedTime = time;
		currentTrack.getModifiableGpxFile().pointsModifiedTime = time;
		points++;

		Map<String, Object> rowsMap = new LinkedHashMap<>();
		rowsMap.put(POINT_COL_LAT, lat);
		rowsMap.put(POINT_COL_LON, lon);
		rowsMap.put(POINT_COL_DATE, time);
		rowsMap.put(POINT_COL_DESCRIPTION, description);
		rowsMap.put(POINT_COL_NAME, name);
		rowsMap.put(POINT_COL_CATEGORY, category);
		rowsMap.put(POINT_COL_COLOR, color);
		rowsMap.put(POINT_COL_ICON, iconName);
		rowsMap.put(POINT_COL_BACKGROUND, backgroundName);

		execWithClose(AndroidDbUtils.createDbInsertQuery(POINT_NAME, rowsMap.keySet()), rowsMap.values().toArray());
		return pt;
	}

	public void updatePointData(WptPt wptPt, double lat, double lon, String description, String name,
	                            String category, int color, String iconName, String iconBackground) {
		long time = System.currentTimeMillis();
		currentTrack.getModifiableGpxFile().modifiedTime = time;
		currentTrack.getModifiableGpxFile().pointsModifiedTime = time;

		List<Object> params = new ArrayList<>();
		params.add(lat);
		params.add(lon);
		params.add(time);
		params.add(description);
		params.add(name);
		params.add(category);
		params.add(color);
		params.add(iconName);
		params.add(iconBackground);

		params.add(wptPt.getLatitude());
		params.add(wptPt.getLongitude());
		params.add(wptPt.time);

		StringBuilder sb = new StringBuilder();
		String prefix = "UPDATE " + POINT_NAME
				+ " SET "
				+ POINT_COL_LAT + "=?, "
				+ POINT_COL_LON + "=?, "
				+ POINT_COL_DATE + "=?, "
				+ POINT_COL_DESCRIPTION + "=?, "
				+ POINT_COL_NAME + "=?, "
				+ POINT_COL_CATEGORY + "=?, "
				+ POINT_COL_COLOR + "=?, "
				+ POINT_COL_ICON + "=?, "
				+ POINT_COL_BACKGROUND + "=? "
				+ "WHERE "
				+ POINT_COL_LAT + "=? AND "
				+ POINT_COL_LON + "=? AND "
				+ POINT_COL_DATE + "=?";

		sb.append(prefix);
		if (wptPt.desc != null) {
			sb.append(" AND ").append(POINT_COL_DESCRIPTION).append("=?");
			params.add(wptPt.desc);
		} else {
			sb.append(" AND ").append(POINT_COL_DESCRIPTION).append(" IS NULL");
		}
		if (wptPt.name != null) {
			sb.append(" AND ").append(POINT_COL_NAME).append("=?");
			params.add(wptPt.name);
		} else {
			sb.append(" AND ").append(POINT_COL_NAME).append(" IS NULL");
		}
		if (wptPt.category != null) {
			sb.append(" AND ").append(POINT_COL_CATEGORY).append("=?");
			params.add(wptPt.category);
		} else {
			sb.append(" AND ").append(POINT_COL_CATEGORY).append(" IS NULL");
		}

		execWithClose(sb.toString(), params.toArray());

		wptPt.lat = lat;
		wptPt.lon = lon;
		wptPt.time = time;
		wptPt.desc = description;
		wptPt.name = name;
		wptPt.category = category;

		if (color != 0) {
			wptPt.setColor(color);
		}
		if (iconName != null) {
			wptPt.setIconName(iconName);
		}
		if (iconBackground != null) {
			wptPt.setBackgroundType(iconBackground);
		}
	}

	public void deletePointData(WptPt pt) {
		app.getSelectedGpxHelper().removePoint(pt, currentTrack.getModifiableGpxFile());
		currentTrack.getModifiableGpxFile().modifiedTime = System.currentTimeMillis();
		currentTrack.getModifiableGpxFile().pointsModifiedTime = System.currentTimeMillis();
		points--;

		List<Object> params = new ArrayList<>();
		params.add(pt.getLatitude());
		params.add(pt.getLongitude());
		params.add(pt.time);

		StringBuilder sb = new StringBuilder();
		String prefix = "DELETE FROM "
				+ POINT_NAME
				+ " WHERE "
				+ POINT_COL_LAT + "=? AND "
				+ POINT_COL_LON + "=? AND "
				+ POINT_COL_DATE + "=?";

		sb.append(prefix);
		if (pt.desc != null) {
			sb.append(" AND ").append(POINT_COL_DESCRIPTION).append("=?");
			params.add(pt.desc);
		} else {
			sb.append(" AND ").append(POINT_COL_DESCRIPTION).append(" IS NULL");
		}
		if (pt.name != null) {
			sb.append(" AND ").append(POINT_COL_NAME).append("=?");
			params.add(pt.name);
		} else {
			sb.append(" AND ").append(POINT_COL_NAME).append(" IS NULL");
		}
		if (pt.category != null) {
			sb.append(" AND ").append(POINT_COL_CATEGORY).append("=?");
			params.add(pt.category);
		} else {
			sb.append(" AND ").append(POINT_COL_CATEGORY).append(" IS NULL");
		}

		execWithClose(sb.toString(), params.toArray());
	}

	private synchronized void execWithClose(@NonNull String script, @NonNull Object[] objects) {
		SQLiteDatabase db = getWritableDatabase();
		if (db != null) {
			try {
				db.execSQL(script, objects);
			} catch (RuntimeException e) {
				log.error(e.getMessage(), e);
			} finally {
				db.close();
			}
		}
	}

	private void executeInsertTrackQuery(double lat, double lon, double alt, double speed, double hdop, long time, float heading, String pluginsInfo) {
		Map<String, Object> rowsMap = new LinkedHashMap<>();
		rowsMap.put(TRACK_COL_LAT, lat);
		rowsMap.put(TRACK_COL_LON, lon);
		rowsMap.put(TRACK_COL_ALTITUDE, alt);
		rowsMap.put(TRACK_COL_SPEED, speed);
		rowsMap.put(TRACK_COL_HDOP, hdop);
		rowsMap.put(TRACK_COL_DATE, time);
		rowsMap.put(TRACK_COL_HEADING, Float.isNaN(heading) ? null : heading);
		rowsMap.put(TRACK_COL_PLUGINS_INFO, pluginsInfo);
		execWithClose(AndroidDbUtils.createDbInsertQuery(TRACK_NAME, rowsMap.keySet()), rowsMap.values().toArray());
	}

	public void loadGpxFromDatabase() {
		Map<String, GPXFile> files = collectRecordedData();
		List<Track> tracks = new ArrayList<>();
		for (Map.Entry<String, GPXFile> entry : files.entrySet()) {
			app.getSelectedGpxHelper().addPoints(entry.getValue().getPoints(), currentTrack.getModifiableGpxFile());
			tracks.addAll(entry.getValue().tracks);
		}
		currentTrack.getModifiableGpxFile().tracks = tracks;
		currentTrack.processPoints(app);
		prepareCurrentTrackForRecording();
		GPXTrackAnalysis analysis = currentTrack.getModifiableGpxFile().getAnalysis(System.currentTimeMillis());
		distance = analysis.getTotalDistance();
		points = analysis.getWptPoints();
		duration = analysis.getTimeSpan();
		trkPoints = analysis.getPoints();
	}

	private void prepareCurrentTrackForRecording() {
		if (currentTrack.getModifiableGpxFile().tracks.size() == 0) {
			currentTrack.getModifiableGpxFile().tracks.add(new Track());
		}
		while (currentTrack.getPointsToDisplay().size() < currentTrack.getModifiableGpxFile().tracks.size()) {
			currentTrack.addEmptySegmentToDisplay();
		}
	}

	public boolean getIsRecording() {
		return PluginsHelper.isActive(OsmandMonitoringPlugin.class)
				&& settings.SAVE_GLOBAL_TRACK_TO_GPX.get() || isRecordingAutomatically();
	}

	private boolean isRecordingAutomatically() {
		return settings.SAVE_TRACK_TO_GPX.get() && (app.getRoutingHelper().isFollowingMode()
				|| lastRoutingApplicationMode == settings.getApplicationMode()
				&& settings.getApplicationMode() != settings.DEFAULT_APPLICATION_MODE.get());
	}

	public float getDistance() {
		return distance;
	}

	public long getDuration() {
		return duration;
	}

	public int getPoints() {
		return points;
	}

	public int getTrkPoints() {
		return trkPoints;
	}

	public long getLastTimeUpdated() {
		return lastTimeUpdated;
	}

	public long getLastTimeFileSaved() {
		return lastTimeFileSaved;
	}

	public void setLastTimeFileSaved(long lastTimeFileSaved) {
		this.lastTimeFileSaved = lastTimeFileSaved;
	}

	public int getCurrentTrackIndex() {
		return currentTrackIndex;
	}

	@NonNull
	public GPXFile getCurrentGpx() {
		return currentTrack.getGpxFile();
	}

	@NonNull
	public SelectedGpxFile getCurrentTrack() {
		return currentTrack;
	}
}