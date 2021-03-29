package net.osmand.plus.liveupdates;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.text.format.DateUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.LocalIndexInfo;
import net.osmand.plus.settings.backend.CommonPreference;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.helpers.FileNameTranslationHelper;
import net.osmand.util.Algorithms;

import java.io.File;
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
	public static final String LIVE_UPDATES_LAST_AVAILABLE = "live_updates_last_available";


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
		final String settingId = fileName + LIVE_UPDATES_ON_POSTFIX;
		return checkPref(settings.registerBooleanPreference(settingId, false));
	}

	public static CommonPreference<Boolean> preferenceLiveUpdatesOn(
			String fileName, OsmandSettings settings) {
		final String settingId = fileName + LIVE_UPDATES_ON_POSTFIX;
		return checkPref(settings.registerBooleanPreference(settingId, false));
	}

	public static CommonPreference<Boolean> preferenceDownloadViaWiFi(
			String fileName, OsmandSettings settings) {
		final String settingId = fileName + DOWNLOAD_VIA_WIFI_POSTFIX;
		return checkPref(settings.registerBooleanPreference(settingId, false));
	}

	public static CommonPreference<Integer> preferenceUpdateFrequency(
			String fileName, OsmandSettings settings) {
		final String settingId = fileName + UPDATE_TIMES_POSTFIX;
		return checkPref(settings.registerIntPreference(settingId, UpdateFrequency.HOURLY.ordinal()));
	}

	public static CommonPreference<Integer> preferenceTimeOfDayToUpdate(
			String fileName, OsmandSettings settings) {
		final String settingId = fileName + TIME_OF_DAY_TO_UPDATE_POSTFIX;
		return checkPref(settings.registerIntPreference(settingId, TimeOfDay.NIGHT.ordinal()));
	}

	public static CommonPreference<Long> preferenceLastCheck(
			String fileName, OsmandSettings settings) {
		final String settingId = fileName + LAST_UPDATE_ATTEMPT_ON_POSTFIX;
		return checkPref(settings.registerLongPreference(settingId, DEFAULT_LAST_CHECK));
	}

	public static CommonPreference<Long> preferenceLatestUpdateAvailable(
			String fileName, OsmandSettings settings) {
		final String settingId = fileName + LIVE_UPDATES_LAST_AVAILABLE;
		return checkPref(settings.registerLongPreference(settingId, DEFAULT_LAST_CHECK));
	}

	public static CommonPreference<Long> preferenceLatestUpdateAvailable(OsmandSettings settings) {
		return checkPref(settings.registerLongPreference(LIVE_UPDATES_LAST_AVAILABLE, DEFAULT_LAST_CHECK));
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

	public static String formatHelpDateTime(Context ctx, UpdateFrequency updateFrequency, TimeOfDay timeOfDay, long lastDateTime) {
		if (lastDateTime == DEFAULT_LAST_CHECK) {
			lastDateTime = System.currentTimeMillis();
		}
		switch (updateFrequency) {
			case DAILY: {
				return helpDateTimeBuilder(ctx, R.string.live_update_frequency_day_variant, lastDateTime, 1, TimeUnit.DAYS, timeOfDay);
			}
			case WEEKLY: {
				return helpDateTimeBuilder(ctx, R.string.live_update_frequency_week_variant, lastDateTime, 7, TimeUnit.DAYS, timeOfDay);
			}
			default:
			case HOURLY: {
				return helpDateTimeBuilder(ctx, R.string.live_update_frequency_hour_variant, lastDateTime, 1, TimeUnit.HOURS, timeOfDay);
			}
		}
	}

	private static String helpDateTimeBuilder(Context ctx, int stringResId, long lastDateTime, long sourceDuration, TimeUnit sourceUnit, TimeOfDay timeOfDay) {
		long nextDateTime = lastDateTime + TimeUnit.MILLISECONDS.convert(sourceDuration, sourceUnit);

		if (sourceUnit != TimeUnit.HOURS) {
			Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(nextDateTime);
			calendar.set(Calendar.HOUR_OF_DAY, timeOfDay == TimeOfDay.MORNING ? MORNING_UPDATE_TIME : NIGHT_UPDATE_TIME);
			nextDateTime = calendar.getTimeInMillis();
		}

		int flagsBase = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_MONTH;
		int flagsBaseNoYear = flagsBase | DateUtils.FORMAT_NO_YEAR;
		int flagsTime = DateUtils.FORMAT_SHOW_TIME;

		String date = DateUtils.formatDateTime(ctx, nextDateTime, isCurrentYear(nextDateTime) ? flagsBaseNoYear : flagsBase);
		String time = DateUtils.formatDateTime(ctx, nextDateTime, flagsTime);

		return ctx.getResources().getString(stringResId, DateUtils.isToday(nextDateTime) ? "" : " " + date, time);
	}

	public static PendingIntent getPendingIntent(@NonNull Context context,
												 @NonNull String fileName) {
		Intent intent = new Intent(context, LiveUpdatesAlarmReceiver.class);
		final File file = new File(fileName);
		final String fileNameNoExt = Algorithms.getFileNameWithoutExtension(file);
		intent.putExtra(LOCAL_INDEX_INFO, fileName);
		intent.setAction(fileNameNoExt);
		return PendingIntent.getBroadcast(context, 0, intent, 0);
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
		alarmMgr.setInexactRepeating(AlarmManager.RTC,
				timeOfFirstUpdate, updateFrequency.getTime(), alarmIntent);
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
		HOURLY(R.string.hourly, AlarmManager.INTERVAL_HOUR),
		DAILY(R.string.daily, AlarmManager.INTERVAL_DAY),
		WEEKLY(R.string.weekly, AlarmManager.INTERVAL_DAY * 7);
		private final int localizedId;
		private final long time;

		UpdateFrequency(int localizedId, long time) {
			this.localizedId = localizedId;
			this.time = time;
		}

		public int getLocalizedId() {
			return localizedId;
		}

		public long getTime() {
			return time;
		}
	}

	public static void runLiveUpdate(Context context, final String fileName, boolean userRequested, @Nullable final Runnable runOnSuccess) {
		final String fnExt = Algorithms.getFileNameWithoutExtension(new File(fileName));
		PerformLiveUpdateAsyncTask task = new PerformLiveUpdateAsyncTask(context, fileName, userRequested);
		task.setRunOnSuccess(runOnSuccess);
		task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, fnExt);
	}

	public static void runLiveUpdate(Context context, boolean userRequested, final LiveUpdateListener listener) {
		for (LocalIndexInfo mapToUpdate : listener.getMapsToUpdate()) {
			runLiveUpdate(context, mapToUpdate.getFileName(), userRequested, new Runnable() {
				@Override
				public void run() {
					listener.processFinish();
				}
			});
		}
	}

	public interface LiveUpdateListener {
		void processFinish();

		List<LocalIndexInfo> getMapsToUpdate();
	}
}
