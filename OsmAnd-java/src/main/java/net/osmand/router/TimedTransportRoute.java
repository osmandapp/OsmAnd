package net.osmand.router;

import java.time.ZoneId;

/**
 * Optional interface of a {@link TransportRouteResult} or a
 * {@link TransportRoutePlanner.TransportRouteResultSegment} that knows real clock times (for example a
 * timetable based result). The route UI then shows clock times instead of elapsed minutes. Results that do
 * not implement it are shown as before.
 * <p>
 * On a result the times span the whole journey including the walks at both ends. On a segment they are the
 * departure from the boarding stop and the arrival at the exit stop.
 */
public interface TimedTransportRoute {

	/** Epoch seconds. */
	long getDepartureTimeSec();

	/** Epoch seconds. */
	long getArrivalTimeSec();

	/** The zone the clock times are shown in: the time zone of the transport agency, not of the device. */
	ZoneId getTimeZone();
}
