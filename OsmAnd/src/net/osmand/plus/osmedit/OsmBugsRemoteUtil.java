package net.osmand.plus.osmedit;


import com.github.scribejava.core.model.Response;

import net.osmand.PlatformUtil;
import net.osmand.osm.io.Base64;
import net.osmand.osm.io.NetworkUtils;
import net.osmand.osm.oauth.OsmOAuthAuthorizationClient;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.osmedit.OsmPoint.Action;
import net.osmand.plus.osmedit.oauth.OsmOAuthAuthorizationAdapter;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.util.Algorithms;
import org.apache.commons.logging.Log;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;

public class OsmBugsRemoteUtil implements OsmBugsUtil {

	private static final Log log = PlatformUtil.getLog(OsmBugsRemoteUtil.class);
	private static final String GET = "GET";
	private static final String POST = "POST";

	String getNotesApi() {
		return settings.getOsmUrl() + "api/0.6/notes";
	}

	String getUserDetailsApi() {
		return settings.getOsmUrl() + "api/0.6/user/details";
	}

	private OsmandApplication app;
	private OsmandSettings settings;

	public OsmBugsRemoteUtil(OsmandApplication app) {
		this.app = app;
		settings = app.getSettings();
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
				b.append(getNotesApi()).append("?"); //$NON-NLS-1$
				b.append("lat=").append(point.getLatitude()); //$NON-NLS-1$
				b.append("&lon=").append(point.getLongitude()); //$NON-NLS-1$
				b.append("&text=").append(URLEncoder.encode(text, "UTF-8")); //$NON-NLS-1$
				msg = "creating bug";
			} else {
				b.append(getNotesApi()).append("/");
				b.append(point.getId()); //$NON-NLS-1$
				if (action == OsmPoint.Action.REOPEN) {
					b.append("/reopen");
					msg = "reopen note";
				} else if (action == OsmPoint.Action.MODIFY) {
					b.append("/comment");
					msg = "adding comment";
				} else if (action == OsmPoint.Action.DELETE) {
					b.append("/close");
					msg = "close note";
				}
				b.append("?text=").append(URLEncoder.encode(text, "UTF-8")); //$NON-NLS-1$
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
		return editingPOI(getUserDetailsApi(), GET, "validate_login", false);
	}

	private OsmBugResult editingPOI(String url, String requestMethod, String userOperation, boolean anonymous) {
		OsmOAuthAuthorizationAdapter authorizationAdapter = new OsmOAuthAuthorizationAdapter(app);
		OsmBugResult result = new OsmBugResult();
		if (authorizationAdapter.isValidToken() && !anonymous) {
			try {
				result = performOAuthRequest(url, requestMethod, userOperation, authorizationAdapter);
			} catch (InterruptedException e) {
				log.error(e);
				result.warning = e.getMessage();
			} catch (ExecutionException e) {
				log.error(e);
				result.warning = e.getMessage();
			} catch (IOException e) {
				log.error(e);
				result.warning = e.getMessage();
			}
		} else {
			try {
				result = performBasicRequest(url, requestMethod, anonymous);
			} catch (FileNotFoundException | NullPointerException e) {
				// that's tricky case why NPE is thrown to fix that problem httpClient could be used
				String msg = app.getString(R.string.auth_failed);
				log.error(msg, e);
				result.warning = app.getString(R.string.auth_failed) + "";
			} catch (MalformedURLException e) {
				log.error(userOperation + " " + app.getString(R.string.failed_op), e);
				result.warning = e.getMessage() + "";
			} catch (IOException e) {
				log.error(userOperation + " " + app.getString(R.string.failed_op), e);
				result.warning = e.getMessage() + " link unavailable";
			}
		}
		return result;
	}

	private OsmBugResult performBasicRequest(String url, String requestMethod, boolean anonymous) throws IOException {
		OsmBugResult result = new OsmBugResult();
		HttpURLConnection connection = NetworkUtils.getHttpURLConnection(url);
		log.info("Editing poi " + url);
		connection.setConnectTimeout(15000);
		connection.setRequestMethod(requestMethod);
		connection.setRequestProperty("User-Agent", Version.getFullVersion(app));
		if (!anonymous) {
			String token = settings.OSM_USER_NAME.get() + ":" + settings.OSM_USER_PASSWORD.get();
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
}