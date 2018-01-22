package net.osmand.plus.mapcontextmenu.controllers;

import android.graphics.drawable.Drawable;
import android.support.v4.app.Fragment;

import net.osmand.data.Amenity;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.TransportStop;
import net.osmand.plus.MapMarkersHelper;
import net.osmand.plus.MapMarkersHelper.MapMarker;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.FavoriteImageDrawable;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.mapcontextmenu.OpeningHoursInfo;
import net.osmand.plus.mapcontextmenu.builders.FavouritePointMenuBuilder;
import net.osmand.plus.mapcontextmenu.editors.FavoritePointEditor;
import net.osmand.plus.mapcontextmenu.editors.FavoritePointEditorFragment;
import net.osmand.plus.transport.TransportStopRoute;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class FavouritePointMenuController extends MenuController {

	private FavouritePoint fav;
	private MapMarker mapMarker;
	private List<TransportStopRoute> routes = new ArrayList<>();
	private OpeningHoursInfo openingHoursInfo;

	public FavouritePointMenuController(MapActivity mapActivity, PointDescription pointDescription, final FavouritePoint fav) {
		super(new FavouritePointMenuBuilder(mapActivity, fav), pointDescription, mapActivity);
		this.fav = fav;

		final MapMarkersHelper markersHelper = mapActivity.getMyApplication().getMapMarkersHelper();
		mapMarker = markersHelper.getMapMarker(fav);
		if (mapMarker == null) {
			mapMarker = markersHelper.getMapMarker(new LatLon(fav.getLatitude(), fav.getLongitude()));
		}
		if (mapMarker != null) {
			MapMarkerMenuController markerMenuController =
					new MapMarkerMenuController(mapActivity, mapMarker.getPointDescription(mapActivity), mapMarker);
			leftTitleButtonController = markerMenuController.getLeftTitleButtonController();
			rightTitleButtonController = markerMenuController.getRightTitleButtonController();
		}
		if (getObject() instanceof TransportStop) {
			TransportStop stop = (TransportStop) getObject();
			TransportStopController transportStopController = new TransportStopController(getMapActivity(), pointDescription, stop);
			routes = transportStopController.processTransportStop();
			builder.setRoutes(routes);
		}

		Object originObject = getBuilder().getOriginObject();
		if (originObject instanceof Amenity) {
			openingHoursInfo = AmenityMenuController.processOpeningHours((Amenity) originObject);
		}
	}

	@Override
	protected void setObject(Object object) {
		if (object instanceof FavouritePoint) {
			this.fav = (FavouritePoint) object;
		}
	}

	@Override
	protected Object getObject() {
		return fav;
	}

	@Override
	public List<TransportStopRoute> getTransportStopRoutes() {
		return routes;
	}

	@Override
	public boolean handleSingleTapOnMap() {
		Fragment fragment = getMapActivity().getSupportFragmentManager().findFragmentByTag(FavoritePointEditor.TAG);
		if (fragment != null) {
			((FavoritePointEditorFragment)fragment).dismiss();
			return true;
		}
		return false;
	}

	@Override
	public boolean needStreetName() {
		return false;
	}

	@Override
	public boolean needTypeStr() {
		return true;
	}

	@Override
	public boolean displayDistanceDirection() {
		return true;
	}

	@Override
	public Drawable getRightIcon() {
		return FavoriteImageDrawable.getOrCreate(getMapActivity().getMyApplication(), fav.getColor(), false);
	}

	@Override
	public boolean isWaypointButtonEnabled() {
		return mapMarker == null;
	}

	@Override
	public Drawable getSecondLineTypeIcon() {
		return getIcon(R.drawable.ic_action_group_name_16, isLight() ? R.color.icon_color : R.color.ctx_menu_bottom_view_icon_dark);
	}

	@Override
	public int getFavActionIconId() {
		return R.drawable.map_action_edit_dark;
	}

	@Override
	public int getFavActionStringId() {
		return R.string.shared_string_edit;
	}

	@Override
	public String getTypeStr() {
		return fav.getCategory().length() == 0 ?
				getMapActivity().getString(R.string.shared_string_favorites) : fav.getCategory();
	}

	private FavouritePointMenuBuilder getBuilder() {
		return (FavouritePointMenuBuilder) builder;
	}

	@Override
	public void addPlainMenuItems(String typeStr, PointDescription pointDescription, LatLon latLon) {
		if (!Algorithms.isEmpty(fav.getDescription())) {
			addPlainMenuItem(R.drawable.ic_action_note_dark, null, fav.getDescription(), true, false, null);
		}
		Object originObject = getBuilder().getOriginObject();
		if (originObject != null) {
			if (originObject instanceof Amenity) {
				Amenity amenity = (Amenity) originObject;
				AmenityMenuController.addPlainMenuItems(amenity, AmenityMenuController.getTypeStr(amenity), builder);
			}
		} else {
			addMyLocationToPlainItems(latLon);
		}
	}

	@Override
	public int getAdditionalInfoColor() {
		if (openingHoursInfo != null) {
			return openingHoursInfo.isOpened() ? R.color.ctx_menu_amenity_opened_text_color : R.color.ctx_menu_amenity_closed_text_color;
		}
		return 0;
	}

	@Override
	public String getAdditionalInfoStr() {
		if (openingHoursInfo != null) {
			return openingHoursInfo.getInfo(getMapActivity());
		}
		return "";
	}

	@Override
	public int getAdditionalInfoIconRes() {
		if (openingHoursInfo != null) {
			return R.drawable.ic_action_opening_hour_16;
		}
		return 0;
	}
}
