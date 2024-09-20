package net.osmand.plus.search;

import android.content.Intent;
import android.os.AsyncTask;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.shared.SharedUtil;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.shared.gpx.GpxUtilities;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.helpers.SearchHistoryHelper.HistoryEntry;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.List;

public class ShareHistoryAsyncTask extends AsyncTask<Void, Void, Pair<File, String>> {

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
	protected void onPreExecute() {
		if (listener != null) {
			listener.onShareHistoryStarted();
		}
	}

	@NonNull
	@Override
	protected Pair<File, String> doInBackground(Void... params) {
		GpxFile gpxFile = new GpxFile(Version.getFullVersion(app));
		for (HistoryEntry h : historyEntries) {
			WptPt pt = new WptPt();
			pt.setLat(h.getLat());
			pt.setLon(h.getLon());
			pt.setName(h.getName().getName());
			boolean hasTypeInDescription = !Algorithms.isEmpty(h.getName().getTypeName());
			if (hasTypeInDescription) {
				pt.setDesc(h.getName().getTypeName());
			}
			gpxFile.addPoint(pt);
		}

		File dir = new File(app.getCacheDir(), "share");
		if (!dir.exists()) {
			dir.mkdir();
		}
		File historyFile = new File(dir, "History.gpx");
		SharedUtil.writeGpxFile(historyFile, gpxFile);

		return Pair.create(historyFile, GpxUtilities.INSTANCE.asString(gpxFile));
	}

	@Override
	protected void onPostExecute(@NonNull Pair<File, String> pair) {
		if (listener != null) {
			listener.onShareHistoryFinished();
		}

		Intent sendIntent = new Intent();
		sendIntent.setAction(Intent.ACTION_SEND);
		sendIntent.putExtra(Intent.EXTRA_TEXT, "History.gpx:\n\n\n" + pair.second);
		sendIntent.putExtra(Intent.EXTRA_SUBJECT, app.getString(R.string.share_history_subject));
		sendIntent.putExtra(Intent.EXTRA_STREAM, AndroidUtils.getUriForFile(app, pair.first));
		sendIntent.setType("text/plain");
		sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		Intent chooserIntent = Intent.createChooser(sendIntent, app.getString(R.string.shared_string_share));

		AndroidUtils.startActivityIfSafe(app, chooserIntent);
	}

	public interface OnShareHistoryListener {

		void onShareHistoryStarted();

		void onShareHistoryFinished();
	}
}
