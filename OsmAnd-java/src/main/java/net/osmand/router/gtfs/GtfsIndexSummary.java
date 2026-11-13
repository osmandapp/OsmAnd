package net.osmand.router.gtfs;

import java.io.File;
import java.time.LocalDate;
import java.time.ZoneId;

/** What a settings screen shows about the built index: counts, size, build time and the dates it has service for. */
public final class GtfsIndexSummary {

	public enum Validity {
		/** The index defines no service day at all. */
		NO_SERVICE,
		/** Today is before the first service day. */
		NOT_STARTED,
		/** Today is within the dates the feeds define. */
		ACTIVE,
		/** Today is after the last service day: results would be empty or misleading. */
		EXPIRED
	}

	public final int feedCount, stopCount, routeCount, tripCount, connectionCount;
	public final long sizeBytes, builtMillis;
	/** First and last service day; null when the feeds define no service. */
	public final LocalDate validFrom, validTo;
	public final ZoneId zone;

	public GtfsIndexSummary(int feedCount, int stopCount, int routeCount, int tripCount, int connectionCount,
	                        long sizeBytes, long builtMillis, LocalDate validFrom, LocalDate validTo, ZoneId zone) {
		this.feedCount = feedCount;
		this.stopCount = stopCount;
		this.routeCount = routeCount;
		this.tripCount = tripCount;
		this.connectionCount = connectionCount;
		this.sizeBytes = sizeBytes;
		this.builtMillis = builtMillis;
		this.validFrom = validFrom;
		this.validTo = validTo;
		this.zone = zone;
	}

	/** The file is only used for its modification time, which is when the index was built. */
	public static GtfsIndexSummary of(GtfsIndex index, File file) {
		boolean hasService = index.getValidFrom() <= index.getValidTo();
		return new GtfsIndexSummary(index.getFeedCount(), index.getStopCount(), index.getRouteCount(),
				index.getTripCount(), index.getConnectionCount(), index.getFileSize(), file.lastModified(),
				hasService ? LocalDate.ofEpochDay(index.getValidFrom()) : null,
				hasService ? LocalDate.ofEpochDay(index.getValidTo()) : null, index.getZone());
	}

	public Validity validity(LocalDate today) {
		if (validFrom == null || validTo == null) {
			return Validity.NO_SERVICE;
		}
		if (today.isBefore(validFrom)) {
			return Validity.NOT_STARTED;
		}
		return today.isAfter(validTo) ? Validity.EXPIRED : Validity.ACTIVE;
	}

	/** "Today" in the time zone of the feeds, which is the day a timetable query means. */
	public Validity validity(long nowMillis) {
		return validity(java.time.Instant.ofEpochMilli(nowMillis).atZone(zone != null ? zone : ZoneId.of("UTC")).toLocalDate());
	}
}
