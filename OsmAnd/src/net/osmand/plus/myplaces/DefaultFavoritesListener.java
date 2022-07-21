package net.osmand.plus.myplaces;

import androidx.annotation.NonNull;

import net.osmand.data.FavouritePoint;

import org.jetbrains.annotations.NotNull;

public class DefaultFavoritesListener implements FavoritesListener {

	@Override
	public void onFavoritesLoaded() { }

	@Override
	public void onFavoriteDataUpdated(@NonNull @NotNull FavouritePoint point) { }

	@Override
	public void onFavoritePropertiesUpdated() { }
}
