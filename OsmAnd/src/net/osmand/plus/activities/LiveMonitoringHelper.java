package net.osmand.plus.activities;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;

import net.osmand.LogUtil;
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
		String url = MessageFormat.format(settings.LIVE_MONITORING_URL.get(), lat+"", lon+"", time+"", hdop+"", alt+"", speed+"");
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
