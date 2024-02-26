// License: GPL. For details, see LICENSE file.
package net.osmand.osm.oauth;

import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.exceptions.OAuthException;
import com.github.scribejava.core.httpclient.jdk.JDKHttpClientConfig;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthAsyncRequestCallback;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;

import net.osmand.PlatformUtil;

import org.apache.commons.logging.Log;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * An OAuth 1.0 authorization client.
 *
 * @since 2746
 */
public class OsmOAuthAuthorizationClient {
    private OAuth2AccessToken accessToken;
    private String apiSecret;
    private final OAuth20Service service;
    private final OsmAndJDKHttpClient httpClient;
    public final static Log log = PlatformUtil.getLog(OsmOAuthAuthorizationClient.class);
    public OsmOAuthAuthorizationClient(String key, String apiSecret, DefaultApi20 api, String redirectUri, String scope) {
        this.apiSecret = apiSecret;
        httpClient = new OsmAndJDKHttpClient(JDKHttpClientConfig.defaultConfig());
        service = new ServiceBuilder(key)
                .apiSecret(apiSecret)
                .httpClient(httpClient)
                .defaultScope(scope)
                .callback(redirectUri)
                .build(api);
    }

    public static class OsmApi extends DefaultApi20 {
        @Override
        public String getAccessTokenEndpoint() {
            return "https://www.openstreetmap.org/oauth2/token";
        }

        @Override
        protected String getAuthorizationBaseUrl() {
            return "https://www.openstreetmap.org/oauth2/authorize";
        }
    }

    public static class OsmDevApi extends DefaultApi20 {
        @Override
        public String getAccessTokenEndpoint() {
            return "https://master.apis.dev.openstreetmap.org/oauth2/token";
        }

        @Override
        protected String getAuthorizationBaseUrl() {
            return "https://master.apis.dev.openstreetmap.org/oauth2/authorize";
        }
    }

    public OsmAndJDKHttpClient getHttpClient() {
        return httpClient;
    }

    public OAuth20Service getService() {
        return service;
    }

    public void setAccessToken(OAuth2AccessToken accessToken) {
        this.accessToken = accessToken;
    }

    public OAuth2AccessToken getAccessToken() {
        return accessToken;
    }

    public String getApiSecret() {
        return apiSecret;
    }

    public Response performRequestWithoutAuth(String url, String requestMethod, String requestBody)
            throws InterruptedException, ExecutionException, IOException {
        Verb verb = parseRequestMethod(requestMethod);
        OAuthRequest req = new OAuthRequest(verb, url);
        req.setPayload(requestBody);
        return service.execute(req);
    }

    public void performGetRequest(String url, OAuthAsyncRequestCallback<Response> callback) {
        if (accessToken == null) {
            throw new IllegalStateException("Access token is null");
        }
        OAuthRequest req = new OAuthRequest(Verb.GET, url);
        service.signRequest(accessToken, req);
        service.execute(req, callback);
    }

    public Response performRequest(String url, String method, String body)
            throws InterruptedException, ExecutionException, IOException {
        if (accessToken == null) {
            throw new IllegalStateException("Access token is null");
        }
        Verb verbMethod = parseRequestMethod(method);
        OAuthRequest req = new OAuthRequest(verbMethod, url);
        req.setPayload(body);
        service.signRequest(accessToken, req);
        req.addHeader("Content-Type", "application/xml");
        return service.execute(req);
    }

    public OAuth2AccessToken authorize(String oauthVerifier) {
        try {
            setAccessToken(service.getAccessToken(oauthVerifier));
        } catch (OAuthException | IOException | InterruptedException | ExecutionException | IllegalArgumentException e) {
            log.error(e);
        }
        return accessToken;
    }

    public boolean isValidToken() {
        return accessToken != null;
    }

    private Verb parseRequestMethod(String method) {
        Verb m = Verb.GET;
        if (method.equals("POST")) {
            m = Verb.POST;
        }
        if (method.equals("PUT")) {
            m = Verb.PUT;
        }
        if (method.equals("DELETE")) {
            m = Verb.DELETE;
        }
        return m;
    }
}
