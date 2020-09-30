// License: GPL. For details, see LICENSE file.
package net.osmand.plus.osmedit.oauth;


import android.net.TrafficStats;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.builder.api.DefaultApi10a;
import com.github.scribejava.core.model.*;
import com.github.scribejava.core.oauth.OAuth10aService;
import net.osmand.plus.BuildConfig;
import net.osmand.plus.OsmandApplication;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * An OAuth 1.0 authorization client.
 *
 * @since 2746
 */
public class OsmOAuthAuthorizationClient {

    OAuth10aService service = new ServiceBuilder(BuildConfig.CONSUMER_KEY)
            .apiSecret(BuildConfig.CONSUMER_SECRET)
            .debug()
            .callback("osmand-oauth://example.com/oauth")
            .build(new OsmApi());

    OAuth1RequestToken requestToken;
    OAuth1AccessToken accessToken;
    OsmandApplication application;

    static class OsmApi extends DefaultApi10a {

        @Override
        public String getRequestTokenEndpoint() { return "https://www.openstreetmap.org/oauth/request_token"; }

        @Override
        public String getAccessTokenEndpoint() {
            return "https://www.openstreetmap.org/oauth/access_token";
        }

        @Override
        protected String getAuthorizationBaseUrl() {
            return "https://www.openstreetmap.org/oauth/authorize";
        }
    }

    public OsmOAuthAuthorizationClient(OsmandApplication application) {
        final int THREAD_ID = 10101;
        TrafficStats.setThreadStatsTag(THREAD_ID);
        this.application = application;
    }

    public void performGetRequest(String url,OAuthAsyncRequestCallback<Response> callback) {
        if (accessToken == null) {
            throw new IllegalStateException("Access token is null");
        }
        OAuthRequest req = new OAuthRequest(Verb.GET, url);
        service.signRequest(accessToken, req);
        service.execute(req, callback);
    }

    public void startOAuth(ViewGroup rootLayout) {
        try {
            requestToken = service.getRequestToken();
            loadWebView(rootLayout, service.getAuthorizationUrl(requestToken));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    public void authorize(String oauthVerifier) {
        try {
            accessToken = service.getAccessToken(requestToken, oauthVerifier);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        saveToken();
    }

    public boolean isValidToken(){
        return !(accessToken == null);
    }

    public void restoreToken() {
        String token = application.getSettings().USER_ACCESS_TOKEN.get();
        String tokenSecret = application.getSettings().USER_ACCESS_TOKEN_SECRET.get();
        accessToken = new OAuth1AccessToken(token, tokenSecret);
    }

    private void saveToken() {
        application.getSettings().USER_ACCESS_TOKEN.set(accessToken.getToken());
        application.getSettings().USER_ACCESS_TOKEN_SECRET.set(accessToken.getTokenSecret());
    }

    private void loadWebView(ViewGroup root, String url) {
        WebView webView = new WebView(root.getContext());
        webView.requestFocus(View.FOCUS_DOWN);
        webView.loadUrl(url);
        root.addView(webView);
    }
}
