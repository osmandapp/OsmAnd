package net.osmand.plus.myplaces;

import androidx.annotation.NonNull;

import net.osmand.data.FavouritePoint;

public interface FavoritesListener {

	void onFavoritesLoaded();

	void onFavoriteDataUpdated(@NonNull FavouritePoint point);

	void onFavoritePropertiesUpdated();
}
