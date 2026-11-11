package net.osmand.router.gtfs.guidance;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import net.osmand.data.LatLon;
import net.osmand.router.gtfs.GuidanceTestItineraries;

/** Drives an engine and keeps every event it emitted. */
final class Sim {

	final GtfsGuidanceEngine engine;
	final List<GuidanceEvent> log = new ArrayList<>();
	List<GuidanceEvent> last = new ArrayList<>();
	final EnglishGuidanceFormatter fmt = new EnglishGuidanceFormatter(ZoneId.of("America/New_York"));

	Sim(GtfsGuidanceEngine engine) {
		this.engine = engine;
	}

	private Sim take(List<GuidanceEvent> events) {
		last = events;
		log.addAll(events);
		return this;
	}

	Sim fix(LatLon p, long t) {
		return take(engine.onLocation(p.getLatitude(), p.getLongitude(), 5f, t, true));
	}

	Sim fix(LatLon p, float accuracy, long t) {
		return take(engine.onLocation(p.getLatitude(), p.getLongitude(), accuracy, t, true));
	}

	Sim noFix(long t) {
		return take(engine.onLocation(0, 0, 0f, t, false));
	}

	Sim tick(long t) {
		return take(engine.onTick(t));
	}

	/** Ticks from..to inclusive every step seconds. */
	Sim ticks(long from, long to, long step) {
		for (long t = from; t <= to; t += step) {
			tick(t);
		}
		return this;
	}

	Sim act(UserAction a, long t) {
		return take(engine.onUserAction(a, t));
	}

	GuidanceState state() {
		return engine.getState();
	}

	long count(GuidanceEventType type) {
		return log.stream().filter(e -> e.getType() == type).count();
	}

	List<GuidanceEvent> of(GuidanceEventType type) {
		List<GuidanceEvent> out = new ArrayList<>();
		for (GuidanceEvent e : log) {
			if (e.getType() == type) {
				out.add(e);
			}
		}
		return out;
	}

	/** States entered, in order. */
	List<GuidanceState> states() {
		List<GuidanceState> out = new ArrayList<>();
		for (GuidanceEvent e : log) {
			if (e.getType() == GuidanceEventType.STATE_CHANGED) {
				out.add(e.getState());
			}
		}
		return out;
	}

	/** Spoken/shown sentences, in order. */
	List<String> texts() {
		List<String> out = new ArrayList<>();
		for (GuidanceEvent e : log) {
			String s = fmt.format(e);
			if (s != null) {
				out.add(s);
			}
		}
		return out;
	}

	static long t(long offset) {
		return GuidanceTestItineraries.T0 + offset;
	}
}
