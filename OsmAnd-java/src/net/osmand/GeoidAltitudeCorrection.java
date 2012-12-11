package net.osmand;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.apache.commons.logging.Log;

public class GeoidAltitudeCorrection {

	private final Log log = LogUtil.getLog(GeoidAltitudeCorrection.class);
	private File f;
	private RandomAccessFile rf;
	
	private int cachedPointer = -1;
	private short cachedValue = 0;

	public GeoidAltitudeCorrection(File dir) {
		String[] fnames = dir.list();
		if(fnames != null) {
			String fn = null;
			for(String f : fnames) {
				if(f.contains("WW15MGH")) {
					fn = f;
					break;
				}
			}
			if (fn != null) {
				this.f = new File(dir, fn);
				if (f.exists()) {
					try {
						rf = new RandomAccessFile(f, "r");
					} catch (FileNotFoundException e) {
						log.error("Error", e);
					}
				}
			}
		}
		
	}
	
	public boolean isGeoidInformationAvailable(){
		return rf != null;
	}
	
	public float getGeoidHeight(double lat, double lon) {
		if (!isGeoidInformationAvailable()) {
			return 0;
		}
		int shy = (int) Math.floor((90 - lat) * 4);
		int shx = (int) Math.floor((lon >= 0 ? lon : lon + 360) * 4);
		int pointer = ((shy * 1440) + shx) * 2;
		short res = 0;
		if (pointer != cachedPointer) {
			try {
				rf.seek(pointer);
				cachedValue = rf.readShort();
				cachedPointer = pointer;
			} catch (IOException e) {
				log.error("Geoid info error", e);
			}
		}
		res = cachedValue;
		return res / 100f;
	}
}
