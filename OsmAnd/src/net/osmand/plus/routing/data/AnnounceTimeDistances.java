package net.osmand.plus.routing.data;

import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;

import net.osmand.Location;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.voice.CommandPlayer;

public class AnnounceTimeDistances {
	// Avoids false negatives: Pre-pone close announcements by this distance to allow for the possible over-estimation of the 'true' lead distance due to positioning error.
	// A smaller value will increase the timing precision, but at the risk of missing prompts which do not meet the precision limit.
	// We can research if a flexible value like min(12, x * gps-hdop) has advantages over a constant (x could be 2 or so).
	private static final int POSITIONING_TOLERANCE = 12;

	public static final int STATE_TURN_NOW = 0;
	public static final int STATE_TURN_IN = 1;
	public static final int STATE_PREPARE_TURN = 2;
	public static final int STATE_LONG_PREPARE_TURN = 3;
	public static final int STATE_SHORT_ALARM_ANNOUNCE = 4;
	public static final int STATE_LONG_ALARM_ANNOUNCE = 5;
	public static final int STATE_SHORT_PNT_APPROACH = 6;
	public static final int STATE_LONG_PNT_APPROACH = 7;

	// Default speed to have comfortable announcements (m/s)
	// initial value is updated from default speed settings anyway
	private float DEFAULT_SPEED = 10;
	private double voicePromptDelayTimeSec = 0;

	private float ARRIVAL_DISTANCE;
	private float OFF_ROUTE_DISTANCE;

	private final int PREPARE_LONG_DISTANCE;
	private int PREPARE_LONG_DISTANCE_END;
	private final int PREPARE_DISTANCE;
	private int PREPARE_DISTANCE_END;
	private final int TURN_IN_DISTANCE;
	private final int TURN_IN_DISTANCE_END;
	private int TURN_NOW_DISTANCE;
	private int LONG_PNT_ANNOUNCE_RADIUS;
	private int SHORT_PNT_ANNOUNCE_RADIUS;
	private int LONG_ALARM_ANNOUNCE_RADIUS;
	private int SHORT_ALARM_ANNOUNCE_RADIUS;
	OsmAndLocationProvider locationProvider;

	public AnnounceTimeDistances(ApplicationMode appMode, OsmandApplication app) {
		OsmandSettings settings = app.getSettings();
		locationProvider = app.getLocationProvider();
		if (appMode.isDerivedRoutingFrom(ApplicationMode.CAR)) {
			// keep it as minimum 30 km/h for voice announcement
			DEFAULT_SPEED = Math.max(8, appMode.getDefaultSpeed());
		} else {
			// minimal is 1 meter for turn now
			DEFAULT_SPEED = (float) Math.max(0.3, appMode.getDefaultSpeed());
		}

		// 300 s: car 3750 m (113 s @ 120 km/h)
		PREPARE_LONG_DISTANCE = (int) (DEFAULT_SPEED * 300);
		// 250 s: car 3125 m (94 s @ 120 km/h)
		PREPARE_LONG_DISTANCE_END = (int) (DEFAULT_SPEED * 250);
		if (DEFAULT_SPEED < 30) {
			// Play only for high speed vehicle with speed > 110 km/h
			// [issue 1411] - used only for goAhead prompt
			PREPARE_LONG_DISTANCE_END = PREPARE_LONG_DISTANCE * 2;
		}

		// 115 s: car 1438 m (45 s @ 120 km/h), bicycle 319 m (46 s @ 25 km/h), pedestrian 128 m
		PREPARE_DISTANCE = (int) (DEFAULT_SPEED * 115);
		// 90  s: car 1136 m, bicycle 250 m (36 s @ 25 km/h)
		PREPARE_DISTANCE_END = (int) (DEFAULT_SPEED * 90);

		// 22 s: car 275 m, bicycle 61 m, pedestrian 24 m
		TURN_IN_DISTANCE = (int) (DEFAULT_SPEED * 22);
		// 15 s: car 189 m, bicycle 42 m, pedestrian 17 m
		TURN_IN_DISTANCE_END = (int) (DEFAULT_SPEED * 15);

		// Do not play prepare: for pedestrian and slow transport
		// same check as speed < 150/(90-22) m/s = 2.2 m/s = 8 km/h
		// if (DEFAULT_SPEED < 2.3) {
		if (PREPARE_DISTANCE_END - TURN_IN_DISTANCE < 150) {
			PREPARE_DISTANCE_END = PREPARE_DISTANCE * 2;
		}

		setArrivalDistances(settings.ARRIVAL_DISTANCE_FACTOR.getModeValue(appMode));

		// Trigger close prompts earlier to allow BT SCO link being established, or when VOICE_PROMPT_DELAY is set >0 for the other stream types
		int ams = settings.AUDIO_MANAGER_STREAM.getModeValue(appMode);
		if ((ams == 0 && !CommandPlayer.isBluetoothScoRunning()) || ams > 0) {
			if (settings.VOICE_PROMPT_DELAY[ams] != null) {
				voicePromptDelayTimeSec = (double) settings.VOICE_PROMPT_DELAY[ams].get() / 1000;
			}
		}
	}

