package net.osmand.plus.dashboard;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.FavouritesDbHelper.FavoritesListener;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.FavoriteImageDrawable;
import net.osmand.plus.dashboard.tools.DashFragmentData;
import net.osmand.plus.dialogs.DirectionsDialogs;
import net.osmand.plus.myplaces.FavoritesActivity;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Denis
 * on 24.11.2014.
 */
public class DashFavoritesFragment extends DashLocationFragment {
	public static final String TAG = "DASH_FAVORITES_FRAGMENT";
	public static final int TITLE_ID = R.string.shared_string_my_favorites;
	List<FavouritePoint> points = new ArrayList<FavouritePoint>();

	public static final String ROW_NUMBER_TAG = TAG + "_row_number";
	private static final DashFragmentData.ShouldShowFunction SHOULD_SHOW_FUNCTION =
			new DashboardOnMap.DefaultShouldShow() {
				@Override
				public int getTitleId() {
					return TITLE_ID;
				}
			};
	public static final DashFragmentData FRAGMENT_DATA =
			new DashFragmentData(TAG, DashFavoritesFragment.class, SHOULD_SHOW_FUNCTION, 90, ROW_NUMBER_TAG);

	private FavoritesListener favoritesListener;

	@Override
	public View initView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = getActivity().getLayoutInflater().inflate(R.layout.dash_common_fragment, container, false);
		(view.findViewById(R.id.show_all)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				startFavoritesActivity(FavoritesActivity.FAV_TAB);
				closeDashboard();
			}
		});
		return view;
	}

	@Override
	public void onOpenDash() {
		FavouritesDbHelper helper = getMyApplication().getFavorites();
		if (helper.isFavoritesLoaded()) {
			setupFavorites();
		} else {
			helper.addListener(favoritesListener = new FavoritesListener() {
				@Override
				public void onFavoritesLoaded() {
					setupFavorites();
				}

				@Override
				public void onFavoriteDataUpdated(@NonNull FavouritePoint favouritePoint) {
				}
			});
		}
	}

	@Override
	public void onCloseDash() {
		super.onCloseDash();
		if (favoritesListener != null) {
			getMyApplication().getFavorites().removeListener(favoritesListener);
			favoritesListener = null;
		}
	}

	public void setupFavorites() {
		View mainView = getView();
		final FavouritesDbHelper helper = getMyApplication().getFavorites();
		points = new ArrayList<FavouritePoint>(helper.getFavouritePoints());
		if (points.size() == 0) {
			(mainView.findViewById(R.id.main_fav)).setVisibility(View.GONE);
			return;
		} else {
			(mainView.findViewById(R.id.main_fav)).setVisibility(View.VISIBLE);
		}
		final LatLon loc = getDefaultLocation();
		if (loc != null) {
			Collections.sort(points, new Comparator<FavouritePoint>() {
				@Override
				public int compare(FavouritePoint point, FavouritePoint point2) {
					// LatLon lastKnownMapLocation = getMyApplication().getSettings().getLastKnownMapLocation();
					int dist = (int) (MapUtils.getDistance(point.getLatitude(), point.getLongitude(),
							loc.getLatitude(), loc.getLongitude()));
					int dist2 = (int) (MapUtils.getDistance(point2.getLatitude(), point2.getLongitude(),
							loc.getLatitude(), loc.getLongitude()));
					return (dist - dist2);
				}
			});
		}
		LinearLayout favorites = (LinearLayout) mainView.findViewById(R.id.items);
		favorites.removeAllViews();
		DashboardOnMap.handleNumberOfRows(points, getMyApplication().getSettings(), ROW_NUMBER_TAG);
		List<DashLocationView> distances = new ArrayList<DashLocationFragment.DashLocationView>();
		for (final FavouritePoint point : points) {
			LayoutInflater inflater = getActivity().getLayoutInflater();
			View view = inflater.inflate(R.layout.favorites_list_item, null, false);
			TextView name = (TextView) view.findViewById(R.id.favourite_label);
			TextView label = (TextView) view.findViewById(R.id.distance);
			ImageView direction = (ImageView) view.findViewById(R.id.direction);
			direction.setVisibility(View.VISIBLE);
			label.setVisibility(View.VISIBLE);
			view.findViewById(R.id.divider).setVisibility(View.VISIBLE);
			ImageView groupImage = (ImageView)view.findViewById(R.id.group_image);
			if (point.getCategory().length() > 0) {
				((TextView) view.findViewById(R.id.group_name)).setText(point.getCategoryDisplayName(getMyApplication()));
				groupImage.setImageDrawable(getMyApplication().getUIUtilities().getThemedIcon(R.drawable.ic_small_group));
			} else {
				groupImage.setVisibility(View.GONE);
			}

			((ImageView) view.findViewById(R.id.favourite_icon)).setImageDrawable(FavoriteImageDrawable.getOrCreate(
					getActivity(), point.getColor(), false, point));
			DashLocationView dv = new DashLocationView(direction, label, new LatLon(point.getLatitude(),
					point.getLongitude()));
			distances.add(dv);

			name.setText(point.getDisplayName(getMyApplication()));
			name.setTypeface(Typeface.DEFAULT, point.isVisible() ? Typeface.NORMAL : Typeface.ITALIC);
			view.findViewById(R.id.navigate_to).setVisibility(View.VISIBLE);

			((ImageView) view.findViewById(R.id.navigate_to)).setImageDrawable(getMyApplication().getUIUtilities().getThemedIcon(R.drawable.ic_action_gdirections_dark));
			view.findViewById(R.id.navigate_to).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					DirectionsDialogs.directionsToDialogAndLaunchMap(getActivity(), point.getLatitude(),
							point.getLongitude(),
							new PointDescription(PointDescription.POINT_TYPE_FAVORITE, point.getDisplayName(getMyApplication())));
				}
			});
			
			view.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					getMyApplication().getSettings().setMapLocationToShow(point.getLatitude(), point.getLongitude(),
							15, new PointDescription(PointDescription.POINT_TYPE_FAVORITE, point.getDisplayName(getMyApplication())), true,
							point); //$NON-NLS-1$
					MapActivity.launchMapActivityMoveToTop(getActivity());
				}
			});
			favorites.addView(view);
		}
		this.distances = distances;
	}
}
