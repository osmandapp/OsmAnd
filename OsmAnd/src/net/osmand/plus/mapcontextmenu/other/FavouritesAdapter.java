package net.osmand.plus.mapcontextmenu.other;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.data.FavouritePoint;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.utils.UiUtilities.UpdateLocationViewCache;
import net.osmand.plus.views.PointImageDrawable;

import java.util.List;

public class FavouritesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {


	private final List<FavouritePoint> favouritePoints;
	private final OsmandApplication app;
	private View.OnClickListener listener;

	private final UpdateLocationViewCache cache;

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
			favouritesViewHolder.title.setText(favouritePoint.getDisplayName(app));
			favouritesViewHolder.description.setText(favouritePoint.getCategoryDisplayName(app));
			favouritesViewHolder.favouriteImage.setImageDrawable(
					PointImageDrawable.getFromFavorite(app,
							app.getFavoritesHelper().getColorWithCategory(favouritePoint,
									ContextCompat.getColor(app, R.color.color_favorite)), false, favouritePoint));
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
			favouriteImage = itemView.findViewById(R.id.favourite_icon);
			title = itemView.findViewById(R.id.favourite_title);
			description = itemView.findViewById(R.id.favourite_description);
			distance = itemView.findViewById(R.id.favourite_distance);
			arrowImage = itemView.findViewById(R.id.favourite_direction_icon);
		}
	}
}

