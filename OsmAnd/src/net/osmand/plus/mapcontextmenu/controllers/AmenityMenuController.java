package net.osmand.plus.mapcontextmenu.controllers;

import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.data.TransportRoute;
import net.osmand.data.TransportStop;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiFilter;
import net.osmand.osm.PoiType;
import net.osmand.plus.MapMarkersHelper.MapMarker;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.mapcontextmenu.WikipediaDialogFragment;
import net.osmand.plus.mapcontextmenu.builders.AmenityMenuBuilder;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.resources.TransportIndexRepository;
import net.osmand.plus.transport.TransportStopRoute;
import net.osmand.plus.transport.TransportStopType;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;
import net.osmand.util.OpeningHoursParser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AmenityMenuController extends MenuController {

	private Amenity amenity;
	private List<TransportStopRoute> routes = new ArrayList<>();

	private MapMarker marker;

	public AmenityMenuController(final MapActivity mapActivity, PointDescription pointDescription, final Amenity amenity) {
		super(new AmenityMenuBuilder(mapActivity, amenity), pointDescription, mapActivity);
		this.amenity = amenity;
		if (amenity.getType().getKeyName().equals("transportation")) {
			boolean showTransportStops = false;
			PoiFilter f = amenity.getType().getPoiFilterByName("public_transport");
			if (f != null) {
				for (PoiType t : f.getPoiTypes()) {
					if (t.getKeyName().equals(amenity.getSubType())) {
						showTransportStops = true;
						break;
					}
				}
			}
			if (showTransportStops) {
				processTransportStop();
			}
		}

		String mapNameForMarker = amenity.getName() + "_" + amenity.getType().getKeyName();
		marker = mapActivity.getMyApplication().getMapMarkersHelper().getMapMarker(mapNameForMarker, amenity.getLocation());
		if (marker != null) {
			MapMarkerMenuController markerMenuController =
					new MapMarkerMenuController(mapActivity, marker.getPointDescription(mapActivity), marker);
			leftTitleButtonController = markerMenuController.getLeftTitleButtonController();
			rightTitleButtonController = markerMenuController.getRightTitleButtonController();
		} else if (amenity.getType().isWiki()) {
			leftTitleButtonController = new TitleButtonController() {
				@Override
				public void buttonPressed() {
					WikipediaDialogFragment.showInstance(mapActivity, amenity);
				}
			};
			leftTitleButtonController.caption = getMapActivity().getString(R.string.context_menu_read_article);
			leftTitleButtonController.updateStateListDrawableIcon(R.drawable.ic_action_read_text, true);
		}

		openingHoursInfo = OpeningHoursParser.getInfo(amenity.getOpeningHours());
	}

	@Override
	protected void setObject(Object object) {
		if (object instanceof Amenity) {
			this.amenity = (Amenity) object;
		}
	}

	@Override
	protected Object getObject() {
		return amenity;
	}

	@Override
	public boolean isWaypointButtonEnabled() {
		return marker == null;
	}

	@Override
	public boolean needStreetName() {
		if (amenity.getSubType() != null && amenity.getType() != null) {
			PoiType pt = amenity.getType().getPoiTypeByKeyName(amenity.getSubType());
			if (pt != null && pt.getOsmTag() != null && pt.getOsmTag().equals("place")) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int getRightIconId() {
		return getRightIconId(amenity);
	}

	private static int getRightIconId(Amenity amenity) {
		String id = null;
		PoiType st = amenity.getType().getPoiTypeByKeyName(amenity.getSubType());
		if (st != null) {
			if (RenderingIcons.containsBigIcon(st.getIconKeyName())) {
				id = st.getIconKeyName();
			} else if (RenderingIcons.containsBigIcon(st.getOsmTag() + "_" + st.getOsmValue())) {
				id = st.getOsmTag() + "_" + st.getOsmValue();
			}
		}
		if (id != null) {
			return RenderingIcons.getBigIconResourceId(id);
		}
		return 0;
	}

	@Override
	public boolean displayDistanceDirection() {
		return true;
	}

	@Override
	public String getNameStr() {
		return amenity.getName(
				amenity.getType().isWiki() ? getPreferredMapAppLang() : getPreferredMapLang(),
				isTransliterateNames());
	}

	@Override
	public String getTypeStr() {
		return getTypeStr(amenity);
	}

	public static String getTypeStr(Amenity amenity) {
		PoiCategory pc = amenity.getType();
		PoiType pt = pc.getPoiTypeByKeyName(amenity.getSubType());
		String typeStr = amenity.getSubType();
		if (pt != null) {
			typeStr = pt.getTranslation();
		} else if (typeStr != null) {
			typeStr = Algorithms.capitalizeFirstLetterAndLowercase(typeStr.replace('_', ' '));
		}
		return typeStr;
	}

	@Override
	public String getCommonTypeStr() {
		PoiCategory pc = amenity.getType();
		return pc.getTranslation();
	}

	@Override
	public List<TransportStopRoute> getTransportStopRoutes() {
		return routes;
	}

	@Override
	public void addPlainMenuItems(String typeStr, PointDescription pointDescription, LatLon latLon) {
	}

	public static void addTypeMenuItem(Amenity amenity, MenuBuilder builder) {
		String typeStr = getTypeStr(amenity);
		if (!Algorithms.isEmpty(typeStr)) {
			int resId = getRightIconId(amenity);
			if (resId == 0) {
				PoiCategory pc = amenity.getType();
				resId = RenderingIcons.getBigIconResourceId(pc.getIconKeyName());
			}
			if (resId == 0) {
				resId = R.drawable.ic_action_folder_stroke;
			}
			builder.addPlainMenuItem(resId, typeStr, false, false, null);
		}
	}

	private void processTransportStop() {
		routes = new ArrayList<>();
		List<TransportIndexRepository> reps = getMapActivity().getMyApplication()
				.getResourceManager().searchTransportRepositories(amenity.getLocation().getLatitude(),
						amenity.getLocation().getLongitude());

		boolean useEnglishNames = getMapActivity().getMyApplication().getSettings().usingEnglishNames();

		for (TransportIndexRepository t : reps) {
			ArrayList<TransportStop> ls = new ArrayList<>();
			QuadRect ll = MapUtils.calculateLatLonBbox(amenity.getLocation().getLatitude(), amenity.getLocation().getLongitude(), 150);
			t.searchTransportStops(ll.top, ll.left, ll.bottom, ll.right, -1, ls, null);
			for (TransportStop tstop : ls) {
				addRoutes(useEnglishNames, t, tstop,
						(int) MapUtils.getDistance(tstop.getLocation(), amenity.getLocation()));
			}
		}
		Collections.sort(routes, new Comparator<TransportStopRoute>() {

			@Override
			public int compare(TransportStopRoute o1, TransportStopRoute o2) {
				if (o1.distance != o2.distance) {
					return Algorithms.compare(o1.distance, o2.distance);
				}
				int i1 = Algorithms.extractFirstIntegerNumber(o1.desc);
				int i2 = Algorithms.extractFirstIntegerNumber(o2.desc);
				if (i1 != i2) {
					return Algorithms.compare(i1, i2);
				}
				return o1.desc.compareTo(o2.desc);
			}
		});

		builder.setRoutes(routes);
	}

	private void addRoutes(boolean useEnglishNames, TransportIndexRepository t, TransportStop s, int dist) {
		Collection<TransportRoute> rts = t.getRouteForStop(s);
		if (rts != null) {
			for (TransportRoute rs : rts) {
				if (!containsRef(rs)) {
					TransportStopType type = TransportStopType.findType(rs.getType());
					TransportStopRoute r = new TransportStopRoute();
					r.type = type;
					r.desc = useEnglishNames ? rs.getEnName(true) : rs.getName();
					r.route = rs;
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
