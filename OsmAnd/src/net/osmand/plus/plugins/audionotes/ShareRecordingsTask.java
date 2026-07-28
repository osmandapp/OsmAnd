package net.osmand.plus.plugins.audionotes;

import static net.osmand.plus.plugins.audionotes.NotesFragment.SHARE_LOCATION_FILE;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.shared.SharedUtil;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.primitives.Link;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.shared.media.MediaUriResolver;
import net.osmand.shared.media.domain.MediaItem;
import net.osmand.util.Algorithms;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class ShareRecordingsTask extends AsyncTask<Void, Void, List<Uri>> {

	private final OsmandApplication app;
	private final WeakReference<Activity> activityRef;
	private final Set<MediaNote> notes;
	private final Collection<MediaNote> allNotes;
	private final List<String> shareTexts = new ArrayList<>();

	private ProgressDialog progressDialog;

	public ShareRecordingsTask(@NonNull Activity activity,
	                           @NonNull Set<MediaNote> notes,
	                           @NonNull Collection<MediaNote> allNotes) {
		this.app = ((OsmandApplication) activity.getApplication());
		this.activityRef = new WeakReference<>(activity);
		this.notes = new HashSet<>(notes);
		this.allNotes = new ArrayList<>(allNotes);
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
		for (MediaNote note : notes) {
			if (isCancelled()) {
				break;
			}

			if (note == SHARE_LOCATION_FILE) {
				File file = generateGpxFileForNotes();
				uris.add(AndroidUtils.getUriForFile(app, file));
			} else if (note.isRecording() && note.getRecording() != null) {
				uris.add(AndroidUtils.getUriForFile(app, note.getRecording().getFile()));
			} else if (note.getMediaItem() != null) {
				MediaItem mediaItem = note.getMediaItem();
				Uri uri = app.getGalleryHelper().getMediaSourceResolver().getShareableUri(mediaItem);
				if (uri != null) {
					uris.add(uri);
				} else {
					String shareUri = MediaUriResolver.getShareUri(mediaItem);
					if (!Algorithms.isEmpty(shareUri)) {
						shareTexts.add(shareUri);
					}
				}
			}
		}
		return uris;
	}

	@NonNull
	private File generateGpxFileForNotes() {
		File tmpFile = new File(app.getCacheDir(), "share/noteLocations.gpx");
		tmpFile.getParentFile().mkdirs();
		GpxFile gpxFile = new GpxFile(Version.getFullVersion(app));
		for (MediaNote note : getNotesForGpx()) {

			if (isCancelled()) {
				return tmpFile;
			}

			if (note != SHARE_LOCATION_FILE) {
				WptPt wpt = new WptPt();
				wpt.setLat(note.getLatitude());
				wpt.setLon(note.getLongitude());
				wpt.setName(note.getName(app, app.getGalleryHelper().getMetadataRepository(), false));
				String href = note.isRecording() && note.getRecording() != null
						? note.getRecording().getFileName()
						: note.getLink() != null ? note.getLink().getHref() : null;
				if (!Algorithms.isEmpty(href)) {
					wpt.setLink(new Link(href));
				}
				long lastModified = note.getLastModified(app.getGalleryHelper().getMetadataRepository());
				if (lastModified > 0) {
					wpt.setTime(lastModified);
				}
				wpt.setCategory(note.getSearchHistoryType());
				wpt.setDesc(note.getTypeWithDuration(app, app.getGalleryHelper().getMetadataRepository()));
				app.getSelectedGpxHelper().addPoint(wpt, gpxFile);
			}
		}
		SharedUtil.writeGpxFile(tmpFile, gpxFile);
		return tmpFile;
	}

	@NonNull
	private Collection<MediaNote> getNotesForGpx() {
		return notes.size() == 1 && notes.contains(SHARE_LOCATION_FILE) ? allNotes : notes;
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
			if (!shareTexts.isEmpty()) {
				intent.putExtra(Intent.EXTRA_TEXT, String.join("\n", shareTexts));
			}
			intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
			Intent chooserIntent = Intent.createChooser(intent, app.getString(R.string.share_note));
			AndroidUtils.startActivityIfSafe(activity, intent, chooserIntent);
		} else if (!shareTexts.isEmpty()) {
			Intent intent = new Intent(Intent.ACTION_SEND);
			intent.setType("text/plain");
			intent.putExtra(Intent.EXTRA_TEXT, String.join("\n", shareTexts));
			AndroidUtils.startActivityIfSafe(activity,
					Intent.createChooser(intent, app.getString(R.string.share_note)));
		}
	}
}
