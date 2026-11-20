package net.osmand.binary;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.binary.OsmandIndex.FileIndex;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The readers the app copies from a cached reader share its {@link RouteRegion}s, and the first of them
 * to need a region reads its encoding rules ({@link BinaryMapIndexReader#initRouteRegion}). A reader that
 * came while the rules were still being read took the half-filled list for a complete one, and routing
 * failed with an IndexOutOfBoundsException in {@link RouteRegion#quickGetEncodingRule} (#26498).
 * <pre>
 * ./gradlew :OsmAnd-java:test --tests "net.osmand.binary.RouteRegionInitTest"
 * </pre>
 */
public class RouteRegionInitTest {

	private static final File MAP = new File("src/test/resources/routing/Routing_test_archive.obf");
	private static final long SECOND_READER_WAIT_MS = 500;

	@Test(timeout = 30_000)
	public void secondReaderGetsAllEncodingRules() throws Exception {
		BinaryMapIndexReader fullReader = new BinaryMapIndexReader(new RandomAccessFile(MAP, "r"), MAP);
		FileIndex fileIndex = new CachedOsmandIndexes().addToCache(fullReader, MAP);
		fullReader.close();
		BinaryMapIndexReader cached = new CachedOsmandIndexes().initReaderFromFileIndex(fileIndex,
				new RandomAccessFile(MAP, "r"), MAP);
		RouteRegion region = cached.getRoutingIndexes().get(0);

		CountDownLatch paused = new CountDownLatch(1);
		CountDownLatch resume = new CountDownLatch(1);
		AtomicInteger rulesAtPause = new AtomicInteger();
		// pauses the first reader once it has read some of the rules
		RandomAccessFile pausingFile = new RandomAccessFile(MAP, "r") {
			@Override
			public int read(byte[] b, int off, int len) throws IOException {
				if (paused.getCount() > 0 && region.quickGetEncodingRulesSize() > 0) {
					rulesAtPause.set(region.quickGetEncodingRulesSize());
					paused.countDown();
					try {
						resume.await();
					} catch (InterruptedException e) {
						throw new IOException(e);
					}
				}
				return super.read(b, off, len);
			}
		};
		BinaryMapIndexReader first = new BinaryMapIndexReader(pausingFile, cached);
		BinaryMapIndexReader second = new BinaryMapIndexReader(new RandomAccessFile(MAP, "r"), cached);
		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			Future<?> firstInit = executor.submit(() -> {
				first.initRouteRegion(region);
				return null;
			});
			assertTrue("The first reader did not start reading the rules", paused.await(10, TimeUnit.SECONDS));
			Future<Integer> secondInit = executor.submit(() -> {
				second.initRouteRegion(region);
				return region.quickGetEncodingRulesSize();
			});
			try {
				secondInit.get(SECOND_READER_WAIT_MS, TimeUnit.MILLISECONDS);
			} catch (TimeoutException e) {
				// the second reader waits for the first one
			}
			resume.countDown();
			firstInit.get();
			int allRules = region.quickGetEncodingRulesSize();
			assertTrue("The first reader paused after it had read all " + allRules + " rules",
					rulesAtPause.get() < allRules);
			assertEquals("Rules the second reader got", allRules, (int) secondInit.get());
		} finally {
			resume.countDown();
			executor.shutdownNow();
			first.close();
			second.close();
			cached.close();
		}
	}
}
