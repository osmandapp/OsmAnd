package net.osmand.plus.base;

import android.app.ProgressDialog;
import android.content.DialogInterface.OnCancelListener;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.AndroidUtils;

import java.lang.ref.WeakReference;

public abstract class BaseLoadAsyncTask<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> {

	protected OsmandApplication app;
	protected OsmandSettings settings;
	protected WeakReference<FragmentActivity> activityRef;
	protected ProgressDialog progress;
	private final OnCancelListener cancelListener = dialog -> cancel(false);
	private boolean shouldShowProgress = true;

	public BaseLoadAsyncTask(@NonNull FragmentActivity activity) {
		app = (OsmandApplication) activity.getApplicationContext();
		settings = app.getSettings();
		activityRef = new WeakReference<>(activity);
	}

	public boolean isShouldShowProgress() {
		return shouldShowProgress;
	}

	public void setShouldShowProgress(boolean shouldShowProgress) {
		this.shouldShowProgress = shouldShowProgress;
	}

	@Override
	protected void onPreExecute() {
		if (isShouldShowProgress()) {
			showProgress(false);
		}
	}

	protected void showProgress(boolean cancelableOnTouchOutside) {
		FragmentActivity activity = activityRef.get();
		if (AndroidUtils.isActivityNotDestroyed(activity)) {
			String title = app.getString(R.string.loading_smth, "");
			progress = ProgressDialog.show(activity, title, app.getString(R.string.loading_data));
			if (cancelableOnTouchOutside) {
				progress.setCanceledOnTouchOutside(true);
			}
			progress.setOnCancelListener(cancelListener);
		}
	}

	protected void hideProgress() {
		FragmentActivity activity = activityRef.get();
		if (progress != null && AndroidUtils.isActivityNotDestroyed(activity)) {
			progress.dismiss();
		}
	}
}