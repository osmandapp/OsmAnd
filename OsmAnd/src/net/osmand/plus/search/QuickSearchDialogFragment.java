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
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import net.osmand.AndroidUtils;
import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.access.AccessibilityAssistant;
import net.osmand.access.NavigationInfo;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.City;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiType;
import net.osmand.plus.AppInitializer;
import net.osmand.plus.AppInitializer.AppInitializeListener;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.plus.LockableViewPager;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmAndLocationProvider.OsmAndCompassListener;
import net.osmand.plus.OsmAndLocationProvider.OsmAndLocationListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.MapActivity.ShowQuickSearchMode;
import net.osmand.plus.helpers.SearchHistoryHelper;
import net.osmand.plus.helpers.SearchHistoryHelper.HistoryEntry;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.resources.RegionAddressRepository;
import net.osmand.plus.search.QuickSearchHelper.SearchHistoryAPI;
import net.osmand.plus.search.listitems.QuickSearchButtonListItem;
import net.osmand.plus.search.listitems.QuickSearchHeaderListItem;
import net.osmand.plus.search.listitems.QuickSearchListItem;
import net.osmand.plus.search.listitems.QuickSearchMoreListItem;
import net.osmand.plus.search.listitems.QuickSearchMoreListItem.SearchMoreItemOnClickListener;
import net.osmand.plus.views.mapwidgets.MapInfoWidgetsFactory.TopToolbarController;
import net.osmand.plus.views.mapwidgets.MapInfoWidgetsFactory.TopToolbarControllerType;
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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import static net.osmand.plus.search.SendSearchQueryBottomSheet.MISSING_SEARCH_LOCATION_KEY;
import static net.osmand.plus.search.SendSearchQueryBottomSheet.MISSING_SEARCH_QUERY_KEY;
import static net.osmand.search.core.ObjectType.POI_TYPE;
import static net.osmand.search.core.ObjectType.SEARCH_STARTED;
import static net.osmand.search.core.SearchCoreFactory.SEARCH_AMENITY_TYPE_PRIORITY;

public class QuickSearchDialogFragment extends DialogFragment implements OsmAndCompassListener, OsmAndLocationListener {

	private static final org.apache.commons.logging.Log LOG = PlatformUtil.getLog(QuickSearchDialogFragment.class);

	public static final String TAG = "QuickSearchDialogFragment";
	private static final String QUICK_SEARCH_QUERY_KEY = "quick_search_query_key";
	private static final String QUICK_SEARCH_LAT_KEY = "quick_search_lat_key";
	private static final String QUICK_SEARCH_LON_KEY = "quick_search_lon_key";
	private static final String QUICK_SEARCH_INTERRUPTED_SEARCH_KEY = "quick_search_interrupted_search_key";
	private static final String QUICK_SEARCH_HIDDEN_KEY = "quick_search_hidden_key";
	private static final String QUICK_SEARCH_TOOLBAR_TITLE_KEY = "quick_search_toolbar_title_key";
	private static final String QUICK_SEARCH_TOOLBAR_VISIBLE_KEY = "quick_search_toolbar_visible_key";
	private static final String QUICK_SEARCH_FAB_VISIBLE_KEY = "quick_search_fab_visible_key";

	private static final String QUICK_SEARCH_RUN_SEARCH_FIRST_TIME_KEY = "quick_search_run_search_first_time_key";
	private static final String QUICK_SEARCH_PHRASE_DEFINED_KEY = "quick_search_phrase_defined_key";

	private static final String QUICK_SEARCH_SHOW_TAB_KEY = "quick_search_show_tab_key";
	private static final String QUICK_SEARCH_TYPE_KEY = "quick_search_type_key";

	private Toolbar toolbar;
	private LockableViewPager viewPager;
	private SearchFragmentPagerAdapter pagerAdapter;
	private TabLayout tabLayout;
	private View tabToolbarView;
	private View tabsView;
	private View searchView;
	private View buttonToolbarView;
	private View sendEmptySearchView;
	private ImageView buttonToolbarImage;
	private ImageButton buttonToolbarFilter;
	private TextView buttonToolbarText;
	private TextView sendEmptySearchText;
	private FrameLayout sendEmptySearchButton;
	private QuickSearchMainListFragment mainSearchFragment;
	private QuickSearchHistoryListFragment historySearchFragment;
	private QuickSearchCategoriesListFragment categoriesSearchFragment;
	private QuickSearchAddressListFragment addressSearchFragment;
	private QuickSearchToolbarController toolbarController;

	private Toolbar toolbarEdit;
	private TextView titleEdit;
	private View fab;

	private EditText searchEditText;
	private ProgressBar progressBar;
	private ImageButton clearButton;

	private AccessibilityAssistant accessibilityAssistant;
	private NavigationInfo navigationInfo;

	private OsmandApplication app;
	private QuickSearchHelper searchHelper;
	private SearchUICore searchUICore;
	private SearchResultListener defaultResultListener;
	private String searchQuery;

	private LatLon centerLatLon;
	private net.osmand.Location location = null;
	private Float heading = null;
	private boolean useMapCenter;
	private boolean paused;
	private boolean cancelPrev;
	private boolean searching;
	private boolean hidden;
	private boolean foundPartialLocation;
	private String toolbarTitle;
	private boolean toolbarVisible;

	private boolean newSearch;
	private boolean interruptedSearch;
	private boolean pausedSearch;
	private long hideTimeMs;
	private boolean expired;
	private boolean poiFilterApplied;
	private boolean fabVisible;
	private boolean sendEmptySearchBottomBarVisible;
	private boolean runSearchFirstTime;
	private boolean phraseDefined;
	private boolean addressSearch;
	private boolean citiesLoaded;
	private LatLon storedOriginalLocation;

	private QuickSearchType searchType = QuickSearchType.REGULAR;

	private static final double DISTANCE_THRESHOLD = 70000; // 70km
	private static final int EXPIRATION_TIME_MIN = 10; // 10 minutes

	public enum QuickSearchTab {
		HISTORY,
		CATEGORIES,
		ADDRESS,
	}

	public enum QuickSearchType {
		REGULAR,
		START_POINT,
		DESTINATION,
		DESTINATION_AND_START,
		INTERMEDIATE,
		HOME_POINT,
		WORK_POINT;

		public boolean isTargetPoint() {
			return this != QuickSearchType.REGULAR;
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = getMyApplication();
		navigationInfo = new NavigationInfo(app);
		accessibilityAssistant = new AccessibilityAssistant(getActivity());
		boolean isLightTheme = app.getSettings().OSMAND_THEME.get() == OsmandSettings.OSMAND_LIGHT_THEME;
		int themeId = isLightTheme ? R.style.OsmandLightTheme : R.style.OsmandDarkTheme;
		setStyle(STYLE_NO_FRAME, themeId);
	}

