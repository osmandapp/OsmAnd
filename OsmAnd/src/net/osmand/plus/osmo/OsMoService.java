package net.osmand.plus.osmo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Build;
import android.provider.Settings.Secure;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;

public class OsMoService {
	private OsMoThread thread;
	private List<OsMoSender> listSenders = new java.util.concurrent.CopyOnWriteArrayList<OsMoSender>();
	private List<OsMoReactor> listReactors = new java.util.concurrent.CopyOnWriteArrayList<OsMoReactor>();
	private OsmandApplication app;
	private static final Log log = PlatformUtil.getLog(OsMoService.class);
	
	public interface OsMoSender {
		
		public String nextSendCommand(OsMoThread tracker);
	}
	
	public interface OsMoReactor {
		
		public boolean acceptCommand(String command, String data, JSONObject obj, OsMoThread tracker);
		
	}
	
	public OsMoService(OsmandApplication app) {
		this.app = app;
	}
	
	public boolean isConnected() {
		return thread != null && thread.isConnected();
	}
	
	public boolean connect(boolean forceReconnect) {
		if(thread != null) {
			if(!forceReconnect ) {
				return isConnected();
			}
			thread.stopConnection();
		}
		thread = new OsMoThread(this, listSenders, listReactors);
		return true;
	}
	
	public void disconnect() {
		if(thread != null) {
			thread.stopConnection();
		}
	}
	
	public void registerSender(OsMoSender sender) {
		if(!listSenders.contains(sender)) {
			listSenders.add(sender);
		}
	}
	
	public void registerReactor(OsMoReactor reactor) {
		if(!listReactors.contains(reactor)) {
			listReactors.add(reactor);
		}
	}

	public void removeSender(OsMoSender s) {
		listSenders.remove(s);
	}
	
	public void removeReactor(OsMoReactor s) {
		listReactors.remove(s);
	}
	

	public Exception registerOsmoDeviceKey() {
		HttpClient httpclient = new DefaultHttpClient();
		HttpPost httppost = new HttpPost("http://api.osmo.mobi/auth");
		try {
			// Add your data
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
			nameValuePairs.add(new BasicNameValuePair("android_id", Secure.ANDROID_ID));
			nameValuePairs.add(new BasicNameValuePair("android_model", Build.MODEL));
			nameValuePairs.add(new BasicNameValuePair("imei", "0"));
			nameValuePairs.add(new BasicNameValuePair("android_product", Build.PRODUCT));
			nameValuePairs.add(new BasicNameValuePair("osmand", Version.getFullVersion(app)));
			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

			// Execute HTTP Post Request
			HttpResponse response = httpclient.execute(httppost);
			InputStream cm = response.getEntity().getContent();
			BufferedReader reader = new BufferedReader(new InputStreamReader(cm));
			String r = reader.readLine();
			reader.close();
			log.info("Authorization key : " + r);
			final JSONObject obj = new JSONObject(r);
			if(obj.has("error")) {
				return new RuntimeException(obj.getString("error"));
			}
			app.getSettings().OSMO_DEVICE_KEY.set(obj.getString("key"));
			return null;
		} catch (ClientProtocolException e) {
			return e;
		} catch (IOException e) {
			return e;
		} catch (JSONException e) {
			return e;
		}
	}
	
	public static class SessionInfo {
		public String hostName;
		public String port;
		public String token;
	}
	
	
	public SessionInfo getSessionToken() throws IOException {
		String deviceKey = app.getSettings().OSMO_DEVICE_KEY.get();
		HttpClient httpclient = new DefaultHttpClient();
		HttpPost httppost = new HttpPost("http://api.osmo.mobi/prepare");
		try {
			// Add your data
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
			nameValuePairs.add(new BasicNameValuePair("key", deviceKey));
			nameValuePairs.add(new BasicNameValuePair("protocol", "1"));
			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

			// Execute HTTP Post Request
			HttpResponse response = httpclient.execute(httppost);
			InputStream cm = response.getEntity().getContent();
			BufferedReader reader = new BufferedReader(new InputStreamReader(cm));
			String r = reader.readLine();
			reader.close();
			log.info("Authorization key : " + r);
			final JSONObject obj = new JSONObject(r);
			if(obj.has("error")) {
				throw new RuntimeException(obj.getString("error"));
			}
			if(!obj.has("port")) {
				throw new RuntimeException("Port not specified");
			}
			if(!obj.has("address")) {
				throw new RuntimeException("Host name not specified");
			}
			if(!obj.has("token")) {
				throw new RuntimeException("Token not specified");
			}
			SessionInfo si = new SessionInfo();
			String a = obj.getString("address");
			int i = a.indexOf(':');
			si.hostName = a.substring(0, i);
			si.port = a.substring(i + 1);
			si.token = obj.getString("token");
			return si;
		} catch (ClientProtocolException e) {
			throw new IOException(e);
		} catch (IOException e) {
			throw e;
		} catch (JSONException e) {
			throw new IOException(e);
		}
	}

	public void showErrorMessage(String string) {
		app.showToastMessage(app.getString(R.string.osmo_io_error) +  string);		
	}
	
}
