package net.osmand.plus.server;

import net.osmand.plus.OsmandApplication;

import fi.iki.elonen.NanoHTTPD;
import net.osmand.plus.activities.MapActivity;

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

	public void setMapActivity(MapActivity activity) {
		//todo
		router.mapActivity = activity;
	}

	public NanoHTTPD.Response handle(NanoHTTPD.IHTTPSession session) {
		return router.route(session);
	}
}
