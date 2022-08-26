package net.osmand.plus.mapcontextmenu.editors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.GPXUtilities;
import net.osmand.data.Amenity;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.osm.MapPoiTypes;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.myplaces.FavoriteGroup;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.util.Algorithms;

import java.util.Map;

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

	public void add(LatLon latLon, String title, String address, String amenityOriginName, String transportStopOriginName,
					int preselectedIconId, double altitude, long timestamp, Amenity amenity) {
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
		Map<String, String> extentions = amenity.toTagValue(GPXUtilities.PRIVATE_PREFIX, GPXUtilities.OSM_PREFIX, GPXUtilities.COLLAPSABLE_PREFIX, app.getPoiTypes());
		favorite.setExtensions(extentions);
		favorite.setDescription("");
		favorite.setAddress(address.isEmpty() ? title : address);
		favorite.setAmenityOriginName(amenityOriginName);
		favorite.setTransportStopOriginName(transportStopOriginName);
		favorite.setIconId(preselectedIconId);

		FavoritePointEditorFragment.showInstance(mapActivity);
	}

	public void add(LatLon latLon, String title, String amenityOriginName, String transportStopOriginName,
					String categoryName, int categoryColor, boolean autoFill) {
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
		favorite.setAmenityOriginName(amenityOriginName);
		favorite.setTransportStopOriginName(transportStopOriginName);
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
