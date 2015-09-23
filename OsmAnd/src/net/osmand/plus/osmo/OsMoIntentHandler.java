package net.osmand.plus.osmo;

import net.osmand.plus.OsmandApplication;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;

public class OsMoIntentHandler extends AsyncTask<Intent, Void, String> {
	
	private OsmandApplication app;
	private OsMoPlugin plugin;

	public OsMoIntentHandler(OsmandApplication app, OsMoPlugin plugin) {
		this.app = app;
		this.plugin = plugin;
	}

	@Override
	protected String doInBackground(Intent... params) {
		try {
			while (app.isApplicationInitializing()) {
				Thread.sleep(200);
			}
			for (Intent intent : params) {
				String scheme = intent.getScheme();
				Uri data = intent.getData();
				if ("http".equals(scheme) && data.getHost().equals("z.osmo.mobi")) {
					String path = data.getPath();
					String lastPath = path.substring(path.lastIndexOf('/') + 1);
					if(lastPath.equals("login")) {
						String user = data.getQueryParameter("u");
						String pwd = data.getQueryParameter("p");
						app.getSettings().OSMO_USER_NAME.set(user);
						app.getSettings().OSMO_USER_PWD.set(pwd);
						plugin.getService().reconnectToServer();
					} else if(lastPath.equals("join")) {
						String gid = data.getQueryParameter("id");
						String name = data.getQueryParameter("name");
						if(name == null) {
							name = "";
						}
						plugin.getGroups().joinGroup(gid, name, app.getSettings().OSMO_USER_NAME.get());
					}
				}
				return null;
			}
		} catch (Exception e) {
			return e.getMessage();
		}
		return null;
	}
	
	@Override
	protected void onPostExecute(String result) {
		if(result != null) {
			
		}
	}

}
