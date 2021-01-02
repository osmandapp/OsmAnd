package net.osmand.plus.routing.data;

import net.osmand.Location;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.voice.AbstractPrologCommandPlayer;

public class AnnounceTimeDistances {
	private static final int DEFAULT_GPS_TOLERANCE = 12;
	public final static int STATE_TURN_NOW = 0;
	public final static int STATE_PREPARE_TURN = 1;
	public final static int STATE_LONG_PREPARE_TURN = 2;
	public final static int STATE_TURN_IN_TURN = 3;
	public final static int STATE_LONG_PNT_ANNOUNCE = 4;
	public final static int STATE_SHORT_PNT_ANNOUNCE = 5;
	public final static int STATE_LONG_ALARM_ANNOUNCE = 6;
	public final static int STATE_SHORT_ALARM_ANNOUNCE = 7;


	// Default speed to have comfortable announcements (Speed in m/s)
	// initial value is updated from default speed settings anyway
	private float DEFAULT_SPEED = 10;
	private float ARRIVAL_DISTANCE_FACTOR = 1;
	private double voicePromptDelayTimeSec = 0;

	private float ARRIVAL_DISTANCE;
	private float OFF_ROUTE_DISTANCE;

	private float TURN_NOW_SPEED;
	private int PREPARE_LONG_DISTANCE;
	private int PREPARE_LONG_DISTANCE_END;
	protected int PREPARE_DISTANCE;
	private int PREPARE_DISTANCE_END;
	private int TURN_IN_DISTANCE;
	private int TURN_IN_DISTANCE_END;
	private int TURN_NOW_DISTANCE;

	// TODO
	private static final int LONG_PNT_ANNOUNCE_RADIUS = 700;
	private static final int SHORT_PNT_ANNOUNCE_RADIUS = 150;
	private static final int LONG_ALARM_ANNOUNCE_RADIUS = 150;
	private static final int SHORT_ALARM_ANNOUNCE_RADIUS = 100;

	public AnnounceTimeDistances(ApplicationMode appMode, OsmandSettings settings) {
		if (appMode.isDerivedRoutingFrom(ApplicationMode.CAR)) {
			// keep it as minimum 30 kmh for voice announcement
			DEFAULT_SPEED = (float) Math.max(8, appMode.getDefaultSpeed());
		} else {
			// minimal is 1 meter for turn now
			DEFAULT_SPEED = (float) Math.max(0.3, appMode.getDefaultSpeed());
		}
		// Calculate minimal distance / time to announce turns, so time to turn is always >= ETALON_TIME
		// Distance < ETALON_DIST or TIME_WITH_CURRENT_SPEED < ETALON_TIME_DEFAULT_SPEED

		// 300 sec: 4 200 - 3 500 m - car [ 115 - 95 sec @ 120 km/h]
		PREPARE_LONG_DISTANCE = (int) (DEFAULT_SPEED * 300);
		PREPARE_LONG_DISTANCE_END = (int) (DEFAULT_SPEED * 250) ;
		if (DEFAULT_SPEED < 30) {
			// Play only for high speed vehicle with speed > 110 km/h
			// [issue 1411] - used only for goAhead prompt
			PREPARE_LONG_DISTANCE_END = PREPARE_LONG_DISTANCE * 2;
		}

		// 115 sec: 1 500 m - car [45 sec @ 120 km/h], 320 m - bicycle [45 sec @ 25 km/h], 230 m - pedestrian
		PREPARE_DISTANCE = (int) (DEFAULT_SPEED * 115);
		// 90  sec: 1 200 m - car, 250 m - bicycle [36 sec @ 25 km/h],
		PREPARE_DISTANCE_END = (int) (DEFAULT_SPEED * 90);

		// 22 sec: 310 m - car, 60 m - bicycle, 50m - pedestrian
		TURN_IN_DISTANCE = (int) (DEFAULT_SPEED  * 22);
		// 15 sec: 210 m - car, 40 m - bicycle, 30 m - pedestrian
		TURN_IN_DISTANCE_END = (int) (DEFAULT_SPEED * 15);

		// Do not play prepare: for pedestrian and slow transport
		// same check as speed < 150/(90-22) m/s = 2.2 m/s = 8 km/h
		// if (DEFAULT_SPEED < 2.3) {
		if (PREPARE_DISTANCE_END - TURN_IN_DISTANCE < 150) {
			PREPARE_DISTANCE_END = PREPARE_DISTANCE * 2;
		}

		// Turn now: 3.5 sec normal speed, 7 second for halfspeed (default)
		// float TURN_NOW_TIME = 7;

		// ** #8749 to keep 1m / 1 sec precision (GPS_TOLERANCE - 12 m)
		// 1 kmh - 1 sec, 4 kmh - 2 sec (pedestrian), 10 kmh - 3 sec (*bicycle), 50 kmh - 7 sec (car)
		float TURN_NOW_TIME = (float) Math.min(Math.sqrt(DEFAULT_SPEED * 3.6), 8);

		// test new: 50 kmh - 50 m (car), 10 kmh - 10 m (bike), 4 kmh - 4 m
		// TURN_NOW_DISTANCE = (int) (TURN_NOW_TIME * DEFAULT_SPEED / 2);
		// old value
		// 1 kmh - 1 m, 4 kmh - 4 m (pedestrian), 10 kmh - 10 m (bicycle), 50 kmh - 50 m (car)
		// TURN_NOW_DISTANCE = (int) (DEFAULT_SPEED * 3.6); // 3.6 sec
		// 50 kmh - 48 m (car), 10 kmh - 20 m, 4 kmh - 15 m, 1 kmh - 12 m
		float factor = Math.max(settings.ARRIVAL_DISTANCE_FACTOR.getModeValue(appMode), 0.1f);
		TURN_NOW_DISTANCE = (int) ((DEFAULT_GPS_TOLERANCE + DEFAULT_SPEED * 2.5) * factor); // 3.6 sec
		TURN_NOW_SPEED = TURN_NOW_DISTANCE / TURN_NOW_TIME;

		// Trigger close prompts earlier to allow BT SCO link being established, or when VOICE_PROMPT_DELAY is set >0 for the other stream types
		int ams = settings.AUDIO_MANAGER_STREAM.getModeValue(appMode);
		if ((ams == 0 && !AbstractPrologCommandPlayer.btScoStatus) || ams > 0) {
			if (settings.VOICE_PROMPT_DELAY[ams] != null) {
				voicePromptDelayTimeSec = (double) settings.VOICE_PROMPT_DELAY[ams].get() / 1000;
			}
		}

		// 5 seconds: car - 80 m @ 50 kmh, bicycle - 45 m @ 25 km/h, bicycle - 25 m @ 10 km/h, pedestrian - 18 m @ 4 km/h,
		ARRIVAL_DISTANCE_FACTOR = Math.max(settings.ARRIVAL_DISTANCE_FACTOR.getModeValue(appMode), 0.1f);
		ARRIVAL_DISTANCE =  (DEFAULT_GPS_TOLERANCE + DEFAULT_SPEED * 5) * ARRIVAL_DISTANCE_FACTOR;

		// 50 kmh - 280 m, 10 kmh - 55 m, 4 kmh - 22 m
		OFF_ROUTE_DISTANCE = DEFAULT_SPEED * 20 * ARRIVAL_DISTANCE_FACTOR; // 20 seconds
	}

