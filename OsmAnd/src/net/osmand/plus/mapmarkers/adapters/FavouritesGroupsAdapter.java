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

public class FavouritesGroupsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

	private static final int TYPE_HEADER = 12;
	private static final int TYPE_ITEM = 13;

	private FavouritesGroupsAdapterListener listener;
	private OsmandApplication app;
	private List<FavoriteGroup> favoriteGroups;
	private IconsCache iconsCache;

	public FavouritesGroupsAdapter(Context context, List<FavoriteGroup> favoriteGroups) {
		this.app = (OsmandApplication) context.getApplicationContext();
		this.favoriteGroups = favoriteGroups;
		this.iconsCache = app.getIconsCache();
	}

	public void setAdapterListener(FavouritesGroupsAdapterListener listener) {
		this.listener = listener;
	}

	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		if (viewType == TYPE_HEADER) {
			View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.add_favourites_group_header, parent, false);
			return new MapMarkersGroupHeaderViewHolder(view);
		} else {
			View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.markers_group_view_holder, parent, false);
			view.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					if (listener != null) {
						listener.onItemClick(view);
					}
				}
			});
			return new MapMarkersGroupViewHolder(view);
		}
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
	public int getItemViewType(int position) {
		return position == 0 ? TYPE_HEADER : TYPE_ITEM;
	}

	@Override
	public int getItemCount() {
		return favoriteGroups.size() + 1;
	}

	private FavoriteGroup getItem(int position) {
		return favoriteGroups.get(position - 1);
	}

	public interface FavouritesGroupsAdapterListener {
		void onItemClick(View view);
	}
}
