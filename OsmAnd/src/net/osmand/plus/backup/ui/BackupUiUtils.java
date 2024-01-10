package net.osmand.plus.backup.ui;


import static net.osmand.plus.settings.backend.backup.items.FileSettingsItem.FileSubtype.TTS_VOICE;
import static net.osmand.plus.settings.backend.backup.items.FileSettingsItem.FileSubtype.VOICE;
import static net.osmand.plus.utils.OsmAndFormatter.getFormattedDate;
import static net.osmand.plus.utils.OsmAndFormatter.getFormattedDateTime;
import static net.osmand.plus.utils.OsmAndFormatter.getFormattedDuration;

import android.content.Context;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.backup.exporttype.ExportType;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem.FileSubtype;
import net.osmand.plus.settings.backend.backup.items.ProfileSettingsItem;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;
import net.osmand.util.Algorithms;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class BackupUiUtils {

	private static final int MIN_DURATION_FOR_DATE_FORMAT = 48 * 60 * 60;
	private static final int MIN_DURATION_FOR_YESTERDAY_DATE_FORMAT = 24 * 60 * 60;

	@NonNull
	public static String getItemName(@NonNull Context context, @NonNull SettingsItem item) {
		String name;
		if (item instanceof ProfileSettingsItem) {
			name = ((ProfileSettingsItem) item).getAppMode().toHumanString();
		} else {
			name = item.getPublicName(context);
			if (item instanceof FileSettingsItem) {
				FileSubtype subtype = ((FileSettingsItem) item).getSubtype();
				if (TTS_VOICE == subtype) {
					String suffix = context.getString(R.string.tts_title);
					name = context.getString(R.string.ltr_or_rtl_combine_via_space, name, suffix);
				} else if (VOICE == subtype) {
					String suffix = context.getString(R.string.shared_string_record);
					name = context.getString(R.string.ltr_or_rtl_combine_via_space, name, suffix);
				}
			}
		}
		return !Algorithms.isEmpty(name) ? name : context.getString(R.string.res_unknown);
	}

	@DrawableRes
	public static int getIconId(@NonNull SettingsItem item) {
		if (item instanceof ProfileSettingsItem) {
			return ((ProfileSettingsItem) item).getAppMode().getIconRes();
		}
		ExportType exportType = ExportType.findBy(item);
		return exportType != null ? exportType.getIconId() : -1;
	}

	@NonNull
	public static String getLastBackupTimeDescription(@NonNull OsmandApplication app, @NonNull String def) {
		long lastUploadedTime = app.getSettings().BACKUP_LAST_UPLOADED_TIME.get();
		return getFormattedPassedTime(app, lastUploadedTime, def, false);
	}

	@NonNull
	public static String getFormattedPassedTime(@NonNull OsmandApplication app, long time, @NonNull String def, boolean showTime) {
		if (time > 0) {
			long duration = (System.currentTimeMillis() - time) / 1000;
			if (duration > MIN_DURATION_FOR_DATE_FORMAT) {
				return showTime ? getFormattedDateTime(app, time) : getFormattedDate(app, time);
			} else {
				String formattedDuration = getFormattedDuration((int) duration, app);
				if (Algorithms.isEmpty(formattedDuration)) {
					return app.getString(R.string.duration_moment_ago);
				} else {
					return app.getString(R.string.duration_ago, formattedDuration);
				}
			}
		}
		return def;
	}

	@NonNull
	public static String generateTimeString(@NonNull OsmandApplication app, @NonNull String summary, long time) {
		return app.getString(R.string.ltr_or_rtl_combine_via_colon, summary, getTimeString(app, time));
	}

	@NonNull
	public static String getTimeString(@NonNull OsmandApplication app, long time) {
		String never = app.getString(R.string.shared_string_never);
		return formatPassedTime(app, time, "d MMM yyyy, HH:mm:ss", "HH:mm:ss", never);
	}

	@NonNull
	public static String formatPassedTime(@NonNull OsmandApplication app, long time,
	                                      @NonNull String longPattern, @NonNull String shortPattern,
	                                      @NonNull String def) {
		if (time > 0) {
			long duration = (System.currentTimeMillis() - time) / 1000;
			if (duration > MIN_DURATION_FOR_DATE_FORMAT) {
				DateFormat dateFormat = new SimpleDateFormat(longPattern, Locale.getDefault());
				Calendar calendar = Calendar.getInstance();
				calendar.setTimeInMillis(time);
				return dateFormat.format(calendar.getTime());
			} else if (duration > MIN_DURATION_FOR_YESTERDAY_DATE_FORMAT) {
				DateFormat dateFormat = new SimpleDateFormat(shortPattern, Locale.getDefault());
				Calendar calendar = Calendar.getInstance();
				calendar.setTimeInMillis(time);
				return app.getString(R.string.yesterday) + ", " + dateFormat.format(calendar.getTime());
			} else {
				String formattedDuration = getFormattedDuration((int) duration, app);
				if (Algorithms.isEmpty(formattedDuration)) {
					return app.getString(R.string.duration_moment_ago);
				} else {
					return app.getString(R.string.duration_ago, formattedDuration);
				}
			}
		}
		return def;
	}
}
