package net.osmand.plus.plugins.osmedit.asynctasks;

import android.app.Activity;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.plugins.osmedit.UploadVisibility;
import net.osmand.plus.plugins.osmedit.helpers.OpenstreetmapRemoteUtil;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.shared.gpx.GpxDataItem;
import net.osmand.shared.gpx.GpxDbHelper;
import net.osmand.shared.gpx.GpxParameter;
import net.osmand.shared.io.KFile;
import net.osmand.util.Algorithms;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class UploadGPXFilesTask extends AsyncTask<File, String, String> {

	private final OsmandApplication app;
	private final GpxDbHelper gpxDbHelper;
	private final OpenstreetmapRemoteUtil remoteUtil;
	private final WeakReference<Activity> activityRef;

	private final String commonTags;
	private final String visibility;
	private final String commonDescription;
	private final String defaultActivity;
	private final UploadGpxListener listener;

	public UploadGPXFilesTask(@NonNull Activity activity,
	                          @NonNull String commonDescription,
	                          @NonNull String commonTags,
	                          @Nullable UploadVisibility visibility,
	                          @Nullable UploadGpxListener listener,
	                          @Nullable String defaultActivity) {
		app = (OsmandApplication) activity.getApplication();
		this.gpxDbHelper = app.getGpxDbHelper();
		this.remoteUtil = new OpenstreetmapRemoteUtil(app);
		this.activityRef = new WeakReference<>(activity);
		this.commonDescription = commonDescription;
		this.commonTags = commonTags;
		this.visibility = visibility != null ? visibility.asUrlParam() : UploadVisibility.PRIVATE.asUrlParam();
		this.listener = listener;
		this.defaultActivity = defaultActivity;
	}

	@Override
	protected String doInBackground(File... params) {
		int count = 0;
		int total = 0;
		for (File file : params) {
			if (!isCancelled() && file != null) {
				String activity = getGpxActivity(file);
				String tags = getGpxTags(activity);
				String description = getGpxDescription(file);
				String warning = remoteUtil.uploadGPXFile(tags, description, visibility, file);
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

	@NonNull
	private String getGpxDescription(@NonNull File file) {
		return Algorithms.isEmpty(commonDescription.trim())
				? Algorithms.getFileNameWithoutExtension(file.getName())
				: commonDescription;
	}

	@NonNull
	private String getGpxTags(@Nullable String activity) {
		if (Algorithms.isEmpty(activity)) {
			return removeDefaultActivity();
		} else if (!commonTags.contains(activity)) {
			return addActivity(activity);
		}
		return commonTags;
	}

	public String removeDefaultActivity() {
		String removedDefaultActivity = commonTags;
		if (!Algorithms.isEmpty(defaultActivity) && commonTags.contains(defaultActivity)) {
			List<String> tagList = Arrays.stream(commonTags.split(","))
					.map(String::trim)
					.filter(tag -> !tag.equals(defaultActivity))
					.collect(Collectors.toList());

			removedDefaultActivity = String.join(", ", tagList);
		}
		return removedDefaultActivity;
	}

	public String addActivity(@Nullable String activity) {
		if (!Algorithms.isEmpty(defaultActivity) && commonTags.contains(defaultActivity)) {
			return commonTags.replace(defaultActivity, activity);
		} else {
			return commonTags + ", " + activity;
		}
	}

	@Nullable
	private String getGpxActivity(@NonNull File file) {
		GpxDataItem item = gpxDbHelper.getItem(new KFile(file.getPath()));
		return item != null ? item.getParameter(GpxParameter.ACTIVITY_TYPE) : null;
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