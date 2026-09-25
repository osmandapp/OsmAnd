package net.osmand.plus.configmap.tracks;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.preferences.ListStringPreference;
import net.osmand.plus.settings.enums.TracksSortMode;
import net.osmand.shared.gpx.enums.TracksSortScope;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
public class TrackSortModesHelperTest {

	private static final String LOCAL_FOLDER = "Local" + File.separator + "A";
	private static final String IMPORTED_FOLDER = "RemoteOnly" + File.separator + "B";

	private OsmandApplication app;
	private ListStringPreference preference;
	private String originalValue;

	@Before
	public void setUp() {
		Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
		app = (OsmandApplication) context.getApplicationContext();
		preference = app.getSettings().TRACKS_TABS_SORT_MODES;
		originalValue = preference.get();
	}

	@After
	public void tearDown() {
		preference.set(originalValue);
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
	public void reloadingDoesNotWriteThePreference() {
		persist(sortModes(LOCAL_FOLDER, TracksSortMode.NAME_ASCENDING,
				IMPORTED_FOLDER, TracksSortMode.LAST_MODIFIED));
		TrackSortModesHelper helper = new TrackSortModesHelper(app);
		String before = preference.get();

		helper.reloadFromPreference();

		assertEquals(before, preference.get());
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
