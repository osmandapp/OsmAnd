package net.osmand.plus.mapmarkers.adapters;

import android.content.Context;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.myplaces.favorites.FavoriteGroup;
import net.osmand.plus.R;

import java.util.List;

public class FavouritesGroupsAdapter extends GroupsAdapter {

	private final List<FavoriteGroup> favoriteGroups;

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
			int color = favoriteGroup.getColor() == 0 ? ContextCompat.getColor(app, R.color.color_favorite) : favoriteGroup.getColor();
			markersGroupViewHolder.icon.setImageDrawable(iconsCache.getPaintedIcon(R.drawable.ic_action_folder, color | 0xff000000));
			markersGroupViewHolder.name.setText(favoriteGroup.getName().length() == 0 ? app.getString(R.string.shared_string_favorites) : favoriteGroup.getName());
			markersGroupViewHolder.numberCount.setText(String.valueOf(favoriteGroup.getPoints().size()));
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
