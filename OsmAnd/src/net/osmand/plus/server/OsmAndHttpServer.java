package net.osmand.plus.server;

import java.io.IOException;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class OsmAndHttpServer extends NanoHTTPD {
	public static int PORT = 24990;
	public static String HOSTNAME = "0.0.0.0";

	public OsmAndHttpServer() throws IOException {
		super(HOSTNAME,PORT);
		start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
		System.out.println("\nRunning! Point your browsers to http://localhost:8080/ \n");
	}

	@Override
	public Response serve(IHTTPSession session) {
		String msg = "<html><body><h1>Hello server</h1>\n";
		Map<String, String> parms = session.getParms();
		if (parms.get("username") == null) {
			msg += "<form action='?' method='get'>\n  <p>Your name: <input type='text' name='username'></p>\n" + "</form>\n";
		} else {
			msg += "<p>Hello, " + parms.get("username") + "!</p>";
		}
		return newFixedLengthResponse(msg + "</body></html>\n");
	}
}
