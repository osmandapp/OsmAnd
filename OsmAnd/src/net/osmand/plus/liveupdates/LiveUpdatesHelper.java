package net.osmand.plus.liveupdates;

import static net.osmand.plus.liveupdates.LiveUpdatesFragment.getFileNameWithoutRoadSuffix;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.text.format.DateUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.download.local.LocalItem;
import net.osmand.plus.helpers.FileNameTranslationHelper;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.util.Algorithms;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class LiveUpdatesHelper {

	private static final String UPDATE_TIMES_POSTFIX = "_update_times";
	private static final String TIME_OF_DAY_TO_UPDATE_POSTFIX = "_time_of_day_to_update";
	private static final String DOWNLOAD_VIA_WIFI_POSTFIX = "_download_via_wifi";
	private static final String LIVE_UPDATES_ON_POSTFIX = "_live_updates_on";
	private static final String LAST_UPDATE_ATTEMPT_ON_POSTFIX = "_last_update_attempt";
	public static final String LOCAL_INDEX_INFO = "local_index_info";
	public static final String LIVE_UPDATES_LAST_SUCCESSFUL_CHECK = "live_updates_last_available";
	public static final String LIVE_UPDATES_LAST_OSM_UPDATE = "live_updates_last_osm_update";

	private static final int MORNING_UPDATE_TIME = 8;
	private static final int NIGHT_UPDATE_TIME = 21;
	private static final int SHIFT = 1000;

	public static final int DEFAULT_LAST_CHECK = -1;

	private static <T> CommonPreference<T> checkPref(CommonPreference<T> p) {
		if (p.isSet()) {
			T vl = p.get();
			p = p.makeGlobal();
			if (!p.isSet()) {
				p.set(vl);
			}
		} else {
			p = p.makeGlobal();
		}
		return p;
	}

	public static CommonPreference<Boolean> preferenceForLocalIndex(
			String fileName, OsmandSettings settings) {
		String settingId = fileName + LIVE_UPDATES_ON_POSTFIX;
		return checkPref(settings.registerBooleanPreference(settingId, false));
	}

	public static CommonPreference<Boolean> preferenceLiveUpdatesOn(
			String fileName, OsmandSettings settings) {
		String settingId = fileName + LIVE_UPDATES_ON_POSTFIX;
		return checkPref(settings.registerBooleanPreference(settingId, false));
	}

	public static CommonPreference<Boolean> preferenceDownloadViaWiFi(
			String fileName, OsmandSettings settings) {
		String settingId = fileName + DOWNLOAD_VIA_WIFI_POSTFIX;
		return checkPref(settings.registerBooleanPreference(settingId, false));
	}

	public static CommonPreference<Integer> preferenceUpdateFrequency(
			String fileName, OsmandSettings settings) {
		String settingId = fileName + UPDATE_TIMES_POSTFIX;
		return checkPref(settings.registerIntPreference(settingId, UpdateFrequency.HOURLY.ordinal()));
	}

	public static CommonPreference<Integer> preferenceTimeOfDayToUpdate(
			String fileName, OsmandSettings settings) {
		String settingId = fileName + TIME_OF_DAY_TO_UPDATE_POSTFIX;
		return checkPref(settings.registerIntPreference(settingId, TimeOfDay.NIGHT.ordinal()));
	}

	public static CommonPreference<Long> preferenceLastSuccessfulUpdateCheck(
			String fileName, OsmandSettings settings) {
		String settingId = fileName + LIVE_UPDATES_LAST_SUCCESSFUL_CHECK;
		return checkPref(settings.registerLongPreference(settingId, DEFAULT_LAST_CHECK));
	}

	public static CommonPreference<Long> preferenceLastOsmChange(@NonNull String fileName, @NonNull OsmandSettings settings) {
		String prefId = fileName + LIVE_UPDATES_LAST_OSM_UPDATE;
		return checkPref(settings.registerLongPreference(prefId, 0));
	}

	public static CommonPreference<Long> preferenceLastCheck(@NonNull String fileName, @NonNull OsmandSettings settings) {
		String prefId = fileName + LAST_UPDATE_ATTEMPT_ON_POSTFIX;
		return checkPref(settings.registerLongPreference(prefId, DEFAULT_LAST_CHECK));
	}

	public static String getNameToDisplay(String fileName, OsmandApplication context) {
		return FileNameTranslationHelper.getFileName(context,
				context.getResourceManager().getOsmandRegions(),
				fileName);
	}

	public static String formatDateTime(Context ctx, long dateTime) {
		java.text.DateFormat dateFormat = android.text.format.DateFormat.getMediumDateFormat(ctx);
		java.text.DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(ctx);
		return dateFormat.format(dateTime) + " " + timeFormat.format(dateTime);
	}

	public static boolean isCurrentYear(long dateTime) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(dateTime);
		return calendar.get(Calendar.YEAR) == Calendar.getInstance().get(Calendar.YEAR);
	}

	public static String formatShortDateTime(Context ctx, long dateTime) {
		if (dateTime == DEFAULT_LAST_CHECK) {
			return ctx.getResources().getString(R.string.shared_string_never);
		} else {
			String date, times;
			if (DateUtils.isToday(dateTime)) {
				date = ctx.getResources().getString(R.string.today);
			} else {
				int flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_MONTH;
				if (isCurrentYear(dateTime)) {
					flags = flags | DateUtils.FORMAT_NO_YEAR;
				}
				date = DateUtils.formatDateTime(ctx, dateTime, flags);
			}
			times = DateUtils.formatDateTime(ctx, dateTime, DateUtils.FORMAT_SHOW_TIME);
			String pattern = ctx.getString(R.string.ltr_or_rtl_combine_via_dash);
			return String.format(pattern, date, times);
		}
	}

	@NonNull
	public static String getNextUpdateDate(@NonNull Context context, long nextUpdateTimeMillis) {
		int flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_MONTH;
		if (isCurrentYear(nextUpdateTimeMillis)) {
			flags |= DateUtils.FORMAT_NO_YEAR;
		}
		return DateUtils.formatDateTime(context, nextUpdateTimeMillis, flags);
	}

	@NonNull
	public static String getNextUpdateTime(@NonNull Context context, long nextUpdateTimeMillis) {
		return DateUtils.formatDateTime(context, nextUpdateTimeMillis, DateUtils.FORMAT_SHOW_TIME);
	}

	public static long getNextUpdateTimeMillis(long lastUpdateTime,
	                                           @NonNull UpdateFrequency updateFrequency,
	                                           @NonNull TimeOfDay timeOfDay) {
		long nextUpdateTime = lastUpdateTime + updateFrequency.intervalMillis;

		if (updateFrequency.timeUnit != TimeUnit.HOURS) {
			Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(nextUpdateTime);
			calendar.set(Calendar.HOUR_OF_DAY, timeOfDay == TimeOfDay.MORNING ? MORNING_UPDATE_TIME : NIGHT_UPDATE_TIME);
			nextUpdateTime = calendar.getTimeInMillis();
		}

		return nextUpdateTime;
	}
	public static PendingIntent getPendingIntent(@NonNull Context context,
												 @NonNull String fileName) {
		Intent intent = new Intent(context, LiveUpdatesAlarmReceiver.class);
		String fileNameNoExt = Algorithms.getFileNameWithoutExtensionAndRoadSuffix(fileName);
		intent.putExtra(LOCAL_INDEX_INFO, fileName);
		intent.setAction(fileNameNoExt);
		return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
	}

	public static void setAlarmForPendingIntent(PendingIntent alarmIntent, AlarmManager alarmMgr, UpdateFrequency updateFrequency, TimeOfDay timeOfDayToUpdate) {
		long timeOfFirstUpdate;
		switch (updateFrequency) {
			case HOURLY:
				timeOfFirstUpdate = System.currentTimeMillis() + SHIFT;
				break;
			case DAILY:
			case WEEKLY:
				timeOfFirstUpdate = getNextUpdateTime(timeOfDayToUpdate);
				break;
			default:
				throw new IllegalStateException("Unexpected update frequency:"
						+ updateFrequency);
		}
		alarmMgr.setInexactRepeating(AlarmManager.RTC, timeOfFirstUpdate, updateFrequency.intervalMillis, alarmIntent);
	}

	private static long getNextUpdateTime(TimeOfDay timeOfDayToUpdate) {
		Calendar calendar = Calendar.getInstance();
		if (timeOfDayToUpdate == TimeOfDay.MORNING) {
			calendar.add(Calendar.DATE, 1);
			calendar.set(Calendar.HOUR_OF_DAY, MORNING_UPDATE_TIME);
		} else if (timeOfDayToUpdate == TimeOfDay.NIGHT) {
			calendar.add(Calendar.DATE, 1);
			calendar.set(Calendar.HOUR_OF_DAY, NIGHT_UPDATE_TIME);
		}
		return calendar.getTimeInMillis();
	}

	public enum TimeOfDay {
		MORNING(R.string.morning),
		NIGHT(R.string.night);
		private final int localizedId;

		TimeOfDay(int localizedId) {
			this.localizedId = localizedId;
		}

		public int getLocalizedId() {
			return localizedId;
		}


		@Override
		public String toString() {
			return super.toString();
		}
	}

	public enum UpdateFrequency {

		HOURLY(R.string.hourly, R.string.live_update_frequency_hour_variant, AlarmManager.INTERVAL_HOUR, TimeUnit.HOURS),
		DAILY(R.string.daily, R.string.live_update_frequency_day_variant, AlarmManager.INTERVAL_DAY, TimeUnit.DAYS),
		WEEKLY(R.string.weekly, R.string.live_update_frequency_week_variant, AlarmManager.INTERVAL_DAY * 7, TimeUnit.DAYS);

		@StringRes
		public final int titleId;
		@StringRes
		public final int descId;
		public final long intervalMillis;
		public final TimeUnit timeUnit;

		UpdateFrequency(@StringRes int titleId, @StringRes int descId, long intervalMillis, @NonNull TimeUnit timeUnit) {
			this.titleId = titleId;
			this.descId = descId;
			this.intervalMillis = intervalMillis;
			this.timeUnit = timeUnit;
		}
	}

	public static void runLiveUpdate(Context context, String fileName, boolean userRequested, @Nullable Runnable runOnSuccess) {
		String fnExt = Algorithms.getFileNameWithoutExtensionAndRoadSuffix(fileName);
		PerformLiveUpdateAsyncTask task = new PerformLiveUpdateAsyncTask(context, fileName, userRequested);
		task.setRunOnSuccess(runOnSuccess);
		OsmAndTaskManager.executeTask(task, fnExt);
	}

	public static void runLiveUpdate(Context context, boolean userRequested, LiveUpdateListener listener) {
		for (LocalItem mapToUpdate : listener.getMapsToUpdate()) {
			runLiveUpdate(context, getFileNameWithoutRoadSuffix(mapToUpdate), userRequested, listener::processFinish);
		}
	}

	public interface LiveUpdateListener {
		void processFinish();

		List<LocalItem> getMapsToUpdate();
	}
}
