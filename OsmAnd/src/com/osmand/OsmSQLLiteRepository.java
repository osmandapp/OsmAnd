package com.osmand;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.osmand.data.Amenity;
import com.osmand.data.Amenity.AmenityType;

public class OsmSQLLiteRepository {
	private static final Log log = LogUtil.getLog(OsmSQLLiteRepository.class);
	
	private SQLiteDatabase db;

	private List<Amenity> cachedAmenities = null;
	private double cTopLatitude;
	private double cBottomLatitude;
	private double cLeftLongitude;
	private double cRightLongitude;
	
	private boolean isLoading = false;
	
	protected synchronized void loadAmenitiesInAnotherThread(final double topLatitude, final double leftLongitude, final double bottomLatitude, final double rightLongitude){
		isLoading = true;
		new Thread(new Runnable(){

			@Override
			public void run() {
				try {
					cachedAmenities = internalSearch(topLatitude, leftLongitude, bottomLatitude, rightLongitude);
					cTopLatitude = topLatitude;
					cLeftLongitude = leftLongitude;
					cBottomLatitude = bottomLatitude ;
					cRightLongitude = rightLongitude;
				} finally {
					synchronized (this) {
						isLoading = false;
					}
				}
			}
		}, "Searching in index...").start();
	}
	
	
	private List<Amenity> internalSearch(double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude){
		long now = System.currentTimeMillis();
		Cursor query = db.query("poi", null, "? < latitude AND latitude < ? AND ? < longitude AND longitude < ?", 
				new String[]{Double.toString(bottomLatitude), 
				Double.toString(topLatitude), Double.toString(leftLongitude), Double.toString(rightLongitude)}, null, null, null);
		List<Amenity> amenities = new ArrayList<Amenity>();
		int idIndex = query.getColumnIndex("id");
		int latitudeIndex = query.getColumnIndex("latitude");
		int longitudeIndex = query.getColumnIndex("longitude");
		int nameIndex = query.getColumnIndex("name");
		int typeIndex = query.getColumnIndex("type");
		int subtypeIndex = query.getColumnIndex("subtype");
		if(query.moveToFirst()){
			do {
				Amenity am = new Amenity();
				if(idIndex != -1){
					am.setId(query.getLong(idIndex));
				}
				if(latitudeIndex != -1 && longitudeIndex != -1){
					am.setLocation(query.getDouble(latitudeIndex), query.getDouble(longitudeIndex));
				}
				if(nameIndex != -1){
					am.setName(query.getString(nameIndex));
				}
				if(typeIndex != -1){
					am.setType(AmenityType.fromString(query.getString(typeIndex)));
				}
				if(subtypeIndex != -1){
					am.setSubType(query.getString(subtypeIndex));
				}
				amenities.add(am);
			} while(query.moveToNext());
		}
		query.deactivate();
		
		if (log.isDebugEnabled()) {
			log.debug(String.format("Search for %s done in %s ms found %s.", 
					topLatitude + " " + leftLongitude, System.currentTimeMillis() - now, amenities.size()));
		}
		return amenities;
	}
	public synchronized List<Amenity> searchAmenities(double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude) {
		if (db == null) {
			return Collections.emptyList();
		}
		if (cTopLatitude >= topLatitude && cLeftLongitude <= leftLongitude && cRightLongitude >= rightLongitude
				&& cBottomLatitude <= bottomLatitude) {
			return cachedAmenities;
		}
		if(!isLoading){
			double h = (topLatitude - bottomLatitude);
			double w = (rightLongitude - leftLongitude);
			topLatitude += h;
			leftLongitude -= w;
			bottomLatitude -= h;
			rightLongitude += w;
			loadAmenitiesInAnotherThread(topLatitude, leftLongitude, bottomLatitude, rightLongitude);
		}
		return Collections.emptyList();
	}

	public void initialize(final IProgress progress, File file) {
		long start = System.currentTimeMillis();
		db = SQLiteDatabase.openOrCreateDatabase(file, null);
		if (log.isDebugEnabled()) {
			log.debug("Initializing db " + file.getAbsolutePath() + " " + (System.currentTimeMillis() - start) + "ms");
		}
	}


	
}
