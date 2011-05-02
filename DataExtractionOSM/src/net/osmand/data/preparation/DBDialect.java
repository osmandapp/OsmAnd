package net.osmand.data.preparation;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.logging.Log;

import net.osmand.Algoritms;

public enum DBDialect {
	DERBY,
	H2,
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
	
	public void removeDatabase(File file)  {
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
	
	protected Connection getDatabaseConnection(String fileName, Log log) throws SQLException {
		if (DBDialect.SQLITE == this) {
			try {
				Class.forName("org.sqlite.JDBC");
			} catch (ClassNotFoundException e) {
				log.error("Illegal configuration", e);
				throw new IllegalStateException(e);
			}
			Connection connection = DriverManager.getConnection("jdbc:sqlite:" + fileName);
			Statement statement = connection.createStatement();
			statement.executeUpdate("PRAGMA synchronous = 0");
			statement.close();
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
