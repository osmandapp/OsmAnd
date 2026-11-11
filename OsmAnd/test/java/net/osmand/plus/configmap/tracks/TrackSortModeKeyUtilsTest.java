package net.osmand.plus.configmap.tracks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import net.osmand.plus.settings.enums.TracksSortMode;
import net.osmand.shared.gpx.data.OrganizedTracksGroup;
import net.osmand.shared.gpx.data.SmartFolder;
import net.osmand.shared.gpx.enums.TracksSortScope;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
public class TrackSortModeKeyUtilsTest {

	@Test
	public void legacyFolderKeysResolveWithoutChangingStoredKeys() {
		Map<String, TracksSortMode> sortModes = new LinkedHashMap<>();
		sortModes.put("tracks", TracksSortMode.NAME_DESCENDING);
		sortModes.put("2025", TracksSortMode.DATE_ASCENDING);
		sortModes.put("Trips" + File.separator + "2025", TracksSortMode.LAST_MODIFIED);
		Map<String, TracksSortMode> expectedStoredModes = new LinkedHashMap<>(sortModes);

		assertEquals(TracksSortMode.NAME_DESCENDING,
				TrackSortModeKeyUtils.resolveSortMode(sortModes, "", TracksSortScope.TRACKS));
		assertEquals(TracksSortMode.LAST_MODIFIED,
				TrackSortModeKeyUtils.resolveSortMode(sortModes,
						"Trips" + File.separator + "2025", TracksSortScope.TRACKS));
		assertEquals(TracksSortMode.DATE_ASCENDING,
				TrackSortModeKeyUtils.resolveSortMode(sortModes,
						"Archive" + File.separator + "2025", TracksSortScope.TRACKS));
		assertEquals(expectedStoredModes, sortModes);
	}

	@Test
	public void legacyFolderKeysAreNotAppliedToOrganizedScopes() {
		Map<String, TracksSortMode> sortModes = new LinkedHashMap<>();
		sortModes.put("2025", TracksSortMode.NAME_DESCENDING);

		assertNull(TrackSortModeKeyUtils.resolveSortMode(sortModes, "2025",
				TracksSortScope.ORGANIZED_BY_NAME));
	}

	@Test
	public void slashInSmartFolderNameDoesNotCreateAFolderHierarchy() {
		String trips = SmartFolder.ID_PREFIX + "Trips";
		String weekend = trips + File.separator + "Weekend";
		String archive = SmartFolder.ID_PREFIX + "Archive";
		TracksSortScope scope = TracksSortScope.ORGANIZED_BY_NAME;
		String groupSuffix = "name__summer";
		Map<String, TracksSortMode> sortModes = new LinkedHashMap<>();
		sortModes.put("Weekend", TracksSortMode.DATE_ASCENDING);
		assertNull(TrackSortModeKeyUtils.resolveSortMode(sortModes, weekend, TracksSortScope.TRACKS));

		sortModes.put(weekend, TracksSortMode.LAST_MODIFIED);
		sortModes.put(TrackSortModeKeyUtils.getSortModeKey(weekend, scope), TracksSortMode.NAME_ASCENDING);
		sortModes.put(OrganizedTracksGroup.Companion.getBaseId(weekend) + groupSuffix,
				TracksSortMode.DATE_DESCENDING);
		Map<String, TracksSortMode> independentModes = new LinkedHashMap<>(sortModes);
		assertEquals(TracksSortMode.LAST_MODIFIED,
				TrackSortModeKeyUtils.resolveSortMode(sortModes, weekend, TracksSortScope.TRACKS));

		sortModes.put(trips, TracksSortMode.NAME_ASCENDING);
		sortModes.put(TrackSortModeKeyUtils.getSortModeKey(trips, scope), TracksSortMode.NAME_DESCENDING);
		sortModes.put(OrganizedTracksGroup.Companion.getBaseId(trips) + groupSuffix,
				TracksSortMode.DATE_ASCENDING);
		assertTrue(TrackSortModeKeyUtils.moveFolderKeys(sortModes, trips, archive));

		Map<String, TracksSortMode> renamedModes = new LinkedHashMap<>(independentModes);
		renamedModes.put(archive, TracksSortMode.NAME_ASCENDING);
		renamedModes.put(TrackSortModeKeyUtils.getSortModeKey(archive, scope), TracksSortMode.NAME_DESCENDING);
		renamedModes.put(OrganizedTracksGroup.Companion.getBaseId(archive) + groupSuffix,
				TracksSortMode.DATE_ASCENDING);
		assertEquals(renamedModes, sortModes);

		assertTrue(TrackSortModeKeyUtils.moveFolderKeys(sortModes, archive, trips));
		assertTrue(TrackSortModeKeyUtils.removeFolderKeys(sortModes, trips));
		assertEquals(independentModes, sortModes);
	}

