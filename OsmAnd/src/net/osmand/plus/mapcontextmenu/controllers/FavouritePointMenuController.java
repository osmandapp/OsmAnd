package net.osmand.plus.mapcontextmenu.controllers;

import android.graphics.drawable.Drawable;
import android.support.v4.app.Fragment;

import net.osmand.data.Amenity;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.TransportStop;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.FavoriteImageDrawable;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.mapcontextmenu.builders.FavouritePointMenuBuilder;
import net.osmand.plus.mapcontextmenu.editors.FavoritePointEditor;
import net.osmand.plus.mapcontextmenu.editors.FavoritePointEditorFragment;
import net.osmand.util.Algorithms;

public class FavouritePointMenuController extends MenuController {

	private FavouritePoint fav;

	public FavouritePointMenuController(MapActivity mapActivity, PointDescription pointDescription, final FavouritePoint fav) {
		super(new FavouritePointMenuBuilder(mapActivity, fav), pointDescription, mapActivity);
		this.fav = fav;
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
	public Drawable getLeftIcon() {
		return FavoriteImageDrawable.getOrCreate(getMapActivity().getMyApplication(), fav.getColor(), false);
	}

	@Override
	public Drawable getSecondLineTypeIcon() {
		return getIcon(R.drawable.map_small_group);
	}

	@Override
	public int getFavActionIconId() {
		return R.drawable.map_action_edit_dark;
	}

	@Override
	public int getFavActionStringId() {
		return R.string.favourites_context_menu_edit;
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
