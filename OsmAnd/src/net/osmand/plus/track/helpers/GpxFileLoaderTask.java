package net.osmand.plus.track.helpers;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.CallbackWithObject;
import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.shared.SharedUtil;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.plus.R;
import net.osmand.plus.utils.AndroidUtils;

import java.io.File;
import java.io.InputStream;
import java.lang.ref.WeakReference;

@SuppressWarnings("deprecation")
public class GpxFileLoaderTask extends AsyncTask<Void, Void, GpxFile> {

	private final File file;
	private final InputStream inputStream;
	private final CallbackWithObject<GpxFile> callback;

	private final WeakReference<Activity> activityRef;
	private ProgressDialog progressDialog;

	/**
	 * @param progressContext in case {@link android.app.ProgressDialog} required
	 */
	public static void loadGpxFile(@NonNull File file,
	                               @Nullable Activity progressContext,
	                               @Nullable CallbackWithObject<GpxFile> callback) {
		OsmAndTaskManager.executeTask(new GpxFileLoaderTask(file, progressContext, callback));
	}

	/**
	 * @param progressContext in case {@link android.app.ProgressDialog} required
	 */
	public static void loadGpxFile(@NonNull InputStream inputStream,
	                               @Nullable Activity progressContext,
	                               @Nullable CallbackWithObject<GpxFile> callback) {
		OsmAndTaskManager.executeTask(new GpxFileLoaderTask(inputStream, progressContext, callback));
	}

	/**
	 * @param progressContext in case {@link android.app.ProgressDialog} required
	 */
	public GpxFileLoaderTask(@NonNull File file,
	                         @Nullable Activity progressContext,
	                         @Nullable CallbackWithObject<GpxFile> callback) {
		this.file = file;
		this.inputStream = null;
		this.callback = callback;
		this.activityRef = new WeakReference<>(progressContext);
	}

	private GpxFileLoaderTask(@NonNull InputStream inputStream,
	                          @Nullable Activity progressContext,
	                          @Nullable CallbackWithObject<GpxFile> callback) {
		this.file = null;
		this.inputStream = inputStream;
		this.callback = callback;
		this.activityRef = new WeakReference<>(progressContext);
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		Activity activity = activityRef.get();
		if (AndroidUtils.isActivityNotDestroyed(activity)) {
			String dialogTitle = activity.getString(R.string.loading_smth, "");
			String dialogMessage = activity.getString(R.string.loading_data);
			progressDialog = ProgressDialog.show(activity, dialogTitle, dialogMessage);
		}
	}

	@Override
	protected GpxFile doInBackground(Void... voids) {
		return file != null
				? SharedUtil.loadGpxFile(file) : SharedUtil.loadGpxFile(inputStream);
	}

	@Override
	protected void onPostExecute(GpxFile gpxFile) {
		if (progressDialog != null && AndroidUtils.isActivityNotDestroyed(activityRef.get())) {
			progressDialog.dismiss();
		}
		if (callback != null) {
			callback.processResult(gpxFile);
		}
	}
}