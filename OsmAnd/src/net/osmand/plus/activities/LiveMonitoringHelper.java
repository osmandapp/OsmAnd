package net.osmand.plus.activities;

import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;

import org.apache.commons.logging.Log;

import net.osmand.LogUtil;
import net.osmand.plus.OsmandSettings;
import android.content.Context;

public class LiveMonitoringHelper  {
	
	protected Context ctx;
	private OsmandSettings settings;
	private long lastTimeUpdated;
	private final static Log log = LogUtil.getLog(LiveMonitoringHelper.class); 

	public LiveMonitoringHelper(Context ctx){
		this.ctx = ctx;
		settings = OsmandSettings.getOsmandSettings(ctx);
	}
	
	public boolean isLiveMonitoringEnabled(){
		return settings.LIVE_MONITORING.get();
	}
	
	public void insertData(double lat, double lon, double alt, double speed, double hdop, long time, OsmandSettings settings){
		if (time - lastTimeUpdated > settings.LIVE_MONITORING_INTERVAL.get() * 1000) {
			sendData((float)lat, (float)lon,(float) alt,(float) speed,(float) hdop, time );
			lastTimeUpdated = time;
		}
	}

	public void sendData(float lat, float lon, float alt, float speed, float hdop, long time) {
		String url = MessageFormat.format(settings.LIVE_MONITORING_URL.get(), lat, lon, time, hdop, alt, speed);
		try {
			URL curl = new URL(url);
			URLConnection conn = curl.openConnection();
			conn.setDoInput(false);
			conn.setDoOutput(false);
			conn.connect();
		} catch (Exception e) {
			log.error("Failed connect to " + url, e);
		}
	}
}
