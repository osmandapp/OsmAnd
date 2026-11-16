package net.osmand.plus.configmap.tracks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import net.osmand.plus.settings.backend.preferences.ListStringPreference;
import net.osmand.plus.settings.enums.TracksSortMode;
import net.osmand.shared.gpx.enums.TracksSortScope;
import net.osmand.test.common.AndroidTest;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class TrackSortModesHelperTest extends AndroidTest {

	private static final long EDIT_TIME = 1_700_000_000_000L;

	@Test
	public void startupDoesNotRewriteSortModes() {
		ListStringPreference preference = settings.TRACKS_TABS_SORT_MODES;
		String originalValue = preference.get();
		long originalEditTime = settings.getLastGlobalPreferencesEditTime();
		try {
			// A folder that exists only on another device, a V1 (folder name) key, keys not in sorted order
			String value = "RemoteOnly25312/Trips,,LAST_MODIFIED;;Weekend25312,,NAME_DESCENDING;;ALL,,DATE_ASCENDING";
			preference.set(value);
			settings.setLastGlobalPreferencesEditTime(EDIT_TIME);
			// The removed startup pass ran in the constructor once the GPX DB was loaded
			assertTrue(app.getGpxDbHelper().isInitialized());

			TrackSortModesHelper helper = new TrackSortModesHelper(app);

			assertEquals(value, preference.get());
			assertEquals(EDIT_TIME, settings.getLastGlobalPreferencesEditTime());
			assertEquals(TracksSortMode.NAME_DESCENDING,
					helper.getSortMode("Local25312/Weekend25312", TracksSortScope.TRACKS));
		} finally {
			preference.set(originalValue);
			settings.setLastGlobalPreferencesEditTime(originalEditTime);
		}
	}
}
