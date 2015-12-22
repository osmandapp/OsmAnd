package net.osmand.plus.liveupdates;

import android.content.Context;

import net.osmand.plus.OsmandSettings;
import net.osmand.plus.activities.LocalIndexInfo;
import net.osmand.plus.activities.OsmandActionBarActivity;
import net.osmand.plus.helpers.FileNameTranslationHelper;

public class LiveUpdatesHelper {
	private static final String UPDATE_TIMES_POSTFIX = "_update_times";
	private static final String TIME_OF_DAY_TO_UPDATE_POSTFIX = "_time_of_day_to_update";
	private static final String DOWNLOAD_VIA_WIFI_POSTFIX = "_download_via_wifi";
	private static final String LIVE_UPDATES_ON_POSTFIX = "_live_updates_on";


	public static OsmandSettings.CommonPreference<Boolean> preferenceForLocalIndex(
			LocalIndexInfo item, OsmandSettings settings) {
		final String settingId = item.getFileName() + LIVE_UPDATES_ON_POSTFIX;
		return settings.registerBooleanPreference(settingId, false);
	}

	public static OsmandSettings.CommonPreference<Boolean> preferenceLiveUpdatesOn(
			LocalIndexInfo item, OsmandSettings settings) {
		final String settingId = item.getFileName() + LIVE_UPDATES_ON_POSTFIX;
		return settings.registerBooleanPreference(settingId, false);
	}

	public static OsmandSettings.CommonPreference<Boolean> preferenceDownloadViaWiFi(
			LocalIndexInfo item, OsmandSettings settings) {
		final String settingId = item.getFileName() + DOWNLOAD_VIA_WIFI_POSTFIX;
		return settings.registerBooleanPreference(settingId, false);
	}

	public static OsmandSettings.CommonPreference<Integer> preferenceUpdateFrequency(
			LocalIndexInfo item, OsmandSettings settings) {
		final String settingId = item.getFileName() + UPDATE_TIMES_POSTFIX;
		return settings.registerIntPreference(settingId, UpdateFrequency.HOURLY.ordinal());
	}

	public static OsmandSettings.CommonPreference<Integer> preferenceTimeOfDayToUpdate(
			LocalIndexInfo item, OsmandSettings settings) {
		final String settingId = item.getFileName() + TIME_OF_DAY_TO_UPDATE_POSTFIX;
		return settings.registerIntPreference(settingId, TimesOfDay.NIGHT.ordinal());
	}

	public static String getNameToDisplay(LocalIndexInfo child, OsmandActionBarActivity activity) {
		String mapName = FileNameTranslationHelper.getFileName(activity,
				activity.getMyApplication().getResourceManager().getOsmandRegions(),
				child.getFileName());
		return mapName;
	}

	public static String formatDateTime(Context ctx, long dateTime) {
		java.text.DateFormat dateFormat = android.text.format.DateFormat.getMediumDateFormat(ctx);
		java.text.DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(ctx);
		return dateFormat.format(dateTime) + " " + timeFormat.format(dateTime);
	}

	public static enum TimesOfDay {
		MORNING,
		NIGHT
	}

	public enum UpdateFrequency {
		HOURLY,
		DAILY,
		WEEKLY
	}
}