	@Test
	public void smartFolderNameRetainsTrailingSlashWhenSavedAndRead() {
		String trips = SmartFolder.ID_PREFIX + "Trips";
		String tripsWithSlash = trips + File.separator;
		Map<String, TracksSortMode> sortModes = new LinkedHashMap<>();
		sortModes.put(trips, TracksSortMode.NAME_ASCENDING);
		sortModes.put(TrackSortModeKeyUtils.getSortModeKey(tripsWithSlash, TracksSortScope.TRACKS),
				TracksSortMode.DATE_ASCENDING);

		Map<String, TracksSortMode> restored = TrackSortModeKeyUtils.parseSortModes(
				TrackSortModeKeyUtils.serializeSortModes(sortModes));
		assertEquals(2, restored.size());
		assertEquals(TracksSortMode.NAME_ASCENDING,
				TrackSortModeKeyUtils.resolveSortMode(restored, trips, TracksSortScope.TRACKS));
		assertEquals(TracksSortMode.DATE_ASCENDING,
				TrackSortModeKeyUtils.resolveSortMode(restored, tripsWithSlash, TracksSortScope.TRACKS));
	}

	@Test
	public void exactV2KeysKeepSameNamedFoldersIndependent() {
		Map<String, TracksSortMode> sortModes = new LinkedHashMap<>();
		sortModes.put("Trips" + File.separator + "2025", TracksSortMode.DATE_ASCENDING);
		sortModes.put("Archive" + File.separator + "2025", TracksSortMode.NAME_DESCENDING);

		assertEquals(TracksSortMode.DATE_ASCENDING,
				TrackSortModeKeyUtils.resolveSortMode(sortModes,
						"Trips" + File.separator + "2025", TracksSortScope.TRACKS));
		assertEquals(TracksSortMode.NAME_DESCENDING,
				TrackSortModeKeyUtils.resolveSortMode(sortModes,
						"Archive" + File.separator + "2025", TracksSortScope.TRACKS));
	}

	@Test
	public void movingNestedFolderMovesItsKeysAndKeepsLegacyKey() {
		Map<String, TracksSortMode> sortModes = new LinkedHashMap<>();
		sortModes.put("2025", TracksSortMode.NAME_ASCENDING);
		sortModes.put("Trips" + File.separator + "2025", TracksSortMode.DATE_ASCENDING);
		sortModes.put("Trips" + File.separator + "2025" + File.separator + "Day trips",
				TracksSortMode.LAST_MODIFIED);

		assertTrue(TrackSortModeKeyUtils.moveFolderKeys(sortModes,
				"Trips" + File.separator + "2025", "Archive" + File.separator + "2025"));

		assertEquals(TracksSortMode.NAME_ASCENDING, sortModes.get("2025"));
		assertNull(sortModes.get("Trips" + File.separator + "2025"));
		assertNull(sortModes.get("Trips" + File.separator + "2025" + File.separator + "Day trips"));
		assertEquals(TracksSortMode.DATE_ASCENDING,
				sortModes.get("Archive" + File.separator + "2025"));
		assertEquals(TracksSortMode.LAST_MODIFIED,
				sortModes.get("Archive" + File.separator + "2025" + File.separator + "Day trips"));
	}

