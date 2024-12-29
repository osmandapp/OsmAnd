package net.osmand.plus.settings.fragments.search;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.plugins.OsmandPlugin;

class PluginsHelper {

	public static OsmandPlugin enablePlugin(final Class<? extends OsmandPlugin> plugin, final OsmandApplication app) {
		final OsmandPlugin osmandPlugin = getPlugin(plugin);
		enablePlugin(osmandPlugin, app);
		return osmandPlugin;
	}

	private static void enablePlugin(final OsmandPlugin plugin, final OsmandApplication app) {
		net.osmand.plus.plugins.PluginsHelper.enablePlugin(null, app, plugin, true);
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
