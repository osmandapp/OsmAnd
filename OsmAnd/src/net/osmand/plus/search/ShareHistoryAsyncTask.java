package net.osmand.plus.search;

import android.content.Intent;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.utils.AndroidUtils;
import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.helpers.SearchHistoryHelper.HistoryEntry;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.List;

public class ShareHistoryAsyncTask extends AsyncTask<Void, Void, GPXFile> {

	private final OsmandApplication app;
	private final List<HistoryEntry> historyEntries;
	private final OnShareHistoryListener listener;

	public ShareHistoryAsyncTask(@NonNull OsmandApplication app,
								 @NonNull List<HistoryEntry> historyEntries,
								 @Nullable OnShareHistoryListener listener) {
		this.app = app;
		this.listener = listener;
		this.historyEntries = historyEntries;
	}

	@Override
	protected GPXFile doInBackground(Void... params) {
		GPXFile gpx = new GPXFile(Version.getFullVersion(app));
		for (HistoryEntry h : historyEntries) {
			WptPt pt = new WptPt();
			pt.lat = h.getLat();
			pt.lon = h.getLon();
			pt.name = h.getName().getName();
			boolean hasTypeInDescription = !Algorithms.isEmpty(h.getName().getTypeName());
			if (hasTypeInDescription) {
				pt.desc = h.getName().getTypeName();
			}
			gpx.addPoint(pt);
		}
		return gpx;
	}

	@Override
	protected void onPreExecute() {
		if (listener != null) {
			listener.onShareHistoryStarted();
		}
	}

	@Override
	protected void onPostExecute(GPXFile gpxFile) {
		if (listener != null) {
			listener.onShareHistoryFinished();
		}
		File dir = new File(app.getCacheDir(), "share");
		if (!dir.exists()) {
			dir.mkdir();
		}
		File dst = new File(dir, "History.gpx");
		GPXUtilities.writeGpxFile(dst, gpxFile);

		Intent sendIntent = new Intent();
		sendIntent.setAction(Intent.ACTION_SEND);
		sendIntent.putExtra(Intent.EXTRA_TEXT, "History.gpx:\n\n\n" + GPXUtilities.asString(gpxFile));
		sendIntent.putExtra(Intent.EXTRA_SUBJECT, app.getString(R.string.share_history_subject));
		sendIntent.putExtra(Intent.EXTRA_STREAM, AndroidUtils.getUriForFile(app, dst));
		sendIntent.setType("text/plain");
		sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		AndroidUtils.startActivityIfSafe(app, sendIntent);
	}

	public interface OnShareHistoryListener {

		void onShareHistoryStarted();

		void onShareHistoryFinished();
	}
}
