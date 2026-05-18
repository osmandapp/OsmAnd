package net.osmand.plus.plugins.audionotes;

import static net.osmand.plus.plugins.audionotes.NotesFragment.SHARE_LOCATION_FILE;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;

import androidx.annotation.NonNull;

import net.osmand.plus.shared.SharedUtil;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.plugins.audionotes.AudioVideoNotesPlugin.Recording;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.util.Algorithms;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class ShareRecordingsTask extends AsyncTask<Void, Void, List<Uri>> {

	private final OsmandApplication app;
	private final WeakReference<Activity> activityRef;
	private final AudioVideoNotesPlugin plugin;
	private final Set<Recording> recordings;

	private ProgressDialog progressDialog;

	public ShareRecordingsTask(@NonNull Activity activity,
	                           @NonNull AudioVideoNotesPlugin plugin,
	                           @NonNull Set<Recording> recordings) {
		this.app = ((OsmandApplication) activity.getApplication());
		this.activityRef = new WeakReference<>(activity);
		this.plugin = plugin;
		this.recordings = recordings;
	}

	@Override
	protected void onPreExecute() {
		Activity activity = activityRef.get();
		if (activity != null) {
			String dialogTitle = activity.getString(R.string.loading_smth, "");
			String dialogMessage = activity.getString(R.string.loading_data);
			progressDialog = ProgressDialog.show(activity, dialogTitle, dialogMessage);
		}
	}

	@NonNull
	@Override
	protected List<Uri> doInBackground(Void... voids) {
		List<Uri> uris = new ArrayList<>();
		for (Recording recording : recordings) {
			if (isCancelled()) {
				break;
			}

			File file = recording == SHARE_LOCATION_FILE ? generateGpxFileForRecordings() : recording.getFile();
			if (file != null) {
				uris.add(AndroidUtils.getUriForFile(app, file));
			}
		}
		return uris;
	}

	@NonNull
	private File generateGpxFileForRecordings() {
		File tmpFile = new File(app.getCacheDir(), "share/noteLocations.gpx");
		tmpFile.getParentFile().mkdirs();
		GpxFile gpxFile = new GpxFile(Version.getFullVersion(app));
		for (Recording recording : getRecordingsForGpx()) {

			if (isCancelled()) {
				return tmpFile;
			}

			if (recording != SHARE_LOCATION_FILE) {
				String desc = recording.getDescriptionName(recording.getFileName());
				if (desc == null) {
					desc = recording.getFileName();
				}
				WptPt wpt = new WptPt();
				wpt.setLat(recording.getLatitude());
				wpt.setLon(recording.getLongitude());
				wpt.setName(desc);
				wpt.setLink(recording.getFileName());
				wpt.setTime(recording.getFile().lastModified());
				wpt.setCategory(recording.getSearchHistoryType());
				wpt.setDesc(recording.getTypeWithDuration(app));
				app.getSelectedGpxHelper().addPoint(wpt, gpxFile);
			}
		}
		SharedUtil.writeGpxFile(tmpFile, gpxFile);
		return tmpFile;
	}

	@NonNull
	private Set<Recording> getRecordingsForGpx() {
		return recordings.size() == 1 && recordings.contains(SHARE_LOCATION_FILE)
				? new HashSet<>(plugin.getAllRecordings())
				: recordings;
	}

	@Override
	protected void onPostExecute(@NonNull List<Uri> uris) {
		Activity activity = activityRef.get();
		if (!AndroidUtils.isActivityNotDestroyed(activity)) {
			return;
		}

		if (progressDialog != null) {
			progressDialog.dismiss();
		}

		if (!Algorithms.isEmpty(uris)) {
			Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
			intent.setType("*/*");
			intent.putExtra(Intent.EXTRA_STREAM, new ArrayList<>(uris));
			intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
			Intent chooserIntent = Intent.createChooser(intent, app.getString(R.string.share_note));
			AndroidUtils.startActivityIfSafe(activity, intent, chooserIntent);
		}
	}
}