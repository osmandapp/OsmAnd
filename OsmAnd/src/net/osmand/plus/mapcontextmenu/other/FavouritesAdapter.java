package net.osmand.plus.mapcontextmenu.other;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.Location;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.plus.IconsCache;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.FavoriteImageDrawable;
import net.osmand.plus.dashboard.DashLocationFragment;
import net.osmand.util.MapUtils;

import java.util.List;

public class FavouritesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

	private FavouritesAdapterListener listener;
	private final Context context;
	private final List<FavouritePoint> FavouritePoints;

	private LatLon location;
	private Float heading;
	private boolean useCenter;
	private int screenOrientation;

	static class FavouritesViewHolder extends RecyclerView.ViewHolder {

		final TextView title;
		final TextView description;
		final TextView distance;
		final ImageView arrowImage;
		final ImageView FavouriteImage;

		public FavouritesViewHolder(View itemView) {
			super(itemView);
			FavouriteImage = itemView.findViewById(R.id.favourite_icon);
			title = itemView.findViewById(R.id.map_marker_title);
			description = itemView.findViewById(R.id.map_marker_description);
			distance = itemView.findViewById(R.id.map_marker_distance);
			arrowImage = itemView.findViewById(R.id.map_marker_direction_icon);
		}
	}

	public FavouritesAdapter(Context context, List<FavouritePoint> FavouritePoints) {
		this.FavouritePoints = FavouritePoints;
		this.context = context;
	}

	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
		View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.bottom_sheet_item_favourite, viewGroup, false);
		view.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (listener != null) {
					listener.onItemClick(view);
				}
			}
		});
		return new FavouritesViewHolder(view);
	}


	@Override
	public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
		if (holder instanceof FavouritesViewHolder) {
			MapActivity mapActivity;
			if (context instanceof MapActivity) {
				mapActivity = (MapActivity) context;
			} else {
				return;
			}
			IconsCache iconsCache = mapActivity.getMyApplication().getIconsCache();
			FavouritesViewHolder favouritesViewHolder = (FavouritesViewHolder) holder;
			FavouritePoint favouritePoint = getItem(position);
			favouritesViewHolder.title.setText(favouritePoint.getName());
			if (!favouritePoint.getCategory().equals("")) {
				favouritesViewHolder.description.setText(favouritePoint.getCategory());
			} else {
				favouritesViewHolder.description.setText(R.string.shared_string_favorites);
			}
			Location myloc = mapActivity.getMyApplication().getLocationProvider().getLastKnownLocation();
			if (myloc == null) {
				return;
			}
			float dist = (float) MapUtils.getDistance(favouritePoint.getLatitude(), favouritePoint.getLongitude(), myloc.getLatitude(), myloc.getLongitude());

			favouritesViewHolder.distance.setText(OsmAndFormatter.getFormattedDistance(dist, mapActivity.getMyApplication()));
			favouritesViewHolder.FavouriteImage.setImageDrawable(FavoriteImageDrawable.getOrCreate(mapActivity, favouritePoint.getColor(), false));
			favouritesViewHolder.arrowImage.setImageDrawable(iconsCache.getIcon(R.drawable.ic_direction_arrow));

			DashLocationFragment.updateLocationView(useCenter, location, heading, favouritesViewHolder.arrowImage,
					favouritesViewHolder.distance, favouritePoint.getLatitude(), favouritePoint.getLongitude(),
					screenOrientation, mapActivity.getMyApplication(), mapActivity);
		}
	}

	@Override
	public int getItemCount() {
		return FavouritePoints.size();
	}

	private FavouritePoint getItem(int position) {
		return FavouritePoints.get(position);
	}

	public void setAdapterListener(FavouritesAdapterListener listener) {
		this.listener = listener;
	}

	public interface FavouritesAdapterListener {
		void onItemClick(View view);
	}

	public void setScreenOrientation(int screenOrientation) {
		this.screenOrientation = screenOrientation;
	}

	public void setLocation(LatLon location) {
		this.location = location;
	}

	public void setHeading(Float heading) {
		this.heading = heading;
	}

	public void setUseCenter(boolean useCenter) {
		this.useCenter = useCenter;
	}
}

