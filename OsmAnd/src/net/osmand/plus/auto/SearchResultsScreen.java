package net.osmand.plus.auto;

import static androidx.car.app.constraints.ConstraintManager.CONTENT_LIMIT_TYPE_PLACE_LIST;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.constraints.ConstraintManager;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.Template;
import androidx.car.app.navigation.model.PlaceListNavigationTemplate;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import net.osmand.plus.AppInitializer;
import net.osmand.plus.AppInitializer.AppInitializeListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.auto.SearchHelper.SearchHelperListener;
import net.osmand.search.core.ObjectType;
import net.osmand.search.core.SearchPhrase;
import net.osmand.search.core.SearchResult;
import net.osmand.search.core.SearchWord;
import net.osmand.util.Algorithms;

import java.util.List;

/**
 * Screen for showing a list of places from a search.
 */
public final class SearchResultsScreen extends Screen implements DefaultLifecycleObserver,
		AppInitializeListener, SearchHelperListener {

	private static final int CONTENT_LIMIT = 12;

	private final SearchHelper searchHelper;

	@NonNull
	private final Action settingsAction;
	@NonNull
	private final SurfaceRenderer surfaceRenderer;
	@NonNull
	private final String searchText;

	private ItemList itemList;
	private boolean loading;
	private boolean destroyed;
	private boolean showResult;

	public SearchResultsScreen(@NonNull CarContext carContext, @NonNull Action settingsAction,
	                           @NonNull SurfaceRenderer surfaceRenderer, @NonNull String searchText) {
		super(carContext);
		ConstraintManager manager = carContext.getCarService(ConstraintManager.class);
		int contentLimit = Math.min(CONTENT_LIMIT, manager.getContentLimit(CONTENT_LIMIT_TYPE_PLACE_LIST));
		this.searchHelper = new SearchHelper(getApp(), false, contentLimit, 2, 5, true);
		this.settingsAction = settingsAction;
		this.surfaceRenderer = surfaceRenderer;
		this.searchText = searchText;

		this.loading = getApp().isApplicationInitializing();
		getLifecycle().addObserver(this);
		getApp().getAppInitializer().addListener(this);
		searchHelper.setListener(this);
		searchHelper.setupSearchSettings(true);
		if (!loading) {
			if (!Algorithms.isEmpty(searchText)) {
				searchHelper.runSearch(searchText);
			}
		}
	}

	@NonNull
	public OsmandApplication getApp() {
		return (OsmandApplication) getCarContext().getApplicationContext();
	}

	@NonNull
	@Override
	public Template onGetTemplate() {
		PlaceListNavigationTemplate.Builder builder = new PlaceListNavigationTemplate.Builder();
		builder.setTitle(getApp().getString(R.string.search_title, searchText))
				.setActionStrip(new ActionStrip.Builder().addAction(settingsAction).build())
				.setHeaderAction(Action.BACK);
		if (loading || searchHelper.isSearching()) {
			builder.setLoading(true);
		} else {
			builder.setLoading(false);
			if (itemList != null) {
				builder.setItemList(itemList);
			}
		}
		return builder.build();
	}

	@Override
	public void onDestroy(@NonNull LifecycleOwner owner) {
		getApp().getAppInitializer().removeListener(this);
		getLifecycle().removeObserver(this);
		destroyed = true;
	}

	@Override
	public void onFinish(@NonNull AppInitializer init) {
		loading = false;
		if (!destroyed) {
			if (!Algorithms.isEmpty(searchText)) {
				searchHelper.runSearch(searchText);
			} else {
				invalidate();
			}
		}
	}

	@Override
	public void onClickSearchResult(@NonNull SearchResult sr) {
		if (sr.objectType == ObjectType.POI
				|| sr.objectType == ObjectType.LOCATION
				|| sr.objectType == ObjectType.HOUSE
				|| sr.objectType == ObjectType.FAVORITE
				|| sr.objectType == ObjectType.RECENT_OBJ
				|| sr.objectType == ObjectType.WPT
				|| sr.objectType == ObjectType.STREET_INTERSECTION
				|| sr.objectType == ObjectType.GPX_TRACK) {

			showResult(sr);
		} else {
			searchHelper.completeQueryWithObject(sr);
			invalidate();
		}
	}

	private void showResult(SearchResult sr) {
		showResult = false;
		getScreenManager().pushForResult(new RoutePreviewScreen(getCarContext(), settingsAction, surfaceRenderer, sr),
				obj -> {
					if (obj != null) {
						onRouteSelected(sr);
					}
				});
	}

	@Override
	public void onClickSearchMore() {
		invalidate();
	}

	@Override
	public void onSearchDone(@NonNull SearchPhrase phrase, @Nullable List<SearchResult> searchResults,
							 @Nullable ItemList itemList, int resultsCount) {
		SearchWord lastSelectedWord = phrase.getLastSelectedWord();
		if (showResult && resultsCount == 0 && lastSelectedWord != null) {
			showResult(lastSelectedWord.getResult());
		} else {
			if (resultsCount > 0) {
				showResult = false;
			}
			this.itemList = itemList;
			invalidate();
		}
	}

	private void onRouteSelected(@NonNull SearchResult sr) {
		getApp().getOsmandMap().getMapLayers().getMapControlsLayer().startNavigation();
		finish();
	}
}
