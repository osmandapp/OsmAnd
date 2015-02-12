package net.osmand.plus.dashboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.osmand.Location;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.OsmAndAppCustomization;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.FavoriteImageDrawable;
import net.osmand.plus.dialogs.DirectionsDialogs;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.views.DirectionDrawable;
import net.osmand.util.MapUtils;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Created by Denis
 * on 24.11.2014.
 */
public class DashFavoritesFragment extends DashLocationFragment implements FavouritesDbHelper.FavoritesUpdatedListener {
	public static final String TAG = "DASH_FAVORITES_FRAGMENT";
	private net.osmand.Location location = null;

	private List<ImageView> arrows = new ArrayList<ImageView>();
	List<FavouritePoint> points = new ArrayList<FavouritePoint>();

	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = getActivity().getLayoutInflater().inflate(R.layout.dash_common_fragment, container, false);
		Typeface typeface = FontCache.getRobotoMedium(getActivity());
		((TextView) view.findViewById(R.id.fav_text)).setTypeface(typeface);
		((Button) view.findViewById(R.id.show_all)).setTypeface(typeface);

		(view.findViewById(R.id.show_all)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Activity activity = getActivity();
				OsmAndAppCustomization appCustomization = getMyApplication().getAppCustomization();
				final Intent favorites = new Intent(activity, appCustomization.getFavoritesActivity());
				favorites.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				activity.startActivity(favorites);
			}
		});
		return view;
	}
	
	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
	}

	@Override
	public void onResume() {
		super.onResume();
		//This is used as origin for both Fav-list and direction arrows
		if (getMyApplication().getSettings().getLastKnownMapLocation() != null) {
			loc = getMyApplication().getSettings().getLastKnownMapLocation();
		} else {
			loc = new LatLon(0f, 0f);
		}

		getMyApplication().getFavorites().addFavoritesUpdatedListener(this);
		setupFavorites();
	}

	@Override
	public void onPause() {
		super.onPause();
		getMyApplication().getFavorites().removeFavoritesUpdatedListener(this);
	}

	public void setupFavorites(){
		View mainView = getView();
		final FavouritesDbHelper helper = getMyApplication().getFavorites();
		points = new ArrayList<FavouritePoint>(helper.getFavouritePoints());
		arrows.clear();
		if (points.size() == 0){
			(mainView.findViewById(R.id.main_fav)).setVisibility(View.GONE);
			return;
		} else {
			(mainView.findViewById(R.id.main_fav)).setVisibility(View.VISIBLE);
		}

		Collections.sort(points, new Comparator<FavouritePoint>() {
			@Override
			public int compare(FavouritePoint point, FavouritePoint point2) {
				//LatLon lastKnownMapLocation = getMyApplication().getSettings().getLastKnownMapLocation();
				int dist = (int) (MapUtils.getDistance(point.getLatitude(), point.getLongitude(),
						loc.getLatitude(), loc.getLongitude()));
				int dist2 = (int) (MapUtils.getDistance(point2.getLatitude(), point2.getLongitude(),
						loc.getLatitude(), loc.getLongitude()));
				return (dist - dist2);
			}
		});
		LinearLayout favorites = (LinearLayout) mainView.findViewById(R.id.items);
		favorites.removeAllViews();
		if (points.size() > 3){
			while (points.size() != 3){
				points.remove(3);
			}
		}
		for (final FavouritePoint point : points) {
			LayoutInflater inflater = getActivity().getLayoutInflater();
			View view = inflater.inflate(R.layout.dash_favorites_item, null, false);
			TextView name = (TextView) view.findViewById(R.id.name);
			TextView label = (TextView) view.findViewById(R.id.distance);
			ImageView direction = (ImageView) view.findViewById(R.id.direction);
			if (point.getCategory().length() > 0) {
				((TextView) view.findViewById(R.id.group_name)).setText(point.getCategory());
			} else {
				view.findViewById(R.id.group_image).setVisibility(View.GONE);
			}


			((ImageView) view.findViewById(R.id.icon)).
					setImageDrawable(FavoriteImageDrawable.getOrCreate(getActivity(), point.getColor()));

			if(loc != null){
				direction.setVisibility(View.VISIBLE);
				updateArrow(getActivity(), loc, new LatLon(point.getLatitude(), point.getLongitude()), direction,
						10, R.drawable.ic_destination_arrow, heading);
			}
			arrows.add(direction);
			name.setText(point.getName());

			//LatLon lastKnownMapLocation = getMyApplication().getSettings().getLastKnownMapLocation();
			int dist = (int) (MapUtils.getDistance(point.getLatitude(), point.getLongitude(),
					loc.getLatitude(), loc.getLongitude()));
			String distance = OsmAndFormatter.getFormattedDistance(dist, getMyApplication()) + "  ";
			view.findViewById(R.id.navigate_to).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					DirectionsDialogs.directionsToDialogAndLaunchMap(getActivity(), point.getLatitude(), point.getLongitude(), point.getName());
				}
			});
			label.setText(distance, TextView.BufferType.SPANNABLE);
			label.setTypeface(Typeface.DEFAULT, point.isVisible() ? Typeface.NORMAL : Typeface.ITALIC);
			view.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					getMyApplication().getSettings().setMapLocationToShow(point.getLatitude(), point.getLongitude(), 15, null, point.getName(),
							point); //$NON-NLS-1$
					MapActivity.launchMapActivityMoveToTop(getActivity());
				}
			});
			favorites.addView(view);
		}
		updateLocation(location);
	}

	private void updateArrows() {
		if (loc == null) {
			return;
		}

		for (int i = 0; i < arrows.size(); i++) {
			arrows.get(i).setVisibility(View.VISIBLE);
			updateArrow(getActivity(), loc, new LatLon(points.get(i).getLatitude(), points.get(i).getLongitude()),
					arrows.get(i), 10, R.drawable.ic_destination_arrow, heading);
		}
	}

	@Override
	public boolean updateCompassValue(float value) {
		if (super.updateCompassValue(value)){
			updateArrows();
		}
		return true;
	}

	@Override
	public void updateLocation(Location location) {
		super.updateLocation(location);
		updateArrows();
	}

	@Override
	public void updateFavourites() {
		getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				setupFavorites();
			}
		});
	}
}
