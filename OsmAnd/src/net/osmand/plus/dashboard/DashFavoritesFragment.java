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
public class DashFavoritesFragment extends DashBaseFragment {
	public static final String TAG = "DASH_FAVORITES_FRAGMENT";
	private net.osmand.Location location = null;
	private LatLon loc = null;
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
	public void onResume() {
		super.onResume();

		//'location' seems actually not needed in this Fragment, as both setupFavorites and updateArrow only reference lastKnownMapLocation
		//if (getMyApplication().getFavorites().getFavouritePoints().size() > 0) {
		//	if(!getMyApplication().getSettings().isLastKnownMapLocation()) {
		//		// show first time when application ran
		//		location = getMyApplication().getLocationProvider().getFirstTimeRunDefaultLocation();
		//	} else {
		//		location = getLocationProvider().getLastKnownLocation();
		//	}
		//}

		//This is used as origin for both Fav-list and direction arrows
		if (getMyApplication().getSettings().getLastKnownMapLocation() != null) {
			loc = getMyApplication().getSettings().getLastKnownMapLocation();
		} else {
			loc = new LatLon(0f, 0f);
		}
		setupFavorites();
	}



	public void setupFavorites(){
		if (getMyApplication() == null) {
			return;
		}
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

			if(loc != null){
				direction.setVisibility(View.VISIBLE);
				updateArrow(point, direction);
			}
			arrows.add(direction);
			name.setText(point.getName());
			icon.setImageDrawable(FavoriteImageDrawable.getOrCreate(getActivity(), point.getColor()));
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

		for(int i =0; i<arrows.size(); i++){
			arrows.get(i).setVisibility(View.VISIBLE);
			updateArrow(points.get(i), arrows.get(i));
		}
	}

	private void updateArrow(FavouritePoint point, ImageView direction) {
		float[] mes = new float[2];
		LatLon l = new LatLon(point.getLatitude(), point.getLongitude());
		Location.distanceBetween(l.getLatitude(), l.getLongitude(), loc.getLatitude(), loc.getLongitude(), mes);
		DirectionDrawable draw = new DirectionDrawable(getActivity(), 10, 10, true);
		Float h = heading;
		float a = h != null ? h : 0;

		//TODO: Hardy: The arrow direction below is correct only for the default display's standard orientation
		//      i.e. still needs to be corrected for .ROTATION_90/180/170
		//	Keep in mind: getRotation was introduced from Android 2.2
		draw.setAngle(mes[1] - a + 180);

		direction.setImageDrawable(draw);
	}

	public void updateCompassValue(float value) {
		//heading = value;
		//updateArrows();
		float lastHeading = heading != null ? heading : 99;
		heading = value;
		if (heading != null && Math.abs(MapUtils.degreesDiff(lastHeading, heading)) > 5) {
			updateArrows();
		} else {
			heading = lastHeading;
		}
	}

	public void updateLocation(Location location) {
		//'location' seems actually not needed in this Fragment, as both setupFavorites and updateArrow only reference lastKnownMapLocation
		//if (location != null) {
			//this.location = location;
			//Next line commented out so that reference is always lastKnownMapLocation, because this is also always used as reference in setupFavorites
		//	loc = new LatLon(location.getLatitude(), location.getLongitude());
		//} else if (getMyApplication().getSettings().getLastKnownMapLocation() != null) {
		//	loc = getMyApplication().getSettings().getLastKnownMapLocation();
		//} else {
		//	return;
		//}

		//This is used as origin for both Fav-list and direction arrows
		if (getMyApplication().getSettings().getLastKnownMapLocation() != null) {
			loc = getMyApplication().getSettings().getLastKnownMapLocation();
		} else {
			loc = new LatLon(0f, 0f);
		}

		this.loc = loc;
		updateArrows();
	}

	private OsmAndLocationProvider getLocationProvider() {
		return getMyApplication().getLocationProvider();
	}
}
