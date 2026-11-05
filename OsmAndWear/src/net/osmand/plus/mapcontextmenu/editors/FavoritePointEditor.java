package net.osmand.plus.mapcontextmenu.editors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.Amenity;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.osm.edit.Entity;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.myplaces.favorites.FavoriteGroup;
import net.osmand.plus.plugins.osmedit.data.OpenstreetmapPoint;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.views.layers.MapSelectionHelper;
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

	public void add(LatLon latLon, String title, String address, Object object) {
		MapActivity mapActivity = getMapActivity();
		if (latLon == null || mapActivity == null) {
			return;
		}

		isNew = true;
		String lastCategory = app.getSettings().LAST_FAV_CATEGORY_ENTERED.get();
		if (!Algorithms.isEmpty(lastCategory) && !app.getFavoritesHelper().groupExists(lastCategory)) {
			lastCategory = "";
		}
		double altitude = Double.NaN;
		if (object instanceof WptPt) {
			altitude = ((WptPt) object).getEle();
		}
		favorite = new FavouritePoint(latLon.getLatitude(), latLon.getLongitude(), title, lastCategory, altitude, 0);
		favorite.setDescription("");
		favorite.setAddress(address.isEmpty() ? title : address);

		if (object instanceof Amenity) {
			setAmenity(((Amenity) object));
		} else if (object instanceof OpenstreetmapPoint) {
			Entity entity = ((OpenstreetmapPoint) object).getEntity();
			Amenity amenity = MapSelectionHelper.findAmenityByOsmId(app, latLon, entity.getId());
			if (amenity != null) {
				setAmenity(amenity);
			}
		}
		FavoritePointEditorFragment.showInstance(mapActivity);
	}

	private void setAmenity(@NonNull Amenity amenity) {
		favorite.setAmenityOriginName(amenity.toStringEn());
		favorite.setIconId(RenderingIcons.getPreselectedIconId(amenity));
		favorite.setAmenityExtensions(amenity.getAmenityExtensions(app.getPoiTypes(), true));
	}

	public void add(LatLon latLon, String title, String categoryName, int categoryColor, boolean autoFill) {
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
