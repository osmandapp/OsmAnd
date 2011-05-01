package net.osmand.data.preparation;

import java.io.File;
import java.sql.SQLException;
import java.sql.Statement;

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
}
