package net.osmand.plus.settings.fragments;

import android.os.AsyncTask;

import androidx.annotation.Nullable;

public class DeleteHistoryTask extends AsyncTask<Void, Void, Void> {

	private final DeleteHistoryListener listener;

	public DeleteHistoryTask(@Nullable DeleteHistoryListener listener) {
		this.listener = listener;
	}

	@Override
	protected void onPreExecute() {
		if (listener != null) {
			listener.onStartDelete();

		}
	}

	@Override
	protected Void doInBackground(Void... voids) {

		if (listener != null) {
			listener.deleteItems();
		}

		return null;
	}

	@Override
	protected void onPostExecute(Void unused) {
		if (listener != null) {
			listener.onEndDelete();
		}
	}

	public interface DeleteHistoryListener{
		void onStartDelete();
		void onEndDelete();
		void deleteItems();
	}
}