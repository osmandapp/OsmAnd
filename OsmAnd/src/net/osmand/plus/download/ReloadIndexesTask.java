package net.osmand.plus.download;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.IProgress;
import net.osmand.plus.OsmandApplication;

import java.util.ArrayList;
import java.util.List;

public class ReloadIndexesTask extends AsyncTask<Void, String, List<String>> {

	private final OsmandApplication app;
	private final ReloadIndexesListener listener;

	public ReloadIndexesTask(@NonNull OsmandApplication app, @Nullable ReloadIndexesListener listener) {
		this.app = app;
		this.listener = listener;
	}

	@Override
	protected void onPreExecute() {
		if (listener != null) {
			listener.reloadIndexesStarted();
		}
	}

	@Override
	protected List<String> doInBackground(Void... params) {
		return app.getResourceManager().reloadIndexes(IProgress.EMPTY_PROGRESS, new ArrayList<String>());
	}

	@Override
	protected void onPostExecute(List<String> warnings) {
		if (listener != null) {
			listener.reloadIndexesFinished(warnings);
		}
	}

	public interface ReloadIndexesListener {

		void reloadIndexesStarted();

		void reloadIndexesFinished(List<String> warnings);
	}
}