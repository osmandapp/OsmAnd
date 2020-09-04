package net.osmand.plus.server;

import fi.iki.elonen.NanoHTTPD;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.server.endpoints.TileEndpoint;

import java.io.IOException;

public class OsmAndHttpServer extends NanoHTTPD {
	public static int PORT = 24990;
	public static String HOSTNAME = "0.0.0.0";
	private final ApiRouter router = new ApiRouter();
	private OsmandApplication application;

	public OsmAndHttpServer() throws IOException {
		super(HOSTNAME, PORT);
		start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
		registerEndpoints();
	}

	public OsmandApplication getApplication() {
		return application;
	}

	public void setApplication(OsmandApplication application) {
		this.application = application;
		router.setApplication(application);
	}

	private void registerEndpoints() {
		router.register("/tile", new TileEndpoint(application));
	}

	@Override
	public Response serve(IHTTPSession session) {
		return router.route(session);
	}
}
