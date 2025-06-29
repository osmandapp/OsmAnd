package net.osmand.plus.mapmarkers;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.mapmarkers.adapters.FavouritesGroupsAdapter;
import net.osmand.plus.mapmarkers.adapters.GroupsAdapter;
import net.osmand.plus.myplaces.favorites.FavoriteGroup;
import net.osmand.plus.myplaces.favorites.FavoritesListener;
import net.osmand.plus.myplaces.favorites.FavouritesHelper;
import net.osmand.plus.utils.AndroidUtils;

public class AddFavouritesGroupBottomSheetDialogFragment extends AddGroupBottomSheetDialogFragment {

	private FavouritesHelper favouritesHelper;

	private FavoritesListener listener;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
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

	public static void showInstance(@NonNull FragmentManager childFragmentManager) {
		if (AndroidUtils.isFragmentCanBeAdded(childFragmentManager, TAG)) {
			AddGroupBottomSheetDialogFragment fragment = new AddFavouritesGroupBottomSheetDialogFragment();
			fragment.setUsedOnMap(false);
			fragment.setRetainInstance(true);
			fragment.show(childFragmentManager, TAG);
		}
	}
}
