package net.osmand.plus.backup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.api.SQLiteAPI.SQLiteConnection;
import net.osmand.plus.api.SQLiteAPI.SQLiteCursor;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BackupDbHelper {

	private final OsmandApplication app;

	private static final String DB_NAME = "backup_files";
	private static final int DB_VERSION = 3;
	private static final String UPLOADED_FILES_TABLE_NAME = "uploaded_files";
	private static final String UPLOADED_FILE_COL_TYPE = "type";
	private static final String UPLOADED_FILE_COL_NAME = "name";
	private static final String UPLOADED_FILE_COL_UPLOAD_TIME = "upload_time";
	private static final String UPLOADED_FILE_COL_MD5_DIGEST = "md5_digest";
	private static final String UPLOADED_FILES_TABLE_CREATE = "CREATE TABLE IF NOT EXISTS " + UPLOADED_FILES_TABLE_NAME + " (" +
			UPLOADED_FILE_COL_TYPE + " TEXT, " +
			UPLOADED_FILE_COL_NAME + " TEXT, " +
			UPLOADED_FILE_COL_UPLOAD_TIME + " long, " +
			UPLOADED_FILE_COL_MD5_DIGEST + " TEXT);";
	private static final String UPLOADED_FILES_INDEX_TYPE_NAME = "indexTypeName";

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
		private String md5Digest = "";

		public UploadedFileInfo(@NonNull String type, @NonNull String name) {
			this.type = type;
			this.name = name;
			this.uploadTime = 0;
		}

		public UploadedFileInfo(@NonNull String type, @NonNull String name, long uploadTime) {
			this.type = type;
			this.name = name;
			this.uploadTime = uploadTime;
		}

		public UploadedFileInfo(@NonNull String type, @NonNull String name, @NonNull String md5Digest) {
			this.type = type;
			this.name = name;
			this.md5Digest = md5Digest;
		}

		public UploadedFileInfo(@NonNull String type, @NonNull String name, long uploadTime, @NonNull String md5Digest) {
			this.type = type;
			this.name = name;
			this.uploadTime = uploadTime;
			this.md5Digest = md5Digest;
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

		@Nullable
		public String getMd5Digest() {
			return md5Digest;
		}

		public void setMd5Digest(@NonNull String md5Digest) {
			this.md5Digest = md5Digest;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			UploadedFileInfo that = (UploadedFileInfo) o;
			return type.equals(that.type) &&
					name.equals(that.name);
		}

		@Override
		public int hashCode() {
			return Algorithms.hash(type, name);
		}

		@NonNull
		@Override
		public String toString() {
			return "UploadedFileInfo{type=" + type + ", name=" + name + ", uploadTime=" + uploadTime + '}';
		}
	}

	public BackupDbHelper(@NonNull OsmandApplication app) {
		this.app = app;
	}

	@Nullable
	public SQLiteConnection openConnection(boolean readonly) {
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
		db.execSQL("CREATE INDEX IF NOT EXISTS " + UPLOADED_FILES_INDEX_TYPE_NAME + " ON " + UPLOADED_FILES_TABLE_NAME
				+ " (" + UPLOADED_FILE_COL_TYPE + ", " + UPLOADED_FILE_COL_NAME + ");");
		db.execSQL(LAST_MODIFIED_TABLE_CREATE);
	}

	public void onUpgrade(SQLiteConnection db, int oldVersion, int newVersion) {
		if (oldVersion < 2) {
			db.execSQL(LAST_MODIFIED_TABLE_CREATE);
		}
		if (oldVersion < 3) {
			db.execSQL("ALTER TABLE " + UPLOADED_FILES_TABLE_NAME + " ADD " + UPLOADED_FILE_COL_MD5_DIGEST + " TEXT");
		}
		db.execSQL("CREATE INDEX IF NOT EXISTS " + UPLOADED_FILES_INDEX_TYPE_NAME + " ON " + UPLOADED_FILES_TABLE_NAME
				+ " (" + UPLOADED_FILE_COL_TYPE + ", " + UPLOADED_FILE_COL_NAME + ");");
	}

	public boolean removeUploadedFileInfo(@NonNull UploadedFileInfo info) {
		SQLiteConnection db = openConnection(false);
		if (db != null) {
			try {
				db.execSQL("DELETE FROM " + UPLOADED_FILES_TABLE_NAME + " WHERE " +
								UPLOADED_FILE_COL_TYPE + " = ? AND " + UPLOADED_FILE_COL_NAME + " = ?",
						new Object[] {info.type, info.name});
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

	private void updateUploadedFileInfo(@NonNull SQLiteConnection db, @NonNull UploadedFileInfo info) {
		db.execSQL(
				"UPDATE " + UPLOADED_FILES_TABLE_NAME + " SET "
						+ UPLOADED_FILE_COL_UPLOAD_TIME + " = ?, "
						+ UPLOADED_FILE_COL_MD5_DIGEST + " = ? "
						+ "WHERE " + UPLOADED_FILE_COL_TYPE + " = ? AND " + UPLOADED_FILE_COL_NAME + " = ?",
				new Object[] {info.uploadTime, info.md5Digest, info.type, info.name});
	}

	private void addUploadedFileInfo(@NonNull SQLiteConnection db, @NonNull UploadedFileInfo info) {
		db.execSQL(
				"INSERT INTO " + UPLOADED_FILES_TABLE_NAME + "(" + UPLOADED_FILE_COL_TYPE + ", "
						+ UPLOADED_FILE_COL_NAME + ", " + UPLOADED_FILE_COL_UPLOAD_TIME + ", "
						+ UPLOADED_FILE_COL_MD5_DIGEST
						+ ") VALUES (?, ?, ?, ?)",
				new Object[] {info.type, info.name, info.uploadTime, info.md5Digest});
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
								UPLOADED_FILE_COL_UPLOAD_TIME + ", " +
								UPLOADED_FILE_COL_MD5_DIGEST +
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

	@NonNull
	public Map<String, UploadedFileInfo> getUploadedFileInfoMap() {
		Map<String, UploadedFileInfo> infoMap = new HashMap<>();
		SQLiteConnection db = openConnection(true);
		if (db != null) {
			try {
				SQLiteCursor query = db.rawQuery(
						"SELECT " + UPLOADED_FILE_COL_TYPE + ", " +
								UPLOADED_FILE_COL_NAME + ", " +
								UPLOADED_FILE_COL_UPLOAD_TIME + ", " +
								UPLOADED_FILE_COL_MD5_DIGEST +
								" FROM " + UPLOADED_FILES_TABLE_NAME, null);
				if (query != null && query.moveToFirst()) {
					do {
						UploadedFileInfo info = readUploadedFileInfo(query);
						infoMap.put(info.getType() + "___" + info.getName(), info);
					} while (query.moveToNext());
				}
				if (query != null) {
					query.close();
				}
			} finally {
				db.close();
			}
		}
		return infoMap;
	}

	@Nullable
	public UploadedFileInfo getUploadedFileInfo(@NonNull String type, @NonNull String name) {
		UploadedFileInfo info = null;
		SQLiteConnection db = openConnection(true);
		if (db != null) {
			try {
				info = getUploadedFileInfo(db, type, name);
			} finally {
				db.close();
			}
		}
		return info;
	}

	@Nullable
	public UploadedFileInfo getUploadedFileInfo(@NonNull SQLiteConnection db, @NonNull String type, @NonNull String name) {
		UploadedFileInfo info = null;
		SQLiteCursor query = db.rawQuery(
				"SELECT " + UPLOADED_FILE_COL_TYPE + ", " +
						UPLOADED_FILE_COL_NAME + ", " +
						UPLOADED_FILE_COL_UPLOAD_TIME + ", " +
						UPLOADED_FILE_COL_MD5_DIGEST +
						" FROM " + UPLOADED_FILES_TABLE_NAME +
						" WHERE " + UPLOADED_FILE_COL_TYPE + " = ? AND " +
						UPLOADED_FILE_COL_NAME + " = ?",
				new String[] {type, name});
		if (query != null && query.moveToFirst()) {
			info = readUploadedFileInfo(query);
		}
		if (query != null) {
			query.close();
		}
		return info;
	}

	public void updateFileUploadTime(@NonNull String type, @NonNull String fileName, long updateTime) {
		SQLiteConnection db = openConnection(true);
		if (db != null) {
			try {
				UploadedFileInfo info = getUploadedFileInfo(db, type, fileName);
				if (info != null) {
					info.setUploadTime(updateTime);
					updateUploadedFileInfo(db, info);
				} else {
					info = new UploadedFileInfo(type, fileName, updateTime);
					addUploadedFileInfo(db, info);
				}
			} finally {
				db.close();
			}
		}
	}

	public void updateFileMd5Digest(@NonNull String type, @NonNull String fileName, @NonNull String md5Digest) {
		SQLiteConnection db = openConnection(true);
		if (db != null) {
			try {
				UploadedFileInfo info = getUploadedFileInfo(db, type, fileName);
				if (info != null) {
					info.setMd5Digest(md5Digest);
					updateUploadedFileInfo(db, info);
				} else {
					info = new UploadedFileInfo(type, fileName, md5Digest);
					addUploadedFileInfo(db, info);
				}
			} finally {
				db.close();
			}
		}
	}

	@NonNull
	private UploadedFileInfo readUploadedFileInfo(SQLiteCursor query) {
		String type = query.getString(0);
		String name = query.getString(1);
		long uploadTime = query.getLong(2);
		String md5Digest = query.getString(3);
		return new UploadedFileInfo(type, name, uploadTime, md5Digest);
	}

	public void setLastModifiedTime(@NonNull String name, long lastModifiedTime) {
		SQLiteConnection db = openConnection(false);
		if (db != null) {
			try {
				db.execSQL("DELETE FROM " + LAST_MODIFIED_TABLE_NAME +
						" WHERE " + LAST_MODIFIED_COL_NAME + " = ?", new Object[] {name});
				db.execSQL("INSERT INTO " + LAST_MODIFIED_TABLE_NAME + "(" + LAST_MODIFIED_COL_NAME + ", "
								+ LAST_MODIFIED_COL_MODIFIED_TIME + ") VALUES (?, ?)",
						new Object[] {name, lastModifiedTime});
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
						new String[] {name});
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
