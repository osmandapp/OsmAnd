package net.osmand.plus.mapcontextmenu.editors;

import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.plus.activities.MapActivity;

public class FavoritePointEditor extends PointEditor {

	private FavouritePoint favorite;

	public static final String TAG = "FavoritePointEditorFragment";

	public FavoritePointEditor(MapActivity mapActivity) {
		super(mapActivity);
	}

	@Override
	public String getFragmentTag() {
		return TAG;
	}

	public FavouritePoint getFavorite() {
		return favorite;
	}

	public void add(LatLon latLon, String title) {
		if (latLon == null) {
			return;
		}
		isNew = true;
		favorite = new FavouritePoint(latLon.getLatitude(), latLon.getLongitude(), title,
				app.getSettings().LAST_FAV_CATEGORY_ENTERED.get());
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
