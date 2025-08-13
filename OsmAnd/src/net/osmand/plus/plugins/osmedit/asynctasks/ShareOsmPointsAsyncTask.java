package net.osmand.plus.plugins.osmedit.asynctasks;

import android.content.Intent;
import android.os.AsyncTask;
import android.util.Xml;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.shared.SharedUtil;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.osm.edit.Entity;
import net.osmand.osm.edit.Node;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.plugins.osmedit.data.OpenstreetmapPoint;
import net.osmand.plus.plugins.osmedit.data.OsmNotesPoint;
import net.osmand.plus.plugins.osmedit.data.OsmPoint;
import net.osmand.plus.plugins.osmedit.data.OsmPoint.Group;
import net.osmand.plus.plugins.osmedit.fragments.OsmEditsFragment;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ShareOsmPointsAsyncTask extends AsyncTask<OsmPoint, OsmPoint, String> {

	private static final Log log = PlatformUtil.getLog(ShareOsmPointsAsyncTask.class);

	private final OsmandApplication app;
	private final File srcFile;
	private final boolean oscFile;
	private final ShareOsmPointsListener listener;

	public ShareOsmPointsAsyncTask(@NonNull OsmandApplication app, int fileType, int exportType,
	                               @Nullable ShareOsmPointsListener listener) {
		this.app = app;
		this.listener = listener;
		oscFile = fileType == OsmEditsFragment.FILE_TYPE_OSC;
		srcFile = app.getAppPath(getFileName(exportType));
	}

	@Override
	protected String doInBackground(OsmPoint... points) {
		if (oscFile) {
			return saveOscFile(points);
		} else {
			return saveGpxFile(points);
		}
	}

	private String saveGpxFile(OsmPoint[] points) {
		GpxFile gpx = new GpxFile(Version.getFullVersion(app));
		for (OsmPoint point : points) {
			if (point.getGroup() == Group.POI) {
				OpenstreetmapPoint p = (OpenstreetmapPoint) point;
				WptPt wpt = new WptPt();
				wpt.setName(p.getTagsString());
				wpt.setLat(p.getLatitude());
				wpt.setLon(p.getLongitude());
				wpt.setDesc("id: " + p.getId() +
						" node" + " " + OsmPoint.stringAction.get(p.getAction()));
				gpx.addPoint(wpt);
			} else if (point.getGroup() == Group.BUG) {
				OsmNotesPoint p = (OsmNotesPoint) point;
				WptPt wpt = new WptPt();
				wpt.setName(p.getText());
				wpt.setLat(p.getLatitude());
				wpt.setLon(p.getLongitude());
				wpt.setDesc("id: " + p.getId() +
						" note" + " " + OsmPoint.stringAction.get(p.getAction()));
				gpx.addPoint(wpt);
			}
		}
		Exception exception = SharedUtil.writeGpxFile(srcFile, gpx);
		if (exception != null) {
			return exception.getMessage();
		}
		return null;
	}

	private String saveOscFile(OsmPoint[] points) {
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(srcFile);
			XmlSerializer sz = Xml.newSerializer();

			sz.setOutput(out, "UTF-8");
			sz.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
			sz.startDocument("UTF-8", true);
			sz.startTag("", "osmChange");
			sz.attribute("", "generator", "OsmAnd");
			sz.attribute("", "version", "0.6");
			sz.startTag("", "create");
			writeContent(sz, points, OsmPoint.Action.CREATE);
			sz.endTag("", "create");
			sz.startTag("", "modify");
			writeContent(sz, points, OsmPoint.Action.MODIFY);
			writeContent(sz, points, OsmPoint.Action.REOPEN);

			sz.endTag("", "modify");
			sz.startTag("", "delete");
			writeContent(sz, points, OsmPoint.Action.DELETE);
			sz.endTag("", "delete");
			sz.endTag("", "osmChange");
			sz.endDocument();
		} catch (Exception e) {
			return e.getMessage();
		} finally {
			Algorithms.closeStream(out);
		}
		return null;
	}

	private String getFileName(int exportType) {
		StringBuilder sb = new StringBuilder();
		if (exportType == OsmEditsFragment.EXPORT_TYPE_POI) {
			sb.append("osm_edits_modification");
		} else if (exportType == OsmEditsFragment.EXPORT_TYPE_NOTES) {
			sb.append("osm_notes_modification");
		} else {
			sb.append("osm_modification");
		}
		sb.append(oscFile ? ".osc" : IndexConstants.GPX_FILE_EXT);
		return sb.toString();
	}

	private void writeContent(XmlSerializer sz, OsmPoint[] points, OsmPoint.Action a) throws IllegalArgumentException, IllegalStateException, IOException {
		for (OsmPoint point : points) {
			if (point.getGroup() == Group.POI) {
				OpenstreetmapPoint p = (OpenstreetmapPoint) point;
				if (p.getAction() == a) {
					Entity entity = p.getEntity();
					if (entity instanceof Node) {
						writeNode(sz, (Node) entity);
					}
				}
			} else if (point.getGroup() == Group.BUG) {
				OsmNotesPoint p = (OsmNotesPoint) point;
				if (p.getAction() == a) {
					sz.startTag("", "note");
					sz.attribute("", "lat", p.getLatitude() + "");
					sz.attribute("", "lon", p.getLongitude() + "");
					sz.attribute("", "id", p.getId() + "");
					sz.startTag("", "comment");
					sz.attribute("", "text", p.getText() + "");
					sz.endTag("", "comment");
					sz.endTag("", "note");
				}
			}
		}
	}

	private void writeNode(XmlSerializer sz, Node p) {
		try {
			sz.startTag("", "node");
			sz.attribute("", "lat", p.getLatitude() + "");
			sz.attribute("", "lon", p.getLongitude() + "");
			sz.attribute("", "id", p.getId() + "");
			sz.attribute("", "version", "1");
			writeTags(sz, p);
			sz.endTag("", "node");
		} catch (IOException e) {
			log.error(e);
		}
	}

	private void writeTags(XmlSerializer sz, Entity p) {
		for (String tag : p.getTagKeySet()) {
			String val = p.getTag(tag);
			if (p.isNotValid(tag)) {
				continue;
			}
			try {
				sz.startTag("", "tag");
				sz.attribute("", "k", tag);
				sz.attribute("", "v", val);
				sz.endTag("", "tag");
			} catch (IOException e) {
				log.error(e);
			}
		}
	}

	@Override
	protected void onPreExecute() {
		if (listener != null) {
			listener.shareOsmPointsStarted();
		}
	}

	@Override
	protected void onPostExecute(String result) {
		if (listener != null) {
			listener.shareOsmPointsFinished();
		}
		if (result != null) {
			app.showToastMessage(app.getString(R.string.local_osm_changes_backup_failed) + " " + result);
		} else {
			Intent sendIntent = new Intent();
			sendIntent.setAction(Intent.ACTION_SEND);
			sendIntent.putExtra(Intent.EXTRA_SUBJECT, app.getString(R.string.share_osm_edits_subject));
			sendIntent.putExtra(Intent.EXTRA_STREAM, AndroidUtils.getUriForFile(app, srcFile));
			sendIntent.setType("text/plain");
			sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

			Intent chooserIntent = Intent.createChooser(sendIntent, app.getString(R.string.share_osm_edits_subject));
			AndroidUtils.startActivityIfSafe(app, chooserIntent);
		}
	}

	public interface ShareOsmPointsListener {

		void shareOsmPointsStarted();

		void shareOsmPointsFinished();
	}
}
