package net.osmand.core.samples.android.sample1.search.requests;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.osmand.core.samples.android.sample1.search.SearchAPI.SearchApiCallback;
import net.osmand.core.samples.android.sample1.search.SearchScope;
import net.osmand.core.samples.android.sample1.search.objects.SearchObject;

import java.util.List;

public abstract class SearchRequest {
	protected SearchScope searchScope;

	protected int maxSearchResults;
	protected Runnable onFinished;
	protected SearchApiCallback searchCallback;

	protected boolean cancelled;

	public SearchRequest(@NonNull SearchScope searchScope, int maxSearchResults,
						 @Nullable SearchApiCallback searchCallback) {
		this.searchScope = searchScope;
		this.maxSearchResults = maxSearchResults;
		this.searchCallback = searchCallback;
	}

	public void run() {

		new AsyncTask<Void, Void, List<SearchObject>>() {

			@Override
			protected List<SearchObject> doInBackground(Void... params) {
				return doSearch();
			}

			@Override
			protected void onPostExecute(List<SearchObject> searchObjects) {

				onSearchRequestPostExecute(searchObjects);

				if (onFinished != null) {
					onFinished.run();
				}

				if (searchCallback != null && !cancelled) {
					searchCallback.onSearchFinished(searchObjects);
				}
			}
		}.execute();
	}

	protected void onSearchRequestPostExecute(List<SearchObject> searchObjects) {
	}

	protected abstract List<SearchObject> doSearch();

	public void cancel() {
		cancelled = true;
	}

	public void setOnFinishedCallback(@Nullable Runnable onFinished) {
		this.onFinished = onFinished;
	}
}
