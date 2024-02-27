package net.osmand.plus.plugins.osmedit.oauth;

import android.content.Context;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.exceptions.OAuthException;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthAsyncRequestCallback;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;

import net.osmand.PlatformUtil;
import net.osmand.osm.oauth.OsmOAuthAuthorizationClient;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.osmedit.OsmEditingPlugin;
import net.osmand.plus.plugins.osmedit.helpers.OsmBugsRemoteUtil;
import net.osmand.plus.utils.AndroidUtils;

import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class OsmOAuthAuthorizationAdapter {

    private static final Log log = PlatformUtil.getLog(OsmOAuthAuthorizationAdapter.class);
    private static final int THREAD_ID = 10101;

    private final OsmandApplication app;
    private final OsmEditingPlugin plugin;
    private final OsmOAuthAuthorizationClient client;

    public OsmOAuthAuthorizationAdapter(@NonNull OsmandApplication app) {
        TrafficStats.setThreadStatsTag(THREAD_ID);
        this.app = app;
        this.plugin = PluginsHelper.getPlugin(OsmEditingPlugin.class);

		DefaultApi20 api20;
		String key;
		String secret;
		if (plugin.OSM_USE_DEV_URL.get()) {
			api20 = new OsmOAuthAuthorizationClient.OsmDevApi();
			key = app.getString(R.string.osm_oauth2_dev_id);
			secret = app.getString(R.string.osm_oauth2_dev_secret);
		} else {
			api20 = new OsmOAuthAuthorizationClient.OsmApi();
			key = app.getString(R.string.osm_oauth2_client_id);
			secret = app.getString(R.string.osm_oauth2_client_secret);
		}
		String redirectUri = app.getString(R.string.oauth2_redirect_uri);
		String scope = app.getString(R.string.oauth2_scope);
		client = new OsmOAuthAuthorizationClient(key, secret, api20, redirectUri, scope);
		restoreToken();
	}

    public OsmOAuthAuthorizationClient getClient() {
        return client;
    }

    public boolean isValidToken() {
        return client.isValidToken();
    }

    public void resetToken() {
        client.setAccessToken(null);
    }

	public void restoreToken() {
		String token = plugin.OSM_USER_ACCESS_TOKEN.get();
		String tokenSecret = client.getApiSecret();
		if (!(token.isEmpty() || tokenSecret.isEmpty())) {
			client.setAccessToken(new OAuth2AccessToken(token, tokenSecret));
		} else {
			client.setAccessToken(null);
		}
	}

	public void startOAuth(ViewGroup rootLayout, boolean nightMode) {
		loadWebView(rootLayout, nightMode, client.getService().getAuthorizationUrl());
	}

	private void saveToken() {
		OAuth2AccessToken accessToken = client.getAccessToken();
		plugin.OSM_USER_ACCESS_TOKEN.set(accessToken.getAccessToken());
	}

    private void loadWebView(ViewGroup root, boolean nightMode, String url) {
        Uri uri = Uri.parse(url);
        Context context = root.getContext();
        AndroidUtils.openUrl(context, uri, nightMode);
    }

    public void performGetRequest(String url, OAuthAsyncRequestCallback<Response> callback) {
        client.performGetRequest(url, callback);
    }

    public Response performRequest(String url, String method, String body)
            throws InterruptedException, ExecutionException, IOException {
        return client.performRequest(url, method, body);
    }

    public Response performRequestWithoutAuth(String url, String method, String body)
            throws InterruptedException, ExecutionException, IOException {
        return client.performRequestWithoutAuth(url, method, body);
    }

    public void authorize(String oauthVerifier, OsmOAuthHelper helper) {
        new AuthorizeAsyncTask(helper).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, oauthVerifier);
    }


	private class AuthorizeAsyncTask extends AsyncTask<String, Void, Void> {

		private final OsmOAuthHelper helper;

		public AuthorizeAsyncTask(OsmOAuthHelper helper) {
			this.helper = helper;
		}

		@Override
		protected Void doInBackground(String... oauthVerifier) {
				client.authorize(oauthVerifier[0]);
				if (isValidToken()) {
					saveToken();
					updateUserName();
				}
			return null;
		}

        @Override
        protected void onPostExecute(Void result) {
            helper.notifyAndRemoveListeners();
        }

        public void updateUserName() {
            String userName = "";
            try {
                userName = getUserName();
            } catch (InterruptedException | ExecutionException | IOException | XmlPullParserException
                    | OAuthException e) {
                log.error(e);
            }
            plugin.OSM_USER_DISPLAY_NAME.set(userName);
        }

        public String getUserName() throws InterruptedException, ExecutionException, IOException, XmlPullParserException {
            Response response = getOsmUserDetails();
            return OsmBugsRemoteUtil.parseUserName(response.getStream());
        }

        public Response getOsmUserDetails() throws InterruptedException, ExecutionException, IOException {
            String osmUserDetailsUrl = plugin.getOsmUrl() + "api/0.6/user/details";
            return performRequest(osmUserDetailsUrl, Verb.GET.name(), null);
        }
    }
}