	@Test
	public void movingNestedFolderCopiesLegacyFallbackWithoutRemovingSourceKey() {
		Map<String, TracksSortMode> sortModes = new LinkedHashMap<>();
		sortModes.put("2025", TracksSortMode.DATE_ASCENDING);

		assertEquals(TracksSortMode.DATE_ASCENDING,
				TrackSortModeKeyUtils.resolveSortMode(sortModes,
						"Trips" + File.separator + "2025", TracksSortScope.TRACKS));
		assertTrue(TrackSortModeKeyUtils.copyLegacyTrackSortModeForRenamedFolder(sortModes,
				"Trips" + File.separator + "2025", "Trips" + File.separator + "2026"));

		assertEquals(TracksSortMode.DATE_ASCENDING, sortModes.get("2025"));
		assertEquals(TracksSortMode.DATE_ASCENDING,
				sortModes.get("Trips" + File.separator + "2026"));
		assertEquals(TracksSortMode.DATE_ASCENDING,
				TrackSortModeKeyUtils.resolveSortMode(sortModes,
						"Trips" + File.separator + "2026", TracksSortScope.TRACKS));
	}

	@Test
	public void movingNestedFolderWithoutRenameKeepsUsingLegacyKey() {
		Map<String, TracksSortMode> sortModes = new LinkedHashMap<>();
		sortModes.put("2025", TracksSortMode.DATE_ASCENDING);
		Map<String, TracksSortMode> expectedStoredModes = new LinkedHashMap<>(sortModes);

		assertFalse(TrackSortModeKeyUtils.copyLegacyTrackSortModeForRenamedFolder(sortModes,
				"Trips" + File.separator + "2025", "Archive" + File.separator + "2025"));
		assertFalse(TrackSortModeKeyUtils.moveFolderKeys(sortModes,
				"Trips" + File.separator + "2025", "Archive" + File.separator + "2025"));

		assertEquals(expectedStoredModes, sortModes);
		assertEquals(TracksSortMode.DATE_ASCENDING,
				TrackSortModeKeyUtils.resolveSortMode(sortModes,
						"Archive" + File.separator + "2025", TracksSortScope.TRACKS));
	}

	@Test
	public void renamingTopLevelFolderMovesItsKeysAndDescendants() {
		String school = "Travel" + File.separator + "School";
		String year = school + File.separator + "2025";
		Map<String, TracksSortMode> sortModes = new LinkedHashMap<>();
		sortModes.put("Travel", TracksSortMode.NAME_ASCENDING);
		sortModes.put("Travel::" + TracksSortScope.ORGANIZED_BY_NAME.name(), TracksSortMode.NAME_DESCENDING);
		sortModes.put(school, TracksSortMode.DATE_DESCENDING);
		sortModes.put(year, TracksSortMode.LAST_MODIFIED);
		sortModes.put("TravelOld" + File.separator + "School", TracksSortMode.DISTANCE_ASCENDING);

		assertTrue(TrackSortModeKeyUtils.moveFolderKeys(sortModes, "Travel", "Archive"));

		Map<String, TracksSortMode> expected = new LinkedHashMap<>();
		expected.put("Archive", TracksSortMode.NAME_ASCENDING);
		expected.put("Archive::" + TracksSortScope.ORGANIZED_BY_NAME.name(), TracksSortMode.NAME_DESCENDING);
		expected.put("Archive" + File.separator + "School", TracksSortMode.DATE_DESCENDING);
		expected.put("Archive" + File.separator + "School" + File.separator + "2025", TracksSortMode.LAST_MODIFIED);
		expected.put("TravelOld" + File.separator + "School", TracksSortMode.DISTANCE_ASCENDING);
		assertEquals(expected, sortModes);
	}

