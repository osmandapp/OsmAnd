package net.osmand.plus.configmap.tracks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import net.osmand.plus.settings.enums.TracksSortMode;
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
		assertFalse(TrackSortModeKeyUtils.copyTopLevelFolderSortMode(sortModes,
				"Trips" + File.separator + "2025", "Archive" + File.separator + "2025"));
	}

	@Test
	public void movingNestedFolderMovesOnlyUnambiguousV2Keys() {
		Map<String, TracksSortMode> sortModes = new LinkedHashMap<>();
		sortModes.put("2025", TracksSortMode.NAME_ASCENDING);
		sortModes.put("Trips" + File.separator + "2025", TracksSortMode.DATE_ASCENDING);
		sortModes.put("Trips" + File.separator + "2025" + File.separator + "Day trips",
				TracksSortMode.LAST_MODIFIED);

		assertTrue(TrackSortModeKeyUtils.moveUnambiguousV2Keys(sortModes,
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
	public void movingTopLevelFolderCopiesDirectKeyAndMovesChildKeys() {
		Map<String, TracksSortMode> sortModes = new LinkedHashMap<>();
		sortModes.put("Trips", TracksSortMode.NAME_ASCENDING);
		sortModes.put("Trips" + File.separator + "2025", TracksSortMode.DATE_ASCENDING);

		assertTrue(TrackSortModeKeyUtils.moveUnambiguousV2Keys(sortModes,
				"Trips", "Archive"));
		assertTrue(TrackSortModeKeyUtils.copyTopLevelFolderSortMode(sortModes,
				"Trips", "Archive"));

		assertEquals(TracksSortMode.NAME_ASCENDING, sortModes.get("Trips"));
		assertEquals(TracksSortMode.NAME_ASCENDING, sortModes.get("Archive"));
		assertNull(sortModes.get("Trips" + File.separator + "2025"));
		assertEquals(TracksSortMode.DATE_ASCENDING,
				sortModes.get("Archive" + File.separator + "2025"));
	}

	@Test
	public void movingTopLevelFolderCopiesOnlyDirectKey() {
		Map<String, TracksSortMode> sortModes = new LinkedHashMap<>();
		sortModes.put("Trips", TracksSortMode.DATE_ASCENDING);

		assertTrue(TrackSortModeKeyUtils.copyTopLevelFolderSortMode(sortModes,
				"Trips", "Archive"));

		assertEquals(TracksSortMode.DATE_ASCENDING, sortModes.get("Trips"));
		assertEquals(TracksSortMode.DATE_ASCENDING, sortModes.get("Archive"));
	}

	@Test
	public void deletingTopLevelFolderKeepsAmbiguousLegacyKey() {
		Map<String, TracksSortMode> sortModes = new LinkedHashMap<>();
		sortModes.put("Trips", TracksSortMode.NAME_ASCENDING);
		sortModes.put("Trips" + File.separator + "2025", TracksSortMode.DATE_ASCENDING);
		sortModes.put("Trips2" + File.separator + "2025", TracksSortMode.DISTANCE_ASCENDING);
		sortModes.put("Other", TracksSortMode.LAST_MODIFIED);

		assertTrue(TrackSortModeKeyUtils.removeUnambiguousV2Keys(sortModes, "Trips"));

		assertEquals(TracksSortMode.NAME_ASCENDING, sortModes.get("Trips"));
		assertNull(sortModes.get("Trips" + File.separator + "2025"));
		assertEquals(TracksSortMode.DISTANCE_ASCENDING,
				sortModes.get("Trips2" + File.separator + "2025"));
		assertEquals(TracksSortMode.LAST_MODIFIED, sortModes.get("Other"));
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
