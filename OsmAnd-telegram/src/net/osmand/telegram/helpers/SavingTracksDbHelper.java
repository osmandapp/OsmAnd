package net.osmand.telegram.helpers;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import net.osmand.PlatformUtil;
import net.osmand.telegram.TelegramApplication;
import net.osmand.telegram.utils.GPXUtilities;
import net.osmand.telegram.utils.GPXUtilities.GPXFile;
import net.osmand.telegram.utils.GPXUtilities.Track;
import net.osmand.telegram.utils.GPXUtilities.TrkSegment;
import net.osmand.telegram.utils.GPXUtilities.WptPt;

import org.apache.commons.logging.Log;
import org.drinkless.td.libcore.telegram.TdApi;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SavingTracksDbHelper extends SQLiteOpenHelper {

	public final static String DATABASE_NAME = "tracks"; //$NON-NLS-1$
	public final static int DATABASE_VERSION = 2;

	public final static String TRACK_NAME = "track"; //$NON-NLS-1$
	public final static String TRACK_COL_USER_ID = "user_id"; //$NON-NLS-1$
	public final static String TRACK_COL_CHAT_ID = "chat_id"; //$NON-NLS-1$
	public final static String TRACK_COL_DATE = "date"; //$NON-NLS-1$
	public final static String TRACK_COL_LAT = "lat"; //$NON-NLS-1$
	public final static String TRACK_COL_LON = "lon"; //$NON-NLS-1$
	public final static String TRACK_COL_ALTITUDE = "altitude"; //$NON-NLS-1$
	public final static String TRACK_COL_SPEED = "speed"; //$NON-NLS-1$
	public final static String TRACK_COL_HDOP = "hdop"; //$NON-NLS-1$

	public final static String UPDATE_SCRIPT = "INSERT INTO " + TRACK_NAME + " (" + TRACK_COL_USER_ID + ", " + TRACK_COL_CHAT_ID + ", " + TRACK_COL_LAT + ", " + TRACK_COL_LON + ", "
			+ TRACK_COL_ALTITUDE + ", " + TRACK_COL_SPEED + ", " + TRACK_COL_HDOP + ", " + TRACK_COL_DATE + ")"
			+ " VALUES (?, ?, ?, ?, ?, ?, ?, ?)"; //$NON-NLS-1$ //$NON-NLS-2$

	public final static Log log = PlatformUtil.getLog(SavingTracksDbHelper.class);

	private final TelegramApplication ctx;

	public SavingTracksDbHelper(TelegramApplication ctx) {
		super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
		this.ctx = ctx;

		ctx.getTelegramHelper().addIncomingMessagesListener(new TelegramHelper.TelegramIncomingMessagesListener() {

			@Override
			public void onReceiveChatLocationMessages(long chatId, @NotNull TdApi.Message... messages) {
				for (TdApi.Message message : messages) {
					updateLocationMessage(message);
				}
			}

			@Override
			public void onDeleteChatLocationMessages(long chatId, @NotNull List<? extends TdApi.Message> messages) {

			}

			@Override
			public void updateLocationMessages() {

			}
		});
		ctx.getTelegramHelper().addOutgoingMessagesListener(new TelegramHelper.TelegramOutgoingMessagesListener() {

			@Override
			public void onUpdateMessages(@NotNull List<? extends TdApi.Message> messages) {
				for (TdApi.Message message : messages) {
					updateLocationMessage(message);
				}
			}

			@Override
			public void onDeleteMessages(long chatId, @NotNull List<Long> messages) {

			}

			@Override
			public void onSendLiveLocationError(int code, @NotNull String message) {

			}
		});
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE " + TRACK_NAME + " (" + TRACK_COL_USER_ID + " long," + TRACK_COL_CHAT_ID + " long," + TRACK_COL_LAT + " double, " + TRACK_COL_LON + " double, " //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$//$NON-NLS-5$
				+ TRACK_COL_ALTITUDE + " double, " + TRACK_COL_SPEED + " double, "  //$NON-NLS-1$ //$NON-NLS-2$
				+ TRACK_COL_HDOP + " double, " + TRACK_COL_DATE + " long )"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	}

	public ArrayList<Integer> getUsersIds() {
		ArrayList<Integer> ids = new ArrayList<>();
		try {
			SQLiteDatabase db = getWritableDatabase();
			if (db != null) {
				try {
					Cursor query = db.rawQuery("SELECT " + TRACK_COL_USER_ID + " FROM " + TRACK_NAME + " ORDER BY " + TRACK_COL_DATE + " DESC", null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					if (query.moveToFirst()) {
						do {
							Integer userId = query.getInt(0);
							if (!ids.contains(userId)) {
								ids.add(userId);
							}
						} while (query.moveToNext());
					}
					query.close();
				} finally {
					db.close();
				}
			}
		} catch (RuntimeException e) {
		}
		return ids;
	}

	/**
	 * @return warnings
	 */
	public synchronized List<String> saveDataToGpx(File dir) {
		List<String> warnings = new ArrayList<String>();
		dir.mkdirs();
		if (dir.getParentFile().canWrite()) {
			if (dir.exists()) {
				Map<Integer, GPXFile> data = collectRecordedData();

				// save file
				for (final Integer f : data.keySet()) {
					File fout = new File(dir, f + ".gpx"); //$NON-NLS-1$
					if (!data.get(f).isEmpty()) {
						WptPt pt = data.get(f).findPointToShow();

						TdApi.User user = ctx.getTelegramHelper().getUser(pt.userId);
						String fileName;
						if (user != null) {
							fileName = TelegramUiHelper.INSTANCE.getUserName(user)
									+ "_" + new SimpleDateFormat("yyyy-MM-dd_HH-mm_EEE", Locale.US).format(new Date(pt.time)); //$NON-NLS-1$
						} else {
							fileName = f + "_" + new SimpleDateFormat("HH-mm_EEE", Locale.US).format(new Date(pt.time)); //$NON-NLS-1$
						}
						fout = new File(dir, fileName + ".gpx"); //$NON-NLS-1$
						int ind = 1;
						while (fout.exists()) {
							fout = new File(dir, fileName + "_" + (++ind) + ".gpx"); //$NON-NLS-1$ //$NON-NLS-2$
						}
					}

					String warn = GPXUtilities.writeGpxFile(fout, data.get(f), ctx);
					if (warn != null) {
						warnings.add(warn);
						return warnings;
					}
				}
			}
		}

		return warnings;
	}

	public Map<Integer, GPXFile> collectRecordedData() {
		Map<Integer, GPXFile> data = new LinkedHashMap<Integer, GPXFile>();
		ArrayList<Integer> usersIds = getUsersIds();
		SQLiteDatabase db = getReadableDatabase();
		if (db != null && db.isOpen()) {
			try {
				collectDBTracks(db, data, usersIds);
			} finally {
				db.close();
			}
		}
		return data;
	}

	private void collectDBTracks(SQLiteDatabase db, Map<Integer, GPXFile> dataTracks, ArrayList<Integer> usersIds) {
		for (Integer userId : usersIds) {
			collectDBTracksForUser(db, dataTracks, userId);
		}
	}

	private void collectDBTracksForUser(SQLiteDatabase db, Map<Integer, GPXFile> dataTracks, Integer userId) {
		Cursor query = db.rawQuery("SELECT " + TRACK_COL_USER_ID + "," + TRACK_COL_CHAT_ID + "," + TRACK_COL_LAT + "," + TRACK_COL_LON + "," + TRACK_COL_ALTITUDE + "," //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
				+ TRACK_COL_SPEED + "," + TRACK_COL_HDOP + "," + TRACK_COL_DATE + " FROM " + TRACK_NAME +
				" WHERE " + TRACK_COL_USER_ID + " = ? ORDER BY " + TRACK_COL_DATE + " ASC ", new String[]{String.valueOf(userId)}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		long previousTime = 0;
		long previousInterval = 0;
		TrkSegment segment = null;
		Track track = null;
		if (query.moveToFirst()) {
			do {
				WptPt pt = new WptPt();
				pt.userId = query.getInt(0);
				pt.chatId = query.getLong(1);
				pt.lat = query.getDouble(2);
				pt.lon = query.getDouble(3);
				pt.ele = query.getDouble(4);
				pt.speed = query.getDouble(5);
				pt.hdop = query.getDouble(6);
				long time = query.getLong(7);
				pt.time = time;
				long currentInterval = Math.abs(time - previousTime);
				boolean newInterval = pt.lat == 0 && pt.lon == 0;

				if (track != null && !newInterval && (
//						!ctx.getSettings().AUTO_SPLIT_RECORDING.get() ||
						currentInterval < 30 * 60 * 1000 || currentInterval < 10 * previousInterval)) {
					// 6 minute - same segment
					segment.points.add(pt);
				} else if (track != null && (
//						ctx.getSettings().AUTO_SPLIT_RECORDING.get() &&
						currentInterval < 2 * 60 * 60 * 1000)) {
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

					if (dataTracks.containsKey(pt.userId)) {
						GPXFile gpx = dataTracks.get(pt.userId);
						gpx.tracks.add(track);
					} else {
						GPXFile file = new GPXFile();
						file.tracks.add(track);
						dataTracks.put(pt.userId, file);
					}
				}
				previousInterval = currentInterval;
				previousTime = time;
			} while (query.moveToNext());
		}
		query.close();
	}

	public void updateLocationMessage(TdApi.Message message) {
		// use because there is a bug on some devices with location.getTime()
		long locationTime = System.currentTimeMillis();
		TdApi.MessageContent content = message.content;
		if (content instanceof TdApi.MessageLocation) {
			TdApi.MessageLocation messageLocation = (TdApi.MessageLocation) content;
			insertData(message.senderUserId, message.chatId, messageLocation.location.latitude, messageLocation.location.longitude, 0.0, 0.0, 0.0, locationTime);
		} else if (content instanceof TelegramHelper.MessageLocation) {
			TelegramHelper.MessageLocation messageLocation = (TelegramHelper.MessageLocation) content;
			insertData(message.senderUserId, message.chatId, messageLocation.getLat(), messageLocation.getLon(),
					messageLocation.getAltitude(), messageLocation.getSpeed(), messageLocation.getHdop(), messageLocation.getLastUpdated() * 1000L);
		}
	}

	public void insertData(int userId, long chatId, double lat, double lon, double alt, double speed, double hdop, long time) {
		execWithClose(UPDATE_SCRIPT, new Object[]{userId, chatId, lat, lon, alt, speed, hdop, time});
	}

	private synchronized void execWithClose(String script, Object[] objects) {
		SQLiteDatabase db = getWritableDatabase();
		try {
			if (db != null) {
				db.execSQL(script, objects);
			}
		} catch (RuntimeException e) {
			log.error(e.getMessage(), e);
		} finally {
			if (db != null) {
				db.close();
			}
		}
	}
}