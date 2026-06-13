package net.osmand.plus.plugins.aistracker;

import static net.osmand.plus.plugins.aistracker.AisTrackerHelper.getCpa;
import static net.osmand.plus.plugins.aistracker.AisTrackerHelper.knotsToMeterPerSecond;
import static net.osmand.plus.plugins.aistracker.AisTrackerHelper.meterToMiles;
import static net.osmand.plus.utils.OsmAndFormatter.FORMAT_MINUTES;

import android.util.Log;

import androidx.annotation.NonNull;

import net.osmand.Location;
import net.osmand.LocationConvert;

public class AisSimulationProvider {

	private final AisTrackerPlugin plugin;

	public AisSimulationProvider(@NonNull AisTrackerPlugin plugin) {
		this.plugin = plugin;
	}

	public void testCrossingTimes() {
		// here some tests for the geo (CPA) calculation
		// intention is to test the function to calculate times of two objects with crossing courses
		// define 12 positions on two courses: position a1 ... a6 at course line A (course 90°)
		//   and position b1 ... b6 at course line B (course 45°)
		// the positions are taken from a (paper) map
		// for coordinate transformation see https://www.koordinaten-umrechner.de
		AisTrackerHelper.Cpa cpa = new AisTrackerHelper.Cpa();
		Location a1 = new Location("test", 49.5d, -3.266667d); // 49°30'N, 3°16'W
		Location a2 = new Location("test", 49.5d, -3.166667d); // 49°30'N, 3°10'W
		Location a3 = new Location("test", 49.5d, -3.116667d); // 49°30'N, 3°7'W
		Location a4 = new Location("test", 49.5d, -3.093333d); // 49°30'N, 3°5.6'W
		Location a5 = new Location("test", 49.5d, -3.05d); // 49°30'N, 3°3'W
		Location a6 = new Location("test", 49.5d, -3.016667d); // 49°30'N, 3°1'W
		Location b1 = new Location("test", 49.395d, -3.25d); // 49°23.7'N, 3°15'W
		Location b2 = new Location("test", 49.441667d, -3.183333d); // 49°26.5'N, 3°11'W
		Location b3 = new Location("test", 49.47d, -3.133333d); // 49°28.2'N, 3°8'W
		Location b4 = new Location("test", 49.5d, -3.093333d); // 49°30'N, 3°5.6'W
		Location b5 = new Location("test", 49.513333d, -3.066667d); // 49°30.8'N, 3°4'W
		Location b6 = new Location("test", 49.538333d, -3.033333d); // 49°32.3'N, 3°2'W
		a1.setSpeed(knotsToMeterPerSecond(1.0f));
		a1.setBearing(90.0f);
		a2.setSpeed(knotsToMeterPerSecond(1.0f));
		a2.setBearing(90.0f);
		a3.setSpeed(knotsToMeterPerSecond(1.0f));
		a3.setBearing(90.0f);
		a4.setSpeed(knotsToMeterPerSecond(1.0f));
		a4.setBearing(90.0f);
		a5.setSpeed(knotsToMeterPerSecond(1.0f));
		a5.setBearing(90.0f);
		a6.setSpeed(knotsToMeterPerSecond(1.0f));
		a6.setBearing(90.0f);
		b1.setSpeed(knotsToMeterPerSecond(1.0f));
		b1.setBearing(45.0f);
		b2.setSpeed(knotsToMeterPerSecond(1.0f));
		b2.setBearing(45.0f);
		b3.setSpeed(knotsToMeterPerSecond(1.0f));
		b3.setBearing(45.0f);
		b4.setSpeed(knotsToMeterPerSecond(1.0f));
		b4.setBearing(45.0f);
		b5.setSpeed(knotsToMeterPerSecond(1.0f));
		b5.setBearing(45.0f);
		b6.setSpeed(knotsToMeterPerSecond(1.0f));
		b6.setBearing(45.0f);
		// now trigger the calculations:
		cpa.reset();
		getCpa(a3, b2, cpa); // expected: t1>0, t2>0
		Log.d("AisTrackerLayer", "# test(a3, b2): t1->" + cpa.getCrossingTime1() +
				", t2->" + cpa.getCrossingTime2() + ", CPA-Dist-> " + cpa.getCpaDist() +
				", TCPA ->" + cpa.getTcpa());
		cpa.reset();
		getCpa(a3, b3, cpa); // expected: t1>0, t2>0
		Log.d("AisTrackerLayer", "# test(a3, b3): t1->" + cpa.getCrossingTime1() +
				", t2->" + cpa.getCrossingTime2() + ", CPA-Dist-> " + cpa.getCpaDist() +
				", TCPA ->" + cpa.getTcpa());
		cpa.reset();
		getCpa(a3, b4, cpa); // expected: t1>0, t2->0
		Log.d("AisTrackerLayer", "# test(a3, b4): t1->" + cpa.getCrossingTime1() +
				", t2->" + cpa.getCrossingTime2() + ", CPA-Dist-> " + cpa.getCpaDist() +
				", TCPA ->" + cpa.getTcpa());
		cpa.reset();
		getCpa(a3, b5, cpa); // expected: t1>0, t2<0
		Log.d("AisTrackerLayer", "# test(a3, b5): t1->" + cpa.getCrossingTime1() +
				", t2->" + cpa.getCrossingTime2() + ", CPA-Dist-> " + cpa.getCpaDist() +
				", TCPA ->" + cpa.getTcpa());
		cpa.reset();
		getCpa(a3, b6, cpa); // expected: t1>0, t2<0
		Log.d("AisTrackerLayer", "# test(a3, b6): t1->" + cpa.getCrossingTime1() +
				", t2->" + cpa.getCrossingTime2() + ", CPA-Dist-> " + cpa.getCpaDist() +
				", TCPA ->" + cpa.getTcpa());
		cpa.reset();
		getCpa(a4, b2, cpa); // expected: t1->0, t2>0
		Log.d("AisTrackerLayer", "# test(a4, b2): t1->" + cpa.getCrossingTime1() +
				", t2->" + cpa.getCrossingTime2() + ", CPA-Dist-> " + cpa.getCpaDist() +
				", TCPA ->" + cpa.getTcpa());
		cpa.reset();
		getCpa(a4, b3, cpa); // expected: t1->0, t2>0
		Log.d("AisTrackerLayer", "# test(a4, b3): t1->" + cpa.getCrossingTime1() +
				", t2->" + cpa.getCrossingTime2() + ", CPA-Dist-> " + cpa.getCpaDist() +
				", TCPA ->" + cpa.getTcpa());
		cpa.reset();
		getCpa(a4, b4, cpa); // expected: t1->0, t2->0
		Log.d("AisTrackerLayer", "# test(a4, b4): t1->" + cpa.getCrossingTime1() +
				", t2->" + cpa.getCrossingTime2() + ", CPA-Dist-> " + cpa.getCpaDist() +
				", TCPA ->" + cpa.getTcpa());
		cpa.reset();
		getCpa(a4, b5, cpa); // expected: t1->0, t2<0
		Log.d("AisTrackerLayer", "# test(a4, b5): t1->" + cpa.getCrossingTime1() +
				", t2->" + cpa.getCrossingTime2() + ", CPA-Dist-> " + cpa.getCpaDist() +
				", TCPA ->" + cpa.getTcpa());
		cpa.reset();
		getCpa(a4, b6, cpa); // expected: t1->0, t2<0
		Log.d("AisTrackerLayer", "# test(a4, b6): t1->" + cpa.getCrossingTime1() +
				", t2->" + cpa.getCrossingTime2() + ", CPA-Dist-> " + cpa.getCpaDist() +
				", TCPA ->" + cpa.getTcpa());
		cpa.reset();
		getCpa(a5, b2, cpa); // expected: t1<0, t2>0
		Log.d("AisTrackerLayer", "# test(a5, b2): t1->" + cpa.getCrossingTime1() +
				", t2->" + cpa.getCrossingTime2() + ", CPA-Dist-> " + cpa.getCpaDist() +
				", TCPA ->" + cpa.getTcpa());
		cpa.reset();
		getCpa(a5, b3, cpa); // expected: t1<0, t2>0
		Log.d("AisTrackerLayer", "# test(a5, b3): t1->" + cpa.getCrossingTime1() +
				", t2->" + cpa.getCrossingTime2() + ", CPA-Dist-> " + cpa.getCpaDist() +
				", TCPA ->" + cpa.getTcpa());
		cpa.reset();
		getCpa(a5, b4, cpa); // expected: t1<0, t2->0
		Log.d("AisTrackerLayer", "# test(a5, b4): t1->" + cpa.getCrossingTime1() +
				", t2->" + cpa.getCrossingTime2() + ", CPA-Dist-> " + cpa.getCpaDist() +
				", TCPA ->" + cpa.getTcpa());
		cpa.reset();
		getCpa(a5, b5, cpa); // expected: t1<0, t2<0
		Log.d("AisTrackerLayer", "# test(a5, b5): t1->" + cpa.getCrossingTime1() +
				", t2->" + cpa.getCrossingTime2() + ", CPA-Dist-> " + cpa.getCpaDist() +
				", TCPA ->" + cpa.getTcpa());
		cpa.reset();
		getCpa(a5, b6, cpa); // expected: t1<0, t2<0
		Log.d("AisTrackerLayer", "# test(a5, b6): t1->" + cpa.getCrossingTime1() +
				", t2->" + cpa.getCrossingTime2() + ", CPA-Dist-> " + cpa.getCpaDist() +
				", TCPA ->" + cpa.getTcpa());

	}

