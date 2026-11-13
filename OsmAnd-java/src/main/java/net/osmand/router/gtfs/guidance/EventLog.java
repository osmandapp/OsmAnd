package net.osmand.router.gtfs.guidance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.osmand.data.LatLon;

/** Numbers events, drops repeats of the same prompt, and collects the events of one engine call. */
final class EventLog {

	private long seq;
	private final Set<String> fired = new HashSet<>();
	private List<GuidanceEvent> buffer = new ArrayList<>();

	/** Fluent description of one event; {@link #emit()} adds it unless an identical prompt already fired. */
	final class Builder {
		private final GuidanceEventType type;
		private final long now;
		private final GuidanceState state;
		private GuidanceState previous;
		private int leg = -1, stopIdx = -1, position = -1, remaining = -1, episode;
		private long time = -1;
		private LatLon from, to;
		private List<Object> args = Collections.emptyList();

		private Builder(GuidanceEventType type, long now, GuidanceState state) {
			this.type = type;
			this.now = now;
			this.state = state;
		}

		Builder leg(int leg) {
			this.leg = leg;
			return this;
		}

		Builder previous(GuidanceState previous) {
			this.previous = previous;
			return this;
		}

		Builder stop(int stopIdx, int position) {
			this.stopIdx = stopIdx;
			this.position = position;
			return this;
		}

		Builder remaining(int remaining) {
			this.remaining = remaining;
			return this;
		}

		Builder time(long time) {
			this.time = time;
			return this;
		}

		Builder points(LatLon from, LatLon to) {
			this.from = from;
			this.to = to;
			return this;
		}

		/** Distinguishes repeatable prompts (a second off-route episode is a new prompt). */
		Builder episode(int episode) {
			this.episode = episode;
			return this;
		}

		Builder args(Object... args) {
			this.args = Arrays.asList(args);
			return this;
		}

		void emit() {
			if (type != GuidanceEventType.STATE_CHANGED) {
				String key = type + ":" + leg + ":" + position + ":" + episode;
				if (!fired.add(key)) {
					return;
				}
			}
			buffer.add(new GuidanceEvent(++seq, type, now, state, previous, leg, stopIdx, position, remaining, time,
					from, to, args));
		}
	}

	Builder event(GuidanceEventType type, long now, GuidanceState state) {
		return new Builder(type, now, state);
	}

	/** Returns the events collected since the last call and starts a new batch. */
	List<GuidanceEvent> take() {
		List<GuidanceEvent> out = Collections.unmodifiableList(buffer);
		buffer = new ArrayList<>();
		return out;
	}
}
