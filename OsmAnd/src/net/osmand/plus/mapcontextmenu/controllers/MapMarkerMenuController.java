package net.osmand.plus.mapcontextmenu.controllers;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;

import net.osmand.data.Amenity;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.TransportStop;
import net.osmand.plus.IconsCache;
import net.osmand.plus.MapMarkersHelper;
import net.osmand.plus.MapMarkersHelper.MapMarker;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.MapMarkerDialogHelper;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.mapcontextmenu.builders.MapMarkerMenuBuilder;
import net.osmand.util.Algorithms;

public class MapMarkerMenuController extends MenuController {

	private MapMarker mapMarker;

	public MapMarkerMenuController(MapActivity mapActivity, PointDescription pointDescription, MapMarker mapMarker) {
		super(new MapMarkerMenuBuilder(mapActivity, mapMarker), pointDescription, mapActivity);
		this.mapMarker = mapMarker;
		final MapMarkersHelper markersHelper = mapActivity.getMyApplication().getMapMarkersHelper();

		leftTitleButtonController = new TitleButtonController() {
			@Override
			public void buttonPressed() {
				markersHelper.moveMapMarkerToHistory(getMapMarker());
				getMapActivity().getContextMenu().close();
			}
		};
		leftTitleButtonController.needColorizeIcon = false;
		leftTitleButtonController.caption = getMapActivity().getString(R.string.mark_passed);
		leftTitleButtonController.leftIconId = isLight() ? R.drawable.passed_icon_light : R.drawable.passed_icon_dark;

		leftSubtitleButtonController = new TitleButtonController() {
			@Override
			public void buttonPressed() {
				markersHelper.moveMarkerToTop(getMapMarker());
				getMapActivity().getContextMenu().close();
			}
		};
		leftSubtitleButtonController.caption = getMapActivity().getString(R.string.show_on_top_bar);
		leftSubtitleButtonController.leftIcon = createShowOnTopbarIcon();
	}

	private Drawable createShowOnTopbarIcon() {
		IconsCache ic = getMapActivity().getMyApplication().getIconsCache();
		Drawable background = ic.getIcon(R.drawable.ic_action_device_top,
				isLight() ? R.color.on_map_icon_color : R.color.ctx_menu_info_text_dark);
		Drawable topbar = ic.getIcon(R.drawable.ic_action_device_topbar, R.color.dashboard_blue);
		return new LayerDrawable(new Drawable[]{background, topbar});
	}

	private MapMarkerMenuBuilder getBuilder() {
		return (MapMarkerMenuBuilder) builder;
	}

	@Override
	protected void setObject(Object object) {
		if (object instanceof MapMarker) {
			this.mapMarker = (MapMarker) object;
		}
	}

	@Override
	protected Object getObject() {
		return mapMarker;
	}

	public MapMarker getMapMarker() {
		return mapMarker;
	}

	@Override
	public boolean needTypeStr() {
		return !Algorithms.isEmpty(getNameStr());
	}

	@Override
	public boolean displayDistanceDirection() {
		return true;
	}

	@Override
	public Drawable getLeftIcon() {
		return MapMarkerDialogHelper.getMapMarkerIcon(getMapActivity().getMyApplication(), mapMarker.colorIndex);
	}

	@Override
	public String getTypeStr() {
		return mapMarker.getPointDescription(getMapActivity()).getTypeName();
	}

	@Override
	public boolean needStreetName() {
		return !needTypeStr();
	}

	@Override
	public int getWaypointActionIconId() {
		return R.drawable.map_action_edit_dark;
	}

	@Override
	public int getWaypointActionStringId() {
		return R.string.rename_marker;
	}

	@Override
	public void addPlainMenuItems(String typeStr, PointDescription pointDescription, LatLon latLon) {
		FavouritePoint fav = mapMarker.favouritePoint;
		if (fav != null && !Algorithms.isEmpty(fav.getDescription())) {
			addPlainMenuItem(R.drawable.ic_action_note_dark, fav.getDescription(), true, false, null);
		}
		Object originObject = getBuilder().getOriginObject();
		if (originObject != null) {
			if (originObject instanceof Amenity) {
				Amenity amenity = (Amenity) originObject;
				AmenityMenuController.addPlainMenuItems(amenity, AmenityMenuController.getTypeStr(amenity), builder);
			} else if (originObject instanceof TransportStop) {
				TransportStop stop = (TransportStop) originObject;
				TransportStopController transportStopController =
						new TransportStopController(getMapActivity(), pointDescription, stop);
				transportStopController.addPlainMenuItems(builder, latLon);
				addMyLocationToPlainItems(latLon);
			}
		} else {
			addMyLocationToPlainItems(latLon);
		}
	}
}