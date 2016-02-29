package net.osmand.plus.mapcontextmenu.controllers;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.TransportStop;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.resources.TransportIndexRepository;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class TransportStopController extends MenuController {

	public enum TransportStopType {
		BUS(R.drawable.mx_route_bus_ref),
		FERRY(R.drawable.mx_route_ferry_ref),
		FUNICULAR(R.drawable.mx_route_funicular_ref),
		LIGHT_RAIL(R.drawable.mx_route_light_rail_ref),
		MONORAIL(R.drawable.mx_route_monorail_ref),
		RAILWAY(R.drawable.mx_route_railway_ref),
		SHARE_TAXI(R.drawable.mx_route_share_taxi_ref),
		TRAIN(R.drawable.mx_route_train_ref),
		TRAM(R.drawable.mx_route_tram_ref),
		TROLLEYBUS(R.drawable.mx_route_trolleybus_ref),
		SUBWAY(R.drawable.mx_subway_station);

		final static TransportStopType[] ALL_TYPES = new TransportStopType[]
				{BUS, FERRY, FUNICULAR, LIGHT_RAIL, MONORAIL, RAILWAY, SHARE_TAXI, TRAIN, TRAM, TROLLEYBUS, SUBWAY};

		final int resId;

		TransportStopType(int resId) {
			this.resId = resId;
		}

		public int getResourceId() {
			return resId;
		}

		public static TransportStopType findType(String typeName) {
			String tName = typeName.toUpperCase();
			for (TransportStopType t : ALL_TYPES) {
				if (t.name().equals(tName)) {
					return t;
				}
			}
			return null;
		}

	}

	private TransportStop transportStop;
	private List<List<TransportStopRoute>> routes = new ArrayList<>();
	private boolean hasTramRoute;

	public TransportStopController(OsmandApplication app, MapActivity mapActivity,
								   PointDescription pointDescription, TransportStop transportStop) {
		super(new MenuBuilder(app), pointDescription, mapActivity);
		this.transportStop = transportStop;
		processTransportStop();
	}

	@Override
	protected void setObject(Object object) {
		if (object instanceof TransportStop) {
			this.transportStop = (TransportStop) object;
			processTransportStop();
		}
	}

	@Override
	protected int getSupportedMenuStatesPortrait() {
		return MenuState.HEADER_ONLY | MenuState.HALF_SCREEN | MenuState.FULL_SCREEN;
	}

	@Override
	public int getLeftIconId() {
		if (!hasTramRoute) {
			return R.drawable.mx_public_transport;
		} else {
			return R.drawable.mx_railway_tram_stop;
		}
	}

	@Override
	public boolean needStreetName() {
		return Algorithms.isEmpty(getNameStr());
	}

	@Override
	public boolean displayDistanceDirection() {
		return true;
	}

	@Override
	public String getTypeStr() {
		return getPointDescription().getTypeName();
	}

	@Override
	public void addPlainMenuItems(String typeStr, PointDescription pointDescription, LatLon latLon) {
		for (List<TransportStopRoute> l : routes) {
			for (TransportStopRoute r : l) {
				if (r.type == null) {
					addPlainMenuItem(R.drawable.ic_action_polygom_dark, r.desc, false, false);
				} else {
					addPlainMenuItem(r.type.resId, r.desc, false, false);
				}
			}
		}
	}

	private void processTransportStop() {
		routes.clear();
		List<TransportIndexRepository> reps = getMapActivity().getMyApplication()
				.getResourceManager().searchTransportRepositories(transportStop.getLocation().getLatitude(),
						transportStop.getLocation().getLongitude());

		boolean useEnglishNames = getMapActivity().getMyApplication().getSettings().usingEnglishNames();

		for (TransportIndexRepository t : reps) {
			if (t.acceptTransportStop(transportStop)) {
				List<String> l;
				if (useEnglishNames) {
					l = t.getRouteDescriptionsForStop(transportStop, "{1} {0} - {3}");
				} else {
					l = t.getRouteDescriptionsForStop(transportStop, "{1} {0} - {2}");
				}
				if (l != null) {
					List<TransportStopRoute> routeList = new ArrayList<>();
					for (String s : l) {
						int firstSpaceIndex = s.indexOf(' ');
						if (firstSpaceIndex != -1) {
							String typeName = s.substring(0, firstSpaceIndex);
							TransportStopType type = TransportStopType.findType(typeName);
							TransportStopRoute r = new TransportStopRoute();
							r.type = type;
							if (type == null) {
								r.desc = s;
							} else {
								r.desc = s.substring(firstSpaceIndex + 1);
							}
							routeList.add(r);
							if (!hasTramRoute && type != null && type == TransportStopType.TRAM) {
								hasTramRoute = true;
							}
						}
					}
					routes.add(routeList);
				}
			}
		}
	}

	private class TransportStopRoute {
		public TransportStopType type;
		public String desc;
	}
}