	@Test
	public void renamingAncestorKeepsLegacyFallbackOfDescendants() {
		Map<String, TracksSortMode> sortModes = new LinkedHashMap<>();
		sortModes.put("School", TracksSortMode.NAME_ASCENDING);
		sortModes.put("Travel" + File.separator + "Other", TracksSortMode.DATE_ASCENDING);

		assertTrue(TrackSortModeKeyUtils.moveFolderKeys(sortModes, "Travel", "Archive"));

		assertNull(sortModes.get("Archive" + File.separator + "School"));
		assertEquals(TracksSortMode.NAME_ASCENDING,
				TrackSortModeKeyUtils.resolveSortMode(sortModes,
						"Archive" + File.separator + "School", TracksSortScope.TRACKS));
	}

	@Test
	public void movedKeysReplaceOnlyMatchingDestinationKeys() {
		String source = "Travel" + File.separator + "School";
		String destination = "Archive" + File.separator + "School";
		Map<String, TracksSortMode> sortModes = new LinkedHashMap<>();
		sortModes.put(source, TracksSortMode.NAME_ASCENDING);
		sortModes.put(destination, TracksSortMode.DATE_DESCENDING);
		sortModes.put(destination + File.separator + "2025", TracksSortMode.LAST_MODIFIED);
		sortModes.put("Archive" + File.separator + "Other", TracksSortMode.DISTANCE_ASCENDING);

		assertTrue(TrackSortModeKeyUtils.moveFolderKeys(sortModes, source, destination));

		Map<String, TracksSortMode> expected = new LinkedHashMap<>();
		expected.put(destination, TracksSortMode.NAME_ASCENDING);
		expected.put(destination + File.separator + "2025", TracksSortMode.LAST_MODIFIED);
		expected.put("Archive" + File.separator + "Other", TracksSortMode.DISTANCE_ASCENDING);
		assertEquals(expected, sortModes);
	}

	@Test
	public void deletingTopLevelFolderRemovesItsKeysAndDescendants() {
		Map<String, TracksSortMode> sortModes = new LinkedHashMap<>();
		sortModes.put("Trips", TracksSortMode.NAME_ASCENDING);
		sortModes.put("Trips::" + TracksSortScope.ORGANIZED_BY_NAME.name(), TracksSortMode.NAME_DESCENDING);
		sortModes.put("Trips" + File.separator + "2025", TracksSortMode.DATE_ASCENDING);
		sortModes.put("Trips2" + File.separator + "2025", TracksSortMode.DISTANCE_ASCENDING);
		sortModes.put("Other", TracksSortMode.LAST_MODIFIED);

		assertTrue(TrackSortModeKeyUtils.removeFolderKeys(sortModes, "Trips"));

		Map<String, TracksSortMode> expected = new LinkedHashMap<>();
		expected.put("Trips2" + File.separator + "2025", TracksSortMode.DISTANCE_ASCENDING);
		expected.put("Other", TracksSortMode.LAST_MODIFIED);
		assertEquals(expected, sortModes);
		assertNull(TrackSortModeKeyUtils.resolveSortMode(sortModes, "Trips", TracksSortScope.TRACKS));
		assertNull(TrackSortModeKeyUtils.resolveSortMode(sortModes,
				"Other" + File.separator + "Trips", TracksSortScope.TRACKS));
	}

	@Test
	public void serializationIsIndependentOfMapInsertionOrder() {
		Map<String, TracksSortMode> first = new LinkedHashMap<>();
		first.put("Trips" + File.separator + "2025", TracksSortMode.DATE_ASCENDING);
		first.put("", TracksSortMode.LAST_MODIFIED);
		first.put("Archive", TracksSortMode.NAME_DESCENDING);

		Map<String, TracksSortMode> second = new LinkedHashMap<>();
		second.put("Archive", TracksSortMode.NAME_DESCENDING);
		second.put("Trips" + File.separator + "2025", TracksSortMode.DATE_ASCENDING);
		second.put("", TracksSortMode.LAST_MODIFIED);

		List<String> expected = Arrays.asList(
				",,LAST_MODIFIED",
				"Archive,,NAME_DESCENDING",
				"Trips" + File.separator + "2025,,DATE_ASCENDING"
		);
		assertEquals(expected, TrackSortModeKeyUtils.serializeSortModes(first));
		assertEquals(expected, TrackSortModeKeyUtils.serializeSortModes(second));
	}
}
