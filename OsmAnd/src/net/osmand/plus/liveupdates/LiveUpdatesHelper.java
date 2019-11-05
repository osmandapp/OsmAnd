package net.osmand.plus.liveupdates;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.OsmandActionBarActivity;
import net.osmand.plus.helpers.FileNameTranslationHelper;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.Calendar;

public class LiveUpdatesHelper {
	private static final String UPDATE_TIMES_POSTFIX = "_update_times";
	private static final String TIME_OF_DAY_TO_UPDATE_POSTFIX = "_time_of_day_to_update";
	private static final String DOWNLOAD_VIA_WIFI_POSTFIX = "_download_via_wifi";
	private static final String LIVE_UPDATES_ON_POSTFIX = "_live_updates_on";
	private static final String LAST_UPDATE_ATTEMPT_ON_POSTFIX = "_last_update_attempt";
	public static final String LOCAL_INDEX_INFO = "local_index_info";


	private static final int MORNING_UPDATE_TIME = 8;
	private static final int NIGHT_UPDATE_TIME = 21;
	private static final int SHIFT = 1000;

	public static final int DEFAULT_LAST_CHECK = -1;

	private static <T> OsmandSettings.CommonPreference<T> checkPref(OsmandSettings.CommonPreference<T> p) {
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
	public static OsmandSettings.CommonPreference<Boolean> preferenceForLocalIndex(
			String fileName, OsmandSettings settings) {
		final String settingId = fileName + LIVE_UPDATES_ON_POSTFIX;
		return checkPref(settings.registerBooleanPreference(settingId, false));
	}

	public static OsmandSettings.CommonPreference<Boolean> preferenceLiveUpdatesOn(
			String fileName, OsmandSettings settings) {
		final String settingId = fileName + LIVE_UPDATES_ON_POSTFIX;
		return checkPref(settings.registerBooleanPreference(settingId, false));
	}

	public static OsmandSettings.CommonPreference<Boolean> preferenceDownloadViaWiFi(
			String fileName, OsmandSettings settings) {
		final String settingId = fileName + DOWNLOAD_VIA_WIFI_POSTFIX;
		return checkPref(settings.registerBooleanPreference(settingId, false));
	}

	public static OsmandSettings.CommonPreference<Integer> preferenceUpdateFrequency(
			String fileName, OsmandSettings settings) {
		final String settingId = fileName + UPDATE_TIMES_POSTFIX;
		return checkPref(settings.registerIntPreference(settingId, UpdateFrequency.HOURLY.ordinal()));
	}

	public static OsmandSettings.CommonPreference<Integer> preferenceTimeOfDayToUpdate(
			String fileName, OsmandSettings settings) {
		final String settingId = fileName + TIME_OF_DAY_TO_UPDATE_POSTFIX;
		return checkPref(settings.registerIntPreference(settingId, TimeOfDay.NIGHT.ordinal()));
	}

	public static OsmandSettings.CommonPreference<Long> preferenceLastCheck(
			String fileName, OsmandSettings settings) {
		final String settingId = fileName + LAST_UPDATE_ATTEMPT_ON_POSTFIX;
		return checkPref(settings.registerLongPreference(settingId, DEFAULT_LAST_CHECK));
	}

	public static String getNameToDisplay(String fileName, OsmandActionBarActivity activity) {
		return FileNameTranslationHelper.getFileName(activity,
				activity.getMyApplication().getResourceManager().getOsmandRegions(),
				fileName);
	}

	public static String formatDateTime(Context ctx, long dateTime) {
		java.text.DateFormat dateFormat = android.text.format.DateFormat.getMediumDateFormat(ctx);
		java.text.DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(ctx);
		return dateFormat.format(dateTime) + " " + timeFormat.format(dateTime);
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

	public static void runLiveUpdate(Context context, final String fileName, boolean userRequested) {
		final String fnExt = Algorithms.getFileNameWithoutExtension(new File(fileName));
		new PerformLiveUpdateAsyncTask(context, fileName, userRequested).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, fnExt);
	}
}
