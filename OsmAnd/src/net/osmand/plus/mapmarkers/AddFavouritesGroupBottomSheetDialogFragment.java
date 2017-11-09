package net.osmand.plus.mapmarkers;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.osmand.plus.FavouritesDbHelper.FavoriteGroup;
import net.osmand.plus.MapMarkersHelper;
import net.osmand.plus.MapMarkersHelper.MarkersSyncGroup;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.mapmarkers.adapters.FavouritesGroupsAdapter;

import java.util.List;

public class AddFavouritesGroupBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public final static String TAG = "AddFavouritesGroupBottomSheetDialogFragment";

	private AddFavouriteGroupListener listener;

	private List<FavoriteGroup> favoriteGroups;
	private MapMarkersHelper mapMarkersHelper;

	public void setListener(AddFavouriteGroupListener listener) {
		this.listener = listener;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		favoriteGroups = getMyApplication().getFavorites().getFavoriteGroups();
		mapMarkersHelper = getMyApplication().getMapMarkersHelper();
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
		final View mainView = View.inflate(new ContextThemeWrapper(getContext(), themeRes), R.layout.fragment_marker_add_favourites_group_bottom_sheet_dialog, container);

		final RecyclerView recyclerView = mainView.findViewById(R.id.favourites_group_recycler_view);
		recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
		final FavouritesGroupsAdapter adapter = new FavouritesGroupsAdapter(getContext(), favoriteGroups);
		adapter.setAdapterListener(new FavouritesGroupsAdapter.FavouritesGroupsAdapterListener() {
			@Override
			public void onItemClick(View view) {
				int position = recyclerView.getChildAdapterPosition(view);
				if (position == RecyclerView.NO_POSITION) {
					return;
				}
				FavoriteGroup group = favoriteGroups.get(position - 1);
				MarkersSyncGroup markersSyncGroup = new MarkersSyncGroup(group.name, group.name, MarkersSyncGroup.FAVORITES_TYPE, group.color);
				mapMarkersHelper.addMarkersSyncGroup(markersSyncGroup);
				mapMarkersHelper.syncGroup(markersSyncGroup);
				if (listener != null) {
					listener.onFavouriteGroupAdded();
				}
				dismiss();
			}
		});
		recyclerView.setAdapter(adapter);

		mainView.findViewById(R.id.close_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				dismiss();
			}
		});

		setupHeightAndBackground(mainView, R.id.favourites_group_recycler_view);

		return mainView;
	}

	public interface AddFavouriteGroupListener {
		void onFavouriteGroupAdded();
	}
}
