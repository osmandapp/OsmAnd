package net.osmand.data.preparation;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import net.osmand.Algoritms;

import org.apache.commons.logging.Log;
import org.sqlite.SQLiteJDBCLoader;

import com.anvisics.jleveldb.LevelDBAccess;
import com.anvisics.jleveldb.ext.DBAccessor;
import com.anvisics.jleveldb.ext.Options;
import com.anvisics.jleveldb.ext.Status;

public enum DBDialect {
	DERBY,
	H2,
	NOSQL,
	SQLITE;
	
	public void deleteTableIfExists(String table, Statement stat) throws SQLException {
		if(this == DERBY){
			try {
				stat.executeUpdate("drop table " + table); //$NON-NLS-1$
			} catch (SQLException e) {
				// ignore it
			}
		} else {
			stat.executeUpdate("drop table if exists " + table); //$NON-NLS-1$
		}
		
	}
	
	public boolean databaseFileExists(File dbFile) {
		if (DBDialect.H2 == this) {
			return new File(dbFile.getAbsolutePath() + ".h2.db").exists(); //$NON-NLS-1$
		} else {
			return dbFile.exists();
		}
	}
	
	public void removeDatabase(File file) {
		if (DBDialect.H2 == this) {
			File[] list = file.getParentFile().listFiles();
			for (File f : list) {
				if (f.getName().startsWith(file.getName())) {
					Algoritms.removeAllFiles(f);
				}
			}
		} else {
			Algoritms.removeAllFiles(file);
		}
	}
	
	public void commitDatabase(Object connection) throws SQLException {
		if(DBDialect.NOSQL != this){
			if (!((Connection) connection).getAutoCommit()) {
				((Connection) connection).commit();
			}
		} else {
			
		}
	}
	
	public void closeDatabase(Object dbConn) throws SQLException {
		if(DBDialect.NOSQL != this){
			if (DBDialect.H2 == this) {
				((Connection) dbConn).createStatement().execute("SHUTDOWN COMPACT"); //$NON-NLS-1$
			}
			((Connection) dbConn).close();
		} else {
//			((DBAccessor) dbConn).close();
		}
	}
	
	protected Object getDatabaseConnection(String fileName, Log log) throws SQLException {
		if (DBDialect.NOSQL == this) {
			DBAccessor dbAccessor = LevelDBAccess.getDBAcessor();
			Options opts = new Options();
			opts.setCreateIfMissing(true);
			Status status = dbAccessor.open(opts, fileName);
			if(!status.ok()){
				throw new SQLException(status.ToString());
			}
			return dbAccessor;
		} else if (DBDialect.SQLITE == this) {
			try {
				Class.forName("org.sqlite.JDBC");
			} catch (ClassNotFoundException e) {
				log.error("Illegal configuration", e);
				throw new IllegalStateException(e);
			}
			Connection connection = DriverManager.getConnection("jdbc:sqlite:" + fileName);
			Statement statement = connection.createStatement();
			statement.executeUpdate("PRAGMA synchronous = 0");
			//no journaling, saves some I/O access, but database can go corrupt
			statement.executeQuery("PRAGMA journal_mode = OFF");
			//we are exclusive, some speed increase ( no need to get and release logs
			statement.executeQuery("PRAGMA locking_mode = EXCLUSIVE");
			//increased cache_size, by default it is 2000 and we have quite huge files...
			//statement.executeUpdate("PRAGMA cache_size = 10000"); cache size could be probably contraproductive on slower disks?
			statement.close();
			System.out.println(String.format("SQLITE running in %s mode", SQLiteJDBCLoader.isNativeMode() ? "native" : "pure-java"));
			return connection;
		} else if (DBDialect.DERBY == this) {
			try {
				Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
			} catch (ClassNotFoundException e) {
				log.error("Illegal configuration", e);
				throw new IllegalStateException(e);
			}
			Connection conn = DriverManager.getConnection("jdbc:derby:" + fileName + ";create=true");
			conn.setAutoCommit(false);
			return conn;
		} else if (DBDialect.H2 == this) {
			try {
				Class.forName("org.h2.Driver");
			} catch (ClassNotFoundException e) {
				log.error("Illegal configuration", e);
				throw new IllegalStateException(e);
			}

			return DriverManager.getConnection("jdbc:h2:file:" + fileName);
		} else {
			throw new UnsupportedOperationException();
		}

	}
}
