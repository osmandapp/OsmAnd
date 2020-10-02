package net.osmand.plus.osmedit.oauth;

import android.net.TrafficStats;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import com.github.scribejava.core.model.OAuth1AccessToken;
import com.github.scribejava.core.model.OAuth1RequestToken;
import com.github.scribejava.core.model.OAuthAsyncRequestCallback;
import com.github.scribejava.core.model.Response;
import net.osmand.osm.oauth.OsmOAuthAuthorizationClient;
import net.osmand.plus.BuildConfig;
import net.osmand.plus.OsmandApplication;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class OsmOAuthAuthorizationAdapter {
    OsmandApplication application;
    OsmOAuthAuthorizationClient client = new OsmOAuthAuthorizationClient(BuildConfig.CONSUMER_KEY, BuildConfig.CONSUMER_SECRET);

    public OsmOAuthAuthorizationAdapter(OsmandApplication application) {
        final int THREAD_ID = 10101;
        TrafficStats.setThreadStatsTag(THREAD_ID);
        this.application = application;
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
        String token = application.getSettings().USER_ACCESS_TOKEN.get();
        String tokenSecret = application.getSettings().USER_ACCESS_TOKEN_SECRET.get();
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
        application.getSettings().USER_ACCESS_TOKEN.set(accessToken.getToken());
        application.getSettings().USER_ACCESS_TOKEN_SECRET.set(accessToken.getTokenSecret());
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
}
