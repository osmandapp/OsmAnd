package net.osmand.plus.activities;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.monitoring.OsmandMonitoringPlugin;

import org.apache.commons.logging.Log;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.apache.http.conn.scheme.LayeredSocketFactory;
import javax.net.ssl.HostnameVerifier;
import org.apache.http.conn.ssl.StrictHostnameVerifier;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSession;
import android.annotation.TargetApi;
import android.net.SSLCertificateSocketFactory;
import javax.net.ssl.SSLPeerUnverifiedException;
import java.io.IOException;
import android.os.Build;

import android.content.Context;
import android.os.AsyncTask;

public class LiveMonitoringHelper  {
	
	protected Context ctx;
	private OsmandSettings settings;
	private long lastTimeUpdated;
	/*private*/ final static Log log = PlatformUtil.getLog(LiveMonitoringHelper.class); 

	public LiveMonitoringHelper(Context ctx){
		this.ctx = ctx;
		settings = ((OsmandApplication) ctx.getApplicationContext()).getSettings();
	}
	
	public boolean isLiveMonitoringEnabled(){
		return settings.LIVE_MONITORING.get() ;
	}
	
	public void updateLocation(net.osmand.Location location) {
		if (OsmAndLocationProvider.isPointAccurateForRouting(location) && isLiveMonitoringEnabled()
				&& OsmandPlugin.getEnabledPlugin(OsmandMonitoringPlugin.class) != null) {
			long locationTime = System.currentTimeMillis();
			//* 1000 in next line seems to be wrong with new IntervalChooseDialog
			//if (time - lastTimeUpdated > settings.LIVE_MONITORING_INTERVAL.get() * 1000) {
			if (locationTime - lastTimeUpdated > settings.LIVE_MONITORING_INTERVAL.get()) {
				LiveMonitoringData data = new LiveMonitoringData((float)location.getLatitude(), (float)location.getLongitude(),
						(float) location.getAltitude(),(float) location.getSpeed(),(float) location.getAccuracy(), (float) location.getBearing(), locationTime );
				new LiveSender().execute(data);
				lastTimeUpdated = locationTime;
			}
		}
		
	}

    /*private*/ static final String TAG = "davdroid.SNISocketFactory";

    final static HostnameVerifier hostnameVerifier = new StrictHostnameVerifier();


// http://blog.dev001.net/post/67082904181/android-using-sni-and-tlsv1-2-with-apache-httpclient
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
	class TlsSniSocketFactory implements LayeredSocketFactory {
	

	    // Plain TCP/IP (layer below TLS)

	    @Override
	    public Socket connectSocket(Socket s, String host, int port, InetAddress localAddress, int localPort, HttpParams params) throws IOException {
		    return null;
	    }

	    @Override
	    public Socket createSocket() throws IOException {
		    return null;
	    }

	    @Override
	    public boolean isSecure(Socket s) throws IllegalArgumentException {
		    if (s instanceof SSLSocket)
		            return ((SSLSocket)s).isConnected();
		    return false;
	    }


	    // TLS layer

	    @Override
	    public Socket createSocket(Socket plainSocket, String host, int port, boolean autoClose) throws IOException, UnknownHostException {
		    if (autoClose) {
		            // we don't need the plainSocket
		            plainSocket.close();
		    }

		    // create and connect SSL socket, but don't do hostname/certificate verification yet
		    SSLCertificateSocketFactory sslSocketFactory = (SSLCertificateSocketFactory) SSLCertificateSocketFactory.getDefault(0);
		    SSLSocket ssl = (SSLSocket)sslSocketFactory.createSocket(InetAddress.getByName(host), port);

		    // enable TLSv1.1/1.2 if available
		    // (see https://github.com/rfc2822/davdroid/issues/229)
		    ssl.setEnabledProtocols(ssl.getSupportedProtocols());

		    // set up SNI before the handshake
		    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
		            log.info(/*TAG, */"Setting SNI hostname");
		            sslSocketFactory.setHostname(ssl, host);
		    } else {
		            log.debug(/*TAG, */"No documented SNI support on Android <4.2, trying with reflection");
		            try {
		                 java.lang.reflect.Method setHostnameMethod = ssl.getClass().getMethod("setHostname", String.class);
		                 //setHostnameMethod.invoke(ssl, host.getHostName());
				 setHostnameMethod.invoke(ssl, host);
		            } catch (Exception e) {
		                    log.warn(/*TAG, */"SNI not useable", e);
		            }
		    }

		    // verify hostname and certificate
		    SSLSession session = ssl.getSession();
		    if (!hostnameVerifier.verify(host, session))
		            throw new SSLPeerUnverifiedException("Cannot verify hostname: " + host);

		    log.info(/*TAG, */"Established " + session.getProtocol() + " connection with " + session.getPeerHost() +
		                    " using " + session.getCipherSuite());

		    return ssl;
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
		String url = MessageFormat.format(st, prm.toArray());
		try {
			HttpClient httpclient = null;
			HttpParams params = new BasicHttpParams();
			HttpConnectionParams.setConnectionTimeout(params, 15000);
			if (url.startsWith("https:")) {
				SchemeRegistry schemeRegistry = new SchemeRegistry();
//				schemeRegistry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));
				schemeRegistry.register(new Scheme("https", new TlsSniSocketFactory(), 443));
				SingleClientConnManager mgr = new SingleClientConnManager(params, schemeRegistry);
				httpclient = new DefaultHttpClient(mgr, params);
			} else {
				httpclient = new DefaultHttpClient(params);
			}
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
				log.info("Montior response : " + responseBody);
			}

			
		} catch (Exception e) {
			log.error("Failed connect to " + url, e);
		}
	}
}
