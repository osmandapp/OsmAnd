package net.osmand.plus.dashboard;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.ProgressImplementation;
import net.osmand.plus.R;

import java.util.ArrayList;

public class ReloadData extends AsyncTask<Void, Void, Boolean> {

	private final OsmandApplication app;
	private final Context ctx;
	private ProgressImplementation progress;

	public ReloadData(Context ctx, OsmandApplication app) {
		this.ctx = ctx;
		this.app = app;
	}

	@Override
	protected void onPreExecute() {
		progress = ProgressImplementation.createProgressDialog(ctx, ctx.getString(R.string.loading_data),
				ctx.getString(R.string.loading_data), ProgressDialog.STYLE_HORIZONTAL);
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
