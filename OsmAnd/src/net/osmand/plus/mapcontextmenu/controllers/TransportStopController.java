package net.osmand.plus.mapcontextmenu.controllers;

import androidx.annotation.NonNull;

import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.TransportRoute;
import net.osmand.data.TransportStop;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.mapcontextmenu.builders.TransportStopMenuBuilder;
import net.osmand.plus.transport.TransportStopRoute;
import net.osmand.plus.transport.TransportStopType;
import net.osmand.plus.views.layers.TransportStopHelper;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.List;

public class TransportStopController extends MenuController {

	public static final int SHOW_STOPS_RADIUS_METERS_UI = 150;

	private TransportStop transportStop;
	private final List<TransportStopRoute> routesNearby = new ArrayList<>();
	private final List<TransportStopRoute> routesOnTheSameExit = new ArrayList<>();
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

	public void processRoutes() {
		routesOnTheSameExit.clear();
		routesNearby.clear();
		processTransportStop(routesOnTheSameExit, routesNearby);
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
		List<TransportStopRoute> routes = new ArrayList<>(routesOnTheSameExit);
		routes.addAll(routesNearby);
		return routes;
	}

	@Override
	protected List<TransportStopRoute> getSubTransportStopRoutes(boolean nearby) {
		return nearby ? routesNearby : routesOnTheSameExit;
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
		Amenity amenity = transportStop.getAmenity();
		if (amenity == null) {
			return transportStop.getName(getPreferredMapLang(), isTransliterateNames());
		} else {
			return amenity.getName(getPreferredMapLang(), isTransliterateNames());
		}
	}

	@NonNull
	@Override
	public String getTypeStr() {
		return getPointDescription().getTypeName();
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

	private void processTransportStop(List<TransportStopRoute> routesOnTheSameExit, List<TransportStopRoute> routesNearby) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			OsmandApplication app = mapActivity.getApp();
			boolean useEnglishNames = app.getSettings().usingEnglishNames();
			if (transportStop.getTransportStopAggregated() == null) {
				TransportStopHelper.processTransportStopAggregated(app, transportStop);
			}
			ArrayList<TransportStop> transportStopsSameExit = new ArrayList<>(transportStop.getLocalTransportStops());
			ArrayList<TransportStop> nearbyTransportStops = new ArrayList<>(transportStop.getNearbyTransportStops());

			addTransportStopRoutes(app, transportStopsSameExit, routesOnTheSameExit, useEnglishNames);
			TransportStopHelper.sortTransportStopRoutes(routesOnTheSameExit);
			if (topType == null && !Algorithms.isEmpty(routesOnTheSameExit)) {
				topType = routesOnTheSameExit.get(0).type;
			}
			addTransportStopRoutes(app, nearbyTransportStops, routesNearby, useEnglishNames);
			TransportStopHelper.sortTransportStopRoutes(routesNearby);
		}
	}

	private void addTransportStopRoutes(OsmandApplication app, List<TransportStop> stops,
			List<TransportStopRoute> routes, boolean useEnglishNames) {
		for (TransportStop tstop : stops) {
			if (!tstop.isDeleted()) {
				addRoutes(app, routes, useEnglishNames, tstop, transportStop, (int) MapUtils.getDistance(tstop.getLocation(), transportStop.getLocation()));
			}
		}
	}

	private void addRoutes(OsmandApplication app, List<TransportStopRoute> routes,
			boolean useEnglishNames, TransportStop s, TransportStop refStop, int dist) {
		List<TransportRoute> rts = app.getResourceManager().getRoutesForStop(s);
		if (rts != null) {
			for (TransportRoute rs : rts) {
				boolean routeAlreadyAdded = TransportStopHelper.checkSameRoute(routes, rs);
				if (routeAlreadyAdded) {
					continue;
				}
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
}