	public void testCpa() {
		// here some tests for the geo (CPA) calculation
		// define 3 (vessel) objects
		// for coordinate transformation see https://www.koordinaten-umrechner.de
		Location x1 = new Location("test", 49.5d, -1.0d); // 49°30'N, 1°00'W
		Location x2 = new Location("test", 49.916667d, 0.416667d); // 49°55'N, 0°25'E
		Location x3 = new Location("test", 49.666667d, -0.75d); // 49°40'N, 0°45'W
		Location x4 = new Location("test", 49.5d, -4.0d); // 49°30'N, 4°00'W
		Location x5 = new Location("test", 50.0d, -3.75d); // 50°00'N, 3°45'W
		// taken from marine chart: distances: x1 - x3: 13.8 nm, x2 - x3: 47,2 nm, x4 - x5: 31.4 nm
		Location y1, y2, y3, y4, y5;
		Log.d("AisTrackerLayer", "# test0: position 1 after 0 hours: "
				+ LocationConvert.convertLatitude(x1.getLatitude(), FORMAT_MINUTES, true)
				+ ", " + LocationConvert.convertLongitude(x1.getLongitude(), FORMAT_MINUTES, true));
		Log.d("AisTrackerLayer", "# test0: position 2 after 0 hours: "
				+ LocationConvert.convertLatitude(x2.getLatitude(), FORMAT_MINUTES, true)
				+ ", " + LocationConvert.convertLongitude(x2.getLongitude(), FORMAT_MINUTES, true));
		Log.d("AisTrackerLayer", "# test0: position 3 after 0 hours: "
				+ LocationConvert.convertLatitude(x3.getLatitude(), FORMAT_MINUTES, true)
				+ ", " + LocationConvert.convertLongitude(x3.getLongitude(), FORMAT_MINUTES, true));
		Log.d("AisTrackerLayer", "# test0: position 4 after 0 hours: "
				+ LocationConvert.convertLatitude(x4.getLatitude(), FORMAT_MINUTES, true)
				+ ", " + LocationConvert.convertLongitude(x4.getLongitude(), FORMAT_MINUTES, true));
		Log.d("AisTrackerLayer", "# test0: position 5 after 0 hours: "
				+ LocationConvert.convertLatitude(x5.getLatitude(), FORMAT_MINUTES, true)
				+ ", " + LocationConvert.convertLongitude(x5.getLongitude(), FORMAT_MINUTES, true));

		// test case: x1: course 0°, speed 5kn, x3: course 270°, speed 10kn, time: 1h, 1.5h
		// taken from marine chart:
		//  position after 1h: x1: 49°35'N, 1°00'W, x3: 49°40'N, 1°0.5'W, distance: 5.0nm
		//  position after 1.5h: x1: 49°37.5'N, 1°00'W, x3: 49°40'N, 1°8.5'W, distance: 6.0nm
		x1.setSpeed(knotsToMeterPerSecond(5.0f));
		x1.setBearing(0.0f);
		x3.setSpeed(knotsToMeterPerSecond(10.0f));
		x3.setBearing(270.0f);
		AisTrackerHelper.Cpa cpa1 = new AisTrackerHelper.Cpa();
		getCpa(x1, x3, cpa1);
		Log.d("AisTrackerLayer", "# test1: tcpa(x1, x3): " + cpa1.getTcpa());
		Log.d("AisTrackerLayer", "# test1: dist at tcpa: " + cpa1.getCpaDist());
		Log.d("AisTrackerLayer", "# test1: dist0: " + meterToMiles(x1.distanceTo(x3)));
		y1 = AisTrackerHelper.getNewPosition(x1, 1.0);
		y3 = AisTrackerHelper.getNewPosition(x3, 1.0);
		if ((y1 != null) && (y3 != null)) {
			Log.d("AisTrackerLayer", "# test1: position 1 after 1 hour: "
					+ LocationConvert.convertLatitude(y1.getLatitude(), FORMAT_MINUTES, true)
					+ ", " + LocationConvert.convertLongitude(y1.getLongitude(), FORMAT_MINUTES, true));
			Log.d("AisTrackerLayer", "# test1: position 3 after 1 hour: "
					+ LocationConvert.convertLatitude(y3.getLatitude(), FORMAT_MINUTES, true)
					+ ", " + LocationConvert.convertLongitude(y3.getLongitude(), FORMAT_MINUTES, true));
			Log.d("AisTrackerLayer", "# test1: dist1: " + meterToMiles(y1.distanceTo(y3)));
		}
		y1 = AisTrackerHelper.getNewPosition(x1, 1.18);
		y3 = AisTrackerHelper.getNewPosition(x3, 1.18);
		if ((y1 != null) && (y3 != null)) {
			Log.d("AisTrackerLayer", "# test1: position 1 after 1.18 hours: "
					+ LocationConvert.convertLatitude(y1.getLatitude(), FORMAT_MINUTES, true)
					+ ", " + LocationConvert.convertLongitude(y1.getLongitude(), FORMAT_MINUTES, true));
			Log.d("AisTrackerLayer", "# test1: position 3 after 1.18 hours: "
					+ LocationConvert.convertLatitude(y3.getLatitude(), FORMAT_MINUTES, true)
					+ ", " + LocationConvert.convertLongitude(y3.getLongitude(), FORMAT_MINUTES, true));
			Log.d("AisTrackerLayer", "# test1: dist1: " + meterToMiles(y1.distanceTo(y3)));
		}
		y1 = AisTrackerHelper.getNewPosition(x1, 1.5);
		y3 = AisTrackerHelper.getNewPosition(x3, 1.5);
		if ((y1 != null) && (y3 != null)) {
			Log.d("AisTrackerLayer", "# test1: position 1 after 1.5 hours: "
					+ LocationConvert.convertLatitude(y1.getLatitude(), FORMAT_MINUTES, true)
					+ ", " + LocationConvert.convertLongitude(y1.getLongitude(), FORMAT_MINUTES, true));
			Log.d("AisTrackerLayer", "# test1: position 3 after 1.5 hours: "
					+ LocationConvert.convertLatitude(y3.getLatitude(), FORMAT_MINUTES, true)
					+ ", " + LocationConvert.convertLongitude(y3.getLongitude(), FORMAT_MINUTES, true));
			Log.d("AisTrackerLayer", "# test1: dist1: " + meterToMiles(y1.distanceTo(y3)));
		}

		// test case: x1: course 0°, speed 5kn, x3: course 270°, speed 5kn, time 1h, 1.5h, 2h
		// taken from marine chart:
		//  position after 1h: x1: 49°35'N, 1°00'W, x3: 49°40'N, 0°52.7'W, distance: 6.8nm
		//  position after 1.5h: x1: 49°37.5'N, 1°00'W, x3: 49°40'N, 0°56.7'W, distance: 3.1nm
		//  position after 2h: x1: 49°40'N, 1°00'W, x3: 49°40'N, 1°0.5'W, distance: 0.3nm
		x1.setSpeed(knotsToMeterPerSecond(5.0f));
		x1.setBearing(0.0f);
		x3.setSpeed(knotsToMeterPerSecond(5.0f));
		x3.setBearing(270.0f);
		cpa1.reset();
		getCpa(x1, x3, cpa1);
		Log.d("AisTrackerLayer", "# test2: tcpa(x1, x3): " + cpa1.getTcpa());
		Log.d("AisTrackerLayer", "# test2: dist at tcpa: " + cpa1.getCpaDist());
		Log.d("AisTrackerLayer", "# test2: dist0: " + meterToMiles(x1.distanceTo(x3)));
		y1 = AisTrackerHelper.getNewPosition(x1, 1.0);
		y3 = AisTrackerHelper.getNewPosition(x3, 1.0);
		if ((y1 != null) && (y3 != null)) {
			Log.d("AisTrackerLayer", "# test2: position 1 after 1 hour: "
					+ LocationConvert.convertLatitude(y1.getLatitude(), FORMAT_MINUTES, true)
					+ ", " + LocationConvert.convertLongitude(y1.getLongitude(), FORMAT_MINUTES, true));
			Log.d("AisTrackerLayer", "# test2: position 3 after 1 hour: "
					+ LocationConvert.convertLatitude(y3.getLatitude(), FORMAT_MINUTES, true)
					+ ", " + LocationConvert.convertLongitude(y3.getLongitude(), FORMAT_MINUTES, true));
			Log.d("AisTrackerLayer", "# test2: dist1: " + meterToMiles(y1.distanceTo(y3)));
		}
		y1 = AisTrackerHelper.getNewPosition(x1, 1.5);
		y3 = AisTrackerHelper.getNewPosition(x3, 1.5);
		if ((y1 != null) && (y3 != null)) {
			Log.d("AisTrackerLayer", "# test2: position 1 after 1.5 hours: "
					+ LocationConvert.convertLatitude(y1.getLatitude(), FORMAT_MINUTES, true)
					+ ", " + LocationConvert.convertLongitude(y1.getLongitude(), FORMAT_MINUTES, true));
			Log.d("AisTrackerLayer", "# test2: position 3 after 1.5 hours: "
					+ LocationConvert.convertLatitude(y3.getLatitude(), FORMAT_MINUTES, true)
					+ ", " + LocationConvert.convertLongitude(y3.getLongitude(), FORMAT_MINUTES, true));
			Log.d("AisTrackerLayer", "# test2: dist1: " + meterToMiles(y1.distanceTo(y3)));
		}
		y1 = AisTrackerHelper.getNewPosition(x1, 2.0);
		y3 = AisTrackerHelper.getNewPosition(x3, 2.0);
		if ((y1 != null) && (y3 != null)) {
			Log.d("AisTrackerLayer", "# test2: position 1 after 2 hours: "
					+ LocationConvert.convertLatitude(y1.getLatitude(), FORMAT_MINUTES, true)
					+ ", " + LocationConvert.convertLongitude(y1.getLongitude(), FORMAT_MINUTES, true));
			Log.d("AisTrackerLayer", "# test2: position 3 after 2 hours: "
					+ LocationConvert.convertLatitude(y3.getLatitude(), FORMAT_MINUTES, true)
					+ ", " + LocationConvert.convertLongitude(y3.getLongitude(), FORMAT_MINUTES, true));
			Log.d("AisTrackerLayer", "# test2: dist1: " + meterToMiles(y1.distanceTo(y3)));
		}

		// test case: x2: course 270°, speed 5kn, x3: course 45°, speed 5kn, time 5h
		// taken from marine chart:
		//  position after 5h: x2: 49°55'N, 0°14.1'W, x3: 49°57.8'N, 0°17.5'W, distance: 3.5nm
		x2.setSpeed(knotsToMeterPerSecond(5.0f));
		x2.setBearing(270.0f);
		x3.setSpeed(knotsToMeterPerSecond(5.0f));
		x3.setBearing(45.0f);
		cpa1.reset();
		getCpa(x2, x3, cpa1);
		Log.d("AisTrackerLayer", "# test3: tcpa(x1, x3): " + cpa1.getTcpa());
		Log.d("AisTrackerLayer", "# test3: dist at tcpa: " + cpa1.getCpaDist());
		Log.d("AisTrackerLayer", "# test3: dist0: " + meterToMiles(x2.distanceTo(x3)));
		y2 = AisTrackerHelper.getNewPosition(x2, 5.0);
		y3 = AisTrackerHelper.getNewPosition(x3, 5.0);
		if ((y2 != null) && (y3 != null)) {
			Log.d("AisTrackerLayer", "# test3: position 2 after 5 hours: "
					+ LocationConvert.convertLatitude(y2.getLatitude(), FORMAT_MINUTES, true)
					+ ", " + LocationConvert.convertLongitude(y2.getLongitude(), FORMAT_MINUTES, true));
			Log.d("AisTrackerLayer", "# test3: position 3 after 5 hours: "
					+ LocationConvert.convertLatitude(y3.getLatitude(), FORMAT_MINUTES, true)
					+ ", " + LocationConvert.convertLongitude(y3.getLongitude(), FORMAT_MINUTES, true));
			Log.d("AisTrackerLayer", "# test3: dist1: " + meterToMiles(y2.distanceTo(y3)));
		}

		// test case: x4: course 45°, speed 10kn, x5: course 70°, speed 5kn, time 6h
		// taken from marine chart:
		//  position after 6h: x4: 50°12.1'N, 2°54.4'W, x5: 50°10.1'N, 3°1.5'W, distance: 5nm
		x4.setSpeed(knotsToMeterPerSecond(10.0f));
		x4.setBearing(45.0f);
		x5.setSpeed(knotsToMeterPerSecond(5.0f));
		x5.setBearing(70.0f);
		cpa1.reset();
		getCpa(x4, x5, cpa1);
		Log.d("AisTrackerLayer", "# test4: tcpa(x4, x5): " + cpa1.getTcpa());
		Log.d("AisTrackerLayer", "# test4: dist at tcpa: " + cpa1.getCpaDist());
		Log.d("AisTrackerLayer", "# test4: dist0: " + meterToMiles(x4.distanceTo(x5)));
		y4 = AisTrackerHelper.getNewPosition(x4, 6.0);
		y5 = AisTrackerHelper.getNewPosition(x5, 6.0);
		if ((y4 != null) && (y5 != null)) {
			Log.d("AisTrackerLayer", "# test4: position 4 after 6 hours: "
					+ LocationConvert.convertLatitude(y4.getLatitude(), FORMAT_MINUTES, true)
					+ ", " + LocationConvert.convertLongitude(y4.getLongitude(), FORMAT_MINUTES, true));
			Log.d("AisTrackerLayer", "# test4: position 5 after 6 hours: "
					+ LocationConvert.convertLatitude(y5.getLatitude(), FORMAT_MINUTES, true)
					+ ", " + LocationConvert.convertLongitude(y5.getLongitude(), FORMAT_MINUTES, true));
			Log.d("AisTrackerLayer", "# test4: dist1: " + meterToMiles(y4.distanceTo(y5)));
		}
	}

