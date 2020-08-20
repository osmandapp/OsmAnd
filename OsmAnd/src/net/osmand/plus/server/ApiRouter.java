package net.osmand.plus.server;

import android.util.Log;

import net.osmand.plus.OsmandApplication;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import fi.iki.elonen.NanoHTTPD;

import static fi.iki.elonen.NanoHTTPD.newFixedLengthResponse;

public class ApiRouter {
	private OsmandApplication androidContext;

	public OsmandApplication getAndroidContext() {
		return androidContext;
	}

	public void setAndroidContext(OsmandApplication androidContext) {
		this.androidContext = androidContext;
	}

	public NanoHTTPD.Response route(NanoHTTPD.IHTTPSession session) {
		Log.d("SERVER", "URI: " + session.getUri());
		if (session.getUri().contains("/scripts/") ||
				session.getUri().contains("/images/") ||
				session.getUri().contains("/css/") ||
				session.getUri().contains("/favicon.ico")
		) {
			return getStatic(session.getUri());
		} else {
			return getGoHtml();
		}
	}

	public NanoHTTPD.Response getStatic(String uri) {
		InputStream is = null;
		if (androidContext != null) {
			try {
				is = androidContext.getAssets().open("server" + uri);
				if (is.available() == 0){
					return ErrorResponses.response404;
				}
				return newFixedLengthResponse(
						NanoHTTPD.Response.Status.OK,
						"text/plain",
						is,
						is.available());
			} catch (IOException e) {
				return ErrorResponses.response500;
			}
		}
		return ErrorResponses.response500;
	}

	public NanoHTTPD.Response getGoHtml() {
		String responseText = "";
		if (androidContext != null) {
			try {
				InputStream is = androidContext.getAssets().open("server/go.html");
				StringBuilder sb = new StringBuilder();
				BufferedReader br = new BufferedReader(new InputStreamReader(is,
						Charset.forName("UTF-8")));
				String str;
				while ((str = br.readLine()) != null) {
					sb.append(str);
				}
				br.close();
				responseText = sb.toString();
			} catch (IOException e) {
				return ErrorResponses.response500;
			}
		}
		return newFixedLengthResponse(responseText);
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