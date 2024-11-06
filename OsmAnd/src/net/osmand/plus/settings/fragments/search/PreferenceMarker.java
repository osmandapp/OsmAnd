package net.osmand.plus.settings.fragments.search;

import android.os.Bundle;

import androidx.preference.Preference;

import net.osmand.plus.plugins.OsmandPlugin;

import java.util.Optional;

import de.KnollFrank.lib.settingssearch.common.Utils;
import de.KnollFrank.lib.settingssearch.db.preference.pojo.SearchablePreferencePOJO;

public class PreferenceMarker {

	private static final String KEY = "settings.search.connection2plugin";

	public static void markPreferenceAsConnectedToPlugin(final Preference preference, final Class<? extends OsmandPlugin> plugin) {
		putClass(preference.getExtras(), plugin);
	}

	public static boolean isPreferenceConnectedToPlugin(final SearchablePreferencePOJO preference, final Class<? extends OsmandPlugin> plugin) {
		return getClass(preference.extras()).equals(Optional.of(plugin));
	}

	private static void putClass(final Bundle bundle, final Class<?> value) {
		bundle.putString(KEY, value.getName());
	}

	private static Optional<Class<?>> getClass(final Bundle bundle) {
		return bundle.containsKey(KEY) ?
				Optional.of(Utils.getClass(bundle.getString(KEY))) :
				Optional.empty();
	}
}
