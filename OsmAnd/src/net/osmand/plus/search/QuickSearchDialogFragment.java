package net.osmand.plus.search;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.FileProvider;
import android.support.v4.view.ViewPager;
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
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.osm.AbstractPoiType;
import net.osmand.plus.AppInitializer;
import net.osmand.plus.AppInitializer.AppInitializeListener;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.LockableViewPager;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmAndLocationProvider.OsmAndCompassListener;
import net.osmand.plus.OsmAndLocationProvider.OsmAndLocationListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.MapActivity.ShowQuickSearchMode;
import net.osmand.plus.helpers.SearchHistoryHelper;
import net.osmand.plus.helpers.SearchHistoryHelper.HistoryEntry;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.search.QuickSearchHelper.SearchHistoryAPI;
import net.osmand.plus.views.mapwidgets.MapInfoWidgetsFactory.TopToolbarController;
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class QuickSearchDialogFragment extends DialogFragment implements OsmAndCompassListener, OsmAndLocationListener {

	public static final String TAG = "QuickSearchDialogFragment";
	private static final String QUICK_SEARCH_QUERY_KEY = "quick_search_query_key";
	private static final String QUICK_SEARCH_LAT_KEY = "quick_search_lat_key";
	private static final String QUICK_SEARCH_LON_KEY = "quick_search_lon_key";
	private static final String QUICK_SEARCH_INTERRUPTED_SEARCH_KEY = "quick_search_interrupted_search_key";
	private static final String QUICK_SEARCH_SHOW_CATEGORIES_KEY = "quick_search_show_categories_key";
	private static final String QUICK_SEARCH_HIDDEN_KEY = "quick_search_hidden_key";
	private static final String QUICK_SEARCH_TOOLBAR_TITLE_KEY = "quick_search_toolbar_title_key";
	private static final String QUICK_SEARCH_TOOLBAR_VISIBLE_KEY = "quick_search_toolbar_visible_key";

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
	private QuickSearchToolbarController toolbarController;

	private Toolbar toolbarEdit;
	private TextView titleEdit;

	private EditText searchEditText;
	private ProgressBar progressBar;
	private ImageButton clearButton;

	private OsmandApplication app;
	private QuickSearchHelper searchHelper;
	private SearchUICore searchUICore;
	private String searchQuery;

	private LatLon centerLatLon;
	private net.osmand.Location location = null;
	private Float heading = null;
	private boolean useMapCenter;
	private boolean paused;
	private boolean searching;
	private boolean hidden;
	private boolean foundPartialLocation;
	private String toolbarTitle;
	private boolean toolbarVisible;

	private boolean newSearch;
	private boolean interruptedSearch;
	private long hideTimeMs;

	private static final double DISTANCE_THRESHOLD = 70000; // 70km
	private static final int EXPIRATION_TIME_MIN = 10; // 10 minutes


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = getMyApplication();
		boolean isLightTheme = app.getSettings().OSMAND_THEME.get() == OsmandSettings.OSMAND_LIGHT_THEME;
		int themeId = isLightTheme ? R.style.OsmandLightTheme : R.style.OsmandDarkTheme;
		setStyle(STYLE_NO_FRAME, themeId);
	}

	@Override
	@SuppressLint("PrivateResource")
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		final MapActivity mapActivity = getMapActivity();
		final View view = inflater.inflate(R.layout.search_dialog_fragment, container, false);

		toolbarController = new QuickSearchToolbarController();
		toolbarController.setOnBackButtonClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mapActivity.showQuickSearch(ShowQuickSearchMode.CURRENT, false);
			}
		});
		toolbarController.setOnTitleClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mapActivity.showQuickSearch(ShowQuickSearchMode.CURRENT, false);
			}
		});
		toolbarController.setOnCloseButtonClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mapActivity.closeQuickSearch();
			}
		});

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

		tabToolbarView = view.findViewById(R.id.tab_toolbar_layout);
		tabsView = view.findViewById(R.id.tabs_view);
		searchView = view.findViewById(R.id.search_view);

		buttonToolbarView = view.findViewById(R.id.button_toolbar_layout);
		buttonToolbarImage = (ImageView) view.findViewById(R.id.buttonToolbarImage);
		buttonToolbarImage.setImageDrawable(app.getIconsCache().getThemedIcon(R.drawable.ic_action_marker_dark));
		buttonToolbarText = (TextView) view.findViewById(R.id.buttonToolbarTitle);
		view.findViewById(R.id.buttonToolbar).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				SearchPhrase searchPhrase = searchUICore.getPhrase();
				if (foundPartialLocation) {
					QuickSearchCoordinatesFragment.showDialog(QuickSearchDialogFragment.this, searchPhrase.getUnknownSearchWord());
				} else if (searchPhrase.isNoSelectedType() || searchPhrase.isLastWord(ObjectType.POI_TYPE)) {
					PoiUIFilter filter;
					if (searchPhrase.isNoSelectedType()) {
						filter = new PoiUIFilter(null, app, "");
						if (!Algorithms.isEmpty(searchPhrase.getUnknownSearchWord())) {
							filter.setFilterByName(searchPhrase.getUnknownSearchWord());
						}
					} else if (searchPhrase.getLastSelectedWord().getResult().object instanceof AbstractPoiType) {
						if (searchPhrase.isNoSelectedType()) {
							filter = new PoiUIFilter(null, app, "");
						} else {
							AbstractPoiType abstractPoiType = (AbstractPoiType) searchPhrase.getLastSelectedWord()
									.getResult().object;
							filter = new PoiUIFilter(abstractPoiType, app, "");
						}
						if (!Algorithms.isEmpty(searchPhrase.getUnknownSearchWord())) {
							filter.setFilterByName(searchPhrase.getUnknownSearchWord());
						}
					} else {
						filter = (PoiUIFilter) searchPhrase.getLastSelectedWord().getResult().object;
					}
					app.getPoiFilters().clearSelectedPoiFilters();
					app.getPoiFilters().addSelectedPoiFilter(filter);
					showToolbar();
					getMapActivity().refreshMap();
					hide();
				} else {
					SearchWord word = searchPhrase.getLastSelectedWord();
					if (word != null && word.getLocation() != null) {
						SearchResult searchResult = word.getResult();
						String name = QuickSearchListItem.getName(app, searchResult);
						String typeName = QuickSearchListItem.getTypeName(app, searchResult);
						PointDescription pointDescription = new PointDescription(PointDescription.POINT_TYPE_ADDRESS, typeName, name);
						app.getSettings().setMapLocationToShow(
								searchResult.location.getLatitude(), searchResult.location.getLongitude(),
								searchResult.preferredZoom, pointDescription, true, searchResult.object);

						showToolbar();
						MapActivity.launchMapActivityMoveToTop(getActivity());
						reloadHistory();
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
		if (centerLatLon != null || showCategories) {
			viewPager.setCurrentItem(1);
		}

		tabLayout = (TabLayout) view.findViewById(R.id.tab_layout);
		tabLayout.setupWithViewPager(viewPager);
		viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
			}

			@Override
			public void onPageSelected(int position) {
				hideKeyboard();
			}

			@Override
			public void onPageScrollStateChanged(int state) {
			}
		});

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
				updateClearButtonVisibility(true);
				updateTabbarVisibility(newQueryText.length() == 0);
				if (!searchQuery.equalsIgnoreCase(newQueryText)) {
					searchQuery = newQueryText;
					if (Algorithms.isEmpty(searchQuery)) {
						searchUICore.resetPhrase();
					} else {
						runSearch();
					}
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
				} else if (useMapCenter && location != null) {
					useMapCenter = false;
					centerLatLon = null;
					updateUseMapCenterUI();
					startLocationUpdate();
					LatLon centerLatLon = new LatLon(location.getLatitude(), location.getLongitude());
					SearchSettings ss = searchUICore.getSearchSettings().setOriginalLocation(
							new LatLon(centerLatLon.getLatitude(), centerLatLon.getLongitude()));
					searchUICore.updateSettings(ss);
					updateClearButtonAndHint();
					updateClearButtonVisibility(true);
				}
				updateToolbarButton();
			}
		});

		setupSearch(mapActivity);

		return view;
	}

	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		updateToolbarButton();
		updateClearButtonAndHint();
		updateClearButtonVisibility(true);
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
		toolbarController.setTitle(toolbarTitle);
		getMapActivity().showTopToolbar(toolbarController);
	}

	public void hideToolbar() {
		toolbarVisible = false;
		getMapActivity().hideTopToolbar(toolbarController);
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
			LatLon mapCenter = getMapActivity().getMapView().getCurrentRotatedTileBox().getCenterLatLon();
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
		app.getPoiFilters().clearSelectedPoiFilters();
		dismiss();
	}

	public void addMainSearchFragment() {
		mainSearchFragment = (QuickSearchMainListFragment) Fragment.instantiate(this.getContext(), QuickSearchMainListFragment.class.getName());
		FragmentManager childFragMan = getChildFragmentManager();
		FragmentTransaction childFragTrans = childFragMan.beginTransaction();
		childFragTrans.replace(R.id.search_view, mainSearchFragment);
		childFragTrans.commit();
	}

	private void updateToolbarButton() {
		if (foundPartialLocation) {
			buttonToolbarText.setText(app.getString(R.string.advanced_coords_search).toUpperCase());
		} else if (searchEditText.getText().length() > 0) {
			SearchWord word = searchUICore.getPhrase().getLastSelectedWord();
			if (word != null && word.getResult() != null) {
				buttonToolbarText.setText(app.getString(R.string.show_something_on_map, word.getResult().localeName).toUpperCase());
			} else {
				buttonToolbarText.setText(app.getString(R.string.shared_string_show_on_map).toUpperCase());
			}
		} else {
			buttonToolbarText.setText(app.getString(R.string.shared_string_show_on_map).toUpperCase());
		}
	}

	private void setupSearch(final MapActivity mapActivity) {
		// Setup search core
		String locale = app.getSettings().MAP_PREFERRED_LOCALE.get();
		searchHelper = app.getSearchUICore();
		searchUICore = searchHelper.getCore();
		if (newSearch) {
			setResultCollection(null);
			searchUICore.resetPhrase();
		}

		location = app.getLocationProvider().getLastKnownLocation();

		LatLon searchLatLon;
		if (centerLatLon == null) {
			LatLon clt = mapActivity.getMapView().getCurrentRotatedTileBox().getCenterLatLon();
			searchLatLon = clt;
			searchEditText.setHint(R.string.search_poi_category_hint);
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
		settings = settings.setLang(locale, false);
		searchUICore.updateSettings(settings);
		searchUICore.setOnResultsComplete(new Runnable() {
			@Override
			public void run() {
				app.runInUIThread(new Runnable() {
					@Override
					public void run() {
						searching = false;
						if (!paused) {
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
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
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
			clearButton.setVisibility(searchEditText.length() > 0 || (useMapCenter && location != null) ? View.VISIBLE : View.GONE);
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

	public void setResultCollection(SearchResultCollection resultCollection) {
		searchHelper.setResultCollection(resultCollection);
	}

	public SearchResultCollection getResultCollection() {
		return searchHelper.getResultCollection();
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
		LatLon mapCenter = getMapActivity().getMapView().getCurrentRotatedTileBox().getCenterLatLon();
		if (useMapCenter) {
			updateUseMapCenterUI();
			searchListFragment.updateLocation(mapCenter, null);
		}
	}

	public void reloadCategories() {
		if (app.isApplicationInitializing()) {
			showProgressBar();
			app.getAppInitializer().addListener(new AppInitializeListener() {
				@Override
				public void onProgress(AppInitializer init, AppInitializer.InitEvents event) {
				}

				@Override
				public void onFinish(AppInitializer init) {
					reloadCategoriesInternal();
					if (!searching) {
						hideProgressBar();
					}
				}
			});
		} else {
			reloadCategoriesInternal();
		}
	}

	private void reloadCategoriesInternal() {
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
			app.showToastMessage(e.getMessage());
		}

	}

	public void reloadHistory() {
		if (app.isApplicationInitializing()) {
			showProgressBar();
			app.getAppInitializer().addListener(new AppInitializeListener() {
				@Override
				public void onProgress(AppInitializer init, AppInitializer.InitEvents event) {
				}

				@Override
				public void onFinish(AppInitializer init) {
					reloadHistoryInternal();
					if (!searching) {
						hideProgressBar();
					}
				}
			});
		} else {
			reloadHistoryInternal();
		}
	}

	private void reloadHistoryInternal() {
		try {
			SearchResultCollection res = searchUICore.shallowSearch(SearchHistoryAPI.class,
					"", null);
			List<QuickSearchListItem> rows = new ArrayList<>();
			if (res != null) {
				for (SearchResult sr : res.getCurrentSearchResults()) {
					rows.add(new QuickSearchListItem(app, sr));
				}
			}
			historySearchFragment.updateListAdapter(rows, false);
		} catch (IOException e) {
			e.printStackTrace();
			app.showToastMessage(e.getMessage());
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
		foundPartialLocation = false;
		updateToolbarButton();
		interruptedSearch = false;
		searching = true;

		if (app.isApplicationInitializing() && text.length() > 0) {
			app.getAppInitializer().addListener(new AppInitializeListener() {
				@Override
				public void onProgress(AppInitializer init, AppInitializer.InitEvents event) {
				}

				@Override
				public void onFinish(AppInitializer init) {
					runCoreSearchInternal(text, updateResult, searchMore);
				}
			});
		} else {
			runCoreSearchInternal(text, updateResult, searchMore);
		}
	}

	private void runCoreSearchInternal(String text, boolean updateResult, boolean searchMore) {
		SearchResultCollection c = searchUICore.search(text, new ResultMatcher<SearchResult>() {
			SearchResultCollection regionResultCollection = null;
			SearchCoreAPI regionResultApi = null;
			List<SearchResult> results = new ArrayList<>();

			@Override
			public boolean publish(SearchResult object) {
				if (paused) {
					if (results.size() > 0) {
						getResultCollection().addSearchResults(results, true, true);
					}
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
						showLocationToolbar();
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

		if (!searchMore) {
			setResultCollection(null);
			updateSearchResult(null, false);
		}
		if (updateResult) {
			updateSearchResult(c, false);
		}
	}

	private void showLocationToolbar() {
		app.runInUIThread(new Runnable() {
			@Override
			public void run() {
				foundPartialLocation = true;
				updateToolbarButton();
			}
		});
	}

	private void showApiResults(final List<SearchResult> apiResults, final SearchPhrase phrase,
								final boolean hasRegionCollection) {
		app.runInUIThread(new Runnable() {
			@Override
			public void run() {
				if (!paused) {
					boolean append = getResultCollection() != null;
					if (append) {
						getResultCollection().addSearchResults(apiResults, false, true);
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
				if (!paused) {
					if (getResultCollection() != null) {
						SearchResultCollection resCollection =
								getResultCollection().combineWithCollection(regionResultCollection, false, true);
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
				new QuickSearchMoreListItem(app, app.getString(R.string.search_POI_level_btn).toUpperCase(), new OnClickListener() {
					@Override
					public void onClick(View v) {
						if (!interruptedSearch) {
							SearchSettings settings = searchUICore.getPhrase().getSettings();
							searchUICore.updateSettings(settings.setRadiusLevel(settings.getRadiusLevel() + 1));
						}
						runCoreSearch(searchQuery, false, true);
					}
				});

		if (!paused && mainSearchFragment != null) {
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

	public static boolean showInstance(final MapActivity mapActivity, final String searchQuery,
									   boolean showCategories, final LatLon latLon) {
		try {

			if (mapActivity.isActivityDestroyed()) {
				return false;
			}

			Bundle bundle = new Bundle();
			bundle.putString(QUICK_SEARCH_QUERY_KEY, searchQuery);
			bundle.putBoolean(QUICK_SEARCH_SHOW_CATEGORIES_KEY, showCategories);
			if (latLon != null) {
				bundle.putDouble(QUICK_SEARCH_LAT_KEY, latLon.getLatitude());
				bundle.putDouble(QUICK_SEARCH_LON_KEY, latLon.getLongitude());
			}
			QuickSearchDialogFragment fragment = new QuickSearchDialogFragment();
			fragment.setArguments(bundle);
			fragment.show(mapActivity.getSupportFragmentManager(), TAG);
			return true;

		} catch (RuntimeException e) {
			return false;
		}
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
		this.location = location;
		final Float heading = this.heading;
		app.runInUIThread(new Runnable() {
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
			if (mainSearchFragment != null && searchView.getVisibility() == View.VISIBLE) {
				mainSearchFragment.updateLocation(latLon, heading);
			} else if (historySearchFragment != null && viewPager.getCurrentItem() == 0) {
				historySearchFragment.updateLocation(latLon, heading);
			} else if (categoriesSearchFragment != null && viewPager.getCurrentItem() == 1) {
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

	public void enableSelectionMode(boolean selectionMode, int position) {
		historySearchFragment.setSelectionMode(selectionMode, position);
		tabToolbarView.setVisibility(selectionMode ? View.GONE : View.VISIBLE);
		buttonToolbarView.setVisibility(View.GONE);
		toolbar.setVisibility(selectionMode ? View.GONE : View.VISIBLE);
		toolbarEdit.setVisibility(selectionMode ? View.VISIBLE : View.GONE);
		viewPager.setSwipeLocked(selectionMode);
	}

	public void updateSelectionMode(List<QuickSearchListItem> selectedItems) {
		if (selectedItems.size() > 0) {
			String text = selectedItems.size() + " " + app.getString(R.string.shared_string_selected_lowercase);
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
					GPXUtilities.writeGpxFile(dst, gpxFile, app);

					final Intent sendIntent = new Intent();
					sendIntent.setAction(Intent.ACTION_SEND);
					sendIntent.putExtra(Intent.EXTRA_TEXT, "History.gpx:\n\n\n" + GPXUtilities.asString(gpxFile, app));
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

	public static class QuickSearchToolbarController extends TopToolbarController {

		public QuickSearchToolbarController() {
			super(TopToolbarControllerType.QUICK_SEARCH);
		}
	}
}
