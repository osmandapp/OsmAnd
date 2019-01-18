package net.osmand.telegram.helpers;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;

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
import java.util.List;
import java.util.Locale;

public class SavingTracksDbHelper extends SQLiteOpenHelper {

	private final static String DATABASE_NAME = "tracks";
	private final static int DATABASE_VERSION = 3;

	private final static String TRACK_NAME = "track"; //$NON-NLS-1$
	private final static String TRACK_COL_USER_ID = "user_id"; //$NON-NLS-1$
	private final static String TRACK_COL_CHAT_ID = "chat_id"; //$NON-NLS-1$
	private final static String TRACK_COL_DATE = "date"; //$NON-NLS-1$
	private final static String TRACK_COL_LAT = "lat"; //$NON-NLS-1$
	private final static String TRACK_COL_LON = "lon"; //$NON-NLS-1$
	private final static String TRACK_COL_ALTITUDE = "altitude"; //$NON-NLS-1$
	private final static String TRACK_COL_SPEED = "speed"; //$NON-NLS-1$
	private final static String TRACK_COL_HDOP = "hdop"; //$NON-NLS-1$
	private final static String TRACK_COL_TEXT_INFO = "text_info"; // 1 = true, 0 = false //$NON-NLS-1$

	private final static String INSERT_SCRIPT = "INSERT INTO " + TRACK_NAME + " (" + TRACK_COL_USER_ID + ", " + TRACK_COL_CHAT_ID + ", " + TRACK_COL_LAT + ", " + TRACK_COL_LON + ", "
			+ TRACK_COL_ALTITUDE + ", " + TRACK_COL_SPEED + ", " + TRACK_COL_HDOP + ", " + TRACK_COL_DATE + ", " + TRACK_COL_TEXT_INFO + ")"
			+ " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

	private final static String CREATE_SCRIPT = "CREATE TABLE " + TRACK_NAME + " (" + TRACK_COL_USER_ID + " long," + TRACK_COL_CHAT_ID + " long," + TRACK_COL_LAT + " double, " + TRACK_COL_LON + " double, " //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$//$NON-NLS-5$
			+ TRACK_COL_ALTITUDE + " double, " + TRACK_COL_SPEED + " double, "  //$NON-NLS-1$ //$NON-NLS-2$
			+ TRACK_COL_HDOP + " double, " + TRACK_COL_DATE + " long, " + TRACK_COL_TEXT_INFO + " int )";

	private final static Log log = PlatformUtil.getLog(SavingTracksDbHelper.class);

	private final TelegramApplication app;

