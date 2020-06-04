package net.osmand.core.samples.android.sample1.search;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import net.osmand.AndroidUtils;
import net.osmand.Location;
import net.osmand.ResultMatcher;
import net.osmand.core.samples.android.sample1.MainActivity;
import net.osmand.core.samples.android.sample1.R;
import net.osmand.core.samples.android.sample1.SampleApplication;
import net.osmand.core.samples.android.sample1.SampleFormatter;
import net.osmand.core.samples.android.sample1.SampleLocationProvider.SampleCompassListener;
import net.osmand.core.samples.android.sample1.SampleLocationProvider.SampleLocationListener;
import net.osmand.core.samples.android.sample1.data.PointDescription;
import net.osmand.data.LatLon;
import net.osmand.osm.PoiCategory;
import net.osmand.search.SearchUICore;
import net.osmand.search.SearchUICore.SearchResultCollection;
import net.osmand.search.core.ObjectType;
import net.osmand.search.core.SearchCoreAPI;
import net.osmand.search.core.SearchCoreFactory.SearchAmenityTypesAPI;
import net.osmand.search.core.SearchPhrase;
import net.osmand.search.core.SearchResult;
import net.osmand.search.core.SearchSettings;
import net.osmand.search.core.SearchWord;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static net.osmand.search.core.SearchCoreFactory.SEARCH_AMENITY_TYPE_PRIORITY;

public class QuickSearchDialogFragment extends DialogFragment implements SampleCompassListener, SampleLocationListener {

	public static final String TAG = "QuickSearchDialogFragment";
	private static final String QUICK_SEARCH_QUERY_KEY = "quick_search_query_key";
	private static final String QUICK_SEARCH_LAT_KEY = "quick_search_lat_key";
	private static final String QUICK_SEARCH_LON_KEY = "quick_search_lon_key";
	private static final String QUICK_SEARCH_INTERRUPTED_SEARCH_KEY = "quick_search_interrupted_search_key";
	private static final String QUICK_SEARCH_SHOW_CATEGORIES_KEY = "quick_search_show_categories_key";
	private static final String QUICK_SEARCH_HIDDEN_KEY = "quick_search_hidden_key";
	private static final String QUICK_SEARCH_TOOLBAR_TITLE_KEY = "quick_search_toolbar_title_key";
	private static final String QUICK_SEARCH_TOOLBAR_VISIBLE_KEY = "quick_search_toolbar_visible_key";
	private static final String QUICK_SEARCH_RUN_SEARCH_FIRST_TIME_KEY = "quick_search_run_search_first_time_key";
	private static final String QUICK_SEARCH_PHRASE_DEFINED_KEY = "quick_search_phrase_defined_key";

	private Toolbar toolbar;
	private View searchView;
	private View categoriesView;
	private View buttonToolbarView;
	private ImageView buttonToolbarImage;
	private TextView buttonToolbarText;
	private QuickSearchMainListFragment mainSearchFragment;
	private QuickSearchCategoriesListFragment categoriesSearchFragment;
	//private QuickSearchToolbarController toolbarController;

	private EditText searchEditText;
	private ProgressBar progressBar;
	private ImageButton clearButton;

	private SampleApplication app;
	private QuickSearchHelper searchHelper;
	private SearchUICore searchUICore;
	private String searchQuery;

	private LatLon centerLatLon;
	private Location location = null;
	private Float heading = null;
	private boolean useMapCenter;
	private boolean paused;
	private boolean cancelPrev;
	private boolean searching;
	private boolean hidden;
	private String toolbarTitle;
	private boolean toolbarVisible;

	private boolean newSearch;
	private boolean interruptedSearch;
	private long hideTimeMs;
	private boolean runSearchFirstTime;
	private boolean phraseDefined;

