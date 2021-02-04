package net.osmand.plus.settings.datastorage.task;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.ProgressImplementation;
import net.osmand.plus.R;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class ReloadDataTask extends AsyncTask<Void, Void, Boolean> {
	private WeakReference<Context> ctx;
	protected ProgressImplementation progress;
	private OsmandApplication app;

	public ReloadDataTask(Context ctx, OsmandApplication app) {
		this.ctx = new WeakReference<>(ctx);
		this.app = app;
	}

	@Override
	protected void onPreExecute() {
		Context c = ctx.get();
		if (c == null) {
			return;
		}
		progress = ProgressImplementation.createProgressDialog(c, c.getString(R.string.loading_data),
				c.getString(R.string.loading_data), ProgressDialog.STYLE_HORIZONTAL);
	}

	@Override
	protected void onPostExecute(Boolean result) {
		try {
			if (progress.getDialog().isShowing()) {
				progress.getDialog().dismiss();
			}
		} catch (Exception e) {
			//ignored
		}
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		app.getResourceManager().reloadIndexes(progress, new ArrayList<String>());
		return true;
	}
}
