package net.osmand.plus.plugins.osmedit.helpers;


import androidx.annotation.NonNull;

import com.github.scribejava.core.exceptions.OAuthException;
import com.github.scribejava.core.model.Response;

import net.osmand.PlatformUtil;
import net.osmand.osm.io.Base64;
import net.osmand.osm.io.NetworkUtils;
import net.osmand.osm.oauth.OsmOAuthAuthorizationClient;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.Version;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.osmedit.OsmEditingPlugin;
import net.osmand.plus.plugins.osmedit.data.OsmNotesPoint;
import net.osmand.plus.plugins.osmedit.data.OsmPoint;
import net.osmand.plus.plugins.osmedit.data.OsmPoint.Action;
import net.osmand.plus.plugins.osmedit.oauth.OsmOAuthAuthorizationAdapter;
import net.osmand.plus.utils.AndroidNetworkUtils;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;

public class OsmBugsRemoteUtil implements OsmBugsUtil {

	private static final Log log = PlatformUtil.getLog(OsmBugsRemoteUtil.class);

	private static final String GET = "GET";
	private static final String POST = "POST";

	private static final String OSM_USER = "user";
	private static final String DISPLAY_NAME = "display_name";

	private static final String NOTES_URL = "api/0.6/notes";
	private static final String USER_DETAILS_URL = "api/0.6/user/details";

	private final OsmandApplication app;
	private final OsmEditingPlugin plugin;

	public OsmBugsRemoteUtil(@NonNull OsmandApplication app) {
		this.app = app;
		plugin = PluginsHelper.getPlugin(OsmEditingPlugin.class);
	}

	@NonNull
	String getNotesApi() {
		return plugin.getOsmUrl() + NOTES_URL;
	}

	@NonNull
	String getUserDetailsApi() {
		return plugin.getOsmUrl() + USER_DETAILS_URL;
	}

	@Override
	public OsmBugResult commit(OsmNotesPoint point, String text, Action action) {
		return commit(point, text, action, false);
	}

	@Override
	public OsmBugResult modify(OsmNotesPoint bug, String text) {
		return null;
	}

	public OsmBugResult commit(OsmNotesPoint point, String text, Action action, boolean anonymous) {
		StringBuilder b = new StringBuilder();
		String msg = "";
		try {
			if (action == OsmPoint.Action.CREATE) {
				b.append(getNotesApi()).append("?");
				b.append("lat=").append(point.getLatitude());
				b.append("&lon=").append(point.getLongitude());
				b.append("&text=").append(URLEncoder.encode(text + "\n\n#OsmAnd", "UTF-8"));
				msg = "Creating bug";
			} else {
				b.append(getNotesApi()).append("/");
				b.append(point.getId());
				if (action == OsmPoint.Action.REOPEN) {
					b.append("/reopen");
					msg = "Reopening note";
				} else if (action == OsmPoint.Action.MODIFY) {
					b.append("/comment");
					msg = "Adding comment";
				} else if (action == OsmPoint.Action.DELETE) {
					b.append("/close");
					msg = "Closing note";
				}
				b.append("?text=").append(URLEncoder.encode(text, "UTF-8"));
			}
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		if (!anonymous) {
			OsmBugResult loginResult = validateLoginDetails();
			if (loginResult.warning != null) {
				return loginResult;
			}
		}
		return editingPOI(b.toString(), POST, msg, anonymous);
	}

	public OsmBugResult validateLoginDetails() {
		return editingPOI(getUserDetailsApi(), GET, "Validating login", false);
	}

	private OsmBugResult editingPOI(String url, String requestMethod, String userOperation, boolean anonymous) {
		OsmOAuthAuthorizationAdapter authorizationAdapter = new OsmOAuthAuthorizationAdapter(app);
		OsmBugResult result = new OsmBugResult();
		if (authorizationAdapter.isValidToken() && !anonymous) {
			try {
				result = performOAuthRequest(url, requestMethod, userOperation, authorizationAdapter);
			} catch (InterruptedException | ExecutionException | IOException | OAuthException e) {
				log.error(userOperation + " failed", e);
				result.warning = e.getMessage();
			}
		} else {
			try {
				result = performBasicRequest(url, requestMethod, userOperation, anonymous);
			} catch (NullPointerException | IOException e) {
				log.error(userOperation + " failed", e);
				result.warning = e.getMessage();
			}
		}
		return result;
	}

	private OsmBugResult performBasicRequest(String url, String requestMethod, String userOperation, boolean anonymous)
			throws IOException {
		OsmBugResult result = new OsmBugResult();
		HttpURLConnection connection = NetworkUtils.getHttpURLConnection(url);
		log.info(userOperation + " " + url);
		connection.setConnectTimeout(AndroidNetworkUtils.CONNECT_TIMEOUT);
		connection.setReadTimeout(AndroidNetworkUtils.READ_TIMEOUT);
		connection.setRequestMethod(requestMethod);
		connection.setRequestProperty("User-Agent", Version.getFullVersion(app));
		if (!anonymous) {
			String token = plugin.OSM_USER_NAME_OR_EMAIL.get() + ":" + plugin.OSM_USER_PASSWORD.get();
			connection.addRequestProperty("Authorization", "Basic " + Base64.encode(token.getBytes(StandardCharsets.UTF_8)));
		}
		connection.setDoInput(true);
		connection.connect();

		String msg = connection.getResponseMessage();
		boolean ok = connection.getResponseCode() == HttpURLConnection.HTTP_OK;
		log.info(msg);
		// populate return fields.

		StringBuilder responseBody;
		if (connection.getResponseCode() == HttpURLConnection.HTTP_CONFLICT) {
			responseBody = Algorithms.readFromInputStream(connection.getErrorStream());
		} else {
			responseBody = Algorithms.readFromInputStream(connection.getInputStream());
			if (!anonymous) {
				try {
					byte[] bytes = String.valueOf(responseBody).getBytes(StandardCharsets.UTF_8);
					result.userName = parseUserName(new ByteArrayInputStream(bytes));
				} catch (IOException | XmlPullParserException e) {
					log.error(e);
				}
			}
		}
		log.info("Response : " + responseBody);
		connection.disconnect();
		if (!ok) {
			result.warning = msg + "\n" + responseBody;
		}
		return result;
	}

	private OsmBugResult performOAuthRequest(String url, String requestMethod, String userOperation,
	                                         OsmOAuthAuthorizationAdapter authorizationAdapter)
			throws InterruptedException, ExecutionException, IOException {
		OsmBugResult result = new OsmBugResult();
		OsmOAuthAuthorizationClient client = authorizationAdapter.getClient();
		Response response = client.performRequest(url, requestMethod, userOperation);
		if (response.getCode() != HttpURLConnection.HTTP_OK) {
			result.warning = response.getMessage() + "\n" + response.getBody();
		}
		return result;
	}

	public static String parseUserName(@NonNull InputStream inputStream) throws XmlPullParserException, IOException {
		String userName = null;
		XmlPullParser parser = PlatformUtil.newXMLPullParser();
		parser.setInput(inputStream, "UTF-8");
		int tok;
		while ((tok = parser.next()) != XmlPullParser.END_DOCUMENT) {
			if (tok == XmlPullParser.START_TAG && OSM_USER.equals(parser.getName())) {
				userName = parser.getAttributeValue("", DISPLAY_NAME);
			}
		}
		return userName;
	}
}