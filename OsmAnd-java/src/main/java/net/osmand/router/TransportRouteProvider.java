package net.osmand.router;

import java.io.IOException;
import java.util.List;

/**
 * Extension point for public transport planning. A provider is asked before the built-in OBF based planner
 * (native or Java) and may answer from another data source, for example an imported timetable.
 * <p>
 * Contract:
 * <ul>
 * <li>Return a non-empty list to use it as the result. Return {@code null} or an empty list to let the next
 * provider, and finally the built-in planner, calculate the route.</li>
 * <li>Poll {@link TransportRouteRequest#getProgress()} {@code .isCancelled} during long work and return
 * {@code null} when it is set. Progress can be reported through the other fields of the same object.</li>
 * <li>An exception is not swallowed: it ends the calculation and is shown as a routing error.</li>
 * <li>Called on the route calculation thread, never on the UI thread.</li>
 * </ul>
 */
public interface TransportRouteProvider {

	List<TransportRouteResult> calculateRoutes(TransportRouteRequest request) throws IOException, InterruptedException;
}
