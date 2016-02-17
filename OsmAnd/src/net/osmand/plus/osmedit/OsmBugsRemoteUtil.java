package net.osmand.plus.osmedit;


import net.osmand.PlatformUtil;
import net.osmand.osm.io.Base64;
import net.osmand.osm.io.NetworkUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.osmedit.OsmPoint.Action;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URLEncoder;

public class OsmBugsRemoteUtil implements OsmBugsUtil {

	private static final Log log = PlatformUtil.getLog(OsmBugsRemoteUtil.class);

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

	private OsmandApplication app;
	private OsmandSettings settings;

	public OsmBugsRemoteUtil(OsmandApplication app) {
		this.app = app;
		settings = app.getSettings();
	}

	@Override
	public OsmBugResult commit(OsmNotesPoint point, String text, Action action) {
		StringBuilder b = new StringBuilder();
		String msg = "";
		if (action == OsmPoint.Action.CREATE) {
			b.append(getNotesApi()).append("?"); //$NON-NLS-1$
			b.append("lat=").append(point.getLatitude()); //$NON-NLS-1$
			b.append("&lon=").append(point.getLongitude()); //$NON-NLS-1$
			b.append("&text=").append(URLEncoder.encode(text)); //$NON-NLS-1$
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
			b.append("?text=").append(URLEncoder.encode(text)); //$NON-NLS-1$
		}
		return editingPOI(b.toString(), "POST", msg);
	}

	private OsmBugResult editingPOI(String url, String requestMethod, String userOperation) {
		OsmBugResult r = new OsmBugResult();
		try {
			HttpURLConnection connection = NetworkUtils.getHttpURLConnection(url);
			log.info("Editing poi " + url);
			connection.setConnectTimeout(15000);
			connection.setRequestMethod(requestMethod);
			connection.setRequestProperty("User-Agent", Version.getFullVersion(app)); //$NON-NLS-1$
			if (true) {
				String token = settings.USER_NAME.get() + ":" + settings.USER_PASSWORD.get(); //$NON-NLS-1$
				connection.addRequestProperty("Authorization", "Basic " + Base64.encode(token.getBytes("UTF-8"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
			connection.setDoInput(true);
			if (requestMethod.equals("PUT") || requestMethod.equals("POST") || requestMethod.equals("DELETE")) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				// connection.setDoOutput(true);
				//				connection.setRequestProperty("Content-type", "text/xml"); //$NON-NLS-1$ //$NON-NLS-2$
				// OutputStream out = connection.getOutputStream();
				// String requestBody = null;
				// if (requestBody != null) {
				//					BufferedWriter bwr = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"), 1024); //$NON-NLS-1$
				// bwr.write(requestBody);
				// bwr.flush();
				// }
				// out.close();
			}
			connection.connect();
			String msg = connection.getResponseMessage();
			boolean ok = connection.getResponseCode() == HttpURLConnection.HTTP_OK;
			log.info(msg); //$NON-NLS-1$
			// populate return fields.

			StringBuilder responseBody = Algorithms.readFromInputStream(connection.getInputStream());
			log.info("Response : " + responseBody); //$NON-NLS-1$
			connection.disconnect();
			if (!ok) {
				r.warning = msg + "\n" + responseBody;
			}
		} catch (NullPointerException e) {
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
