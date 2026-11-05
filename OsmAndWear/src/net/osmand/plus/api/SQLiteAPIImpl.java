package net.osmand.plus.api;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;

import org.apache.commons.logging.Log;

public class SQLiteAPIImpl implements SQLiteAPI {

	private final OsmandApplication app;
	private static final Log LOG = PlatformUtil.getLog(SQLiteAPIImpl.class);

	public SQLiteAPIImpl(OsmandApplication app) {
		this.app = app;
	}

	@SuppressLint("InlinedApi")
	@Nullable
	@Override
	public SQLiteConnection getOrCreateDatabase(String name, boolean readOnly) {
		android.database.sqlite.SQLiteDatabase db = null;
		try {
			db = app.openOrCreateDatabase(name, Context.MODE_PRIVATE
					| (readOnly ? 0 : Context.MODE_ENABLE_WRITE_AHEAD_LOGGING), null);
		} catch (RuntimeException e) {
			LOG.error(e.getMessage(), e);
		}
		if (db == null) {
			return null;
		}
		return new SQLiteDatabaseWrapper(db);
	}

	public class SQLiteDatabaseWrapper implements SQLiteConnection {
		android.database.sqlite.SQLiteDatabase ds;

		
		public SQLiteDatabaseWrapper(android.database.sqlite.SQLiteDatabase ds) {
			this.ds = ds;
		}

		@Override
		public int getVersion() {
			return ds.getVersion();
		}

		@Override
		public void close() {
			ds.close();
			
		}

		@Override
		public SQLiteCursor rawQuery(String sql, String[] selectionArgs) {
			Cursor c = ds.rawQuery(sql, selectionArgs);
			if(c == null) {
				return null;
			}
			return new SQLiteCursor() {
				
				@Override
				public boolean moveToNext() {
					return c.moveToNext();
				}
				
				@Override
				public String[] getColumnNames() {
					return c.getColumnNames();
				}
				
				@Override
				public boolean moveToFirst() {
					return c.moveToFirst();
				}
				
				@Override
				public String getString(int ind) {
					return c.getString(ind);
				}
				
				@Override
				public void close() {
					c.close();
				}
				
				public boolean isNull(int ind) {
					return c.isNull(ind);
				}

				@Override
				public int getColumnIndex(String columnName) {
					return c.getColumnIndex(columnName);
				}


				@Override
				public double getDouble(int ind) {
					return c.getDouble(ind);
				}
				

				@Override
				public long getLong(int ind) {
					return c.getLong(ind);
				}

				@Override
				public int getInt(int ind) {
					return c.getInt(ind);
				}

				@Override
				public byte[] getBlob(int ind) {
					return c.getBlob(ind);
				}
			};
		}

		@Override
		public void execSQL(String query) {
			ds.execSQL(query);			
		}

		@Override
		public void execSQL(String query, Object[] objects) {
			ds.execSQL(query, objects);
		}

		@Override
		public SQLiteStatement compileStatement(String query) {
			android.database.sqlite.SQLiteStatement st = ds.compileStatement(query);
			if(st == null) {
				return null;
			}
			return new SQLiteStatement() {
				
				@Override
				public void execute() {
					st.execute();
					
				}
				
				@Override
				public void close() {
					st.close();
				}
				
				@Override
				public void bindString(int i, String value) {
					st.bindString(i, value);
					
				}
				
				@Override
				public void bindNull(int i) {
					st.bindNull(i);
				}

				@Override
				public long simpleQueryForLong() {
					return st.simpleQueryForLong();
				}

				@Override
				public String simpleQueryForString() {
					return st.simpleQueryForString();
				}

				@Override
				public void bindLong(int i, long val) {
					st.bindLong(i, val);
				}

				@Override
				public void bindBlob(int i, byte[] val) {
					st.bindBlob(i, val);
				}
			};
		}

		@Override
		public void setVersion(int newVersion) {
			ds.setVersion(newVersion);
		}

		@Override
		public boolean isReadOnly() {
			return ds.isReadOnly();
		}

		@Override
		public boolean isClosed() {
			return !ds.isOpen();
		}
		
	}

	@Nullable
	@Override
	public SQLiteConnection openByAbsolutePath(String path, boolean readOnly) {
		// fix http://stackoverflow.com/questions/26937152/workaround-for-nexus-9-sqlite-file-write-operations-on-external-dirs
		android.database.sqlite.SQLiteDatabase db = SQLiteDatabase.openDatabase(path, null,
				readOnly ? SQLiteDatabase.OPEN_READONLY : (SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.ENABLE_WRITE_AHEAD_LOGGING));
		if (db == null) {
			return null;
		}
		return new SQLiteDatabaseWrapper(db);
	}
}
