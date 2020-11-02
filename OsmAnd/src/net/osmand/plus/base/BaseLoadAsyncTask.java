package net.osmand.plus.base;

import android.app.ProgressDialog;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;

import java.lang.ref.WeakReference;

public abstract class BaseLoadAsyncTask<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> {

	protected OsmandApplication app;
	protected WeakReference<FragmentActivity> activityRef;
	protected ProgressDialog progress;

	public BaseLoadAsyncTask(@NonNull FragmentActivity activity) {
		app = (OsmandApplication) activity.getApplicationContext();
		activityRef = new WeakReference<>(activity);
	}

	@Override
	protected void onPreExecute() {
		showProgress();
	}

	protected void showProgress() {
		FragmentActivity activity = activityRef.get();
		if (AndroidUtils.isActivityNotDestroyed(activity)) {
			String title = app.getString(R.string.loading_smth, "");
			progress = ProgressDialog.show(activity, title, app.getString(R.string.loading_data));
		}
	}

	protected void hideProgress() {
		FragmentActivity activity = activityRef.get();
		if (progress != null && AndroidUtils.isActivityNotDestroyed(activity)) {
			progress.dismiss();
		}
	}
}