package net.osmand.plus.server;

import net.osmand.plus.OsmandApplication;

import fi.iki.elonen.NanoHTTPD;

public class ServerSessionHandler {
	private OsmandApplication androidContext;

	private ApiRouter router = new ApiRouter();

	public OsmandApplication getAndroidContext() {
		return androidContext;
	}

	public void setAndroidContext(OsmandApplication androidContext) {
		this.androidContext = androidContext;
		router.setAndroidContext(androidContext);
	}

	public NanoHTTPD.Response handle(NanoHTTPD.IHTTPSession session) {
		return router.route(session);
	}
}
