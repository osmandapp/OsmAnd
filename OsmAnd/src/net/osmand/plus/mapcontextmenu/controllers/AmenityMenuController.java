package net.osmand.plus.mapcontextmenu.controllers;

import android.view.View;

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
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.mapcontextmenu.OpeningHoursInfo;
import net.osmand.plus.mapcontextmenu.builders.AmenityMenuBuilder;
import net.osmand.plus.mapcontextmenu.controllers.TransportStopController.TransportStopRoute;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.resources.TransportIndexRepository;
import net.osmand.plus.views.POIMapLayer;
import net.osmand.plus.views.TransportStopsLayer;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;
import net.osmand.util.OpeningHoursParser;

import java.util.ArrayList;
import java.util.Calendar;
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

		marker = mapActivity.getMyApplication().getMapMarkersHelper().getMapMarker(amenity.getName());
		if (marker != null) {
			MapMarkerMenuController markerMenuController =
					new MapMarkerMenuController(mapActivity, marker.getPointDescription(mapActivity), marker);
			leftTitleButtonController = markerMenuController.getLeftTitleButtonController();
			rightTitleButtonController = markerMenuController.getRightTitleButtonController();
		} else if (amenity.getType().isWiki()) {
			leftTitleButtonController = new TitleButtonController() {
				@Override
				public void buttonPressed() {
					POIMapLayer.showWikipediaDialog(mapActivity, mapActivity.getMyApplication(), amenity);
				}
			};
			leftTitleButtonController.caption = getMapActivity().getString(R.string.context_menu_read_article);
			leftTitleButtonController.leftIcon = getIcon(R.drawable.ic_action_note_dark, isLight() ? R.color.ctx_menu_controller_button_text_color_light_n : R.color.ctx_menu_controller_button_text_color_dark_n);
		}
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
	public int getLeftIconId() {
		return getLeftIconId(amenity);
	}

	private static int getLeftIconId(Amenity amenity) {
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

	@Override
	public OpeningHoursInfo getOpeningHoursInfo() {
		return processOpeningHours(amenity);
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

	public static OpeningHoursInfo processOpeningHours(Amenity amenity) {
		OpeningHoursParser.OpeningHours openingHours = OpeningHoursParser.parseOpenedHours(amenity.getOpeningHours());
		if (openingHours == null) {
			return null;
		} else {
			OpeningHoursInfo info = new OpeningHoursInfo();
			Calendar cal = Calendar.getInstance();
			boolean opened = openingHours.isOpenedForTime(cal);
			info.setOpened(opened);
			if (opened) {
				info.setOpened24_7(openingHours.isOpened24_7());
				info.setClosingTime(openingHours.getClosingTime(cal));
				info.setNearToClosingTime(openingHours.getNearToClosingTime(cal));
			} else {
				info.setOpeningTime(openingHours.getOpeningTime(cal));
				info.setNearToOpeningTime(openingHours.getNearToOpeningTime(cal));
				info.setOpeningDay(openingHours.getOpeningDay(cal));
			}
			return info;
		}
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

	public static void addPlainMenuItems(Amenity amenity, String typeStr, MenuBuilder builder) {
		if (!Algorithms.isEmpty(typeStr)) {
			int resId = getLeftIconId(amenity);
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
					TransportStopController.TransportStopType type = TransportStopController.TransportStopType.findType(rs.getType());
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