	public void setArrivalDistances(float arrivalDistanceFactor) {
		arrivalDistanceFactor = Math.max(arrivalDistanceFactor, 0.1f);

		// TURN_NOW lead time heuristically: Math.min(Math.sqrt(DEFAULT_SPEED * 3.6), 8)
		// car: 6.7 s * 45 km/h = 83 m, bicycle 3.2s * 10 km/h = 8 m -> 12 m,
		// pedestrian 2 s * 4 km/h = 2 m -> 12 m, 1 km/h - 1 s (all floored by POSITIONING_TOLERANCE)
		TURN_NOW_DISTANCE = (int) (Math.max(POSITIONING_TOLERANCE, 
				DEFAULT_SPEED * (float) Math.min(Math.sqrt(DEFAULT_SPEED * 3.6), 8)) * arrivalDistanceFactor);

		// 5 s: car 63 m, bicycle 14 m, pedestrian 6 m -> 12 m (floored by POSITIONING_TOLERANCE)
		ARRIVAL_DISTANCE = (int) (Math.max(POSITIONING_TOLERANCE, DEFAULT_SPEED * 5.) * arrivalDistanceFactor);

		// 20 s: car 250 m, bicycle 56 m, pedestrian 22 m
		OFF_ROUTE_DISTANCE = DEFAULT_SPEED * 20 * (2.0f - arrivalDistanceFactor); // 20 seconds

		// assume for backward compatibility speed - 10 m/s
		SHORT_ALARM_ANNOUNCE_RADIUS = (int) (7 * DEFAULT_SPEED * arrivalDistanceFactor); // 70 m
		LONG_ALARM_ANNOUNCE_RADIUS = (int) (12 * DEFAULT_SPEED * arrivalDistanceFactor); // 120 m
		SHORT_PNT_ANNOUNCE_RADIUS = (int) (15 * DEFAULT_SPEED * arrivalDistanceFactor); // 150 m
		LONG_PNT_ANNOUNCE_RADIUS = (int) (60 * DEFAULT_SPEED * arrivalDistanceFactor); // 600 m
	}

	public int getImminentTurnStatus(float dist, Location loc) {
		float speed = getSpeed(loc);
		if (isTurnStateActive(speed, dist, STATE_TURN_NOW)) {
			return 0;
		} else if (isTurnStateActive(speed, dist, STATE_PREPARE_TURN)) {
			// STATE_TURN_IN included
			return 1;
		} else {
			return -1;
		}
	}

	public boolean isTurnStateActive(float currentSpeed, double dist, int turnType) {
		switch (turnType) {
			case STATE_TURN_NOW:
				if (currentSpeed < DEFAULT_SPEED) {
					// Issue #17376: low speed adjustment for TURN_NOW timing
					return dist <= Math.max(POSITIONING_TOLERANCE, currentSpeed / DEFAULT_SPEED * TURN_NOW_DISTANCE)
							+ currentSpeed * voicePromptDelayTimeSec;
				}
				return isDistanceLess(currentSpeed, dist, TURN_NOW_DISTANCE);
			case STATE_TURN_IN:
				return isDistanceLess(currentSpeed, dist, TURN_IN_DISTANCE);
			case STATE_PREPARE_TURN:
				return isDistanceLess(currentSpeed, dist, PREPARE_DISTANCE);
			case STATE_LONG_PREPARE_TURN:
				return isDistanceLess(currentSpeed, dist, PREPARE_LONG_DISTANCE);
			case STATE_SHORT_ALARM_ANNOUNCE:
				return isDistanceLess(currentSpeed, dist, SHORT_ALARM_ANNOUNCE_RADIUS);
			case STATE_LONG_ALARM_ANNOUNCE:
				return isDistanceLess(currentSpeed, dist, LONG_ALARM_ANNOUNCE_RADIUS);
			case STATE_SHORT_PNT_APPROACH:
				return isDistanceLess(currentSpeed, dist, SHORT_PNT_ANNOUNCE_RADIUS);
			case STATE_LONG_PNT_APPROACH:
				return isDistanceLess(currentSpeed, dist, LONG_PNT_ANNOUNCE_RADIUS);
		}
		return false;
	}

	public boolean isTurnStateNotPassed(float currentSpeed, double dist, int turnType) {
		switch (turnType) {
			case STATE_TURN_IN:
				return !isDistanceLess(currentSpeed, dist, TURN_IN_DISTANCE_END);
			case STATE_PREPARE_TURN:
				return !isDistanceLess(currentSpeed, dist, PREPARE_DISTANCE_END);
			case STATE_LONG_PREPARE_TURN:
				return !isDistanceLess(currentSpeed, dist, PREPARE_LONG_DISTANCE_END);
			case STATE_LONG_PNT_APPROACH:
				return !isDistanceLess(currentSpeed, dist, LONG_PNT_ANNOUNCE_RADIUS * 0.5);
			case STATE_LONG_ALARM_ANNOUNCE:
				return !isDistanceLess(currentSpeed, dist, LONG_ALARM_ANNOUNCE_RADIUS * 0.5);
		}
		return true;
	}

