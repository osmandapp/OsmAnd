package net.osmand.router.lanes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.osmand.router.lanes.TurnTypeAI.TransportMode;
import net.osmand.router.lanes.TurnTypeAI.TurnIndication;


/**
 * The lane table a driver looks at, in the driver's own direction of travel.
 *
 * <p>This is the whole interface an UI needs, and it is deliberately not the model. {@link TurnTypeAI}
 * knows OSM: an access hierarchy, tag keys, direction groups, designations. None of that is a
 * question a driver asks. A driver asks four:
 *
 * <ol>
 * <li>how many lanes are there and in what order - the list, index 0 leftmost AS SEEN BY THE DRIVER;</li>
 * <li>which of them leads where I am going - {@link Lane#active};</li>
 * <li>may I be in this lane - {@link Lane#open};</li>
 * <li>whose lane is it - {@link Lane#purpose}.</li>
 * </ol>
 *
 * <p>Two more answers come free with the model and cost nothing to carry: the arrows painted on the
 * lane, and whether the line beside it may be crossed.
 *
 * <p>Everything here is already resolved for the traveller the route was built for, so nothing in
 * an interface has to know what {@code psv} means or that {@code vehicle} outranks {@code access}.
 */
public final class LanePanelAI {

	/** whose lane it is, in the words a person would use */
	public enum LanePurpose {
		/** an ordinary lane */
		GENERAL,
		/** a bus lane, a tram lane, anything reserved for public service vehicles */
		PUBLIC_TRANSPORT,
		BICYCLE,
		TAXI,
		HEAVY_GOODS,
		EMERGENCY
	}

	/** one row of the table */
	public static final class Lane {

		private final int index;
		private final List<TurnIndication> arrows;
		private final boolean active;
		private final TurnIndication taken;
		private final boolean open;
		private final LanePurpose purpose;
		private final boolean sharedWithOncoming;
		private final boolean mayEnterFromLeft;
		private final boolean mayEnterFromRight;

		Lane(int index, List<TurnIndication> arrows, boolean active, TurnIndication taken, boolean open,
		     LanePurpose purpose, boolean sharedWithOncoming, boolean mayEnterFromLeft,
		     boolean mayEnterFromRight) {
			this.index = index;
			this.arrows = Collections.unmodifiableList(new ArrayList<>(arrows));
			this.active = active;
			this.taken = taken;
			this.open = open;
			this.purpose = purpose;
			this.sharedWithOncoming = sharedWithOncoming;
			this.mayEnterFromLeft = mayEnterFromLeft;
			this.mayEnterFromRight = mayEnterFromRight;
		}

		/** position from the left, as the driver sees it */
		public int index() {
			return index;
		}

		/** what is painted on the road, in the order it was mapped; may be empty */
		public List<TurnIndication> arrows() {
			return arrows;
		}

		/** this lane leads where the route goes */
		public boolean isActive() {
			return active;
		}

		/** the arrow of this lane the route follows, null when the lane carries no arrows */
		public TurnIndication taken() {
			return taken;
		}

		/** this driver may be in this lane */
		public boolean isOpen() {
			return open;
		}

		public LanePurpose purpose() {
			return purpose;
		}

		/** a central lane shared with oncoming traffic, a left-turn pocket or a suicide lane */
		public boolean isSharedWithOncoming() {
			return sharedWithOncoming;
		}

		/** the line on this side may be crossed, so the lane can be entered from that neighbour */
		public boolean mayEnterFromLeft() {
			return mayEnterFromLeft;
		}

		public boolean mayEnterFromRight() {
			return mayEnterFromRight;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(active ? "+" : open ? "-" : "x");
			for (int i = 0; i < arrows.size(); i++) {
				sb.append(i > 0 ? ";" : "").append(arrows.get(i) == taken ? "*" : "")
						.append(arrows.get(i).osmValue());
			}
			if (purpose != LanePurpose.GENERAL) {
				sb.append('[').append(purpose.name().toLowerCase()).append(']');
			}
			if (sharedWithOncoming) {
				sb.append("[oncoming]");
			}
			return sb.toString();
		}
	}

	private final List<Lane> lanes;

	private LanePanelAI(List<Lane> lanes) {
		this.lanes = Collections.unmodifiableList(lanes);
	}

	public static LanePanelAI of(TurnTypeAI turn) {
		if (turn == null || turn.lanes().isEmpty()) {
			return new LanePanelAI(Collections.<Lane>emptyList());
		}
		TransportMode traveller = turn.traveller();
		List<TurnTypeAI.Lane> source = turn.lanes();
		List<Lane> rows = new ArrayList<>(source.size());
		for (int i = 0; i < source.size(); i++) {
			TurnTypeAI.Lane lane = source.get(i);
			boolean fromLeft = i > 0 && source.get(i - 1).mayChangeRight() && lane.mayChangeLeft();
			boolean fromRight = i + 1 < source.size() && source.get(i + 1).mayChangeLeft() && lane.mayChangeRight();
			rows.add(new Lane(i, lane.turns(), lane.isActive(), lane.taken(), lane.isUsableBy(traveller),
					purposeOf(lane), lane.group() == TurnTypeAI.LaneGroup.BOTH_WAYS,
					i == 0 || fromLeft, i == source.size() - 1 || fromRight));
		}
		return new LanePanelAI(rows);
	}

	private static LanePurpose purposeOf(TurnTypeAI.Lane lane) {
		TransportMode owner = lane.designatedFor();
		if (owner == null) {
			return LanePurpose.GENERAL;
		}
		switch (owner) {
			case BUS:
			case PSV:
				return LanePurpose.PUBLIC_TRANSPORT;
			case TAXI:
				return LanePurpose.TAXI;
			case BICYCLE:
				return LanePurpose.BICYCLE;
			case HGV:
				return LanePurpose.HEAVY_GOODS;
			case EMERGENCY:
				return LanePurpose.EMERGENCY;
			default:
				return LanePurpose.GENERAL;
		}
	}

	public List<Lane> lanes() {
		return lanes;
	}

	public int count() {
		return lanes.size();
	}

	public boolean isEmpty() {
		return lanes.isEmpty();
	}

	public Lane get(int index) {
		return lanes.get(index);
	}

	/** the lanes to be in, as a range, because they are always neighbours; -1 when there are none */
	public int activeFrom() {
		for (Lane lane : lanes) {
			if (lane.isActive()) {
				return lane.index();
			}
		}
		return -1;
	}

	public int activeTo() {
		for (int i = lanes.size() - 1; i >= 0; i--) {
			if (lanes.get(i).isActive()) {
				return i;
			}
		}
		return -1;
	}

	/** lanes this driver has no business in: a bus lane for a car, a cycle lane for a bus */
	public int closedCount() {
		int count = 0;
		for (Lane lane : lanes) {
			if (!lane.isOpen()) {
				count++;
			}
		}
		return count;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < lanes.size(); i++) {
			sb.append(i > 0 ? " | " : "").append(lanes.get(i));
		}
		return sb.toString();
	}
}
