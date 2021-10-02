package net.osmand.plus.auto;

import android.text.SpannableString;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.DurationSpan;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import androidx.car.app.navigation.model.RoutePreviewNavigationTemplate;

import net.osmand.plus.R;
import net.osmand.search.core.SearchResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * The route preview screen for the app.
 */
public final class RoutePreviewScreen extends Screen {
	private static final String TAG = "NavigationDemo";

	@NonNull
	private final Action settingsAction;
	@NonNull
	private final SurfaceRenderer surfaceRenderer;
	@NonNull
	private final SearchResult searchResult;
	@NonNull
	private final List<Row> routeRows;

	int lastSelectedIndex = -1;

	public RoutePreviewScreen(@NonNull CarContext carContext, @NonNull Action settingsAction,
			@NonNull SurfaceRenderer surfaceRenderer, @NonNull SearchResult searchResult) {
		super(carContext);
		this.settingsAction = settingsAction;
		this.surfaceRenderer = surfaceRenderer;
		this.searchResult = searchResult;

		routeRows = new ArrayList<>();
		SpannableString firstRoute = new SpannableString("   \u00b7 Shortest route");
		firstRoute.setSpan(DurationSpan.create(TimeUnit.HOURS.toSeconds(26)), 0, 1, 0);
		SpannableString secondRoute = new SpannableString("   \u00b7 Less busy");
		secondRoute.setSpan(DurationSpan.create(TimeUnit.HOURS.toSeconds(24)), 0, 1, 0);
		SpannableString thirdRoute = new SpannableString("   \u00b7 HOV friendly");
		thirdRoute.setSpan(DurationSpan.create(TimeUnit.MINUTES.toSeconds(867)), 0, 1, 0);

		routeRows.add(new Row.Builder().setTitle(firstRoute).addText("Via NE 8th Street").build());
		routeRows.add(new Row.Builder().setTitle(secondRoute).addText("Via NE 1st Ave").build());
		routeRows.add(new Row.Builder().setTitle(thirdRoute).addText("Via NE 4th Street").build());
	}

	@NonNull
	@Override
	public Template onGetTemplate() {
		Log.i(TAG, "In RoutePreviewScreen.onGetTemplate()");
		onRouteSelected(0);

		ItemList.Builder listBuilder = new ItemList.Builder();
		listBuilder
				.setOnSelectedListener(this::onRouteSelected)
				.setOnItemsVisibilityChangedListener(this::onRoutesVisible);
		for (Row row : routeRows) {
			listBuilder.addItem(row);
		}
		return new RoutePreviewNavigationTemplate.Builder()
				.setItemList(listBuilder.build())
				.setTitle(getCarContext().getString(R.string.current_route))
				.setActionStrip(new ActionStrip.Builder().addAction(settingsAction).build())
				.setHeaderAction(Action.BACK)
				.setNavigateAction(
						new Action.Builder()
								.setTitle("Continue to route")
								.setOnClickListener(this::onNavigate)
								.build())
				.build();
	}

	private void onRouteSelected(int index) {
		lastSelectedIndex = index;

	}

	private void onRoutesVisible(int startIndex, int endIndex) {
	}

	private void onNavigate() {
		setResult(lastSelectedIndex);
		finish();
	}
}
