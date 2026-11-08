package net.osmand.plus.configmap.tracks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import net.osmand.IndexConstants;
import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.myplaces.tracks.tasks.DeleteTracksTask;
import net.osmand.plus.myplaces.tracks.tasks.DeleteTracksTask.GpxFilesDeletionListener;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.ListStringPreference;
import net.osmand.plus.settings.enums.TracksSortMode;
import net.osmand.plus.utils.FileUtils;
import net.osmand.plus.shared.SharedUtil;
import net.osmand.shared.gpx.SmartFolderHelper;
import net.osmand.shared.gpx.data.OrganizedTracksGroup;
import net.osmand.shared.gpx.data.SmartFolder;
import net.osmand.shared.gpx.data.TrackFolder;
import net.osmand.shared.gpx.enums.TracksSortScope;
import net.osmand.util.Algorithms;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class TrackSortModesHelperTest {

	private static final String LOCAL_FOLDER = "Local" + File.separator + "A";
	private static final String IMPORTED_FOLDER = "RemoteOnly" + File.separator + "B";

	private final List<File> createdDirs = new ArrayList<>();
	private OsmandApplication app;
	private OsmandSettings settings;
	private ListStringPreference preference;
	private String originalValue;
	private long originalEditTime;

	@Before
	public void setUp() {
		Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
		app = (OsmandApplication) context.getApplicationContext();
		settings = app.getSettings();
		preference = settings.TRACKS_TABS_SORT_MODES;
		originalValue = preference.get();
		originalEditTime = settings.getLastGlobalPreferencesEditTime();
	}

	@After
	public void tearDown() {
		preference.set(originalValue);
		settings.setLastGlobalPreferencesEditTime(originalEditTime);
		for (File dir : createdDirs) {
			Algorithms.removeAllFiles(dir);
		}
	}

	@Test
	public void anExternallyReplacedPreferenceIsVisibleWithoutRecreatingTheHelper() {
		persist(sortModes(LOCAL_FOLDER, TracksSortMode.NAME_ASCENDING));
		TrackSortModesHelper helper = new TrackSortModesHelper(app);

		persist(sortModes(LOCAL_FOLDER, TracksSortMode.DATE_DESCENDING,
				IMPORTED_FOLDER, TracksSortMode.LAST_MODIFIED));

		assertEquals(TracksSortMode.DATE_DESCENDING,
				helper.getSortMode(LOCAL_FOLDER, TracksSortScope.TRACKS));
		assertEquals(TracksSortMode.LAST_MODIFIED,
				helper.getSortMode(IMPORTED_FOLDER, TracksSortScope.TRACKS));
	}

	@Test
	public void anImportedEntryOutlivesTheNextUserSortingChange() {
		persist(sortModes(LOCAL_FOLDER, TracksSortMode.NAME_ASCENDING));
		TrackSortModesHelper helper = new TrackSortModesHelper(app);

		persist(sortModes(LOCAL_FOLDER, TracksSortMode.DATE_DESCENDING,
				IMPORTED_FOLDER, TracksSortMode.LAST_MODIFIED));

		helper.setSortMode(LOCAL_FOLDER, TracksSortScope.TRACKS, TracksSortMode.NAME_DESCENDING);
		helper.syncSettings();

		assertEquals(sortModes(LOCAL_FOLDER, TracksSortMode.NAME_DESCENDING,
				IMPORTED_FOLDER, TracksSortMode.LAST_MODIFIED), persisted());
	}

	@Test
	public void startupAndReloadDoNotWriteThePreference() {
		// Legacy keys, a folder that is not on this device and an unsorted value must all survive.
		preference.set(IMPORTED_FOLDER + ",,LAST_MODIFIED;;tracks,,NAME_DESCENDING;;2025,,DATE_ASCENDING;;");
		long editTime = System.currentTimeMillis() - 60_000L;
		settings.setLastGlobalPreferencesEditTime(editTime);
		String before = preference.get();

		TrackSortModesHelper helper = new TrackSortModesHelper(app);
		helper.reloadFromPreference();

		assertEquals(before, preference.get());
		assertEquals(editTime, settings.getLastGlobalPreferencesEditTime());

		helper.setSortMode(LOCAL_FOLDER, TracksSortScope.TRACKS, TracksSortMode.NAME_ASCENDING);
		helper.syncSettings();

		assertNotEquals(before, preference.get());
		assertTrue(settings.getLastGlobalPreferencesEditTime() > editTime);
		assertEquals(TracksSortMode.LAST_MODIFIED, persisted().get(IMPORTED_FOLDER));
	}

	@Test
	public void movingFoldersOnDiskCarriesTheirSortModes() {
		String root = "SortMoveRoot_" + System.nanoTime();
		String target = "SortMoveTarget_" + System.nanoTime();
		File rootDir = createTrackDir(root + File.separator + "Child" + File.separator + "Grand");
		rootDir = rootDir.getParentFile().getParentFile();
		File innerDir = createTrackDir(target + File.separator + "Inner");
		File targetDir = innerDir.getParentFile();
		persist(sortModes(
				root, TracksSortMode.NAME_ASCENDING,
				root + File.separator + "Child", TracksSortMode.DATE_DESCENDING,
				root + File.separator + "Child" + File.separator + "Grand", TracksSortMode.LAST_MODIFIED,
				root + "Old", TracksSortMode.DISTANCE_ASCENDING));

		// Top-level folder into a nested location.
		File nestedDir = moveTrackFolder(rootDir, innerDir);
		String nested = target + File.separator + "Inner" + File.separator + root;
		assertEquals(sortModes(
				nested, TracksSortMode.NAME_ASCENDING,
				nested + File.separator + "Child", TracksSortMode.DATE_DESCENDING,
				nested + File.separator + "Child" + File.separator + "Grand", TracksSortMode.LAST_MODIFIED,
				root + "Old", TracksSortMode.DISTANCE_ASCENDING), persisted());

		// Nested folder into another nested location.
		File otherDir = moveTrackFolder(nestedDir, targetDir);
		String other = target + File.separator + root;
		assertEquals(TracksSortMode.NAME_ASCENDING, persisted().get(other));
		assertNull(persisted().get(nested));

		// Nested folder back to the top level.
		moveTrackFolder(otherDir, app.getAppPath(IndexConstants.GPX_INDEX_DIR));
		assertEquals(sortModes(
				root, TracksSortMode.NAME_ASCENDING,
				root + File.separator + "Child", TracksSortMode.DATE_DESCENDING,
				root + File.separator + "Child" + File.separator + "Grand", TracksSortMode.LAST_MODIFIED,
				root + "Old", TracksSortMode.DISTANCE_ASCENDING), persisted());
	}

	@Test
	public void deletingFolderOnDiskRemovesItsSortModesOnlyAfterItIsGone() {
		String root = "SortDeleteRoot_" + System.nanoTime();
		File rootDir = createTrackDir(root + File.separator + "A").getParentFile();
		assertTrue(new File(rootDir, "B").mkdirs());
		persist(sortModes(
				root, TracksSortMode.NAME_ASCENDING,
				root + File.separator + "A", TracksSortMode.DATE_DESCENDING,
				root + File.separator + "B", TracksSortMode.LAST_MODIFIED,
				root + "Old", TracksSortMode.DISTANCE_ASCENDING));
		TrackFolder trackFolder = createTrackFolder(rootDir);
		Map<String, TracksSortMode> before = persisted();

		// Only A was deleted: the folder is still there, so nothing is reconciled yet.
		assertTrue(Algorithms.removeAllFiles(new File(rootDir, "A")));
		FileUtils.updateAfterDeleteTrackFolder(app, trackFolder);
		assertEquals(before, persisted());

		assertTrue(Algorithms.removeAllFiles(rootDir));
		FileUtils.updateAfterDeleteTrackFolder(app, trackFolder);

		assertEquals(sortModes(root + "Old", TracksSortMode.DISTANCE_ASCENDING), persisted());
		assertNull(app.getTrackSortModesHelper().getSortMode(root, TracksSortScope.TRACKS));
	}

	@Test
	public void deleteTracksTaskRemovesSortModesOfDeletedFolders() throws InterruptedException {
		String root = "SortTaskRoot_" + System.nanoTime();
		File rootDir = createTrackDir(root + File.separator + "Child" + File.separator + "Grand")
				.getParentFile().getParentFile();
		persist(sortModes(
				root, TracksSortMode.NAME_ASCENDING,
				root + "::" + TracksSortScope.ORGANIZED_BY_NAME.name(), TracksSortMode.NAME_DESCENDING,
				root + File.separator + "Child", TracksSortMode.DATE_DESCENDING,
				root + File.separator + "Child" + File.separator + "Grand", TracksSortMode.LAST_MODIFIED,
				root + "Old", TracksSortMode.DISTANCE_ASCENDING));
		CountDownLatch finished = new CountDownLatch(1);
		DeleteTracksTask task = new DeleteTracksTask(app, null,
				Collections.singleton(createTrackFolder(rootDir)), new GpxFilesDeletionListener() {
			@Override
			public void onGpxFilesDeletionFinished() {
				finished.countDown();
			}
		});

		InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> OsmAndTaskManager.executeTask(task));

		assertTrue(finished.await(30, TimeUnit.SECONDS));
		assertFalse(rootDir.exists());
		assertEquals(sortModes(root + "Old", TracksSortMode.DISTANCE_ASCENDING), persisted());
	}

	@Test
	public void anEntryDroppedByTheImportLeavesTheCache() {
		persist(sortModes(LOCAL_FOLDER, TracksSortMode.NAME_ASCENDING,
				IMPORTED_FOLDER, TracksSortMode.LAST_MODIFIED));
		TrackSortModesHelper helper = new TrackSortModesHelper(app);

		persist(sortModes(IMPORTED_FOLDER, TracksSortMode.LAST_MODIFIED));

		assertEquals(sortModes(IMPORTED_FOLDER, TracksSortMode.LAST_MODIFIED), persisted());
		helper.syncSettings();
		assertEquals(sortModes(IMPORTED_FOLDER, TracksSortMode.LAST_MODIFIED), persisted());
	}

	@Test
	public void smartFolderRenameAndDeleteCarryTheirSortModes() {
		SmartFolderHelper smartFolderHelper = app.getSmartFolderHelper();
		TrackSortModesHelper sortModesHelper = app.getTrackSortModesHelper();
		String name = "SortSmart_" + System.nanoTime();
		String newName = name + "_renamed";
		try {
			smartFolderHelper.saveNewSmartFolder(name, null);
			SmartFolder smartFolder = smartFolderHelper.getSmartFolder(name);
			String id = smartFolder.getId();
			String organizedId = OrganizedTracksGroup.Companion.getBaseId(id) + "activity__walking";
			persist(sortModes(
					id, TracksSortMode.NAME_ASCENDING,
					id + "::" + TracksSortScope.ORGANIZED_BY_VALUE.name(), TracksSortMode.VALUE_DESCENDING,
					organizedId, TracksSortMode.LAST_MODIFIED,
					id + "Other", TracksSortMode.DATE_DESCENDING));

			// The same calls as SmartFolderOptionsController makes after the user confirms.
			smartFolderHelper.renameSmartFolder(smartFolder, newName);
			sortModesHelper.onSmartFolderRenamed(id, smartFolder.getId());

			String newId = smartFolder.getId();
			assertEquals(sortModes(
					newId, TracksSortMode.NAME_ASCENDING,
					newId + "::" + TracksSortScope.ORGANIZED_BY_VALUE.name(), TracksSortMode.VALUE_DESCENDING,
					OrganizedTracksGroup.Companion.getBaseId(newId) + "activity__walking", TracksSortMode.LAST_MODIFIED,
					id + "Other", TracksSortMode.DATE_DESCENDING), persisted());

			SmartFolder renamed = smartFolderHelper.getSmartFolder(newName);
			smartFolderHelper.deleteSmartFolder(renamed);
			sortModesHelper.onTrackFolderDeleted(renamed);
			assertEquals(sortModes(id + "Other", TracksSortMode.DATE_DESCENDING), persisted());

			smartFolderHelper.saveNewSmartFolder(newName, null);
			assertNull(sortModesHelper.getSortMode(smartFolderHelper.getSmartFolder(newName).getId(),
					TracksSortScope.TRACKS));
		} finally {
			for (String folderName : new String[] {name, newName}) {
				SmartFolder folder = smartFolderHelper.getSmartFolder(folderName);
				if (folder != null) {
					smartFolderHelper.deleteSmartFolder(folder);
				}
			}
		}
	}

	private File createTrackDir(String relativePath) {
		File dir = new File(app.getAppPath(IndexConstants.GPX_INDEX_DIR), relativePath);
		assertTrue(dir.mkdirs());
		createdDirs.add(new File(app.getAppPath(IndexConstants.GPX_INDEX_DIR), relativePath.split(File.separator)[0]));
		return dir;
	}

	private TrackFolder createTrackFolder(File dir) {
		TrackFolder tracksFolder = new TrackFolder(SharedUtil.kFile(app.getAppPath(IndexConstants.GPX_INDEX_DIR)), null);
		return new TrackFolder(SharedUtil.kFile(dir), tracksFolder);
	}

	/** Moves the directory on disk and then updates the sort modes, as MoveTrackFoldersTask does. */
	private File moveTrackFolder(File src, File destinationFolder) {
		TrackFolder trackFolder = new TrackFolder(SharedUtil.kFile(src), null);
		File dest = new File(destinationFolder, src.getName());
		assertTrue(src.renameTo(dest));
		FileUtils.updateMovedTrackFolder(app, trackFolder, src, dest);
		return dest;
	}

	private void persist(Map<String, TracksSortMode> sortModes) {
		preference.setStringsList(TrackSortModeKeyUtils.serializeSortModes(sortModes));
	}

	private Map<String, TracksSortMode> persisted() {
		List<String> tokens = preference.getStringsList();
		return TrackSortModeKeyUtils.parseSortModes(tokens);
	}

	private static Map<String, TracksSortMode> sortModes(Object... idsAndModes) {
		Map<String, TracksSortMode> sortModes = new LinkedHashMap<>();
		for (int i = 0; i < idsAndModes.length; i += 2) {
			sortModes.put((String) idsAndModes[i], (TracksSortMode) idsAndModes[i + 1]);
		}
		return sortModes;
	}
}
