package net.osmand.plus.mapcontextmenu.controllers;

import android.support.annotation.NonNull;

import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.data.TransportRoute;
import net.osmand.data.TransportStop;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.mapcontextmenu.builders.TransportStopMenuBuilder;
import net.osmand.plus.resources.TransportIndexRepository;
import net.osmand.plus.transport.TransportStopRoute;
import net.osmand.plus.transport.TransportStopType;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import gnu.trove.list.array.TLongArrayList;

public class TransportStopController extends MenuController {

	public static final int SHOW_STOPS_RADIUS_METERS = 150;

	private TransportStop transportStop;
	private List<TransportStopRoute> routes = new ArrayList<>();
	private TransportStopType topType;

	public TransportStopController(@NonNull MapActivity mapActivity,
								   @NonNull PointDescription pointDescription,
								   @NonNull TransportStop transportStop) {
		super(new TransportStopMenuBuilder(mapActivity, transportStop), pointDescription, mapActivity);
		this.transportStop = transportStop;
		processRoutes();
	}

	@Override
	protected void setObject(Object object) {
		if (object instanceof TransportStop) {
			this.transportStop = (TransportStop) object;
			processRoutes();
		}
	}

	private void processRoutes() {
		routes = processTransportStop();
		builder.setRoutes(routes);
	}

	@Override
	protected Object getObject() {
		return transportStop;
	}

	@Override
	public int getRightIconId() {
		if (topType == null) {
			return R.drawable.mx_public_transport;
		} else {
			return topType.getTopResourceId();
		}
	}

	@Override
	public List<TransportStopRoute> getTransportStopRoutes() {
		return routes;
	}

	@Override
	protected List<TransportStopRoute> getSubTransportStopRoutes(boolean nearby) {
		List<TransportStopRoute> allRoutes = getTransportStopRoutes();
		if (allRoutes != null) {
			List<TransportStopRoute> res = new ArrayList<>();
			for (TransportStopRoute route : allRoutes) {
				boolean isCurrentRouteNearby = route.stop != null && !route.stop.equals(transportStop);
				if ((nearby && isCurrentRouteNearby) || (!nearby && !isCurrentRouteNearby)) {
					res.add(route);
				}
			}
			return res;
		}
		return null;
	}
	
	@Override
	public boolean needStreetName() {
		return Algorithms.isEmpty(getNameStr());
	}

	@Override
	public boolean displayDistanceDirection() {
		return true;
	}

	@NonNull
	@Override
	public String getNameStr() {
		return transportStop.getName(getPreferredMapLang(), isTransliterateNames());
	}

	@NonNull
	@Override
	public String getTypeStr() {
		return getPointDescription().getTypeName();
	}

	public List<TransportStopRoute> processTransportStop() {
		ArrayList<TransportStopRoute> routes = new ArrayList<>();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			List<TransportIndexRepository> reps = mapActivity.getMyApplication()
					.getResourceManager().searchTransportRepositories(transportStop.getLocation().getLatitude(),
							transportStop.getLocation().getLongitude());

			boolean useEnglishNames = mapActivity.getMyApplication().getSettings().usingEnglishNames();

			TLongArrayList addedTransportStops = new TLongArrayList();
			for (TransportIndexRepository t : reps) {
				if (t.acceptTransportStop(transportStop)) {
					boolean empty = transportStop.getReferencesToRoutes() == null || transportStop.getReferencesToRoutes().length == 0;
					if (!empty) {
						addRoutes(routes, useEnglishNames, t, transportStop, transportStop, 0);
					}
					ArrayList<TransportStop> ls = new ArrayList<>();
					QuadRect ll = MapUtils.calculateLatLonBbox(transportStop.getLocation().getLatitude(), transportStop.getLocation().getLongitude(), SHOW_STOPS_RADIUS_METERS);
					t.searchTransportStops(ll.top, ll.left, ll.bottom, ll.right, -1, ls, null);
					for (TransportStop tstop : ls) {
						if (!addedTransportStops.contains(tstop.getId())) {
							addedTransportStops.add(tstop.getId());
							if (!tstop.isDeleted() && (tstop.getId().longValue() != transportStop.getId().longValue() || empty)) {
								addRoutes(routes, useEnglishNames, t, tstop, transportStop,
										(int) MapUtils.getDistance(tstop.getLocation(), transportStop.getLocation()));
							}
						}
					}
				}
			}
			Collections.sort(routes, new Comparator<TransportStopRoute>() {

				@Override
				public int compare(TransportStopRoute o1, TransportStopRoute o2) {
//					int radEqual = 50;
//					int dist1 = o1.distance / radEqual;
//					int dist2 = o2.distance / radEqual;
//					if (dist1 != dist2) {
//						return Algorithms.compare(dist1, dist2);
//					}
					int i1 = Algorithms.extractFirstIntegerNumber(o1.route.getRef());
					int i2 = Algorithms.extractFirstIntegerNumber(o2.route.getRef());
					if (i1 != i2) {
						return Algorithms.compare(i1, i2);
					}
					return o1.desc.compareTo(o2.desc);
				}
			});
		}
		return routes;
	}

	private void addRoutes(List<TransportStopRoute> routes, boolean useEnglishNames, TransportIndexRepository t, TransportStop s, TransportStop refStop, int dist) {
		Collection<TransportRoute> rts = t.getRouteForStop(s);
		if (rts != null) {
			for (TransportRoute rs : rts) {
				TransportStopType type = TransportStopType.findType(rs.getType());
				if (topType == null && type != null && type.isTopType()) {
					topType = type;
				}
				TransportStopRoute r = new TransportStopRoute();
				r.type = type;
				r.desc = useEnglishNames ? rs.getEnName(true) : rs.getName();
				r.route = rs;
				r.refStop = refStop;
				r.stop = s;
				r.distance = dist;
				routes.add(r);
			}
		}
	}

	@Override
	public void addPlainMenuItems(String typeStr, PointDescription pointDescription, LatLon latLon) {
		Amenity amenity = transportStop.getAmenity();
		if (amenity != null) {
			AmenityMenuController.addTypeMenuItem(amenity, builder);
		} else {
			super.addPlainMenuItems(typeStr, pointDescription, latLon);
		}
	}
}
