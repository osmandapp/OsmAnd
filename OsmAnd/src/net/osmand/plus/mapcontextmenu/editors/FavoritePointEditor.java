package net.osmand.plus.mapcontextmenu.editors;

import androidx.annotation.NonNull;

import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.activities.MapActivity;
import net.osmand.util.Algorithms;

public class FavoritePointEditor extends PointEditor {

	private FavouritePoint favorite;

	public static final String TAG = "FavoritePointEditorFragment";

	public FavoritePointEditor(@NonNull MapActivity mapActivity) {
		super(mapActivity);
	}

	@Override
	public String getFragmentTag() {
		return TAG;
	}

	public FavouritePoint getFavorite() {
		return favorite;
	}

	public void add(LatLon latLon, String title, String address, String originObjectName, double altitude, long timestamp) {
		MapActivity mapActivity = getMapActivity();
		if (latLon == null || mapActivity == null) {
			return;
		}
		isNew = true;
		String lastCategory = app.getSettings().LAST_FAV_CATEGORY_ENTERED.get();
		if (!Algorithms.isEmpty(lastCategory) && !app.getFavorites().groupExists(lastCategory)) {
			lastCategory = "";
		}
		favorite = new FavouritePoint(latLon.getLatitude(), latLon.getLongitude(), title, lastCategory, altitude, timestamp);
		favorite.setDescription("");
		favorite.setAddress(address.isEmpty() ? title : address);
		favorite.setOriginObjectName(originObjectName);
		FavoritePointEditorFragmentNew.showInstance(mapActivity);
	}

	public void add(LatLon latLon, String title, String originObjectName, String categoryName, int categoryColor, boolean autoFill) {
		MapActivity mapActivity = getMapActivity();
		if (latLon == null || mapActivity == null) {
			return;
		}
		isNew = true;
		if (categoryName != null && !categoryName.isEmpty()) {
			FavouritesDbHelper.FavoriteGroup category = mapActivity.getMyApplication().getFavorites()
					.getGroup(categoryName);
			if (category == null) {
				mapActivity.getMyApplication().getFavorites().addEmptyCategory(categoryName, categoryColor);
			}
		} else {
			categoryName = "";
		}

		favorite = new FavouritePoint(latLon.getLatitude(), latLon.getLongitude(), title, categoryName);
		favorite.setDescription("");
		favorite.setAddress("");
		favorite.setOriginObjectName(originObjectName);
		FavoritePointEditorFragmentNew.showAutoFillInstance(mapActivity, autoFill);
	}

	public void edit(FavouritePoint favorite) {
		MapActivity mapActivity = getMapActivity();
		if (favorite == null || mapActivity == null) {
			return;
		}
		isNew = false;
		this.favorite = favorite;
		FavoritePointEditorFragmentNew.showInstance(mapActivity);
	}
}
