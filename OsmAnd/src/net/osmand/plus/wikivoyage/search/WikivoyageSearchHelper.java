package net.osmand.plus.wikivoyage.search;

import android.support.annotation.Nullable;

import net.osmand.ResultMatcher;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.wikivoyage.data.WikivoyageSearchResult;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WikivoyageSearchHelper {

	private static final int TIMEOUT_BETWEEN_CHARS = 700;
	private static final int SLEEP_TIME = 50;

	private ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();

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

	public void search(final String query, final ResultMatcher<WikivoyageSearchResult> rm) {
		singleThreadExecutor.submit(new Runnable() {
			@Override
			public void run() {
				try {
					application.runInUIThread(new Runnable() {
						@Override
						public void run() {
							for (SearchListener listener : listeners) {
								listener.onSearchStarted();
							}
						}
					});

					rm.publish(null);

					long startTime = System.currentTimeMillis();
					while (System.currentTimeMillis() - startTime <= TIMEOUT_BETWEEN_CHARS) {
						if (rm.isCancelled()) {
							return;
						}
						Thread.sleep(SLEEP_TIME);
					}

					final List<WikivoyageSearchResult> results = application.getWikivoyageDbHelper().search(query);

					application.runInUIThread(new Runnable() {
						@Override
						public void run() {
							for (SearchListener listener : listeners) {
								listener.onSearchFinished(results);
							}
						}
					});
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
	}

	public interface SearchListener {

		void onSearchStarted();

		void onSearchFinished(@Nullable List<WikivoyageSearchResult> results);
	}
}
