package net.osmand.plus.track.fragments.controller;

import androidx.annotation.NonNull;

import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.myplaces.favorites.FavouritesHelper;

public class EditFavoriteDescriptionController extends EditPointDescriptionController {

	public EditFavoriteDescriptionController(@NonNull MapActivity mapActivity) {
		super(mapActivity);
	}

	@Override
	protected void saveEditedDescriptionImpl(@NonNull String editedText) {
		FavouritePoint point = (FavouritePoint) getContextMenuObject();
		if (point != null) {
			FavouritesHelper helper = activity.getApp().getFavoritesHelper();
			helper.editFavouriteName(point, point.getName(), point.getCategory(), editedText, point.getAddress());
			LatLon latLon = new LatLon(point.getLatitude(), point.getLongitude());
			updateContextMenu(latLon, point.getPointDescription(activity), point);
		}
	}

	@NonNull
	@Override
	public String getTitle() {
		FavouritePoint point = (FavouritePoint) getContextMenuObject();
		return  point != null ? point.getName() : super.getTitle();
	}

}
