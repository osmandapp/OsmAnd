package net.osmand.plus.auto;

import static android.text.Spanned.SPAN_INCLUSIVE_INCLUSIVE;

import android.text.SpannableString;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.CarContext;
import androidx.car.app.constraints.ConstraintManager;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.CarLocation;
import androidx.car.app.model.DistanceSpan;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.Metadata;
import androidx.car.app.model.Place;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import androidx.car.app.navigation.model.PlaceListNavigationTemplate;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.R;
import net.osmand.plus.myplaces.favorites.FavoriteGroup;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.PointImageDrawable;
import net.osmand.search.core.ObjectType;
import net.osmand.search.core.SearchResult;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Screen for showing a list of favorite places.
 */
public final class FavoritesScreen extends BaseOsmAndAndroidAutoScreen {
	private static final String TAG = "NavigationDemo";

	@NonNull
	private final Action settingsAction;
	@NonNull
	private final SurfaceRenderer surfaceRenderer;

	@Nullable
	private FavoriteGroup selectedGroup;

	public FavoritesScreen(
			@NonNull CarContext carContext,
			@NonNull Action settingsAction,
			@NonNull SurfaceRenderer surfaceRenderer,
			@Nullable FavoriteGroup group) {
		super(carContext);
		this.settingsAction = settingsAction;
		this.surfaceRenderer = surfaceRenderer;
		selectedGroup = group;
		getLifecycle().addObserver(new DefaultLifecycleObserver() {
			@Override
			public void onDestroy(@NonNull LifecycleOwner owner) {
				DefaultLifecycleObserver.super.onDestroy(owner);
				getApp().getOsmandMap().getMapLayers().getFavouritesLayer().setCustomMapObjects(null);
				getApp().getOsmandMap().getMapView().backToLocation();
			}
		});
	}

	@NonNull
	@Override
	public Template onGetTemplate() {
		ItemList.Builder listBuilder = new ItemList.Builder();
		setupFavorites(listBuilder);
		return new PlaceListNavigationTemplate.Builder()
				.setItemList(listBuilder.build())
				.setTitle(getApp().getString(R.string.shared_string_favorites))
				.setActionStrip(new ActionStrip.Builder().addAction(createSearchAction()).build())
				.setHeaderAction(Action.BACK)
				.build();
	}

	@Override
	protected int getConstraintLimitType() {
		return ConstraintManager.CONTENT_LIMIT_TYPE_PLACE_LIST;
	}

	private void setupFavorites(ItemList.Builder listBuilder) {
		LatLon location = getApp().getSettings().getLastKnownMapLocation();
		List<FavouritePoint> favoritesPoints = getFavorites();
		int favoritesPointsSize = favoritesPoints.size();
		List<FavouritePoint> limitedFavoritesPoints = favoritesPoints.subList(0, Math.min(favoritesPointsSize, getContentLimit() - 1));
		getApp().getOsmandMap().getMapLayers().getFavouritesLayer().setCustomMapObjects(limitedFavoritesPoints);
		QuadRect mapRect = new QuadRect();
		extendRectToContainPoint(mapRect, location.getLongitude(), location.getLatitude());
		for (FavouritePoint point : limitedFavoritesPoints) {
			double longitude = point.getLongitude();
			double latitude = point.getLatitude();
			extendRectToContainPoint(mapRect, longitude, latitude);
			String title = point.getDisplayName(getApp());
			int color = getApp().getFavoritesHelper().getColorWithCategory(point, ContextCompat.getColor(getApp(), R.color.color_favorite));
			CarIcon icon = new CarIcon.Builder(IconCompat.createWithBitmap(
					AndroidUtils.drawableToBitmap(PointImageDrawable.getFromFavorite(getApp(), color, false, point)))).build();
			String description = point.getSpecialPointType() != null ? point.getDescription() : point.getCategory();
			double dist = MapUtils.getDistance(point.getLatitude(), point.getLongitude(),
					location.getLatitude(), location.getLongitude());
			SpannableString address = new SpannableString(Algorithms.isEmpty(description) ? " " : "  • " + description);
			DistanceSpan distanceSpan = DistanceSpan.create(TripHelper.getDistance(getApp(), dist));
			address.setSpan(distanceSpan, 0, 1, SPAN_INCLUSIVE_INCLUSIVE);
			listBuilder.addItem(new Row.Builder()
					.setTitle(title)
					.setImage(icon)
					.addText(address)
					.setOnClickListener(() -> onClickFavorite(point))
					.setMetadata(new Metadata.Builder().setPlace(new Place.Builder(
							CarLocation.create(point.getLatitude(), point.getLongitude())).build()).build())
					.build());
		}
		if (mapRect.left != 0.0 && mapRect.right != 0.0 && mapRect.top != 0.0 && mapRect.bottom != 0.0) {
			OsmandMapTileView mapView = getApp().getOsmandMap().getMapView();
			RotatedTileBox tileBox =mapView.getCurrentRotatedTileBox().copy();
			mapView.fitRectToMap(mapRect.left, mapRect.right, mapRect.top, mapRect.bottom, tileBox.getPixHeight(), tileBox.getPixHeight(), 0);
		}
	}

	private void extendRectToContainPoint(QuadRect mapRect, double longitude, double latitude) {
		if (mapRect.left == 0.0) {
			mapRect.left = longitude;
		} else {
			mapRect.left = Math.min(mapRect.left, longitude);
		}
		mapRect.right = Math.max(mapRect.right, longitude);
		if (mapRect.bottom == 0.0) {
			mapRect.bottom = latitude;
		} else {
			mapRect.bottom = Math.min(mapRect.bottom, latitude);
		}
		mapRect.top = Math.max(mapRect.top, latitude);
	}

	private void onClickFavorite(@NonNull FavouritePoint point) {
		SearchResult result = new SearchResult();
		result.location = new LatLon(point.getLatitude(), point.getLongitude());
		result.objectType = ObjectType.FAVORITE;
		result.object = point;
		result.localeName = point.getAddress();
		openRoutePreview(settingsAction, surfaceRenderer, result);
	}

	@NonNull
	private List<FavouritePoint> getFavorites() {
		ArrayList<FavouritePoint> filteredFavorites = new ArrayList<>();
		if (selectedGroup == null) {
			filteredFavorites.addAll(getApp().getFavoritesHelper().getFavouritePoints());
		} else {
			filteredFavorites.addAll(selectedGroup.getPoints());
		}
		Collections.sort(filteredFavorites, (left, right) -> Long.compare(right.getTimestamp(), left.getTimestamp()));
		return filteredFavorites;
	}

	private void onRouteSelected(@NonNull SearchResult sr) {
		getApp().getOsmandMap().getMapLayers().getMapControlsLayer().startNavigation();
		NavigationSession session = getApp().getCarNavigationSession();
		if (session != null && session.hasStarted()) {
			session.startNavigation();
		}
	}

}
