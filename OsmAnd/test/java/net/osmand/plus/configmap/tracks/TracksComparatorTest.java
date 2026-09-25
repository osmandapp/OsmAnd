package net.osmand.plus.configmap.tracks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.shared.SharedUtil;
import net.osmand.plus.settings.enums.TracksSortMode;
import net.osmand.shared.gpx.TrackItem;
import net.osmand.shared.gpx.data.TrackFolder;
import net.osmand.shared.gpx.filters.TrackFolderAnalysis;
import net.osmand.shared.io.KFile;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Regression test for the "Comparison method violates its general contract!" crash on the
 * Tracks screen (BaseTrackFolderFragment.sortItems), reproduced while a bulk track deletion
 * is still running in background.
 */
@RunWith(AndroidJUnit4.class)
public class TracksComparatorTest {

	private static final int TRACKS_COUNT = 400;
	private static final int PAIRWISE_TRACKS_COUNT = 50;

	private OsmandApplication app;
	private File dir;

	@Before
	public void setUp() {
		Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
		app = (OsmandApplication) targetContext.getApplicationContext();
		dir = new File(targetContext.getCacheDir(), "tracks_comparator_test");
		removeDir(dir);
		//noinspection ResultOfMethodCallIgnored
		dir.mkdirs();
	}

	@After
	public void tearDown() {
		removeDir(dir);
	}

	/**
	 * The comparator must not read mutable filesystem state: two runs over the very same items
	 * have to produce the very same answers even if the files are gone in between. That is exactly
	 * the guarantee TimSort relies on while a sort is in progress.
	 */
	@Test
	public void comparisonDoesNotDependOnFilesPresence() throws IOException {
		List<TrackItem> items = createTrackItems(PAIRWISE_TRACKS_COUNT);
		TracksComparator comparator = new TracksComparator(TracksSortMode.LAST_MODIFIED, app);

		List<Integer> before = compareAllPairs(comparator, items);
		deleteFiles(items);
		List<Integer> after = compareAllPairs(comparator, items);

		assertEquals("Comparator result changed after the track files were deleted", before, after);
	}

	/**
	 * Reproduces the reported crash: the Tracks list is re-sorted (loadTracksProgress ->
	 * updateContent -> sortItems) while DeleteTracksTask keeps removing files in background.
	 */
	@Test
	public void sortIsStableWhileTracksAreBeingDeleted() throws Exception {
		List<TrackItem> items = createTrackItems(TRACKS_COUNT);
		TracksComparator comparator = new TracksComparator(TracksSortMode.LAST_MODIFIED, app);

		List<TrackItem> toDelete = new ArrayList<>(items);
		Collections.shuffle(toDelete);

		AtomicBoolean deletionFinished = new AtomicBoolean(false);
		Thread deleter = new Thread(() -> {
			for (TrackItem item : toDelete) {
				KFile file = item.getFile();
				if (file != null) {
					//noinspection ResultOfMethodCallIgnored
					SharedUtil.jFile(file).delete();
				}
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					break;
				}
			}
			deletionFinished.set(true);
		}, "delete-tracks-test");
		deleter.start();

