package net.osmand.plus.myplaces.favorites;

import androidx.annotation.NonNull;

import net.osmand.data.FavouritePoint;

public interface FavoritesListener {

	default void onFavoritesLoaded() {
	}

	default void onFavoriteDataUpdated(@NonNull FavouritePoint point) {
	}

	default void onFavoritePropertiesUpdated() {
	}
}
