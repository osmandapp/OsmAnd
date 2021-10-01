package net.osmand.plus.auto;

import static net.osmand.search.core.SearchCoreFactory.MAX_DEFAULT_SEARCH_RADIUS;
import static net.osmand.search.core.SearchCoreFactory.SEARCH_AMENITY_TYPE_PRIORITY;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.Row;
import androidx.car.app.model.SearchTemplate;
import androidx.car.app.model.SearchTemplate.SearchCallback;
import androidx.car.app.model.Template;
import androidx.core.graphics.drawable.IconCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import net.osmand.AndroidUtils;
import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.PoiType;
import net.osmand.plus.AppInitializer;
import net.osmand.plus.AppInitializer.AppInitializeListener;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.TargetPointsHelper.TargetPoint;
import net.osmand.plus.helpers.SearchHistoryHelper;
import net.osmand.plus.mapmarkers.MapMarker;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.search.QuickSearchHelper.SearchHistoryAPI;
import net.osmand.plus.search.listitems.QuickSearchListItem;
import net.osmand.search.SearchUICore;
import net.osmand.search.SearchUICore.SearchResultCollection;
import net.osmand.search.core.ObjectType;
import net.osmand.search.core.SearchPhrase;
import net.osmand.search.core.SearchResult;
import net.osmand.search.core.SearchSettings;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;

public final class SearchScreen extends Screen implements DefaultLifecycleObserver, AppInitializeListener {

	private static final Log LOG = PlatformUtil.getLog(SearchScreen.class);
	private static final double DISTANCE_THRESHOLD = 70000; // 70km
	private static final int SEARCH_TOTAL_LIMIT = 50;
	private static final int MAP_MARKERS_LIMIT = 3;

	private final OsmandApplication app;
	private final SearchUICore searchUICore;

	@NonNull
	private final Action mSettingsAction;

	@NonNull
	private final SurfaceRenderer mSurfaceRenderer;

	private ItemList itemList = withNoResults(new ItemList.Builder()).build();

	@Nullable
	private String searchQuery;
	private String searchText;
	private String searchHint;
	private int searchRadiusLevel = 1;
	private boolean loading;
	private boolean searching;
	private List<SearchResult> searchResults;
	private List<SearchResult> recentResults;
	private boolean destroyed;
	private boolean useMapCenter;
	private LatLon searchLocation;

	public SearchScreen(@NonNull CarContext carContext, @NonNull Action settingsAction,
						@NonNull SurfaceRenderer surfaceRenderer) {
		super(carContext);
		app = (OsmandApplication) carContext.getApplicationContext();
		searchUICore = app.getSearchUICore().getCore();
		mSettingsAction = settingsAction;
		mSurfaceRenderer = surfaceRenderer;

		getLifecycle().addObserver(this);
		app.getAppInitializer().addListener(this);
		setupSearchSettings(true);
		reloadHistory();
	}

	@Override
	public void onDestroy(@NonNull LifecycleOwner owner) {
		destroyed = true;
	}

	@NonNull
	@Override
	public Template onGetTemplate() {
		SearchTemplate.Builder builder = new SearchTemplate.Builder(
				new SearchCallback() {
					@Override
					public void onSearchTextChanged(@NonNull String searchText) {
						SearchScreen.this.searchText = searchText;
						searchRadiusLevel = 1;
						doSearch(searchText);
					}

					@Override
					public void onSearchSubmitted(@NonNull String searchTerm) {
						// When the user presses the search key use the top item in the list
						// as the result and simulate as if the user had pressed that.
						List<SearchResult> searchResults = SearchScreen.this.searchResults;
						if (!Algorithms.isEmpty(searchResults)) {
							onClickSearch(searchResults.get(0));
						}
					}
				});

		builder.setHeaderAction(Action.BACK)
				.setShowKeyboardByDefault(false)
				.setInitialSearchText(searchQuery == null ? "" : searchQuery);
		if (!Algorithms.isEmpty(searchHint)) {
			builder.setSearchHint(searchHint);
		}
		if (loading || searching) {
			builder.setLoading(true);
		} else if (itemList != null) {
			builder.setLoading(false);
			builder.setItemList(itemList);
		}

		return builder.build();
	}

	void doSearch(String searchText) {
		this.searchQuery = searchText;
		if (app.isApplicationInitializing() && !searchText.isEmpty()) {
			searching = true;
		} else {
			if (searchText.isEmpty()) {
				showRecents();
			} else {
				runSearch();
			}
		}
		invalidate();
	}

