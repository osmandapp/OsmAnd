package net.osmand.plus.settings.fragments.search;

import android.os.PersistableBundle;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ConfigurationBundleConverter implements de.KnollFrank.lib.settingssearch.db.preference.pojo.converters.ConfigurationBundleConverter<Configuration> {

	private static final String ENABLED_PLUGINS = "enabledPlugins";

	@Override
	public PersistableBundle convertForward(final Configuration configuration) {
		final PersistableBundle bundle = new PersistableBundle();
		putStringSet(bundle, ENABLED_PLUGINS, configuration.enabledPlugins());
		return bundle;
	}

	@Override
	public Configuration convertBackward(final PersistableBundle bundle) {
		return new Configuration(getStringSet(bundle, ENABLED_PLUGINS));
	}

	private static void putStringSet(final PersistableBundle bundle, final String key, final Set<String> strings) {
		bundle.putStringArray(key, strings.toArray(String[]::new));
	}

	private static Set<String> getStringSet(final PersistableBundle bundle, final String key) {
		return new HashSet<>(Arrays.asList(bundle.getStringArray(key)));
	}
}