	public int getImminentTurnStatus(float dist, Location loc) {
		float speed = getSpeed(loc);
		if (isTurnStateActive(speed, dist, STATE_TURN_NOW)) {
			return 0;
		} else if (isTurnStateActive(speed, dist, STATE_PREPARE_TURN)) {
			// STATE_TURN_IN_TURN included
			return 1;
		} else {
			return -1;
		}
	}

	public boolean isTurnStateActive(float currentSpeed, double dist, int turnType) {
		switch (turnType) {
			case STATE_TURN_IN_TURN:
				return isDistanceLess(currentSpeed, dist, TURN_IN_DISTANCE);
			case STATE_PREPARE_TURN:
				return isDistanceLess(currentSpeed, dist, PREPARE_DISTANCE);
			case STATE_LONG_PREPARE_TURN:
				return isDistanceLess(currentSpeed, dist, PREPARE_LONG_DISTANCE);
			case STATE_TURN_NOW:
				return isDistanceLess(currentSpeed, dist, TURN_NOW_DISTANCE, TURN_NOW_SPEED);
			case STATE_LONG_PNT_ANNOUNCE:
				return isDistanceLess(currentSpeed, dist, LONG_PNT_ANNOUNCE_RADIUS);
			case STATE_SHORT_PNT_ANNOUNCE:
				return isDistanceLess(currentSpeed, dist, SHORT_PNT_ANNOUNCE_RADIUS);
			case STATE_LONG_ALARM_ANNOUNCE:
				return isDistanceLess(currentSpeed, dist, LONG_ALARM_ANNOUNCE_RADIUS);
			case STATE_SHORT_ALARM_ANNOUNCE:
				return isDistanceLess(currentSpeed, dist, SHORT_ALARM_ANNOUNCE_RADIUS);
		}
		return false;
	}

	public boolean isTurnStateNotPassed(float currentSpeed, double dist, int turnType) {
		switch (turnType) {
			case STATE_TURN_IN_TURN:
				return !isDistanceLess(currentSpeed, dist, TURN_IN_DISTANCE_END);
			case STATE_PREPARE_TURN:
				return !isDistanceLess(currentSpeed, dist, PREPARE_DISTANCE_END);
			case STATE_LONG_PREPARE_TURN:
				return !isDistanceLess(currentSpeed, dist, PREPARE_LONG_DISTANCE_END);
			case STATE_LONG_PNT_ANNOUNCE:
				return !isDistanceLess(currentSpeed, dist, LONG_PNT_ANNOUNCE_RADIUS * 0.5);
		}
		return true;
	}

	private boolean isDistanceLess(float currentSpeed, double dist, double etalon) {
		return isDistanceLess(currentSpeed, dist, etalon, DEFAULT_SPEED);
	}

	private boolean isDistanceLess(float currentSpeed, double dist, double etalon, float defSpeed) {
		if (dist - voicePromptDelayTimeSec * currentSpeed <= etalon) {
			return true;
		}
		// check only if speed > 0
		if (currentSpeed > 0 && (dist / currentSpeed - voicePromptDelayTimeSec) <= etalon / defSpeed) {
			return true;
		}
		return false;
	}

	public float getSpeed(Location loc) {
		float speed = DEFAULT_SPEED;
		if (loc != null && loc.hasSpeed()) {
			speed = Math.max(loc.getSpeed(), speed);
		}
		return speed;
	}

	public float getOffRouteDistance() {
		return OFF_ROUTE_DISTANCE;
	}

	public float getArrivalDistance() {
		return ARRIVAL_DISTANCE;
	}

	public int calcDistanceWithoutDelay(float speed, int dist) {
		return (int) (dist - voicePromptDelayTimeSec * speed);
	}
}