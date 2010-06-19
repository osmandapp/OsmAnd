package com.osmand.activities;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlSerializer;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.text.format.DateFormat;
import android.util.Xml;
import android.widget.Toast;

import com.osmand.LogUtil;
import com.osmand.OsmandSettings;
import com.osmand.Version;

public class SavingTrackHelper extends SQLiteOpenHelper {
	public final static String TRACKS_PATH = "tracks";
	
	public final static String DATABASE_NAME = "tracks";
	public final static int DATABASE_VERSION = 1;
	
	public final static String TRACK_NAME = "track";
	public final static String TRACK_COL_DATE = "date";
	public final static String TRACK_COL_LAT = "lat";
	public final static String TRACK_COL_LON = "lon";
	public final static String TRACK_COL_ALTITUDE = "altitude";
	public final static String TRACK_COL_SPEED = "speed";
	
	public final static Log log = LogUtil.getLog(SavingTrackHelper.class);
	

	private String updateScript;
	private long lastTimeUpdated = 0;
	private final Context ctx;
	
	public SavingTrackHelper(Context ctx){
		super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
		this.ctx = ctx;
		updateScript = "INSERT INTO " + TRACK_NAME + " VALUES (?, ?, ?, ?, ?)";
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE " + TRACK_NAME+ " ("+TRACK_COL_LAT +" double, " + TRACK_COL_LON+" double, "  
				+ TRACK_COL_ALTITUDE+" double, " + TRACK_COL_SPEED+" double, " 
				+ TRACK_COL_DATE +" long )" );
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	}
	
	private static class TrkPt {
		public double lat;
		public double lon;
		public double ele;
		public double speed;
		public long time;
	}
	
	
	protected void saveToXMLFiles(File dir, Map<String, List<List<TrkPt>>> data ){
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		try {
			for (String f : data.keySet()) {
				File fout = new File(dir, f + ".gpx");
				int ind = 1;
				while(fout.exists()){
					fout = new File(dir, f + "_"+(++ind)+".gpx");
				}
				FileOutputStream output = new FileOutputStream(fout);
				XmlSerializer serializer = Xml.newSerializer();
				serializer.setOutput(output, "UTF-8");
				serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
				serializer.startDocument("UTF-8", true);
				serializer.startTag(null, "gpx");
				serializer.attribute(null, "version", "1.1");
				serializer.attribute(null, "creator", Version.APP_NAME_VERSION);
				serializer.attribute("xmlns", "xsi", "http://www.w3.org/2001/XMLSchema-instance");
				serializer.attribute("xsi", "schemaLocation", "http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd");
				serializer.attribute(null, "xmlns", "http://www.topografix.com/GPX/1/1");
				
				serializer.startTag(null, "trk");
				for(List<TrkPt> l : data.get(f)){
					serializer.startTag(null, "trkseg");
					for(TrkPt p : l){
						serializer.startTag(null, "trkpt");
						serializer.attribute(null, "lat", p.lat+"");
						serializer.attribute(null, "lon", p.lon+"");
						serializer.startTag(null, "time");
						serializer.text(format.format(new Date(p.time)));
						serializer.endTag(null, "time");
						serializer.startTag(null, "ele");
						serializer.text(p.ele+"");
						serializer.endTag(null, "ele");
						if (p.speed > 0) {
							serializer.startTag(null, "speed");
							serializer.text(p.speed + "");
							serializer.endTag(null, "speed");
						}
						
						serializer.endTag(null, "trkpt");
					}
					serializer.endTag(null, "trkseg");
				}
				serializer.endTag(null, "trk");
				
				serializer.endTag(null, "gpx");
				serializer.flush();
				serializer.endDocument();
				
				
			}
		} catch (RuntimeException e) {
			log.error("Error saving gpx");
			Toast.makeText(ctx, "Exception occurred while saving gpx", Toast.LENGTH_LONG);
		} catch (IOException e) {
			log.error("Error saving gpx");
			Toast.makeText(ctx, "Exception occurred while saving gpx", Toast.LENGTH_LONG);
		}
	}
	
	public boolean hasDataToSave(){
		SQLiteDatabase db = getReadableDatabase();
		if(db != null){
			Cursor q = db.query(false, TRACK_NAME, new String[0], null, null, null, null, null, null);
			boolean m = q.moveToFirst();
			q.close();
			return m;
		}
		
		return false;
	}
	
	public void saveDataToGpx(){
		SQLiteDatabase db = getReadableDatabase();
		File file = Environment.getExternalStorageDirectory();
		if(db != null && file.canWrite()){
			file = new File(file, "/osmand/"+TRACKS_PATH);
			file.mkdirs();
			if (file.exists()) {
				Cursor query = db.rawQuery("SELECT " + TRACK_COL_LAT + "," + TRACK_COL_LON + "," + TRACK_COL_ALTITUDE + ","
						+ TRACK_COL_SPEED + "," + TRACK_COL_DATE + " FROM " + TRACK_NAME, null);
				long previousTime = 0;
				Map<String, List<List<TrkPt>>> data = new LinkedHashMap<String, List<List<TrkPt>>>();
				List<TrkPt> segment = new ArrayList<TrkPt>();
				List<List<TrkPt>> track = new ArrayList<List<TrkPt>>();
				track.add(segment);
				if (query.moveToFirst()) {
					do {
						TrkPt pt = new TrkPt();
						pt.lat = query.getDouble(0);
						pt.lon = query.getDouble(1);
						pt.ele = query.getDouble(2);
						pt.speed = query.getDouble(3);
						long time = query.getLong(4);
						pt.time = time;
						
						if (previousTime == 0) {
							data.put(DateFormat.format("yyyy-MM-dd", time).toString(), track);
							segment.add(pt);
						} else if (Math.abs(time - previousTime) < 60000) {
							// 1 hour - same segment
							segment.add(pt);
						} else if (Math.abs(time - previousTime) < 3600000) {
							// 1 hour - same track
							segment = new ArrayList<TrkPt>();
							segment.add(pt);
							track.add(segment);
						} else {
							// check day (possibly better create new track (not new segment)
							String date = DateFormat.format("yyyy-MM-dd", time).toString();
							if (data.containsKey(date)) {
								track = data.get(date);
							} else {
								track = new ArrayList<List<TrkPt>>();
								data.put(date, track);
							}
							segment = new ArrayList<TrkPt>();
							segment.add(pt);
							track.add(segment);
						}

						previousTime = time;
					} while (query.moveToNext());
				}
				query.close();
				saveToXMLFiles(file, data);
			}
		}
		
		db = getWritableDatabase();
		if(db != null){
//			Calendar cal = Calendar.getInstance();
//			cal.setTime(new java.util.Date());
//			cal.set(Calendar.HOUR_OF_DAY, 0);
//			cal.set(Calendar.MINUTE, 0);
//			cal.set(Calendar.SECOND, 0);
//			cal.set(Calendar.MILLISECOND, 0);
			// remove all from db
			db.execSQL("DELETE FROM " + TRACK_NAME+ " WHERE " + TRACK_COL_DATE + " <= ?", new Object[]{System.currentTimeMillis()});
		}
	}
	
	public void insertData(double lat, double lon, double alt, double speed, long time){
		if (time - lastTimeUpdated > OsmandSettings.getSavingTrackInterval(ctx)) {
			SQLiteDatabase db = getWritableDatabase();
			if (db != null) {
				db.execSQL(updateScript, new Object[] { lat, lon, alt, speed, time });
			}
			lastTimeUpdated = time;
		}
	}
	

}
