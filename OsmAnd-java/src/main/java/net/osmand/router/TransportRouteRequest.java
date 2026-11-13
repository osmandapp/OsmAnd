package net.osmand.router;

import net.osmand.data.LatLon;

import java.util.Collections;
import java.util.Map;

/**
 * What a {@link TransportRouteProvider} is asked to calculate.
 */
public class TransportRouteRequest {

	private final LatLon start;
	private final LatLon end;
	private final long departureTimeSec;
	private final Map<String, String> params;
	private final TransportRoutingConfiguration config;
	private final RouteCalculationProgress progress;

	public TransportRouteRequest(LatLon start, LatLon end, long departureTimeSec, Map<String, String> params,
	                             TransportRoutingConfiguration config, RouteCalculationProgress progress) {
		this.start = start;
		this.end = end;
		this.departureTimeSec = departureTimeSec;
		this.params = Collections.unmodifiableMap(params);
		this.config = config;
		this.progress = progress;
	}

	public LatLon getStart() {
		return start;
	}

	public LatLon getEnd() {
		return end;
	}

	/** The earliest time the traveller leaves the start, in epoch seconds. Never "unset": "now" is already resolved. */
	public long getDepartureTimeSec() {
		return departureTimeSec;
	}

	/** The routing parameters of the profile as they are passed to {@link TransportRoutingConfiguration}. */
	public Map<String, String> getParams() {
		return params;
	}

	/** The configuration built from the profile (walking speed and so on). */
	public TransportRoutingConfiguration getConfig() {
		return config;
	}

	public RouteCalculationProgress getProgress() {
		return progress;
	}
}
