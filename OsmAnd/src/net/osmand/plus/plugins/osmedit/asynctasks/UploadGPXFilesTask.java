package net.osmand.plus.plugins.osmedit.asynctasks;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.plugins.osmedit.UploadVisibility;
import net.osmand.plus.plugins.osmedit.helpers.OpenstreetmapRemoteUtil;
import net.osmand.shared.gpx.GpxDataItem;
import net.osmand.shared.gpx.GpxDbHelper;
import net.osmand.shared.gpx.GpxParameter;
import net.osmand.shared.io.KFile;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.Arrays;
import java.util.stream.Collectors;

public class UploadGPXFilesTask extends AsyncTask<File, String, String> {

	private final OsmandApplication app;
	private final GpxDbHelper gpxDbHelper;
	private final OpenstreetmapRemoteUtil remoteUtil;

	private final String tags;
	private final String visibility;
	private final String description;
	private final String defaultActivity;
	private final UploadGpxListener listener;

	public UploadGPXFilesTask(@NonNull OsmandApplication app, @NonNull String tags,
			@NonNull String description, @Nullable String defaultActivity,
			@Nullable UploadVisibility visibility, @Nullable UploadGpxListener listener) {
		this.app = app;
		this.gpxDbHelper = app.getGpxDbHelper();
		this.remoteUtil = new OpenstreetmapRemoteUtil(app);
		this.tags = tags;
		this.description = description;
		this.defaultActivity = defaultActivity;
		this.visibility = visibility != null ? visibility.asUrlParam() : UploadVisibility.PRIVATE.asUrlParam();
		this.listener = listener;
	}

	@Override
	protected void onPreExecute() {
		if (listener != null) {
			listener.onGpxUploadStarted();
		}
	}

	@Override
	protected String doInBackground(File... params) {
		int count = 0;
		int total = params.length;
		boolean includeActivity = shouldIncludeActivity();

		for (File file : params) {
			if (isCancelled() || file == null) continue;
			String updatedTags = adjustTags(file, includeActivity);
			String fileDescription = getGpxDescription(file);
			String warning = remoteUtil.uploadGPXFile(updatedTags, fileDescription, visibility, file);

			if (warning == null) {
				count++;
			} else {
				publishProgress(warning);
			}
		}
		return app.getString(R.string.local_index_items_uploaded, count, total);
	}

	private boolean shouldIncludeActivity() {
		return !Algorithms.isEmpty(defaultActivity) && tags.contains(defaultActivity);
	}

	@NonNull
	private String getGpxDescription(@NonNull File file) {
		return Algorithms.isEmpty(description.trim())
				? Algorithms.getFileNameWithoutExtension(file.getName()) : description;
	}

	@NonNull
	private String adjustTags(@NonNull File file, boolean includeActivity) {
		if (includeActivity) {
			String activity = getGpxActivity(file);
			if (Algorithms.isEmpty(activity)) {
				return removeDefaultActivity();
			}
			return tags.contains(activity) ? tags : tags.replaceFirst(defaultActivity, activity);
		}
		return tags;
	}

	@NonNull
	private String removeDefaultActivity() {
		return Arrays.stream(tags.split(","))
				.map(String::trim)
				.filter(tag -> !tag.equals(defaultActivity))
				.collect(Collectors.joining(", "));
	}

	@Nullable
	private String getGpxActivity(@NonNull File file) {
		GpxDataItem item = gpxDbHelper.getItem(new KFile(file.getPath()));
		return item != null ? item.getParameter(GpxParameter.ACTIVITY_TYPE) : null;
	}

	@Override
	protected void onProgressUpdate(String... values) {
		if (values.length > 0) {
			app.showToastMessage(String.join("\n", values));
		}
	}

	@Override
	protected void onPostExecute(String result) {
		if (listener != null) {
			listener.onGpxUploadFinished(result);
		}
		app.showToastMessage(result);
	}

	public interface UploadGpxListener {
		default void onGpxUploadStarted() {
		}

		void onGpxUploadFinished(String result);
	}
}