package net.osmand.plus.server;

import android.webkit.MimeTypeMap;
import fi.iki.elonen.NanoHTTPD;
import net.osmand.PlatformUtil;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.server.endpoints.TileEndpoint;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class OsmAndHttpServer extends NanoHTTPD {
	private static final String ASSETS_FOLDER_NAME = "server";
	private static final org.apache.commons.logging.Log LOG = PlatformUtil.getLog(OsmAndHttpServer.class);
	private final Map<String, ApiEndpoint> endpoints = new HashMap<>();
	private MapActivity mapActivity;

	public OsmAndHttpServer(String hostname, int port) {
		super(hostname, port);
	}

	@Override
	public void stop() {
		mapActivity.getMapView().setServerRendering(false);
		super.stop();
	}

	@Override
	public Response serve(IHTTPSession session) {
		String uri = session.getUri();
		if (uri.equals("/")) {
			return getStatic("/index.html");
		}
		if (isApiUrl(uri)) {
			return routeApi(session);
		}
		return getStatic(uri);
	}

	public void start(MapActivity mapActivity) throws IOException {
		this.mapActivity = mapActivity;
		registerEndpoints();
		start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
		mapActivity.getMapView().setServerRendering(true);
	}

	public String getUrl() {
		RotatedTileBox rtb = mapActivity.getMapView().getCurrentRotatedTileBox();
		float lat = (float) rtb.getLatitude();
		float lon = (float) rtb.getLongitude();
		int z = rtb.getZoom();
		return String.format("http://%s:%d/?lat=%.4f&lon%.4f&zoom=%d", getHostname(), getListeningPort(),
				lat, lon, z);
	}

	private NanoHTTPD.Response routeApi(NanoHTTPD.IHTTPSession session) {
		String uri = session.getUri();
		Iterator<Map.Entry<String, ApiEndpoint>> it = endpoints.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, ApiEndpoint> e = it.next();
			if (uri.startsWith(e.getKey())) {
				try {
					return e.getValue().process(session, uri);
				} catch (Exception exception) {
					LOG.error("SERVER ERROR: " + exception.getMessage());
					return ErrorResponses.response500;
				}
			}
		}
		return ErrorResponses.response404;
	}

	private boolean isApiUrl(String uri) {
		for (String endpoint : endpoints.keySet()) {
			int stringLength = endpoint.length();
			if (uri.startsWith(endpoint) &&
					(uri.length() == endpoint.length() || uri.charAt(stringLength) == '/')) {
				return true;
			}
		}
		return false;
	}

	private void registerEndpoints() {
		register("/tile", new TileEndpoint(mapActivity));
	}

	private void register(String path, ApiEndpoint endpoint) {
		endpoints.put(path, endpoint);
	}

	private NanoHTTPD.Response getStatic(String uri) {
		InputStream is;
		String mimeType = parseMimeType(uri);
		OsmandApplication app = mapActivity.getMyApplication();
		if (app != null) {
			try {
				is = app.getAssets().open(ASSETS_FOLDER_NAME + uri);
				if (is.available() == 0) {
					return ErrorResponses.response404;
				}
				return newFixedLengthResponse(
						NanoHTTPD.Response.Status.OK,
						mimeType,
						is,
						is.available());
			} catch (IOException e) {
				LOG.error(e);
				return ErrorResponses.response404;
			}
		}
		return ErrorResponses.response500;
	}

	private String parseMimeType(String url) {
		String type = "text/plain";
		if (url.endsWith(".js")) return "text/javascript";
		String extension = MimeTypeMap.getFileExtensionFromUrl(url);
		if (extension != null) {
			type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
		}
		return type;
	}

	public interface ApiEndpoint {
		NanoHTTPD.Response process(IHTTPSession session, String url);
	}

	public static class ErrorResponses {
		public static NanoHTTPD.Response response404 =
				newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_FOUND,
						NanoHTTPD.MIME_PLAINTEXT, "404 Not Found");

		public static NanoHTTPD.Response response500 =
				newFixedLengthResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR,
						NanoHTTPD.MIME_PLAINTEXT, "500 Internal Server Error");
	}
}
