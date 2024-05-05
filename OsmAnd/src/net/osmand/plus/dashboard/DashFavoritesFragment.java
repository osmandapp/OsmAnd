package net.osmand.plus.dashboard;

import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dashboard.tools.DashFragmentData;
import net.osmand.plus.dialogs.DirectionsDialogs;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.myplaces.MyPlacesActivity;
import net.osmand.plus.myplaces.favorites.FavoritesListener;
import net.osmand.plus.myplaces.favorites.FavouritesHelper;
import net.osmand.plus.views.PointImageUtils;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Denis
 * on 24.11.2014.
 */
public class DashFavoritesFragment extends DashLocationFragment {

	public static final String TAG = "DASH_FAVORITES_FRAGMENT";
	public static final int TITLE_ID = R.string.shared_string_my_favorites;

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
		(view.findViewById(R.id.show_all)).setOnClickListener(v -> {
			startMyPlacesActivity(MyPlacesActivity.FAV_TAB);
			closeDashboard();
		});
		return view;
	}

	@Override
	public void onOpenDash() {
		FavouritesHelper helper = getMyApplication().getFavoritesHelper();
		if (helper.isFavoritesLoaded()) {
			setupFavorites();
		} else {
			helper.addListener(favoritesListener = new FavoritesListener() {
				@Override
				public void onFavoritesLoaded() {
					setupFavorites();
				}
			});
		}
	}

	@Override
	public void onCloseDash() {
		super.onCloseDash();
		if (favoritesListener != null) {
			getMyApplication().getFavoritesHelper().removeListener(favoritesListener);
			favoritesListener = null;
		}
	}

	public void setupFavorites() {
		View mainView = getView();
		OsmandApplication app = getMyApplication();
		if (mainView == null || app == null) {
			return;
		}

		List<FavouritePoint> favouritePoints = app.getFavoritesHelper().getFavouritePoints();
		if (Algorithms.isEmpty(favouritePoints)) {
			AndroidUiHelper.updateVisibility(mainView.findViewById(R.id.main_fav), false);
			return;
		} else {
			AndroidUiHelper.updateVisibility(mainView.findViewById(R.id.main_fav), true);
		}
		LatLon loc = getDefaultLocation();
		if (loc != null) {
			Collections.sort(favouritePoints, (left, right) -> {
				int dist = (int) (MapUtils.getDistance(left.getLatitude(), left.getLongitude(),
						loc.getLatitude(), loc.getLongitude()));
				int dist2 = (int) (MapUtils.getDistance(right.getLatitude(), right.getLongitude(),
						loc.getLatitude(), loc.getLongitude()));
				return (dist - dist2);
			});
		}
		LinearLayout favorites = mainView.findViewById(R.id.items);
		favorites.removeAllViews();
		DashboardOnMap.handleNumberOfRows(favouritePoints, app.getSettings(), ROW_NUMBER_TAG);
		List<DashLocationView> distances = new ArrayList<DashLocationFragment.DashLocationView>();
		for (FavouritePoint point : favouritePoints) {
			LayoutInflater inflater = getActivity().getLayoutInflater();
			View view = inflater.inflate(R.layout.favorites_list_item, null, false);
			TextView name = view.findViewById(R.id.favourite_label);
			TextView label = view.findViewById(R.id.distance);
			ImageView direction = view.findViewById(R.id.direction);
			direction.setVisibility(View.VISIBLE);
			label.setVisibility(View.VISIBLE);
			view.findViewById(R.id.divider).setVisibility(View.VISIBLE);
			ImageView groupImage = view.findViewById(R.id.group_image);
			if (point.getCategory().length() > 0) {
				((TextView) view.findViewById(R.id.group_name)).setText(point.getCategoryDisplayName(app));
				groupImage.setImageDrawable(app.getUIUtilities().getThemedIcon(R.drawable.ic_action_group_name_16));
			} else {
				groupImage.setVisibility(View.GONE);
			}

			int iconColor = app.getFavoritesHelper().getColorWithCategory(point, getColor(R.color.color_favorite));
			Drawable favoriteIcon = PointImageUtils.getFromPoint(app, iconColor, false, point);
			((ImageView) view.findViewById(R.id.favourite_icon)).setImageDrawable(favoriteIcon);
			DashLocationView dv = new DashLocationView(direction, label, new LatLon(point.getLatitude(),
					point.getLongitude()));
			distances.add(dv);

			name.setText(point.getDisplayName(app));
			name.setTypeface(Typeface.DEFAULT, point.isVisible() ? Typeface.NORMAL : Typeface.ITALIC);
			view.findViewById(R.id.navigate_to).setVisibility(View.VISIBLE);

			Drawable directionIcon = app.getUIUtilities().getThemedIcon(R.drawable.ic_action_gdirections_dark);
			((ImageView) view.findViewById(R.id.navigate_to)).setImageDrawable(directionIcon);

			view.findViewById(R.id.navigate_to).setOnClickListener(v -> {
				if (getActivity() != null) {
					PointDescription pointDescription = new PointDescription(
							PointDescription.POINT_TYPE_FAVORITE, point.getDisplayName(app));
					DirectionsDialogs.directionsToDialogAndLaunchMap(getActivity(), point.getLatitude(),
							point.getLongitude(), pointDescription);
				}
			});
			
			view.setOnClickListener(v -> {
				if (getActivity() != null) {
					PointDescription pointDescription = new PointDescription(
							PointDescription.POINT_TYPE_FAVORITE, point.getDisplayName(app));
					app.getSettings().setMapLocationToShow(point.getLatitude(), point.getLongitude(),
							15, pointDescription, true, point);
					MapActivity.launchMapActivityMoveToTop(getActivity());
				}
			});

			favorites.addView(view);
		}
		this.distances = distances;
	}
}