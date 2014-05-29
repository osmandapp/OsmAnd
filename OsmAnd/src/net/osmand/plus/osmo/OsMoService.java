package net.osmand.plus.osmo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;

import org.apache.commons.logging.Log;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Build;
import android.provider.Settings.Secure;

public class OsMoService implements OsMoReactor {
	public static final String REGENERATE_CMD = "TRACKER_REGENERATE_ID";
	public static final String SIGN_IN_URL = "http://osmo.mobi/signin";
	private OsMoThread thread;
	private List<OsMoReactor> listReactors = new java.util.concurrent.CopyOnWriteArrayList<OsMoReactor>();
	private ConcurrentLinkedQueue<String> commands = new ConcurrentLinkedQueue<String>();
	private OsmandApplication app;
	private static final Log log = PlatformUtil.getLog(OsMoService.class);
	public static final String SHARE_TRACKER_URL = "http://m.osmo.mobi/connect/";
	public static final String SHARE_GROUP_URL = "http://m.osmo.mobi/join/";
	public static final String TRACK_URL = "http://test1342.osmo.mobi/u/";
	public static final String GROUP_URL = "http://test1342.osmo.mobi/g/";
	private String lastRegistrationError = null;  
	
	
	
	public OsMoService(OsmandApplication app) {
		this.app = app;
		listReactors.add(this);
	}
	
	public boolean isConnected() {
		return thread != null && thread.isConnected();
	}
	
	
	public boolean isActive() {
		return thread != null && thread.isActive();
	}
	
	public long getLastCommandTime() {
		if(isConnected()) {
			return thread.getLastCommandTime();
		}
		return 0;
	}
	
	public List<String> getHistoryOfCommands() {
		if(thread == null) {
			return Collections.emptyList();
		}
		return new ArrayList<String>(thread.getLast100Commands());
	}
	
	
	public long getConnectionTime() {
		return thread == null || !thread.isConnected() ? System.currentTimeMillis() : thread.getConnectionTime(); 
	}
	
	
	public String getLastRegistrationError() {
		return lastRegistrationError;
	}
	
	public boolean connect(boolean forceReconnect) {
		if(thread != null) {
			if(!forceReconnect ) {
				return isConnected();
			}
			thread.stopConnection();
		}
		thread = new OsMoThread(this, listReactors);
		return true;
	}
	
	public void disconnect() {
		if(thread != null) {
			thread.stopConnection();
		}
	}
	
	
	public void registerReactor(OsMoReactor reactor) {
		if(!listReactors.contains(reactor)) {
			listReactors.add(reactor);
		}
	}

	public void removeReactor(OsMoReactor s) {
		listReactors.remove(s);
	}
	

	public String registerOsmoDeviceKey() throws IOException {
		HttpClient httpclient = new DefaultHttpClient();
		HttpPost httppost = new HttpPost("http://api.osmo.mobi/auth");
		try {
			// Add your data
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
			nameValuePairs.add(new BasicNameValuePair("android_id",
					Secure.getString(app.getContentResolver(),
                            Secure.ANDROID_ID)));
			nameValuePairs.add(new BasicNameValuePair("android_model", Build.MODEL));
			nameValuePairs.add(new BasicNameValuePair("imei", "0"));
			nameValuePairs.add(new BasicNameValuePair("android_product", Build.PRODUCT));
			nameValuePairs.add(new BasicNameValuePair("client", Version.getFullVersion(app)));
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
				lastRegistrationError = obj.getString("error");
				throw new RuntimeException(obj.getString("error"));
			}
			app.getSettings().OSMO_DEVICE_KEY.set(obj.getString("key"));
			return obj.getString("key");
		} catch (JSONException e) {
			throw new IOException(e);
		}
	}
	
	public static class SessionInfo {
		public String hostName;
		public String port;
		public String token;
		// after auth
		public String protocol = "";
		public String groupTrackerId;
		public String trackerId;
		public String username;
		public long serverTimeDelta;
		public long motdTimestamp;
		
		public String motd = "";
	}
	
	public SessionInfo getCurrentSessionInfo() {
		if(thread == null) {
			return null;
		}
		return thread.getSessionInfo();
	}
	
	public String getRegisteredUserName() {
		SessionInfo si = getCurrentSessionInfo();
		if(si != null && si.username != null && si.username.length() > 0) {
			return si.username;
		}
		return null;
	}
	
	public String getMyGroupTrackerId() {
		String myGroupTrackerId = "";
		SessionInfo currentSessionInfo = getCurrentSessionInfo();
		if (currentSessionInfo != null) {
			myGroupTrackerId = currentSessionInfo.groupTrackerId;
		}
		return myGroupTrackerId;
	}
	
	
	public SessionInfo prepareSessionToken() throws IOException {
		String deviceKey = app.getSettings().OSMO_DEVICE_KEY.get();
		if(deviceKey.length() == 0) {
			deviceKey = registerOsmoDeviceKey();
		}
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
				lastRegistrationError = obj.getString("error");
				throw new RuntimeException(obj.getString("error"));
			}
			if(!obj.has("address")) {
				lastRegistrationError = "Host name not specified";
				throw new RuntimeException("Host name not specified");
			}
			if(!obj.has("token")) {
				lastRegistrationError = "Token not specified by server";
				throw new RuntimeException("Token not specified by server");
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
	
	public void pushCommand(String cmd) {
		commands.add(cmd);
	}

	@Override
	public String nextSendCommand(OsMoThread tracker) {
		if(!commands.isEmpty()) {
			return commands.poll();
		}
		return null;
	}

	@Override
	public boolean acceptCommand(String command, String id, String data, JSONObject obj, OsMoThread tread) {
		if(command.equals("MOTD")) {
			SessionInfo si = getCurrentSessionInfo();
			if(si != null) {
				if(data.startsWith("[")){
					try {
						data = new JSONArray(data).getString(0);
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
				si.motd = data;
			}
			return true;
		}
		return false;
	}

	@Override
	public void reconnect() {
		
	}
	
	public void reconnectToServer() {
		if(thread != null) {
			thread.reconnect();
		}
	}
	
}
