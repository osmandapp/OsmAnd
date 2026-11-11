package net.osmand.router.gtfs.selftest;

import net.osmand.router.TransportRouteProvider;
import net.osmand.router.TransportRouteProviders;
import net.osmand.router.gtfs.GtfsFeedStore;
import net.osmand.router.gtfs.GtfsIndexSource;
import net.osmand.router.gtfs.GtfsTransitRouteProvider;

/**
 * The plugin's provider bookkeeping without Android: the provider is registered only while the plugin is
 * enabled and the store has an index (the rule of GtfsTransitPlugin.refreshProvider). Used by the scenarios
 * that change a scratch store, so the real app state is never touched.
 */
final class ProviderHarness {

	final GtfsFeedStore store;
	final GtfsIndexSource source;
	final TransportRouteProviders providers = new TransportRouteProviders();
	final TransportRouteProvider provider;
	boolean enabled = true;
	boolean registered;

	ProviderHarness(GtfsFeedStore store) {
		this.store = store;
		this.source = new GtfsIndexSource(store.getIndexFile());
		this.provider = new GtfsTransitRouteProvider(source, () -> 800);
	}

	void refresh() {
		boolean wanted = enabled && store.hasIndex();
		if (wanted && !registered) {
			providers.add(provider);
			registered = true;
		} else if (!wanted && registered) {
			providers.remove(provider);
			registered = false;
		}
	}
}
