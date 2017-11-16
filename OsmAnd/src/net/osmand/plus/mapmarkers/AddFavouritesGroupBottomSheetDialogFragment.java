package net.osmand.plus.mapmarkers;

import android.os.Bundle;

import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.FavouritesDbHelper.FavoriteGroup;
import net.osmand.plus.MapMarkersHelper.MarkersSyncGroup;
import net.osmand.plus.mapmarkers.adapters.FavouritesGroupsAdapter;

public class AddFavouritesGroupBottomSheetDialogFragment extends AddGroupBottomSheetDialogFragment {

	private FavouritesDbHelper favouritesDbHelper;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		favouritesDbHelper = getMyApplication().getFavorites();
	}

	@Override
	public MarkersSyncGroup createMapMarkersSyncGroup(int position) {
		FavoriteGroup group = favouritesDbHelper.getFavoriteGroups().get(position - 1);
		if (!group.visible) {
			favouritesDbHelper.editFavouriteGroup(group, group.name, group.color, true);
		}
		return new MarkersSyncGroup(group.name, group.name, MarkersSyncGroup.FAVORITES_TYPE, group.color);
	}

	@Override
	public void createAdapter() {
		adapter = new FavouritesGroupsAdapter(getContext(), favouritesDbHelper.getFavoriteGroups());
	}
}
