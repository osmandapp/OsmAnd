package net.osmand.plus.plugins.osmedit.asynctasks;

import android.app.Activity;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.track.helpers.GPXInfo;
import net.osmand.plus.plugins.osmedit.OsmEditingPlugin.UploadVisibility;
import net.osmand.plus.plugins.osmedit.helpers.OpenstreetmapRemoteUtil;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.util.Algorithms;

import java.io.File;
import java.lang.ref.WeakReference;

public class UploadGPXFilesTask extends AsyncTask<GPXInfo, String, String> {

	private final OsmandApplication app;
	private final WeakReference<Activity> activityRef;

	private final String tags;
	private final String visibility;
	private final String commonDescription;
	private final UploadGpxListener listener;

	public UploadGPXFilesTask(@NonNull Activity activity,
	                          @NonNull String commonDescription,
	                          @NonNull String tags,
	                          @Nullable UploadVisibility visibility,
	                          @Nullable UploadGpxListener listener) {
		app = (OsmandApplication) activity.getApplication();
		this.activityRef = new WeakReference<>(activity);
		this.commonDescription = commonDescription;
		this.tags = tags;
		this.visibility = visibility != null ? visibility.asUrlParam() : UploadVisibility.PRIVATE.asUrlParam();
		this.listener = listener;
	}

	@Override
	protected String doInBackground(GPXInfo... params) {
		int count = 0;
		int total = 0;
		for (GPXInfo info : params) {
			if (!isCancelled() && info.getFile() != null) {
				File file = info.getFile();
				OpenstreetmapRemoteUtil remoteUtil = new OpenstreetmapRemoteUtil(app);
				String gpxDescription = Algorithms.isEmpty(commonDescription.trim())
						? Algorithms.getFileNameWithoutExtension(info.getFileName())
						: commonDescription;
				String warning = remoteUtil.uploadGPXFile(tags, gpxDescription, visibility, file);
				total++;
				if (warning == null) {
					count++;
				} else {
					publishProgress(warning);
				}
			}
		}
		return app.getString(R.string.local_index_items_uploaded, count, total);
	}

	@Override
	protected void onProgressUpdate(String... values) {
		if (values.length > 0) {
			StringBuilder b = new StringBuilder();
			for (int i = 0; i < values.length; i++) {
				if (i > 0) {
					b.append("\n");
				}
				b.append(values[i]);
			}
			app.showToastMessage(b.toString());
		}
	}

	@Override
	protected void onPreExecute() {
		Activity activity = activityRef.get();
		if (AndroidUtils.isActivityNotDestroyed(activity)) {
			activity.setProgressBarIndeterminateVisibility(true);
		}
	}

	@Override
	protected void onPostExecute(String result) {
		if (listener != null) {
			listener.onGpxUploaded(result);
		}
		Activity activity = activityRef.get();
		if (AndroidUtils.isActivityNotDestroyed(activity)) {
			activity.setProgressBarIndeterminateVisibility(false);
		}
		app.showToastMessage(result);
	}

	public interface UploadGpxListener {
		void onGpxUploaded(String result);
	}
}