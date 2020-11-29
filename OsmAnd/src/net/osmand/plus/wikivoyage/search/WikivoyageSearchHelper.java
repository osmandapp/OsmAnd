package net.osmand.plus.wikivoyage.search;

import net.osmand.ResultMatcher;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.wikivoyage.data.WikivoyageSearchResult;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class WikivoyageSearchHelper {

	private static final int TIMEOUT_BETWEEN_CHARS = 700;
	private static final int SLEEP_TIME = 50;

	private ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();

	private OsmandApplication application;
	private AtomicInteger requestNumber = new AtomicInteger();

	WikivoyageSearchHelper(OsmandApplication application) {
		this.application = application;
	}

	public void search(final String query, final ResultMatcher<List<WikivoyageSearchResult>> rm) {
		final int req = requestNumber.incrementAndGet();

		singleThreadExecutor.submit(new Runnable() {

			final int request = req;

			@Override
			public void run() {
				try {
					long startTime = System.currentTimeMillis();
					while (System.currentTimeMillis() - startTime <= TIMEOUT_BETWEEN_CHARS) {
						if (isCancelled()) {
							return;
						}
						Thread.sleep(SLEEP_TIME);
					}

					if (!isCancelled()) {
						List<WikivoyageSearchResult> results = application.getTravelHelper().search(query);
						if (!isCancelled()) {
							rm.publish(results);
						}
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			private boolean isCancelled() {
				return requestNumber.get() != request || rm.isCancelled();
			}
		});
	}
}
