package net.osmand.plus.server;

import android.util.Log;
import android.webkit.MimeTypeMap;
import fi.iki.elonen.NanoHTTPD;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.server.endpoints.TileEndpoint;
import net.osmand.util.Algorithms;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import static fi.iki.elonen.NanoHTTPD.newFixedLengthResponse;

public class ApiRouter {
	private OsmandApplication application;
	private final String FOLDER_NAME = "server";
	private final Map<String, ApiEndpoint> endpoints = new HashMap<>();
	private static final org.apache.commons.logging.Log LOG = PlatformUtil.getLog(ApiRouter.class);

	public ApiRouter() {
		initRoutes();
	}

	public OsmandApplication getApplication() {
		return application;
	}

	final TileEndpoint tileEndpoint = new TileEndpoint(application);

	private void initRoutes() {
		endpoints.put(tileEndpoint.uri, tileEndpoint);
	}

	public void setApplication(OsmandApplication application) {
		this.application = application;
		tileEndpoint.setApplication(application);
	}

	public NanoHTTPD.Response route(NanoHTTPD.IHTTPSession session) {
		Log.d("SERVER", "URI: " + session.getUri());
		String uri = session.getUri();
		if (uri.equals("/")) {
			return getStatic("/go.html");
		}
		if (uri.contains("/scripts/") ||
				uri.contains("/images/") ||
				uri.contains("/css/") ||
				uri.contains("/fonts/") ||
				uri.contains("/favicon.ico")
		) {
			return getStatic(uri);
		}
		if (isApiUrl(uri)) {
			return routeApi(session);
		} else {
			return routeContent(session);
		}
	}

	private NanoHTTPD.Response routeApi(NanoHTTPD.IHTTPSession session) {
		String uri = session.getUri();
		int pathEnd = uri.indexOf("/", 1);
		if (pathEnd != -1) {
			uri = uri.substring(0, pathEnd);
		}
		ApiEndpoint endpoint = endpoints.get(uri);
		if (endpoint != null) {
			return endpoint.apiCall.call(session);
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

	private NanoHTTPD.Response routeContent(NanoHTTPD.IHTTPSession session) {
		String url = session.getUri();
		//add index page
		//return getStatic(session.getUri());
		String responseText = getHtmlPage(url);
		if (responseText != null) {
			return newFixedLengthResponse(responseText);
		} else {
			return ErrorResponses.response404;
		}
	}

	public NanoHTTPD.Response getStatic(String uri) {
		InputStream is;
		String mimeType = parseMimeType(uri);
		if (application != null) {
			try {
				is = application.getAssets().open(FOLDER_NAME + uri);
				if (is.available() == 0) {
					return ErrorResponses.response404;
				}
				return newFixedLengthResponse(
						NanoHTTPD.Response.Status.OK,
						mimeType,
						is,
						is.available());
			} catch (IOException e) {
				return ErrorResponses.response404;
			}
		}
		return ErrorResponses.response500;
	}

	private String parseMimeType(String url) {
		String type = null;
		if (url.endsWith(".js")) return "text/javascript";
		String extension = MimeTypeMap.getFileExtensionFromUrl(url);
		if (extension != null) {
			type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
		}
		return type;
	}

	private String readHTMLFromFile(String filename) {
//		try {
//			InputStream is = application.getAssets().open(FOLDER_NAME + filename);
//			return Algorithms.readFromInputStream(is,false).toString();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		return null;
		StringBuilder sb = new StringBuilder();
		try {
			InputStream is = application.getAssets().open(FOLDER_NAME + filename);
			BufferedReader br = new BufferedReader(new InputStreamReader(is,
					Charset.forName("UTF-8")));
			String str;
			while ((str = br.readLine()) != null) {
				sb.append(str);
			}
			br.close();
		} catch (IOException e) {
			LOG.error("IOException", e);
			return null;
		}
		return sb.toString();
	}

	public String getHtmlPage(String name) {
		String responseText = "";
		if (application != null) {
			responseText = readHTMLFromFile(name);
		}
		return responseText;
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