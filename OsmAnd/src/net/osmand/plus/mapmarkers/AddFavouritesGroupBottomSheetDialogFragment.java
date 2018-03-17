package net.osmand.plus.mapmarkers;

import android.os.Bundle;

import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.FavouritesDbHelper.FavoriteGroup;
import net.osmand.plus.MapMarkersHelper;
import net.osmand.plus.mapmarkers.adapters.FavouritesGroupsAdapter;
import net.osmand.plus.mapmarkers.adapters.GroupsAdapter;

public class AddFavouritesGroupBottomSheetDialogFragment extends AddGroupBottomSheetDialogFragment {

	private FavouritesDbHelper favouritesDbHelper;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		favouritesDbHelper = getMyApplication().getFavorites();
	}

	@Override
	public GroupsAdapter createAdapter() {
		return new FavouritesGroupsAdapter(getContext(), favouritesDbHelper.getFavoriteGroups());
	}

	@Override
	protected void onItemClick(int position) {
		showProgressBar();
		FavoriteGroup group = favouritesDbHelper.getFavoriteGroups().get(position - 1);
		if (!group.visible) {
			favouritesDbHelper.editFavouriteGroup(group, group.name, group.color, true);
		}
		addAndSyncGroup(MapMarkersHelper.createGroup(group));
	}
}
