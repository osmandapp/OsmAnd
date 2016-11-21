package net.osmand.plus.mapcontextmenu.controllers;

import android.view.View;
import android.view.View.OnClickListener;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.data.TransportRoute;
import net.osmand.data.TransportStop;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.resources.TransportIndexRepository;
import net.osmand.plus.views.TransportStopsLayer;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TransportStopController extends MenuController {

	public enum TransportStopType {
		BUS(R.drawable.mx_route_bus_ref, R.drawable.mx_route_bus_ref),
		FERRY(R.drawable.mx_route_ferry_ref, R.drawable.mx_route_ferry_ref),
		FUNICULAR(R.drawable.mx_route_funicular_ref, R.drawable.mx_route_funicular_ref),
		LIGHT_RAIL(R.drawable.mx_route_light_rail_ref, R.drawable.mx_route_light_rail_ref),
		MONORAIL(R.drawable.mx_route_monorail_ref, R.drawable.mx_route_monorail_ref),
		RAILWAY(R.drawable.mx_route_railway_ref, R.drawable.mx_route_railway_ref),
		SHARE_TAXI(R.drawable.mx_route_share_taxi_ref, R.drawable.mx_route_share_taxi_ref),
		TRAIN(R.drawable.mx_route_train_ref, R.drawable.mx_route_train_ref),
		TRAM(R.drawable.mx_route_tram_ref, R.drawable.mx_railway_tram_stop),
		TROLLEYBUS(R.drawable.mx_route_trolleybus_ref, R.drawable.mx_route_trolleybus_ref),
		SUBWAY(R.drawable.mx_subway_station, R.drawable.mx_subway_station);

		final int resId;
		final int topResId;

		TransportStopType(int resId, int topResId) {
			this.resId = resId;
			this.topResId = topResId;
		}

		public int getResourceId() {
			return resId;
		}

		public int getTopResourceId() {
			return topResId;
		}

		public boolean isTopType() {
			return this == TRAM || this == SUBWAY;
		}

		public static TransportStopType findType(String typeName) {
			String tName = typeName.toUpperCase();
			for (TransportStopType t : values()) {
				if (t.name().equals(tName)) {
					return t;
				}
			}
			return null;
		}

	}

	private TransportStop transportStop;
	private List<TransportStopRoute> routes = new ArrayList<>();
	private TransportStopType topType;

	public TransportStopController(MapActivity mapActivity,
								   PointDescription pointDescription, TransportStop transportStop) {
		super(new MenuBuilder(mapActivity), pointDescription, mapActivity);
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
	protected Object getObject() {
		return transportStop;
	}

	@Override
	public int getLeftIconId() {
		if (topType == null) {
			return R.drawable.mx_public_transport;
		} else {
			return topType.getTopResourceId();
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
	public void addPlainMenuItems(String typeStr, PointDescription pointDescription, final LatLon latLon) {
		addPlainMenuItems(builder, latLon);
		super.addPlainMenuItems(typeStr, pointDescription, latLon);
	}

	public void addPlainMenuItems(MenuBuilder builder, final LatLon latLon) {
		for (final TransportStopRoute r : routes) {
			OnClickListener listener = new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					MapContextMenu mm = getMapActivity().getContextMenu();
					PointDescription pd = new PointDescription(PointDescription.POINT_TYPE_TRANSPORT_ROUTE, 
							r.getDescription(getMapActivity().getMyApplication(), false));
					mm.show(latLon, pd, r);
					TransportStopsLayer stopsLayer = getMapActivity().getMapLayers().getTransportStopsLayer();
					stopsLayer.setRoute(r.route);
					int cz = r.calculateZoom(0, getMapActivity().getMapView().getCurrentRotatedTileBox());
					getMapActivity().changeZoom(cz - getMapActivity().getMapView().getZoom());
				}
			};
			if (r.type == null) {
				builder.addPlainMenuItem(R.drawable.ic_action_polygom_dark, r.getDescription(getMapActivity().getMyApplication(), true),
						false, false, listener );
			} else {
				builder.addPlainMenuItem(r.type.getResourceId(), r.getDescription(getMapActivity().getMyApplication(), true),
						false, false, listener);
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
				boolean empty = transportStop.getReferencesToRoutes() == null || transportStop.getReferencesToRoutes().length == 0;
				if(!empty) {
					addRoutes(useEnglishNames, t, transportStop, transportStop, 0);
				}
				ArrayList<TransportStop> ls = new ArrayList<>();
				QuadRect ll = MapUtils.calculateLatLonBbox(transportStop.getLocation().getLatitude(), transportStop.getLocation().getLongitude(), 150);
				t.searchTransportStops(ll.top, ll.left, ll.bottom, ll.right, -1, ls, null);
				for(TransportStop tstop : ls) {
					if(tstop.getId().longValue() != transportStop.getId().longValue() || empty) {
						addRoutes(useEnglishNames, t, tstop, transportStop,  
								(int) MapUtils.getDistance(tstop.getLocation(), transportStop.getLocation()));
					}
				}
			}
		}
		Collections.sort(routes, new Comparator<TransportStopRoute>() {

			@Override
			public int compare(TransportStopRoute o1, TransportStopRoute o2) {
				if(o1.distance != o2.distance) {
					return Algorithms.compare(o1.distance, o2.distance);
				}
				int i1 = Algorithms.extractFirstIntegerNumber(o1.desc);
				int i2 = Algorithms.extractFirstIntegerNumber(o2.desc);
				if(i1 != i2) {
					return Algorithms.compare(i1, i2);
				}
				return o1.desc.compareTo(o2.desc);
			}
		});
	}

	private void addRoutes(boolean useEnglishNames, TransportIndexRepository t, TransportStop s, TransportStop refStop, int dist) {
		Collection<TransportRoute> rts = t.getRouteForStop(s);
		if (rts != null) {
			for (TransportRoute rs : rts) {
				TransportStopType type = TransportStopType.findType(rs.getType());
				TransportStopRoute r = new TransportStopRoute();
				if (topType == null && type != null && type.isTopType()) {
					topType = type;
				}
				r.type = type;
				r.desc = rs.getRef() + " " + (useEnglishNames ?  rs.getEnName(true) : rs.getName());
				r.route = rs;
				r.refStop = refStop;
				r.stop = s;
				r.distance = dist;
				this.routes.add(r);
			}
		}
	}

	public static class TransportStopRoute {
		public TransportStop refStop;
		public TransportStopType type;
		public String desc;
		public TransportRoute route;
		public TransportStop stop;
		public int distance;
		public boolean showWholeRoute;

		public String getDescription(OsmandApplication ctx, boolean useDistance) {
			if (useDistance && distance > 0) {
				String nm = OsmAndFormatter.getFormattedDistance(distance, ctx);
				if (refStop != null && !refStop.getName().equals(stop.getName())) {
					nm = refStop.getName() + ", " + nm;
				}
				return desc + " (" + nm + ")";
			}
			return desc;
		}
		
		public int calculateZoom(int startPosition, RotatedTileBox currentRotatedTileBox) {
			RotatedTileBox cp = currentRotatedTileBox.copy();
			boolean notContains = true;
			while (cp.getZoom() > 12 && notContains) {
				notContains = false;
				List<TransportStop> sts = route.getForwardStops();
				for(int i = startPosition; i < sts.size(); i++) {
					TransportStop st = sts.get(startPosition);
					if (!cp.containsLatLon(st.getLocation())) {
						notContains = true;
						break;
					}
				}
				cp.setZoom(cp.getZoom() - 1);
			}
			return cp.getZoom();
		}
	}
}
