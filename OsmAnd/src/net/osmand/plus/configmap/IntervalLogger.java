package net.osmand.plus.configmap;

import androidx.annotation.NonNull;

import net.osmand.PlatformUtil;

import org.apache.commons.logging.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class IntervalLogger {

	private static final Log LOG = PlatformUtil.getLog(IntervalLogger.class);

	private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("hh:mm:ss.SSS", Locale.US);

	private static final String START_MESSAGE_PATTERN = " * %1$s started at %2$s";
	private static final String FINISH_MESSAGE_PATTERN = "%1$s %2$s ms (%3$s)";

	private static Map<EventType, Long> startPoints = new HashMap<>();

	public enum EventType {
		// implement your events here
		TOTAL,

		APP_ON_CREATE_TOTAL,

		APP_ON_CREATE_PART_1,
		APP_ON_CREATE_PART_2,
		APP_ON_CREATE_APP_INITIALIZER,

		MAP_ON_CREATE_TOTAL,

		MAP_ON_CREATE_PART_1,
		MAP_ON_CREATE_CONTENT_VIEW,
		MAP_ON_CREATE_PART_2,
		MAP_ON_CREATE_DASHBOARD,
		MAP_ON_CREATE_PART_3,
		MAP_ON_CREATE_MAP_LAYERS,
		MAP_ON_CREATE_PART_4,
		MAP_ON_CREATE_PART_5,

		MAP_ON_RESUME_TOTAL,

		MAP_ON_RESUME_PART_1,
		MAP_ON_RESUME_UPDATE_APP_MODE_SETTINGS,
		MAP_ON_RESUME_PART_2,
		MAP_ON_RESUME_LOCATION_PROVIDER,
		MAP_ON_RESUME_PART_3,
	}

	public static void start(@NonNull EventType event) {
		start(event, false);
	}

	public static void start(@NonNull EventType event, boolean showMessage) {
		long now = updateRecordedStartPoints(event, true);
		if (showMessage) {
			LOG.debug(String.format(START_MESSAGE_PATTERN, event.name(), formatTime(now)));
		}
	}

	public static void finish(@NonNull EventType event) {
		Long startTime = startPoints.get(event);
		if (startTime != null) {
			long now = updateRecordedStartPoints(event, false);
			long dif = now - startTime;
			LOG.debug(String.format(FINISH_MESSAGE_PATTERN, event.name(), dif, formatTime(now)));
		}
	}

	public static void nextLine() {
		LOG.debug("");
	}

	private static long updateRecordedStartPoints(@NonNull EventType event, boolean add) {
		Map<EventType, Long> updated = new HashMap<>(startPoints);
		long now = System.currentTimeMillis();
		if (add) {
			updated.put(event, now);
		} else {
			updated.remove(event);
		}
		startPoints = updated;
		return now;
	}

	private static String formatTime(long time) {
		return TIME_FORMAT.format(new Date(time));
	}

}
