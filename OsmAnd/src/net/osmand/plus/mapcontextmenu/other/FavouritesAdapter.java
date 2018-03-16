package net.osmand.plus.mapcontextmenu.other;

import android.app.Activity;
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
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.FavoriteImageDrawable;
import net.osmand.plus.dashboard.DashLocationFragment;
import net.osmand.util.MapUtils;

import java.util.List;

public class FavouritesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

	private final Context context;
	private final List<FavouritePoint> favouritePoints;

	private View.OnClickListener listener;
	private LatLon location;
	private Float heading;
	private boolean useCenter;
	private int screenOrientation;

	public FavouritesAdapter(Context context, List<FavouritePoint> FavouritePoints) {
		this.favouritePoints = FavouritePoints;
		this.context = context;
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
			OsmandApplication app = (OsmandApplication) ((Activity) context).getApplication();
			IconsCache iconsCache = app.getIconsCache();
			FavouritesViewHolder favouritesViewHolder = (FavouritesViewHolder) holder;
			FavouritePoint favouritePoint = getItem(position);
			favouritesViewHolder.title.setText(favouritePoint.getName());
			if (favouritePoint.getCategory().equals("")) {
				favouritesViewHolder.description.setText(R.string.shared_string_favorites);
			} else {
				favouritesViewHolder.description.setText(favouritePoint.getCategory());
			}
			Location myloc = app.getLocationProvider().getLastKnownLocation();
			favouritesViewHolder.favouriteImage.setImageDrawable(FavoriteImageDrawable.getOrCreate(context, favouritePoint.getColor(), false));
			if (myloc == null) {
				return;
			}
			float dist = (float) MapUtils.getDistance(favouritePoint.getLatitude(), favouritePoint.getLongitude(), myloc.getLatitude(), myloc.getLongitude());
			favouritesViewHolder.distance.setText(OsmAndFormatter.getFormattedDistance(dist, app));
			favouritesViewHolder.arrowImage.setImageDrawable(iconsCache.getIcon(R.drawable.ic_direction_arrow));
			DashLocationFragment.updateLocationView(useCenter, location, heading, favouritesViewHolder.arrowImage,
					favouritesViewHolder.distance, favouritePoint.getLatitude(), favouritePoint.getLongitude(),
					screenOrientation, app, context);
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

