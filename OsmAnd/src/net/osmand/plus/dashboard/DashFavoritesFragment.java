package net.osmand.plus.dashboard;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import net.osmand.Location;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.plus.*;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.search.SearchPOIActivity;
import net.osmand.plus.base.FavoriteImageDrawable;
import net.osmand.plus.dialogs.DirectionsDialogs;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.views.DirectionDrawable;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Denis on 24.11.2014.
 */
public class DashFavoritesFragment extends DashBaseFragment implements OsmAndLocationProvider.OsmAndCompassListener, OsmAndLocationProvider.OsmAndLocationListener {
	public static final String TAG = "DASH_FAVORITES_FRAGMENT";
	private net.osmand.Location location = null;
	private Float heading = null;
	private List<ImageView> arrows = new ArrayList<ImageView>();
	List<FavouritePoint> points = new ArrayList<FavouritePoint>();

	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = getActivity().getLayoutInflater().inflate(R.layout.dash_favorites_fragment, container, false);
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
	public void onPause() {
		super.onPause();
		getLocationProvider().removeCompassListener(this);
		getLocationProvider().removeLocationListener(this);
	}

	@Override
	public void onResume() {
		super.onResume();
		if (getMyApplication().getFavorites().getFavouritePoints().size() > 0) {
			registerListeners();
			if(!getMyApplication().getSettings().isLastKnownMapLocation()) {
				// show first time when application ran
				location = getMyApplication().getLocationProvider().getFirstTimeRunDefaultLocation();
			} else {
				location = getLocationProvider().getLastKnownLocation();
			}
			updateLocation(location);
		}


		setupFavorites();

	}

	private void registerListeners() {
		getLocationProvider().addLocationListener(this);
		getLocationProvider().addCompassListener(this);
		getLocationProvider().registerOrUnregisterCompassListener(true);
		getLocationProvider().resumeAllUpdates();
	}

	private void setupFavorites(){
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
				LatLon lastKnownMapLocation = getMyApplication().getSettings().getLastKnownMapLocation();
				int dist = (int) (MapUtils.getDistance(point.getLatitude(), point.getLongitude(),
						lastKnownMapLocation.getLatitude(), lastKnownMapLocation.getLongitude()));
				int dist2 = (int) (MapUtils.getDistance(point2.getLatitude(), point2.getLongitude(),
						lastKnownMapLocation.getLatitude(), lastKnownMapLocation.getLongitude()));
				return (dist - dist2);
			}
		});
		LinearLayout favorites = (LinearLayout) mainView.findViewById(R.id.favorites);
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
			ImageView icon = (ImageView) view.findViewById(R.id.icon);
			ImageView direction = (ImageView) view.findViewById(R.id.direction);

			if(location != null){
				direction.setVisibility(View.VISIBLE);
				updateArrow(point, direction);
			}
			arrows.add(direction);
			name.setText(point.getName());
			icon.setImageDrawable(FavoriteImageDrawable.getOrCreate(getActivity(), point.getColor()));
			LatLon lastKnownMapLocation = getMyApplication().getSettings().getLastKnownMapLocation();
			int dist = (int) (MapUtils.getDistance(point.getLatitude(), point.getLongitude(),
					lastKnownMapLocation.getLatitude(), lastKnownMapLocation.getLongitude()));
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
			int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, getResources().getDisplayMetrics());

			LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, height);
			view.setLayoutParams(lp);
			favorites.addView(view);
		}
	}

	private void updateArrows() {
		if (location == null) {
			return;
		}

		for(int i =0; i<arrows.size(); i++){
			arrows.get(i).setVisibility(View.VISIBLE);
			updateArrow(points.get(i), arrows.get(i));
		}
	}

	private void updateArrow(FavouritePoint point, ImageView direction) {
		float[] mes = new float[2];
		LatLon l = new LatLon(point.getLatitude(), point.getLongitude());
		Location.distanceBetween(l.getLatitude(), l.getLongitude(), location.getLatitude(), location.getLongitude(), mes);
		DirectionDrawable draw = new DirectionDrawable(getActivity(), 10, 10, true);
		Float h = heading;
		float a = h != null ? h : 0;
		draw.setAngle(mes[1] - a + 180);
		direction.setImageDrawable(draw);
	}

	@Override
	public void updateCompassValue(float value) {
		heading = value;
		updateArrows();
	}

	@Override
	public void updateLocation(Location location) {
		if (location == null){
			return;
		}
		this.location = location;
		updateArrows();

	}

	private OsmAndLocationProvider getLocationProvider() {
		return getMyApplication().getLocationProvider();
	}
}
