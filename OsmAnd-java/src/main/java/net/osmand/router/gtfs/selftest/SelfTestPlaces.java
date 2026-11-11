package net.osmand.router.gtfs.selftest;

import java.util.ArrayList;
import java.util.List;

import net.osmand.data.LatLon;

/** Fixed origin/destination pairs used by the plan, guidance and perf scenarios (NYC, MTA feeds). */
final class SelfTestPlaces {

	/** A fixed trip. {@code expectRoute} false means the provider must answer null (outside coverage). */
	static final class Od {
		final String name;
		final LatLon from, to;
		final boolean expectRoute;

		Od(String name, double lat1, double lon1, double lat2, double lon2, boolean expectRoute) {
			this.name = name;
			this.from = new LatLon(lat1, lon1);
			this.to = new LatLon(lat2, lon2);
			this.expectRoute = expectRoute;
		}
	}

	private SelfTestPlaces() {
	}

	static List<Od> all() {
		List<Od> l = new ArrayList<>();
		l.add(new Od("Newkirk Plaza -> Times Sq", 40.6350, -73.9625, 40.7553, -73.9869, true));
		l.add(new Od("Flushing Main St -> Jamaica Sutphin", 40.7596, -73.8300, 40.7005, -73.8079, true));
		l.add(new Od("St George -> Staten Island Mall", 40.6437, -74.0737, 40.5826, -74.1651, true));
		l.add(new Od("Fordham Plaza -> Bryant Park", 40.8614, -73.8902, 40.7536, -73.9832, true));
		l.add(new Od("Coney Island -> Union Sq", 40.5775, -73.9814, 40.7347, -73.9906, true));
		l.add(new Od("Philadelphia -> Philadelphia (outside coverage)", 39.9526, -75.1652, 39.9566, -75.1500, false));
		l.add(new Od("Times Sq -> Boston (one end outside)", 40.7553, -73.9869, 42.3601, -71.0589, false));
		return l;
	}

	/** The three pairs the guidance scenarios use. */
	static List<Od> guidance() {
		List<Od> all = all();
		return new ArrayList<>(all.subList(0, 3));
	}
}
