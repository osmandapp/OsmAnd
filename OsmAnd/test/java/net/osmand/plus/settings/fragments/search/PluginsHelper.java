package net.osmand.plus.settings.fragments.search;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.plugins.OsmandPlugin;

class PluginsHelper {

	private final OsmandApplication osmandApplication;

	public PluginsHelper(final OsmandApplication osmandApplication) {
		this.osmandApplication = osmandApplication;
	}

	public void enablePlugin(final Class<? extends OsmandPlugin> plugin) {
		enablePlugin(getPlugin(plugin));
	}

	private void enablePlugin(final OsmandPlugin plugin) {
		net.osmand.plus.plugins.PluginsHelper.enablePlugin(null, osmandApplication, plugin, true);
	}

	private static <T extends OsmandPlugin> T getPlugin(final Class<T> plugin) {
		return net.osmand.plus.plugins.PluginsHelper
				.getAvailablePlugins()
				.stream()
				.filter(plugin::isInstance)
				.map(plugin::cast)
				.findFirst()
				.orElseThrow();
	}
}
