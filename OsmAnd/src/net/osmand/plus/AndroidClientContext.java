package net.osmand.plus;

import java.io.File;

import net.osmand.Version;
import net.osmand.access.AccessibleToast;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;

public class AndroidClientContext implements ClientContext {
	
	private final OsmandApplication app;

	public AndroidClientContext(OsmandApplication app) {
		this.app = app;
	}

	@Override
	public String getFullVersion() {
		return Version.getFullVersion(app);
	}
	
	@Override
	public String getVersionAsURLParam() {
		return Version.getVersionAsURLParam(app);
	}

	@Override
	public boolean isWifiConnected() {
		ConnectivityManager mgr =  (ConnectivityManager) app.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo ni = mgr.getActiveNetworkInfo();
		return ni != null && ni.getType() == ConnectivityManager.TYPE_WIFI;
	}

	

	@Override
	public String getString(int resId) {
		return app.getString(resId);
	}

	@Override
	public File getAppDir() {
		return app.getSettings().extendOsmandPath(ResourceManager.APP_DIR);
	}
	
	@Override
	public File getAppDir(String extend) {
		return app.getSettings().extendOsmandPath(ResourceManager.APP_DIR + extend);
	}


	@Override
	public void showToastMessage(int msgId) {
		AccessibleToast.makeText(app, msgId, Toast.LENGTH_LONG).show();
	}

}
