package net.osmand.plus.mapmarkers;

import android.os.Bundle;

import androidx.annotation.NonNull;

import net.osmand.data.FavouritePoint;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.mapmarkers.adapters.FavouritesGroupsAdapter;
import net.osmand.plus.mapmarkers.adapters.GroupsAdapter;
import net.osmand.plus.myplaces.FavoriteGroup;
import net.osmand.plus.myplaces.FavouritesHelper;
import net.osmand.plus.myplaces.FavoritesListener;

public class AddFavouritesGroupBottomSheetDialogFragment extends AddGroupBottomSheetDialogFragment {

	private OsmandApplication app;
	private FavouritesHelper favouritesHelper;

	private FavoritesListener listener;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requiredMyApplication();
		favouritesHelper = app.getFavoritesHelper();
	}

	@Override
	public void onPause() {
		super.onPause();
		if (listener != null) {
			favouritesHelper.removeListener(listener);
			listener = null;
		}
	}

	@Override
	public GroupsAdapter createAdapter() {
		if (!favouritesHelper.isFavoritesLoaded()) {
			favouritesHelper.addListener(listener = new FavoritesListener() {
				@Override
				public void onFavoritesLoaded() {
					if (adapter != null) {
						adapter.notifyDataSetChanged();
					}
				}

				@Override
				public void onFavoriteDataUpdated(@NonNull FavouritePoint point) {
				}
			});
		}
		return new FavouritesGroupsAdapter(getContext(), favouritesHelper.getFavoriteGroups());
	}

	@Override
	protected void onItemClick(int position) {
		FavoriteGroup favoriteGroup = favouritesHelper.getFavoriteGroups().get(position - 1);
		if (!favoriteGroup.isVisible()) {
			favouritesHelper.updateGroupVisibility(favoriteGroup, true, true);
		}
		app.getMapMarkersHelper().addOrEnableGroup(favoriteGroup);
		dismiss();
	}
}
