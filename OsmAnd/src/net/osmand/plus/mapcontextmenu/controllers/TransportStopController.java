package net.osmand.plus.mapcontextmenu.controllers;

import android.view.View;
import android.view.View.OnClickListener;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.data.TransportRoute;
import net.osmand.data.TransportStop;
import net.osmand.plus.R;
import net.osmand.plus.transport.TransportStopRoute;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.resources.TransportIndexRepository;
import net.osmand.plus.transport.TransportStopType;
import net.osmand.plus.views.TransportStopsLayer;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TransportStopController extends MenuController {

	private TransportStop transportStop;
	private List<TransportStopRoute> routes = new ArrayList<>();
	private TransportStopType topType;

	public TransportStopController(MapActivity mapActivity,
								   PointDescription pointDescription, TransportStop transportStop) {
		super(new MenuBuilder(mapActivity), pointDescription, mapActivity);
		this.transportStop = transportStop;
		processTransportStop(builder);
	}

	@Override
	protected void setObject(Object object) {
		if (object instanceof TransportStop) {
			this.transportStop = (TransportStop) object;
			processTransportStop(builder);
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
	public List<TransportStopRoute> getTransportStopRoutes() {
		return routes;
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
					stopsLayer.setRoute(r);
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

	public void processTransportStop(MenuBuilder builder) {
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

		builder.setRoutes(routes);
	}

	private void addRoutes(boolean useEnglishNames, TransportIndexRepository t, TransportStop s, TransportStop refStop, int dist) {
		Collection<TransportRoute> rts = t.getRouteForStop(s);
		if (rts != null) {
			for (TransportRoute rs : rts) {
				TransportStopType type = TransportStopType.findType(rs.getType());
				if (topType == null && type != null && type.isTopType()) {
					topType = type;
				}
				if (!containsRef(rs)) {
					TransportStopRoute r = new TransportStopRoute();
					r.type = type;
					r.desc = useEnglishNames ? rs.getEnName(true) : rs.getName();
					r.route = rs;
					r.refStop = refStop;
					r.stop = s;
					r.distance = dist;
					this.routes.add(r);
				}
			}
		}
	}

	private boolean containsRef(TransportRoute transportRoute) {
		for (TransportStopRoute route : routes) {
			if (route.route.getRef().equals(transportRoute.getRef())) {
				return true;
			}
		}
		return false;
	}

}