	public void initFakePosition() {
		// fake the own position, course and speed to a (fixed) hard coded value
		double fakeLat = 50.76077d;
		double fakeLon = 7.08747d;
		float fakeCOG = 340.0f;
		//float fakeCOG = 100.0f;
		float fakeSOG = 3.0f;
		Location fake = new Location("test", fakeLat, fakeLon);
		fake.setBearing(fakeCOG);
		fake.setSpeed(knotsToMeterPerSecond(fakeSOG));
		plugin.fakeOwnPosition(fake);
		Log.d("AisTrackerLayer", "initFakePosition: fake: " + fake.toString());
		// in order to visualize this faked (own) position on the map, create an AIS object at this location...
		AisObject ais = new AisObject(324578, 18, 20, AisObjectConstants.INVALID_NAV_STATUS,
				AisObjectConstants.INVALID_MANEUVER_INDICATOR,
				(int) fakeCOG, fakeCOG, fakeSOG, fakeLat, fakeLon, AisObjectConstants.INVALID_ROT);
		plugin.onAisObjectReceived(ais);
		ais = new AisObject(324578, 24, 0, "callsign", "fake", 60, 56,
				65, 8, 12, AisObjectConstants.INVALID_DRAUGHT,
				"home", AisObjectConstants.INVALID_ETA, AisObjectConstants.INVALID_ETA,
				AisObjectConstants.INVALID_ETA_HOUR, AisObjectConstants.INVALID_ETA_MIN);
		plugin.onAisObjectReceived(ais);
		//AisObject ais = new AisObject(324578, 1, 20, 0, 1, (int)fakeCOG,
		//        fakeCOG, fakeSOG, fakeLat, fakeLon, 0.0);
		//updateAisObjectList(ais);
		//ais = new AisObject(324578, 5, 0, "own-position", "fake", 60 /* passenger */, 56,
		//        65, 8, 12, 2,
		//        "home", 8, 15, 22, 5);
		//updateAisObjectList(ais);
	}

