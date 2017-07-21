package net.osmand.plus.osmo;

import android.app.Activity;
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
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.notifications.OsmandNotification.NotificationType;

import org.apache.commons.logging.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class OsMoService implements OsMoReactor {
	private static final String HTTP_API_PREPARE = "http://api.osmo.mobi/prepare";
	private static final String HTTPS_API_PREPARE = "https://api.osmo.mobi/prepare";
	private static final String HTTP_AUTH = "http://api.osmo.mobi/auth";
	private static final String HTTPS_AUTH = "https://api.osmo.mobi/auth";
	
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
	public static final String TRACK_URL = "http://osmo.mobi/s/";
	private String lastRegistrationError = null;
	private OsMoPlugin plugin;  
	private boolean enabled = false;
	private BroadcastReceiver broadcastReceiver;
	private Notification notification;
	
	public final static String OSMO_REGISTER_AGAIN  = "OSMO_REGISTER_AGAIN"; //$NON-NLS-1$
	private final static int SIMPLE_NOTFICATION_ID = 5;

	private class HttpPostWriter {
		BufferedWriter writer;
		boolean first;

		HttpPostWriter(OutputStream outputStream) {
			this.writer = new BufferedWriter(new OutputStreamWriter(outputStream));
			this.first = true;
		}

		void addPair(String key, String value) throws IOException {
			if (this.first)
				this.first = false;
			else
				this.writer.write("&");

			this.writer.write(URLEncoder.encode(key, "UTF-8"));
			this.writer.write("=");
			this.writer.write(URLEncoder.encode(value, "UTF-8"));
		}

		void flush() throws IOException {
			this.writer.flush();
			this.writer.close();
		}
	}


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
		app.getNotificationHelper().refreshNotification(NotificationType.OSMO);
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
			app.getNotificationHelper().refreshNotification(NotificationType.OSMO);
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
		URL url = new URL(plugin.useHttps()? HTTPS_AUTH : HTTP_AUTH);

		try {
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);

			// Add your data
			HttpPostWriter postWriter = new HttpPostWriter(conn.getOutputStream());
			postWriter.addPair("android_id", Secure.getString(app.getContentResolver(),
					Secure.ANDROID_ID));

			postWriter.addPair("android_model", Build.MODEL);
			postWriter.addPair("imei", "0");
			postWriter.addPair("android_product", Build.PRODUCT);
			postWriter.addPair("client", Version.getFullVersion(app));
			postWriter.addPair("osmand", Version.getFullVersion(app));

			// Execute HTTP Post Request
			postWriter.flush();
			BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String r = reader.readLine();
			reader.close();
			conn.disconnect();
			log.info("Authorization key : " + r);
			final JSONObject obj = new JSONObject(r);
			if(obj.has("error")) {
				lastRegistrationError = obj.getString("error");
				throw new OsMoConnectionException(obj.getString("error"));
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
		if (deviceKey.length() == 0) {
			deviceKey = registerOsmoDeviceKey();
		}

		URL url = new URL(plugin.useHttps() ? HTTPS_API_PREPARE : HTTP_API_PREPARE);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		try {
			conn.setDoOutput(true);
			HttpPostWriter postWriter = new HttpPostWriter(conn.getOutputStream());

			// Add your data
			postWriter.addPair("app", Version.getFullVersion(app));
			postWriter.addPair("key", deviceKey);
			if (app.getSettings().OSMO_USER_PWD.get() != null) {
				postWriter.addPair("auth", app.getSettings().OSMO_USER_PWD.get());
			}
			postWriter.addPair("protocol", "1");

			// Execute HTTP Post Request
			postWriter.flush();
			BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String r = reader.readLine();
			reader.close();
			conn.disconnect();
			log.info("Authorization key : " + r);
			final JSONObject obj = new JSONObject(r);
			if (obj.has("error")) {
				lastRegistrationError = obj.getString("error");
				runNotification(lastRegistrationError);
				return null;
			}
			if (!obj.has("address")) {
				lastRegistrationError = "Host name not specified";
				throw new RuntimeException("Host name not specified");
			}
			if (!obj.has("token")) {
				lastRegistrationError = "Token not specified by server";
				throw new RuntimeException("Token not specified by server");
			}

			SessionInfo si = new SessionInfo();
			String a = obj.getString("address");
			if (obj.has("name")) {
				si.username = obj.getString("name");
			}
			if (obj.has("uid")) {
				si.uid = obj.getString("uid");
			}
			int i = a.indexOf(':');
			si.hostName = a.substring(0, i);
			si.port = a.substring(i + 1);
			si.token = obj.getString("token");
			return si;
		} catch (IOException e) {
			throw e;
		} catch (JSONException e) {
			throw new IOException(e);
		}
	}

	private void runNotification(final String error) {
		final Activity ga = plugin.getGroupsActivity();
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
			bld.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
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
	

	protected void showRegisterAgain(Activity ga, String msg) {
		AlertDialog.Builder bld = new AlertDialog.Builder(ga);
		bld.setMessage(msg);
		bld.setPositiveButton(R.string.shared_string_ok, new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				registerAsync();
			}
		});
		bld.setNegativeButton(R.string.shared_string_cancel, null);
		
	}


	public void showErrorMessage(String string) {
		app.showToastMessage(app.getString(R.string.osmo_io_error) + string);
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

	public OsmandApplication getMyApplication() {
		return app;
	}
}
