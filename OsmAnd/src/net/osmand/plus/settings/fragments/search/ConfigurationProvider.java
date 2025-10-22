package net.osmand.plus.settings.fragments.search;

import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.PluginsHelper;

import java.util.Set;
import java.util.stream.Collectors;

public class ConfigurationProvider {

	public static Configuration getActualConfiguration() {
		return new Configuration(getEnabledPlugins());
	}

	public static Set<String> getEnabledPlugins() {
		return PluginsHelper
				.getEnabledPlugins()
				.stream()
				.map(OsmandPlugin::getId)
				.collect(Collectors.toSet());
	}
}
