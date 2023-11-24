package net.osmand.plus.plugins.osmedit.asynctasks;

import android.app.Activity;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.plugins.osmedit.OsmEditingPlugin.UploadVisibility;
import net.osmand.plus.plugins.osmedit.helpers.OpenstreetmapRemoteUtil;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.util.Algorithms;

import java.io.File;
import java.lang.ref.WeakReference;

public class UploadGPXFilesTask extends AsyncTask<File, String, String> {

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
	protected String doInBackground(File... params) {
		int count = 0;
		int total = 0;
		for (File file : params) {
			if (!isCancelled() && file != null) {
				OpenstreetmapRemoteUtil remoteUtil = new OpenstreetmapRemoteUtil(app);
				String gpxDescription = Algorithms.isEmpty(commonDescription.trim())
						? Algorithms.getFileNameWithoutExtension(file.getName())
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
			StringBuilder builder = new StringBuilder();
			for (int i = 0; i < values.length; i++) {
				if (i > 0) {
					builder.append("\n");
				}
				builder.append(values[i]);
			}
			app.showToastMessage(builder.toString());
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