package net.osmand.plus.myplaces.favorites.dialogs;

import androidx.annotation.Nullable;

import net.osmand.plus.myplaces.favorites.FavoriteGroup;

public interface IFavoriteListListener {

	void reloadData();

	void shareFavorites(@Nullable FavoriteGroup group);
}