	public void initTestPassengerShip() {
		// passenger ship
		AisObject ais = new AisObject(34568, 1, 20, 0, 1, 320,
				320.0, 8.4, 50.738d, 7.099d, 0.0);
		plugin.onAisObjectReceived(ais);
		ais = new AisObject(34568, 5, 0, "TEST-CALLSIGN1", "TEST-Ship", 60 /* passenger */, 56,
				65, 8, 12, 2,
				"Potsdam", 8, 15, 22, 5);
		plugin.onAisObjectReceived(ais);
	}

	public void initTestSailingBoat() {
		// SailingBoat
		AisObject ais = new AisObject(454011, 18, 20, AisObjectConstants.INVALID_NAV_STATUS,
				AisObjectConstants.INVALID_MANEUVER_INDICATOR,
				125, 125.0, 4.4, 50.737d, 7.098d, AisObjectConstants.INVALID_ROT);
		plugin.onAisObjectReceived(ais);
		ais = new AisObject(454011, 24, 0, "TEST-CALLSIGN2", "TEST-Sailor", 36 /* sailing  */, 0,
				0, 0, 0, AisObjectConstants.INVALID_DRAUGHT,
				"home", AisObjectConstants.INVALID_ETA, AisObjectConstants.INVALID_ETA,
				AisObjectConstants.INVALID_ETA_HOUR, AisObjectConstants.INVALID_ETA_MIN);
		plugin.onAisObjectReceived(ais);
	}

	public void initTestLandStation() {
		// LandStation
		AisObject ais = new AisObject(878121, 4, 50.736d, 7.100d);
		plugin.onAisObjectReceived(ais);
		// AIDS
		ais = new AisObject(521077, 21, 50.735d, 7.101d, 1,
				0, 0, 0, 0);
		plugin.onAisObjectReceived(ais);
	}

	public void initTestAircraft() {
		// Aircraft
		AisObject ais = new AisObject(910323, 9, 15, 65, 180.5, 55.0, 50.734d, 7.102d);
		plugin.onAisObjectReceived(ais);
	}

	public void initTestLawEnforcement() {
		// LawEnforcement
		AisObject ais = new AisObject(34569, 1, 20, 5 /* moored */, 1, 15,
				25.0, 8.4, 50.739d, 7.0931d, 0.0);
		plugin.onAisObjectReceived(ais);
		ais = new AisObject(34569, 5, 0, "TEST-CALLSIGN3",
				"Mecklenburg Vorpommern", 55 /* law enforcement */, 26,
				5, 8, 4, 1,
				"Potsdam", 8, 15, 22, 5);
		plugin.onAisObjectReceived(ais);
	}
}