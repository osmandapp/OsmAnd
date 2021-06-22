package net.osmand.plus.backup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.api.SQLiteAPI.SQLiteConnection;
import net.osmand.plus.api.SQLiteAPI.SQLiteCursor;

import java.util.ArrayList;
import java.util.List;

public class BackupDbHelper {

	private final OsmandApplication app;

	private static final String DB_NAME = "backup_files";
	private static final int DB_VERSION = 2;
	private static final String UPLOADED_FILES_TABLE_NAME = "uploaded_files";
	private static final String UPLOADED_FILE_COL_TYPE = "type";
	private static final String UPLOADED_FILE_COL_NAME = "name";
	private static final String UPLOADED_FILE_COL_UPLOAD_TIME = "upload_time";
	private static final String UPLOADED_FILES_TABLE_CREATE = "CREATE TABLE IF NOT EXISTS " + UPLOADED_FILES_TABLE_NAME + " (" +
			UPLOADED_FILE_COL_TYPE + " TEXT, " +
			UPLOADED_FILE_COL_NAME + " TEXT, " +
			UPLOADED_FILE_COL_UPLOAD_TIME + " long);";

	private static final String LAST_MODIFIED_TABLE_NAME = "last_modified_items";
	private static final String LAST_MODIFIED_COL_NAME = "name";
	private static final String LAST_MODIFIED_COL_MODIFIED_TIME = "last_modified_time";
	private static final String LAST_MODIFIED_TABLE_CREATE = "CREATE TABLE IF NOT EXISTS " + LAST_MODIFIED_TABLE_NAME + " (" +
			LAST_MODIFIED_COL_NAME + " TEXT, " +
			LAST_MODIFIED_COL_MODIFIED_TIME + " long);";

	public static class UploadedFileInfo {
		private final String type;
		private final String name;
		private long uploadTime;

		public UploadedFileInfo(String type, String name, long uploadTime) {
			this.type = type;
			this.name = name;
			this.uploadTime = uploadTime;
		}

		public String getType() {
			return type;
		}

		public String getName() {
			return name;
		}

		public long getUploadTime() {
			return uploadTime;
		}

		public void setUploadTime(long uploadTime) {
			this.uploadTime = uploadTime;
		}
	}

	public BackupDbHelper(OsmandApplication app) {
		this.app = app;
		//removeUploadedFileInfos();
	}

	@Nullable
	private SQLiteConnection openConnection(boolean readonly) {
		SQLiteConnection conn = app.getSQLiteAPI().getOrCreateDatabase(DB_NAME, readonly);
		if (conn != null && conn.getVersion() < DB_VERSION) {
			if (readonly) {
				conn.close();
				conn = app.getSQLiteAPI().getOrCreateDatabase(DB_NAME, false);
			}
			if (conn != null) {
				int version = conn.getVersion();
				if (version == 0) {
					onCreate(conn);
				} else {
					onUpgrade(conn, version, DB_VERSION);
				}
				conn.setVersion(DB_VERSION);
			}
		}
		return conn;
	}

	public void onCreate(SQLiteConnection db) {
		db.execSQL(UPLOADED_FILES_TABLE_CREATE);
		db.execSQL(LAST_MODIFIED_TABLE_CREATE);
	}

	public void onUpgrade(SQLiteConnection db, int oldVersion, int newVersion) {
		if (oldVersion < 2) {
			db.execSQL(LAST_MODIFIED_TABLE_CREATE);
		}
	}

	public boolean removeUploadedFileInfo(@NonNull UploadedFileInfo info) {
		SQLiteConnection db = openConnection(false);
		if (db != null) {
			try {
				db.execSQL("DELETE FROM " + UPLOADED_FILES_TABLE_NAME + " WHERE " +
								UPLOADED_FILE_COL_TYPE + " = ? AND " + UPLOADED_FILE_COL_NAME + " = ?",
						new Object[]{info.type, info.name});
			} finally {
				db.close();
			}
			return true;
		}
		return false;
	}

	public boolean removeUploadedFileInfos() {
		SQLiteConnection db = openConnection(false);
		if (db != null) {
			try {
				db.execSQL("DELETE FROM " + UPLOADED_FILES_TABLE_NAME);
			} finally {
				db.close();
			}
			return true;
		}
		return false;
	}

