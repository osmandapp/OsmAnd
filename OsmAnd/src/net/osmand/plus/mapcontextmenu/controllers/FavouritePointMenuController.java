package net.osmand.plus.mapcontextmenu.controllers;

import android.graphics.drawable.Drawable;
import android.support.v4.app.Fragment;

import net.osmand.data.FavouritePoint;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.FavoriteImageDrawable;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.mapcontextmenu.builders.FavouritePointMenuBuilder;
import net.osmand.plus.mapcontextmenu.editors.FavoritePointEditor;
import net.osmand.plus.mapcontextmenu.editors.FavoritePointEditorFragment;

public class FavouritePointMenuController extends MenuController {

	private FavouritePoint fav;

	public FavouritePointMenuController(OsmandApplication app, MapActivity mapActivity, PointDescription pointDescription, final FavouritePoint fav) {
		super(new FavouritePointMenuBuilder(app, fav), pointDescription, mapActivity);
		this.fav = fav;
	}

	@Override
	protected void setObject(Object object) {
		if (object instanceof FavouritePoint) {
			this.fav = (FavouritePoint) object;
		}
	}

	@Override
	protected int getSupportedMenuStatesPortrait() {
		return MenuState.HEADER_ONLY | MenuState.HALF_SCREEN | MenuState.FULL_SCREEN;
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
	public String getTypeStr() {
		return fav.getCategory().length() == 0 ?
				getMapActivity().getString(R.string.shared_string_favorites) : fav.getCategory();
	}
}