		int sorts = 0;
		try {
			while (!deletionFinished.get()) {
				Collections.sort(new ArrayList<>(items), comparator);
				sorts++;
			}
		} catch (IllegalArgumentException e) {
			fail("Comparison method violates its general contract after " + sorts
					+ " sorts: " + e.getMessage());
		} finally {
			deleter.interrupt();
			deleter.join();
		}
		assertTrue("The test did not manage to sort while deleting", sorts > 0);
	}

	/**
	 * Same guarantee for track folders: {@link TrackFolder#lastModified()} is a live
	 * {@code stat} of the directory, so deleting a folder must not change an already started sort.
	 */
	@Test
	public void folderComparisonDoesNotDependOnDirPresence() throws IOException {
		File dir1 = new File(dir, "folder_a");
		File dir2 = new File(dir, "folder_b");
		// folder_a is older than folder_b, so "last modified" order is the opposite of the name order
		createFolderWithTrack(dir1, 10);
		createFolderWithTrack(dir2, 0);

		TrackFolder folder1 = new TrackFolder(SharedUtil.kFile(dir1), null);
		TrackFolder folder2 = new TrackFolder(SharedUtil.kFile(dir2), null);
		TracksComparator comparator = new TracksComparator(TracksSortMode.LAST_MODIFIED, app);

		int before = Integer.signum(comparator.compare(folder1, folder2));
		removeDir(dir1);
		removeDir(dir2);
		int after = Integer.signum(comparator.compare(folder1, folder2));

		assertEquals("Folder comparison changed after the folders were deleted", before, after);
	}

	/**
	 * The folder statistics row is not a track and not a folder, so it used to compare as "equal"
	 * to every track while the tracks themselves are ordered - an intransitive comparator.
	 */
	@Test
	public void folderStatsRowKeepsComparatorTransitive() throws IOException {
		List<TrackItem> items = createTrackItems(2);
		TrackItem item1 = items.get(0);
		TrackItem item2 = items.get(1);
		TrackFolder folder = new TrackFolder(SharedUtil.kFile(dir), null);
		TrackFolderAnalysis statsRow = new TrackFolderAnalysis(folder);

		TracksComparator comparator = new TracksComparator(TracksSortMode.LAST_MODIFIED, app);

		assertTrue("Tracks are expected to be ordered", comparator.compare(item1, item2) != 0);
		assertTrue("Stats row must not be equal to a track",
				comparator.compare(statsRow, item1) != 0 && comparator.compare(statsRow, item2) != 0);
	}

	private void createFolderWithTrack(@NonNull File folder, int index) throws IOException {
		//noinspection ResultOfMethodCallIgnored
		folder.mkdirs();
		File file = new File(folder, "track.gpx");
		try (FileOutputStream out = new FileOutputStream(file)) {
			out.write("<gpx></gpx>".getBytes());
		}
		//noinspection ResultOfMethodCallIgnored
		folder.setLastModified(System.currentTimeMillis() - index * 60_000L);
	}

	@NonNull
	private List<TrackItem> createTrackItems(int count) throws IOException {
		List<TrackItem> items = new ArrayList<>();
		long now = System.currentTimeMillis();
		for (int i = 0; i < count; i++) {
			File file = new File(dir, "track_" + i + ".gpx");
			try (FileOutputStream out = new FileOutputStream(file)) {
				out.write(("<gpx><name>track_" + i + "</name></gpx>").getBytes());
			}
			// distinct modification times, so that the order is defined by them
			//noinspection ResultOfMethodCallIgnored
			file.setLastModified(now - i * 1000L);
			items.add(new TrackItem(SharedUtil.kFile(file)));
		}
		return items;
	}

	private void deleteFiles(@NonNull List<TrackItem> items) {
		for (TrackItem item : items) {
			KFile file = item.getFile();
			if (file != null) {
				//noinspection ResultOfMethodCallIgnored
				SharedUtil.jFile(file).delete();
			}
		}
	}

	@NonNull
	private List<Integer> compareAllPairs(@NonNull TracksComparator comparator,
	                                      @NonNull List<TrackItem> items) {
		List<Integer> result = new ArrayList<>();
		for (TrackItem item1 : items) {
			for (TrackItem item2 : items) {
				result.add(Integer.signum(comparator.compare(item1, item2)));
			}
		}
		return result;
	}

	private static void removeDir(@NonNull File dir) {
		File[] files = dir.listFiles();
		if (files != null) {
			for (File file : files) {
				if (file.isDirectory()) {
					removeDir(file);
				} else {
					//noinspection ResultOfMethodCallIgnored
					file.delete();
				}
			}
		}
		//noinspection ResultOfMethodCallIgnored
		dir.delete();
	}
}
