package net.osmand.plus.wikivoyage.search;

import net.osmand.ResultMatcher;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.wikivoyage.data.TravelObfHelper;
import net.osmand.plus.wikivoyage.data.WikivoyageSearchResult;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class WikivoyageSearchHelper {

	private static final int TIMEOUT_BETWEEN_CHARS = 700;
	private static final int SLEEP_TIME = 50;

	private final ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();

	private final OsmandApplication application;
	private final AtomicInteger requestNumber = new AtomicInteger();

	WikivoyageSearchHelper(OsmandApplication application) {
		this.application = application;
	}

	public void search(String query, ResultMatcher<List<WikivoyageSearchResult>> rm) {
		int req = requestNumber.incrementAndGet();
		if (application.getTravelHelper() instanceof TravelObfHelper) {
			((TravelObfHelper) application.getTravelHelper()).requestNumber = req;
		}

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
						List<WikivoyageSearchResult> results = application.getTravelHelper().search(query, request);
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
