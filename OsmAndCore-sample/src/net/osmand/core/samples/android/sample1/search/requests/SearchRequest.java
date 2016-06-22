package net.osmand.core.samples.android.sample1.search.requests;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.osmand.core.samples.android.sample1.search.SearchAPI;
import net.osmand.core.samples.android.sample1.search.SearchAPI.SearchCallback;
import net.osmand.core.samples.android.sample1.search.SearchScope;
import net.osmand.core.samples.android.sample1.search.SearchString;
import net.osmand.core.samples.android.sample1.search.items.SearchItem;

import java.util.List;

public abstract class SearchRequest {
	protected SearchAPI searchAPI;
	protected SearchString searchString;
	protected SearchScope searchScope;

	protected int maxSearchResults;
	protected Runnable onFinished;
	protected SearchCallback searchCallback;

	protected boolean cancelled;

	public SearchRequest(@NonNull SearchAPI searchAPI, int maxSearchResults, @Nullable SearchCallback searchCallback) {
		this.searchAPI = searchAPI;
		this.searchString = searchAPI.getSearchString();
		this.searchScope= searchAPI.getSearchScope();
		this.maxSearchResults = maxSearchResults;
		this.searchCallback = searchCallback;
	}

	public void run() {

		new AsyncTask<Void, Void, List<SearchItem>>() {

			@Override
			protected List<SearchItem> doInBackground(Void... params) {
				return doSearch();
			}

			@Override
			protected void onPostExecute(List<SearchItem> searchItems) {

				onSearchRequestPostExecute(searchItems);

				if (onFinished != null) {
					onFinished.run();
				}

				if (searchCallback != null && !cancelled) {
					searchCallback.onSearchFinished(searchItems);
				}
			}
		}.execute();
	}

	protected void onSearchRequestPostExecute(List<SearchItem> searchItems) {
	}

	protected abstract List<SearchItem> doSearch();

	public void cancel() {
		cancelled = true;
	}

	public void setOnFinishedCallback(@Nullable Runnable onFinished) {
		this.onFinished = onFinished;
	}
}
