package net.osmand.plus.mapcontextmenu.other;

import java.util.List;

import net.osmand.data.FavouritePoint;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities.UpdateLocationViewCache;
import net.osmand.plus.base.FavoriteImageDrawable;
import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class FavouritesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {


	private final List<FavouritePoint> favouritePoints;
	private OsmandApplication app;
	private View.OnClickListener listener;

	private UpdateLocationViewCache cache;

	public FavouritesAdapter(OsmandApplication app, List<FavouritePoint> FavouritePoints) {
		this.app = app;
		this.favouritePoints = FavouritePoints;
		cache = app.getUIUtilities().getUpdateLocationViewCache();
	}

	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
		View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.favourite_list_item, viewGroup, false);
		view.setOnClickListener(listener);
		return new FavouritesViewHolder(view);
	}

	@Override
	public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
		if (holder instanceof FavouritesViewHolder) {
			FavouritesViewHolder favouritesViewHolder = (FavouritesViewHolder) holder;
			FavouritePoint favouritePoint = getItem(position);
			favouritesViewHolder.title.setText(favouritePoint.getName());
			if (favouritePoint.getCategory().equals("")) {
				favouritesViewHolder.description.setText(R.string.shared_string_favorites);
			} else {
				favouritesViewHolder.description.setText(favouritePoint.getCategory());
			}
			favouritesViewHolder.favouriteImage.setImageDrawable(
					FavoriteImageDrawable.getOrCreate(app, favouritePoint.getColor(), false, favouritePoint));
			app.getUIUtilities().updateLocationView(cache, favouritesViewHolder.arrowImage, favouritesViewHolder.distance,
					favouritePoint.getLatitude(), favouritePoint.getLongitude());
		}
	}

	@Override
	public int getItemCount() {
		return favouritePoints.size();
	}

	private FavouritePoint getItem(int position) {
		return favouritePoints.get(position);
	}

	public void setItemClickListener(View.OnClickListener listener) {
		this.listener = listener;
	}


	class FavouritesViewHolder extends RecyclerView.ViewHolder {

		final TextView title;
		final TextView description;
		final TextView distance;
		final ImageView arrowImage;
		final ImageView favouriteImage;

		public FavouritesViewHolder(View itemView) {
			super(itemView);
			favouriteImage = (ImageView) itemView.findViewById(R.id.favourite_icon);
			title = (TextView) itemView.findViewById(R.id.favourite_title);
			description = (TextView) itemView.findViewById(R.id.favourite_description);
			distance = (TextView) itemView.findViewById(R.id.favourite_distance);
			arrowImage = (ImageView) itemView.findViewById(R.id.favourite_direction_icon);
		}
	}
}

