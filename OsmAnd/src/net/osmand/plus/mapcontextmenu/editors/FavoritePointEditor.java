package net.osmand.plus.mapcontextmenu.editors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.myplaces.FavoriteGroup;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.util.Algorithms;

public class FavoritePointEditor extends PointEditor {

	public static final String TAG = FavoritePointEditor.class.getSimpleName();

	private FavouritePoint favorite;

	public FavoritePointEditor(@NonNull MapActivity mapActivity) {
		super(mapActivity);
	}

	@Override
	public boolean isProcessingTemplate() {
		return false;
	}

	@Nullable
	@Override
	public String getPreselectedIconName() {
		return isNew && favorite != null ? RenderingIcons.getBigIconName(favorite.getIconId()) : null;
	}

	@Override
	public String getFragmentTag() {
		return TAG;
	}

	public FavouritePoint getFavorite() {
		return favorite;
	}

	public void add(LatLon latLon, String title, String address, String originObjectName,
	                int preselectedIconId, double altitude, long timestamp) {
		MapActivity mapActivity = getMapActivity();
		if (latLon == null || mapActivity == null) {
			return;
		}

		isNew = true;
		String lastCategory = app.getSettings().LAST_FAV_CATEGORY_ENTERED.get();
		if (!Algorithms.isEmpty(lastCategory) && !app.getFavoritesHelper().groupExists(lastCategory)) {
			lastCategory = "";
		}
		favorite = new FavouritePoint(latLon.getLatitude(), latLon.getLongitude(), title, lastCategory, altitude, timestamp);
		favorite.setDescription("");
		favorite.setAddress(address.isEmpty() ? title : address);
		favorite.setOriginObjectName(originObjectName);
		favorite.setIconId(preselectedIconId);

		FavoritePointEditorFragment.showInstance(mapActivity);
	}

	public void add(LatLon latLon, String title, String originObjectName, String categoryName, int categoryColor, boolean autoFill) {
		MapActivity mapActivity = getMapActivity();
		if (latLon == null || mapActivity == null) {
			return;
		}
		isNew = true;
		if (categoryName != null && !categoryName.isEmpty()) {
			FavoriteGroup category = mapActivity.getMyApplication().getFavoritesHelper()
					.getGroup(categoryName);
			if (category == null) {
				mapActivity.getMyApplication().getFavoritesHelper().addFavoriteGroup(categoryName, categoryColor);
			}
		} else {
			categoryName = "";
		}

		favorite = new FavouritePoint(latLon.getLatitude(), latLon.getLongitude(), title, categoryName);
		favorite.setDescription("");
		favorite.setAddress("");
		favorite.setOriginObjectName(originObjectName);
		FavoritePointEditorFragment.showAutoFillInstance(mapActivity, autoFill);
	}

	public void edit(FavouritePoint favorite) {
		MapActivity mapActivity = getMapActivity();
		if (favorite == null || mapActivity == null) {
			return;
		}
		isNew = false;
		this.favorite = favorite;
		FavoritePointEditorFragment.showInstance(mapActivity);
	}
}
