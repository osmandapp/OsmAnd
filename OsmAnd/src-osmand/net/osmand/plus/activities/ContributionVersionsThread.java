package net.osmand.plus.activities;

import static net.osmand.plus.activities.ContributionVersionActivity.DOWNLOAD_BUILDS_LIST;
import static net.osmand.plus.activities.ContributionVersionActivity.INSTALL_BUILD;

import androidx.annotation.Nullable;

import net.osmand.osm.io.NetworkUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.Version;
import net.osmand.plus.activities.ContributionVersionActivity.OperationType;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.Date;

class ContributionVersionsThread extends Thread {

	private static final String URL_GET_BUILD = "https://osmand.net/";
	private static final String URL_TO_RETRIEVE_BUILDS = "https://osmand.net/builds";

	private OsmandApplication app;
	private ContributionVersionActivity activity;
	private int operationId;

	public void setActivity(@Nullable ContributionVersionActivity activity) {
		this.activity = activity;
		if (activity != null) {
			this.app = activity.getApp();
		}
	}

	@OperationType
	public int getOperationId() {
		return operationId;
	}

	public void setOperationId(@OperationType int operationId) {
		this.operationId = operationId;
	}

	@Override
	public void run() {
		Exception ex = null;
		try {
			if (this.activity != null) {
				executeThreadOperation(operationId);
			}
		} catch (Exception e) {
			ex = e;
		}
		final Exception e = ex;
		if (this.activity != null) {
			this.activity.runOnUiThread(() -> {
				if (activity != null) {
					activity.endThreadOperation(operationId, e);
				}
			});
		}
	}

	private void executeThreadOperation(@OperationType int operationId) throws Exception {
		if (operationId == DOWNLOAD_BUILDS_LIST) {
			URLConnection connection = NetworkUtils.getHttpURLConnection(URL_TO_RETRIEVE_BUILDS);
			XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
			parser.setInput(connection.getInputStream(), "UTF-8");
			int next;
			while ((next = parser.next()) != XmlPullParser.END_DOCUMENT) {
				if (next == XmlPullParser.START_TAG && parser.getName().equals("build")) { //$NON-NLS-1$
					if ("osmand".equalsIgnoreCase(parser.getAttributeValue(null, "type"))) {
						String path = parser.getAttributeValue(null, "path"); //$NON-NLS-1$
						String size = parser.getAttributeValue(null, "size"); //$NON-NLS-1$
						String date = parser.getAttributeValue(null, "timestamp"); //$NON-NLS-1$
						String tag = parser.getAttributeValue(null, "tag"); //$NON-NLS-1$
						Date d = null;
						if (date != null) {
							try {
								d = new Date(Long.parseLong(date));
							} catch (RuntimeException e) {
								e.printStackTrace();
							}
						}
						OsmAndBuild build = new OsmAndBuild(path, size, d, tag);
						if (!Version.isFreeVersion(app) || path.contains("default")) {
							activity.downloadedBuilds.add(build);
						}
					}
				}
			}
		} else if (operationId == INSTALL_BUILD) {
			URLConnection connection = NetworkUtils.getHttpURLConnection(URL_GET_BUILD + activity.currentSelectedBuild.path);
			if (activity.pathToDownload.exists()) {
				activity.pathToDownload.delete();
			}
			int bufflen = 16000;
			byte[] buffer = new byte[bufflen];
			InputStream is = connection.getInputStream();
			FileOutputStream fout = new FileOutputStream(activity.pathToDownload);
			try {
				int totalRead = 0;
				int read;
				while ((read = is.read(buffer, 0, bufflen)) != -1) {
					fout.write(buffer, 0, read);
					totalRead += read;
					if (totalRead > 1024) {
						activity.progressDlg.incrementProgressBy(totalRead / 1024);
						totalRead %= 1024;
					}
				}
			} finally {
				fout.close();
				is.close();
			}
		}
	}
}
