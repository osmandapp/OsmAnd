package net.osmand.plus.search;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.Location;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.osm.AbstractPoiType;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.LockableViewPager;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmAndLocationProvider.OsmAndCompassListener;
import net.osmand.plus.OsmAndLocationProvider.OsmAndLocationListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.SearchHistoryHelper;
import net.osmand.plus.helpers.SearchHistoryHelper.HistoryEntry;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.resources.RegionAddressRepository;
import net.osmand.search.SearchUICore;
import net.osmand.search.SearchUICore.SearchResultCollection;
import net.osmand.search.SearchUICore.SearchResultMatcher;
import net.osmand.search.core.ObjectType;
import net.osmand.search.core.SearchCoreAPI;
import net.osmand.search.core.SearchCoreFactory.SearchAmenityTypesAPI;
import net.osmand.search.core.SearchCoreFactory.SearchBaseAPI;
import net.osmand.search.core.SearchPhrase;
import net.osmand.search.core.SearchResult;
import net.osmand.search.core.SearchSettings;
import net.osmand.search.core.SearchWord;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class QuickSearchDialogFragment extends DialogFragment implements OsmAndCompassListener, OsmAndLocationListener {

	public static final String TAG = "QuickSearchDialogFragment";
	private static final String QUICK_SEARCH_QUERY_KEY = "quick_search_query_key";
	private Toolbar toolbar;
	private LockableViewPager viewPager;
	private SearchFragmentPagerAdapter pagerAdapter;
	private TabLayout tabLayout;
	private View tabToolbarView;
	private View tabsView;
	private View searchView;
	private View buttonToolbarView;
	private ImageView buttonToolbarImage;
	private TextView buttonToolbarText;
	private QuickSearchMainListFragment mainSearchFragment;
	private QuickSearchHistoryListFragment historySearchFragment;
	private QuickSearchCategoriesListFragment categoriesSearchFragment;

	private Toolbar toolbarEdit;
	private TextView titleEdit;

	private EditText searchEditText;
	private ProgressBar progressBar;
	private ImageButton clearButton;

	private SearchUICore searchUICore;
	private String searchQuery = "";
	private SearchResultCollection resultCollection;

	private net.osmand.Location location = null;
	private Float heading = null;
	private boolean useMapCenter;
	private boolean paused;

	public static final int SEARCH_FAVORITE_API_PRIORITY = 2;
	public static final int SEARCH_FAVORITE_OBJECT_PRIORITY = 10;
	public static final int SEARCH_WPT_API_PRIORITY = 2;
	public static final int SEARCH_WPT_OBJECT_PRIORITY = 10;
	public static final int SEARCH_HISTORY_API_PRIORITY = 3;
	public static final int SEARCH_HISTORY_OBJECT_PRIORITY = 10;
	private static final double DISTANCE_THRESHOLD = 70000; // 70km


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		boolean isLightTheme = getMyApplication().getSettings().OSMAND_THEME.get() == OsmandSettings.OSMAND_LIGHT_THEME;
		int themeId = isLightTheme ? R.style.OsmandLightTheme : R.style.OsmandDarkTheme;
		setStyle(STYLE_NO_FRAME, themeId);
	}

	@Override
	@SuppressLint("PrivateResource")
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		final MapActivity mapActivity = getMapActivity();
		final OsmandApplication app = getMyApplication();
		final View view = inflater.inflate(R.layout.search_dialog_fragment, container, false);

		if (savedInstanceState != null) {
			searchQuery = savedInstanceState.getString(QUICK_SEARCH_QUERY_KEY);
		}
		if (searchQuery == null) {
			searchQuery = getArguments().getString(QUICK_SEARCH_QUERY_KEY);
		}
		if (searchQuery == null)
			searchQuery = "";

		tabToolbarView = view.findViewById(R.id.tab_toolbar_layout);
		tabsView = view.findViewById(R.id.tabs_view);
		searchView = view.findViewById(R.id.search_view);

		buttonToolbarView = view.findViewById(R.id.button_toolbar_layout);
		buttonToolbarImage = (ImageView) view.findViewById(R.id.buttonToolbarImage);
		buttonToolbarImage.setImageDrawable(app.getIconsCache().getThemedIcon(R.drawable.ic_action_marker_dark));
		buttonToolbarText = (TextView) view.findViewById(R.id.buttonToolbarTitle);
		buttonToolbarText.setText(app.getString(R.string.show_on_map).toUpperCase());
		view.findViewById(R.id.buttonToolbar).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				OsmandSettings settings = app.getSettings();
				SearchPhrase searchPhrase = searchUICore.getPhrase();
				if (searchPhrase.isNoSelectedType() || searchPhrase.isLastWord(ObjectType.POI_TYPE)) {
					PoiUIFilter filter;
					if (searchPhrase.isNoSelectedType()) {
						filter = new PoiUIFilter(null, app, "");
					} else {
						AbstractPoiType abstractPoiType = (AbstractPoiType) searchPhrase.getLastSelectedWord().getResult().object;
						filter = new PoiUIFilter(abstractPoiType, app, "");
					}
					if (!Algorithms.isEmpty(searchPhrase.getUnknownSearchWord())) {
						filter.setFilterByName(searchPhrase.getUnknownSearchWord());
					}
					app.getPoiFilters().clearSelectedPoiFilters();
					app.getPoiFilters().addSelectedPoiFilter(filter);
					if (location != null) {
						settings.setMapLocationToShow(location.getLatitude(), location.getLongitude(), 15);
					}
					getMapActivity().setQuickSearchTopbarActive(searchPhrase.isNoSelectedType());
					MapActivity.launchMapActivityMoveToTop(getActivity());
					if (searchPhrase.isNoSelectedType()) {
						hide();
					} else {
						dismiss();
					}
				} else {
					SearchWord word = searchPhrase.getLastSelectedWord();
					if (word != null && word.getLocation() != null) {
						SearchResult searchResult = word.getResult();
						String name = QuickSearchListItem.getName(app, searchResult);
						String typeName = QuickSearchListItem.getTypeName(app, searchResult);
						PointDescription pointDescription = new PointDescription(PointDescription.POINT_TYPE_ADDRESS, typeName, name);
						getMyApplication().getSettings().setMapLocationToShow(
								searchResult.location.getLatitude(), searchResult.location.getLongitude(),
								searchResult.preferredZoom, pointDescription, true, searchResult.object);

						getMapActivity().setQuickSearchTopbarActive(true);
						MapActivity.launchMapActivityMoveToTop(getActivity());
						hide();
					}
				}
			}
		});

		toolbar = (Toolbar) view.findViewById(R.id.toolbar);
		toolbar.setNavigationIcon(app.getIconsCache().getThemedIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha));
		toolbar.setNavigationContentDescription(R.string.access_shared_string_navigate_up);
		toolbar.setNavigationOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

		toolbarEdit = (Toolbar) view.findViewById(R.id.toolbar_edit);
		toolbarEdit.setNavigationIcon(app.getIconsCache().getIcon(R.drawable.ic_action_remove_dark));
		toolbarEdit.setNavigationContentDescription(R.string.shared_string_cancel);
		toolbarEdit.setNavigationOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				enableSelectionMode(false, -1);
			}
		});

		titleEdit = (TextView) view.findViewById(R.id.titleEdit);
		view.findViewById(R.id.shareButton).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				List<HistoryEntry> historyEntries = new ArrayList<HistoryEntry>();
				List<QuickSearchListItem> selectedItems = historySearchFragment.getListAdapter().getSelectedItems();
				for (QuickSearchListItem searchListItem : selectedItems) {
					HistoryEntry historyEntry = (HistoryEntry) searchListItem.getSearchResult().object;
					historyEntries.add(historyEntry);
				}
				if (historyEntries.size() > 0) {
					shareHistory(historyEntries);
					enableSelectionMode(false, -1);
				}
			}
		});
		view.findViewById(R.id.deleteButton).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {

				new DialogFragment() {
					@NonNull
					@Override
					public Dialog onCreateDialog(Bundle savedInstanceState) {
						AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
						builder.setTitle(R.string.confirmation_to_delete_history_items)
								.setPositiveButton(R.string.shared_string_yes, new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										SearchHistoryHelper helper = SearchHistoryHelper.getInstance(app);
										List<QuickSearchListItem> selectedItems = historySearchFragment.getListAdapter().getSelectedItems();
										for (QuickSearchListItem searchListItem : selectedItems) {
											HistoryEntry historyEntry = (HistoryEntry) searchListItem.getSearchResult().object;
											helper.remove(historyEntry);
										}
										reloadHistory();
										enableSelectionMode(false, -1);
									}
								})
								.setNegativeButton(R.string.shared_string_no, null);
						return builder.create();
					}
				}.show(getChildFragmentManager(), "DeleteHistoryConfirmationFragment");
			}
		});

		viewPager = (LockableViewPager) view.findViewById(R.id.pager);
		pagerAdapter = new SearchFragmentPagerAdapter(getChildFragmentManager(), getResources());
		viewPager.setAdapter(pagerAdapter);

		tabLayout = (TabLayout) view.findViewById(R.id.tab_layout);
		tabLayout.setupWithViewPager(viewPager);

		searchEditText = (EditText) view.findViewById(R.id.searchEditText);
		searchEditText.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}

			@Override
			public void afterTextChanged(Editable s) {
				String newQueryText = s.toString();
				updateClearButtonAndHint();
				updateClearButtonVisibility(newQueryText.length() > 0);
				updateTabbarVisibility(newQueryText.length() == 0);
				if (!searchQuery.equalsIgnoreCase(newQueryText)) {
					searchQuery = newQueryText;
					runSearch();
				}
			}
		});
		
		progressBar = (ProgressBar) view.findViewById(R.id.searchProgressBar);
		clearButton = (ImageButton) view.findViewById(R.id.clearButton);
		clearButton.setImageDrawable(app.getIconsCache().getThemedIcon(R.drawable.ic_action_remove_dark));
		clearButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (searchEditText.getText().length() > 0) {
					String newText = searchUICore.getPhrase().getTextWithoutLastWord();
					searchEditText.setText(newText);
					searchEditText.setSelection(newText.length());
					SearchWord word = searchUICore.getPhrase().getLastSelectedWord();
					if (word != null && word.getResult() != null) {
						buttonToolbarText.setText(app.getString(R.string.show_something_on_map, word.getResult().localeName).toUpperCase());
					} else {
						buttonToolbarText.setText(app.getString(R.string.show_on_map).toUpperCase());
					}
				} else if (useMapCenter && location != null) {
					useMapCenter = false;
					updateUseMapCenterUI();
					startLocationUpdate();
					LatLon centerLatLon = new LatLon(location.getLatitude(), location.getLongitude());
					SearchSettings ss = searchUICore.getSearchSettings().setOriginalLocation(
							new LatLon(centerLatLon.getLatitude(), centerLatLon.getLongitude()));
					searchUICore.updateSettings(ss);
					updateClearButtonAndHint();
				}
			}
		});

		setupSearch(mapActivity);

		updateClearButtonAndHint();
		addMainSearchFragment();

		searchEditText.requestFocus();
		AndroidUtils.softKeyboardDelayed(searchEditText);

		return view;
	}

	public String getText() {
		return searchEditText.getText().toString();
	}

	public void show() {
		getMapActivity().setQuickSearchTopbarActive(false);
		if (useMapCenter) {
			LatLon mapCenter = getMapActivity().getMapView().getCurrentRotatedTileBox().getCenterLatLon();
			SearchSettings ss = searchUICore.getSearchSettings().setOriginalLocation(
					new LatLon(mapCenter.getLatitude(), mapCenter.getLongitude()));
			searchUICore.updateSettings(ss);
			updateLocationUI(mapCenter, null);
		}
		getDialog().show();
	}

	public void hide() {
		getDialog().hide();
	}

	public void closeSearch() {
		MapActivity mapActivity = getMapActivity();
		mapActivity.getMyApplication().getPoiFilters().clearSelectedPoiFilters();
		dismiss();
	}

	public void addMainSearchFragment() {
		FragmentManager childFragMan = getChildFragmentManager();
		FragmentTransaction childFragTrans = childFragMan.beginTransaction();
		mainSearchFragment = new QuickSearchMainListFragment();
		childFragTrans.add(R.id.search_view, mainSearchFragment);
		childFragTrans.addToBackStack("QuickSearchMainListFragment");
		childFragTrans.commit();
	}

	private void setupSearch(final MapActivity mapActivity) {

		final OsmandApplication app = mapActivity.getMyApplication();

		// Setup search core
		String locale = app.getSettings().MAP_PREFERRED_LOCALE.get();

		Collection<RegionAddressRepository> regionAddressRepositories = app.getResourceManager().getAddressRepositories();
		BinaryMapIndexReader[] binaryMapIndexReaderArray = new BinaryMapIndexReader[regionAddressRepositories.size()];
		int i = 0;
		for (RegionAddressRepository rep : regionAddressRepositories) {
			binaryMapIndexReaderArray[i++] = rep.getFile();
		}
		searchUICore = new SearchUICore(app.getPoiTypes(), locale, binaryMapIndexReaderArray);

		location = app.getLocationProvider().getLastKnownLocation();
		LatLon clt = mapActivity.getMapView().getCurrentRotatedTileBox().getCenterLatLon();
		LatLon centerLatLon = clt;
		searchEditText.setHint(R.string.search_poi_category_hint);
		if (location != null) {
			double d = MapUtils.getDistance(clt, location.getLatitude(), location.getLongitude());
			if (d < DISTANCE_THRESHOLD) {
				centerLatLon = new LatLon(location.getLatitude(), location.getLongitude());
			} else {
				useMapCenter = true;
			}
		}
		SearchSettings settings = searchUICore.getPhrase().getSettings().setOriginalLocation(
				new LatLon(centerLatLon.getLatitude(), centerLatLon.getLongitude()));
		settings = settings.setLang(locale);
		searchUICore.updateSettings(settings);
		searchUICore.setOnResultsComplete(new Runnable() {
			@Override
			public void run() {
				app.runInUIThread(new Runnable() {
					@Override
					public void run() {
						hideProgressBar();
						addMoreButton();
					}
				});
			}
		});

		// Register favorites search api
		searchUICore.registerAPI(new SearchBaseAPI() {

			@Override
			public boolean search(SearchPhrase phrase, SearchResultMatcher resultMatcher) {
				List<FavouritePoint> favList = getMyApplication().getFavorites().getFavouritePoints();
				for (FavouritePoint point : favList) {
					SearchResult sr = new SearchResult(phrase);
					sr.localeName = point.getName();
					sr.object = point;
					sr.priority = SEARCH_FAVORITE_OBJECT_PRIORITY;
					sr.objectType = ObjectType.FAVORITE;
					sr.location = new LatLon(point.getLatitude(), point.getLongitude());
					sr.preferredZoom = 17;
					if (phrase.getUnknownSearchWordLength() <= 1 && phrase.isNoSelectedType()) {
						resultMatcher.publish(sr);
					} else if (phrase.getNameStringMatcher().matches(sr.localeName)) {
						resultMatcher.publish(sr);
					} else if (point.getCategory() != null && phrase.getNameStringMatcher().matches(point.getCategory())) {
						resultMatcher.publish(sr);
					}
				}
				return true;
			}

			@Override
			public int getSearchPriority(SearchPhrase p) {
				if(!p.isNoSelectedType() || !p.isUnknownSearchWordPresent()) {
					return -1;
				}
				return SEARCH_FAVORITE_API_PRIORITY;
			}
		});

		// Register WptPt search api
		searchUICore.registerAPI(new SearchWptAPI(app));
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setShowsDialog(true);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putString(QUICK_SEARCH_QUERY_KEY, searchQuery);
	}

	@Override
	public void onResume() {
		super.onResume();
		if (!useMapCenter) {
			startLocationUpdate();
		}
		paused = false;
	}

	@Override
	public void onPause() {
		super.onPause();
		paused = true;
		stopLocationUpdate();
	}

	@Override
	public void onDismiss(DialogInterface dialog) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			getMapActivity().setQuickSearchTopbarActive(false);
			getChildFragmentManager().popBackStack();
		}
		super.onDismiss(dialog);
	}

	public Toolbar getToolbar() {
		return toolbar;
	}

	public boolean isUseMapCenter() {
		return useMapCenter;
	}

	private void startLocationUpdate() {
		OsmandApplication app = getMyApplication();
		app.getLocationProvider().addCompassListener(this);
		app.getLocationProvider().addLocationListener(this);
		location = app.getLocationProvider().getLastKnownLocation();
		updateLocation(location);
	}

	private void stopLocationUpdate() {
		OsmandApplication app = getMyApplication();
		app.getLocationProvider().removeLocationListener(this);
		app.getLocationProvider().removeCompassListener(this);
	}

	private void showProgressBar() {
		updateClearButtonVisibility(false);
		progressBar.setVisibility(View.VISIBLE);
	}

	private void hideProgressBar() {
		updateClearButtonVisibility(true);
		progressBar.setVisibility(View.GONE);
	}

	private void updateClearButtonAndHint() {
		OsmandApplication app = getMyApplication();
		if (useMapCenter && searchEditText.length() == 0) {
			LatLon latLon = searchUICore.getSearchSettings().getOriginalLocation();
			double d = MapUtils.getDistance(latLon, location.getLatitude(), location.getLongitude());
			String dist = OsmAndFormatter.getFormattedDistance((float) d, app);
			searchEditText.setHint(getString(R.string.dist_away_from_my_location, dist));
			clearButton.setImageDrawable(app.getIconsCache().getIcon(R.drawable.ic_action_get_my_location, R.color.color_myloc_distance));
		} else {
			searchEditText.setHint(R.string.search_poi_category_hint);
			clearButton.setImageDrawable(app.getIconsCache().getThemedIcon(R.drawable.ic_action_remove_dark));
		}
	}

	private void updateClearButtonVisibility(boolean show) {
		if (show) {
			clearButton.setVisibility(searchEditText.length() > 0 || useMapCenter ? View.VISIBLE : View.GONE);
		} else {
			clearButton.setVisibility(View.GONE);
		}
	}

	private void updateTabbarVisibility(boolean show) {
		if (show && tabsView.getVisibility() == View.GONE) {
			tabToolbarView.setVisibility(View.VISIBLE);
			buttonToolbarView.setVisibility(View.GONE);
			tabsView.setVisibility(View.VISIBLE);
			searchView.setVisibility(View.GONE);
		} else if (!show && tabsView.getVisibility() == View.VISIBLE) {
			tabToolbarView.setVisibility(View.GONE);
			buttonToolbarView.setVisibility(View.VISIBLE);
			tabsView.setVisibility(View.GONE);
			searchView.setVisibility(View.VISIBLE);
		}
	}

	public void onSearchListFragmentResume(QuickSearchListFragment searchListFragment) {
		switch (searchListFragment.getType()) {
			case HISTORY:
				historySearchFragment = (QuickSearchHistoryListFragment) searchListFragment;
				reloadHistory();
				break;

			case CATEGORIES:
				categoriesSearchFragment = (QuickSearchCategoriesListFragment) searchListFragment;
				reloadCategories();
				break;

			case MAIN:
				if (!Algorithms.isEmpty(searchQuery) && !searchQuery.equals(searchEditText.getText().toString())) {
					String txt = searchQuery;
					searchQuery = "";
					searchEditText.setText(txt);
					searchEditText.setSelection(txt.length());
				}
				break;
		}
		LatLon mapCenter = getMapActivity().getMapView().getCurrentRotatedTileBox().getCenterLatLon();
		if (useMapCenter) {
			searchListFragment.updateLocation(mapCenter, null);
		}
	}

	private void reloadCategories() {
		SearchAmenityTypesAPI amenityTypesAPI =
				new SearchAmenityTypesAPI(getMyApplication().getPoiTypes());
		final List<SearchResult> amenityTypes = new ArrayList<>();
		SearchPhrase sp = new SearchPhrase(null).generateNewPhrase("", searchUICore.getSearchSettings());
		try {
			amenityTypesAPI.search(sp, new SearchResultMatcher(
					new ResultMatcher<SearchResult>() {
						@Override
						public boolean publish(SearchResult object) {
							amenityTypes.add(object);
							return true;
						}

						@Override
						public boolean isCancelled() {
							return false;
						}
					}, 0, new AtomicInteger(0), -1));
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (amenityTypes.size() > 0) {
			searchUICore.sortSearchResults(sp, amenityTypes);
			List<QuickSearchListItem> rows = new ArrayList<>();
			OsmandApplication app = getMyApplication();
			for (SearchResult sr : amenityTypes) {
				rows.add(new QuickSearchListItem(app, sr));
			}
			categoriesSearchFragment.updateListAdapter(rows, false);
		}
	}

	private void reloadHistory() {
		SearchHistoryAPI historyAPI = new SearchHistoryAPI(getMyApplication());
		final List<SearchResult> history = new ArrayList<>();
		SearchPhrase sp = new SearchPhrase(null).generateNewPhrase("", searchUICore.getSearchSettings());
		historyAPI.search(sp, new SearchResultMatcher(
				new ResultMatcher<SearchResult>() {
					@Override
					public boolean publish(SearchResult object) {
						history.add(object);
						return true;
					}

					@Override
					public boolean isCancelled() {
						return false;
					}
				}, 0, new AtomicInteger(0), -1));
		List<QuickSearchListItem> rows = new ArrayList<>();
		if (history.size() > 0) {
			OsmandApplication app = getMyApplication();
			for (SearchResult sr : history) {
				rows.add(new QuickSearchListItem(app, sr));
			}
		}
		historySearchFragment.updateListAdapter(rows, false);
	}

	private void runSearch() {
		runSearch(searchQuery);
	}

	private void runSearch(String text) {
		showProgressBar();
		SearchSettings settings = searchUICore.getPhrase().getSettings();
		if(settings.getRadiusLevel() != 1){
			searchUICore.updateSettings(settings.setRadiusLevel(1));
		}
		SearchResultCollection c = runCoreSearch(text);
		updateSearchResult(c, false);
	}

	private SearchResultCollection runCoreSearch(String text) {
		showProgressBar();
		return searchUICore.search(text, new ResultMatcher<SearchResult>() {

			SearchResultCollection regionResultCollection = null;
			SearchCoreAPI regionResultApi = null;
			List<SearchResult> results = new ArrayList<>();

			@Override
			public boolean publish(SearchResult object) {

				if (paused) {
					return false;
				}

				switch (object.objectType) {
					case SEARCH_API_FINISHED:
						final SearchCoreAPI searchApi = (SearchCoreAPI) object.object;

						final List<SearchResult> apiResults;
						final SearchPhrase phrase = object.requiredSearchPhrase;
						final SearchCoreAPI regionApi = regionResultApi;
						final SearchResultCollection regionCollection = regionResultCollection;

						final boolean hasRegionCollection = (searchApi == regionApi && regionCollection != null);
						if (hasRegionCollection) {
							apiResults = regionCollection.getCurrentSearchResults();
						} else {
							apiResults = results;
							searchUICore.sortSearchResults(phrase, apiResults);
						}

						regionResultApi = null;
						regionResultCollection = null;
						results = new ArrayList<>();

						getMyApplication().runInUIThread(new Runnable() {
							@Override
							public void run() {
								boolean appended = false;
								if (resultCollection == null || resultCollection.getPhrase() != phrase) {
									resultCollection = new SearchResultCollection(apiResults, phrase);
								} else {
									resultCollection.getCurrentSearchResults().addAll(apiResults);
									appended = true;
								}
								if (!hasRegionCollection) {
									updateSearchResult(resultCollection, appended);
								}
							}
						});
						break;
					case SEARCH_API_REGION_FINISHED:
						regionResultApi = (SearchCoreAPI) object.object;

						final List<SearchResult> regionResults = new ArrayList<>(results);
						final SearchPhrase regionPhrase = object.requiredSearchPhrase;
						searchUICore.sortSearchResults(regionPhrase, regionResults);

						getMyApplication().runInUIThread(new Runnable() {
							@Override
							public void run() {
								boolean appended = resultCollection != null && resultCollection.getPhrase() == regionPhrase;
								regionResultCollection = new SearchResultCollection(regionResults, regionPhrase);
								if (appended) {
									List<SearchResult> res = new ArrayList<>(resultCollection.getCurrentSearchResults());
									res.addAll(regionResults);
									SearchResultCollection resCollection = new SearchResultCollection(res, regionPhrase);
									updateSearchResult(resCollection, true);
								} else {
									updateSearchResult(regionResultCollection, false);
								}
							}
						});
						break;
					case PARTIAL_LOCATION:
						// todo
						break;
					default:
						results.add(object);
				}

				return false;
			}

			@Override
			public boolean isCancelled() {
				return paused;
			}
		});
	}

	public void completeQueryWithObject(SearchResult sr) {
		searchUICore.selectSearchResult(sr);
		String txt = searchUICore.getPhrase().getText(true);
		searchQuery = txt;
		searchEditText.setText(txt);
		searchEditText.setSelection(txt.length());
		buttonToolbarText.setText(getMyApplication().getString(R.string.show_something_on_map, sr.localeName).toUpperCase());
		runCoreSearch(txt);
	}

	private void addMoreButton() {
		OsmandApplication app = getMyApplication();
		if ((searchUICore.getPhrase().isUnknownSearchWordPresent() || searchUICore.getPhrase().isLastWord(ObjectType.POI_TYPE))
				&& searchUICore.getPhrase().getSettings().getRadiusLevel() < 7) {

			QuickSearchMoreListItem moreListItem =
					new QuickSearchMoreListItem(app, app.getString(R.string.search_POI_level_btn).toUpperCase(), new OnClickListener() {
						@Override
						public void onClick(View v) {
							SearchSettings settings = searchUICore.getPhrase().getSettings();
							searchUICore.updateSettings(settings.setRadiusLevel(settings.getRadiusLevel() + 1));
							runCoreSearch(searchQuery);
						}
					});

			if (!paused && mainSearchFragment != null) {
				mainSearchFragment.addListItem(moreListItem);
			}
		}
	}

	private void updateSearchResult(SearchResultCollection res, boolean appended) {

		if (!paused && mainSearchFragment != null) {
			OsmandApplication app = getMyApplication();
			List<QuickSearchListItem> rows = new ArrayList<>();
			if (res.getCurrentSearchResults().size() > 0) {
				for (final SearchResult sr : res.getCurrentSearchResults()) {
					rows.add(new QuickSearchListItem(app, sr));
				}
			}
			mainSearchFragment.updateListAdapter(rows, appended);
		}
	}

	public static boolean showInstance(final MapActivity mapActivity, final String searchQuery) {
		try {

			if (mapActivity.isActivityDestroyed()) {
				return false;
			}

			final OsmandApplication app = mapActivity.getMyApplication();
			if (app.isApplicationInitializing()) {
				new AsyncTask<Void, Void, Void>() {

					private ProgressDialog dlg;

					@Override
					protected void onPreExecute() {
						dlg = new ProgressDialog(mapActivity);
						dlg.setTitle("");
						dlg.setMessage(mapActivity.getString(R.string.wait_current_task_finished));
						dlg.setCanceledOnTouchOutside(false);
						dlg.show();
					}

					@Override
					protected Void doInBackground(Void... params) {
						while (app.isApplicationInitializing()) {
							try {
								Thread.sleep(50);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
						return null;
					}

					@Override
					protected void onPostExecute(Void aVoid) {
						dlg.hide();
						showInternal(mapActivity, searchQuery);
					}
				}.execute();

			} else {
				showInternal(mapActivity, searchQuery);
			}

			return true;

		} catch (RuntimeException e) {
			return false;
		}
	}

	private static void showInternal(MapActivity mapActivity, String searchQuery) {
		Bundle bundle = new Bundle();
		bundle.putString(QUICK_SEARCH_QUERY_KEY, searchQuery);
		QuickSearchDialogFragment fragment = new QuickSearchDialogFragment();
		fragment.setArguments(bundle);
		fragment.show(mapActivity.getSupportFragmentManager(), TAG);
	}

	private MapActivity getMapActivity() {
		return (MapActivity) getActivity();
	}

	private OsmandApplication getMyApplication() {
		return (OsmandApplication) getActivity().getApplication();
	}

	@Override
	public void updateCompassValue(final float value) {
		// 99 in next line used to one-time initialize arrows (with reference vs. fixed-north direction)
		// on non-compass devices
		float lastHeading = heading != null ? heading : 99;
		heading = value;
		if (Math.abs(MapUtils.degreesDiff(lastHeading, heading)) > 5) {
			final Location location = this.location;
			getMyApplication().runInUIThread(new Runnable() {
				@Override
				public void run() {
					updateLocationUI(location, value);
				}
			});
		} else {
			heading = lastHeading;
		}
	}

	@Override
	public void updateLocation(final Location location) {
		this.location = location;
		final Float heading = this.heading;
		getMyApplication().runInUIThread(new Runnable() {
			@Override
			public void run() {
				updateLocationUI(location, heading);
			}
		});
	}

	private void updateLocationUI(Location location, Float heading) {
		LatLon latLon = null;
		if (location != null) {
			latLon = new LatLon(location.getLatitude(), location.getLongitude());
		}
		updateLocationUI(latLon, heading);
	}

	private void updateLocationUI(LatLon latLon, Float heading) {
		if (latLon != null && !paused) {
			if (mainSearchFragment != null) {
				mainSearchFragment.updateLocation(latLon, heading);
			}
			if (historySearchFragment != null) {
				historySearchFragment.updateLocation(latLon, heading);
			}
			if (categoriesSearchFragment != null) {
				categoriesSearchFragment.updateLocation(latLon, heading);
			}
		}
	}

	private void updateUseMapCenterUI() {
		if (!paused) {
			if (mainSearchFragment != null) {
				mainSearchFragment.getListAdapter().setUseMapCenter(useMapCenter);
			}
			if (historySearchFragment != null) {
				historySearchFragment.getListAdapter().setUseMapCenter(useMapCenter);
			}
			if (categoriesSearchFragment != null) {
				categoriesSearchFragment.getListAdapter().setUseMapCenter(useMapCenter);
			}
		}
	}

	public void enableSelectionMode(boolean selectionMode,int position) {
		historySearchFragment.setSelectionMode(selectionMode, position);
		tabToolbarView.setVisibility(selectionMode ? View.GONE : View.VISIBLE);
		buttonToolbarView.setVisibility(View.GONE);
		toolbar.setVisibility(selectionMode ? View.GONE : View.VISIBLE);
		toolbarEdit.setVisibility(selectionMode ? View.VISIBLE : View.GONE);
		viewPager.setSwipeLocked(selectionMode);
	}

	public void updateSelectionMode(List<QuickSearchListItem> selectedItems) {
		if (selectedItems.size() > 0) {
			String text = selectedItems.size() + " " + getMyApplication().getString(R.string.shared_string_selected_lowercase);
			titleEdit.setText(text);
		} else {
			titleEdit.setText("");
		}
	}

	private void shareHistory(final List<HistoryEntry> historyEntries) {
		if (!historyEntries.isEmpty()) {
			final AsyncTask<Void, Void, GPXFile> exportTask = new AsyncTask<Void, Void, GPXFile>() {
				@Override
				protected GPXFile doInBackground(Void... params) {
					GPXFile gpx = new GPXFile();
					for (HistoryEntry h : historyEntries) {
						WptPt pt = new WptPt();
						pt.lat = h.getLat();
						pt.lon = h.getLon();
						pt.name = h.getName().getName();
						boolean hasTypeInDescription = !Algorithms.isEmpty(h.getName().getTypeName());
						if (hasTypeInDescription) {
							pt.desc = h.getName().getTypeName();
						}
						gpx.points.add(pt);
					}
					return gpx;
				}

				@Override
				protected void onPreExecute() {
					showProgressBar();
				}

				@Override
				protected void onPostExecute(GPXFile gpxFile) {
					hideProgressBar();
					File dir = new File(getActivity().getCacheDir(), "share");
					if (!dir.exists()) {
						dir.mkdir();
					}
					File dst = new File(dir, "History.gpx");
					GPXUtilities.writeGpxFile(dst, gpxFile, getMyApplication());

					final Intent sendIntent = new Intent();
					sendIntent.setAction(Intent.ACTION_SEND);
					sendIntent.putExtra(Intent.EXTRA_TEXT, "History.gpx:\n\n\n" + GPXUtilities.asString(gpxFile, getMyApplication()));
					sendIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_history_subject));
					sendIntent.putExtra(Intent.EXTRA_STREAM,
							FileProvider.getUriForFile(getActivity(),
									getActivity().getPackageName() + ".fileprovider", dst));
					sendIntent.setType("text/plain");
					startActivity(sendIntent);
				}
			};
			exportTask.execute();
		}
	}

	public class SearchFragmentPagerAdapter extends FragmentPagerAdapter {
		private final String[] fragments = new String[]{QuickSearchHistoryListFragment.class.getName(),
				QuickSearchCategoriesListFragment.class.getName()};
		private final int[] titleIds = new int[]{QuickSearchHistoryListFragment.TITLE,
				QuickSearchCategoriesListFragment.TITLE};
		private final String[] titles;

		public SearchFragmentPagerAdapter(FragmentManager fm, Resources res) {
			super(fm);
			titles = new String[titleIds.length];
			for (int i = 0; i < titleIds.length; i++) {
				titles[i] = res.getString(titleIds[i]);
			}
		}

		@Override
		public int getCount() {
			return fragments.length;
		}

		@Override
		public Fragment getItem(int position) {
			return Fragment.instantiate(QuickSearchDialogFragment.this.getContext(), fragments[position]);
		}

		@Override
		public CharSequence getPageTitle(int position) {
			return titles[position];
		}
	}

	public static class QuickSearchHistoryListFragment extends QuickSearchListFragment {
		public static final int TITLE = R.string.shared_string_history;
		private boolean selectionMode;

		@Override
		public SearchListFragmentType getType() {
			return SearchListFragmentType.HISTORY;
		}

		public boolean isSelectionMode() {
			return selectionMode;
		}

		public void setSelectionMode(boolean selectionMode, int position) {
			this.selectionMode = selectionMode;
			getListAdapter().setSelectionMode(selectionMode, position);
		}

		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
			super.onActivityCreated(savedInstanceState);
			getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
				@Override
				public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
					if (selectionMode) {
						return false;
					} else {
						getDialogFragment().enableSelectionMode(true, position - getListView().getHeaderViewsCount());
						return true;
					}
				}
			});
			getListAdapter().setSelectionListener(new QuickSearchListAdapter.OnSelectionListener() {
				@Override
				public void onUpdateSelectionMode(List<QuickSearchListItem> selectedItems) {
					getDialogFragment().updateSelectionMode(selectedItems);
				}

				@Override
				public void reloadData() {
					getDialogFragment().reloadHistory();
				}
			});
		}

		@Override
		public void onListItemClick(ListView l, View view, int position, long id) {
			if (selectionMode) {
				CheckBox ch = (CheckBox) view.findViewById(R.id.toggle_item);
				ch.setChecked(!ch.isChecked());
				getListAdapter().toggleCheckbox(position - l.getHeaderViewsCount(), ch);
			} else {
				super.onListItemClick(l, view, position, id);
			}
		}
	}

	public static class QuickSearchCategoriesListFragment extends QuickSearchListFragment {
		public static final int TITLE = R.string.search_categories;

		@Override
		public SearchListFragmentType getType() {
			return SearchListFragmentType.CATEGORIES;
		}
	}

	public static class QuickSearchMainListFragment extends QuickSearchListFragment {

		@Override
		public SearchListFragmentType getType() {
			return SearchListFragmentType.MAIN;
		}
	}

	public static class SearchHistoryAPI extends SearchBaseAPI {

		private OsmandApplication app;

		public SearchHistoryAPI(OsmandApplication app) {
			this.app = app;
		}

		@Override
		public boolean search(SearchPhrase phrase, SearchResultMatcher resultMatcher) {
			SearchHistoryHelper helper = SearchHistoryHelper.getInstance(app);
			List<HistoryEntry> points = helper.getHistoryEntries();
			for (HistoryEntry point : points) {
				SearchResult sr = new SearchResult(phrase);
				sr.localeName = point.getName().getName();
				sr.object = point;
				sr.priority = SEARCH_HISTORY_OBJECT_PRIORITY;
				sr.objectType = ObjectType.RECENT_OBJ;
				sr.location = new LatLon(point.getLat(), point.getLon());
				sr.preferredZoom = 17;
				if (phrase.getUnknownSearchWordLength() <= 1 && phrase.isNoSelectedType()) {
					resultMatcher.publish(sr);
				} else if (phrase.getNameStringMatcher().matches(sr.localeName)) {
					resultMatcher.publish(sr);
				}
			}
			return true;
		}

		@Override
		public int getSearchPriority(SearchPhrase p) {
			if(!p.isNoSelectedType()) {
				return -1;
			}
			return SEARCH_HISTORY_API_PRIORITY;
		}
	}

	public static class SearchWptAPI extends SearchBaseAPI {

		private OsmandApplication app;

		public SearchWptAPI(OsmandApplication app) {
			this.app = app;
		}

		@Override
		public boolean search(SearchPhrase phrase, SearchResultMatcher resultMatcher) {

			if (phrase.isEmpty()) {
				return false;
			}

			List<SelectedGpxFile> list = app.getSelectedGpxHelper().getSelectedGPXFiles();
			for (SelectedGpxFile selectedGpx : list) {
				if (selectedGpx != null) {
					for (WptPt point : selectedGpx.getGpxFile().points) {
						SearchResult sr = new SearchResult(phrase);
						sr.localeName = point.getPointDescription(app).getName();
						sr.object = point;
						sr.priority = SEARCH_WPT_OBJECT_PRIORITY;
						sr.objectType = ObjectType.WPT;
						sr.location = new LatLon(point.getLatitude(), point.getLongitude());
						sr.localeRelatedObjectName = app.getRegions().getCountryName(sr.location);
						sr.relatedObject = selectedGpx.getGpxFile();
						sr.preferredZoom = 17;
						if (phrase.getUnknownSearchWordLength() <= 1 && phrase.isNoSelectedType()) {
							resultMatcher.publish(sr);
						} else if (phrase.getNameStringMatcher().matches(sr.localeName)) {
							resultMatcher.publish(sr);
						}
					}
				}
			}
			return true;
		}

		@Override
		public int getSearchPriority(SearchPhrase p) {
			if(!p.isNoSelectedType()) {
				return -1;
			}
			return SEARCH_WPT_API_PRIORITY;
		}
	}
}
