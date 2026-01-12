package net.osmand.plus.settings.fragments.search;

import androidx.preference.Preference;

import net.osmand.plus.plugins.OsmandPlugin;

import de.KnollFrank.lib.settingssearch.db.preference.pojo.SearchablePreferenceOfHostWithinTree;

public class PreferenceMarker {

	private static final String KEY = "settings.search.connection2plugin";

	public static void markPreferenceAsConnectedToPlugin(final Preference preference, final Class<? extends OsmandPlugin> plugin) {
		preference.getExtras().putString(KEY, plugin.getName());
	}

	public static boolean isPreferenceConnectedToPlugin(final SearchablePreferenceOfHostWithinTree preference, final Class<? extends OsmandPlugin> plugin) {
		return plugin.getName().equals(preference.searchablePreference().getExtras().getString(KEY));
	}
}
