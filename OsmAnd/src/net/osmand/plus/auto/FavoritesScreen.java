package net.osmand.plus.auto;

import static android.text.Spanned.SPAN_INCLUSIVE_INCLUSIVE;

import android.text.SpannableString;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
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

import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.myplaces.favorites.FavoriteGroup;
import net.osmand.plus.utils.AndroidUtils;
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
public final class FavoritesScreen extends Screen {
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
	}

	@NonNull
	@Override
	public Template onGetTemplate() {
		ItemList.Builder listBuilder = new ItemList.Builder();
		setupFavorites(listBuilder);
		return new PlaceListNavigationTemplate.Builder()
				.setItemList(listBuilder.build())
				.setTitle(getApp().getString(R.string.shared_string_favorites))
				.setActionStrip(new ActionStrip.Builder().addAction(settingsAction).build())
				.setHeaderAction(Action.BACK)
				.build();
	}

	private void setupFavorites(ItemList.Builder listBuilder) {
		LatLon location = getApp().getSettings().getLastKnownMapLocation();
		for (FavouritePoint point : getFavorites()) {
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
	}

	private void onClickFavorite(@NonNull FavouritePoint point) {
		SearchResult result = new SearchResult();
		result.location = new LatLon(point.getLatitude(), point.getLongitude());
		result.objectType = ObjectType.FAVORITE;
		result.object = point;
		result.localeName = point.getAddress();
		getScreenManager().pushForResult(new RoutePreviewScreen(getCarContext(), settingsAction, surfaceRenderer, result),
				obj -> {
					if (obj != null) {
						onRouteSelected(result);
					}
				});
		finish();
	}

	@NonNull
	private List<FavouritePoint> getFavorites() {
		ArrayList<FavouritePoint> filteredFavorites = new ArrayList<>();
		if (selectedGroup == null) {
			filteredFavorites.addAll(getApp().getFavoritesHelper().getFavouritePoints());
			Collections.sort(filteredFavorites, (left, right) -> Long.compare(right.getTimestamp(), left.getTimestamp()));
		} else {
			for (FavouritePoint point : getApp().getFavoritesHelper().getFavouritePoints()) {
				if (getApp().getFavoritesHelper().getGroup(point) == selectedGroup) {
					filteredFavorites.add(point);
				}
			}
		}
		return filteredFavorites;
	}

	private void onRouteSelected(@NonNull SearchResult sr) {
		getApp().getOsmandMap().getMapLayers().getMapControlsLayer().startNavigation();
		NavigationSession session = getApp().getCarNavigationSession();
		if (session != null && session.hasStarted()) {
			session.startNavigation();
		}
	}

	@NonNull
	private OsmandApplication getApp() {
		return ((OsmandApplication) getCarContext().getApplicationContext());
	}
}
