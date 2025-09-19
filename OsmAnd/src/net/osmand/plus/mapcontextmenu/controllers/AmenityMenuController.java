package net.osmand.plus.mapcontextmenu.controllers;

import static net.osmand.osm.MapPoiTypes.ROUTE_ARTICLE_POINT;
import static net.osmand.osm.MapPoiTypes.ROUTE_TRACK_POINT;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.TransportStop;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiFilter;
import net.osmand.osm.PoiType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AmenityExtensionsHelper;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.mapcontextmenu.TitleButtonController;
import net.osmand.plus.mapcontextmenu.builders.AmenityMenuBuilder;
import net.osmand.plus.mapmarkers.MapMarker;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.transport.TransportStopRoute;
import net.osmand.plus.views.layers.TransportStopHelper;
import net.osmand.plus.wikivoyage.data.TravelArticle;
import net.osmand.plus.wikivoyage.data.TravelGpx;
import net.osmand.plus.wikivoyage.data.TravelHelper;
import net.osmand.util.Algorithms;
import net.osmand.util.OpeningHoursParser;

import org.apache.commons.logging.Log;

import java.util.List;

public class AmenityMenuController extends MenuController {

	private static final Log LOG = PlatformUtil.getLog(AmenityMenuController.class);

	protected Amenity amenity;
	protected final MapMarker marker;
	protected TransportStopController transportStopController;

	public AmenityMenuController(@NonNull MapActivity mapActivity,
			@NonNull PointDescription pointDescription,
			@NonNull Amenity amenity) {
		this(mapActivity, new AmenityMenuBuilder(mapActivity, amenity), pointDescription, amenity);
	}

	public AmenityMenuController(@NonNull MapActivity mapActivity,
			@NonNull MenuBuilder builder,
			@NonNull PointDescription pointDescription,
			@NonNull Amenity amenity) {
		super(builder, pointDescription, mapActivity);
		this.amenity = amenity;
		acquireTransportStopController(mapActivity, pointDescription);

		String mapNameForMarker = amenity.getName() + "_" + amenity.getType().getKeyName();
		marker = mapActivity.getApp().getMapMarkersHelper().getMapMarker(mapNameForMarker, amenity.getLocation());
		if (marker != null) {
			MapMarkerMenuController markerMenuController =
					new MapMarkerMenuController(mapActivity, marker.getPointDescription(mapActivity), marker);
			leftTitleButtonController = markerMenuController.getLeftTitleButtonController();
			rightTitleButtonController = markerMenuController.getRightTitleButtonController();
		} else if (amenity.isRoutePoint()) {
			TitleButtonController openTrackButtonController = new TitleButtonController(this) {
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
		}
		openingHoursInfo = OpeningHoursParser.getInfo(amenity.getOpeningHours());
	}

	protected void acquireTransportStopController(@NonNull MapActivity activity,
			@NonNull PointDescription description) {
		transportStopController = acquireTransportStopController(amenity, activity, description);
		if (transportStopController != null) {
			transportStopController.processRoutes();
		}
	}

	@Nullable
	protected TransportStopController acquireTransportStopController(@NonNull Amenity amenity,
			@NonNull MapActivity activity, @NonNull PointDescription description) {
		if (amenity.getType().getKeyName().equals("transportation")) {
			boolean showTransportStops = false;
			PoiFilter filter = amenity.getType().getPoiFilterByName("public_transport");
			if (filter != null) {
				for (PoiType type : filter.getPoiTypes()) {
					if (type.getKeyName().equals(amenity.getSubType())) {
						showTransportStops = true;
						break;
					}
				}
			}
			if (showTransportStops) {
				TransportStop transportStop = TransportStopHelper.findBestTransportStopForAmenity(getApplication(), amenity);
				if (transportStop != null) {
					return new TransportStopController(activity, description, transportStop);
				}
			}
		}
		return null;
	}

	void openTrack(MapActivity mapActivity) {
		TravelHelper travelHelper = mapActivity.getApp().getTravelHelper();
		if (ROUTE_ARTICLE_POINT.equals(amenity.getSubType())) {
			String lang = amenity.getTagSuffix(Amenity.LANG_YES + ":");
			String name = amenity.getTagContent(Amenity.ROUTE_NAME);
			if (name == null || lang == null) {
				LOG.error(amenity.toString() + ": name/lang is null");
				return;
			}
			TravelArticle article = travelHelper.getArticleByTitle(name, lang, true, null);
			if (article != null) {
				travelHelper.openTrackMenu(article, mapActivity, name, amenity.getLocation(), false);
			}
		} else if (ROUTE_TRACK_POINT.equals(amenity.getSubType())) {
			TravelGpx travelGpx = travelHelper.searchTravelGpx(amenity.getLocation(), amenity.getRouteId());
			if (travelGpx != null) {
				travelHelper.openTrackMenu(travelGpx, mapActivity, travelGpx.getTitle(), amenity.getLocation(), false);
			} else {
				LOG.error("openTrack() searchTravelGpx() travelGpx is null");
			}
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
		return getRightIconId(getApplication(), amenity);
	}

	public static int getRightIconId(@NonNull Context ctx, @NonNull Amenity amenity) {
		String iconName = amenity.getGpxIcon();
		if (iconName == null) {
			String mapIconName = amenity.getMapIconName();
			if (!Algorithms.isEmpty(mapIconName) && (RenderingIcons.containsBigIcon(mapIconName))) {
				iconName = mapIconName;
			} else {
				iconName = RenderingIcons.getBigIconNameForAmenity(ctx, amenity);
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
		if (Algorithms.isEmpty(name) && amenity.isRouteTrack()) {
			name = amenity.getAdditionalInfo(Amenity.ROUTE_ID);
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
		return amenity.isRouteTrack()
				? getTypeWithDistanceStr(amenity, getApplication())
				: getTypeStr(amenity);
	}

	@NonNull
	private String getTypeWithDistanceStr(@NonNull Amenity amenity, @NonNull OsmandApplication app) {
		String type = getTypeStr(amenity);
		String metrics = AmenityExtensionsHelper.getAmenityMetricsFormatted(amenity, app);
		String activityType = amenity.getRouteActivityType();
		if (!Algorithms.isEmpty(activityType)) {
			type = activityType;
		}
		if (metrics != null) {
			return app.getString(R.string.ltr_or_rtl_combine_via_comma, type, metrics);
		} else {
			return type;
		}
	}

	public static String getTypeStr(@NonNull Amenity amenity) {
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

	public static void addTypeMenuItem(@NonNull Amenity amenity, @NonNull MenuBuilder builder) {
		String typeStr = getTypeStr(amenity);
		if (!Algorithms.isEmpty(typeStr)) {
			int resId = getRightIconId(builder.getApplication(), amenity);
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
		return amenity.isRouteTrack()
				? NetworkRouteDrawable.getIconByAmenityShieldTags(amenity, getApplication(), !isLight())
				: null;
	}
}
