package net.osmand.plus.wikivoyage.search;

import android.os.AsyncTask;
import android.support.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.wikivoyage.data.SearchResult;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class WikivoyageSearchHelper {

	private static final int TIMEOUT_BETWEEN_CHARS = 700;
	private static final int SLEEP_TIME = 50;

	private LinkedBlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>();
	private ThreadPoolExecutor singleThreadExecutor =
			new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, workQueue);
	private AsyncTask<Void, Void, List<SearchResult>> currentTask;

	private OsmandApplication application;
	private Set<SearchListener> listeners = new HashSet<>();

	WikivoyageSearchHelper(OsmandApplication application) {
		this.application = application;
	}

	public void registerListener(SearchListener listener) {
		listeners.add(listener);
	}

	public void unregisterListener(SearchListener listener) {
		listeners.remove(listener);
	}

	public void cancelSearch() {
		workQueue.clear();
		if (currentTask != null) {
			currentTask.cancel(true);
		}
	}

	public void search(String query) {
		cancelSearch();
		currentTask = new SearchAsyncTask(query);
		currentTask.executeOnExecutor(singleThreadExecutor);
	}

	public interface SearchListener {

		void onSearchStarted();

		void onSearchFinished(@Nullable List<SearchResult> results, boolean lastTask);
	}

	private class SearchAsyncTask extends AsyncTask<Void, Void, List<SearchResult>> {

		private String query;

		SearchAsyncTask(String query) {
			this.query = query;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			for (SearchListener listener : listeners) {
				listener.onSearchStarted();
			}
		}

		@Override
		protected List<SearchResult> doInBackground(Void... voids) {
			long startTime = System.currentTimeMillis();
			while (System.currentTimeMillis() - startTime <= TIMEOUT_BETWEEN_CHARS) {
				if (isCancelled()) {
					return null;
				}
				try {
					Thread.sleep(SLEEP_TIME);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			return application.getWikivoyageDbHelper().search(query);
		}

		@Override
		protected void onPostExecute(List<SearchResult> results) {
			super.onPostExecute(results);
			for (SearchListener listener : listeners) {
				listener.onSearchFinished(results, workQueue.isEmpty());
			}
		}
	}
}
