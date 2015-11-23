package net.osmand.plus.monitoring;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;

import org.apache.commons.logging.Log;

import android.content.Context;
import android.os.AsyncTask;

public class LiveMonitoringHelper  {
	
	protected Context ctx;
	private OsmandSettings settings;
	private long lastTimeUpdated;
	private final static Log log = PlatformUtil.getLog(LiveMonitoringHelper.class); 

	public LiveMonitoringHelper(Context ctx){
		this.ctx = ctx;
		settings = ((OsmandApplication) ctx.getApplicationContext()).getSettings();
	}
	
	public boolean isLiveMonitoringEnabled(){
		return settings.LIVE_MONITORING.get() && (settings.SAVE_TRACK_TO_GPX.get() || settings.SAVE_GLOBAL_TRACK_TO_GPX.get());
	}
	
	public void updateLocation(net.osmand.Location location) {
		if (OsmAndLocationProvider.isPointAccurateForRouting(location) && isLiveMonitoringEnabled()
				&& OsmAndLocationProvider.isNotSimulatedLocation(location)
				&& OsmandPlugin.getEnabledPlugin(OsmandMonitoringPlugin.class) != null) {
			long locationTime = System.currentTimeMillis();
			if (locationTime - lastTimeUpdated > settings.LIVE_MONITORING_INTERVAL.get()) {
				LiveMonitoringData data = new LiveMonitoringData((float)location.getLatitude(), (float)location.getLongitude(),
						(float)location.getAltitude(), location.getSpeed(), location.getAccuracy(), location.getBearing(), locationTime);
				new LiveSender().execute(data);
				lastTimeUpdated = locationTime;
			}
		}
		
	}
	
	
	private static class LiveMonitoringData {

		private final float lat;
		private final float lon;
		private final float alt;
		private final float speed;
		private final float bearing;
		private final float hdop;
		private final long time;

		public LiveMonitoringData(float lat, float lon, float alt, float speed, float hdop, float bearing, long time) {
			this.lat = lat;
			this.lon = lon;
			this.alt = alt;
			this.speed = speed;
			this.hdop = hdop;
			this.time = time;
			this.bearing = bearing;
		}
		
	}
	
	private class LiveSender extends AsyncTask<LiveMonitoringData, Void, Void> {

		@Override
		protected Void doInBackground(LiveMonitoringData... params) {
			for(LiveMonitoringData d : params){
				sendData(d);
			}
			return null;
		}
		
	}

	public void sendData(LiveMonitoringData data) {
		String st = settings.LIVE_MONITORING_URL.get();
		List<String> prm = new ArrayList<String>();
		int maxLen = 0;
		for(int i = 0; i < 7; i++) {
			boolean b = st.contains("{"+i+"}");
			if(b) {
				maxLen = i;
			}
		}
		for (int i = 0; i < maxLen + 1; i++) {
			switch (i) {
			case 0:
				prm.add(data.lat + "");
				break;
			case 1:
				prm.add(data.lon + "");
				break;
			case 2:
				prm.add(data.time + "");
				break;
			case 3:
				prm.add(data.hdop + "");
				break;
			case 4:
				prm.add(data.alt + "");
				break;
			case 5:
				prm.add(data.speed + "");
				break;
			case 6:
				prm.add(data.bearing + "");
				break;

			default:
				break;
			}
		}
		String urlStr = MessageFormat.format(st, prm.toArray());
		try {

			// Parse the URL and let the URI constructor handle proper encoding of special characters such as spaces
			URL url = new URL(urlStr);
			HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
			URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(),
						url.getPath(), url.getQuery(), url.getRef());

			urlConnection.setConnectTimeout(15000);
			urlConnection.setReadTimeout(15000);

			// allow certificates where hostnames doesn't match CN
			if (url.getProtocol() == "https") {
				((HttpsURLConnection) urlConnection).setHostnameVerifier(
						new HostnameVerifier() {
							public boolean verify(String host, SSLSession session) {
								return (true);
							}
						});
			}

			log.info("Monitor " + uri);

			if (urlConnection.getResponseCode() != 200) {

				String msg = urlConnection.getResponseCode() + " : " + //$NON-NLS-1$//$NON-NLS-2$
						urlConnection.getResponseMessage();
				log.error("Error sending monitor request: " +  msg);
			} else {
				InputStream is = urlConnection.getInputStream();
				StringBuilder responseBody = new StringBuilder();
				if (is != null) {
					BufferedReader in = new BufferedReader(new InputStreamReader(is, "UTF-8")); //$NON-NLS-1$
					String s;
					while ((s = in.readLine()) != null) {
						responseBody.append(s);
						responseBody.append("\n"); //$NON-NLS-1$
					}
					is.close();
				}
				log.info("Monitor response (" + urlConnection.getHeaderField("Content-Type") + "): " + responseBody.toString());
			}

			urlConnection.disconnect();
			
		} catch (Exception e) {
			log.error("Failed connect to " + urlStr + ": " + e.getMessage(), e);
		}
	}
}
