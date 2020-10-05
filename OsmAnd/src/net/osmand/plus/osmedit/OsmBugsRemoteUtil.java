package net.osmand.plus.osmedit;


import net.osmand.PlatformUtil;
import net.osmand.osm.io.Base64;
import net.osmand.osm.io.NetworkUtils;
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

public class OsmBugsRemoteUtil implements OsmBugsUtil {

	private static final Log log = PlatformUtil.getLog(OsmBugsRemoteUtil.class);
	private static final String GET = "GET";
	private static final String POST = "POST";

	static String getNotesApi() {
		final int deviceApiVersion = android.os.Build.VERSION.SDK_INT;
		String RETURN_API;
		if (deviceApiVersion >= android.os.Build.VERSION_CODES.GINGERBREAD) {
			RETURN_API = "https://api.openstreetmap.org/api/0.6/notes";
		} else {
			RETURN_API = "http://api.openstreetmap.org/api/0.6/notes";
		}
		return RETURN_API;
	}

	static String getUserDetailsApi() {
		final int deviceApiVersion = android.os.Build.VERSION.SDK_INT;
		String RETURN_API;
		if (deviceApiVersion >= android.os.Build.VERSION_CODES.GINGERBREAD) {
			RETURN_API = "https://api.openstreetmap.org/api/0.6/user/details";
		} else {
			RETURN_API = "http://api.openstreetmap.org/api/0.6/user/details";
		}
		return RETURN_API;
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

	private OsmBugResult editingPOI(String url, String requestMethod, String userOperation,
									boolean anonymous) {
		OsmOAuthAuthorizationAdapter client = new OsmOAuthAuthorizationAdapter(app);
		OsmBugResult r = new OsmBugResult();
		try {
			HttpURLConnection connection = NetworkUtils.getHttpURLConnection(url);
			log.info("Editing poi " + url);
			connection.setConnectTimeout(15000);
			connection.setRequestMethod(requestMethod);
			connection.setRequestProperty("User-Agent", Version.getFullVersion(app)); //$NON-NLS-1$

			if (!anonymous) {
				if (client.isValidToken()){
					connection.addRequestProperty("Authorization", "OAuth " + client.getClient().getAccessToken().getToken());
				}
				else {
					String token = settings.USER_NAME.get() + ":" + settings.USER_PASSWORD.get(); //$NON-NLS-1$
					connection.addRequestProperty("Authorization", "Basic " + Base64.encode(token.getBytes("UTF-8"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}
			}

			connection.setDoInput(true);
			connection.connect();
			String msg = connection.getResponseMessage();
			boolean ok = connection.getResponseCode() == HttpURLConnection.HTTP_OK;
			log.info(msg); //$NON-NLS-1$
			// populate return fields.

			StringBuilder responseBody;
			if (connection.getResponseCode() == HttpURLConnection.HTTP_CONFLICT) {
				responseBody = Algorithms.readFromInputStream(connection.getErrorStream());
			} else {
				responseBody = Algorithms.readFromInputStream(connection.getInputStream());
			}
			log.info("Response : " + responseBody); //$NON-NLS-1$
			connection.disconnect();
			if (!ok) {
				r.warning = msg + "\n" + responseBody;
			}
		} catch (FileNotFoundException | NullPointerException e) {
			// that's tricky case why NPE is thrown to fix that problem httpClient could be used
			String msg = app.getString(R.string.auth_failed);
			log.error(msg, e);
			r.warning = app.getString(R.string.auth_failed) + "";
		} catch (MalformedURLException e) {
			log.error(userOperation + " " + app.getString(R.string.failed_op), e); //$NON-NLS-1$
			r.warning = e.getMessage() + "";
		} catch (IOException e) {
			log.error(userOperation + " " + app.getString(R.string.failed_op), e); //$NON-NLS-1$
			r.warning = e.getMessage() + " link unavailable";
		}
		return r;
	}

}
