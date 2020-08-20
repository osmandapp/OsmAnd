package net.osmand.plus.server;

import android.util.Log;
import android.webkit.MimeTypeMap;

import com.google.gson.Gson;

import net.osmand.data.FavouritePoint;
import net.osmand.plus.OsmandApplication;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.List;

import fi.iki.elonen.NanoHTTPD;

import static fi.iki.elonen.NanoHTTPD.newFixedLengthResponse;

public class ApiRouter {
	private OsmandApplication androidContext;

	public OsmandApplication getAndroidContext() {
		return androidContext;
	}

	private final String FOLDER_NAME = "server";
	private Gson gson = new Gson();

	public void setAndroidContext(OsmandApplication androidContext) {
		this.androidContext = androidContext;
	}

	public NanoHTTPD.Response route(NanoHTTPD.IHTTPSession session) {
		Log.d("SERVER", "URI: " + session.getUri());
		if (session.getUri().equals("/")) return getStatic("/go.html");
		if (session.getUri().contains("/scripts/") ||
				session.getUri().contains("/images/") ||
				session.getUri().contains("/css/") ||
				session.getUri().contains("/fonts/") ||
				session.getUri().contains("/favicon.ico")
		) return getStatic(session.getUri());
		if (isApiUrl(session.getUri())){
			return routeApi(session);
		}
		else {
			return routeContent(session);
		}
	}

	private NanoHTTPD.Response routeApi(NanoHTTPD.IHTTPSession session) {
		return newFixedLengthResponse("");
	}

	private boolean isApiUrl(String uri) {
		return uri.endsWith("/favorites");
	}

	private NanoHTTPD.Response routeContent(NanoHTTPD.IHTTPSession session) {
		String url = session.getUri();
		//add index page
		String responseText = getHtmlPage(url);
		if (responseText != null) {
			responseText = addFavoritesToResponse(responseText);
			return newFixedLengthResponse(responseText);
		} else {
			return ErrorResponses.response404;
		}
	}

	public NanoHTTPD.Response getStatic(String uri) {
		InputStream is = null;
		String mimeType = parseMimeType(uri);
		if (androidContext != null) {
			try {
				is = androidContext.getAssets().open(FOLDER_NAME + uri);
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
		StringBuilder sb = new StringBuilder();
		try {
			InputStream is = androidContext.getAssets().open(FOLDER_NAME + filename);
			BufferedReader br = new BufferedReader(new InputStreamReader(is,
					Charset.forName("UTF-8")));
			String str;
			while ((str = br.readLine()) != null) {
				sb.append(str);
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return sb.toString();
	}

	public String getHtmlPage(String name) {
		String responseText = "";
		if (androidContext != null) {
			responseText = readHTMLFromFile(name);
		}
		if (responseText == null) {
			return null;
		}
		return responseText;
	}

	private String addFavoritesToResponse(String responseText) {
		List<FavouritePoint> points = androidContext.getFavorites().getFavouritePoints();
		StringBuilder text = new StringBuilder();
		for (FavouritePoint p : points) {
			String json = jsonFromFavorite(p);
			text.append(json);
			text.append(",");
		}
		return responseText + "<script>var osmand = {}; window.osmand.favoritePoints = [" + text.toString() + "];setupMarkers();</script>";
	}

	private String jsonFromFavorite(FavouritePoint favouritePoint) {
		return gson.toJson(favouritePoint);
	}

	static class ErrorResponses {
		static NanoHTTPD.Response response404 =
				newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_FOUND,
						NanoHTTPD.MIME_PLAINTEXT, "404 Not Found");

		static NanoHTTPD.Response response500 =
				newFixedLengthResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR,
						NanoHTTPD.MIME_PLAINTEXT, "500 Internal Server Error");
	}
}