package net.osmand.plus.mapcontextmenu.editors;

import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.plus.activities.MapActivity;
import net.osmand.util.Algorithms;

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
		String lastCategory = app.getSettings().LAST_FAV_CATEGORY_ENTERED.get();
		if (!Algorithms.isEmpty(lastCategory) && !app.getFavorites().groupExists(lastCategory)) {
			lastCategory = "";
		}
		favorite = new FavouritePoint(latLon.getLatitude(), latLon.getLongitude(), title, lastCategory);
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
