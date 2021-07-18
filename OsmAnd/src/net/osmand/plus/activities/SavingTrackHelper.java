package net.osmand.plus.activities;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.format.DateFormat;

import net.osmand.AndroidUtils;
import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.GPXTrackAnalysis;
import net.osmand.GPXUtilities.Track;
import net.osmand.GPXUtilities.TrkSegment;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.plus.GPXDatabase.GpxDataItem;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.Version;
import net.osmand.plus.monitoring.OsmandMonitoringPlugin;
import net.osmand.plus.notifications.OsmandNotification.NotificationType;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import androidx.annotation.NonNull;

public class SavingTrackHelper extends SQLiteOpenHelper {

	public final static String DATABASE_NAME = "tracks"; //$NON-NLS-1$
	public final static int DATABASE_VERSION = 7;

	public final static String TRACK_NAME = "track"; //$NON-NLS-1$
	public final static String TRACK_COL_DATE = "date"; //$NON-NLS-1$
	public final static String TRACK_COL_LAT = "lat"; //$NON-NLS-1$
	public final static String TRACK_COL_LON = "lon"; //$NON-NLS-1$
	public final static String TRACK_COL_ALTITUDE = "altitude"; //$NON-NLS-1$
	public final static String TRACK_COL_SPEED = "speed"; //$NON-NLS-1$
	public final static String TRACK_COL_HDOP = "hdop"; //$NON-NLS-1$
	public final static String TRACK_COL_HEADING = "heading"; //$NON-NLS-1$

	public final static String POINT_NAME = "point"; //$NON-NLS-1$
	public final static String POINT_COL_DATE = "date"; //$NON-NLS-1$
	public final static String POINT_COL_LAT = "lat"; //$NON-NLS-1$
	public final static String POINT_COL_LON = "lon"; //$NON-NLS-1$
	public final static String POINT_COL_NAME = "pname"; //$NON-NLS-1$
	public final static String POINT_COL_CATEGORY = "category"; //$NON-NLS-1$
	public final static String POINT_COL_DESCRIPTION = "description"; //$NON-NLS-1$
	public final static String POINT_COL_COLOR = "color"; //$NON-NLS-1$
	public final static String POINT_COL_ICON = "icon"; //$NON-NLS-1$
	public final static String POINT_COL_BACKGROUND = "background"; //$NON-NLS-1$

	public final static float NO_HEADING = -1.0f;

	public final static Log log = PlatformUtil.getLog(SavingTrackHelper.class);

	private long lastTimeUpdated = 0;
	private final OsmandApplication ctx;
	private final OsmandSettings settings;

	private LatLon lastPoint;
	private float distance = 0;
	private long duration = 0;
	private final SelectedGpxFile currentTrack;
	private int points;
	private int trkPoints = 0;
	private long lastTimeFileSaved;

	private ApplicationMode lastRoutingApplicationMode;

	public SavingTrackHelper(OsmandApplication ctx) {
		super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
		this.ctx = ctx;
		this.settings = ctx.getSettings();
		this.currentTrack = new SelectedGpxFile();
		this.currentTrack.setShowCurrentTrack(true);
		GPXFile gx = new GPXFile(Version.getFullVersion(ctx));
		gx.showCurrentTrack = true;
		this.currentTrack.setGpxFile(gx, ctx);
		prepareCurrentTrackForRecording();
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		createTableForTrack(db);
		createTableForPoints(db);
	}

