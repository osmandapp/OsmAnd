package net.osmand.plus.mapcontextmenu.controllers;

import static net.osmand.osm.MapPoiTypes.ROUTE_ARTICLE_POINT;

import android.graphics.drawable.Drawable;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.TransportStop;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiFilter;
import net.osmand.osm.PoiType;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.mapcontextmenu.builders.AmenityMenuBuilder;
import net.osmand.plus.mapmarkers.MapMarker;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.transport.TransportStopRoute;
import net.osmand.plus.wikipedia.WikipediaDialogFragment;
import net.osmand.plus.wikivoyage.data.TravelArticle;
import net.osmand.plus.wikivoyage.data.TravelHelper;
import net.osmand.util.Algorithms;
import net.osmand.util.OpeningHoursParser;

import java.util.List;

public class AmenityMenuController extends MenuController {

	private Amenity amenity;
	private final MapMarker marker;
	private TransportStopController transportStopController;

	public AmenityMenuController(@NonNull MapActivity mapActivity,
	                             @NonNull PointDescription pointDescription,
	                             @NonNull Amenity amenity) {
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
				TransportStop transportStop = TransportStopController.findBestTransportStopForAmenity(mapActivity.getMyApplication(), amenity);
				if (transportStop != null) {
					transportStopController = new TransportStopController(mapActivity, pointDescription, transportStop);
					transportStopController.processRoutes();
				}
			}
		}

		String mapNameForMarker = amenity.getName() + "_" + amenity.getType().getKeyName();
		marker = mapActivity.getMyApplication().getMapMarkersHelper().getMapMarker(mapNameForMarker, amenity.getLocation());
		if (marker != null) {
			MapMarkerMenuController markerMenuController =
					new MapMarkerMenuController(mapActivity, marker.getPointDescription(mapActivity), marker);
			leftTitleButtonController = markerMenuController.getLeftTitleButtonController();
			rightTitleButtonController = markerMenuController.getRightTitleButtonController();
		} else if (amenity.getSubType().equals(ROUTE_ARTICLE_POINT)) {
			TitleButtonController openTrackButtonController = new TitleButtonController() {
				@Override
				public void buttonPressed() {
					MapActivity mapActivity = getMapActivity();
					if (mapActivity != null) {
						openTrack(mapActivity);
					}
				}
			};
			openTrackButtonController.startIconId = R.drawable.ic_action_polygom_dark;
			openTrackButtonController.caption = mapActivity.getString(R.string.shared_string_open_track);
			leftTitleButtonController = openTrackButtonController;
		} else if (amenity.getType().isWiki()) {
			leftTitleButtonController = new TitleButtonController() {
				@Override
				public void buttonPressed() {
					MapActivity activity = getMapActivity();
					if (activity != null) {
						WikipediaDialogFragment.showInstance(activity, amenity);
					}
				}
			};
			leftTitleButtonController.caption = mapActivity.getString(R.string.context_menu_read_article);
			leftTitleButtonController.startIconId = R.drawable.ic_action_read_text;
		}

		openingHoursInfo = OpeningHoursParser.getInfo(amenity.getOpeningHours());
	}

	void openTrack(MapActivity mapActivity) {
		TravelHelper travelHelper = mapActivity.getMyApplication().getTravelHelper();
		String lang = amenity.getTagSuffix(Amenity.LANG_YES + ":");
		String name = amenity.getTagContent(Amenity.ROUTE_NAME);
		TravelArticle article = travelHelper.getArticleByTitle(name, lang, true, null);
		if (article != null) {
			travelHelper.openTrackMenu(article, mapActivity, name, amenity.getLocation());
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
	protected Object getCorrespondingMapObject() {
		return marker;
	}

	@Override
	public boolean needStreetName() {
		if (amenity.getSubType() != null && amenity.getType() != null) {
			PoiType pt = amenity.getType().getPoiTypeByKeyName(amenity.getSubType());
			return pt == null || pt.getOsmTag() == null || !pt.getOsmTag().equals("place");
		}
		return true;
	}

	@Override
	public int getRightIconId() {
		return getRightIconId(amenity);
	}

	public static int getRightIconId(Amenity amenity) {
		String iconName = amenity.getGpxIcon();
		if (iconName == null) {
			String mapIconName = amenity.getMapIconName();
			if (!Algorithms.isEmpty(mapIconName) && (RenderingIcons.containsBigIcon(mapIconName))) {
				iconName = mapIconName;
			} else {
				iconName = RenderingIcons.getBigIconNameForAmenity(amenity);
			}
		}
		return iconName == null ? 0 : RenderingIcons.getBigIconResourceId(iconName);
	}

	@Override
	public int getWaypointActionIconId() {
		if (marker != null) {
			return R.drawable.ic_action_edit_dark;
		}
		return super.getWaypointActionIconId();
	}

	@Override
	public int getWaypointActionStringId() {
		if (marker != null) {
			return R.string.shared_string_edit;
		}
		return super.getWaypointActionStringId();
	}

	@Override
	public boolean displayDistanceDirection() {
		return true;
	}

	@NonNull
	@Override
	public String getNameStr() {
		String preferredLang = PluginsHelper.onGetMapObjectPreferredLang(amenity,
				getPreferredMapAppLang(), getPreferredMapLang());
		String name = amenity.getName(preferredLang, isTransliterateNames());
		String ref = amenity.getAdditionalInfo("ref");
		if (!TextUtils.isEmpty(ref) && !ref.equals(name)) {
			return name + " (" + ref + ")";
		}
		if (Algorithms.isEmpty(name) && amenity.getSubType().equalsIgnoreCase("atm")) {
			String operator = amenity.getAdditionalInfo("operator");
			if (!Algorithms.isEmpty(operator)) {
				name = operator;
			}
		}
		return name;
	}

	@NonNull
	@Override
	public String getFirstNameStr() {
		if (marker != null) {
			return marker.getName(getMapActivity());
		}
		return super.getFirstNameStr();
	}

	@NonNull
	@Override
	public String getTypeStr() {
		return getTypeStr(amenity);
	}

	public static String getTypeStr(Amenity amenity) {
		return amenity.getSubTypeStr();
	}

	@NonNull
	@Override
	public String getCommonTypeStr() {
		PoiCategory pc = amenity.getType();
		return pc.getTranslation();
	}

	@Override
	public List<TransportStopRoute> getTransportStopRoutes() {
		if (transportStopController != null) {
			return transportStopController.getTransportStopRoutes();
		}
		return null;
	}

	@Override
	protected List<TransportStopRoute> getSubTransportStopRoutes(boolean nearby) {
		if (transportStopController != null) {
			return transportStopController.getSubTransportStopRoutes(nearby);
		}
		return null;
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

	@Override
	public Drawable getRightIcon() {
		String region = amenity.getAdditionalInfo("subway_region");
		if (region != null) {
			return RenderingIcons.getBigIcon(getMapActivity(), "subway_" + region);
		}
		return null;
	}
}