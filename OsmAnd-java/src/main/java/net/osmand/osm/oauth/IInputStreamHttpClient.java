package net.osmand.osm.oauth;

import com.github.scribejava.core.httpclient.HttpClient;

public interface IInputStreamHttpClient extends HttpClient {
	String getUserAgent();
}
