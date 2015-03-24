package net.osmand.plus.osmo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.crypto.Cipher;

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

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.Settings.Secure;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

public class OsMoService implements OsMoReactor {
	private static final String HTTP_API_PREPARE = "http://api.osmo.mobi/prepare";
	private static final String HTTPS_API_PREPARE = "https://api.osmo.mobi/prepare";
	private static final String HTTP_AUTH = "http://api.osmo.mobi/auth";
	private static final String HTTPS_AUTH = "https://api.osmo.mobi/auth";
	private static final boolean USE_RSA_ENCRYPTION = true;
	
	public static final String REGENERATE_CMD = "TRACKER_REGENERATE_ID";
	public static final String SIGN_IN_URL = "http://osmo.mobi/signin?key=";
	private OsMoThread thread;
	private List<OsMoReactor> listReactors = new java.util.concurrent.CopyOnWriteArrayList<OsMoReactor>();
	private ConcurrentLinkedQueue<String> commands = new ConcurrentLinkedQueue<String>();
	private OsmandApplication app;
	private static final Log log = PlatformUtil.getLog(OsMoService.class);
	public static final String SHARE_TRACKER_URL = "http://z.osmo.mobi/connect?id=";
	public static final String SHARE_GROUP_URL = "http://z.osmo.mobi/join?id=";
	public static final String SIGNED_IN_CONTAINS = "z.osmo.mobi/login";
	public static final String TRACK_URL = "http://osmo.mobi/u/";
	private String lastRegistrationError = null;
	private OsMoPlugin plugin;  
	private boolean enabled = false;
	private BroadcastReceiver broadcastReceiver;
	private Notification notification;
	
	public final static String OSMO_REGISTER_AGAIN  = "OSMO_REGISTER_AGAIN"; //$NON-NLS-1$
	private final static int SIMPLE_NOTFICATION_ID = 5;



