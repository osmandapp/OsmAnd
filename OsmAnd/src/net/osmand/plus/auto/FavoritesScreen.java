package net.osmand.plus.auto;

import android.text.SpannableString;

import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.views.PointImageDrawable;
import net.osmand.search.core.ObjectType;
import net.osmand.search.core.SearchResult;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
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

import static android.text.Spanned.SPAN_INCLUSIVE_INCLUSIVE;

/**
 * Screen for showing a list of favorite places.
 */
public final class FavoritesScreen extends Screen {
	private static final String TAG = "NavigationDemo";

	@NonNull
	private final Action settingsAction;
	@NonNull
	private final SurfaceRenderer surfaceRenderer;

	public FavoritesScreen(
			@NonNull CarContext carContext,
			@NonNull Action settingsAction,
			@NonNull SurfaceRenderer surfaceRenderer) {
		super(carContext);
		this.settingsAction = settingsAction;
		this.surfaceRenderer = surfaceRenderer;
	}

	@NonNull
	@Override
	public Template onGetTemplate() {
		ItemList.Builder listBuilder = new ItemList.Builder();
		OsmandApplication app = getApp();
		LatLon location = app.getSettings().getLastKnownMapLocation();
		for (FavouritePoint point : getFavorites()) {
			String title = point.getDisplayName(app);
			int color = app.getFavoritesHelper().getColorWithCategory(point, ContextCompat.getColor(app, R.color.color_favorite));
			CarIcon icon = new CarIcon.Builder(IconCompat.createWithBitmap(
					AndroidUtils.drawableToBitmap(PointImageDrawable.getFromFavorite(app, color, false, point)))).build();
			String description = point.getSpecialPointType() != null ? point.getDescription() : point.getCategory();
			double dist = MapUtils.getDistance(point.getLatitude(), point.getLongitude(),
					location.getLatitude(), location.getLongitude());
			SpannableString address = new SpannableString(Algorithms.isEmpty(description) ? " " : "  • " + description);
			DistanceSpan distanceSpan = DistanceSpan.create(TripHelper.getDistance(app, dist));
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

		return new PlaceListNavigationTemplate.Builder()
				.setItemList(listBuilder.build())
				.setTitle(app.getString(R.string.shared_string_favorites))
				.setActionStrip(new ActionStrip.Builder().addAction(settingsAction).build())
				.setHeaderAction(Action.BACK)
				.build();
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
		List<FavouritePoint> favouritePoints = getApp().getFavoritesHelper().getFavouritePoints();
		Collections.sort(favouritePoints, (left, right) -> Long.compare(right.getTimestamp(), left.getTimestamp()));
		return favouritePoints;
	}

	private void onRouteSelected(@NonNull SearchResult sr) {
		getApp().getOsmandMap().getMapLayers().getMapControlsLayer().startNavigation();
		finish();
	}

	@NonNull
	private OsmandApplication getApp() {
		return ((OsmandApplication) getCarContext().getApplicationContext());
	}
}
