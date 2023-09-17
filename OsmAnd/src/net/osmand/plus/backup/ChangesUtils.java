package net.osmand.plus.backup;

import static net.osmand.plus.utils.OsmAndFormatter.formatChangesPassedTime;

import android.content.Context;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.ExportSettingsType;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem.FileSubtype;
import net.osmand.plus.settings.backend.backup.items.ProfileSettingsItem;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;
import net.osmand.util.Algorithms;

public class ChangesUtils {

	private static final String LONG_DATE_PATTERN = "MMM d, HH:mm";
	private static final String SHORT_DATE_PATTERN = "HH:mm";

	public static String generateTimeString(OsmandApplication app, long time, String summary) {
		return app.getString(R.string.ltr_or_rtl_combine_via_colon, summary, getTimeString(app, time));
	}

	public static String getTimeString(OsmandApplication app, long time) {
		String never = app.getString(R.string.shared_string_never);
		if (time != -1) {
			return formatChangesPassedTime(app, time, never);
		} else {
			return app.getString(R.string.shared_string_never);
		}
	}

	public static String generateDeletedTimeString(@NonNull OsmandApplication app, long time) {
		String deleted = app.getString(R.string.shared_string_deleted);
		String formattedDate = formatChangesPassedTime(app, time, "", LONG_DATE_PATTERN, SHORT_DATE_PATTERN);
		return app.getString(R.string.ltr_or_rtl_combine_via_colon, deleted, formattedDate);
	}

	public static String getName(@NonNull Context context, SettingsItem settingsItem) {
		String name = "";
		if (settingsItem instanceof ProfileSettingsItem) {
			name = ((ProfileSettingsItem) settingsItem).getAppMode().toHumanString();
		} else {
			name = settingsItem.getPublicName(context);
			if (settingsItem instanceof FileSettingsItem) {
				FileSettingsItem fileItem = (FileSettingsItem) settingsItem;
				if (fileItem.getSubtype() == FileSubtype.TTS_VOICE) {
					String suffix = context.getString(R.string.tts_title);
					name = context.getString(R.string.ltr_or_rtl_combine_via_space, name, suffix);
				} else if (fileItem.getSubtype() == FileSubtype.VOICE) {
					String suffix = context.getString(R.string.shared_string_record);
					name = context.getString(R.string.ltr_or_rtl_combine_via_space, name, suffix);
				}
			} else if (Algorithms.isEmpty(name)) {
				name = context.getString(R.string.res_unknown);
			}
		}
		return name;
	}

	public static int getIconId(@NonNull SettingsItem item) {
		if (item instanceof ProfileSettingsItem) {
			ProfileSettingsItem profileItem = (ProfileSettingsItem) item;
			ApplicationMode mode = profileItem.getAppMode();
			return mode.getIconRes();
		} else {
			ExportSettingsType type = ExportSettingsType.getExportSettingsTypeForItem(item);
			if (type != null) {
				return type.getIconRes();
			}
		}
		return -1;
	}
}