	private void createTableForTrack(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE " + TRACK_NAME + " (" + TRACK_COL_LAT + " double, " + TRACK_COL_LON + " double, " //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				+ TRACK_COL_ALTITUDE + " double, " + TRACK_COL_SPEED + " double, "  //$NON-NLS-1$ //$NON-NLS-2$
				+ TRACK_COL_HDOP + " double, " + TRACK_COL_DATE + " long, "
				+ TRACK_COL_HEADING + " float )"); //$NON-NLS-1$ //$NON-NLS-2$
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
					q = db.query(false, POINT_NAME, new String[]{POINT_COL_LAT, POINT_COL_LON}, null, null, null, null, null, null);
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
	public synchronized SaveGpxResult saveDataToGpx(File dir) {
		List<String> warnings = new ArrayList<>();
		Map<String, GPXFile> gpxFilesByName = new LinkedHashMap<>();
		dir.mkdirs();
		if (dir.getParentFile().canWrite()) {
			if (dir.exists()) {
				Map<String, GPXFile> data = collectRecordedData();

				// save file
				for (final Map.Entry<String, GPXFile> entry : data.entrySet()) {
					final String f = entry.getKey();
					GPXFile gpx = entry.getValue();
					log.debug("Filename: " + f);
					File fout = new File(dir, f + IndexConstants.GPX_FILE_EXT);
					if (!gpx.isEmpty()) {
						WptPt pt = gpx.findPointToShow();
						String fileName = f + "_" + new SimpleDateFormat("HH-mm_EEE", Locale.US).format(new Date(pt.time)); //$NON-NLS-1$
						Integer trackStorageDirectory = ctx.getSettings().TRACK_STORAGE_DIRECTORY.get();
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
						return new SaveGpxResult(warnings, new HashMap<String, GPXFile>());
					}

					GPXTrackAnalysis analysis = gpx.getAnalysis(fout.lastModified());
					GpxDataItem item = new GpxDataItem(fout, analysis);
					ctx.getGpxDbHelper().add(item);
					lastTimeFileSaved = fout.lastModified();
				}
			}
		}
		clearRecordedData(warnings.isEmpty());
		return new SaveGpxResult(warnings, gpxFilesByName);
	}

	public void clearRecordedData(boolean isWarningEmpty) {
		if (isWarningEmpty) {
			SQLiteDatabase db = getWritableDatabase();
			if (db != null) {
				try {
					if (db.isOpen()) {
						db.execSQL("DELETE FROM " + TRACK_NAME + " WHERE " + TRACK_COL_DATE + " <= ?", new Object[]{System.currentTimeMillis()}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						db.execSQL("DELETE FROM " + POINT_NAME + " WHERE " + POINT_COL_DATE + " <= ?", new Object[]{System.currentTimeMillis()}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
		ctx.getSelectedGpxHelper().clearPoints(currentTrack.getModifiableGpxFile());
		currentTrack.getModifiableGpxFile().tracks.clear();
		currentTrack.getModifiablePointsToDisplay().clear();
		currentTrack.getModifiableGpxFile().modifiedTime = System.currentTimeMillis();
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

	private void collectDBPoints(SQLiteDatabase db, Map<String, GPXFile> dataTracks) {
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
					gpx = new GPXFile(Version.getFullVersion(ctx));
					dataTracks.put(date, gpx);
				}
				ctx.getSelectedGpxHelper().addPoint(pt, gpx);

			} while (query.moveToNext());
		}
		query.close();
	}

	private void collectDBTracks(SQLiteDatabase db, Map<String, GPXFile> dataTracks) {
		Cursor query = db.rawQuery("SELECT " + TRACK_COL_LAT + "," + TRACK_COL_LON + "," + TRACK_COL_ALTITUDE + "," //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				+ TRACK_COL_SPEED + "," + TRACK_COL_HDOP + "," + TRACK_COL_DATE + "," + TRACK_COL_HEADING + " FROM " + TRACK_NAME + " ORDER BY " + TRACK_COL_DATE + " ASC", null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
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
				long time = query.getLong(5);
				pt.time = time;
				float heading = query.getFloat(6);
				pt.heading = heading == NO_HEADING ? Float.NaN : heading;
				long currentInterval = Math.abs(time - previousTime);
				boolean newInterval = pt.lat == 0 && pt.lon == 0;

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
					// check if date the same - new track otherwise new file  
					track = new Track();
					segment = new TrkSegment();
					track.segments.add(segment);
					if (!newInterval) {
						segment.points.add(pt);
					}
					String date = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date(time));  //$NON-NLS-1$
					if (dataTracks.containsKey(date)) {
						GPXFile gpx = dataTracks.get(date);
						gpx.tracks.add(track);
					} else {
						GPXFile file = new GPXFile(Version.getFullVersion(ctx));
						file.tracks.add(track);
						dataTracks.put(date, file);
					}
				}
				previousInterval = currentInterval;
				previousTime = time;
			} while (query.moveToNext());
		}
		query.close();

		// drop empty tracks
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
		executeInsertTrackQuery(0, 0, 0, 0, 0, System.currentTimeMillis(), NO_HEADING);
		addTrackPoint(null, true, System.currentTimeMillis());
	}

	public void updateLocation(net.osmand.Location location, Float heading) {
		// use because there is a bug on some devices with location.getTime()
		long locationTime = System.currentTimeMillis();
		if (heading != null && settings.SAVE_HEADING_TO_GPX.get()) {
			heading = MapUtils.normalizeDegrees360(heading);
		} else {
			heading = NO_HEADING;
		}
		if (ctx.getRoutingHelper().isFollowingMode()) {
			lastRoutingApplicationMode = settings.getApplicationMode();
		} else if (settings.getApplicationMode() == settings.DEFAULT_APPLICATION_MODE.get()) {
			lastRoutingApplicationMode = null;
		}
		boolean record = false;
		if (location != null && OsmAndLocationProvider.isNotSimulatedLocation(location)
				&& OsmandPlugin.isActive(OsmandMonitoringPlugin.class)) {
			if (isRecordingAutomatically() && locationTime - lastTimeUpdated > settings.SAVE_TRACK_INTERVAL.get()) {
				record = true;
			} else if (settings.SAVE_GLOBAL_TRACK_TO_GPX.get()
					&& locationTime - lastTimeUpdated > settings.SAVE_GLOBAL_TRACK_INTERVAL.get()) {
				record = true;
			}
			float minDistance = settings.SAVE_TRACK_MIN_DISTANCE.get();
			if (minDistance > 0 && lastPoint != null && MapUtils.getDistance(lastPoint, location.getLatitude(), location.getLongitude()) <
					minDistance) {
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
		if (record) {
			insertData(location.getLatitude(), location.getLongitude(), location.getAltitude(), location.getSpeed(),
					location.getAccuracy(), locationTime, heading);
			ctx.getNotificationHelper().refreshNotification(NotificationType.GPX);
		}
	}

	public void insertData(double lat, double lon, double alt, double speed, double hdop, long time, float heading) {
		executeInsertTrackQuery(lat, lon, alt, speed, hdop, time, heading);
		boolean newSegment = false;
		if (lastPoint == null || (time - lastTimeUpdated) > 180 * 1000) {
			lastPoint = new LatLon(lat, lon);
			newSegment = true;
		} else {
			float[] lastInterval = new float[1];
			net.osmand.Location.distanceBetween(lat, lon, lastPoint.getLatitude(), lastPoint.getLongitude(),
					lastInterval);
			if (lastTimeUpdated > 0 && time > lastTimeUpdated) {
				duration += time - lastTimeUpdated;
			}
			distance += lastInterval[0];
			lastPoint = new LatLon(lat, lon);
		}
		lastTimeUpdated = time;
		heading = heading == NO_HEADING ? Float.NaN : heading;
		WptPt pt = new GPXUtilities.WptPt(lat, lon, time, alt, speed, hdop, heading);
		addTrackPoint(pt, newSegment, time);
		trkPoints++;
	}

	private void addTrackPoint(WptPt pt, boolean newSegment, long time) {
		List<TrkSegment> points = currentTrack.getModifiablePointsToDisplay();
		Track track = currentTrack.getModifiableGpxFile().tracks.get(0);
		if (points.size() == 0 || newSegment) {
			points.add(new TrkSegment());
		}
		boolean segmentAdded = false;
		if (track.segments.size() == 0 || newSegment) {
			track.segments.add(new TrkSegment());
			segmentAdded = true;
		}
		if (pt != null) {
			int ind = points.size() - 1;
			TrkSegment last = points.get(ind);
			last.points.add(pt);
			TrkSegment lt = track.segments.get(track.segments.size() - 1);
			lt.points.add(pt);
		}
		if (segmentAdded) {
			currentTrack.processPoints(ctx);
		}
		currentTrack.getModifiableGpxFile().modifiedTime = time;
	}

	public WptPt insertPointData(double lat, double lon, long time, String description, String name, String category,
								 int color) {
		return insertPointData(lat, lon, time, description, name, category, color, null, null);
	}

	public WptPt insertPointData(double lat, double lon, long time, String description, String name, String category,
								 int color, String iconName, String backgroundName) {
		final WptPt pt = new WptPt(lat, lon, time, Double.NaN, 0, Double.NaN);
		pt.name = name;
		pt.category = category;
		pt.desc = description;
		if (color != 0) {
			pt.setColor(color);
		}
		pt.setIconName(iconName);
		pt.setBackgroundType(backgroundName);
		ctx.getSelectedGpxHelper().addPoint(pt, currentTrack.getModifiableGpxFile());
		currentTrack.getModifiableGpxFile().modifiedTime = time;
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

		execWithClose(AndroidUtils.createDbInsertQuery(POINT_NAME, rowsMap.keySet()), rowsMap.values().toArray());
		return pt;
	}

	public void updatePointData(WptPt pt, double lat, double lon, long time, String description, String name, String category, int color) {
		updatePointData(pt, lat, lon, time, description, name, category, color, null, null);
	}

	public void updatePointData(WptPt pt, double lat, double lon, long time, String description, String name,
								String category, int color, String iconName, String iconBackground) {
		currentTrack.getModifiableGpxFile().modifiedTime = time;

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

		params.add(pt.getLatitude());
		params.add(pt.getLongitude());
		params.add(pt.time);

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

		pt.lat = lat;
		pt.lon = lon;
		pt.time = time;
		pt.desc = description;
		pt.name = name;
		pt.category = category;
		if (color != 0) {
			pt.setColor(color);
		}
		if (iconName != null) {
			pt.setIconName(iconName);
		}
		if (iconBackground != null) {
			pt.setBackgroundType(iconBackground);
		}
	}

	public void deletePointData(WptPt pt) {
		ctx.getSelectedGpxHelper().removePoint(pt, currentTrack.getModifiableGpxFile());
		currentTrack.getModifiableGpxFile().modifiedTime = System.currentTimeMillis();
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

	private void executeInsertTrackQuery(double lat, double lon, double alt, double speed, double hdop, long time, float heading) {
		Map<String, Object> rowsMap = new LinkedHashMap<>();
		rowsMap.put(TRACK_COL_LAT, lat);
		rowsMap.put(TRACK_COL_LON, lon);
		rowsMap.put(TRACK_COL_ALTITUDE, alt);
		rowsMap.put(TRACK_COL_SPEED, speed);
		rowsMap.put(TRACK_COL_HDOP, hdop);
		rowsMap.put(TRACK_COL_DATE, time);
		rowsMap.put(TRACK_COL_HEADING, heading);
		execWithClose(AndroidUtils.createDbInsertQuery(TRACK_NAME, rowsMap.keySet()), rowsMap.values().toArray());
	}

	public void loadGpxFromDatabase() {
		Map<String, GPXFile> files = collectRecordedData();
		currentTrack.getModifiableGpxFile().tracks.clear();
		for (Map.Entry<String, GPXFile> entry : files.entrySet()) {
			ctx.getSelectedGpxHelper().addPoints(entry.getValue().getPoints(), currentTrack.getModifiableGpxFile());
			currentTrack.getModifiableGpxFile().tracks.addAll(entry.getValue().tracks);
		}
		currentTrack.processPoints(ctx);
		prepareCurrentTrackForRecording();
		GPXTrackAnalysis analysis = currentTrack.getModifiableGpxFile().getAnalysis(System.currentTimeMillis());
		distance = analysis.totalDistance;
		points = analysis.wptPoints;
		duration = analysis.timeSpan;
		trkPoints = analysis.points;
	}

	private void prepareCurrentTrackForRecording() {
		if (currentTrack.getModifiableGpxFile().tracks.size() == 0) {
			currentTrack.getModifiableGpxFile().tracks.add(new Track());
		}
		while (currentTrack.getPointsToDisplay().size() < currentTrack.getModifiableGpxFile().tracks.size()) {
			TrkSegment trkSegment = new TrkSegment();
			currentTrack.getModifiablePointsToDisplay().add(trkSegment);
		}
	}

	public boolean getIsRecording() {
		return OsmandPlugin.isActive(OsmandMonitoringPlugin.class)
				&& settings.SAVE_GLOBAL_TRACK_TO_GPX.get() || isRecordingAutomatically();
	}

	private boolean isRecordingAutomatically() {
		return settings.SAVE_TRACK_TO_GPX.get() && (ctx.getRoutingHelper().isFollowingMode()
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

	public GPXFile getCurrentGpx() {
		return currentTrack.getGpxFile();
	}

	@NonNull
	public SelectedGpxFile getCurrentTrack() {
		return currentTrack;
	}

	public static class SaveGpxResult {

		private final List<String> warnings;
		private final Map<String, GPXFile> gpxFilesByName;

		public SaveGpxResult(List<String> warnings, Map<String, GPXFile> gpxFilesByName) {
			this.warnings = warnings;
			this.gpxFilesByName = gpxFilesByName;
		}

		public List<String> getWarnings() {
			return warnings;
		}

		public Map<String, GPXFile> getGpxFilesByName() {
			return gpxFilesByName;
		}
	}
}