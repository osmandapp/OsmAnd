package net.osmand.plus.mapcontextmenu.editors;

import android.os.Bundle;

import net.osmand.data.FavouritePoint;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;

public class FavoritePointEditor extends PointEditor {

	private FavouritePoint favorite;

	public static final String FRAGMENT_NAME = "FavoritePointEditorFragment";

	public FavoritePointEditor(OsmandApplication app, MapActivity mapActivity) {
		super(app, mapActivity);
	}

	@Override
	public void saveState(Bundle bundle) {

	}

	@Override
	public void restoreState(Bundle bundle) {

	}

	@Override
	public String getFragmentName() {
		return FRAGMENT_NAME;
	}

	public FavouritePoint getFavorite() {
		return favorite;
	}

	public void add(PointDescription point) {
		if (point == null) {
			return;
		}
		isNew = true;
		favorite = new FavouritePoint(point.getLat(), point.getLon(), point.getName(), app.getSettings().LAST_FAV_CATEGORY_ENTERED.get());
		favorite.setDescription("");
		FavoritePointEditorFragment.showInstance(mapActivity);
	}

	public void edit(FavouritePoint favorite) {
		if (favorite == null) {
			return;
		}
		isNew = false;
		this.favorite = favorite;
		FavoritePointEditorFragment.showInstance(mapActivity);
	}

}
