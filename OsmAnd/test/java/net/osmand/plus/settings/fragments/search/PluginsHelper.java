package net.osmand.plus.settings.fragments.search;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.plugins.OsmandPlugin;

class PluginsHelper {

	public static OsmandPlugin enablePlugin(final Class<? extends OsmandPlugin> plugin,
											final OsmandApplication app) {
		return setPlugin(plugin, app, true);
	}

	public static OsmandPlugin disablePlugin(final Class<? extends OsmandPlugin> plugin,
											 final OsmandApplication app) {
		return setPlugin(plugin, app, false);
	}

	private static OsmandPlugin setPlugin(final Class<? extends OsmandPlugin> plugin,
										  final OsmandApplication app,
										  final boolean enable) {
		final OsmandPlugin osmandPlugin = getPlugin(plugin);
		setPlugin(app, osmandPlugin, enable);
		return osmandPlugin;
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

	private static void setPlugin(final OsmandApplication app, final OsmandPlugin plugin, final boolean enable) {
		net.osmand.plus.plugins.PluginsHelper.enablePlugin(null, app, plugin, enable);
	}
}
