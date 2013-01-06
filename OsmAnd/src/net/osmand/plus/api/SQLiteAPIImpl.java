package net.osmand.plus.api;

import net.osmand.plus.OsmandApplication;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class SQLiteAPIImpl implements SQLiteAPI {

	private OsmandApplication app;

	public SQLiteAPIImpl(OsmandApplication app) {
		this.app = app;
	}

	@Override
	public SQLiteConnection getOrCreateDatabase(String name, boolean readOnly) {
		android.database.sqlite.SQLiteDatabase db = app.openOrCreateDatabase(name,
				readOnly? SQLiteDatabase.OPEN_READONLY : SQLiteDatabase.OPEN_READWRITE, null);
		if(db == null) {
			return null;
		}
		return new SQLiteDatabaseWrapper(db) ;
	}
	
	
	public class SQLiteDatabaseWrapper implements SQLiteConnection {
		android.database.sqlite.SQLiteDatabase ds;

		
		public SQLiteDatabaseWrapper(android.database.sqlite.SQLiteDatabase ds) {
			super();
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
			final Cursor c = ds.rawQuery(sql, selectionArgs);
			if(c == null) {
				return null;
			}
			return new SQLiteCursor() {
				
				@Override
				public boolean moveToNext() {
					return c.moveToNext();
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

				@Override
				public double getDouble(int ind) {
					return c.getDouble(ind);
				}

				@Override
				public long getLong(int ind) {
					return c.getLong(ind);
				}

				@Override
				public long getInt(int ind) {
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
			final android.database.sqlite.SQLiteStatement st = ds.compileStatement(query);
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
		public boolean isDbLockedByOtherThreads() {
			return ds.isDbLockedByOtherThreads();
		}

		@Override
		public boolean isClosed() {
			return !ds.isOpen();
		}
		
	}


	@Override
	public SQLiteConnection openByAbsolutePath(String path, boolean readOnly) {
		android.database.sqlite.SQLiteDatabase db = SQLiteDatabase.openDatabase(path, null,
				readOnly? SQLiteDatabase.OPEN_READONLY : SQLiteDatabase.OPEN_READWRITE);
		if(db == null) {
			return null;
		}
		return new SQLiteDatabaseWrapper(db) ;
	}
}
