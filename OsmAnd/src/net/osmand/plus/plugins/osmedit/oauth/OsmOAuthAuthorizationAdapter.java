package net.osmand.plus.plugins.osmedit.oauth;

import android.content.Context;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.github.scribejava.core.builder.api.DefaultApi10a;
import com.github.scribejava.core.exceptions.OAuthException;
import com.github.scribejava.core.model.OAuth1AccessToken;
import com.github.scribejava.core.model.OAuth1RequestToken;
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

        DefaultApi10a api10a;
        String key;
        String secret;
        if (plugin.OSM_USE_DEV_URL.get()) {
            api10a = new OsmOAuthAuthorizationClient.OsmDevApi();
            key = app.getString(R.string.osm_oauth_developer_key);
            secret = app.getString(R.string.osm_oauth_developer_secret);
        } else {
            api10a = new OsmOAuthAuthorizationClient.OsmApi();
            key = app.getString(R.string.osm_oauth_consumer_key);
            secret = app.getString(R.string.osm_oauth_consumer_secret);
        }
        client = new OsmOAuthAuthorizationClient(key, secret, api10a);
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
        String tokenSecret = plugin.OSM_USER_ACCESS_TOKEN_SECRET.get();
        if (!(token.isEmpty() || tokenSecret.isEmpty())) {
            client.setAccessToken(new OAuth1AccessToken(token, tokenSecret));
        } else {
            client.setAccessToken(null);
        }
    }

    public void startOAuth(ViewGroup rootLayout, boolean nightMode) {
        new StartOAuthAsyncTask(rootLayout, nightMode).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
    }

    private void saveToken() {
        OAuth1AccessToken accessToken = client.getAccessToken();
        plugin.OSM_USER_ACCESS_TOKEN.set(accessToken.getToken());
        plugin.OSM_USER_ACCESS_TOKEN_SECRET.set(accessToken.getTokenSecret());
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

    private class StartOAuthAsyncTask extends AsyncTask<Void, Void, OAuth1RequestToken> {

        private final ViewGroup rootLayout;
        boolean nightMode;

        public StartOAuthAsyncTask(ViewGroup rootLayout, boolean nightMode) {
            this.rootLayout = rootLayout;
            this.nightMode = nightMode;
        }

        @Override
        protected OAuth1RequestToken doInBackground(Void... params) {
            return client.startOAuth();
        }

        @Override
        protected void onPostExecute(OAuth1RequestToken requestToken) {
            if (requestToken != null) {
                loadWebView(rootLayout, nightMode, client.getService().getAuthorizationUrl(requestToken));
            } else {
                app.showShortToastMessage(app.getString(R.string.internet_not_available));
            }
        }
    }

    private class AuthorizeAsyncTask extends AsyncTask<String, Void, Void> {

        private final OsmOAuthHelper helper;

        public AuthorizeAsyncTask(OsmOAuthHelper helper) {
            this.helper = helper;
        }

        @Override
        protected Void doInBackground(String... oauthVerifier) {
            if (client.getRequestToken() != null) {
                client.authorize(oauthVerifier[0]);
                if (isValidToken()) {
                    saveToken();
                    updateUserName();
                }
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