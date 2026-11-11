package net.osmand.router.gtfs;

import java.util.List;
import java.util.function.IntSupplier;

import net.osmand.PlatformUtil;
import net.osmand.router.TransportRouteProvider;
import net.osmand.router.TransportRouteRequest;
import net.osmand.router.TransportRouteResult;

import org.apache.commons.logging.Log;

/**
 * Answers public transport requests from the imported GTFS timetable. It returns null, which lets the
 * OBF planner run, when there is no usable index, when the start or end is further from every stop than the
 * walking limit, or when nothing is found.
 */
public final class GtfsTransitRouteProvider implements TransportRouteProvider {

	private static final Log LOG = PlatformUtil.getLog(GtfsTransitRouteProvider.class);

	private final GtfsIndexSource source;
	private final IntSupplier maxWalkMeters;

	public GtfsTransitRouteProvider(GtfsIndexSource source, IntSupplier maxWalkMeters) {
		this.source = source;
		this.maxWalkMeters = maxWalkMeters;
	}

	@Override
	public List<TransportRouteResult> calculateRoutes(TransportRouteRequest request) {
		GtfsIndexSource.Loaded loaded = source.get();
		if (loaded == null) {
			return null;
		}
		int walk = maxWalkMeters.getAsInt();
		if (!loaded.coverage.contains(request.getStart(), walk) || !loaded.coverage.contains(request.getEnd(), walk)) {
			return null;
		}
		GtfsPlanOptions options = new GtfsPlanOptions();
		options.maxWalkMeters = walk;
		GtfsProgressListener cancel = new GtfsProgressListener() {
			@Override
			public void onProgress(String stage, int percent) {
			}

			@Override
			public boolean isCancelled() {
				return request.getProgress() != null && request.getProgress().isCancelled;
			}
		};
		try {
			List<GtfsItinerary> itineraries;
			synchronized (loaded) {
				itineraries = loaded.planner.plan(request.getStart(), request.getEnd(), request.getDepartureTimeSec(),
						options, cancel);
			}
			if (itineraries.isEmpty() || cancel.isCancelled()) {
				return null;
			}
			List<TransportRouteResult> res = new GtfsTransportResultAdapter(loaded.index, request.getConfig())
					.convert(itineraries);
			return res.isEmpty() ? null : res;
		} catch (RuntimeException e) {
			// a bad index or a planner bug must not take public transport routing down: use the OBF planner
			LOG.error("GTFS planning failed, falling back to the map based planner", e);
			return null;
		}
	}
}
