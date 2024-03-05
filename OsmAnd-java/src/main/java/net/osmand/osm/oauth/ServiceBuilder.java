package net.osmand.osm.oauth;

import com.github.scribejava.core.builder.ServiceBuilderOAuth20;
import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.httpclient.HttpClient;
import com.github.scribejava.core.httpclient.HttpClientConfig;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.github.scribejava.core.oauth.OAuthService;
import com.github.scribejava.core.utils.Preconditions;

import java.io.OutputStream;

/**
 * Implementation of the Builder pattern, with a fluent interface that creates a {@link OAuthService}
 */
public class ServiceBuilder implements ServiceBuilderOAuth20 {

    private String callback;
    private String apiKey;
    private String apiSecret;
    private String scope;
    private OutputStream debugStream;
    private String responseType = "code";
    private String userAgent;

    private HttpClientConfig httpClientConfig;
    private HttpClient httpClient;

    public ServiceBuilder(String apiKey) {
        apiKey(apiKey);
    }

    @Override
    public ServiceBuilder callback(String callback) {
        this.callback = callback;
        return this;
    }

    @Override
    public final ServiceBuilder apiKey(String apiKey) {
        Preconditions.checkEmptyString(apiKey, "Invalid Api key");
        this.apiKey = apiKey;
        return this;
    }

    @Override
    public ServiceBuilder apiSecret(String apiSecret) {
        Preconditions.checkEmptyString(apiSecret, "Invalid Api secret");
        this.apiSecret = apiSecret;
        return this;
    }

    private ServiceBuilder setScope(String scope) {
        Preconditions.checkEmptyString(scope, "Invalid OAuth scope");
        this.scope = scope;
        return this;
    }

    @Override
    public ServiceBuilderOAuth20 defaultScope(String defaultScope) {
        return setScope(defaultScope);
    }

    @Override
    public ServiceBuilder debugStream(OutputStream debugStream) {
        Preconditions.checkNotNull(debugStream, "debug stream can't be null");
        this.debugStream = debugStream;
        return this;
    }

    @Override
    public ServiceBuilderOAuth20 responseType(String responseType) {
        Preconditions.checkEmptyString(responseType, "Invalid OAuth responseType");
        this.responseType = responseType;
        return this;
    }

    @Override
    public ServiceBuilder httpClientConfig(HttpClientConfig httpClientConfig) {
        Preconditions.checkNotNull(httpClientConfig, "httpClientConfig can't be null");
        this.httpClientConfig = httpClientConfig;
        return this;
    }

    @Override
    public ServiceBuilder httpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
        return this;
    }

    @Override
    public ServiceBuilder userAgent(String userAgent) {
        this.userAgent = userAgent;
        return this;
    }

    @Override
    public ServiceBuilder debug() {
        return debugStream(System.out);
    }

    @Override
    public OAuth20Service build(DefaultApi20 api) {
        return api.createService(apiKey, apiSecret, callback, scope, responseType, debugStream, userAgent,
                httpClientConfig, httpClient);
    }
}