	private void runSearch() {
		searching = true;
		SearchSettings searchSettings = setupSearchSettings(false);
		SearchUICore core = app.getSearchUICore().getCore();
		core.setOnResultsComplete(() -> {
			ItemList.Builder itemList = new ItemList.Builder();
			SearchResultCollection resultCollection = core.getCurrentSearchResult();
			int count = 0;
			List<SearchResult> searchResults = new ArrayList<>();
			for (SearchResult r : resultCollection.getCurrentSearchResults()) {
				String name = QuickSearchListItem.getName(app, r);
				if (Algorithms.isEmpty(name)) {
					continue;
				}
				Drawable icon = QuickSearchListItem.getIcon(app, r);
				String typeName = QuickSearchListItem.getTypeName(app, r);
				itemList.setNoItemsMessage(getCarContext().getString(R.string.search_nothing_found));
				Row.Builder builder = buildSearchRow(searchSettings.getOriginalLocation(), r.location, name, icon, typeName);
				builder.setOnClickListener(() -> onClickSearch(r));
				itemList.addItem(builder.build());
				searchResults.add(r);
				count++;
				if (count >= SEARCH_TOTAL_LIMIT) {
					break;
				}
			}
			SearchPhrase phrase = searchUICore.getPhrase();
			if (searchUICore.isSearchMoreAvailable(phrase)) {
				Row.Builder builder = new Row.Builder();
				builder.setTitle(app.getString(R.string.increase_search_radius));
				int minimalSearchRadius = searchUICore.getMinimalSearchRadius(phrase);
				if (minimalSearchRadius != Integer.MAX_VALUE) {
					double rd = OsmAndFormatter.calculateRoundedDist(minimalSearchRadius, app);
					builder.addText(app.getString(R.string.nothing_found_in_radius) + " "
							+ OsmAndFormatter.getFormattedDistance((float) rd, app, false));
				}
				builder.setOnClickListener(this::onClickSearchMore);
				itemList.addItem(builder.build());
			}
			app.runInUIThread(() -> {
				this.searchResults = searchResults;
				this.itemList = itemList.build();
				searching = false;
				invalidate();
			});
		});
		core.search(searchQuery, true, null, searchSettings);
	}

	private SearchSettings setupSearchSettings(boolean resetPhrase) {
		Location location = app.getLocationProvider().getLastKnownLocation();
		SearchUICore core = app.getSearchUICore().getCore();
		if (resetPhrase) {
			core.resetPhrase();
		}
		int radiusLevel = this.searchRadiusLevel;
		if (radiusLevel < 1) {
			radiusLevel = 1;
		} else if (radiusLevel > MAX_DEFAULT_SEARCH_RADIUS) {
			radiusLevel = MAX_DEFAULT_SEARCH_RADIUS;
		}
		LatLon searchLatLon;
		LatLon clt = app.getOsmandMap().getMapView().getCurrentRotatedTileBox().getCenterLatLon();
		searchLatLon = clt;
		if (location != null) {
			double d = MapUtils.getDistance(clt, location.getLatitude(), location.getLongitude());
			if (d < DISTANCE_THRESHOLD) {
				searchLatLon = new LatLon(location.getLatitude(), location.getLongitude());
				useMapCenter = false;
			} else {
				useMapCenter = true;
			}
		} else {
			useMapCenter = true;
		}
		searchLocation = searchLatLon;
		String locale = app.getSettings().MAP_PREFERRED_LOCALE.get();
		boolean transliterate = app.getSettings().MAP_TRANSLITERATE_NAMES.get();
		SearchSettings searchSettings = new SearchSettings(core.getSearchSettings())
				.resetSearchTypes()
				.setRadiusLevel(radiusLevel)
				.setEmptyQueryAllowed(false)
				.setSortByName(false)
				.setLang(locale, transliterate)
				.setOriginalLocation(searchLatLon);

		updateSearchHint(searchSettings);
		return searchSettings;
	}

	private void updateSearchHint(@NonNull SearchSettings searchSettings) {
		Location location = app.getLocationProvider().getLastKnownLocation();
		LatLon searchLocation = searchSettings.getOriginalLocation();
		if (useMapCenter && location != null && searchLocation != null && Algorithms.isEmpty(searchText)) {
			double d = MapUtils.getDistance(searchLocation, location.getLatitude(), location.getLongitude());
			String dist = OsmAndFormatter.getFormattedDistance((float) d, app);
			searchHint = app.getString(R.string.dist_away_from_my_location, dist);
		} else {
			searchHint = app.getString(R.string.search_poi_category_hint);
		}
	}

