package net.osmand.plus.settings.fragments.search;

import com.google.common.collect.MoreCollectors;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.plugins.OsmandPlugin;

class PluginsHelper {

	public static OsmandPlugin enablePlugin(final Class<? extends OsmandPlugin> plugin,
											final OsmandApplication app) {
		return getPluginAndSetState(plugin, State.ENABLED, app);
	}

	public static OsmandPlugin disablePlugin(final Class<? extends OsmandPlugin> plugin,
											 final OsmandApplication app) {
		return getPluginAndSetState(plugin, State.DISABLED, app);
	}

	private enum State {
		ENABLED, DISABLED
	}

	private static OsmandPlugin getPluginAndSetState(final Class<? extends OsmandPlugin> plugin,
													 final State state,
													 final OsmandApplication app) {
		final OsmandPlugin osmandPlugin = getPlugin(plugin);
		setPluginState(osmandPlugin, state, app);
		return osmandPlugin;
	}

	private static <T extends OsmandPlugin> T getPlugin(final Class<T> plugin) {
		return net.osmand.plus.plugins.PluginsHelper
				.getAvailablePlugins()
				.stream()
				.filter(plugin::isInstance)
				.map(plugin::cast)
				.collect(MoreCollectors.onlyElement());
	}

	private static void setPluginState(final OsmandPlugin plugin,
									   final State state,
									   final OsmandApplication app) {
		net.osmand.plus.plugins.PluginsHelper.enablePlugin(
				null,
				app,
				plugin,
				state == State.ENABLED);
	}
}
