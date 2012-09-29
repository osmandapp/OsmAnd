package net.osmand.plus.activities;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import net.osmand.LogUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;

import org.apache.commons.logging.Log;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.content.Context;
import android.os.AsyncTask;

public class LiveMonitoringHelper  {
	
	protected Context ctx;
	private OsmandSettings settings;
	private long lastTimeUpdated;
	private final static Log log = LogUtil.getLog(LiveMonitoringHelper.class); 

	public LiveMonitoringHelper(Context ctx){
		this.ctx = ctx;
		settings = ((OsmandApplication) ctx.getApplicationContext()).getSettings();
	}
	
	public boolean isLiveMonitoringEnabled(){
		return settings.LIVE_MONITORING.get();
	}
	
	public void insertData(double lat, double lon, double alt, double speed, double hdop, long time, OsmandSettings settings){
		//* 1000 in next line seems to be wrong with new IntervalChooseDialog
		//if (time - lastTimeUpdated > settings.LIVE_MONITORING_INTERVAL.get() * 1000) {
		if (time - lastTimeUpdated > settings.LIVE_MONITORING_INTERVAL.get()) {
			LiveMonitoringData data = new LiveMonitoringData((float)lat, (float)lon,(float) alt,(float) speed,(float) hdop, time );
			new LiveSender().execute(data);
			lastTimeUpdated = time;
		}
	}
	
	private static class LiveMonitoringData {

		private final float lat;
		private final float lon;
		private final float alt;
		private final float speed;
		private final float hdop;
		private final long time;

		public LiveMonitoringData(float lat, float lon, float alt, float speed, float hdop, long time) {
			this.lat = lat;
			this.lon = lon;
			this.alt = alt;
			this.speed = speed;
			this.hdop = hdop;
			this.time = time;
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
		for(int i = 0; i < 6; i++) {
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

			default:
				break;
			}
		}
		String url = MessageFormat.format(st, prm.toArray());
		try {

			HttpParams params = new BasicHttpParams();
			HttpConnectionParams.setConnectionTimeout(params, 15000);
			DefaultHttpClient httpclient = new DefaultHttpClient(params);
			HttpRequestBase method = new HttpGet(url);
			log.info("Monitor " + url);
			HttpResponse response = httpclient.execute(method);
			
			if(response.getStatusLine() == null || 
				response.getStatusLine().getStatusCode() != 200){
				
				String msg;
				if(response.getStatusLine() != null){
					msg = ctx.getString(R.string.failed_op); //$NON-NLS-1$
				} else {
					msg = response.getStatusLine().getStatusCode() + " : " + //$NON-NLS-1$//$NON-NLS-2$
							response.getStatusLine().getReasonPhrase();
				}
				log.error("Error sending monitor request request : " +  msg);
			} else {
				InputStream is = response.getEntity().getContent();
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
				httpclient.getConnectionManager().shutdown();
				log.info("Montior response : " + responseBody.toString());
			}

			
		} catch (Exception e) {
			log.error("Failed connect to " + url, e);
		}
	}
}
