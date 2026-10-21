package net.osmand.core.android;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import net.osmand.data.SourceFingerprint;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

/**
 * Cached Panoramax raster tiles must match their current vector source and follow the same
 * refreshability and expiration semantics as the vector cache.
 */
@RunWith(AndroidJUnit4.class)
public class PanoramaxTilesProviderTest {

	private static final long NOW = 1767312000000L;
	private static final long DAY = TimeUnit.DAYS.toMillis(1);
	private static final long LENGTH = 4096L;
	private static final SourceFingerprint SOURCE = new SourceFingerprint(NOW, LENGTH);
	private static final String FILTER = "v1;enabled=false";

	private static final boolean ONLINE = true;
	private static final boolean OFFLINE = false;

	private static final long CONFIRM = PanoramaxTilesProvider.REFRESH_IDLE_CONFIRM_MS;

	private static final boolean IN_PROGRESS = true;
	private static final boolean IDLE = false;

	@Test
	public void missingSourceIsNeverFresh() {
		assertFalse(PanoramaxTilesProvider.isTileFresh(0, DAY, NOW));
		assertFalse(PanoramaxTilesProvider.isTileFresh(0, -1, NOW));
	}

	@Test
	public void sourceAgedExactlyToTheExpirationIsStillFresh() {
		assertTrue(PanoramaxTilesProvider.isTileFresh(NOW - DAY, DAY, NOW));
		assertFalse(PanoramaxTilesProvider.isTileFresh(NOW - DAY - 1, DAY, NOW));
	}

	@Test
	public void onlyMinusOneMeansTheSourceNeverExpires() {
		assertTrue(PanoramaxTilesProvider.isTileFresh(NOW - 10 * DAY, -1, NOW));
		assertFalse(PanoramaxTilesProvider.isTileFresh(NOW - 1, 0, NOW));
		assertTrue(PanoramaxTilesProvider.isTileFresh(NOW, 0, NOW));
	}

	@Test
	public void missingSourceIsNeverUsable() {
		assertFalse(PanoramaxTilesProvider.isSourceUsable(0, DAY, NOW, ONLINE));
		assertFalse(PanoramaxTilesProvider.isSourceUsable(0, -1, NOW, OFFLINE));
	}

	@Test
	public void offlineKeepsAnExpiredSourceUsable() {
		assertTrue(PanoramaxTilesProvider.isSourceUsable(NOW - 10 * DAY, DAY, NOW, OFFLINE));
	}

	@Test
	public void onlineRejectsAnExpiredSource() {
		assertFalse(PanoramaxTilesProvider.isSourceUsable(NOW - 10 * DAY, DAY, NOW, ONLINE));
		assertTrue(PanoramaxTilesProvider.isSourceUsable(NOW - DAY, DAY, NOW, ONLINE));
	}

	@Test
	public void expiredSourceAwaitsItsRefreshOnlyWhileOneIsPossible() {
		assertTrue(PanoramaxTilesProvider.mustAwaitRefresh(NOW - 10 * DAY, DAY, NOW, ONLINE));
		assertFalse(PanoramaxTilesProvider.mustAwaitRefresh(NOW - 10 * DAY, DAY, NOW, OFFLINE));
	}

	@Test
	public void freshOrMissingSourceNeverAwaitsRefresh() {
		assertFalse(PanoramaxTilesProvider.mustAwaitRefresh(NOW - DAY, DAY, NOW, ONLINE));
		assertFalse(PanoramaxTilesProvider.mustAwaitRefresh(NOW, -1, NOW, ONLINE));
		assertFalse(PanoramaxTilesProvider.mustAwaitRefresh(0, DAY, NOW, ONLINE));
	}

