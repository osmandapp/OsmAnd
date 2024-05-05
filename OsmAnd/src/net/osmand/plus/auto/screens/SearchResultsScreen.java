package net.osmand.plus.auto.screens;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.CarContext;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.Template;
import androidx.car.app.navigation.model.PlaceListNavigationTemplate;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import net.osmand.plus.AppInitializer;
import net.osmand.plus.AppInitializeListener;
import net.osmand.plus.R;
import net.osmand.search.core.ObjectType;
import net.osmand.search.core.SearchPhrase;
import net.osmand.search.core.SearchResult;
import net.osmand.search.core.SearchWord;
import net.osmand.util.Algorithms;

import java.util.List;

/**
 * Screen for showing a list of places from a search.
 */
public final class SearchResultsScreen extends BaseSearchScreen implements DefaultLifecycleObserver,
		AppInitializeListener {


	@NonNull
	private final Action settingsAction;
	@NonNull
	private final String searchText;

	private ItemList itemList;
	private boolean loading;
	private boolean destroyed;
	private boolean showResult;

	public SearchResultsScreen(@NonNull CarContext carContext, @NonNull Action settingsAction,
	                           @NonNull String searchText) {
		super(carContext);
		this.settingsAction = settingsAction;
		this.searchText = searchText;

		this.loading = getApp().isApplicationInitializing();
		getLifecycle().addObserver(this);
		getApp().getAppInitializer().addListener(this);
		if (!loading) {
			if (!Algorithms.isEmpty(searchText)) {
				getSearchHelper().runSearch(searchText);
			}
		}
	}

	@NonNull
	@Override
	public Template onGetTemplate() {
		PlaceListNavigationTemplate.Builder builder = new PlaceListNavigationTemplate.Builder();
		builder.setTitle(getApp().getString(R.string.search_title, searchText))
				.setActionStrip(new ActionStrip.Builder().addAction(settingsAction).build())
				.setHeaderAction(Action.BACK);
		if (loading || getSearchHelper().isSearching()) {
			builder.setLoading(true);
		} else {
			builder.setLoading(false);
			if (itemList != null) {
				builder.setItemList(itemList);
			} else {
				builder.setItemList(new ItemList.Builder().setNoItemsMessage(getApp().getString(R.string.location_not_found)).build());
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
				getSearchHelper().runSearch(searchText);
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
			getSearchHelper().completeQueryWithObject(sr);
			invalidate();
		}
	}

	private void showResult(SearchResult sr) {
		showResult = false;
		openRoutePreview(settingsAction, sr);
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
}
