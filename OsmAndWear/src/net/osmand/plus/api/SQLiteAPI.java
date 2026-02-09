package net.osmand.plus.api;


import androidx.annotation.Nullable;

public interface SQLiteAPI {

	interface SQLiteConnection {
		
		void close();
		
		SQLiteCursor rawQuery(String sql, String[] selectionArgs);

		void execSQL(String query);
		
		void execSQL(String query, Object[] objects);

		SQLiteStatement compileStatement(String string);

		void setVersion(int newVersion);
		
		int getVersion();

		boolean isReadOnly();

		boolean isClosed();

	}
	
	interface SQLiteCursor {
		
		String[] getColumnNames();

		boolean moveToFirst();
		
		boolean moveToNext();

		/**
		 * Takes parameter value (zero based)
		 */
		String getString(int ind);

		double getDouble(int ind);
		
		long getLong(int ind);
		
		int getInt(int ind);
		
		byte[] getBlob(int ind);
		
		boolean isNull(int ind);

		int getColumnIndex(String columnName);
		
		void close();

	}
	
	interface SQLiteStatement {

		// 1 based argument
		void bindString(int i, String filterId);

		// 1 based argument
		void bindNull(int i);

		void execute();

		void close();

		long simpleQueryForLong();

		String simpleQueryForString();

		void bindLong(int i, long val);
		
		void bindBlob(int i, byte[] val);

	}

	@Nullable
	SQLiteConnection getOrCreateDatabase(String name, boolean readOnly);

	@Nullable
	SQLiteConnection openByAbsolutePath(String path, boolean readOnly);
}