	@Override
	@SuppressLint("PrivateResource, ValidFragment")
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
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
			searchType = QuickSearchType.valueOf(savedInstanceState.getString(QUICK_SEARCH_TYPE_KEY, QuickSearchType.REGULAR.name()));
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
			fabVisible = savedInstanceState.getBoolean(QUICK_SEARCH_FAB_VISIBLE_KEY, false);
		}
		if (searchQuery == null && arguments != null) {
			searchType = QuickSearchType.valueOf(arguments.getString(QUICK_SEARCH_TYPE_KEY, QuickSearchType.REGULAR.name()));
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

		QuickSearchTab showSearchTab = QuickSearchTab.HISTORY;
		if (arguments != null) {
			showSearchTab = QuickSearchTab.valueOf(arguments.getString(QUICK_SEARCH_SHOW_TAB_KEY, QuickSearchTab.HISTORY.name()));
		}
		if (showSearchTab == QuickSearchTab.ADDRESS) {
			addressSearch = true;
		}

		tabToolbarView = view.findViewById(R.id.tab_toolbar_layout);
		tabsView = view.findViewById(R.id.tabs_view);
		searchView = view.findViewById(R.id.search_view);

		buttonToolbarView = view.findViewById(R.id.button_toolbar_layout);
		buttonToolbarImage = (ImageView) view.findViewById(R.id.buttonToolbarImage);
		buttonToolbarImage.setImageDrawable(app.getUIUtilities().getThemedIcon(R.drawable.ic_action_marker_dark));
		buttonToolbarFilter = (ImageButton) view.findViewById(R.id.filterButton);
		buttonToolbarFilter.setImageDrawable(app.getUIUtilities().getThemedIcon(R.drawable.ic_action_filter));
		buttonToolbarFilter.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				SearchPhrase searchPhrase = searchUICore.getPhrase();
				if (searchPhrase.isLastWord(POI_TYPE)) {
					String filterId = null;
					String filterByName = searchPhrase.getUnknownSearchPhrase().trim();
					Object object = searchPhrase.getLastSelectedWord().getResult().object;
					if (object instanceof PoiUIFilter) {
						PoiUIFilter model = (PoiUIFilter) object;
						if (!Algorithms.isEmpty(model.getSavedFilterByName())) {
							model.setFilterByName(model.getSavedFilterByName());
						}
						filterId = model.getFilterId();
					} else if (object instanceof AbstractPoiType) {
						AbstractPoiType abstractPoiType = (AbstractPoiType) object;
						PoiUIFilter custom = app.getPoiFilters().getFilterById(PoiUIFilter.STD_PREFIX + abstractPoiType.getKeyName());
						if (custom != null) {
							custom.setFilterByName(null);
							custom.clearFilter();
							custom.updateTypesToAccept(abstractPoiType);
							filterId = custom.getFilterId();
						}
					}
					if (filterId != null) {
						QuickSearchPoiFilterFragment.showDialog(
								QuickSearchDialogFragment.this, filterByName, filterId);
					}
				}
			}
		});

		buttonToolbarText = (TextView) view.findViewById(R.id.buttonToolbarTitle);
		view.findViewById(R.id.buttonToolbar).setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View v) {
						cancelSearch();
						SearchPhrase searchPhrase = searchUICore.getPhrase();
						if (foundPartialLocation) {
							QuickSearchCoordinatesFragment.showDialog(QuickSearchDialogFragment.this, searchPhrase.getUnknownSearchWord());
						} else if (searchPhrase.isNoSelectedType() || searchPhrase.isLastWord(POI_TYPE)) {
							PoiUIFilter filter;
							if (searchPhrase.isNoSelectedType()) {
								if (isOnlineSearch() && !Algorithms.isEmpty(searchPhrase.getUnknownSearchWord())) {
									app.getPoiFilters().resetNominatimFilters();
									filter = app.getPoiFilters().getNominatimPOIFilter();
									filter.setFilterByName(searchPhrase.getUnknownSearchPhrase());
									filter.clearCurrentResults();
								} else if (searchPhrase.hasUnknownSearchWordPoiType()) {
									AbstractPoiType pt = searchPhrase.getUnknownSearchWordPoiType();
									filter = new PoiUIFilter(pt, app, "");
									String customName = searchPhrase.getPoiNameFilter();
									if (!Algorithms.isEmpty(customName)) {
										filter.setFilterByName(customName);
									}
								} else {
									filter = app.getPoiFilters().getSearchByNamePOIFilter();
									if (!Algorithms.isEmpty(searchPhrase.getUnknownSearchWord())) {
										filter.setFilterByName(searchPhrase.getUnknownSearchWord());
										filter.clearCurrentResults();
									}
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

							mapActivity.getContextMenu().closeActiveToolbar();
							showToolbar();
							getMapActivity().updateStatusBarColor();
							getMapActivity().refreshMap();
							hide();
						} else {
							SearchWord word = searchPhrase.getLastSelectedWord();
							if (word != null) {
								if (searchType.isTargetPoint() && word.getLocation() != null) {
									if (mainSearchFragment != null) {
										mainSearchFragment.showResult(word.getResult());
									}
								} else if (word.getLocation() != null) {
									SearchResult searchResult = word.getResult();
									String name = QuickSearchListItem.getName(app, searchResult);
									String typeName = QuickSearchListItem.getTypeName(app, searchResult);
									PointDescription pointDescription = new PointDescription(PointDescription.POINT_TYPE_ADDRESS, typeName, name);
									app.getSettings().setMapLocationToShow(
											searchResult.location.getLatitude(), searchResult.location.getLongitude(),
											searchResult.preferredZoom, pointDescription, true, searchResult.object);

									hideToolbar();
									MapActivity.launchMapActivityMoveToTop(getActivity());
									reloadHistory();
									hide();
								} else if (word.getType() == ObjectType.FAVORITE_GROUP) {
									FavouritesDbHelper.FavoriteGroup group = (FavouritesDbHelper.FavoriteGroup) word.getResult().object;
									if (group.points.size() > 1) {
										double left = 0, right = 0;
										double top = 0, bottom = 0;
										for (FavouritePoint p : group.points) {
											if (left == 0) {
												left = p.getLongitude();
												right = p.getLongitude();
												top = p.getLatitude();
												bottom = p.getLatitude();
											} else {
												left = Math.min(left, p.getLongitude());
												right = Math.max(right, p.getLongitude());
												top = Math.max(top, p.getLatitude());
												bottom = Math.min(bottom, p.getLatitude());
											}
										}
										getMapActivity().getMapView().fitRectToMap(left, right, top, bottom, 0, 0, 0);
										hideToolbar();
										MapActivity.launchMapActivityMoveToTop(getActivity());
										hide();
									} else if (group.points.size() == 1) {
										FavouritePoint p = group.points.get(0);
										app.getSettings().setMapLocationToShow(p.getLatitude(), p.getLongitude(), word.getResult().preferredZoom);
										hideToolbar();
										MapActivity.launchMapActivityMoveToTop(getActivity());
										hide();
									}
								}
							}
						}
					}
				}

		);

		toolbar = (Toolbar) view.findViewById(R.id.toolbar);
		if (!app.getSettings().isLightContent()) {
			toolbar.setBackgroundColor(ContextCompat.getColor(mapActivity, R.color.app_bar_color_dark));
		}
		toolbar.setNavigationIcon(app.getUIUtilities().getThemedIcon(R.drawable.ic_arrow_back));
		toolbar.setNavigationContentDescription(R.string.access_shared_string_navigate_up);
		toolbar.setNavigationOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View v) {
						if (!processBackAction()) {
							dismiss();
						}
					}
				}
		);

		toolbarEdit = (Toolbar) view.findViewById(R.id.toolbar_edit);
		toolbarEdit.setNavigationIcon(app.getUIUtilities().getIcon(R.drawable.ic_action_remove_dark));
		toolbarEdit.setNavigationContentDescription(R.string.shared_string_cancel);
		toolbarEdit.setNavigationOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View v) {
						enableSelectionMode(false, -1);
					}
				}
		);

		titleEdit = (TextView) view.findViewById(R.id.titleEdit);
		view.findViewById(R.id.shareButton).setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View v) {
						List<HistoryEntry> historyEntries = new ArrayList<HistoryEntry>();
						List<QuickSearchListItem> selectedItems = historySearchFragment.getListAdapter().getSelectedItems();
						for (QuickSearchListItem searchListItem : selectedItems) {
							Object object = searchListItem.getSearchResult().object;
							if (object instanceof HistoryEntry) {
								historyEntries.add((HistoryEntry) object);
							}
						}
						if (historyEntries.size() > 0) {
							shareHistory(historyEntries);
							enableSelectionMode(false, -1);
						}
					}
				}
		);
		view.findViewById(R.id.deleteButton).setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View v) {
						DeleteDialogFragment deleteDialog = new DeleteDialogFragment();
						deleteDialog.setSelectedItems(historySearchFragment.getListAdapter().getSelectedItems());
						deleteDialog.show(getChildFragmentManager(), "DeleteHistoryConfirmationFragment");
					}
				}
		);

		viewPager = (LockableViewPager) view.findViewById(R.id.pager);
		viewPager.setOffscreenPageLimit(2);
		pagerAdapter = new SearchFragmentPagerAdapter(getChildFragmentManager(), getResources());
		viewPager.setAdapter(pagerAdapter);
		switch (showSearchTab) {
			case HISTORY:
				viewPager.setCurrentItem(0);
				break;
			case CATEGORIES:
				viewPager.setCurrentItem(1);
				break;
			case ADDRESS:
				viewPager.setCurrentItem(2);
				break;
		}

		tabLayout = (TabLayout) view.findViewById(R.id.tab_layout);
		tabLayout.setupWithViewPager(viewPager);
		viewPager.addOnPageChangeListener(
				new ViewPager.OnPageChangeListener() {
					@Override
					public void onPageScrolled(int position, float positionOffset,
											   int positionOffsetPixels) {
					}

					@Override
					public void onPageSelected(int position) {
						hideKeyboard();
						addressSearch = position == 2;
						updateClearButtonAndHint();
						if (addressSearch && !citiesLoaded) {
							reloadCities();
						} else {
							restoreSearch();
						}
					}

					@Override
					public void onPageScrollStateChanged(int state) {
					}
				}
		);

		searchEditText = (EditText) view.findViewById(R.id.searchEditText);
		searchEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_SEARCH) {
					String newQueryText = searchQuery + " ";
					searchEditText.setText(newQueryText);
					searchEditText.setSelection(newQueryText.length());
					AndroidUtils.hideSoftKeyboard(getActivity(), searchEditText);
					return true;
				}
				return false;
			}
		});
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
						updateTabbarVisibility(textEmpty && !isOnlineSearch());
						updateSendEmptySearchBottomBar(false);
						if (textEmpty) {
							if (addressSearch) {
								startAddressSearch();
							}
							if (poiFilterApplied) {
								poiFilterApplied = false;
								reloadCategories();
								if (fabVisible) {
									fabVisible = false;
									updateFab();
								}
							}
						}
						if (!searchQuery.equalsIgnoreCase(newQueryText)) {
							searchQuery = newQueryText;
							if (Algorithms.isEmpty(searchQuery)) {
								cancelSearch();
								setResultCollection(null);
								searchUICore.resetPhrase();
								mainSearchFragment.getAdapter().clear();
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
		clearButton.setImageDrawable(app.getUIUtilities().getThemedIcon(R.drawable.ic_action_remove_dark));
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
						updateSendEmptySearchBottomBar(false);
						updateToolbarButton();
					}
				}
		);

		fab = view.findViewById(R.id.fab);
		fab.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				saveCustomFilter();
			}
		});
		updateFab();

		setupSearch(mapActivity);

		sendEmptySearchView = view.findViewById(R.id.no_search_results_bottom_bar);
		sendEmptySearchText = view.findViewById(R.id.no_search_results_description);
		sendEmptySearchButton = view.findViewById(R.id.send_empty_search_button);
		sendEmptySearchButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				OsmandApplication app = getMyApplication();
				if (app != null) {
					if (!app.getSettings().isInternetConnectionAvailable()) {
						Toast.makeText(app, R.string.internet_not_available, Toast.LENGTH_LONG).show();
					} else {
						if (searchQuery != null) {
							Bundle args = new Bundle();
							SendSearchQueryBottomSheet fragment = new SendSearchQueryBottomSheet();
							args.putString(MISSING_SEARCH_LOCATION_KEY, String.valueOf(location));
							args.putString(MISSING_SEARCH_QUERY_KEY, searchQuery);
							fragment.setArguments(args);
							fragment.show(mapActivity.getSupportFragmentManager(), SendSearchQueryBottomSheet.TAG);
						}
					}
				}
			}
		});
		updateFab();

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
			openKeyboard();
		}
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Dialog dialog = new Dialog(getActivity(), getTheme()){
			@Override
			public void onBackPressed() {
				if (!processBackAction()) {
					cancel();
				}
			}
		};
		if (!getMyApplication().getSettings().DO_NOT_USE_ANIMATIONS.get()) {
			dialog.getWindow().getAttributes().windowAnimations = R.style.Animations_Alpha;
		}
		return dialog;
	}

	public void saveCustomFilter() {
		final OsmandApplication app = getMyApplication();
		final PoiUIFilter filter = app.getPoiFilters().getCustomPOIFilter();
		AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
		builder.setTitle(R.string.access_hint_enter_name);

		final EditText editText = new EditText(getContext());
		editText.setHint(R.string.new_filter);

		final TextView textView = new TextView(getContext());
		textView.setText(app.getString(R.string.new_filter_desc));
		textView.setTextAppearance(getContext(), R.style.TextAppearance_ContextMenuSubtitle);
		LinearLayout ll = new LinearLayout(getContext());
		ll.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
		ll.setOrientation(LinearLayout.VERTICAL);
		ll.setPadding(AndroidUtils.dpToPx(getContext(), 20f), AndroidUtils.dpToPx(getContext(), 12f), AndroidUtils.dpToPx(getContext(), 20f), AndroidUtils.dpToPx(getContext(), 12f));
		ll.addView(editText, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
		textView.setPadding(AndroidUtils.dpToPx(getContext(), 4f), AndroidUtils.dpToPx(getContext(), 6f), AndroidUtils.dpToPx(getContext(), 4f), AndroidUtils.dpToPx(getContext(), 4f));
		ll.addView(textView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

		builder.setView(ll);

		builder.setNegativeButton(R.string.shared_string_cancel, null);
		builder.setPositiveButton(R.string.shared_string_save, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				PoiUIFilter nFilter = new PoiUIFilter(editText.getText().toString(), null, filter.getAcceptedTypes(), app);
				if (!Algorithms.isEmpty(filter.getFilterByName())) {
					nFilter.setSavedFilterByName(filter.getFilterByName());
				}
				if (app.getPoiFilters().createPoiFilter(nFilter, false)) {
					Toast.makeText(getContext(), MessageFormat.format(getContext().getText(R.string.edit_filter_create_message).toString(),
							editText.getText().toString()), Toast.LENGTH_SHORT).show();
					app.getSearchUICore().refreshCustomPoiFilters();
					replaceQueryWithUiFilter(nFilter, "");
					reloadCategories();

					fabVisible = false;
					updateFab();
				}
			}
		});
		builder.create().show();
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

	public QuickSearchType getSearchType() {
		return searchType;
	}

	public String getText() {
		return searchEditText.getText().toString();
	}

	public boolean isTextEmpty() {
		return Algorithms.isEmpty(getText());
	}

	public AccessibilityAssistant getAccessibilityAssistant() {
		return accessibilityAssistant;
	}

	public NavigationInfo getNavigationInfo() {
		return navigationInfo;
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
		return expired || (hideTimeMs > 0 && System.currentTimeMillis() - hideTimeMs > EXPIRATION_TIME_MIN * 60 * 1000);
	}

	public void show() {
		Dialog dialog = getDialog();
		if (dialog == null) {
			return;
		}
		if (useMapCenter && getMapActivity() != null) {
			LatLon mapCenter = getMapActivity().getMapView().getCurrentRotatedTileBox().getCenterLatLon();
			SearchSettings ss = searchUICore.getSearchSettings().setOriginalLocation(
					new LatLon(mapCenter.getLatitude(), mapCenter.getLongitude()));
			searchUICore.updateSettings(ss);
			updateUseMapCenterUI();
			updateLocationUI(mapCenter, null);
		}
		app.getLocationProvider().removeCompassListener(app.getLocationProvider().getNavigationInfo());
		dialog.show();
		paused = false;
		cancelPrev = false;
		hidden = false;
		if (interruptedSearch) {
			addMoreButton(true);
			interruptedSearch = false;
		}
	}

	public void hide() {
		paused = true;
		hidden = true;
		expired = searchType != QuickSearchType.REGULAR;
		hideTimeMs = System.currentTimeMillis();
		interruptedSearch = searching;
		searching = false;
		hideProgressBar();
		updateClearButtonVisibility(true);
		Dialog dialog = getDialog();
		if (dialog != null) {
			dialog.hide();
		}
		app.getLocationProvider().addCompassListener(app.getLocationProvider().getNavigationInfo());
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
		SearchWord word = searchUICore.getPhrase().getLastSelectedWord();
		if (foundPartialLocation) {
			buttonToolbarText.setText(app.getString(R.string.advanced_coords_search).toUpperCase());
		} else if (searchEditText.getText().length() > 0) {
			if (searchType.isTargetPoint()) {
				if (word != null && word.getResult() != null) {
					buttonToolbarText.setText(app.getString(R.string.shared_string_select).toUpperCase() + " " + word.getResult().localeName.toUpperCase());
				} else {
					buttonToolbarText.setText(app.getString(R.string.shared_string_select).toUpperCase());
				}
			} else {
				if (word != null && word.getResult() != null) {
					buttonToolbarText.setText(app.getString(R.string.show_something_on_map, word.getResult().localeName).toUpperCase());
				} else {
					buttonToolbarText.setText(app.getString(R.string.shared_string_show_on_map).toUpperCase());
				}
			}
		} else {
			buttonToolbarText.setText(app.getString(R.string.shared_string_show_on_map).toUpperCase());
		}
		boolean filterButtonVisible = word != null && word.getType() != null && word.getType().equals(POI_TYPE);
		buttonToolbarFilter.setVisibility(filterButtonVisible ? View.VISIBLE : View.GONE);
		if (filterButtonVisible) {
			if (word.getResult().object instanceof PoiUIFilter) {
				buttonToolbarFilter.setImageDrawable(app.getUIUtilities().getIcon(R.drawable.ic_action_filter,
						app.getSettings().isLightContent() ? R.color.active_color_primary_light : R.color.active_color_primary_dark));
			} else{
				buttonToolbarFilter.setImageDrawable(app.getUIUtilities().getThemedIcon(R.drawable.ic_action_filter));
			}
		}
	}

	private void setupSearch(final MapActivity mapActivity) {
		// Setup search core
		String locale = app.getSettings().MAP_PREFERRED_LOCALE.get();
		boolean transliterate = app.getSettings().MAP_TRANSLITERATE_NAMES.get();
		searchHelper = app.getSearchUICore();
		searchUICore = searchHelper.getCore();
		defaultResultListener = new SearchResultListener() {
			@Override
			public void publish(SearchResultCollection res, boolean append) {
				updateSearchResult(res, append);
			}

			@Override
			public void searchStarted(SearchPhrase phrase) {
			}

			@Override
			public boolean searchFinished(SearchPhrase phrase) {
				SearchWord lastSelectedWord = phrase.getLastSelectedWord();
				if (mainSearchFragment != null && mainSearchFragment.isShowResult() &&
						isResultEmpty() && lastSelectedWord != null) {
					mainSearchFragment.showResult(lastSelectedWord.getResult());
				}
				return true;
			}
		};
		stopAddressSearch();

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
		settings = settings.setLang(locale, transliterate);
		searchUICore.updateSettings(settings);

		if (newSearch) {
			setResultCollection(null);
			if (!phraseDefined) {
				searchUICore.resetPhrase();
			}
			phraseDefined = false;
		}
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setShowsDialog(true);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putString(QUICK_SEARCH_TYPE_KEY, searchType.name());
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
		outState.putBoolean(QUICK_SEARCH_FAB_VISIBLE_KEY, fabVisible);
	}

	@Override
	public void onResume() {
		super.onResume();
		if (!useMapCenter) {
			startLocationUpdate();
		}
		expired = false;
		paused = false;
		if (pausedSearch && !TextUtils.isEmpty(searchQuery)) {
			runSearch();
		}
		pausedSearch = false;
	}

	@Override
	public void onPause() {
		super.onPause();
		paused = true;
		pausedSearch = searching;
		hideTimeMs = System.currentTimeMillis();
		stopLocationUpdate();
		hideProgressBar();
	}

	@Override
	public void onDismiss(DialogInterface dialog) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			hideToolbar();
			mapActivity.updateStatusBarColor();
			mapActivity.refreshMap();
			getChildFragmentManager().popBackStack();
		}
		super.onDismiss(dialog);
	}

	private boolean processBackAction() {
		if (addressSearch && isSearchViewVisible()) {
			searchEditText.setText("");
			return true;
		}
		return false;
	}

	public Toolbar getToolbar() {
		return toolbar;
	}

	public boolean isUseMapCenter() {
		return useMapCenter;
	}

	private void startLocationUpdate() {
		app.getLocationProvider().removeCompassListener(app.getLocationProvider().getNavigationInfo());
		app.getLocationProvider().addCompassListener(this);
		app.getLocationProvider().addLocationListener(this);
		location = app.getLocationProvider().getLastKnownLocation();
		updateLocation(location);
	}

	private void stopLocationUpdate() {
		app.getLocationProvider().removeLocationListener(this);
		app.getLocationProvider().removeCompassListener(this);
		app.getLocationProvider().addCompassListener(app.getLocationProvider().getNavigationInfo());
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
			clearButton.setImageDrawable(app.getUIUtilities().getIcon(R.drawable.ic_action_get_my_location, R.color.color_myloc_distance));
		} else {
			if (addressSearch) {
				searchEditText.setHint(R.string.type_address);
			} else {
				searchEditText.setHint(R.string.search_poi_category_hint);
			}
			clearButton.setImageDrawable(app.getUIUtilities().getThemedIcon(R.drawable.ic_action_remove_dark));
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
		if (show) {
			tabToolbarView.setVisibility(View.VISIBLE);
			buttonToolbarView.setVisibility(View.GONE);
			tabsView.setVisibility(View.VISIBLE);
			searchView.setVisibility(View.GONE);
		} else {
			tabToolbarView.setVisibility(View.GONE);
			SearchWord lastWord = searchUICore.getPhrase().getLastSelectedWord();
			boolean buttonToolbarVisible = (isOnlineSearch() && !isTextEmpty())
					|| !searchUICore.getSearchSettings().isCustomSearch();
			if (searchType.isTargetPoint() && (lastWord == null || lastWord.getLocation() == null)) {
				buttonToolbarVisible = false;
			}
			buttonToolbarView.setVisibility(buttonToolbarVisible ? View.VISIBLE : View.GONE);
			tabsView.setVisibility(View.GONE);
			searchView.setVisibility(View.VISIBLE);
		}
	}

	private boolean isOnlineSearch() {
		return searchUICore.getSearchSettings().hasCustomSearchType(ObjectType.ONLINE_SEARCH);
	}

	private boolean isSearchViewVisible() {
		return searchView.getVisibility() == View.VISIBLE;
	}

	public void setResultCollection(SearchResultCollection resultCollection) {
		searchHelper.setResultCollection(resultCollection);
	}

	public SearchResultCollection getResultCollection() {
		return searchHelper.getResultCollection();
	}

	public boolean isResultEmpty() {
		SearchResultCollection res = getResultCollection();
		return res == null || res.getCurrentSearchResults().size() == 0;
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

			case ADDRESS:
				addressSearchFragment = (QuickSearchAddressListFragment) searchListFragment;
				if (addressSearch && !citiesLoaded) {
					reloadCities();
				}
				break;

			case MAIN:
				if (!Algorithms.isEmpty(searchQuery)) {
					searchEditText.setText(searchQuery);
					searchEditText.setSelection(searchQuery.length());
				}
				if (getResultCollection() != null) {
					updateSearchResult(getResultCollection(), false);
					addMoreButton(searchUICore.isSearchMoreAvailable(searchUICore.getPhrase()));
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
					if (!paused) {
						reloadCategoriesInternal();
						if (!searching) {
							hideProgressBar();
						}
					}
				}
			});
		} else {
			reloadCategoriesInternal();
		}
	}

	private void reloadCategoriesInternal() {
		try {
			if (SearchUICore.isDebugMode()) {
				LOG.info("UI >> Start loading categories");
			}
			SearchResultCollection res = searchUICore.shallowSearch(SearchAmenityTypesAPI.class, "", null);
			if (res != null) {
				List<QuickSearchListItem> rows = new ArrayList<>();
				for (SearchResult sr : res.getCurrentSearchResults()) {
					rows.add(new QuickSearchListItem(app, sr));
				}
				rows.add(new QuickSearchButtonListItem(app, R.drawable.ic_world_globe_dark,
						app.getString(R.string.search_online_address), new OnClickListener() {
					@Override
					public void onClick(View view) {
						final OsmandSettings settings = app.getSettings();
						if (!settings.isInternetConnectionAvailable()) {
							Toast.makeText(app, R.string.internet_not_available, Toast.LENGTH_LONG).show();
							return;
						}
						startOnlineSearch();
						mainSearchFragment.getAdapter().clear();
						updateTabbarVisibility(false);
						openKeyboard();
					}
				}));
				rows.add(new QuickSearchButtonListItem(app, R.drawable.ic_action_search_dark,
						app.getString(R.string.custom_search), new OnClickListener() {
					@Override
					public void onClick(View v) {
						PoiUIFilter filter = app.getPoiFilters().getCustomPOIFilter();
						filter.clearFilter();
						QuickSearchCustomPoiFragment.showDialog(
								QuickSearchDialogFragment.this, filter.getFilterId());
					}
				}));
				if (categoriesSearchFragment != null) {
					categoriesSearchFragment.updateListAdapter(rows, false);
				}
			}
			if (SearchUICore.isDebugMode()) {
				LOG.info("UI >> Categories loaded");
			}
		} catch (IOException e) {
			e.printStackTrace();
			app.showToastMessage(e.getMessage());
		}
	}

	public void reloadCities() {
		if (app.isApplicationInitializing()) {
			showProgressBar();
			app.getAppInitializer().addListener(new AppInitializeListener() {
				@Override
				public void onProgress(AppInitializer init, AppInitializer.InitEvents event) {
				}

				@Override
				public void onFinish(AppInitializer init) {
					if (!paused) {
						reloadCitiesInternal();
						if (!searching) {
							hideProgressBar();
						}
					}
				}
			});
		} else {
			reloadCitiesInternal();
		}
	}

	private void reloadCitiesInternal() {
		if (SearchUICore.isDebugMode()) {
			LOG.info("UI >> Start loading nearest cities");
		}
		startNearestCitySearch();
		runCoreSearch("", false, false, new SearchResultListener() {
			@Override
			public void searchStarted(SearchPhrase phrase) {
			}

			@Override
			public void publish(SearchResultCollection res, boolean append) {
			}

			@Override
			public boolean searchFinished(SearchPhrase phrase) {
				if (SearchUICore.isDebugMode()) {
					LOG.info("UI >> Nearest cities found: " + getSearchResultCollectionFormattedSize(getResultCollection()));
				}
				updateCitiesItems();
				if (SearchUICore.isDebugMode()) {
					LOG.info("UI >> Nearest cities loaded");
				}
				return true;
			}
		});
		restoreSearch();
	}

	private void updateCitiesItems() {
		SearchResultCollection res = getResultCollection();

		final OsmandSettings settings = app.getSettings();
		List<QuickSearchListItem> rows = new ArrayList<>();

		if (SearchUICore.isDebugMode()) {
			LOG.info("UI >> Start last city searching (within nearests)");
		}
		SearchResult lastCity = null;
		if (res != null) {
			citiesLoaded = res.getCurrentSearchResults().size() > 0;
			final long lastCityId = settings.getLastSearchedCity();
			for (SearchResult sr : res.getCurrentSearchResults()) {
				if (sr.objectType == ObjectType.CITY && ((City) sr.object).getId() == lastCityId) {
					lastCity = sr;
					break;
				}
			}
		}
		if (SearchUICore.isDebugMode()) {
			LOG.info("UI >> Last city found: " + (lastCity != null ? lastCity.localeName : "-"));
		}

		final String lastCityName = lastCity == null ? settings.getLastSearchedCityName() : lastCity.localeName;
		if (!Algorithms.isEmpty(lastCityName)) {
			String selectStreets = app.getString(R.string.search_street);
			String inCityName = app.getString(R.string.shared_string_in_name, lastCityName);
			Spannable spannable = new SpannableString(selectStreets + " " + inCityName);
			boolean light = settings.isLightContent();
			spannable.setSpan(new ForegroundColorSpan(getResources().getColor(light ? R.color.icon_color_default_light : R.color.color_white)),
					selectStreets.length() + 1, spannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

			final SearchResult lastCityFinal = lastCity;
			rows.add(new QuickSearchButtonListItem(app, R.drawable.ic_action_street_name,
					spannable, new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (lastCityFinal == null) {
						final long lastCityId = settings.getLastSearchedCity();
						final LatLon lastCityPoint = settings.getLastSearchedPoint();
						if (lastCityId != -1 && lastCityPoint != null) {
							startLastCitySearch(lastCityPoint);
							if (SearchUICore.isDebugMode()) {
								LOG.info("UI >> Start last city searching (standalone)");
							}
							runCoreSearch("", false, false, new SearchResultListener() {

								boolean cityFound = false;

								@Override
								public void publish(SearchResultCollection res, boolean append) {
									if (res != null) {
										for (SearchResult sr : res.getCurrentSearchResults()) {
											if (sr.objectType == ObjectType.CITY && ((City) sr.object).getId() == lastCityId) {
												if (SearchUICore.isDebugMode()) {
													LOG.info("UI >> Last city found: " + sr.localeName);
												}
												cityFound = true;
												completeQueryWithObject(sr);
												break;
											}
										}
									}
								}

								@Override
								public void searchStarted(SearchPhrase phrase) {
								}

								@Override
								public boolean searchFinished(SearchPhrase phrase) {
									if (!cityFound) {
										replaceQueryWithText(lastCityName + " ");
									}
									return false;
								}
							});
							restoreSearch();
						} else {
							replaceQueryWithText(lastCityName + " ");
						}
					} else {
						completeQueryWithObject(lastCityFinal);
					}
					openKeyboard();
				}
			}));
		}
		rows.add(new QuickSearchButtonListItem(app, R.drawable.ic_action_building_number,
				app.getString(R.string.start_search_from_city), new OnClickListener() {
			@Override
			public void onClick(View v) {
				searchEditText.setHint(R.string.type_city_town);
				startCitySearch();
				updateTabbarVisibility(false);
				runCoreSearch("", false, false);
				openKeyboard();
			}
		}));
		rows.add(new QuickSearchButtonListItem(app, R.drawable.ic_action_postcode,
				app.getString(R.string.select_postcode), new OnClickListener() {
			@Override
			public void onClick(View v) {
				searchEditText.setHint(R.string.type_postcode);
				startPostcodeSearch();
				mainSearchFragment.getAdapter().clear();
				updateTabbarVisibility(false);
				openKeyboard();
			}
		}));
		rows.add(new QuickSearchButtonListItem(app, R.drawable.ic_action_marker_dark,
				app.getString(R.string.coords_search), new OnClickListener() {
			@Override
			public void onClick(View v) {
				LatLon latLon = searchUICore.getSearchSettings().getOriginalLocation();
				QuickSearchCoordinatesFragment.showDialog(QuickSearchDialogFragment.this,
						latLon.getLatitude(), latLon.getLongitude());
			}
		}));

		if (res != null) {
			rows.add(new QuickSearchHeaderListItem(app, app.getString(R.string.nearest_cities), true));
			int limit = 15;
			for (SearchResult sr : res.getCurrentSearchResults()) {
				if (limit > 0) {
					rows.add(new QuickSearchListItem(app, sr));
				}
				limit--;
			}
		}
		addressSearchFragment.updateListAdapter(rows, false);
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
					if (!paused) {
						reloadHistoryInternal();
						if (!searching) {
							hideProgressBar();
						}
					}
				}
			});
		} else {
			reloadHistoryInternal();
		}
	}

	private void reloadHistoryInternal() {
		if (historySearchFragment != null) {
			try {
				SearchResultCollection res = searchUICore.shallowSearch(SearchHistoryAPI.class, "", null);
				List<QuickSearchListItem> rows = new ArrayList<>();
				if (res != null) {
					for (SearchResult sr : res.getCurrentSearchResults()) {
						rows.add(new QuickSearchListItem(app, sr));
					}
				}
				historySearchFragment.updateListAdapter(rows, false);
			} catch (Exception e) {
				e.printStackTrace();
				app.showToastMessage(e.getMessage());
			}
		}
	}

	private void restoreSearch() {
		if (addressSearch) {
			startAddressSearch();
		} else {
			stopAddressSearch();
		}
		if (storedOriginalLocation != null) {
			// Restore previous search location
			searchUICore.updateSettings(searchUICore.getSearchSettings().setOriginalLocation(storedOriginalLocation));
			storedOriginalLocation = null;
		}
	}

	private void startOnlineSearch() {
		SearchSettings settings = searchUICore.getSearchSettings()
				.setSearchTypes(ObjectType.ONLINE_SEARCH)
				.setEmptyQueryAllowed(false)
				.setSortByName(false)
				.setRadiusLevel(1);

		searchUICore.updateSettings(settings);
		setResultCollection(null);
	}

	private void startAddressSearch() {
		SearchSettings settings = searchUICore.getSearchSettings()
				.setEmptyQueryAllowed(true)
				.setSortByName(false)
				.setSearchTypes(ObjectType.CITY, ObjectType.VILLAGE, ObjectType.POSTCODE,
						ObjectType.HOUSE, ObjectType.STREET_INTERSECTION, ObjectType.STREET,
						ObjectType.LOCATION, ObjectType.PARTIAL_LOCATION)
				.setRadiusLevel(1);

		searchUICore.updateSettings(settings);
	}

	private void startCitySearch() {
		SearchSettings settings = searchUICore.getSearchSettings()
				.setEmptyQueryAllowed(true)
				.setSortByName(true)
				.setSearchTypes(ObjectType.CITY, ObjectType.VILLAGE)
				.setRadiusLevel(1);

		searchUICore.updateSettings(settings);
	}

	private void startNearestCitySearch() {
		SearchSettings settings = searchUICore.getSearchSettings()
				.setEmptyQueryAllowed(true)
				.setSortByName(false)
				.setSearchTypes(ObjectType.CITY)
				.setRadiusLevel(1);

		searchUICore.updateSettings(settings);
	}

	private void startLastCitySearch(LatLon latLon) {
		SearchSettings settings = searchUICore.getSearchSettings();
		storedOriginalLocation = settings.getOriginalLocation();
		settings = settings.setEmptyQueryAllowed(true)
				.setSortByName(false)
				.setSearchTypes(ObjectType.CITY)
				.setOriginalLocation(latLon)
				.setRadiusLevel(1);

		searchUICore.updateSettings(settings);
	}

	private void startPostcodeSearch() {
		SearchSettings settings = searchUICore.getSearchSettings()
				.setSearchTypes(ObjectType.POSTCODE)
				.setEmptyQueryAllowed(false)
				.setSortByName(true)
				.setRadiusLevel(1);

		searchUICore.updateSettings(settings);
	}

	private void stopAddressSearch() {
		SearchSettings settings = searchUICore.getSearchSettings()
				.resetSearchTypes()
				.setEmptyQueryAllowed(false)
				.setSortByName(false)
				.setRadiusLevel(1);

		searchUICore.updateSettings(settings);
	}

	private void cancelSearch() {
		cancelPrev = true;
		if (!paused) {
			hideProgressBar();
		}
	}

	private void runSearch() {
		runSearch(searchQuery);
	}

	private void runSearch(String text) {
		showProgressBar();
		SearchSettings settings = searchUICore.getSearchSettings();
		if (settings.getRadiusLevel() != 1) {
			searchUICore.updateSettings(settings.setRadiusLevel(1));
		}
		runCoreSearch(text, true, false);
	}

	private void runCoreSearch(final String text, final boolean showQuickResult, final boolean searchMore) {
		runCoreSearch(text, showQuickResult, searchMore, defaultResultListener);
	}

	private void runCoreSearch(final String text, final boolean showQuickResult, final boolean searchMore,
							   final SearchResultListener resultListener) {
		showProgressBar();
		foundPartialLocation = false;
		updateToolbarButton();
		interruptedSearch = false;
		searching = true;
		cancelPrev = true;

		if (app.isApplicationInitializing() && text.length() > 0) {
			app.getAppInitializer().addListener(new AppInitializeListener() {
				@Override
				public void onProgress(AppInitializer init, AppInitializer.InitEvents event) {
				}

				@Override
				public void onFinish(AppInitializer init) {
					if (!paused) {
						runCoreSearchInternal(text, showQuickResult, searchMore, resultListener);
					}
				}
			});
		} else {
			runCoreSearchInternal(text, showQuickResult, searchMore, resultListener);
		}
	}

	private void runCoreSearchInternal(String text, boolean showQuickResult, boolean searchMore,
									   final SearchResultListener resultListener) {
		searchUICore.search(text, showQuickResult, new ResultMatcher<SearchResult>() {
			SearchResultCollection regionResultCollection = null;
			SearchCoreAPI regionResultApi = null;
			List<SearchResult> results = new ArrayList<>();

			@Override
			public boolean publish(final SearchResult object) {
				if (object.objectType == SEARCH_STARTED) {
					cancelPrev = false;
				}
				if (paused || cancelPrev) {
					return false;
				}
				switch (object.objectType) {
					case SEARCH_STARTED:
						if (resultListener != null) {
							app.runInUIThread(new Runnable() {
								@Override
								public void run() {
									resultListener.searchStarted(object.requiredSearchPhrase);
								}
							});
						}
						break;
					case SEARCH_FINISHED:
						app.runInUIThread(new Runnable() {
							@Override
							public void run() {
								if (paused) {
									return;
								}
								searching = false;
								if (resultListener == null || resultListener.searchFinished(object.requiredSearchPhrase)) {
									hideProgressBar();
									addMoreButton(searchUICore.isSearchMoreAvailable(object.requiredSearchPhrase));
								}
							}
						});
						break;
					case FILTER_FINISHED:
						if (resultListener != null) {
							app.runInUIThread(new Runnable() {
								@Override
								public void run() {
									resultListener.publish(searchUICore.getCurrentSearchResult(), false);
								}
							});
						}
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
						showApiResults(searchApi, apiResults, phrase, hasRegionCollection, resultListener);
						break;
					case SEARCH_API_REGION_FINISHED:
						regionResultApi = (SearchCoreAPI) object.object;
						final SearchPhrase regionPhrase = object.requiredSearchPhrase;
						regionResultCollection =
								new SearchResultCollection(regionPhrase).addSearchResults(results, true, true);
						showRegionResults(object.file, regionPhrase, regionResultCollection, resultListener);
						break;
					case PARTIAL_LOCATION:
						showLocationToolbar();
						break;
					default:
						results.add(object);
				}

				return true;
			}


			@Override
			public boolean isCancelled() {
				return paused || cancelPrev;
			}
		});

		if (!searchMore) {
			setResultCollection(null);
			if (!showQuickResult) {
				updateSearchResult(null, false);
			}
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

	private void showApiResults(final SearchCoreAPI searchApi,
								final List<SearchResult> apiResults,
								final SearchPhrase phrase,
								final boolean hasRegionCollection,
								final SearchResultListener resultListener) {
		app.runInUIThread(new Runnable() {
			@Override
			public void run() {
				if (!paused && !cancelPrev) {
					if (SearchUICore.isDebugMode()) {
						LOG.info("UI >> Showing API results <" + phrase + "> API=<" + searchApi + "> Results=" + apiResults.size());
					}
					boolean append = getResultCollection() != null;
					if (append) {
						if (SearchUICore.isDebugMode()) {
							LOG.info("UI >> Appending API results <" + phrase + "> API=<" + searchApi + "> Result collection=" + getSearchResultCollectionFormattedSize(getResultCollection()));
						}
						getResultCollection().addSearchResults(apiResults, true, true);
						if (SearchUICore.isDebugMode()) {
							LOG.info("UI >> API results appended <" + phrase + "> API=<" + searchApi + "> Result collection=" + getSearchResultCollectionFormattedSize(getResultCollection()));
						}
					} else {
						if (SearchUICore.isDebugMode()) {
							LOG.info("UI >> Assign API results <" + phrase + "> API=<" + searchApi + ">");
						}
						SearchResultCollection resCollection = new SearchResultCollection(phrase);
						resCollection.addSearchResults(apiResults, true, true);
						setResultCollection(resCollection);
						if (SearchUICore.isDebugMode()) {
							LOG.info("UI >> API results assigned <" + phrase + "> API=<" + searchApi + "> Result collection=" + getSearchResultCollectionFormattedSize(getResultCollection()));
						}
					}
					if (!hasRegionCollection && resultListener != null) {
						resultListener.publish(getResultCollection(), append);
					}
					if (SearchUICore.isDebugMode()) {
						LOG.info("UI >> API results shown <" + phrase + "> API=<" + searchApi + "> Results=" + getSearchResultCollectionFormattedSize(getResultCollection()));
					}
				}
			}
		});
	}

	private void showRegionResults(final BinaryMapIndexReader region,
								   final SearchPhrase phrase,
								   final SearchResultCollection regionResultCollection,
								   final SearchResultListener resultListener) {
		app.runInUIThread(new Runnable() {
			@Override
			public void run() {
				if (!paused && !cancelPrev) {
					if (SearchUICore.isDebugMode()) {
						LOG.info("UI >> Showing region results <" + phrase + "> Region=<" + region.getFile().getName() + "> Results=" + getSearchResultCollectionFormattedSize(regionResultCollection));
					}
					if (getResultCollection() != null) {
						if (SearchUICore.isDebugMode()) {
							LOG.info("UI >> Combining region results <" + phrase + "> Region=<" + region.getFile().getName() + "> Result collection=" + getSearchResultCollectionFormattedSize(getResultCollection()));
						}
						SearchResultCollection resCollection =
								getResultCollection().combineWithCollection(regionResultCollection, true, true);
						if (SearchUICore.isDebugMode()) {
							LOG.info("UI >> Region results combined <" + phrase + "> Region=<" + region.getFile().getName() + "> Result collection=" + getSearchResultCollectionFormattedSize(resCollection));
						}
						if (resultListener != null) {
							resultListener.publish(resCollection, true);
						}
						if (SearchUICore.isDebugMode()) {
							LOG.info("UI >> Region results shown <" + phrase + "> Region=<" + region.getFile().getName() + "> Results=" + getSearchResultCollectionFormattedSize(resCollection));
						}
					} else if (resultListener != null) {
						resultListener.publish(regionResultCollection, false);
						if (SearchUICore.isDebugMode()) {
							LOG.info("UI >> Region results shown <" + phrase + "> Region=<" + region.getFile().getName() + "> Results=" + getSearchResultCollectionFormattedSize(regionResultCollection));
						}
					}
				}
			}
		});
	}

	private String getSearchResultCollectionFormattedSize(@Nullable SearchResultCollection resultCollection) {
		return resultCollection != null ? String.valueOf(resultCollection.getCurrentSearchResults().size()) : "empty";
	}

	public void completeQueryWithObject(SearchResult sr) {
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
		if (addressSearch) {
			startAddressSearch();
			if (sr.objectType == ObjectType.CITY) {
				if (sr.relatedObject instanceof BinaryMapIndexReader) {
					File f = ((BinaryMapIndexReader) sr.relatedObject).getFile();
					if (f != null) {
						RegionAddressRepository region = app.getResourceManager().getRegionRepository(f.getName());
						if (region != null) {
							City city = (City) sr.object;
							String lastSearchedRegion = app.getSettings().getLastSearchedRegion();
							long lastCityId = app.getSettings().getLastSearchedCity();
							if (!lastSearchedRegion.equals(region.getFileName()) || city.getId() != lastCityId) {
								app.getSettings().setLastSearchedRegion(region.getFileName(), region.getEstimatedRegionCenter());
								app.getSettings().setLastSearchedCity(city.getId(), sr.localeName, city.getLocation());
								updateCitiesItems();
							}
						}
					}
				}
			}
		}
		String txt = searchUICore.getPhrase().getText(true);
		replaceQueryWithText(txt);
		if (sr.objectType == ObjectType.CITY) {
			openKeyboard();
		}
	}

	private void openKeyboard() {
		searchEditText.requestFocus();
		AndroidUtils.softKeyboardDelayed(searchEditText);
	}

	public void replaceQueryWithText(String txt) {
		searchQuery = txt;
		searchEditText.setText(txt);
		searchEditText.setSelection(txt.length());
		SearchWord lastWord = searchUICore.getPhrase().getLastSelectedWord();
		boolean buttonToolbarVisible = searchType == QuickSearchType.REGULAR;
		if (!buttonToolbarVisible) {
			if (lastWord == null) {
				buttonToolbarVisible = true;
			} else if (searchType.isTargetPoint() && lastWord.getLocation() != null) {
				buttonToolbarVisible = true;
			}
		}
		buttonToolbarView.setVisibility(buttonToolbarVisible ? View.VISIBLE : View.GONE);
		updateToolbarButton();
		SearchSettings settings = searchUICore.getSearchSettings();
		if (settings.getRadiusLevel() != 1) {
			searchUICore.updateSettings(settings.setRadiusLevel(1));
		}
		runCoreSearch(txt, false, false);
	}

	public void replaceQueryWithUiFilter(PoiUIFilter filter, String nameFilter) {
		SearchPhrase searchPhrase = searchUICore.getPhrase();
		if (searchPhrase.isLastWord(POI_TYPE)) {
			poiFilterApplied = true;
			SearchResult sr = searchPhrase.getLastSelectedWord().getResult();
			sr.object = filter;
			sr.localeName = filter.getName();
			searchUICore.getPhrase().syncWordsWithResults();
			String txt = filter.getName()
					+ (!Algorithms.isEmpty(nameFilter) && filter.isStandardFilter() ? " " + nameFilter : " ");
			searchQuery = txt;
			searchEditText.setText(txt);
			searchEditText.setSelection(txt.length());
			updateToolbarButton();
			runCoreSearch(txt, false, false);
		}
	}

	public void clearLastWord() {
		if (searchEditText.getText().length() > 0) {
			String newText = searchUICore.getPhrase().getTextWithoutLastWord();
			searchEditText.setText(newText);
			searchEditText.setSelection(newText.length());
		}
	}

	private void addMoreButton(boolean searchMoreAvailable) {
		if (!paused && !cancelPrev && mainSearchFragment != null && !isTextEmpty()) {
			QuickSearchMoreListItem moreListItem =
					new QuickSearchMoreListItem(app, null, new SearchMoreItemOnClickListener() {
						@Override
						public void increaseRadiusOnClick() {
							if (!interruptedSearch) {
								SearchSettings settings = searchUICore.getSearchSettings();
								searchUICore.updateSettings(settings.setRadiusLevel(settings.getRadiusLevel() + 1));
							}
							runCoreSearch(searchQuery, false, true);
						}

						@Override
						public void onlineSearchOnClick() {
							final OsmandSettings settings = app.getSettings();
							if (!settings.isInternetConnectionAvailable()) {
								Toast.makeText(app, R.string.internet_not_available, Toast.LENGTH_LONG).show();
								return;
							}
							startOnlineSearch();
							mainSearchFragment.getAdapter().clear();
							updateTabbarVisibility(false);
							runCoreSearch(searchQuery, false, true);
						}
					});
			moreListItem.setInterruptedSearch(interruptedSearch);
			moreListItem.setEmptySearch(isResultEmpty());
			moreListItem.setOnlineSearch(isOnlineSearch());
			moreListItem.setSearchMoreAvailable(searchMoreAvailable);
			mainSearchFragment.addListItem(moreListItem);
			updateSendEmptySearchBottomBar(isResultEmpty() && !interruptedSearch);
		}
	}

	private void updateSearchResult(SearchResultCollection res, boolean append) {

		if (!paused && mainSearchFragment != null) {
			List<QuickSearchListItem> rows = new ArrayList<>();
			if (res != null && res.getCurrentSearchResults().size() > 0) {
				for (final SearchResult sr : res.getCurrentSearchResults()) {
					rows.add(new QuickSearchListItem(app, sr));
				}
				updateSendEmptySearchBottomBar(false);
			}
			mainSearchFragment.updateListAdapter(rows, append);
		}
	}

	public static boolean showInstance(@NonNull MapActivity mapActivity,
									   @NonNull String searchQuery,
									   @Nullable Object object,
									   QuickSearchType searchType,
									   QuickSearchTab showSearchTab,
									   @Nullable LatLon latLon) {
		try {

			if (mapActivity.isActivityDestroyed()) {
				return false;
			}

			mapActivity.getMyApplication().logEvent("search_open");

			Bundle bundle = new Bundle();
			if (object != null) {
				bundle.putBoolean(QUICK_SEARCH_RUN_SEARCH_FIRST_TIME_KEY, true);
				String objectLocalizedName = searchQuery;

				if (object instanceof PoiCategory) {
					PoiCategory c = (PoiCategory) object;
					objectLocalizedName = c.getTranslation();

					SearchUICore searchUICore = mapActivity.getMyApplication().getSearchUICore().getCore();
					SearchPhrase phrase = searchUICore.resetPhrase(objectLocalizedName + " ");
					SearchResult sr = new SearchResult(phrase);
					sr.localeName = objectLocalizedName;
					sr.object = c;
					sr.priority = SEARCH_AMENITY_TYPE_PRIORITY;
					sr.priorityDistance = 0;
					sr.objectType = ObjectType.POI_TYPE;
					searchUICore.selectSearchResult(sr);

					bundle.putBoolean(QUICK_SEARCH_PHRASE_DEFINED_KEY, true);

				} else if (object instanceof PoiUIFilter) {
					PoiUIFilter filter = (PoiUIFilter) object;
					objectLocalizedName = filter.getName();
					SearchUICore searchUICore = mapActivity.getMyApplication().getSearchUICore().getCore();
					SearchPhrase phrase = searchUICore.resetPhrase();
					SearchResult sr = new SearchResult(phrase);
					sr.localeName = objectLocalizedName;
					sr.object = filter;
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
			bundle.putString(QUICK_SEARCH_SHOW_TAB_KEY, showSearchTab.name());
			bundle.putString(QUICK_SEARCH_TYPE_KEY, searchType.name());
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
			} else if (historySearchFragment != null && viewPager.getCurrentItem() == 0) {
				historySearchFragment.updateLocation(latLon, heading);
			} else if (categoriesSearchFragment != null && viewPager.getCurrentItem() == 1) {
				categoriesSearchFragment.updateLocation(latLon, heading);
			} else if (addressSearchFragment != null && viewPager.getCurrentItem() == 2) {
				addressSearchFragment.updateLocation(latLon, heading);
			}
		}
	}

	private void updateUseMapCenterUI() {
		if (!paused && !cancelPrev) {
			if (mainSearchFragment != null) {
				mainSearchFragment.getListAdapter().setUseMapCenter(useMapCenter);
			}
			if (historySearchFragment != null) {
				historySearchFragment.getListAdapter().setUseMapCenter(useMapCenter);
			}
			if (categoriesSearchFragment != null) {
				categoriesSearchFragment.getListAdapter().setUseMapCenter(useMapCenter);
			}
			if (addressSearchFragment != null) {
				addressSearchFragment.getListAdapter().setUseMapCenter(useMapCenter);
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
					GPXFile gpx = new GPXFile(Version.getFullVersion(getMyApplication()));
					for (HistoryEntry h : historyEntries) {
						WptPt pt = new WptPt();
						pt.lat = h.getLat();
						pt.lon = h.getLon();
						pt.name = h.getName().getName();
						boolean hasTypeInDescription = !Algorithms.isEmpty(h.getName().getTypeName());
						if (hasTypeInDescription) {
							pt.desc = h.getName().getTypeName();
						}
						gpx.addPoint(pt);
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
					GPXUtilities.writeGpxFile(dst, gpxFile);

					final Intent sendIntent = new Intent();
					sendIntent.setAction(Intent.ACTION_SEND);
					sendIntent.putExtra(Intent.EXTRA_TEXT, "History.gpx:\n\n\n" + GPXUtilities.asString(gpxFile));
					sendIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_history_subject));
					sendIntent.putExtra(Intent.EXTRA_STREAM, AndroidUtils.getUriForFile(getMapActivity(), dst));
					sendIntent.setType("text/plain");
					sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
					startActivity(sendIntent);
				}
			};
			exportTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
	}

	public void showFilter(@NonNull String filterId) {
		PoiUIFilter filter = app.getPoiFilters().getFilterById(filterId);
		boolean isCustomFilter = filterId.equals(app.getPoiFilters().getCustomPOIFilter().getFilterId());
		if (isCustomFilter) {
			fabVisible = true;
			poiFilterApplied = true;
			updateFab();

			PoiUIFilter nFilter = new PoiUIFilter(filter.getTypesName(), null, filter.getAcceptedTypes(), app);
			if (!Algorithms.isEmpty(filter.getFilterByName())) {
				nFilter.setSavedFilterByName(filter.getFilterByName());
			}
			app.getPoiFilters().createPoiFilter(nFilter, true);
			SearchHistoryHelper.getInstance(app).addNewItemToHistory(nFilter);
			reloadHistory();
		}

		SearchResult sr = new SearchResult(searchUICore.getPhrase());
		sr.localeName = filter.getName();
		sr.object = filter;
		sr.priority = 0;
		sr.objectType = ObjectType.POI_TYPE;
		searchUICore.selectSearchResult(sr);

		String txt = filter.getName() + " ";
		searchQuery = txt;
		searchEditText.setText(txt);
		searchEditText.setSelection(txt.length());
		updateToolbarButton();
		SearchSettings settings = searchUICore.getSearchSettings();
		if (settings.getRadiusLevel() != 1) {
			searchUICore.updateSettings(settings.setRadiusLevel(1));
		}
		runCoreSearch(txt, false, false);
	}

	private void updateFab() {
		fab.setVisibility(fabVisible ? View.VISIBLE : View.GONE);
		updateFabHeight();
	}

	private void updateFabHeight() {
		if (fabVisible) {
			int bottomMargin;
			if (sendEmptySearchBottomBarVisible) {
				bottomMargin = app.getResources().getDimensionPixelSize(R.dimen.fab_margin_bottom_big);
			} else {
				bottomMargin = app.getResources().getDimensionPixelSize(R.dimen.fab_margin_right);
			}
			FrameLayout.LayoutParams parameter = (FrameLayout.LayoutParams) fab.getLayoutParams();
			parameter.setMargins(parameter.leftMargin, parameter.topMargin, parameter.rightMargin, bottomMargin);
			fab.setLayoutParams(parameter);
		}
	}

	private void updateSendEmptySearchBottomBar(boolean sendSearchQueryVisible) {
		sendEmptySearchView.setVisibility(sendSearchQueryVisible ? View.VISIBLE : View.GONE);
		sendEmptySearchText.setVisibility(sendSearchQueryVisible ? View.VISIBLE : View.GONE);
		sendEmptySearchButton.setVisibility(sendSearchQueryVisible ? View.VISIBLE : View.GONE);
		sendEmptySearchBottomBarVisible = sendSearchQueryVisible;
		updateFabHeight();
	}

	public interface SearchResultListener {
		void searchStarted(SearchPhrase phrase);
		void publish(SearchResultCollection res, boolean append);
		// return true if search done, false if next search will be ran immediately
		boolean searchFinished(SearchPhrase phrase);
	}

	public class SearchFragmentPagerAdapter extends FragmentPagerAdapter {
		private final String[] fragments = new String[] {
				QuickSearchHistoryListFragment.class.getName(),
				QuickSearchCategoriesListFragment.class.getName(),
				QuickSearchAddressListFragment.class.getName()
		};
		private final int[] titleIds = new int[]{
				QuickSearchHistoryListFragment.TITLE,
				QuickSearchCategoriesListFragment.TITLE,
				QuickSearchAddressListFragment.TITLE
		};
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

	public static class QuickSearchAddressListFragment extends QuickSearchListFragment {
		public static final int TITLE = R.string.address;

		@Override
		public SearchListFragmentType getType() {
			return SearchListFragmentType.ADDRESS;
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

	public static class DeleteDialogFragment extends DialogFragment {

		private List<QuickSearchListItem> selectedItems;

		public void setSelectedItems(List<QuickSearchListItem> selectedItems) {
			this.selectedItems = selectedItems;
		}

		@NonNull
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle(R.string.confirmation_to_delete_history_items)
					.setPositiveButton(R.string.shared_string_yes, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							Fragment parentFragment = getParentFragment();
							if (parentFragment instanceof QuickSearchDialogFragment) {
								QuickSearchDialogFragment searchDialogFragment = (QuickSearchDialogFragment) parentFragment;
								SearchHistoryHelper helper = SearchHistoryHelper.getInstance(searchDialogFragment.getMyApplication());
								for (QuickSearchListItem searchListItem : selectedItems) {
									helper.remove(searchListItem.getSearchResult().object);
								}
								searchDialogFragment.reloadHistory();
								searchDialogFragment.enableSelectionMode(false, -1);
							}
						}
					})
					.setNegativeButton(R.string.shared_string_no, null);
			return builder.create();
		}
	}
}