	private boolean isDistanceLess(float currentSpeed, double dist, double leadDist) {
		// Check trigger. Lead distance is scaled up for high speeds. (For cases without such scaling we pass currentSpeed=0.)
		return dist <= Math.max(leadDist, currentSpeed / DEFAULT_SPEED * leadDist) + currentSpeed * voicePromptDelayTimeSec;
	}

	public float getSpeed(Location loc) {
		float speed = DEFAULT_SPEED;
		if (loc != null && loc.hasSpeed()) {
			speed = loc.getSpeed();
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

	private void appendTurnDesc(OsmandApplication app, SpannableStringBuilder builder, String name, int dist, String meter, String second) {
		appendTurnDesc(app, builder, name, dist, (int) (dist / DEFAULT_SPEED), meter, second);
	}

	private void appendTurnDesc(OsmandApplication app, SpannableStringBuilder builder, String name, int dist, int time, String meter, String second) {
		int minDist = (dist / 5) * 5;
		if (time > 15) {
			// round to 5
			time = (time / 5) * 5;
		}
		String distStr = String.format("\n%s: %d - %d %s", name, minDist, minDist + 5, meter);
		String timeStr = String.format("%d %s.", time, second);
		builder.append(app.getString(R.string.ltr_or_rtl_combine_via_comma, distStr, timeStr));

	}

	public Spannable getIntervalsDescription(OsmandApplication app) {
		String meter = app.getString(R.string.m);
		String second = app.getString(R.string.shared_string_sec);
		String turn = app.getString(R.string.shared_string_turn);
		String arrive = app.getString(R.string.announcement_time_arrive);
		String offRoute = app.getString(R.string.announcement_time_off_route);
		String traffic = "\n" + app.getString(R.string.way_alarms);
		String point = "\n" + String.format(
				"%s / %s / %s", app.getString(R.string.shared_string_waypoint),
				app.getString(R.string.favorite), app.getString(R.string.poi)
		);

		String prepare = "   • " + app.getString(R.string.announcement_time_prepare);
		String longPrepare = "   • " + app.getString(R.string.announcement_time_prepare_long);
		String approach = "   • " + app.getString(R.string.announcement_time_approach);
		String passing = "   • " + app.getString(R.string.announcement_time_passing);

		SpannableStringBuilder builder = new SpannableStringBuilder();

		// Turn
		builder.append(turn);
		makeBold(builder, turn);
		if (PREPARE_DISTANCE_END <= PREPARE_DISTANCE) {
			appendTurnDesc(app, builder, prepare, PREPARE_DISTANCE, meter, second);
		}
		if (PREPARE_LONG_DISTANCE_END <= PREPARE_LONG_DISTANCE) {
			appendTurnDesc(app, builder, longPrepare, PREPARE_LONG_DISTANCE, meter, second);
		}
		appendTurnDesc(app, builder, approach, TURN_IN_DISTANCE, meter, second);
		appendTurnDesc(app, builder, passing, TURN_NOW_DISTANCE, (int) (TURN_NOW_DISTANCE / DEFAULT_SPEED), meter, second);

		// Arrive at destination
		appendTurnDesc(app, builder, arrive, (int) (getArrivalDistance()), meter, second);
		makeBoldFormatted(builder, arrive);

		// Off-route
		if (getOffRouteDistance() > 0) {
			appendTurnDesc(app, builder, offRoute, (int) getOffRouteDistance(), meter, second);
			makeBoldFormatted(builder, offRoute);
		}

		// Traffic warnings
		builder.append(traffic);
		makeBold(builder, traffic);
		appendTurnDesc(app, builder, approach, LONG_ALARM_ANNOUNCE_RADIUS, meter, second);
		appendTurnDesc(app, builder, passing, SHORT_ALARM_ANNOUNCE_RADIUS, meter, second);

		// Waypoint / Favorite / POI
		builder.append(point);
		makeBold(builder, point);
		appendTurnDesc(app, builder, approach, LONG_PNT_ANNOUNCE_RADIUS, meter, second);
		appendTurnDesc(app, builder, passing, SHORT_PNT_ANNOUNCE_RADIUS, meter, second);

		return builder;
	}

	private void makeBold(SpannableStringBuilder b, String word) {
		int end = b.length();
		int start = end - word.length();
		b.setSpan(new StyleSpan(Typeface.BOLD), start, end,
				SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE);
	}

	private void makeBoldFormatted(SpannableStringBuilder b, String word) {
		int start = b.toString().indexOf(word);
		int end = start + word.length() + 1;
		b.setSpan(new StyleSpan(Typeface.BOLD), start, end,
				SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE);
	}
}
