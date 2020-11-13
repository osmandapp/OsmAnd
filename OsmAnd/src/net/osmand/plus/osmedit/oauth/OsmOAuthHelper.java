package net.osmand.plus.osmedit.oauth;

import android.view.ViewGroup;

import androidx.annotation.NonNull;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;

import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class OsmOAuthHelper {

	private static final Log log = PlatformUtil.getLog(OsmOAuthHelper.class);

	private final OsmandApplication app;

	private final OsmOAuthAuthorizationAdapter authorizationAdapter;

	public OsmOAuthHelper(@NonNull OsmandApplication app) {
		this.app = app;
		authorizationAdapter = new OsmOAuthAuthorizationAdapter(app);
	}

	public void startOAuth(ViewGroup view) {
		authorizationAdapter.startOAuth(view);
	}

	public void authorize(String oauthVerifier) {
		authorizationAdapter.authorize(oauthVerifier);
		updateUserName();
	}

	public OsmOAuthAuthorizationAdapter getAuthorizationAdapter() {
		return authorizationAdapter;
	}

	private void updateUserName() {
		String userName = "";
		try {
			userName = authorizationAdapter.getUserName();
		} catch (InterruptedException e) {
			log.error(e);
		} catch (ExecutionException e) {
			log.error(e);
		} catch (IOException e) {
			log.error(e);
		} catch (XmlPullParserException e) {
			log.error(e);
		}
		app.getSettings().USER_DISPLAY_NAME.set(userName);
	}

	public interface OsmAuthorizationListener {
		void authorizationCompleted();
	}
}