package net.osmand.plus.server;

import net.osmand.plus.OsmandApplication;

import java.io.IOException;

import fi.iki.elonen.NanoHTTPD;

public class OsmAndHttpServer extends NanoHTTPD {
	public static int PORT = 24990;
	public static String HOSTNAME = "0.0.0.0";

	private ServerSessionHandler sessionHandler = new ServerSessionHandler();
	private OsmandApplication androidApplication;

	public OsmandApplication getAndroidApplication() {
		return androidApplication;
	}

	public void setAndroidApplication(OsmandApplication androidApplication) {
		this.androidApplication = androidApplication;
		sessionHandler.setAndroidApplication(androidApplication);
	}

	public OsmAndHttpServer() throws IOException {
		super(HOSTNAME,PORT);
		start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
	}

	@Override
	public Response serve(IHTTPSession session) {
		return sessionHandler.handle(session);
	}
}
