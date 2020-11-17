package net.osmand.plus.osmedit.oauth;

import android.net.TrafficStats;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import com.github.scribejava.core.builder.api.DefaultApi10a;
import com.github.scribejava.core.model.OAuth1AccessToken;
import com.github.scribejava.core.model.OAuth1RequestToken;
import com.github.scribejava.core.model.OAuthAsyncRequestCallback;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;

import net.osmand.PlatformUtil;
import net.osmand.osm.oauth.OsmOAuthAuthorizationClient;
import net.osmand.plus.OsmAndConstants;
import net.osmand.plus.OsmandApplication;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class OsmOAuthAuthorizationAdapter {

    private static final int THREAD_ID = 10101;
    private static final String OSM_USER = "user";
    private static final String DISPLAY_NAME = "display_name";
    private OsmandApplication app;
    private final OsmOAuthAuthorizationClient client;

    public OsmOAuthAuthorizationAdapter(OsmandApplication app) {
        TrafficStats.setThreadStatsTag(THREAD_ID);
        this.app = app;
        DefaultApi10a api10a;
        String key;
        String secret;
        if (app.getSettings().USE_DEV_URL.get()) {
            api10a = new OsmOAuthAuthorizationClient.OsmDevApi();
            key = OsmAndConstants.OSM_OAUTH_DEVELOPER_KEY;
            secret = OsmAndConstants.OSM_OAUTH_DEVELOPER_SECRET;
        } else {
            api10a = new OsmOAuthAuthorizationClient.OsmApi();
            key = OsmAndConstants.OSM_OAUTH_CONSUMER_KEY;
            secret = OsmAndConstants.OSM_OAUTH_CONSUMER_SECRET;
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
        String token = app.getSettings().USER_ACCESS_TOKEN.get();
        String tokenSecret = app.getSettings().USER_ACCESS_TOKEN_SECRET.get();
        if (!(token.isEmpty() || tokenSecret.isEmpty())) {
            client.setAccessToken(new OAuth1AccessToken(token, tokenSecret));
        } else {
            client.setAccessToken(null);
        }
    }

    public void startOAuth(ViewGroup rootLayout) {
        OAuth1RequestToken requestToken = client.startOAuth();
        loadWebView(rootLayout, client.getService().getAuthorizationUrl(requestToken));
    }

    private void saveToken() {
        OAuth1AccessToken accessToken = client.getAccessToken();
        app.getSettings().USER_ACCESS_TOKEN.set(accessToken.getToken());
        app.getSettings().USER_ACCESS_TOKEN_SECRET.set(accessToken.getTokenSecret());
    }

    private void loadWebView(ViewGroup root, String url) {
        WebView webView = new WebView(root.getContext());
        webView.requestFocus(View.FOCUS_DOWN);
        webView.loadUrl(url);
        root.addView(webView);
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

    public void authorize(String oauthVerifier) {
        client.authorize(oauthVerifier);
        saveToken();
    }

    public String getUserName() throws InterruptedException, ExecutionException, IOException, XmlPullParserException {
        Response response = getOsmUserDetails();
        return parseUserName(response);
    }

    public Response getOsmUserDetails() throws InterruptedException, ExecutionException, IOException {
        String osmUserDetailsUrl = app.getSettings().getOsmUrl() + "api/0.6/user/details";
        return performRequest(osmUserDetailsUrl, Verb.GET.name(), null);
    }

    public String parseUserName(Response response) throws XmlPullParserException, IOException {
        String userName = null;
        XmlPullParser parser = PlatformUtil.newXMLPullParser();
        parser.setInput(response.getStream(), "UTF-8");
        int tok;
        while ((tok = parser.next()) != XmlPullParser.END_DOCUMENT) {
            if (tok == XmlPullParser.START_TAG && OSM_USER.equals(parser.getName())) {
                userName = parser.getAttributeValue("", DISPLAY_NAME);
            }
        }
        return userName;
    }
}