	void onClickSearch(@NonNull SearchResult sr) {
		if (sr.objectType == ObjectType.POI
				|| sr.objectType == ObjectType.LOCATION
				|| sr.objectType == ObjectType.HOUSE
				|| sr.objectType == ObjectType.FAVORITE
				|| sr.objectType == ObjectType.RECENT_OBJ
				|| sr.objectType == ObjectType.WPT
				|| sr.objectType == ObjectType.STREET_INTERSECTION
				|| sr.objectType == ObjectType.GPX_TRACK) {

			getScreenManager().pushForResult(new RoutePreviewScreen(getCarContext(), mSettingsAction, mSurfaceRenderer),
					sr1 -> onRouteSelected(sr));
		} else {
			completeQueryWithObject(sr);
		}
	}

	void onClickSearchMore() {
		searchRadiusLevel++;
		runSearch();
		invalidate();
	}

	private void completeQueryWithObject(@NonNull SearchResult sr) {
		if (sr.object instanceof AbstractPoiType) {
			SearchHistoryHelper.getInstance(app).addNewItemToHistory((AbstractPoiType) sr.object);
			reloadHistory();
		} else if (sr.object instanceof PoiUIFilter) {
			SearchHistoryHelper.getInstance(app).addNewItemToHistory((PoiUIFilter) sr.object);
			reloadHistory();
		}
		if (sr.object instanceof PoiType && ((PoiType) sr.object).isAdditional()) {
			PoiType additional = (PoiType) sr.object;
			AbstractPoiType parent = additional.getParentType();
			if (parent != null) {
				PoiUIFilter custom = app.getPoiFilters().getFilterById(PoiUIFilter.STD_PREFIX + parent.getKeyName());
				if (custom != null) {
					custom.clearFilter();
					custom.updateTypesToAccept(parent);
					custom.setFilterByName(additional.getKeyName().replace('_', ':').toLowerCase());

					SearchPhrase phrase = searchUICore.getPhrase();
					sr = new SearchResult(phrase);
					sr.localeName = custom.getName();
					sr.object = custom;
					sr.priority = SEARCH_AMENITY_TYPE_PRIORITY;
					sr.priorityDistance = 0;
					sr.objectType = ObjectType.POI_TYPE;
				}
			}
		}
		searchUICore.selectSearchResult(sr);
		searchQuery = searchUICore.getPhrase().getText(true);
		SearchSettings settings = searchUICore.getSearchSettings();
		if (settings.getRadiusLevel() != 1) {
			searchUICore.updateSettings(settings.setRadiusLevel(1));
		}
		runSearch();
		invalidate();
	}

	private ItemList.Builder withNoResults(ItemList.Builder builder) {
		return builder.setNoItemsMessage(getCarContext().getString(R.string.search_nothing_found));
	}

	public void reloadHistory() {
		if (app.isApplicationInitializing()) {
			loading = true;
		} else {
			reloadHistoryInternal();
		}
	}

	private void reloadHistoryInternal() {
		if (!destroyed) {
			try {
				List<SearchResult> recentResults = new ArrayList<>();

				// Home / work
				FavouritesDbHelper favorites = app.getFavorites();
				FavouritePoint homePoint = favorites.getSpecialPoint(FavouritePoint.SpecialPointType.HOME);
				FavouritePoint workPoint = favorites.getSpecialPoint(FavouritePoint.SpecialPointType.WORK);
				if (homePoint != null) {
					SearchResult result = new SearchResult();
					result.location = new LatLon(homePoint.getLatitude(), homePoint.getLongitude());
					result.objectType = ObjectType.FAVORITE;
					result.object = homePoint;
					result.localeName = homePoint.getAddress();
					recentResults.add(result);
				}
				if (workPoint != null) {
					SearchResult result = new SearchResult();
					result.location = new LatLon(workPoint.getLatitude(), workPoint.getLongitude());
					result.objectType = ObjectType.FAVORITE;
					result.object = workPoint;
					result.localeName = workPoint.getAddress();
					recentResults.add(result);
				}

				// Previous route card
				TargetPointsHelper targetPointsHelper = app.getTargetPointsHelper();
				TargetPoint startPoint = targetPointsHelper.getPointToStartBackup();
				boolean myLocation = false;
				if (startPoint == null) {
					myLocation = true;
					startPoint = targetPointsHelper.getMyLocationToStart();
				}
				TargetPoint destinationPoint = targetPointsHelper.getPointToNavigateBackup();
				if (startPoint != null && destinationPoint != null) {
					StringBuilder startText = new StringBuilder(myLocation ? app.getText(R.string.my_location) : "");
					String startDescr = getPointName(startPoint);
					if (!Algorithms.isEmpty(startDescr)) {
						if (startText.length() > 0) {
							startText.append(" — ");
						}
						startText.append(startDescr);
					}
					String destDescr = getPointName(destinationPoint);
					SearchResult result = new SearchResult();
					result.location = new LatLon(destinationPoint.getLatitude(), destinationPoint.getLongitude());
					result.objectType = ObjectType.ROUTE;
					result.object = destinationPoint;
					result.localeName = destDescr;
					result.relatedObject = startPoint;
					result.localeRelatedObjectName = startText.toString();
					recentResults.add(result);
				}

				// Map markers
				List<MapMarker> mapMarkers = app.getMapMarkersHelper().getMapMarkers();
				int mapMarkersCount = 0;
				for (MapMarker marker : mapMarkers) {
					SearchResult result = new SearchResult();
					result.location = new LatLon(marker.getLatitude(), marker.getLongitude());
					result.objectType = ObjectType.MAP_MARKER;
					result.object = marker;
					result.localeName = marker.getName(app);
					recentResults.add(result);
					mapMarkersCount++;
					if (mapMarkersCount >= MAP_MARKERS_LIMIT) {
						break;
					}
				}

				// History
				SearchResultCollection res = searchUICore.shallowSearch(SearchHistoryAPI.class, "", null, false, false);
				recentResults.addAll(res.getCurrentSearchResults());
				this.recentResults = recentResults;
				if (!searching && Algorithms.isEmpty(searchQuery)) {
					showRecents();
					invalidate();
				}
			} catch (Exception e) {
				LOG.error(e.getMessage(), e);
				app.showToastMessage(e.getMessage());
			}
		}
	}

