package net.osmand.plus.osmedit;

import android.app.Activity;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.myplaces.AvailableGPXFragment.GpxInfo;
import net.osmand.plus.osmedit.OsmEditingPlugin.UploadVisibility;

import java.io.File;
import java.lang.ref.WeakReference;

public class UploadGPXFilesTask extends AsyncTask<GpxInfo, String, String> {

	private final OsmandApplication app;
	private final WeakReference<Activity> activityRef;

	private final String visibility;
	private final String description;
	private final String tagstring;

	public UploadGPXFilesTask(@NonNull Activity activity, String description, String tagsString,
							  @Nullable UploadVisibility visibility) {
		app = (OsmandApplication) activity.getApplication();
		this.activityRef = new WeakReference<>(activity);
		this.description = description;
		this.tagstring = tagsString;
		this.visibility = visibility != null ? visibility.asURLparam() : UploadVisibility.PRIVATE.asURLparam();
	}

	@Override
	protected String doInBackground(GpxInfo... params) {
		int count = 0;
		int total = 0;
		for (GpxInfo info : params) {
			if (!isCancelled() && info.file != null) {
				File file = info.file;
				OpenstreetmapRemoteUtil remoteUtil = new OpenstreetmapRemoteUtil(app);
				String warning = remoteUtil.uploadGPXFile(tagstring, description, visibility, file);
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
		Activity activity = activityRef.get();
		if (AndroidUtils.isActivityNotDestroyed(activity)) {
			activity.setProgressBarIndeterminateVisibility(false);
		}
		app.showToastMessage(result);
	}
}