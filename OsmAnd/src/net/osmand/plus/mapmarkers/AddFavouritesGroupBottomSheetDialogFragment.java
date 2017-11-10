package net.osmand.plus.mapmarkers;

import android.os.Bundle;

import net.osmand.plus.FavouritesDbHelper.FavoriteGroup;
import net.osmand.plus.MapMarkersHelper.MarkersSyncGroup;
import net.osmand.plus.mapmarkers.adapters.FavouritesGroupsAdapter;

import java.util.List;

public class AddFavouritesGroupBottomSheetDialogFragment extends AddMarkersGroupBottomSheetDialogFragment {

	private List<FavoriteGroup> favoriteGroups;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		favoriteGroups = getMyApplication().getFavorites().getFavoriteGroups();
	}

	@Override
	public MarkersSyncGroup createMapMarkersSyncGroup(int position) {
		FavoriteGroup group = favoriteGroups.get(position - 1);
		return new MarkersSyncGroup(group.name, group.name, MarkersSyncGroup.FAVORITES_TYPE, group.color);
	}

	@Override
	public void createAdapter() {
		adapter = new FavouritesGroupsAdapter(getContext(), favoriteGroups);
	}
}
