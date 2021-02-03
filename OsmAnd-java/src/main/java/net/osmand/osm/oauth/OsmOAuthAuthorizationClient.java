// License: GPL. For details, see LICENSE file.
package net.osmand.osm.oauth;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.builder.api.DefaultApi10a;
import com.github.scribejava.core.builder.api.OAuth1SignatureType;
import com.github.scribejava.core.httpclient.jdk.JDKHttpClientConfig;
import com.github.scribejava.core.model.*;
import com.github.scribejava.core.oauth.OAuth10aService;
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
    private OAuth1RequestToken requestToken;
    private OAuth1AccessToken accessToken;
    private final OAuth10aService service;
    private final OsmAndJDKHttpClient httpClient;
    public final static Log log = PlatformUtil.getLog(OsmOAuthAuthorizationClient.class);

    public OsmOAuthAuthorizationClient(String key, String secret, DefaultApi10a api) {
        httpClient = new OsmAndJDKHttpClient(JDKHttpClientConfig.defaultConfig());
        service = new ServiceBuilder(key)
                .apiSecret(secret)
                .httpClient(httpClient)
                .callback("osmand-oauth://example.com/oauth")
                .build(api);
    }

    public static class OsmApi extends DefaultApi10a {
        @Override
        public OAuth1SignatureType getSignatureType() {
            return OAuth1SignatureType.QUERY_STRING;
        }

        @Override
        public String getRequestTokenEndpoint() {
            return "https://www.openstreetmap.org/oauth/request_token";
        }

        @Override
        public String getAccessTokenEndpoint() {
            return "https://www.openstreetmap.org/oauth/access_token";
        }

        @Override
        protected String getAuthorizationBaseUrl() {
            return "https://www.openstreetmap.org/oauth/authorize";
        }
    }

    public static class OsmDevApi extends DefaultApi10a {
        @Override
        public OAuth1SignatureType getSignatureType() {
            return OAuth1SignatureType.QUERY_STRING;
        }

        @Override
        public String getRequestTokenEndpoint() {
            return "https://master.apis.dev.openstreetmap.org/oauth/request_token";
        }

        @Override
        public String getAccessTokenEndpoint() {
            return "https://master.apis.dev.openstreetmap.org/oauth/access_token";
        }

        @Override
        protected String getAuthorizationBaseUrl() {
            return "https://master.apis.dev.openstreetmap.org/oauth/authorize";
        }
    }

    public OsmAndJDKHttpClient getHttpClient() {
        return httpClient;
    }

    public OAuth10aService getService() {
        return service;
    }

    public void setAccessToken(OAuth1AccessToken accessToken) {
        this.accessToken = accessToken;
    }

    public OAuth1AccessToken getAccessToken() {
        return accessToken;
    }

    public OAuth1RequestToken getRequestToken() {
        return requestToken;
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
        service.getApi().getSignatureType();
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

    public OAuth1RequestToken startOAuth() {
        try {
            requestToken = service.getRequestToken();
        } catch (IOException e) {
            log.error(e);
        } catch (InterruptedException e) {
            log.error(e);
        } catch (ExecutionException e) {
            log.error(e);
        }
        return requestToken;
    }

    public OAuth1AccessToken authorize(String oauthVerifier) {
        try {
            setAccessToken(service.getAccessToken(requestToken, oauthVerifier));
        } catch (IOException e) {
            log.error(e);
        } catch (InterruptedException e) {
            log.error(e);
        } catch (ExecutionException e) {
            log.error(e);
        }
        return accessToken;
    }

    public boolean isValidToken() {
        return !(accessToken == null);
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
