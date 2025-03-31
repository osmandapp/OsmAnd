package net.osmand.plus.settings.fragments.search;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.plugins.OsmandPlugin;

// FK-TODO: refactor
class PluginsHelper {

	public static OsmandPlugin enablePlugin(final Class<? extends OsmandPlugin> plugin, final OsmandApplication app) {
		final OsmandPlugin osmandPlugin = getPlugin(plugin);
		enablePlugin(osmandPlugin, app);
		return osmandPlugin;
	}

	public static OsmandPlugin disablePlugin(final Class<? extends OsmandPlugin> plugin, final OsmandApplication app) {
		final OsmandPlugin osmandPlugin = getPlugin(plugin);
		disablePlugin(osmandPlugin, app);
		return osmandPlugin;
	}

	private static void enablePlugin(final OsmandPlugin plugin, final OsmandApplication app) {
		setPlugin(app, plugin, true);
	}

	private static void disablePlugin(final OsmandPlugin plugin, final OsmandApplication app) {
		setPlugin(app, plugin, false);
	}

	private static void setPlugin(final OsmandApplication app, final OsmandPlugin plugin, final boolean enable) {
		net.osmand.plus.plugins.PluginsHelper.enablePlugin(null, app, plugin, enable);
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
