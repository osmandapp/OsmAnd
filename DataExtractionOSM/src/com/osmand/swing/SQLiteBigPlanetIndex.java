package com.osmand.swing;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.logging.Log;

import com.osmand.LogUtil;

public class SQLiteBigPlanetIndex {
	private static final Log log = LogUtil.getLog(SQLiteBigPlanetIndex.class);
	
	private static final int BATCH_SIZE = 50;
	
	public static void createSQLiteDatabase(File dirWithTiles, String regionName, String tileSource) throws SQLException, IOException {
		long now = System.currentTimeMillis();
		try {
			Class.forName("org.sqlite.JDBC"); //$NON-NLS-1$
		} catch (ClassNotFoundException e) {
			log.error("Illegal configuration", e); //$NON-NLS-1$
			throw new IllegalStateException(e);
		}
		File fileToWrite = new File(dirWithTiles, regionName + "." + tileSource + ".sqlitedb");
		fileToWrite.delete();
		Connection conn = DriverManager.getConnection("jdbc:sqlite:" + fileToWrite.getAbsolutePath()); //$NON-NLS-1$
		Statement statement = conn.createStatement();
		statement.execute("CREATE TABLE tiles (x int, y int, z int, s int, image blob, PRIMARY KEY (x,y,z,s))");
		statement.execute("CREATE INDEX IND on tiles (x,y,z,s)");
		statement.execute("CREATE TABLE info(minzoom,maxzoom)");
		statement.execute("CREATE TABLE android_metadata (locale TEXT)");
		statement.close();

		conn.setAutoCommit(false);
		
		PreparedStatement pStatement = conn.prepareStatement("INSERT INTO tiles VALUES (?, ?, ?, ?, ?)");
		int ch = 0;
		int bufSize = 32 * 1024;
		byte[] buf = new byte[bufSize];
		int maxZoom = 17;
		int minZoom = 1;
		
		File rootDir = new File(dirWithTiles, tileSource);
		for(File z : rootDir.listFiles()){
			try {
				int zoom = Integer.parseInt(z.getName());
				for(File xDir : z.listFiles()){
					try {
						int x = Integer.parseInt(xDir.getName());
						for(File f : xDir.listFiles()){
							if(!f.isFile()){
								continue;
							}
							try {
								int i = f.getName().indexOf('.');
								int y = Integer.parseInt(f.getName().substring(0, i));
								buf = new byte[(int) f.length()]; 
								if(zoom > maxZoom){
									maxZoom = zoom;
								}
								if(zoom < minZoom){
									minZoom = zoom;
								}
								
								FileInputStream is = new FileInputStream(f);
								int l = is.read(buf);
								if (l > 0) {
									pStatement.setInt(1, x);
									pStatement.setInt(2, y);
									pStatement.setInt(3, zoom);
									pStatement.setInt(4, 0);
									pStatement.setBytes(5, buf);
									pStatement.addBatch();
									ch++;
									if (ch >= BATCH_SIZE) {
										pStatement.executeBatch();
										ch = 0;
									}
								}
							
							} catch (NumberFormatException e) {
							}
						}
					} catch (NumberFormatException e) {
					}
				}
				
			} catch (NumberFormatException e) {
			}
			
		}
		
		if (ch > 0) {
			pStatement.executeBatch();
			ch = 0;
		}
		
		pStatement.close();
		conn.commit();
		conn.close();
		log.info("Index created " + fileToWrite.getName() + " " + (System.currentTimeMillis() - now) + " ms");
	}

}