	public boolean updateUploadedFileInfo(@NonNull UploadedFileInfo info) {
		SQLiteConnection db = openConnection(false);
		if (db != null) {
			try {
				db.execSQL(
						"UPDATE " + UPLOADED_FILES_TABLE_NAME + " SET " + UPLOADED_FILE_COL_UPLOAD_TIME + "= ? " +
								"WHERE " + UPLOADED_FILE_COL_TYPE + " = ? AND " + UPLOADED_FILE_COL_NAME + " = ?",
						new Object[]{info.uploadTime, info.type, info.name});
			} finally {
				db.close();
			}
			return true;
		}
		return false;
	}

	public boolean addUploadedFileInfo(@NonNull UploadedFileInfo info) {
		SQLiteConnection db = openConnection(false);
		if (db != null) {
			try {
				db.execSQL(
						"INSERT INTO " + UPLOADED_FILES_TABLE_NAME + " VALUES (?, ?, ?)",
						new Object[]{info.type, info.name, info.uploadTime});
			} finally {
				db.close();
			}
			return true;
		}
		return false;
	}

	@NonNull
	public List<UploadedFileInfo> getUploadedFileInfos() {
		List<UploadedFileInfo> infos = new ArrayList<>();
		SQLiteConnection db = openConnection(true);
		if (db != null) {
			try {
				SQLiteCursor query = db.rawQuery(
						"SELECT " + UPLOADED_FILE_COL_TYPE + ", " +
								UPLOADED_FILE_COL_NAME + ", " +
								UPLOADED_FILE_COL_UPLOAD_TIME +
								" FROM " + UPLOADED_FILES_TABLE_NAME, null);
				if (query != null && query.moveToFirst()) {
					do {
						UploadedFileInfo info = readUploadedFileInfo(query);
						infos.add(info);
					} while (query.moveToNext());
				}
				if (query != null) {
					query.close();
				}
			} finally {
				db.close();
			}
		}
		return infos;
	}

	@Nullable
	public UploadedFileInfo getUploadedFileInfo(@NonNull String type, @NonNull String name) {
		UploadedFileInfo info = null;
		SQLiteConnection db = openConnection(true);
		if (db != null) {
			try {
				SQLiteCursor query = db.rawQuery(
						"SELECT " + UPLOADED_FILE_COL_TYPE + ", " +
								UPLOADED_FILE_COL_NAME + ", " +
								UPLOADED_FILE_COL_UPLOAD_TIME +
								" FROM " + UPLOADED_FILES_TABLE_NAME +
								" WHERE " + UPLOADED_FILE_COL_TYPE + " = ? AND " +
								UPLOADED_FILE_COL_NAME + " = ?",
						new String[]{type, name});
				if (query != null && query.moveToFirst()) {
					info = readUploadedFileInfo(query);
				}
				if (query != null) {
					query.close();
				}
			} finally {
				db.close();
			}
		}
		return info;
	}

	@NonNull
	private UploadedFileInfo readUploadedFileInfo(SQLiteCursor query) {
		String type = query.getString(0);
		String name = query.getString(1);
		long uploadTime = query.getLong(2);
		return new UploadedFileInfo(type, name, uploadTime);
	}

	public void setLastModifiedTime(@NonNull String name, long lastModifiedTime) {
		SQLiteConnection db = openConnection(false);
		if (db != null) {
			try {
				db.execSQL("DELETE FROM " + LAST_MODIFIED_TABLE_NAME +
						" WHERE " + LAST_MODIFIED_COL_NAME + " = ?", new Object[]{name});
				db.execSQL("INSERT INTO " + LAST_MODIFIED_TABLE_NAME + " VALUES (?, ?)",
						new Object[]{name, lastModifiedTime});
			} finally {
				db.close();
			}
		}
	}

	public long getLastModifiedTime(@NonNull String name) {
		long res = 0;
		SQLiteConnection db = openConnection(true);
		if (db != null) {
			try {
				SQLiteCursor query = db.rawQuery(
						"SELECT " + LAST_MODIFIED_COL_MODIFIED_TIME +
								" FROM " + LAST_MODIFIED_TABLE_NAME +
								" WHERE " + LAST_MODIFIED_COL_NAME + " = ?",
						new String[]{name});
				if (query != null && query.moveToFirst()) {
					res = query.getLong(0);
				}
				if (query != null) {
					query.close();
				}
			} finally {
				db.close();
			}
		}
		return res;
	}
}
