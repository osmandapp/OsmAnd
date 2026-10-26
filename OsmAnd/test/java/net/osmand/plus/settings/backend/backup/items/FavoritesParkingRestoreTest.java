package net.osmand.plus.settings.backend.backup.items;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNotNull;
import static org.junit.Assume.assumeTrue;

import android.content.Context;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class FavoritesParkingRestoreTest {

	private static final double LAT_A = 52.371;
	private static final double LON_A = 4.892;
	private static final double LAT_B = 48.856;
	private static final double LON_B = 2.352;
	private static final long PICKUP_A = 1_700_000_000_000L;
	private static final long PICKUP_B = 1_800_000_000_000L;
	private static final long LIMIT_B = 1_900_000_000_000L;

	private OsmandApplication app;
	private OsmandSettings settings;
	private FavouritesHelper favoritesHelper;
	private ParkingPositionPlugin plugin;

	private boolean originalType;
	private long originalTime;
	private long originalPickupDate;
	private boolean originalEvent;
	private LatLon originalPosition;
	private long originalGlobalEditTime;
	private ApplicationMode originalMode;
	private boolean originalShowFavorites;
	private boolean originalShowFavoritesSet;
	private long originalModeEditTime;
	private final List<String> originallyUnsetParkingPreferences = new ArrayList<>();
	private boolean restorePreferences = true;

	@Before
	public void setUp() {
		Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
		app = (OsmandApplication) context.getApplicationContext();
		settings = app.getSettings();
		favoritesHelper = app.getFavoritesHelper();
		plugin = PluginsHelper.getPlugin(ParkingPositionPlugin.class);
		assumeNotNull(plugin);

		originalType = plugin.getParkingType();
		originalTime = plugin.getParkingTime();
		originalPickupDate = plugin.getStartParkingTime();
		originalEvent = plugin.isParkingEventAdded();
		originalPosition = plugin.constructParkingPosition();
		originalGlobalEditTime = settings.getLastGlobalPreferencesEditTime();
		originalMode = settings.getApplicationMode();
		originalShowFavorites = settings.SHOW_FAVORITES.get();
		originalShowFavoritesSet = settings.SHOW_FAVORITES.isSet();
		originalModeEditTime = settings.getLastModePreferencesEditTime(originalMode);
		for (String id : new String[]{
				ParkingPositionPlugin.PARKING_POINT_LAT,
				ParkingPositionPlugin.PARKING_POINT_LON,
				ParkingPositionPlugin.PARKING_TYPE,
				ParkingPositionPlugin.PARKING_TIME,
				ParkingPositionPlugin.PARKING_PICKUP_DATE,
				ParkingPositionPlugin.PARKING_EVENT_ADDED}) {
			if (!settings.getPreference(id).isSet()) {
				originallyUnsetParkingPreferences.add(id);
			}
		}
	}

	@After
	public void tearDown() {
		if (plugin != null && restorePreferences) {
			// setParkingType() clears the limit, so the time has to be restored after it.
			plugin.setParkingType(originalType);
			plugin.setParkingTime(originalTime);
			plugin.setParkingPickupDate(originalPickupDate);
			plugin.setParkingPosition(originalPosition != null ? originalPosition.getLatitude() : 0,
					originalPosition != null ? originalPosition.getLongitude() : 0);
			plugin.addOrRemoveParkingEvent(originalEvent);
			for (String id : originallyUnsetParkingPreferences) {
				settings.getPreference(id).resetToDefault();
			}
			if (originalShowFavoritesSet) {
				settings.SHOW_FAVORITES.set(originalShowFavorites);
			} else {
				settings.SHOW_FAVORITES.resetToDefault();
			}
			settings.setLastModePreferencesEditTime(originalMode, originalModeEditTime);
			settings.setLastGlobalPreferencesEditTime(originalGlobalEditTime);
		}
	}

	@Test
	public void anIdenticalParkingPointUpdateLeavesSharedSettingsUntouched() {
		plugin.updateParkingPoint(parking(LAT_A, LON_A, PICKUP_A, 0));
		long globalEditTime = markGlobalEditTime();

		plugin.updateParkingPoint(parking(LAT_A, LON_A, PICKUP_A, 0));

		assertParkingState(LAT_A, LON_A, false, 0, PICKUP_A, false);
		assertEquals(globalEditTime, settings.getLastGlobalPreferencesEditTime());
	}

	@Test
	public void aChangedParkingPointUpdateAppliesEveryPreference() {
		plugin.updateParkingPoint(parking(LAT_A, LON_A, PICKUP_A, 0));
		// Set the event flag without a point, so no calendar intent is fired from a test.
		plugin.addOrRemoveParkingEvent(true);

		plugin.updateParkingPoint(parking(LAT_B, LON_B, PICKUP_B, LIMIT_B));

		assertParkingState(LAT_B, LON_B, true, LIMIT_B, PICKUP_B, false);
	}

	@Test
	public void aParkingPointUpdateCanReturnToAnUnlimitedState() {
		plugin.updateParkingPoint(parking(LAT_B, LON_B, PICKUP_B, LIMIT_B));

		plugin.updateParkingPoint(parking(LAT_A, LON_A, PICKUP_A, 0));

		assertParkingState(LAT_A, LON_A, false, 0, PICKUP_A, false);
	}

	@Test
	public void aNonParkingPointDoesNotTouchParkingSettings() {
		plugin.updateParkingPoint(parking(LAT_A, LON_A, PICKUP_A, 0));
		long globalEditTime = markGlobalEditTime();

		plugin.updateParkingPoint(new FavouritePoint(LAT_B, LON_B, "Some place", "My places"));

		assertParkingState(LAT_A, LON_A, false, 0, PICKUP_A, false);
		assertEquals(globalEditTime, settings.getLastGlobalPreferencesEditTime());
	}

	@Test
	public void anExplicitParkingChangeStillUpdatesSharedSettings() {
		plugin.updateParkingPoint(parking(LAT_A, LON_A, PICKUP_A, 0));
		long globalEditTime = markGlobalEditTime();

		plugin.setParkingPosition(LAT_B, LON_B);
		plugin.setParkingType(true);
		plugin.setParkingTime(LIMIT_B);
		plugin.setParkingPickupDate(PICKUP_B);
		plugin.addOrRemoveParkingEvent(true);

		assertParkingState(LAT_B, LON_B, true, LIMIT_B, PICKUP_B, true);
		assertTrue(settings.getLastGlobalPreferencesEditTime() > globalEditTime);

		plugin.setParkingType(false);
		assertEquals(false, plugin.getParkingType());
		assertEquals(-1L, plugin.getParkingTime());
	}

	@Test
	public void anIdenticalRestoreThroughTheFavoritesItemKeepsSharedSettings() {
		assumeFavoritesStoreIsEmpty();
		try {
			givenLocalParking(LAT_A, LON_A, PICKUP_A, 0);
			long globalEditTime = markGlobalEditTime();

			restoreParking(parking(LAT_A, LON_A, PICKUP_A, 0));

			assertParkingState(LAT_A, LON_A, false, 0, PICKUP_A, false);
			assertEquals(globalEditTime, settings.getLastGlobalPreferencesEditTime());
			assertSingleParkingFavourite(LAT_A, LON_A);
		} finally {
			removePersonalFavourites();
		}
	}

	@Test
	public void aChangedRestoreThroughTheFavoritesItemAppliesTheImportedPoint() {
		assumeFavoritesStoreIsEmpty();
		try {
			givenLocalParking(LAT_A, LON_A, PICKUP_A, 0);

			restoreParking(parking(LAT_B, LON_B, PICKUP_B, LIMIT_B));

			assertParkingState(LAT_B, LON_B, true, LIMIT_B, PICKUP_B, false);
			assertSingleParkingFavourite(LAT_B, LON_B);
		} finally {
			removePersonalFavourites();
		}
	}

	/** A full Favorites save also rewrites groups unrelated to Parking. */
	private void assumeFavoritesStoreIsEmpty() {
		restorePreferences = false;
		assumeTrue("Device already holds favourites",
				favoritesHelper.getFavoriteGroups().isEmpty());
		assumeTrue("Device already holds a parking favourite",
				favoritesHelper.getSpecialPoint(SpecialPointType.PARKING) == null);
		restorePreferences = true;
	}

	private void givenLocalParking(double lat, double lon, long pickupDate, long limit) {
		FavouritePoint local = parking(lat, lon, pickupDate, limit);
		favoritesHelper.addFavourite(local, new AddFavoriteOptions());
		plugin.updateParkingPoint(local);
	}

	private void restoreParking(FavouritePoint imported) {
		FavoriteGroup personal = new FavoriteGroup(FavoriteGroup.PERSONAL_CATEGORY,
				new ArrayList<>(Collections.singletonList(imported)), 0, true, true);
		FavoritesSettingsItem item = new FavoritesSettingsItem(app,
				new ArrayList<>(Collections.singletonList(personal)));
		item.setShouldReplace(true);
		item.processDuplicateItems();
		item.apply();
	}

	private void assertSingleParkingFavourite(double lat, double lon) {
		FavoriteGroup personal = favoritesHelper.getGroup(FavoriteGroup.PERSONAL_CATEGORY);
		assertNotNull(personal);
		List<FavouritePoint> parkingPoints = new ArrayList<>();
		for (FavouritePoint point : personal.getPoints()) {
			if (point.getSpecialPointType() == SpecialPointType.PARKING) {
				parkingPoints.add(point);
			}
		}
		assertEquals(1, parkingPoints.size());
		assertEquals(lat, parkingPoints.get(0).getLatitude(), 0.0001);
		assertEquals(lon, parkingPoints.get(0).getLongitude(), 0.0001);
	}

	private void removePersonalFavourites() {
		FavoriteGroup personal = favoritesHelper.getGroup(FavoriteGroup.PERSONAL_CATEGORY);
		if (personal != null) {
			favoritesHelper.deleteGroup(personal, false);
			favoritesHelper.saveCurrentPointsIntoFile(false);
			favoritesHelper.loadFavorites();
			assertNull(favoritesHelper.getGroup(FavoriteGroup.PERSONAL_CATEGORY));
		}
		assertNull(favoritesHelper.getSpecialPoint(SpecialPointType.PARKING));
	}

	private void assertParkingState(double lat, double lon, boolean limited, long time,
	                                long pickupDate, boolean calendarEvent) {
		LatLon position = plugin.constructParkingPosition();
		assertNotNull(position);
		assertEquals(lat, position.getLatitude(), 0.0001);
		assertEquals(lon, position.getLongitude(), 0.0001);
		assertEquals(limited, plugin.getParkingType());
		assertEquals(time, plugin.getParkingTime());
		assertEquals(pickupDate, plugin.getStartParkingTime());
		assertEquals(calendarEvent, plugin.isParkingEventAdded());
	}

	private long markGlobalEditTime() {
		long time = System.currentTimeMillis() - 60_000;
		settings.setLastGlobalPreferencesEditTime(time);
		return time;
	}

	private static FavouritePoint parking(double lat, double lon, long pickupDate, long limit) {
		FavouritePoint point = new FavouritePoint(lat, lon, SpecialPointType.PARKING.getName(),
				FavoriteGroup.PERSONAL_CATEGORY);
		point.setAltitude(10);
		point.setTimestamp(limit);
		point.setPickupDate(pickupDate);
		point.setCalendarEvent(false);
		return point;
	}
}