	private String getPointName(TargetPoint targetPoint) {
		String name = "";
		if (targetPoint != null) {
			PointDescription description = targetPoint.getOriginalPointDescription();
			if (description != null && !Algorithms.isEmpty(description.getName()) &&
					!description.getName().equals(app.getString(R.string.no_address_found))) {
				name = description.getName();
			} else {
				name = PointDescription.getLocationName(app, targetPoint.point.getLatitude(),
						targetPoint.point.getLongitude(), true).replace('\n', ' ');
			}
		}
		return name;
	}

	private void showRecents() {
		ItemList.Builder itemList = new ItemList.Builder();
		itemList.setNoItemsMessage(getCarContext().getString(R.string.search_nothing_found));
		if (Algorithms.isEmpty(recentResults)) {
			this.itemList = itemList.build();
			return;
		}
		int count = 0;
		for (SearchResult r : recentResults) {
			String name = QuickSearchListItem.getName(app, r);
			if (Algorithms.isEmpty(name)) {
				continue;
			}
			Drawable icon = QuickSearchListItem.getIcon(app, r);
			String typeName = QuickSearchListItem.getTypeName(app, r);
			Row.Builder builder = buildSearchRow(searchLocation, r.location, name, icon, typeName);
			builder.setOnClickListener(() -> onClickSearch(r));
			itemList.addItem(builder.build());
			count++;
			if (count >= SEARCH_TOTAL_LIMIT) {
				break;
			}
		}
		this.itemList = itemList.build();
	}

	private Row.Builder buildSearchRow(@Nullable LatLon searchLocation, @Nullable LatLon placeLocation,
									   @NonNull String name, @Nullable Drawable icon, @Nullable String typeName) {
		Row.Builder builder = new Row.Builder();
		if (icon != null) {
			builder.setImage(new CarIcon.Builder(IconCompat.createWithBitmap(AndroidUtils.drawableToBitmap(icon))).build());
		}
		builder.setTitle(name);
		if (name.equals(typeName)) {
			typeName = "";
		}
		if (placeLocation != null && searchLocation != null) {
			float dist = (float) MapUtils.getDistance(placeLocation, searchLocation);
			String distStr = OsmAndFormatter.getFormattedDistance(dist, app);
			if (!Algorithms.isEmpty(typeName)) {
				builder.addText(distStr + " • " + typeName);
			} else {
				builder.addText(distStr);
			}
		} else if (!Algorithms.isEmpty(typeName)) {
			builder.addText(typeName);
		}
		return builder;
	}

	@Override
	public void onStart(AppInitializer init) {

	}

	@Override
	public void onProgress(AppInitializer init, AppInitializer.InitEvents event) {

	}

	@Override
	public void onFinish(AppInitializer init) {
		loading = false;
		if (!destroyed) {
			reloadHistoryInternal();
			if (!Algorithms.isEmpty(searchQuery)) {
				runSearch();
			}
			invalidate();
		}
	}

	private void onRouteSelected(@NonNull SearchResult sr) {
		// TODO

		// Start the same demo instructions. More will be added in the future.
		//setResult(DemoScripts.getNavigateHome(getCarContext()));
		finish();
	}
}
