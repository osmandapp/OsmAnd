package net.osmand.plus.dropbox;

import net.osmand.LogUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;

import org.apache.commons.logging.Log;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.android.AuthActivity;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session.AccessType;

public class DropboxPlugin extends OsmandPlugin {

	public static final String ID = "osmand.dropbox";
	private static final Log log = LogUtil.getLog(DropboxPlugin.class);
	private OsmandApplication app;
	private DropboxAPI<AndroidAuthSession> mApi;
	
	final static private String APP_KEY = "CHANGE_ME";
	final static private String APP_SECRET = "CHANGE_ME_SECRET";
	final static private AccessType ACCESS_TYPE = AccessType.APP_FOLDER;
	
    final static private String ACCESS_KEY_NAME = "DROPBOX_ACCESS_KEY";
    final static private String ACCESS_SECRET_NAME = "DROPBOX_ACCESS_SECRET";


	@Override
	public String getId() {
		return ID;
	}

	public DropboxPlugin(OsmandApplication app) {
		this.app = app;

	}

	@Override
	public String getDescription() {
		// TODO
		return app.getString(R.string.osmodroid_plugin_description);
	}

	@Override
	public String getName() {
		// TODO
		return app.getString(R.string.osmodroid_plugin_name);
	}

	@Override
	public boolean init(final OsmandApplication app) {
		this.app = app;
        AndroidAuthSession session = buildSession();
        mApi = new DropboxAPI<AndroidAuthSession>(session);
		return true;
	}
	
	public void syncFolders(){
		try {
			Entry f = mApi.createFolder("osmand");
		} catch (DropboxException e) {
		}
	}
	
	private String[] getKeys() {
		OsmandSettings set = app.getSettings();
        SharedPreferences prefs = (SharedPreferences) set.getGlobalPreferences();
        String key = prefs.getString(ACCESS_KEY_NAME, null);
        String secret = prefs.getString(ACCESS_SECRET_NAME, null);
        if (key != null && secret != null) {
        	String[] ret = new String[2];
        	ret[0] = key;
        	ret[1] = secret;
        	return ret;
        } else {
        	return null;
        }
    }
	
	public void storeKeys(String key, String secret) {
        // Save the access key for later
		OsmandSettings set = app.getSettings();
        SharedPreferences prefs = (SharedPreferences) set.getGlobalPreferences();
        prefs.edit().putString(ACCESS_KEY_NAME, key)
        .putString(ACCESS_SECRET_NAME, secret).commit();
    }

	public void clearKeys() {
        SharedPreferences prefs = (SharedPreferences) app.getSettings().getGlobalPreferences();
        prefs.edit().remove(ACCESS_KEY_NAME).remove(ACCESS_SECRET_NAME).commit();
    }

    private AndroidAuthSession buildSession() {
        AppKeyPair appKeyPair = new AppKeyPair(APP_KEY, APP_SECRET);
        AndroidAuthSession session;

        String[] stored = getKeys();
        if (stored != null) {
            AccessTokenPair accessToken = new AccessTokenPair(stored[0], stored[1]);
            session = new AndroidAuthSession(appKeyPair, ACCESS_TYPE, accessToken);
        } else {
            session = new AndroidAuthSession(appKeyPair, ACCESS_TYPE);
        }

        return session;
    }
    
    private void checkAppKeySetup() {
        // Check if the app has set up its manifest properly.
        Intent testIntent = new Intent(Intent.ACTION_VIEW);
        String scheme = "db-" + APP_KEY;
        String uri = scheme + "://" + AuthActivity.AUTH_VERSION + "/test";
        testIntent.setData(Uri.parse(uri));
        PackageManager pm = app.getPackageManager();
        if (0 == pm.queryIntentActivities(testIntent, 0).size()) {
            log.warn("URL scheme in your app's " +
                    "manifest is not set up correctly. You should have a " +
                    "com.dropbox.client2.android.AuthActivity with the " +
                    "scheme: " + scheme);
        }
    }

	@Override
	public void registerLayers(MapActivity activity) {

	}
	
	@Override
	public void disable(OsmandApplication app) {
	}


}
