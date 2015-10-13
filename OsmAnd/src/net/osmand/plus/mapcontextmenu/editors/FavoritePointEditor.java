package net.osmand.plus.mapcontextmenu.editors;

import android.os.Bundle;

import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;

public class FavoritePointEditor extends PointEditor {

	private FavouritePoint favorite;

	public static final String TAG = "FavoritePointEditorFragment";
	private static final String KEY_CTX_EDIT_FAV_OBJECT = "key_ctx_edit_fav_object";

	public FavoritePointEditor(OsmandApplication app, MapActivity mapActivity) {
		super(app, mapActivity);
	}

	@Override
	public void saveState(Bundle bundle) {
		bundle.putSerializable(KEY_CTX_EDIT_FAV_OBJECT, favorite);

	}

	@Override
	public void restoreState(Bundle bundle) {
		Object object = bundle.getSerializable(KEY_CTX_EDIT_FAV_OBJECT);
		if (object != null) {
			favorite = (FavouritePoint)object;
		}
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