	private static final double DISTANCE_THRESHOLD = 70000; // 70km
	private static final int EXPIRATION_TIME_MIN = 10; // 10 minutes


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = getMyApplication();
		setStyle(STYLE_NO_FRAME, R.style.AppTheme);
	}

	@Override
	@SuppressLint("PrivateResource, ValidFragment")
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		final MainActivity mainActivity = getMainActivity();
		final View view = inflater.inflate(R.layout.search_dialog_fragment, container, false);

		/*
		toolbarController = new QuickSearchToolbarController();
		toolbarController.setOnBackButtonClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mainActivity.showQuickSearch(ShowQuickSearchMode.CURRENT, false);
			}
		});
		toolbarController.setOnTitleClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mainActivity.showQuickSearch(ShowQuickSearchMode.CURRENT, false);
			}
		});
		toolbarController.setOnCloseButtonClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mainActivity.closeQuickSearch();
			}
		});
		*/

		Bundle arguments = getArguments();
		if (savedInstanceState != null) {
			searchQuery = savedInstanceState.getString(QUICK_SEARCH_QUERY_KEY);
			double lat = savedInstanceState.getDouble(QUICK_SEARCH_LAT_KEY, Double.NaN);
			double lon = savedInstanceState.getDouble(QUICK_SEARCH_LON_KEY, Double.NaN);
			if (!Double.isNaN(lat) && !Double.isNaN(lon)) {
				centerLatLon = new LatLon(lat, lon);
			}
			interruptedSearch = savedInstanceState.getBoolean(QUICK_SEARCH_INTERRUPTED_SEARCH_KEY, false);
			hidden = savedInstanceState.getBoolean(QUICK_SEARCH_HIDDEN_KEY, false);
			toolbarTitle = savedInstanceState.getString(QUICK_SEARCH_TOOLBAR_TITLE_KEY);
			toolbarVisible = savedInstanceState.getBoolean(QUICK_SEARCH_TOOLBAR_VISIBLE_KEY, false);
		}
		if (searchQuery == null && arguments != null) {
			searchQuery = arguments.getString(QUICK_SEARCH_QUERY_KEY);
			runSearchFirstTime = arguments.getBoolean(QUICK_SEARCH_RUN_SEARCH_FIRST_TIME_KEY, false);
			phraseDefined = arguments.getBoolean(QUICK_SEARCH_PHRASE_DEFINED_KEY, false);
			double lat = arguments.getDouble(QUICK_SEARCH_LAT_KEY, Double.NaN);
			double lon = arguments.getDouble(QUICK_SEARCH_LON_KEY, Double.NaN);
			if (!Double.isNaN(lat) && !Double.isNaN(lon)) {
				centerLatLon = new LatLon(lat, lon);
			}
			newSearch = true;
		}
		if (searchQuery == null)
			searchQuery = "";

		boolean showCategories = false;
		if (arguments != null) {
			showCategories = arguments.getBoolean(QUICK_SEARCH_SHOW_CATEGORIES_KEY, false);
		}

		searchView = view.findViewById(R.id.search_view);
		categoriesView = view.findViewById(R.id.categories_view);

		buttonToolbarView = view.findViewById(R.id.button_toolbar_layout);
		buttonToolbarImage = (ImageView) view.findViewById(R.id.buttonToolbarImage);
		buttonToolbarImage.setImageDrawable(app.getIconsCache().getThemedIcon("ic_action_marker_dark"));

		buttonToolbarText = (TextView) view.findViewById(R.id.buttonToolbarTitle);
		view.findViewById(R.id.buttonToolbar).setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View v) {
						SearchPhrase searchPhrase = searchUICore.getPhrase();
						SearchWord word = searchPhrase.getLastSelectedWord();
						if (word != null && word.getLocation() != null) {
							SearchResult searchResult = word.getResult();
							String name = QuickSearchListItem.getName(app, searchResult);
							String typeName = QuickSearchListItem.getTypeName(app, searchResult);
							PointDescription pointDescription = new PointDescription(PointDescription.POINT_TYPE_ADDRESS, typeName, name);

							mainActivity.showOnMap(searchResult.location, searchResult.preferredZoom);
							mainActivity.getContextMenu().show(searchResult.location, pointDescription, searchResult.object);
							hide();
						}
					}
				}
		);

		toolbar = (Toolbar) view.findViewById(R.id.toolbar);
		toolbar.setNavigationIcon(app.getIconsCache().getThemedIcon(R.drawable.ic_arrow_back));
		toolbar.setNavigationContentDescription(app.getString("access_shared_string_navigate_up"));
		toolbar.setNavigationOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View v) {
						dismiss();
					}
				}
		);

		searchEditText = (EditText) view.findViewById(R.id.searchEditText);
		searchEditText.addTextChangedListener(
				new TextWatcher() {
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
						updateClearButtonVisibility(true);
						boolean textEmpty = newQueryText.length() == 0;
						updateViewsVisibility(textEmpty);
						if (!searchQuery.equalsIgnoreCase(newQueryText)) {
							searchQuery = newQueryText;
							if (Algorithms.isEmpty(searchQuery)) {
								searchUICore.resetPhrase();
							} else {
								runSearch();
							}
						} else if (runSearchFirstTime) {
							runSearchFirstTime = false;
							runSearch();
						}
					}
				}
		);

		progressBar = (ProgressBar) view.findViewById(R.id.searchProgressBar);
		clearButton = (ImageButton) view.findViewById(R.id.clearButton);
		clearButton.setImageDrawable(app.getIconsCache().getThemedIcon(R.drawable.ic_action_remove_dark));
		clearButton.setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View v) {
						if (searchEditText.getText().length() > 0) {
							String newText = searchUICore.getPhrase().getTextWithoutLastWord();
							searchEditText.setText(newText);
							searchEditText.setSelection(newText.length());
						} else if (useMapCenter && location != null) {
							useMapCenter = false;
							centerLatLon = null;
							updateUseMapCenterUI();
							LatLon centerLatLon = new LatLon(location.getLatitude(), location.getLongitude());
							SearchSettings ss = searchUICore.getSearchSettings().setOriginalLocation(
									new LatLon(centerLatLon.getLatitude(), centerLatLon.getLongitude()));
							searchUICore.updateSettings(ss);
							updateClearButtonAndHint();
							updateClearButtonVisibility(true);
							startLocationUpdate();
						}
						updateToolbarButton();
					}
				}
		);

		setupSearch(mainActivity);
		return view;
	}

	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		updateToolbarButton();
		updateClearButtonAndHint();
		updateClearButtonVisibility(true);
		addCategoriesFragment();
		addMainSearchFragment();

		if (centerLatLon == null) {
			searchEditText.requestFocus();
			AndroidUtils.softKeyboardDelayed(searchEditText);
		}
	}

	public void restoreToolbar() {
		if (toolbarVisible) {
			if (toolbarTitle != null) {
				showToolbar(toolbarTitle);
			} else {
				showToolbar();
			}
		}
	}

	public void showToolbar() {
		showToolbar(getText());
	}

	public void showToolbar(String title) {
		toolbarVisible = true;
		toolbarTitle = title;
		//todo
		//toolbarController.setTitle(toolbarTitle);
		//getMainActivity().showTopToolbar(toolbarController);
	}

	public void hideToolbar() {
		toolbarVisible = false;
		//getMainActivity().hideTopToolbar(toolbarController); todo
	}

	public String getText() {
		return searchEditText.getText().toString();
	}

	public void hideKeyboard() {
		if (searchEditText.hasFocus()) {
			AndroidUtils.hideSoftKeyboard(getActivity(), searchEditText);
		}
	}

	public boolean isSearchHidden() {
		return hidden;
	}

	public boolean isExpired() {
		return hideTimeMs > 0 && System.currentTimeMillis() - hideTimeMs > EXPIRATION_TIME_MIN * 60 * 1000;
	}

	public void show() {
		if (useMapCenter) {
			LatLon mapCenter = getMainActivity().getScreenCenter();
			SearchSettings ss = searchUICore.getSearchSettings().setOriginalLocation(
					new LatLon(mapCenter.getLatitude(), mapCenter.getLongitude()));
			searchUICore.updateSettings(ss);
			updateUseMapCenterUI();
			updateLocationUI(mapCenter, null);
		}
		getDialog().show();
		paused = false;
		hidden = false;
		if (interruptedSearch) {
			interruptedSearch = false;
			addMoreButton();
		}
	}

	public void hide() {
		paused = true;
		hidden = true;
		hideTimeMs = System.currentTimeMillis();
		interruptedSearch = searching;
		searching = false;
		hideProgressBar();
		updateClearButtonVisibility(true);
		getDialog().hide();
	}

	public void closeSearch() {
		dismiss();
	}

	public void addMainSearchFragment() {
		mainSearchFragment = (QuickSearchMainListFragment) Fragment.instantiate(this.getContext(), QuickSearchMainListFragment.class.getName());
		FragmentManager childFragMan = getChildFragmentManager();
		FragmentTransaction childFragTrans = childFragMan.beginTransaction();
		childFragTrans.replace(R.id.search_view, mainSearchFragment);
		childFragTrans.commit();
	}

	public void addCategoriesFragment() {
		categoriesSearchFragment = (QuickSearchCategoriesListFragment) Fragment.instantiate(this.getContext(), QuickSearchCategoriesListFragment.class.getName());
		FragmentManager childFragMan = getChildFragmentManager();
		FragmentTransaction childFragTrans = childFragMan.beginTransaction();
		childFragTrans.replace(R.id.categories_view, categoriesSearchFragment);
		childFragTrans.commit();
	}

	private void updateToolbarButton() {
		SearchWord word = searchUICore.getPhrase().getLastSelectedWord();
		if (word != null && word.getLocation() != null) {
			if (searchEditText.getText().length() > 0) {
				if (word.getResult() != null) {
					buttonToolbarText.setText(app.getString("show_something_on_map", word.getResult().localeName).toUpperCase());
				} else {
					buttonToolbarText.setText(app.getString("shared_string_show_on_map").toUpperCase());
				}
			} else {
				buttonToolbarText.setText(app.getString("shared_string_show_on_map").toUpperCase());
			}
			buttonToolbarView.setVisibility(View.VISIBLE);
		} else {
			buttonToolbarView.setVisibility(View.GONE);
		}
	}

	private void setupSearch(final MainActivity mainActivity) {
		// Setup search core
		String locale = ""; //app.getSettings().MAP_PREFERRED_LOCALE.get();
		boolean transliterate = false; //app.getSettings().MAP_TRANSLITERATE_NAMES.get();
		searchHelper = app.getSearchUICore();
		searchUICore = searchHelper.getCore();
		if (newSearch) {
			setResultCollection(null);
			if (!phraseDefined) {
				searchUICore.resetPhrase();
			}
			phraseDefined = false;
		}

		location = app.getLocationProvider().getLastKnownLocation();

		LatLon searchLatLon;
		if (centerLatLon == null) {
			LatLon clt = mainActivity.getScreenCenter();
			searchLatLon = clt;
			searchEditText.setHint(app.getString("search_poi_category_hint"));
			if (location != null) {
				double d = MapUtils.getDistance(clt, location.getLatitude(), location.getLongitude());
				if (d < DISTANCE_THRESHOLD) {
					searchLatLon = new LatLon(location.getLatitude(), location.getLongitude());
				} else {
					useMapCenter = true;
				}
			} else {
				useMapCenter = true;
			}
		} else {
			searchLatLon = centerLatLon;
			useMapCenter = true;
		}
		SearchSettings settings = searchUICore.getSearchSettings().setOriginalLocation(
				new LatLon(searchLatLon.getLatitude(), searchLatLon.getLongitude()));
		settings = settings.setLang(locale, transliterate);
		searchUICore.updateSettings(settings);
		searchUICore.setOnSearchStart(new Runnable() {
			@Override
			public void run() {
				cancelPrev = false;
			}
		});
		searchUICore.setOnResultsComplete(new Runnable() {
			@Override
			public void run() {
				app.runInUIThread(new Runnable() {
					@Override
					public void run() {
						searching = false;
						if (!paused && !cancelPrev) {
							hideProgressBar();
							if (searchUICore.isSearchMoreAvailable(searchUICore.getPhrase())) {
								addMoreButton();
							}
						}
					}
				});
			}
		});
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setShowsDialog(true);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putString(QUICK_SEARCH_QUERY_KEY, searchQuery);
		outState.putBoolean(QUICK_SEARCH_INTERRUPTED_SEARCH_KEY, interruptedSearch = searching);
		outState.putBoolean(QUICK_SEARCH_HIDDEN_KEY, hidden);
		if (toolbarTitle != null) {
			outState.putString(QUICK_SEARCH_TOOLBAR_TITLE_KEY, toolbarTitle);
		}
		outState.putBoolean(QUICK_SEARCH_TOOLBAR_VISIBLE_KEY, toolbarVisible);
		if (centerLatLon != null) {
			outState.putDouble(QUICK_SEARCH_LAT_KEY, centerLatLon.getLatitude());
			outState.putDouble(QUICK_SEARCH_LON_KEY, centerLatLon.getLongitude());
		}
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
		hideTimeMs = System.currentTimeMillis();
		stopLocationUpdate();
		hideProgressBar();
	}

	@Override
	public void onDismiss(DialogInterface dialog) {
		MainActivity mainActivity = getMainActivity();
		if (mainActivity != null) {
			hideToolbar();
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
		app.getLocationProvider().addCompassListener(this);
		app.getLocationProvider().addLocationListener(this);
		location = app.getLocationProvider().getLastKnownLocation();
		updateLocation(location);
	}

	private void stopLocationUpdate() {
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
		if (useMapCenter && location != null && searchEditText.length() == 0) {
			LatLon latLon = searchUICore.getSearchSettings().getOriginalLocation();
			double d = MapUtils.getDistance(latLon, location.getLatitude(), location.getLongitude());
			String dist = SampleFormatter.getFormattedDistance((float) d, app);
			searchEditText.setHint(app.getString("dist_away_from_my_location", dist));
			clearButton.setImageDrawable(app.getIconsCache().getIcon("ic_action_get_my_location", R.color.color_myloc_distance));
		} else {
			searchEditText.setHint(app.getString("search_poi_category_hint"));
			clearButton.setImageDrawable(app.getIconsCache().getThemedIcon(R.drawable.ic_action_remove_dark));
		}
	}

	private void updateClearButtonVisibility(boolean show) {
		if (show) {
			clearButton.setVisibility(searchEditText.length() > 0 || (useMapCenter && location != null) ? View.VISIBLE : View.GONE);
		} else {
			clearButton.setVisibility(View.GONE);
		}
	}

	private void updateViewsVisibility(boolean show) {
		if (show) {
			buttonToolbarView.setVisibility(View.GONE);
			searchView.setVisibility(View.GONE);
			categoriesView.setVisibility(View.VISIBLE);
		} else {
			buttonToolbarView.setVisibility(View.VISIBLE);
			searchView.setVisibility(View.VISIBLE);
			categoriesView.setVisibility(View.GONE);
		}
	}

	public void setResultCollection(SearchResultCollection resultCollection) {
		searchHelper.setResultCollection(resultCollection);
	}

	public SearchResultCollection getResultCollection() {
		return searchHelper.getResultCollection();
	}

	public void onSearchListFragmentResume(QuickSearchListFragment searchListFragment) {
		switch (searchListFragment.getType()) {
			case CATEGORIES:
				reloadCategories();
				break;

			case MAIN:
				if (!Algorithms.isEmpty(searchQuery)) {
					searchEditText.setText(searchQuery);
					searchEditText.setSelection(searchQuery.length());
				}
				if (getResultCollection() != null) {
					updateSearchResult(getResultCollection(), false);
					if (interruptedSearch || searchUICore.isSearchMoreAvailable(searchUICore.getPhrase())) {
						addMoreButton();
					}
				}
				break;
		}
		LatLon mapCenter = getMainActivity().getScreenCenter();
		if (useMapCenter) {
			updateUseMapCenterUI();
			searchListFragment.updateLocation(mapCenter, null);
		}
	}

	public void reloadCategories() {
		try {
			SearchResultCollection res = searchUICore.shallowSearch(SearchAmenityTypesAPI.class,
					"", null);
			if (res != null) {
				List<QuickSearchListItem> rows = new ArrayList<>();
				for (SearchResult sr : res.getCurrentSearchResults()) {
					rows.add(new QuickSearchListItem(app, sr));
				}
				categoriesSearchFragment.updateListAdapter(rows, false);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void runSearch() {
		runSearch(searchQuery);
	}

	private void runSearch(String text) {
		showProgressBar();
		SearchSettings settings = searchUICore.getPhrase().getSettings();
		if (settings.getRadiusLevel() != 1) {
			searchUICore.updateSettings(settings.setRadiusLevel(1));
		}
		runCoreSearch(text, true, false);
	}

	private void runCoreSearch(final String text, final boolean updateResult, final boolean searchMore) {
		showProgressBar();
		updateToolbarButton();
		interruptedSearch = false;
		searching = true;
		cancelPrev = true;
		runCoreSearchInternal(text, updateResult, searchMore);
	}

	private void runCoreSearchInternal(String text, boolean updateResult, boolean searchMore) {
		searchUICore.search(text, updateResult, new ResultMatcher<SearchResult>() {
			SearchResultCollection regionResultCollection = null;
			SearchCoreAPI regionResultApi = null;
			List<SearchResult> results = new ArrayList<>();

			@Override
			public boolean publish(SearchResult object) {
				if (paused || cancelPrev) {
					if (results.size() > 0) {
						getResultCollection().addSearchResults(results, true, true);
					}
					return false;
				}
				switch (object.objectType) {
					case SEARCH_STARTED:
					case SEARCH_FINISHED:
						break;
					case FILTER_FINISHED:
						app.runInUIThread(new Runnable() {
							@Override
							public void run() {
								updateSearchResult(searchUICore.getCurrentSearchResult(), false);
							}
						});
						break;

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
						}
						regionResultApi = null;
						regionResultCollection = null;
						results = new ArrayList<>();
						showApiResults(apiResults, phrase, hasRegionCollection);
						break;
					case SEARCH_API_REGION_FINISHED:
						regionResultApi = (SearchCoreAPI) object.object;
						final SearchPhrase regionPhrase = object.requiredSearchPhrase;
						regionResultCollection =
								new SearchResultCollection(regionPhrase).addSearchResults(results, true, true);
						showRegionResults(regionResultCollection);
						break;
					case PARTIAL_LOCATION:
						break;
					default:
						results.add(object);
				}

				return false;
			}


			@Override
			public boolean isCancelled() {
				return paused || cancelPrev;
			}
		});

		if (!searchMore) {
			setResultCollection(null);
			if (!updateResult) {
				updateSearchResult(null, false);
			}
		}
	}

	private void showApiResults(final List<SearchResult> apiResults, final SearchPhrase phrase,
								final boolean hasRegionCollection) {
		app.runInUIThread(new Runnable() {
			@Override
			public void run() {
				if (!paused && !cancelPrev) {
					boolean append = getResultCollection() != null;
					if (append) {
						getResultCollection().addSearchResults(apiResults, true, true);
					} else {
						SearchResultCollection resCollection = new SearchResultCollection(phrase);
						resCollection.addSearchResults(apiResults, true, true);
						setResultCollection(resCollection);
					}
					if (!hasRegionCollection) {
						updateSearchResult(getResultCollection(), append);
					}
				}
			}
		});
	}

	private void showRegionResults(final SearchResultCollection regionResultCollection) {
		app.runInUIThread(new Runnable() {
			@Override
			public void run() {
				if (!paused && !cancelPrev) {
					if (getResultCollection() != null) {
						SearchResultCollection resCollection =
								getResultCollection().combineWithCollection(regionResultCollection, true, true);
						updateSearchResult(resCollection, true);
					} else {
						updateSearchResult(regionResultCollection, false);
					}
				}
			}
		});
	}

	public void completeQueryWithObject(SearchResult sr) {
		searchUICore.selectSearchResult(sr);
		String txt = searchUICore.getPhrase().getText(true);
		searchQuery = txt;
		searchEditText.setText(txt);
		searchEditText.setSelection(txt.length());
		updateToolbarButton();
		SearchSettings settings = searchUICore.getPhrase().getSettings();
		if (settings.getRadiusLevel() != 1) {
			searchUICore.updateSettings(settings.setRadiusLevel(1));
		}
		runCoreSearch(txt, false, false);
	}


	private void addMoreButton() {
		QuickSearchMoreListItem moreListItem =
				new QuickSearchMoreListItem(app, app.getString("search_POI_level_btn").toUpperCase(), new OnClickListener() {
					@Override
					public void onClick(View v) {
						if (!interruptedSearch) {
							SearchSettings settings = searchUICore.getPhrase().getSettings();
							searchUICore.updateSettings(settings.setRadiusLevel(settings.getRadiusLevel() + 1));
						}
						runCoreSearch(searchQuery, false, true);
					}
				});

		if (!paused && !cancelPrev && mainSearchFragment != null) {
			mainSearchFragment.addListItem(moreListItem);
		}
	}

	private void updateSearchResult(SearchResultCollection res, boolean append) {

		if (!paused && mainSearchFragment != null) {
			List<QuickSearchListItem> rows = new ArrayList<>();
			if (res != null && res.getCurrentSearchResults().size() > 0) {
				for (final SearchResult sr : res.getCurrentSearchResults()) {
					rows.add(new QuickSearchListItem(app, sr));
				}
			}
			mainSearchFragment.updateListAdapter(rows, append);
		}
	}

	public static boolean showInstance(@NonNull MainActivity mainActivity,
									   @NonNull String searchQuery,
									   @Nullable Object object,
									   boolean showCategories,
									   @Nullable LatLon latLon) {
		try {
			Bundle bundle = new Bundle();
			if (object != null) {
				bundle.putBoolean(QUICK_SEARCH_RUN_SEARCH_FIRST_TIME_KEY, true);
				String objectLocalizedName = searchQuery;

				if (object instanceof PoiCategory) {
					PoiCategory c = (PoiCategory) object;
					objectLocalizedName = c.getTranslation();

					SearchUICore searchUICore = mainActivity.getMyApplication().getSearchUICore().getCore();
					SearchPhrase phrase = searchUICore.resetPhrase(objectLocalizedName + " ");
					SearchResult sr = new SearchResult(phrase);
					sr.localeName = objectLocalizedName;
					sr.object = c;
					sr.priority = SEARCH_AMENITY_TYPE_PRIORITY;
					sr.priorityDistance = 0;
					sr.objectType = ObjectType.POI_TYPE;
					searchUICore.selectSearchResult(sr);

					bundle.putBoolean(QUICK_SEARCH_PHRASE_DEFINED_KEY, true);
				}
				searchQuery = objectLocalizedName.trim() + " ";

			} else if (!Algorithms.isEmpty(searchQuery)) {
				bundle.putBoolean(QUICK_SEARCH_RUN_SEARCH_FIRST_TIME_KEY, true);
			}

			bundle.putString(QUICK_SEARCH_QUERY_KEY, searchQuery);
			bundle.putBoolean(QUICK_SEARCH_SHOW_CATEGORIES_KEY, showCategories);
			if (latLon != null) {
				bundle.putDouble(QUICK_SEARCH_LAT_KEY, latLon.getLatitude());
				bundle.putDouble(QUICK_SEARCH_LON_KEY, latLon.getLongitude());
			}
			QuickSearchDialogFragment fragment = new QuickSearchDialogFragment();
			fragment.setArguments(bundle);
			fragment.show(mainActivity.getSupportFragmentManager(), TAG);
			return true;

		} catch (RuntimeException e) {
			return false;
		}
	}

	private MainActivity getMainActivity() {
		return (MainActivity) getActivity();
	}

	private SampleApplication getMyApplication() {
		return (SampleApplication) getActivity().getApplication();
	}

	@Override
	public void updateCompassValue(final float value) {
		// 99 in next line used to one-time initialize arrows (with reference vs. fixed-north direction)
		// on non-compass devices
		float lastHeading = heading != null ? heading : 99;
		heading = value;
		if (Math.abs(MapUtils.degreesDiff(lastHeading, heading)) > 5) {
			final Location location = this.location;
			app.runInUIThread(new Runnable() {
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
		final Float heading = this.heading;
		app.runInUIThread(new Runnable() {
			@Override
			public void run() {
				updateLocationUI(location, heading);
			}
		});
	}

	private void updateLocationUI(Location location, Float heading) {
		this.location = location;
		LatLon latLon = null;
		if (location != null) {
			latLon = new LatLon(location.getLatitude(), location.getLongitude());
		}
		updateLocationUI(latLon, heading);
	}

	private void updateLocationUI(LatLon latLon, Float heading) {
		if (latLon != null && !paused && !cancelPrev) {
			if (mainSearchFragment != null && searchView.getVisibility() == View.VISIBLE) {
				mainSearchFragment.updateLocation(latLon, heading);
			} else if (categoriesSearchFragment != null) {
				categoriesSearchFragment.updateLocation(latLon, heading);
			}
		}
	}

	private void updateUseMapCenterUI() {
		if (!paused && !cancelPrev) {
			if (mainSearchFragment != null) {
				mainSearchFragment.getListAdapter().setUseMapCenter(useMapCenter);
			}
			if (categoriesSearchFragment != null) {
				categoriesSearchFragment.getListAdapter().setUseMapCenter(useMapCenter);
			}
		}
	}

	public static class QuickSearchCategoriesListFragment extends QuickSearchListFragment {

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

	/*
	public static class QuickSearchToolbarController extends TopToolbarController {

		public QuickSearchToolbarController() {
			super(TopToolbarControllerType.QUICK_SEARCH);
		}
	}
	*/
}