	public OsMoService(final OsmandApplication app, OsMoPlugin plugin) {
		this.app = app;
		this.plugin = plugin;
		listReactors.add(this);
		listReactors.add(plugin);
		broadcastReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				notification = null;
				NotificationManager mNotificationManager = (NotificationManager) app
						.getSystemService(Context.NOTIFICATION_SERVICE);
				mNotificationManager.cancel(SIMPLE_NOTFICATION_ID);
				registerAsync();
			}


		};
		app.registerReceiver(broadcastReceiver, new IntentFilter(OSMO_REGISTER_AGAIN));
	}
	
	private void registerAsync() {
		new AsyncTask<Void, Void, Void>() {
			public Void doInBackground(Void... voids ) {
				try {
					registerOsmoDeviceKey();
					onConnected();
				} catch (IOException e) {
					e.printStackTrace();
				}
				return null;
			}
		}.execute((Void)null);
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
		return new ArrayList<String>(thread.getLastCommands());
	}
	
	
	public long getConnectionTime() {
		return thread == null || !thread.isConnected() ? System.currentTimeMillis() : thread.getConnectionTime(); 
	}
	
	protected List<OsMoReactor> getListReactors() {
		return listReactors;
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
		thread = new OsMoThread(this);
		enabled = true;
		return true;
	}
	
	public boolean isEnabled() {
		return enabled;
	}
	
	public void disconnect() {
		if(thread != null) {
			enabled = false;
			thread.stopConnection();
			app.getSettings().OSMO_LAST_PING.set(0l);
		}
	}
	
	public void registerReactor(OsMoReactor reactor) {
		if(!listReactors.contains(reactor)) {
			ArrayList<OsMoReactor> lst = new ArrayList<OsMoReactor>(listReactors);
			lst.add(reactor);
			listReactors = lst;
		}
	}

	public void removeReactor(OsMoReactor s) {
		if(listReactors.contains(s)) {
			ArrayList<OsMoReactor> lst = new ArrayList<OsMoReactor>(listReactors);
			lst.remove(s);
			listReactors = lst;
		}
	}
	

	public String registerOsmoDeviceKey() throws IOException {
		HttpClient httpclient = new DefaultHttpClient();
		HttpPost httppost = new HttpPost(plugin.useHttps()? HTTPS_AUTH : HTTP_AUTH);
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
		public String uid;
		public String username;
		
		// after auth
		public String protocol = "";
		public String groupTrackerId;
		public String trackerId;
		public long serverTimeDelta;
		public long motdTimestamp;
		
		public String motd = "";
		public Cipher clientEncCypher;
		public Cipher clientDecCypher;
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
	
	public String getMyTrackerId() {
		String myGroupTrackerId = "";
		SessionInfo currentSessionInfo = getCurrentSessionInfo();
		if (currentSessionInfo != null) {
			myGroupTrackerId = currentSessionInfo.trackerId;
		}
		return myGroupTrackerId;
	}
	
	public SessionInfo prepareSessionToken() throws IOException {
		String deviceKey = app.getSettings().OSMO_DEVICE_KEY.get();
		if(deviceKey.length() == 0) {
			deviceKey = registerOsmoDeviceKey();
		}
		HttpClient httpclient = new DefaultHttpClient();
		KeyPair getMsgPair = null;
		if (plugin.useHttps() && USE_RSA_ENCRYPTION) {
			try {
				KeyPairGenerator rsaGen = KeyPairGenerator.getInstance("RSA");
				getMsgPair = rsaGen.generateKeyPair();
			} catch (Exception e1) {
				if (thread != null) {
					thread.exc("Private key can't be generated", e1);
				} else {
					e1.printStackTrace();
				}
			}
		}
		HttpPost httppost = new HttpPost(plugin.useHttps()? HTTPS_API_PREPARE : HTTP_API_PREPARE);
		try {
			// Add your data
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
			nameValuePairs.add(new BasicNameValuePair("app", Version.getFullVersion(app)));
			nameValuePairs.add(new BasicNameValuePair("key", deviceKey));
			if(getMsgPair != null && getMsgPair.getPublic() instanceof RSAPublicKey) {
				nameValuePairs.add(new BasicNameValuePair("encAlgorithm", "RSA"));
					BigInteger modulus = ((RSAPublicKey) getMsgPair.getPublic()).getModulus();
					BigInteger pe = ((RSAPublicKey) getMsgPair.getPublic()).getPublicExponent();
					nameValuePairs.add(new BasicNameValuePair("encClientPublicKey1", modulus.toString()));
					nameValuePairs.add(new BasicNameValuePair("encClientPublicKey2", pe.toString()));
			}
			
			if(app.getSettings().OSMO_USER_PWD.get() != null) {
				nameValuePairs.add(new BasicNameValuePair("auth", app.getSettings().OSMO_USER_PWD.get()));
			}
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
				runNotification(lastRegistrationError);
				return null;
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
			if(obj.has("name")) {
				si.username = obj.getString("name");
			}
			if(obj.has("uid")) {
				si.uid = obj.getString("uid");
			}
			int i = a.indexOf(':');
			si.hostName = a.substring(0, i);
			si.port = a.substring(i + 1);
			si.token = obj.getString("token");
			try {
				if(getMsgPair != null && obj.has("encServerPublicKey1")) {
					si.clientEncCypher = Cipher.getInstance("RSA");
					PublicKey pk = KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(new BigInteger(obj.getString("encServerPublicKey1")),
							new BigInteger(obj.getString("encServerPublicKey2"))));
					si.clientEncCypher.init(Cipher.ENCRYPT_MODE, pk);
					
					si.clientDecCypher = Cipher.getInstance("RSA");
					si.clientDecCypher.init(Cipher.DECRYPT_MODE, getMsgPair.getPrivate());
				}
			} catch (Exception e) {
				if (thread != null) {
					thread.exc("Error exchanging private keys", e);
				} else {
					e.printStackTrace();
				}
			}
			return si;
		} catch (ClientProtocolException e) {
			throw new IOException(e);
		} catch (IOException e) {
			throw e;
		} catch (JSONException e) {
			throw new IOException(e);
		}
	}

	private void runNotification(final String error) {
		final OsMoGroupsActivity ga = plugin.getGroupsActivity();
		if(ga != null) {
			app.runInUIThread(new Runnable() {
				
				@Override
				public void run() {
					showRegisterAgain(ga, app.getString(R.string.osmo_auth_error, error));
					
				}
			});
		} else if (notification == null) {
			Intent notificationIntent = new Intent(OSMO_REGISTER_AGAIN);
			PendingIntent intent = PendingIntent.getBroadcast(app, 0, notificationIntent,
					PendingIntent.FLAG_UPDATE_CURRENT);
			android.support.v4.app.NotificationCompat.Builder bld = new NotificationCompat.Builder(app);
			bld.setContentInfo(app.getString(R.string.osmo_auth_error, error));
			bld.setContentIntent(intent);
			bld.setContentTitle(app.getString(R.string.osmo_auth_error_short));
			bld.setSmallIcon(R.drawable.bgs_icon);

			NotificationManager mNotificationManager = (NotificationManager) app
					.getSystemService(Context.NOTIFICATION_SERVICE);
			notification = bld.getNotification();
			mNotificationManager.notify(SIMPLE_NOTFICATION_ID, notification);
		}
	}
	

	protected void showRegisterAgain(OsMoGroupsActivity ga, String msg) {
		Builder bld = new AlertDialog.Builder(ga);
		bld.setMessage(msg);
		bld.setPositiveButton(R.string.shared_string_ok, new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				registerAsync();
			}
		});
		bld.setNegativeButton(R.string.shared_string_cancel, null);
		
	}

	private void showDialogAskToReregister(String error) {
//		Builder bld = new AlertDialog.Builder(this);
//		bld.setMessage(app.getString(R.string.osmo_io_error) +  error);
//		bld.show();
	}

	public void showErrorMessage(String string) {
		app.showToastMessage(app.getString(R.string.osmo_io_error) +  string);		
	}
	
	
	public void pushCommand(String cmd) {
		commands.add(cmd);
	}

	@Override
	public String nextSendCommand(OsMoThread tracker) {
		if(System.currentTimeMillis() - app.getSettings().OSMO_LAST_PING.get() > 30000) {
			app.getSettings().OSMO_LAST_PING.set(System.currentTimeMillis());
		}
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
		} else if(command.equals("TRACK_GET")) {
			try {
				JSONArray ar = new JSONArray(data);
				AsyncTask<JSONObject, String, String> task = plugin.getDownloadGpxTask(false);
				JSONObject[] a = new JSONObject[ar.length()];
				for(int i = 0; i < a.length; i++) {
					a[i] = (JSONObject) ar.get(i);
				}
				task.execute(a);
			} catch (JSONException e) {
				e.printStackTrace();
				showErrorMessage(e.getMessage());
			}
		}
		return false;
	}
	

	@Override
	public void onConnected() {
		pushCommand("TRACK_GET");
	}

	@Override
	public void onDisconnected(String msg) {
	}
	
	public void reconnectToServer() {
		if(thread != null) {
			thread.reconnect();
		}
	}

	public boolean isLoggedIn() {
		String psswd = app.getSettings().OSMO_USER_PWD.get();
		String userName = app.getSettings().OSMO_USER_NAME.get();
		return ((!TextUtils.isEmpty(psswd) && !TextUtils.isEmpty(userName)));
	}
}
