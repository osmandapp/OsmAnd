package net.osmand.plus.auto;

import static android.text.Spanned.SPAN_INCLUSIVE_INCLUSIVE;

import android.text.SpannableString;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.CarContext;
import androidx.car.app.OnScreenResultListener;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.CarLocation;
import androidx.car.app.model.Distance;
import androidx.car.app.model.DistanceSpan;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.Metadata;
import androidx.car.app.model.OnClickListener;
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
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.util.MapUtils;

import java.util.List;

/**
 * Screen for showing a list of favorite places.
 */
public final class FavoritesScreen extends Screen {
	private static final String TAG = "NavigationDemo";

	@NonNull
	private final Action mSettingsAction;
	@NonNull
	private final SurfaceRenderer mSurfaceRenderer;

	public FavoritesScreen(
			@NonNull CarContext carContext,
			@NonNull Action settingsAction,
			@NonNull SurfaceRenderer surfaceRenderer) {
		super(carContext);
		mSettingsAction = settingsAction;
		mSurfaceRenderer = surfaceRenderer;
	}

	@NonNull
	@Override
	public Template onGetTemplate() {
		Log.i(TAG, "In FavoritesScreen.onGetTemplate()");
		ItemList.Builder listBuilder = new ItemList.Builder();
		OsmandApplication app = (OsmandApplication) getCarContext().getApplicationContext();
		LatLon location = app.getSettings().getLastKnownMapLocation();
		for (FavouritePoint point : getFavorites()) {
			String title = point.getDisplayName(app);
			String description;
			CarIcon icon;
			if (point.getSpecialPointType() != null) {
				int iconColor = app.getSettings().isLightContent()
						? R.color.icon_color_default_light : R.color.icon_color_default_dark;
				icon = new CarIcon.Builder(IconCompat.createWithResource(
						app, point.getSpecialPointType().getIconId(app)).setTint(ContextCompat.getColor(app, iconColor))).build();
				description = point.getDescription();
			} else {
				if (point.getCategory().isEmpty()) {
					description = app.getString(R.string.shared_string_favorites);
				} else {
					description = point.getCategory();
				}
				int color = app.getFavorites().getColorWithCategory(point, ContextCompat.getColor(app, R.color.color_favorite));
				icon = new CarIcon.Builder(IconCompat.createWithResource(
						app, R.drawable.ic_action_favorite).setTint(color)).build();
			}
			if (description == null) {
				description = "";
			}
			double dist = MapUtils.getDistance(point.getLatitude(), point.getLongitude(),
					location.getLatitude(), location.getLongitude());
			SpannableString address = new SpannableString(" ");
			DistanceSpan distanceSpan = DistanceSpan.create(Distance.create(dist / 1000.0, Distance.UNIT_KILOMETERS_P1));
			address.setSpan(distanceSpan, 0, 1, SPAN_INCLUSIVE_INCLUSIVE);
			listBuilder.addItem(
					new Row.Builder()
							.setTitle(title)
							.setImage(icon)
							.addText(address)
							.setOnClickListener(() -> onClickFavorite(point))
							.setMetadata(new Metadata.Builder()
									.setPlace(new Place.Builder(CarLocation.create(
											point.getLatitude(), point.getLongitude())).build())
									.build())
							.build());
		}

		return new PlaceListNavigationTemplate.Builder()
				.setItemList(listBuilder.build())
				.setTitle(app.getString(R.string.app_name))
				.setActionStrip(new ActionStrip.Builder().addAction(mSettingsAction).build())
				.setHeaderAction(Action.BACK)
				.build();
	}

	private void onClickFavorite(@NonNull FavouritePoint point) {
		OsmandMapTileView mapView = mSurfaceRenderer.getMapView();
		if (mapView != null) {
			mapView.setLatLon(point.getLatitude(), point.getLongitude(), true);
			finish();
		}
	}

	@NonNull
	private List<FavouritePoint> getFavorites() {
		OsmandApplication app = (OsmandApplication) getCarContext().getApplicationContext();
		return app.getFavorites().getFavouritePoints();
	}
}
