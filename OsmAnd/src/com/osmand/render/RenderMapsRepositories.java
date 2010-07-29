package com.osmand.render;

import java.io.File;

import org.apache.commons.logging.Log;

import android.database.sqlite.SQLiteDatabase;

import com.osmand.IProgress;
import com.osmand.LogUtil;
import com.osmand.data.index.IndexConstants;

public class RenderMapsRepositories {
	
	private final static Log log = LogUtil.getLog(RenderMapsRepositories.class);
	private SQLiteDatabase db;
	private double cTopLatitude;
	private double cBottomLatitude;
	private int cZoom;
	private double cLeftLongitude;
	private double cRightLongitude;


	public boolean initializeNewResource(final IProgress progress, File file) {
		long start = System.currentTimeMillis();
		// TODO should support multiple db
		if(db != null){
			// close previous db
			db.close();
		}
		db = SQLiteDatabase.openOrCreateDatabase(file, null);
		if(db.getVersion() != IndexConstants.MAP_TABLE_VERSION){
			db.close();
			db = null;
			return false;
		}
		if (log.isDebugEnabled()) {
			log.debug("Initializing db " + file.getAbsolutePath() + " " + (System.currentTimeMillis() - start) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		return true;
	}
	
	
	public void clearAllResources(){
		if(db != null){
			// close previous db
			db.close();
			db = null;
		}
	}
	
	/**
	 * @return true if no need to reevaluate map
	 */
	public boolean updateMap(double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude, int zoom){
		if (db == null) {
			return true;
		}
		boolean inside = cTopLatitude >= topLatitude && cLeftLongitude <= leftLongitude && cRightLongitude >= rightLongitude
				&& cBottomLatitude <= bottomLatitude && cZoom == zoom;
		return inside;
	}

	
	public void loadMap(double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude, int zoom){
		cTopLatitude = topLatitude + (topLatitude - bottomLatitude);
		cBottomLatitude = bottomLatitude - (topLatitude -bottomLatitude);
		cLeftLongitude = leftLongitude - (rightLongitude - leftLongitude);
		cRightLongitude = rightLongitude + (rightLongitude - leftLongitude);
		cZoom = zoom;
		// TODO clear cache
		loadingData();
		// after prepare images
	}
	
	private void loadingData(){
		
	}
	

	public void clearCache() {
		
	}

}
