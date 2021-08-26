package net.osmand.plus.auto;

import static android.text.Spanned.SPAN_INCLUSIVE_INCLUSIVE;

import android.text.SpannableString;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.CarLocation;
import androidx.car.app.model.Distance;
import androidx.car.app.model.DistanceSpan;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.Metadata;
import androidx.car.app.model.Place;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import androidx.car.app.navigation.model.PlaceListNavigationTemplate;

import net.osmand.data.FavouritePoint;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;

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
		mSurfaceRenderer.updateMarkerVisibility(
				/* showMarkers=*/ false, /* numMarkers=*/ 0, /* activeMarker=*/ -1);
		ItemList.Builder listBuilder = new ItemList.Builder();

		for (FavouritePoint point : getFavorites()) {
			SpannableString address = new SpannableString("   \u00b7 " + point.getAddress());
			DistanceSpan distanceSpan =
					DistanceSpan.create(
							Distance.create(/* displayDistance= */ 1, Distance.UNIT_KILOMETERS_P1));
			address.setSpan(distanceSpan, 0, 1, SPAN_INCLUSIVE_INCLUSIVE);
			listBuilder.addItem(
					new Row.Builder()
							.setTitle(point.getName())
							.addText(address)
							.setOnClickListener(this::onClickFavorite)
							.setMetadata(
									new Metadata.Builder()
											.setPlace(
													new Place.Builder(CarLocation.create(1, 1))
															.build())
											.build())
							.build());
		}

		return new PlaceListNavigationTemplate.Builder()
				.setItemList(listBuilder.build())
				.setTitle(getCarContext().getString(R.string.app_name))
				.setActionStrip(new ActionStrip.Builder().addAction(mSettingsAction).build())
				.setHeaderAction(Action.BACK)
				.build();
	}

	private void onClickFavorite() {
		getScreenManager()
				.pushForResult(
						new RoutePreviewScreen(getCarContext(), mSettingsAction, mSurfaceRenderer),
						this::onRoutePreviewResult);
	}

	private void onRoutePreviewResult(@Nullable Object previewResult) {
		int previewIndex = previewResult == null ? -1 : (int) previewResult;
		if (previewIndex < 0) {
			return;
		}
		// Start the same demo instructions. More will be added in the future.
		//setResult(DemoScripts.getNavigateHome(getCarContext()));
		finish();
	}

	@NonNull
	private List<FavouritePoint> getFavorites() {
		OsmandApplication app = (OsmandApplication) getCarContext().getApplicationContext();
		return app.getFavorites().getFavouritePoints();
	}
}
