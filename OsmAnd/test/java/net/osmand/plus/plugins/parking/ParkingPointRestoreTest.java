package net.osmand.plus.plugins.parking;

import static net.osmand.plus.plugins.parking.ParkingPositionPlugin.PARKING_EVENT_ADDED;
import static net.osmand.plus.plugins.parking.ParkingPositionPlugin.PARKING_PICKUP_DATE;
import static net.osmand.plus.plugins.parking.ParkingPositionPlugin.PARKING_POINT_LAT;
import static net.osmand.plus.plugins.parking.ParkingPositionPlugin.PARKING_POINT_LON;
import static net.osmand.plus.plugins.parking.ParkingPositionPlugin.PARKING_TIME;
import static net.osmand.plus.plugins.parking.ParkingPositionPlugin.PARKING_TYPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import net.osmand.data.FavouritePoint;
import net.osmand.data.SpecialPointType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A parking point restored from OsmAnd Cloud, a backup or an imported file must give the plugin
 * the same state as on the device that saved it. FavouritesHelper.setParkingPoint() keeps the
 * pick-up time in the point's pickup date and the time of parking in its timestamp.
 */
@RunWith(AndroidJUnit4.class)
public class ParkingPointRestoreTest {

	private static final double LAT = 47.13821;
	private static final double LON = 9.52099;
	// Whole seconds: the pickup date is written to GPX with second precision.
	private static final long PARKED_AT = 1793066733000L;  // 2026-10-27 02:05:33 UTC
	private static final long PICK_UP_UNTIL = PARKED_AT + 60 * 60 * 1000L;

	private static final String[] PARKING_KEYS = {PARKING_POINT_LAT, PARKING_POINT_LON,
			PARKING_TYPE, PARKING_TIME, PARKING_PICKUP_DATE, PARKING_EVENT_ADDED};

	private OsmandApplication app;
	private ParkingPositionPlugin plugin;
	private final Map<String, Object> savedValues = new LinkedHashMap<>();

	@Before
	public void setUp() {
		Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
		app = (OsmandApplication) targetContext.getApplicationContext();
		plugin = PluginsHelper.getPlugin(ParkingPositionPlugin.class);
		assertNotNull(plugin);
		for (String key : PARKING_KEYS) {
			OsmandPreference<?> preference = app.getSettings().getPreference(key);
			savedValues.put(key, preference.isSet() ? preference.get() : null);
		}
	}

	@After
	@SuppressWarnings("unchecked")
	public void tearDown() {
		if (plugin == null) {
			return;
		}
		Float lat = (Float) savedValues.get(PARKING_POINT_LAT);
		Float lon = (Float) savedValues.get(PARKING_POINT_LON);
		// Also refreshes the position cached by the plugin
		plugin.setParkingPosition(lat != null ? lat : 0, lon != null ? lon : 0);
		for (Map.Entry<String, Object> entry : savedValues.entrySet()) {
			OsmandPreference<Object> preference = (OsmandPreference<Object>) app.getSettings().getPreference(entry.getKey());
			if (entry.getValue() != null) {
				preference.set(entry.getValue());
			} else {
				preference.resetToDefault();
			}
		}
	}

	@Test
	public void timeUnlimitedParkingStaysUnlimited() {
		plugin.updateParkingPoint(restore(savedParkingPoint(-1)));

		assertFalse(plugin.getParkingType());
		assertTrue(plugin.getParkingTime() <= 0);
		assertEquals(PARKED_AT, plugin.getStartParkingTime());
		assertEquals(LAT, plugin.getParkingPosition().getLatitude(), 1e-5);
		assertEquals(LON, plugin.getParkingPosition().getLongitude(), 1e-5);
	}

	@Test
	public void timeLimitedParkingKeepsItsPickUpTime() {
		plugin.updateParkingPoint(restore(savedParkingPoint(PICK_UP_UNTIL)));

		assertTrue(plugin.getParkingType());
		assertEquals(PICK_UP_UNTIL, plugin.getParkingTime());
		assertEquals(PARKED_AT, plugin.getStartParkingTime());
	}

	@Test
	public void parkingSetAgainWithoutTimeLimitStaysUnlimited() {
		// A point loaded from a file keeps all its GPX extensions, and setParkingPoint() reuses it for the next parking
		FavouritePoint point = savedParkingPoint(PICK_UP_UNTIL);
		point.setCalendarEvent(true);
		FavouritePoint loaded = restore(point);
		loaded.setPickupDate(-1);
		loaded.setCalendarEvent(false);

		FavouritePoint restored = restore(loaded);
		assertFalse(restored.getCalendarEvent());
		plugin.updateParkingPoint(restored);

		assertFalse(plugin.getParkingType());
		assertTrue(plugin.getParkingTime() <= 0);
	}

	/**
	 * The point as FavouritesHelper.setParkingPoint() creates it; the plugin passes -1
	 * as the pick-up time of a time-unlimited parking.
	 */
	@NonNull
	private FavouritePoint savedParkingPoint(long pickupTime) {
		SpecialPointType type = SpecialPointType.PARKING;
		FavouritePoint point = new FavouritePoint(LAT, LON, type.getName(), type.getCategory(), 0, PARKED_AT);
		point.setPickupDate(pickupTime);
		return point;
	}

	@NonNull
	private FavouritePoint restore(@NonNull FavouritePoint point) {
		FavouritePoint restored = FavouritePoint.fromWpt(point.toWpt(app), point.getCategory());
		assertEquals(SpecialPointType.PARKING, restored.getSpecialPointType());
		return restored;
	}
}
