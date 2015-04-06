package net.osmand.plus.api;



public interface SQLiteAPI {

	public interface SQLiteConnection {
		
		void close();
		
		SQLiteCursor rawQuery(String sql, String[] selectionArgs);

		void execSQL(String query);
		
		void execSQL(String query, Object[] objects);

		SQLiteStatement compileStatement(String string);

		void setVersion(int newVersion);
		
		int getVersion();

		boolean isReadOnly();

		boolean isDbLockedByOtherThreads();

		boolean isClosed();

		void beginTransaction();
		
		void endTransaction();
	}
	
	public interface SQLiteCursor {
		
		String[] getColumnNames();

		boolean moveToFirst();
		
		boolean moveToNext();

		/**
		 * Takes parameter value (zero based)
		 */
		String getString(int ind);

		double getDouble(int ind);
		
		long getLong(int ind);
		
		long getInt(int ind);
		
		byte[] getBlob(int ind);
		
		void close();

		
		
	}
	
	public interface SQLiteStatement {

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
		
		void clearBindings();

	}
	
	public SQLiteConnection getOrCreateDatabase(String name, boolean readOnly);
	
	public SQLiteConnection openByAbsolutePath(String path, boolean readOnly);
}
