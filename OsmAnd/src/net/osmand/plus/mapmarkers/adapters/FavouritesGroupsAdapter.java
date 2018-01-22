package net.osmand.plus.mapmarkers.adapters;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.osmand.plus.FavouritesDbHelper.FavoriteGroup;
import net.osmand.plus.IconsCache;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;

import java.util.List;

public class FavouritesGroupsAdapter extends GroupsAdapter {

	private List<FavoriteGroup> favoriteGroups;

	public FavouritesGroupsAdapter(Context context, List<FavoriteGroup> favoriteGroups) {
		super(context);
		this.favoriteGroups = favoriteGroups;
	}

	@Override
	public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
		if (holder instanceof MapMarkersGroupHeaderViewHolder) {
			MapMarkersGroupHeaderViewHolder markersGroupHeaderViewHolder = (MapMarkersGroupHeaderViewHolder) holder;
			markersGroupHeaderViewHolder.title.setText(app.getText(R.string.favourites_group));
			markersGroupHeaderViewHolder.description.setText(app.getText(R.string.add_favourites_group_to_markers_descr));
		} else if (holder instanceof MapMarkersGroupViewHolder) {
			FavoriteGroup favoriteGroup = getItem(position);
			MapMarkersGroupViewHolder markersGroupViewHolder = (MapMarkersGroupViewHolder) holder;
			int color = favoriteGroup.color == 0 || favoriteGroup.color == Color.BLACK ? app.getResources().getColor(R.color.color_favorite) : favoriteGroup.color;
			markersGroupViewHolder.icon.setImageDrawable(iconsCache.getPaintedIcon(R.drawable.ic_action_folder, color | 0xff000000));
			markersGroupViewHolder.name.setText(favoriteGroup.name.length() == 0 ? app.getString(R.string.shared_string_favorites) : favoriteGroup.name);
			markersGroupViewHolder.numberCount.setText(String.valueOf(favoriteGroup.points.size()));
		}
	}

	@Override
	public int getItemCount() {
		return favoriteGroups.size() + 1;
	}

	private FavoriteGroup getItem(int position) {
		return favoriteGroups.get(position - 1);
	}
}