	/** A render may be served exactly when nothing is about to replace what it was made from. */
	@Test
	public void anExistingSourceIsUsableWheneverNoRefreshIsAwaited() {
		for (long modified : new long[] {NOW, NOW - DAY, NOW - DAY - 1, NOW - 10 * DAY}) {
			for (long expiration : new long[] {-1, 0, DAY}) {
				for (boolean useInternet : new boolean[] {ONLINE, OFFLINE}) {
					boolean usable = PanoramaxTilesProvider.isSourceUsable(modified, expiration, NOW, useInternet);
					boolean awaiting = PanoramaxTilesProvider.mustAwaitRefresh(modified, expiration, NOW, useInternet);
					assertTrue(modified + "/" + expiration + "/" + useInternet, usable != awaiting);
				}
			}
		}
	}

	@Test
	public void storedProvenanceMatchingTheSourceIsAHit() {
		assertTrue(PanoramaxTilesProvider.matchesStoredSource(SOURCE, FILTER, SOURCE, FILTER));
	}

	@Test
	public void storedProvenanceOfAnotherSourceVersionIsAMiss() {
		assertFalse(PanoramaxTilesProvider.matchesStoredSource(
				new SourceFingerprint(NOW - 1, LENGTH), FILTER, SOURCE, FILTER));
		assertFalse(PanoramaxTilesProvider.matchesStoredSource(
				new SourceFingerprint(NOW, LENGTH + 1), FILTER, SOURCE, FILTER));
	}

	@Test
	public void storedProvenanceOfAnotherFilterIsAMiss() {
		assertFalse(PanoramaxTilesProvider.matchesStoredSource(SOURCE, "v1;enabled=true", SOURCE, FILTER));
	}

	/** Rows written before this cache stored provenance stay readable PNGs and simply miss. */
	@Test
	public void rowWithoutProvenanceIsAMiss() {
		assertFalse(PanoramaxTilesProvider.matchesStoredSource(SourceFingerprint.EMPTY, null, SOURCE, FILTER));
	}

	@Test
	public void aRefreshInProgressIsNeverFailed() {
		PanoramaxTilesProvider.RefreshWatch watch = new PanoramaxTilesProvider.RefreshWatch();
		assertFalse(watch.hasFailed(IN_PROGRESS, NOW));
		assertFalse(watch.hasFailed(IN_PROGRESS, NOW + 10 * CONFIRM));
	}

	@Test
	public void oneIdleSampleIsNotAFailedRefresh() {
		PanoramaxTilesProvider.RefreshWatch watch = new PanoramaxTilesProvider.RefreshWatch();
		assertFalse(watch.hasFailed(IDLE, NOW));
	}

	/** The downloader leaves both queues while a request moves from pending to downloading. */
	@Test
	public void theHandoffBetweenPendingAndDownloadingIsNotAFailedRefresh() {
		PanoramaxTilesProvider.RefreshWatch watch = new PanoramaxTilesProvider.RefreshWatch();
		assertFalse(watch.hasFailed(IN_PROGRESS, NOW));
		assertFalse(watch.hasFailed(IDLE, NOW + 50));
		assertFalse(watch.hasFailed(IN_PROGRESS, NOW + 100));
		assertFalse(watch.hasFailed(IN_PROGRESS, NOW + 100 + CONFIRM));
	}

	@Test
	public void idleHeldLongEnoughIsAFailedRefresh() {
		PanoramaxTilesProvider.RefreshWatch watch = new PanoramaxTilesProvider.RefreshWatch();
		assertFalse(watch.hasFailed(IDLE, NOW));
		assertFalse(watch.hasFailed(IDLE, NOW + CONFIRM - 1));
		assertTrue(watch.hasFailed(IDLE, NOW + CONFIRM));
	}

	@Test
	public void aRequestComingBackRestartsTheConfirmation() {
		PanoramaxTilesProvider.RefreshWatch watch = new PanoramaxTilesProvider.RefreshWatch();
		assertFalse(watch.hasFailed(IDLE, NOW));
		assertFalse(watch.hasFailed(IN_PROGRESS, NOW + CONFIRM));
		assertFalse(watch.hasFailed(IDLE, NOW + CONFIRM + 1));
		assertFalse(watch.hasFailed(IDLE, NOW + 2 * CONFIRM));
		assertTrue(watch.hasFailed(IDLE, NOW + 2 * CONFIRM + 1));
	}
}
