package net.osmand.plus.plugins.gtfs.selftest;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.gtfs.GtfsTransitPlugin;
import net.osmand.plus.routing.TransportRoutingHelper;
import net.osmand.router.gtfs.selftest.ScenarioResult;
import net.osmand.router.gtfs.selftest.SelfTestEnv;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The real GtfsTransitPlugin in the real app: enabling and disabling it programmatically (the same
 * PluginsHelper.enablePlugin call the plugin list uses) and what that does to the route provider list of
 * TransportRoutingHelper. TransportRoutingHelper asks providers only when the list is not empty, so an empty
 * list means a route request goes straight to the stock planner. The enabled flag and the real index are put
 * back at the end, also on failure.
 */
final class PluginStateScenario {

	private final OsmandApplication app;

	PluginStateScenario(OsmandApplication app) {
		this.app = app;
	}

	private void onUi(Runnable r) throws Exception {
		CountDownLatch done = new CountDownLatch(1);
		AtomicReference<Throwable> err = new AtomicReference<>();
		app.runInUIThread(() -> {
			try {
				r.run();
			} catch (Throwable t) {
				err.set(t);
			} finally {
				done.countDown();
			}
		});
		if (!done.await(30, TimeUnit.SECONDS)) {
			throw new IllegalStateException("the UI thread did not run the plugin switch within 30 s");
		}
		if (err.get() != null) {
			throw new IllegalStateException("plugin switch failed: " + err.get(), err.get());
		}
	}

	private void setEnabled(GtfsTransitPlugin plugin, boolean on) throws Exception {
		onUi(() -> PluginsHelper.enablePlugin(null, app, plugin, on));
	}

	void run(SelfTestEnv host, ScenarioResult r) throws Exception {
		GtfsTransitPlugin plugin = PluginsHelper.getPlugin(GtfsTransitPlugin.class);
		r.expect(plugin != null, "GtfsTransitPlugin is not registered in PluginsHelper");
		TransportRoutingHelper helper = app.getTransportRoutingHelper();
		r.expect(helper != null, "TransportRoutingHelper does not exist");
		if (plugin == null || helper == null) {
			return;
		}
		boolean wasEnabled = plugin.isEnabled();
		File index = plugin.getStore().getIndexFile();
		File aside = new File(index.getPath() + ".selftest-aside");
		boolean hadIndex = plugin.getStore().hasIndex();
		r.put("enabledAtStart", wasEnabled);
		r.put("realIndexPresent", hadIndex);
		r.put("providersAtStart", helper.hasRouteProviders());
		try {
			setEnabled(plugin, false);
			r.put("afterDisable_enabled", plugin.isEnabled());
			r.put("afterDisable_providerRegistered", helper.hasRouteProviders());
			r.expect(!plugin.isEnabled(), "plugin still enabled after disable");
			r.expect(!helper.hasRouteProviders(), "a provider is registered while the plugin is disabled");

			setEnabled(plugin, true);
			r.put("afterEnable_enabled", plugin.isEnabled());
			r.put("afterEnable_providerRegistered", helper.hasRouteProviders());
			r.expect(plugin.isEnabled(), "plugin not enabled after enable");
			r.expect(helper.hasRouteProviders() == hadIndex, "enabled with " + (hadIndex ? "an" : "no") + " index but provider registered="
					+ helper.hasRouteProviders());

			if (hadIndex) {
				// enabled but no index file: the provider must go; then back
				r.expect(index.renameTo(aside), "cannot move the index aside for the test");
				onUi(plugin::refreshProvider);
				r.put("enabledNoIndex_providerRegistered", helper.hasRouteProviders());
				r.expect(!helper.hasRouteProviders(), "provider still registered with the index file missing");
				r.expect(aside.renameTo(index), "cannot move the index back");
				onUi(plugin::refreshProvider);
				r.put("indexBack_providerRegistered", helper.hasRouteProviders());
				r.expect(helper.hasRouteProviders(), "provider not registered again after the index came back");
				r.expect(plugin.summary() != null, "the restored index cannot be opened");
			} else {
				r.note("the app has no imported GTFS index, so enabled+index could not be shown on the real plugin; "
						+ "the import scenarios cover the same rule on scratch stores");
			}
			setEnabled(plugin, false);
			r.expect(!helper.hasRouteProviders(), "provider registered after the second disable");
			r.note("route request while disabled: the provider list is empty, and TransportRoutingHelper.calculateRouteImpl "
					+ "only consults providers when the list is not empty, so the stock planner runs");
		} finally {
			if (aside.exists() && !index.exists()) {
				aside.renameTo(index);
			}
			if (plugin.isEnabled() != wasEnabled) {
				setEnabled(plugin, wasEnabled);
			}
			onUi(plugin::refreshProvider);
			r.put("restoredEnabled", plugin.isEnabled());
			r.expect(plugin.isEnabled() == wasEnabled, "could not restore the enabled state");
			r.expect(!hadIndex || index.isFile(), "the real index file is missing after the test");
		}
	}
}
