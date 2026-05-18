package net.osmand.plus.widgets.ctxmenu.callback;

import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;

import java.util.List;

/**
 * An callback to notify when action is deleted or reset
 */
public interface OnItemDeleteAction {

	void itemWasDeleted(ApplicationMode appMode, boolean profileOnly);


	static OnItemDeleteAction makeDeleteAction(OsmandPreference<?>... prefs) {
		return (appMode, profileOnly) -> {
			for (OsmandPreference<?> pref : prefs) {
				resetSetting(appMode, pref, profileOnly);
			}
		};
	}

	static OnItemDeleteAction makeDeleteAction(List<? extends OsmandPreference<?>> prefs) {
		return makeDeleteAction(prefs.toArray(new OsmandPreference[0]));
	}

	static void resetSetting(ApplicationMode appMode, OsmandPreference<?> preference, boolean profileOnly) {
		if (profileOnly) {
			preference.resetModeToDefault(appMode);
		} else {
			for (ApplicationMode mode : ApplicationMode.allPossibleValues()) {
				preference.resetModeToDefault(mode);
			}
		}
	}
}
