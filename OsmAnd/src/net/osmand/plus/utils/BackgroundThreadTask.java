package net.osmand.plus.utils;

import android.os.AsyncTask;

import androidx.annotation.Nullable;

public class BackgroundThreadTask<T> extends AsyncTask<Object, Void, T> {

	private final BackgroundThreadExecuteSource<T> executeSource;

	public BackgroundThreadTask(@Nullable BackgroundThreadExecuteSource<T> listener) {
		this.executeSource = listener;
	}

	@Override
	protected void onPreExecute() {
		if (executeSource != null) {
			executeSource.onPreExecute();
		}
	}

	@Override
	protected T doInBackground(Object... params) {
		if (executeSource != null) {
			return executeSource.onBackground();
		} else {
			return null;
		}
	}

	@Override
	protected void onPostExecute(T result) {
		if (executeSource != null) {
			executeSource.onPostExecute(result);
		}
	}

	public interface BackgroundThreadExecuteSource<T> {

		default void onPreExecute() {
		}

		@Nullable
		default T onBackground() {
			return null;
		}

		default void onPostExecute(@Nullable T result) {
		}
	}
}
