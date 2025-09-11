package net.osmand.router.transport;

import net.osmand.data.TransportSchedule;
import net.osmand.data.TransportStop;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TransportRouteResult {

	List<TransportRouteResultSegment> segments  = new ArrayList<TransportRouteResultSegment>(4);
	double finishWalkDist;
	double routeTime;
	private final ITransportRoutingConfiguration cfg;

	public TransportRouteResult(ITransportRoutingContext ctx) {
		cfg = ctx.getCfg();
	}

	public TransportRouteResult(TransportRoutingConfiguration cfg) {
		this.cfg = cfg;
	}

	public List<TransportRouteResultSegment> getSegments() {
		return segments;
	}

	public void setFinishWalkDist(double finishWalkDist) {
		this.finishWalkDist = finishWalkDist;
	}

	public void setRouteTime(double routeTime) {
		this.routeTime = routeTime;
	}

	public void addSegment(TransportRouteResultSegment seg) {
		segments.add(seg);
	}

	public double getWalkDist() {
		double d = finishWalkDist;
		for (TransportRouteResultSegment s : segments) {
			d += s.walkDist;
		}
		return d;
	}

	public double getFinishWalkDist() {
		return finishWalkDist;
	}

	public double getWalkSpeed() {
		return  cfg.getWalkSpeed();
	}

	public double getRouteTime() {
		return routeTime;
	}

	public int getStops() {
		int stops = 0;
		for(TransportRouteResultSegment s : segments) {
			stops += (s.end - s.start);
		}
		return stops;
	}

	public boolean isRouteStop(TransportStop stop) {
		for(TransportRouteResultSegment s : segments) {
			if (s.getTravelStops().contains(stop)) {
				return true;
			}
		}
		return false;
	}

	public TransportRouteResultSegment getRouteStopSegment(TransportStop stop) {
		for(TransportRouteResultSegment s : segments) {
			if (s.getTravelStops().contains(stop)) {
				return s;
			}
		}
		return null;
	}

	public double getTravelDist() {
		double d = 0;
		for (TransportRouteResultSegment s : segments) {
			d += s.getTravelDist();
		}
		return d;
	}

	public double getTravelTime() {
		double t = 0;
		for (TransportRouteResultSegment s : segments) {
			if (cfg.getUseSchedule()) {
				TransportSchedule sts = s.route.getSchedule();
				for (int k = s.start; k < s.end; k++) {
					t += sts.getAvgStopIntervals()[k] * 10;
				}
			} else {
				t += cfg.getBoardingTime();
				t += s.getTravelTime();
			}
		}
		return t;
	}

	public double getWalkTime() {
		return getWalkDist() / cfg.getWalkSpeed();
	}

	public double getChangeTime() {
		return cfg.getChangeTime();
	}

	public double getBoardingTime() {
		return cfg.getBoardingTime();
	}



	public int getChanges() {
		return segments.size() - 1;
	}

	@Override
	public String toString() {
		StringBuilder bld = new StringBuilder();
		bld.append(String.format(Locale.US, "Route %d stops, %d changes, %.2f min: %.2f m (%.1f min) to walk, %.2f m (%.1f min) to travel\n",
				getStops(), getChanges(), routeTime / 60, getWalkDist(), getWalkTime() / 60.0,
				getTravelDist(), getTravelTime() / 60.0));
		for(int i = 0; i < segments.size(); i++) {
			TransportRouteResultSegment s = segments.get(i);
			String time = "";
			String arriveTime = "";
			if(s.depTime != -1) {
				time = String.format("at %s", TransportRoutePlanner.formatTransportTime(s.depTime));
			}
			int aTime = s.getArrivalTime();
			if(aTime != -1) {
				arriveTime = String.format("and arrive at %s", TransportRoutePlanner.formatTransportTime(aTime));
			}
			bld.append(String.format(Locale.US, " %d. %s [%d]: walk %.1f m to '%s' and travel %s to '%s' by %s %d stops %s\n",
					i + 1, s.route.getRef(), s.route.getId() / 2, s.walkDist, s.getStart().getName(),
					 time, s.getEnd().getName(),s.route.getName(),  (s.end - s.start), arriveTime));
		}
		bld.append(String.format(" F. Walk %.1f m to reach your destination", finishWalkDist));
		return bld.toString();
	}
}
