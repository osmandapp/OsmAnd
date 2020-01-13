package net.osmand.plus.mapcontextmenu.controllers;

import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
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
import net.osmand.plus.mapcontextmenu.builders.FavouritePointMenuBuilder;
import net.osmand.plus.mapcontextmenu.editors.FavoritePointEditor;
import net.osmand.plus.mapcontextmenu.editors.FavoritePointEditorFragment;
import net.osmand.plus.transport.TransportStopRoute;
import net.osmand.util.OpeningHoursParser;

import java.util.List;

public class FavouritePointMenuController extends MenuController {

	private FavouritePoint fav;
	private MapMarker mapMarker;

	private TransportStopController transportStopController;

	public FavouritePointMenuController(@NonNull MapActivity mapActivity, @NonNull PointDescription pointDescription, final @NonNull FavouritePoint fav) {
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
			transportStopController = new TransportStopController(mapActivity, pointDescription, stop);
			transportStopController.processRoutes();
		}

		Object originObject = getBuilder().getOriginObject();
		if (originObject instanceof Amenity) {
			openingHoursInfo = OpeningHoursParser.getInfo(((Amenity) originObject).getOpeningHours());
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
	public boolean handleSingleTapOnMap() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			Fragment fragment = mapActivity.getSupportFragmentManager().findFragmentByTag(FavoritePointEditor.TAG);
			if (fragment != null) {
				((FavoritePointEditorFragment) fragment).dismiss();
				return true;
			}
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
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			return FavoriteImageDrawable.getOrCreate(mapActivity.getMyApplication(), fav.getColor(),
					false, fav);
		} else {
			return null;
		}
	}

	@Override
	public boolean isWaypointButtonEnabled() {
		return mapMarker == null;
	}

	@NonNull
	@Override
	public String getNameStr() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			return fav.getDisplayName(mapActivity);
		} else {
			return super.getNameStr();
		}
	}

	@Override
	public Drawable getSecondLineTypeIcon() {
		return getIcon(R.drawable.ic_action_group_name_16, isLight() ? R.color.icon_color_default_light : R.color.ctx_menu_bottom_view_icon_dark);
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
	public boolean isFavButtonEnabled() {
		return !fav.isSpecialPoint();
	}

	@NonNull
	@Override
	public String getTypeStr() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			return fav.getCategory().length() == 0 ?
					mapActivity.getString(R.string.shared_string_favorites) : fav.getCategoryDisplayName(mapActivity);
		} else {
			return "";
		}
	}

	private FavouritePointMenuBuilder getBuilder() {
		return (FavouritePointMenuBuilder) builder;
	}

	@Override
	public void addPlainMenuItems(String typeStr, PointDescription pointDescription, LatLon latLon) {
		Object originObject = getBuilder().getOriginObject();
		if (originObject != null) {
			if (originObject instanceof Amenity) {
				AmenityMenuController.addTypeMenuItem((Amenity) originObject, builder);
			}
		} 
	}
}
