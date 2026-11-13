package net.osmand.router;

import net.osmand.data.LatLon;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.LongSupplier;

/**
 * The registered {@link TransportRouteProvider}s, asked in registration order.
 */
public class TransportRouteProviders {

	private final List<TransportRouteProvider> providers = new CopyOnWriteArrayList<>();
	private final LongSupplier epochSecondsNow;

	public TransportRouteProviders() {
		this(() -> System.currentTimeMillis() / 1000);
	}

	public TransportRouteProviders(LongSupplier epochSecondsNow) {
		this.epochSecondsNow = epochSecondsNow;
	}

	public void add(TransportRouteProvider provider) {
		if (provider != null && !providers.contains(provider)) {
			providers.add(provider);
		}
	}

	public void remove(TransportRouteProvider provider) {
		providers.remove(provider);
	}

	public boolean isEmpty() {
		return providers.isEmpty();
	}

	/**
	 * @param departureTimeSec epoch seconds, or null for "now"
	 * @return the first non-empty answer of a provider, or null when none answered (the caller then falls
	 * through to the built-in planner) or the calculation was cancelled
	 */
	public List<TransportRouteResult> calculate(LatLon start, LatLon end, Long departureTimeSec,
	                                            Map<String, String> params, TransportRoutingConfiguration config,
	                                            RouteCalculationProgress progress) throws IOException, InterruptedException {
		if (providers.isEmpty()) {
			return null;
		}
		long departure = departureTimeSec != null ? departureTimeSec : epochSecondsNow.getAsLong();
		TransportRouteRequest request = new TransportRouteRequest(start, end, departure, params, config, progress);
		for (TransportRouteProvider provider : providers) {
			if (progress != null && progress.isCancelled) {
				return null;
			}
			List<TransportRouteResult> res = provider.calculateRoutes(request);
			if (res != null && !res.isEmpty()) {
				return res;
			}
		}
		return null;
	}
}
