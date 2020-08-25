package net.osmand.plus.server;

import net.osmand.plus.OsmandApplication;

import fi.iki.elonen.NanoHTTPD;

public class ServerSessionHandler {
	private OsmandApplication androidApplication;

	private ApiRouter router = new ApiRouter();

	public OsmandApplication getAndroidApplication() {
		return androidApplication;
	}

	public void setAndroidApplication(OsmandApplication androidApplication) {
		this.androidApplication = androidApplication;
		router.setAndroidContext(androidApplication);
	}

	public NanoHTTPD.Response handle(NanoHTTPD.IHTTPSession session) {
		return router.route(session);
	}
}