	public SavingTracksDbHelper(TelegramApplication app) {
		super(app, DATABASE_NAME, null, DATABASE_VERSION);
		this.app = app;

		app.getTelegramHelper().addIncomingMessagesListener(new TelegramHelper.TelegramIncomingMessagesListener() {

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
		app.getTelegramHelper().addOutgoingMessagesListener(new TelegramHelper.TelegramOutgoingMessagesListener() {

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
		db.execSQL(CREATE_SCRIPT);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (oldVersion < 3) {
			db.execSQL("ALTER TABLE " + TRACK_NAME + " ADD " + TRACK_COL_TEXT_INFO + " int");
		}
	}

	public void saveUserDataToGpx(SaveGpxListener listener, File dir, int userId, long chatId, long start, long end) {
		GPXFile gpxFile = collectRecordedDataForUserAndChat(userId, chatId, start, end);
		if (gpxFile != null && !gpxFile.isEmpty()) {
			SaveGPXTrackToFileTask task = new SaveGPXTrackToFileTask(app, listener, gpxFile, dir, userId);
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
	}

	public void saveGpx(SaveGpxListener listener, File dir, GPXFile gpxFile) {
		if (gpxFile != null && !gpxFile.isEmpty()) {
			SaveGPXTrackToFileTask task = new SaveGPXTrackToFileTask(app, listener, gpxFile, dir, 0);
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
	}

	private void updateLocationMessage(TdApi.Message message) {
		log.debug(message);
		TdApi.MessageContent content = message.content;
		int senderId = app.getTelegramHelper().getSenderMessageId(message);
		if (content instanceof TdApi.MessageLocation) {
			long lastTextMessageUpdate = getLastTextTrackPointTimeForUser(message.senderUserId);
			long currentTime = System.currentTimeMillis();
			if (lastTextMessageUpdate == 0 || currentTime - lastTextMessageUpdate < 10 * 1000) {
				log.debug("Add map message " + message.senderUserId);
				TdApi.MessageLocation messageLocation = (TdApi.MessageLocation) content;
				insertData(senderId, message.chatId, messageLocation.location.latitude,
						messageLocation.location.longitude, 0.0, 0.0, 0.0,
						Math.max(message.date, message.editDate), 0);
			} else {
				log.debug("Skip map message");
			}
		} else if (content instanceof TelegramHelper.MessageLocation) {
			log.debug("Add text message " + message.senderUserId);
			TelegramHelper.MessageLocation messageLocation = (TelegramHelper.MessageLocation) content;
			insertData(senderId, message.chatId, messageLocation.getLat(), messageLocation.getLon(),
					messageLocation.getAltitude(), messageLocation.getSpeed(), messageLocation.getHdop(),
					messageLocation.getLastUpdated() * 1000L, 1);
		}
	}

	private void insertData(int userId, long chatId, double lat, double lon, double alt, double speed, double hdop, long time, int textMessage) {
		execWithClose(INSERT_SCRIPT, new Object[]{userId, chatId, lat, lon, alt, speed, hdop, time, textMessage});
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

	private long getLastTextTrackPointTimeForUser(int userId) {
		long res = 0;
		try {
			SQLiteDatabase db = getWritableDatabase();
			if (db != null) {
				try {
					Cursor query = db.rawQuery("SELECT " + TRACK_COL_DATE + " FROM " + TRACK_NAME + " WHERE " + TRACK_COL_USER_ID + " = ? AND "
							+ TRACK_COL_TEXT_INFO + " = ?" + " ORDER BY " + TRACK_COL_DATE + " ASC ", new String[]{String.valueOf(userId), String.valueOf(1)});
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

	public GPXFile collectRecordedDataForUserAndChat(int userId, long chatId, long start, long end) {
		GPXFile gpxFile = null;
		SQLiteDatabase db = getReadableDatabase();
		if (db != null && db.isOpen()) {
			try {
				gpxFile = collectDBTracksForUser(db, userId, chatId, start, end);
			} finally {
				db.close();
			}
		}
		return gpxFile;
	}

	public GPXFile collectRecordedDataForUser(int userId, long chatId, long start, long end) {
		GPXFile gpxFile = null;
		SQLiteDatabase db = getReadableDatabase();
		if (db != null && db.isOpen()) {
			try {
				if (chatId == 0) {
					gpxFile = collectDBTracksForUser(db, userId, start, end);
				} else {
					gpxFile = collectDBTracksForUser(db, userId, chatId, start, end);
				}
			} finally {
				db.close();
			}
		}
		return gpxFile;
	}

	public ArrayList<GPXFile> collectRecordedDataForUsers(long start, long end, ArrayList<Integer> ignoredUsersIds) {
		ArrayList<GPXFile> data = new ArrayList<>();
		SQLiteDatabase db = getReadableDatabase();
		if (db != null && db.isOpen()) {
			try {
				collectDBTracksForUsers(db, data, start, end, ignoredUsersIds);
			} finally {
				db.close();
			}
		}
		return data;
	}

	private GPXFile collectDBTracksForUser(SQLiteDatabase db, int userId, long chatId, long start, long end) {
		Cursor query = db.rawQuery("SELECT " + TRACK_COL_USER_ID + "," + TRACK_COL_CHAT_ID + ","
				+ TRACK_COL_LAT + "," + TRACK_COL_LON + "," + TRACK_COL_ALTITUDE + "," + TRACK_COL_SPEED + ","
				+ TRACK_COL_HDOP + "," + TRACK_COL_DATE + " FROM " + TRACK_NAME + " WHERE " + TRACK_COL_USER_ID + " = ?"
				+ " AND " + TRACK_COL_CHAT_ID + " = ?" + " AND " + TRACK_COL_DATE + " BETWEEN " + start + " AND " + end
				+ " ORDER BY " + TRACK_COL_DATE + " ASC ", new String[]{String.valueOf(userId), String.valueOf(chatId)});

		GPXFile gpxFile = null;
		long previousTime = 0;
		TrkSegment segment = null;
		Track track = null;
		if (query.moveToFirst()) {
			gpxFile = new GPXFile();
			gpxFile.chatId = chatId;
			gpxFile.userId = userId;
			do {
				long time = query.getLong(7);
				WptPt pt = new WptPt();
				pt.userId = query.getInt(0);
				pt.chatId = query.getLong(1);
				pt.lat = query.getDouble(2);
				pt.lon = query.getDouble(3);
				pt.ele = query.getDouble(4);
				pt.speed = query.getDouble(5);
				pt.hdop = query.getDouble(6);
				pt.time = time;
				long currentInterval = Math.abs(time - previousTime);

				if (track != null) {
					if (currentInterval < 30 * 60 * 1000) {
						// 30 minute - same segment
						segment.points.add(pt);
					} else {
						segment = new TrkSegment();
						segment.points.add(pt);
						track.segments.add(segment);
					}
				} else {
					track = new Track();
					segment = new TrkSegment();
					track.segments.add(segment);
					segment.points.add(pt);

					gpxFile.tracks.add(track);
				}
				previousTime = time;
			} while (query.moveToNext());
		}
		query.close();
		return gpxFile;
	}

	private GPXFile collectDBTracksForUser(SQLiteDatabase db, int userId, long start, long end) {
		Cursor query = db.rawQuery("SELECT " + TRACK_COL_USER_ID + "," + TRACK_COL_CHAT_ID + ","
				+ TRACK_COL_LAT + "," + TRACK_COL_LON + "," + TRACK_COL_ALTITUDE + "," + TRACK_COL_SPEED + ","
				+ TRACK_COL_HDOP + "," + TRACK_COL_DATE + " FROM " + TRACK_NAME + " WHERE " + TRACK_COL_USER_ID + " = ?"
				+ " AND " + TRACK_COL_DATE + " BETWEEN " + start + " AND " + end
				+ " ORDER BY " + TRACK_COL_DATE + " ASC ", new String[]{String.valueOf(userId)});

		GPXFile gpxFile = null;
		long previousTime = 0;
		TrkSegment segment = null;
		Track track = null;
		if (query.moveToFirst()) {
			gpxFile = new GPXFile();
			gpxFile.userId = userId;
			do {
				long time = query.getLong(7);
				WptPt pt = new WptPt();
				pt.userId = query.getInt(0);
				pt.chatId = query.getLong(1);
				pt.lat = query.getDouble(2);
				pt.lon = query.getDouble(3);
				pt.ele = query.getDouble(4);
				pt.speed = query.getDouble(5);
				pt.hdop = query.getDouble(6);
				pt.time = time;
				long currentInterval = Math.abs(time - previousTime);

				if (track != null) {
					if (currentInterval < 30 * 60 * 1000) {
						// 30 minute - same segment
						segment.points.add(pt);
					} else {
						segment = new TrkSegment();
						segment.points.add(pt);
						track.segments.add(segment);
					}
				} else {
					track = new Track();
					segment = new TrkSegment();
					track.segments.add(segment);
					segment.points.add(pt);

					gpxFile.tracks.add(track);
				}
				previousTime = time;
			} while (query.moveToNext());
		}
		query.close();
		return gpxFile;
	}

	private void collectDBTracksForUsers(SQLiteDatabase db, ArrayList<GPXFile> dataTracks, long start, long end, ArrayList<Integer> ignoredUsersIds) {
		Cursor query = db.rawQuery("SELECT " + TRACK_COL_USER_ID + "," + TRACK_COL_CHAT_ID + ","
				+ TRACK_COL_LAT + "," + TRACK_COL_LON + "," + TRACK_COL_ALTITUDE + "," + TRACK_COL_SPEED + ","
				+ TRACK_COL_HDOP + "," + TRACK_COL_DATE + " FROM " + TRACK_NAME + " WHERE " + TRACK_COL_DATE
				+ " BETWEEN " + start + " AND " + end + " ORDER BY " + TRACK_COL_USER_ID + " ASC, "
				+ TRACK_COL_CHAT_ID + " ASC, " + TRACK_COL_DATE + " ASC ", null);

		long previousTime = 0;
		long previousChatId = 0;
		int previousUserId = 0;
		TrkSegment segment = null;
		Track track = null;
		GPXFile gpx = new GPXFile();
		if (query.moveToFirst()) {
			do {
				int userId = query.getInt(0);
				if (ignoredUsersIds.contains(userId)) {
					continue;
				}
				int chatId = query.getInt(1);
				long time = query.getLong(7);
				if (previousUserId != userId || previousChatId != chatId) {
					gpx = new GPXFile();
					gpx.chatId = chatId;
					gpx.userId = userId;
					previousTime = 0;
					track = null;
					segment = null;
					dataTracks.add(gpx);
				}

				WptPt pt = new WptPt();
				pt.userId = userId;
				pt.chatId = chatId;
				pt.lat = query.getDouble(2);
				pt.lon = query.getDouble(3);
				pt.ele = query.getDouble(4);
				pt.speed = query.getDouble(5);
				pt.hdop = query.getDouble(6);
				pt.time = time;
				long currentInterval = Math.abs(time - previousTime);
				if (track != null) {
					if (currentInterval < 30 * 60 * 1000) {
						// 30 minute - same segment
						segment.points.add(pt);
					} else {
						segment = new TrkSegment();
						segment.points.add(pt);
						track.segments.add(segment);
					}
				} else {
					track = new Track();
					segment = new TrkSegment();
					track.segments.add(segment);
					segment.points.add(pt);

					gpx.tracks.add(track);
				}
				previousTime = time;
				previousUserId = userId;
				previousChatId = chatId;
			} while (query.moveToNext());
		}
		query.close();
	}

	private static class SaveGPXTrackToFileTask extends AsyncTask<Void, Void, List<String>> {

		private TelegramApplication app;
		private SaveGpxListener listener;

		private final GPXFile gpxFile;
		private File dir;
		private int userId;

		SaveGPXTrackToFileTask(TelegramApplication app, SaveGpxListener listener, GPXFile gpxFile, File dir, int userId) {
			this.gpxFile = gpxFile;
			this.listener = listener;
			this.app = app;
			this.dir = dir;
			this.userId = userId;
		}

		@Override
		protected List<String> doInBackground(Void... params) {
			List<String> warnings = new ArrayList<String>();
			dir.mkdirs();
			if (dir.getParentFile().canWrite()) {
				if (dir.exists()) {

					// save file
					File fout = new File(dir, userId + ".gpx"); //$NON-NLS-1$
					if (!gpxFile.isEmpty()) {
						WptPt pt = gpxFile.findPointToShow();

						TdApi.User user = app.getTelegramHelper().getUser(pt.userId);
						String fileName;
						if (user != null) {
							fileName = TelegramUiHelper.INSTANCE.getUserName(user)
									+ "_" + new SimpleDateFormat("yyyy-MM-dd_HH-mm_EEE", Locale.US).format(new Date(pt.time)); //$NON-NLS-1$
						} else {
							fileName = userId + "_" + new SimpleDateFormat("yyyy-MM-dd_HH-mm_EEE", Locale.US).format(new Date(pt.time)); //$NON-NLS-1$
						}
						fout = new File(dir, fileName + ".gpx"); //$NON-NLS-1$
						int ind = 1;
						while (fout.exists()) {
							fout = new File(dir, fileName + "_" + (++ind) + ".gpx"); //$NON-NLS-1$ //$NON-NLS-2$
						}
					}
					String warn = GPXUtilities.writeGpxFile(fout, gpxFile, app);
					if (warn != null) {
						warnings.add(warn);
						return warnings;
					}
				}
			}

			return warnings;
		}

		@Override
		protected void onPostExecute(List<String> warnings) {
			if (listener != null) {
				if (warnings != null && warnings.isEmpty()) {
					listener.onSavingGpxFinish(gpxFile.path);
				} else {
					listener.onSavingGpxError(warnings);
				}
			}
		}
	}


	public interface SaveGpxListener {

		void onSavingGpxFinish(String path);

		void onSavingGpxError(List<String> warnings);
	}
}