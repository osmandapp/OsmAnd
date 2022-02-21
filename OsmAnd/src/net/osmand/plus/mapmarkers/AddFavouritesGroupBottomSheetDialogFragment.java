package net.osmand.plus.mapmarkers;

import android.os.Bundle;

import androidx.annotation.NonNull;

import net.osmand.data.FavouritePoint;
import net.osmand.plus.myplaces.FavouritesHelper;
import net.osmand.plus.myplaces.FavoriteGroup;
import net.osmand.plus.myplaces.FavouritesHelper.FavoritesListener;
import net.osmand.plus.mapmarkers.adapters.FavouritesGroupsAdapter;
import net.osmand.plus.mapmarkers.adapters.GroupsAdapter;

public class AddFavouritesGroupBottomSheetDialogFragment extends AddGroupBottomSheetDialogFragment {

	private FavouritesHelper favouritesHelper;
	private FavoritesListener favoritesListener;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		favouritesHelper = getMyApplication().getFavoritesHelper();
	}

	@Override
	public void onPause() {
		super.onPause();
		if (favoritesListener != null) {
			favouritesHelper.removeListener(favoritesListener);
			favoritesListener = null;
		}
	}

	@Override
	public GroupsAdapter createAdapter() {
		if (!favouritesHelper.isFavoritesLoaded()) {
			favouritesHelper.addListener(favoritesListener = new FavoritesListener() {
				@Override
				public void onFavoritesLoaded() {
					if (adapter != null) {
						adapter.notifyDataSetChanged();
					}
				}

				@Override
				public void onFavoriteDataUpdated(@NonNull FavouritePoint favouritePoint) {
				}
			});
		}
		return new FavouritesGroupsAdapter(getContext(), favouritesHelper.getFavoriteGroups());
	}

	@Override
	protected void onItemClick(int position) {
		FavoriteGroup group = favouritesHelper.getFavoriteGroups().get(position - 1);
		if (!group.isVisible()) {
			favouritesHelper.editFavouriteGroup(group, group.getName(), group.getColor(), true);
		}
		getMyApplication().getMapMarkersHelper().addOrEnableGroup(group);
		dismiss();
	}
}
