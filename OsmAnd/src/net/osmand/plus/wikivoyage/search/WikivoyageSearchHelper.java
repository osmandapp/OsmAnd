package net.osmand.plus.wikivoyage.search;

import android.support.annotation.Nullable;

import net.osmand.ResultMatcher;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.wikivoyage.data.WikivoyageSearchResult;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WikivoyageSearchHelper {

	private static final int TIMEOUT_BETWEEN_CHARS = 700;
	private static final int SLEEP_TIME = 50;

	private ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();

	private OsmandApplication application;
	private SearchListener listener;

	WikivoyageSearchHelper(OsmandApplication application) {
		this.application = application;
	}

	public void registerListener(SearchListener listener) {
		this.listener = listener;
	}

	public void unregisterListener() {
		this.listener = null;
	}

	public void search(final String query, final ResultMatcher<WikivoyageSearchResult> rm) {
		singleThreadExecutor.submit(new Runnable() {
			@Override
			public void run() {
				try {
					if (listener != null) {
						listener.onSearchStarted();
					}

					rm.publish(null);

					long startTime = System.currentTimeMillis();
					while (System.currentTimeMillis() - startTime <= TIMEOUT_BETWEEN_CHARS) {
						if (rm.isCancelled()) {
							return;
						}
						Thread.sleep(SLEEP_TIME);
					}

					final List<WikivoyageSearchResult> results = application.getWikivoyageDbHelper().search(query);

					if (listener != null) {
						listener.onSearchFinished(results);
					}
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
