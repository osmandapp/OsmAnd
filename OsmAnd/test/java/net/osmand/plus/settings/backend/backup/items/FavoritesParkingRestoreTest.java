package net.osmand.plus.settings.backend.backup.items;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNotNull;
import static org.junit.Assume.assumeTrue;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.SpecialPointType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.myplaces.favorites.FavoriteGroup;
import net.osmand.plus.myplaces.favorites.FavouritesHelper;
import net.osmand.plus.myplaces.favorites.add.AddFavoriteOptions;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.parking.ParkingPositionPlugin;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class FavoritesParkingRestoreTest {

	private static final double LAT_A = 52.371;
	private static final double LON_A = 4.892;
	private static final double LAT_B = 48.856;
	private static final double LON_B = 2.352;
	private static final long START_A = 1_700_000_000_123L;
	private static final long START_B = 1_800_000_000_456L;
	private static final long LIMIT_B = 1_900_000_000_789L;

	private OsmandApplication app;
	private OsmandSettings settings;
	private FavouritesHelper favoritesHelper;
	private ParkingPositionPlugin plugin;

	private long originalGlobalEditTime;
	private ApplicationMode originalMode;
	private long originalModeEditTime;
	private boolean originalShowFavorites;
	private boolean originalShowFavoritesSet;
	private boolean restorePreferences;

	@Before
	public void setUp() throws InterruptedException {
		Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
		app = (OsmandApplication) context.getApplicationContext();
		CountDownLatch initialized = new CountDownLatch(1);
		app.getAppInitializer().addOnFinishListener(init -> initialized.countDown());
		assertTrue("Application initialization timed out", initialized.await(30, TimeUnit.SECONDS));
		InstrumentationRegistry.getInstrumentation().waitForIdleSync();
		settings = app.getSettings();
		favoritesHelper = app.getFavoritesHelper();
		plugin = PluginsHelper.getPlugin(ParkingPositionPlugin.class);
		assumeNotNull(plugin);
		// A full Favorites save also rewrites groups unrelated to Parking, and tearDown() clears the parking.
		assumeTrue("Device already holds favourites", favoritesHelper.getFavoriteGroups().isEmpty());
		assumeTrue("Device already holds a parking position", plugin.constructParkingPosition() == null);

		originalGlobalEditTime = settings.getLastGlobalPreferencesEditTime();
		originalMode = settings.getApplicationMode();
		originalModeEditTime = settings.getLastModePreferencesEditTime(originalMode);
		originalShowFavorites = settings.SHOW_FAVORITES.get();
		originalShowFavoritesSet = settings.SHOW_FAVORITES.isSet();
		restorePreferences = true;
	}

	@After
	public void tearDown() {
		if (!restorePreferences) {
			return;
		}
		FavoriteGroup personal = favoritesHelper.getGroup(FavoriteGroup.PERSONAL_CATEGORY);
		if (personal != null) {
			favoritesHelper.deleteGroup(personal, false);
			favoritesHelper.saveCurrentPointsIntoFile(false);
		}
		plugin.clearParkingPosition();
		if (originalShowFavoritesSet) {
			settings.SHOW_FAVORITES.set(originalShowFavorites);
		} else {
			settings.SHOW_FAVORITES.resetToDefault();
		}
		settings.setLastModePreferencesEditTime(originalMode, originalModeEditTime);
		settings.setLastGlobalPreferencesEditTime(originalGlobalEditTime);
	}

	@Test
	public void anIdenticalRestoreThroughTheFavoritesItemKeepsSharedSettings() throws Exception {
		givenLocalParking(LAT_A, LON_A, START_A, -1);
		long globalEditTime = markGlobalEditTime();

		restoreParking(parking(LAT_A, LON_A, START_A, -1));

		assertEquals(globalEditTime, settings.getLastGlobalPreferencesEditTime());
		assertParkingState(LAT_A, LON_A, false, -1, START_A, false);
	}

	@Test
	public void aChangedRestoreThroughTheFavoritesItemAppliesTheImportedPoint() throws Exception {
		givenLocalParking(LAT_B, LON_B, START_B, LIMIT_B);
		plugin.addOrRemoveParkingEvent(true);

		restoreParking(parking(LAT_A, LON_A, START_A, -1));

		assertParkingState(LAT_A, LON_A, false, -1, START_A / 1000 * 1000, false);
	}

	@Test
	public void anIdenticalLimitedGpxRestorePreservesLocalMilliseconds() throws Exception {
		givenLocalParking(LAT_B, LON_B, START_B, LIMIT_B);
		long globalEditTime = markGlobalEditTime();

		restoreParking(parking(LAT_B, LON_B, START_B, LIMIT_B));

		assertParkingState(LAT_B, LON_B, true, LIMIT_B, START_B, false);
		assertEquals(globalEditTime, settings.getLastGlobalPreferencesEditTime());
	}

	@Test
	public void aRestoredCalendarFlagDoesNotOpenTheCalendarEditor() throws Exception {
		Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
		IntentFilter filter = new IntentFilter(Intent.ACTION_EDIT);
		filter.addDataType("vnd.android.cursor.item/event");
		Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(filter,
				new Instrumentation.ActivityResult(Activity.RESULT_OK, null), true);
		try {
			givenLocalParking(LAT_B, LON_B, START_B, LIMIT_B);
			plugin.addOrRemoveParkingEvent(true);
			instrumentation.runOnMainSync(() -> plugin.addCalendarEvent(app));
			assertEquals("The monitor must intercept an explicit calendar action", 1, monitor.getHits());
			FavouritePoint imported = parking(LAT_B, LON_B, START_B, LIMIT_B);
			imported.setCalendarEvent(true);

			restoreParking(imported);

			assertEquals(1, monitor.getHits());
			assertParkingState(LAT_B, LON_B, true, LIMIT_B, START_B, true);
		} finally {
			instrumentation.removeMonitor(monitor);
		}
	}

	private void givenLocalParking(double lat, double lon, long startTime, long limit) {
		FavouritePoint local = parking(lat, lon, startTime, limit);
		favoritesHelper.addFavourite(local, new AddFavoriteOptions());
		plugin.updateParkingPoint(local);
	}

	/** Writes the point as a Favorites backup (GPX keeps whole seconds) and restores it. */
	private void restoreParking(FavouritePoint imported) throws Exception {
		FavoriteGroup personal = new FavoriteGroup(FavoriteGroup.PERSONAL_CATEGORY,
				new ArrayList<>(Collections.singletonList(imported)), 0, true, true);
		FavoritesSettingsItem backup = new FavoritesSettingsItem(app,
				new ArrayList<>(Collections.singletonList(personal)));
		ByteArrayOutputStream gpx = new ByteArrayOutputStream();
		backup.getWriter().writeToStream(gpx, null);

		FavoritesSettingsItem item = new FavoritesSettingsItem(app, new JSONObject()
				.put("type", "FAVOURITES").put("file", backup.getDefaultFileName()));
		item.getReader().readFromStream(new ByteArrayInputStream(gpx.toByteArray()), null,
				backup.getDefaultFileName());
		assertEquals(1, item.getItems().size());
		item.setShouldReplace(true);
		item.processDuplicateItems();
		item.apply();
		assertTrue(item.isAppliedLocally());
	}

	private void assertParkingState(double lat, double lon, boolean limited, long time,
	                                long startTime, boolean calendarEvent) {
		LatLon position = plugin.constructParkingPosition();
		assertNotNull(position);
		assertEquals(lat, position.getLatitude(), 0.0001);
		assertEquals(lon, position.getLongitude(), 0.0001);
		assertEquals(limited, plugin.getParkingType());
		assertEquals(time, plugin.getParkingTime());
		assertEquals(startTime, plugin.getStartParkingTime());
		assertEquals(calendarEvent, plugin.isParkingEventAdded());
	}

	private long markGlobalEditTime() {
		long time = System.currentTimeMillis() - 60_000;
		settings.setLastGlobalPreferencesEditTime(time);
		return time;
	}

	private static FavouritePoint parking(double lat, double lon, long startTime, long limit) {
		FavouritePoint point = new FavouritePoint(lat, lon, SpecialPointType.PARKING.getName(),
				FavoriteGroup.PERSONAL_CATEGORY);
		point.setAltitude(10);
		point.setTimestamp(startTime);
		point.setPickupDate(limit);
		point.setCalendarEvent(false);
		return point;
	}
}
