package net.osmand.plus.search.dialogs;

import static net.osmand.search.core.ObjectType.CITY;
import static net.osmand.search.core.ObjectType.HOUSE;
import static net.osmand.search.core.ObjectType.LOCATION;
import static net.osmand.search.core.ObjectType.ONLINE_SEARCH;
import static net.osmand.search.core.ObjectType.PARTIAL_LOCATION;
import static net.osmand.search.core.ObjectType.POI_TYPE;
import static net.osmand.search.core.ObjectType.POSTCODE;
import static net.osmand.search.core.ObjectType.SEARCH_STARTED;
import static net.osmand.search.core.ObjectType.STREET;
import static net.osmand.search.core.ObjectType.STREET_INTERSECTION;
import static net.osmand.search.core.ObjectType.VILLAGE;
import static net.osmand.search.core.SearchCoreFactory.SEARCH_AMENITY_TYPE_PRIORITY;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.Amenity;
import net.osmand.data.City;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.PoiCategory;
import net.osmand.plus.LockableViewPager;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmAndLocationProvider.OsmAndCompassListener;
import net.osmand.plus.OsmAndLocationProvider.OsmAndLocationListener;
import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseFullScreenDialogFragment;
import net.osmand.plus.download.DownloadIndexesThread.DownloadEvents;
import net.osmand.plus.exploreplaces.ExplorePlacesFragment;
import net.osmand.plus.helpers.LocaleHelper;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.myplaces.favorites.FavoriteGroup;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.accessibility.AccessibilityAssistant;
import net.osmand.plus.plugins.accessibility.NavigationInfo;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.poi.RearrangePoiFiltersFragment;
import net.osmand.plus.resources.RegionAddressRepository;
import net.osmand.plus.search.QuickSearchHelper;
import net.osmand.plus.search.QuickSearchHelper.SearchHistoryAPI;
import net.osmand.plus.search.ShareHistoryAsyncTask;
import net.osmand.plus.search.ShareHistoryAsyncTask.OnShareHistoryListener;
import net.osmand.plus.search.history.HistoryEntry;
import net.osmand.plus.search.listitems.QuickSearchButtonListItem;
import net.osmand.plus.search.listitems.QuickSearchDisabledHistoryItem;
import net.osmand.plus.search.listitems.QuickSearchHeaderListItem;
import net.osmand.plus.search.listitems.QuickSearchListItem;
import net.osmand.plus.search.listitems.QuickSearchMoreListItem;
import net.osmand.plus.search.listitems.QuickSearchMoreListItem.SearchMoreItemOnClickListener;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.enums.HistorySource;
import net.osmand.plus.settings.fragments.OnPreferenceChanged;
import net.osmand.plus.settings.fragments.SearchHistorySettingsFragment;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.mapwidgets.TopToolbarController;
import net.osmand.plus.widgets.tools.SimpleTextWatcher;
import net.osmand.search.SearchUICore;
import net.osmand.search.SearchUICore.SearchResultCollection;
import net.osmand.search.core.*;
import net.osmand.search.core.SearchCoreFactory.SearchAmenityTypesAPI;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public class QuickSearchDialogFragment extends BaseFullScreenDialogFragment implements OsmAndCompassListener,
		OsmAndLocationListener, DownloadEvents, OnPreferenceChanged {

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
	private static final double MIN_COMPASS_DEGREES_TO_UPDATE_CONTENT = 5.0;

	private Toolbar toolbar;
	private LockableViewPager viewPager;
	private View tabToolbarView;
	private View tabsView;
	private View searchView;
	private View buttonToolbarView;
	private View sendEmptySearchView;
	private ImageButton buttonToolbarFilter;
	private View buttonToolbarMap;
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

	private QuickSearchHelper searchHelper;
	private SearchUICore searchUICore;
	private SearchResultListener defaultResultListener;
	private String searchQuery;

	private LatLon centerLatLon;
	private net.osmand.Location location;
	private Float heading;
	private boolean useMapCenter;
	private boolean paused;
	private boolean cancelPrev;
	private boolean searching;
	private boolean hidden;
	private boolean foundPartialLocation;
	private String toolbarTitle;
	private boolean toolbarVisible;
	private boolean tabBarHidden;

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

	private static boolean isDebugMode = SearchUICore.isDebugMode();
	private ProcessTopIndex processTopIndexAfterLoad = ProcessTopIndex.NO;

	private enum ProcessTopIndex {
		FILTER,
		MAP,
		NO
	}

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
			return this != REGULAR;
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		FragmentActivity activity = requireActivity();
		navigationInfo = new NavigationInfo(app);
		accessibilityAssistant = new AccessibilityAssistant(activity);
	}

	@Override
	@SuppressLint("PrivateResource, ValidFragment")
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		updateNightMode();
		MapActivity mapActivity = requireMapActivity();
		View view = inflate(R.layout.search_dialog_fragment, container, false);

		toolbarController = new QuickSearchToolbarController(mapActivity);

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
		if (searchQuery == null) {
			searchQuery = "";
		}
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
		ImageView buttonToolbarImage = view.findViewById(R.id.buttonToolbarImage);
		buttonToolbarImage.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_marker_dark));
		buttonToolbarFilter = view.findViewById(R.id.filterButton);
		buttonToolbarFilter.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_filter_dark));
		buttonToolbarFilter.setOnClickListener(v -> {
			SearchPhrase searchPhrase = searchUICore.getPhrase();
			if (searchPhrase.isLastWord(POI_TYPE)) {
				String filterId = null;
				String filterByName = searchPhrase.getUnknownSearchPhrase().trim();
				Object object = searchPhrase.getLastSelectedWord().getResult().object;
				if (object instanceof PoiUIFilter model) {
					if (!Algorithms.isEmpty(model.getSavedFilterByName())) {
						model.setFilterByName(model.getSavedFilterByName());
					}
					filterId = model.getFilterId();
				} else if (object instanceof AbstractPoiType abstractPoiType) {
					PoiUIFilter custom = app.getPoiFilters().getFilterById(PoiUIFilter.STD_PREFIX + abstractPoiType.getKeyName());
					if (custom != null) {
						custom.setFilterByName(null);
						custom.clearFilter();
						custom.updateTypesToAccept(abstractPoiType);
						filterId = custom.getFilterId();
					}
				} else if (object instanceof TopIndexFilter topIndexFilter) {
					PoiUIFilter poiUIFilter = initPoiUIFilter(topIndexFilter, ProcessTopIndex.FILTER);
					if (poiUIFilter != null) {
						filterId = poiUIFilter.getFilterId();
						filterByName = topIndexFilter.getValue();
					}
				}
				if (filterId != null) {
					QuickSearchPoiFilterFragment.showInstance(QuickSearchDialogFragment.this, filterByName, filterId);
				}
			}
		});

		buttonToolbarText = view.findViewById(R.id.buttonToolbarTitle);
		buttonToolbarMap = view.findViewById(R.id.buttonToolbar);
		buttonToolbarMap.setOnClickListener(v -> {
					cancelSearch();
					SearchPhrase searchPhrase = searchUICore.getPhrase();
					PoiUIFilter poiUIFilter = ((QuickSearchListAdapter) mainSearchFragment.getAdapter()).getPoiUIFilter();
					if (poiUIFilter != null) {
						showFilterOnMap(poiUIFilter, getString(R.string.popular_places));
					} else if (foundPartialLocation) {
						QuickSearchCoordinatesFragment.showInstance(QuickSearchDialogFragment.this, searchPhrase.getFirstUnknownSearchWord());
					} else if (searchPhrase.isNoSelectedType() || searchPhrase.isLastWord(POI_TYPE)) {
						PoiUIFilter filter;
						Object object = searchPhrase.isLastWord(POI_TYPE) ? searchPhrase.getLastSelectedWord().getResult().object : null;
						if (object instanceof TopIndexFilter topIndexFilter) {
							filter = initPoiUIFilter(topIndexFilter, ProcessTopIndex.MAP);
							if (filter != null) {
								filter.setFilterByName(topIndexFilter.getValue());
								filter.setFilterByKey(topIndexFilter.getTag());
							} else {
								return;
							}
						} else {
							filter = app.getPoiFilters().getShowOnMapFilter(searchPhrase);
						}
						showFilterOnMap(filter, getText());
					} else {
						SearchWord word = searchPhrase.getLastSelectedWord();
						if (word != null) {
							if (searchType.isTargetPoint() && word.getLocation() != null) {
								if (mainSearchFragment != null) {
									mainSearchFragment.showResult(word.getResult());
								}
							} else if (word.getLocation() != null) {
								SearchResult searchResult = word.getResult();
								Object object = searchResult.object;

								if (word.getType() == ObjectType.CITY || word.getType() == ObjectType.VILLAGE) {
									Amenity amenity = app.getSearchUICore().findAmenity(searchResult.localeName,
											searchResult.location.getLatitude(), searchResult.location.getLongitude());
									if (amenity != null) {
										object = amenity;
									}
								}

								String name = QuickSearchListItem.getName(app, searchResult);
								String typeName = QuickSearchListItem.getTypeName(app, searchResult);
								PointDescription pointDescription = new PointDescription(
										PointDescription.POINT_TYPE_ADDRESS, typeName, name);
								settings.setMapLocationToShow(
										searchResult.location.getLatitude(), searchResult.location.getLongitude(),
										searchResult.preferredZoom, pointDescription, true, object);

								hideToolbar();
								MapActivity.launchMapActivityMoveToTop(requireActivity());
								reloadHistory();
								hide();
							} else if (word.getType() == ObjectType.FAVORITE_GROUP) {
								FavoriteGroup group = (FavoriteGroup) word.getResult().object;
								if (group.getPoints().size() > 1) {
									double left = 0, right = 0;
									double top = 0, bottom = 0;
									for (FavouritePoint p : group.getPoints()) {
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
									MapActivity.launchMapActivityMoveToTop(requireActivity());
									hide();
								} else if (group.getPoints().size() == 1) {
									FavouritePoint p = group.getPoints().get(0);
									app.getSettings().setMapLocationToShow(p.getLatitude(), p.getLongitude(), word.getResult().preferredZoom);
									hideToolbar();
									MapActivity.launchMapActivityMoveToTop(requireActivity());
									hide();
								}
							}
						}
					}
				}

		);

		toolbar = view.findViewById(R.id.toolbar);
		if (nightMode) {
			toolbar.setBackgroundColor(ContextCompat.getColor(mapActivity, R.color.app_bar_main_dark));
		}
		Drawable icBack = iconsCache.getThemedIcon(AndroidUtils.getNavigationIconResId(app));
		toolbar.setNavigationIcon(icBack);
		toolbar.setNavigationContentDescription(R.string.access_shared_string_navigate_up);
		toolbar.setNavigationOnClickListener(v -> onBackButtonPressed());

		toolbarEdit = view.findViewById(R.id.toolbar_edit);
		toolbarEdit.setNavigationIcon(iconsCache.getIcon(R.drawable.ic_action_remove_dark));
		toolbarEdit.setNavigationContentDescription(R.string.shared_string_cancel);
		toolbarEdit.setNavigationOnClickListener(v -> enableSelectionMode(false, -1));

		titleEdit = view.findViewById(R.id.titleEdit);
		Drawable shareIcon = iconsCache.getIcon(R.drawable.ic_action_gshare_dark, R.color.card_and_list_background_light);
		shareIcon = AndroidUtils.getDrawableForDirection(app, shareIcon);
		ImageView shareButton = view.findViewById(R.id.shareButton);
		shareButton.setImageDrawable(shareIcon);
		shareButton.setOnClickListener(v -> {
			List<HistoryEntry> historyEntries = new ArrayList<HistoryEntry>();
			List<QuickSearchListItem> selectedItems = historySearchFragment.getListAdapter().getSelectedItems();
			for (QuickSearchListItem searchListItem : selectedItems) {
				Object object = searchListItem.getSearchResult().object;
				if (object instanceof HistoryEntry) {
					historyEntries.add((HistoryEntry) object);
				}
			}
			if (!historyEntries.isEmpty()) {
				shareHistory(historyEntries);
				enableSelectionMode(false, -1);
			}
		});
		view.findViewById(R.id.deleteButton).setOnClickListener(v -> {
			List<QuickSearchListItem> items = historySearchFragment.getListAdapter().getSelectedItems();
			DeleteHistoryConfirmationFragment.showInstance(getChildFragmentManager(), items);
		});

		viewPager = view.findViewById(R.id.pager);
		viewPager.setOffscreenPageLimit(2);
		SearchFragmentPagerAdapter pagerAdapter = new SearchFragmentPagerAdapter(requireContext(), getChildFragmentManager());
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

		TabLayout tabLayout = view.findViewById(R.id.tab_layout);
		tabLayout.setupWithViewPager(viewPager);
		viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
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
		});

		searchEditText = view.findViewById(R.id.searchEditText);
		searchEditText.setOnEditorActionListener((v, actionId, event) -> {
			if (actionId == EditorInfo.IME_ACTION_SEARCH) {
				String newQueryText = searchQuery + " ";
				searchEditText.setText(newQueryText);
				searchEditText.setSelection(newQueryText.length());
				AndroidUtils.hideSoftKeyboard(requireActivity(), searchEditText);
				return true;
			}
			return false;
		});
		searchEditText.addTextChangedListener(new SimpleTextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				String newQueryText = s.toString();
				updateClearButtonAndHint();
				updateClearButtonVisibility(true);
				boolean textEmpty = newQueryText.isEmpty();
				updateTabBarVisibility(textEmpty && !searchUICore.isOnlineSearch());
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
		});

		progressBar = view.findViewById(R.id.searchProgressBar);
		clearButton = view.findViewById(R.id.clearButton);
		clearButton.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_remove_dark));
		clearButton.setOnClickListener(v -> {
			if (!searchEditText.getText().toString().isEmpty()) {
				clearLastWord();
			} else if (useMapCenter && location != null) {
				useMapCenter = false;
				centerLatLon = null;
				updateUseMapCenterUI();
				LatLon centerLatLon = new LatLon(location.getLatitude(), location.getLongitude());
				SearchSettings ss = searchUICore.getSearchSettings().setOriginalLocation(new LatLon(centerLatLon.getLatitude(), centerLatLon.getLongitude()));
				searchUICore.updateSettings(ss);
				updateClearButtonAndHint();
				updateClearButtonVisibility(true);
				startLocationUpdate();
			}
			updateSendEmptySearchBottomBar(false);
			updateToolbarButton();
		});

		fab = view.findViewById(R.id.fab);
		fab.setOnClickListener(v -> saveCustomFilter());
		updateFab();

		setupSearch(mapActivity);

		sendEmptySearchView = view.findViewById(R.id.no_search_results_bottom_bar);
		sendEmptySearchText = view.findViewById(R.id.no_search_results_description);
		sendEmptySearchButton = view.findViewById(R.id.send_empty_search_button);
		sendEmptySearchButton.setOnClickListener(v -> {
			if (!settings.isInternetConnectionAvailable()) {
				app.showToastMessage(R.string.internet_not_available);
			} else {
				if (searchQuery != null) {
					SendSearchQueryBottomSheet.showInstance(mapActivity, String.valueOf(location), searchQuery);
				}
			}
		});
		updateFab();

		return view;
	}

	@Nullable
	@Override
	public List<Integer> getBottomContainersIds() {
		List<Integer> ids = new ArrayList<>();
		ids.add(R.id.bottom_buttons_container);
		return ids;
	}

	private void showFilterOnMap(@Nullable PoiUIFilter filter, @Nullable String title) {
		MapActivity activity = getMapActivity();
		if (activity != null) {
			MapContextMenu contextMenu = activity.getContextMenu();
			contextMenu.close();
			contextMenu.closeActiveToolbar();

			showToolbar(filter, title);
			activity.updateStatusBarColor();
			activity.refreshMap();

			if (filter != null) {
				FragmentManager manager = activity.getSupportFragmentManager();
				ExplorePlacesFragment.Companion.showInstance(manager, filter);
			}

			hide();
		}
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
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
	public Dialog createDialog(Bundle savedInstanceState) {
		return new Dialog(requireActivity(), getTheme()) {
			@Override
			public void onBackPressed() {
				onBackButtonPressed();
			}
		};
	}

	private void onBackButtonPressed() {
		if (tabBarHidden) {
			hideKeyboard();
			searchEditText.setText("");
			updateTabBarVisibility(true);
		} else if (!processBackAction()) {
			Dialog dialog = getDialog();
			if (dialog != null) {
				dialog.cancel();
			}
		}
		app.getPoiFilters().restoreSelectedPoiFilters();
	}

	public void saveCustomFilter() {
		PoiUIFilter filter = app.getPoiFilters().getCustomPOIFilter();
		AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
		builder.setTitle(R.string.access_hint_enter_name);

		EditText editText = new EditText(getContext());
		editText.setHint(R.string.new_filter);

		TextView textView = new TextView(getContext());
		textView.setText(getString(R.string.new_filter_desc));
		textView.setTextAppearance(R.style.TextAppearance_ContextMenuSubtitle);
		LinearLayout ll = new LinearLayout(getContext());
		ll.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
		ll.setOrientation(LinearLayout.VERTICAL);
		ll.setPadding(dpToPx(20f), dpToPx(12f), dpToPx(20f), dpToPx(12f));
		ll.addView(editText, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
		textView.setPadding(dpToPx(4f), dpToPx(6f), dpToPx(4f), dpToPx(4f));
		ll.addView(textView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

		builder.setView(ll);

		builder.setNegativeButton(R.string.shared_string_cancel, null);
		builder.setPositiveButton(R.string.shared_string_save, (dialog, which) -> {
			PoiUIFilter nFilter = new PoiUIFilter(editText.getText().toString(), null, filter.getAcceptedTypes(), app);
			if (!Algorithms.isEmpty(filter.getFilterByName())) {
				nFilter.setSavedFilterByName(filter.getFilterByName());
			}
			if (app.getPoiFilters().createPoiFilter(nFilter, false)) {
				app.showShortToastMessage(R.string.edit_filter_create_message, editText.getText().toString());
				app.getSearchUICore().refreshCustomPoiFilters();
				replaceQueryWithUiFilter(nFilter, "");
				reloadCategories();

				fabVisible = false;
				updateFab();
			}
		});
		builder.create().show();
	}

	public void restoreToolbar() {
		if (toolbarVisible) {
			MapActivity mapActivity = getMapActivity();
			TopToolbarController controller = mapActivity.getTopToolbarController(toolbarController.getType());
			if (controller == null) {
				PoiUIFilter filter = toolbarController.getSelectedFilter();
				if (toolbarTitle != null) {
					showToolbar(filter, toolbarTitle);
				} else {
					showToolbar(filter);
				}
			}
		}
	}

	public void showToolbar(@Nullable PoiUIFilter filter) {
		showToolbar(filter, getText());
	}

	public void showToolbar(@Nullable PoiUIFilter filter, String title) {
		toolbarVisible = true;
		toolbarTitle = title;
		if (filter != null) {
			app.getPoiFilters().replaceSelectedPoiFilters(filter);
		}
		toolbarController.setSelectedFilter(filter);
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
		Activity activity = getActivity();
		if (activity != null && searchEditText.hasFocus()) {
			AndroidUtils.hideSoftKeyboard(activity, searchEditText);
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
			SearchSettings ss = searchUICore.getSearchSettings().setOriginalLocation(new LatLon(mapCenter.getLatitude(), mapCenter.getLongitude()));
			searchUICore.updateSettings(ss);
			updateUseMapCenterUI();
			updateContent(null);
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
		visibilityChanged(true);
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
		visibilityChanged(false);
	}

	private void visibilityChanged(boolean visible) {
		for (Fragment fragment : getChildFragmentManager().getFragments()) {
			if (fragment instanceof SearchVisibilityListener listener && fragment.isAdded()) {
				listener.onVisibilityChanged(visible);
			}
		}
	}

	public void closeSearch() {
		app.getPoiFilters().restoreSelectedPoiFilters();
		dismiss();
	}

	public void addMainSearchFragment() {
		mainSearchFragment = new QuickSearchMainListFragment();
		FragmentManager childFragmentManager = getChildFragmentManager();
		String tag = mainSearchFragment.getClass().getName();
		childFragmentManager.beginTransaction()
				.replace(R.id.search_view, mainSearchFragment, tag)
				.commitAllowingStateLoss();
	}

	private void updateToolbarButton() {
		SearchWord word = searchUICore.getPhrase().getLastSelectedWord();
		if (foundPartialLocation) {
			buttonToolbarText.setText(getString(R.string.advanced_coords_search).toUpperCase());
		} else if (!Algorithms.isEmpty(searchEditText.getText())) {
			if (searchType.isTargetPoint()) {
				if (word != null && word.getResult() != null) {
					buttonToolbarText.setText(getString(R.string.shared_string_select).toUpperCase() + " " + word.getResult().localeName.toUpperCase());
				} else {
					buttonToolbarText.setText(getString(R.string.shared_string_select).toUpperCase());
				}
			} else {
				if (word != null && word.getResult() != null) {
					buttonToolbarText.setText(getString(R.string.show_something_on_map, word.getResult().localeName).toUpperCase());
				} else {
					buttonToolbarText.setText(getString(R.string.shared_string_show_on_map).toUpperCase());
				}
			}
		} else {
			buttonToolbarText.setText(getString(R.string.shared_string_show_on_map).toUpperCase());
		}
		boolean filterButtonVisible = word != null && word.getType() != null && word.getType().equals(POI_TYPE);
		buttonToolbarFilter.setVisibility(filterButtonVisible ? View.VISIBLE : View.GONE);
		if (filterButtonVisible) {
			if (word.getResult().object instanceof PoiUIFilter) {
				int activeColor = ColorUtilities.getActiveColorId(nightMode);
				buttonToolbarFilter.setImageDrawable(getIcon(R.drawable.ic_action_filter_dark, activeColor));
				buttonToolbarFilter.setImageDrawable(getIcon(R.drawable.ic_action_filter_dark, activeColor));
			} else {
				buttonToolbarFilter.setImageDrawable(app.getUIUtilities().getThemedIcon(R.drawable.ic_action_filter_dark));
			}
		}
	}

	private void setupSearch(MapActivity mapActivity) {
		// Setup search core
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

		String appLang = app.getLanguage();
		String mapLang = settings.MAP_PREFERRED_LOCALE.get();
		boolean transliterate = settings.MAP_TRANSLITERATE_NAMES.get();

		SearchSettings settings = searchUICore.getSearchSettings().setOriginalLocation(
				new LatLon(searchLatLon.getLatitude(), searchLatLon.getLongitude()));
		settings = settings.setLangs(appLang, mapLang, transliterate);
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
	public void onStart() {
		super.onStart();
		if (isSearchHidden()) {
			hide();
			restoreToolbar();
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
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
		super.onSaveInstanceState(outState);
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
		visibilityChanged(true);
	}

	@Override
	public void onPause() {
		super.onPause();
		paused = true;
		pausedSearch = searching;
		hideTimeMs = System.currentTimeMillis();
		stopLocationUpdate();
		hideProgressBar();
		visibilityChanged(false);
	}

	@Override
	public void onDismiss(@NonNull DialogInterface dialog) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			hideToolbar();
			mapActivity.updateStatusBarColor();
			mapActivity.refreshMap();
			FragmentManager fragmentManager = getChildFragmentManager();
			if (!fragmentManager.isStateSaved()) {
				fragmentManager.popBackStack();
			}
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
		OsmAndLocationProvider locationProvider = app.getLocationProvider();
		locationProvider.removeCompassListener(locationProvider.getNavigationInfo());
		locationProvider.addCompassListener(this);
		locationProvider.addLocationListener(this);
		location = locationProvider.getLastKnownLocation();
		updateLocation(location);
	}

	private void stopLocationUpdate() {
		OsmAndLocationProvider locationProvider = app.getLocationProvider();
		locationProvider.removeLocationListener(this);
		locationProvider.removeCompassListener(this);
		locationProvider.addCompassListener(locationProvider.getNavigationInfo());
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
		searchEditText.setEnabled(true);
		if (useMapCenter && location != null && searchEditText.length() == 0) {
			LatLon latLon = searchUICore.getSearchSettings().getOriginalLocation();
			double d = MapUtils.getDistance(latLon, location.getLatitude(), location.getLongitude());
			String dist = OsmAndFormatter.getFormattedDistance((float) d, app);
			searchEditText.setHint(getString(R.string.dist_away_from_my_location, dist));
			clearButton.setImageDrawable(getIcon(R.drawable.ic_action_get_my_location, R.color.color_myloc_distance));
		} else {
			if (addressSearch) {
				searchEditText.setHint(R.string.type_address);
			} else {
				searchEditText.setHint(R.string.search_poi_category_hint);
			}
			clearButton.setImageDrawable(getContentIcon(R.drawable.ic_action_remove_dark));
		}
	}

	private void updateClearButtonVisibility(boolean show) {
		if (show) {
			clearButton.setVisibility(searchEditText.length() > 0 || (useMapCenter && location != null) ? View.VISIBLE : View.GONE);
		} else {
			clearButton.setVisibility(View.GONE);
		}
	}

	private void updateTabBarVisibility(boolean show) {
		tabBarHidden = !show;
		if (show) {
			tabToolbarView.setVisibility(View.VISIBLE);
			buttonToolbarView.setVisibility(View.GONE);
			tabsView.setVisibility(View.VISIBLE);
			searchView.setVisibility(View.GONE);
		} else {
			tabToolbarView.setVisibility(View.GONE);
			SearchWord lastWord = searchUICore.getPhrase().getLastSelectedWord();
			boolean buttonToolbarVisible = (searchUICore.isOnlineSearch() && !isTextEmpty())
					|| !searchUICore.getSearchSettings().isCustomSearch();
			if (searchType.isTargetPoint() && (lastWord == null || lastWord.getLocation() == null)) {
				buttonToolbarVisible = false;
			}
			buttonToolbarView.setVisibility(buttonToolbarVisible ? View.VISIBLE : View.GONE);
			tabsView.setVisibility(View.GONE);
			searchView.setVisibility(View.VISIBLE);
		}
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
		return res == null || res.getCurrentSearchResults().isEmpty();
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
					onSearchFinished(searchUICore.getPhrase());
				} else if (!Algorithms.isEmpty(searchQuery)) {
					runSearch();
				}
				break;
		}
		if (useMapCenter) {
			updateUseMapCenterUI();
			searchListFragment.updateLocation(null);
		}
	}

	public void reloadCategories() {
		if (app.isApplicationInitializing()) {
			showProgressBar();
			app.getAppInitializer().addOnFinishListener(result -> {
				if (!paused) {
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
			if (isDebugMode) {
				LOG.info("UI >> Start loading categories");
			}
			SearchResultCollection res = searchUICore.shallowSearch(SearchAmenityTypesAPI.class, "", null);
			if (res != null) {
				List<QuickSearchListItem> rows = new ArrayList<>();
				for (SearchResult sr : res.getCurrentSearchResults()) {
					rows.add(new QuickSearchListItem(app, sr));
				}
				rows.add(new QuickSearchButtonListItem(app, R.drawable.ic_world_globe_dark, getString(R.string.search_online_address), view -> {
					OsmandSettings settings = app.getSettings();
					if (!settings.isInternetConnectionAvailable()) {
						app.showToastMessage(R.string.internet_not_available);
						return;
					}
					startOnlineSearch();
					mainSearchFragment.getAdapter().clear();
					updateTabBarVisibility(false);
					openKeyboard();
				}));
				rows.add(new QuickSearchButtonListItem(app, R.drawable.ic_action_search_dark, getString(R.string.custom_search), v -> {
					PoiUIFilter filter = app.getPoiFilters().getCustomPOIFilter();
					filter.clearFilter();
					QuickSearchCustomPoiFragment.showInstance(QuickSearchDialogFragment.this, filter.getFilterId());
				}));
				rows.add(new QuickSearchButtonListItem(app, R.drawable.ic_action_item_move, getString(R.string.rearrange_categories), v -> {
					ApplicationMode appMode = settings.getApplicationMode();
					RearrangePoiFiltersFragment.showInstance(appMode, QuickSearchDialogFragment.this, false, new RearrangePoiFiltersFragment.OnApplyPoiFiltersState() {

						@Override
						public void onApplyPoiFiltersState(ApplicationMode appMode, boolean stateChanged) {
							if (stateChanged) {
								searchHelper.refreshCustomPoiFilters();
								reloadCategoriesInternal();
							}
							View containerView = getView();
							if (containerView != null) {
								//show "Apply to all profiles" SnackBar
								String modeName = appMode.toHumanString();
								String text = getString(R.string.changes_applied_to_profile, modeName);
								SpannableString message = UiUtilities.createSpannableString(text, Typeface.BOLD, modeName);
								Snackbar snackbar = Snackbar.make(containerView, message, Snackbar.LENGTH_LONG).setAction(R.string.apply_to_all_profiles, view -> {
									String orders = settings.POI_FILTERS_ORDER.getModeValue(appMode);
									String inactive = settings.INACTIVE_POI_FILTERS.getModeValue(appMode);
									for (ApplicationMode mode : ApplicationMode.allPossibleValues()) {
										settings.POI_FILTERS_ORDER.setModeValue(mode, orders);
										settings.INACTIVE_POI_FILTERS.setModeValue(mode, inactive);
									}
									searchHelper.refreshCustomPoiFilters();
									reloadCategoriesInternal();
								});
								UiUtilities.setupSnackbarVerticalLayout(snackbar);
								UiUtilities.setupSnackbar(snackbar, nightMode);
								snackbar.show();
							}
						}

						@Override
						public void onCustomFiltersDeleted() {
							searchHelper.refreshCustomPoiFilters();
							reloadCategoriesInternal();
						}
					});
				}));
				if (categoriesSearchFragment != null) {
					categoriesSearchFragment.updateListAdapter(rows, false);
				}
			}
			if (isDebugMode) {
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
			app.getAppInitializer().addOnFinishListener(result -> {
				if (!paused) {
					reloadCitiesInternal();
					if (!searching) {
						hideProgressBar();
					}
				}
			});
		} else {
			reloadCitiesInternal();
		}
	}

	private void reloadCitiesInternal() {
		if (isDebugMode) {
			LOG.info("UI >> Start loading nearest cities");
		}
		updateCitiesItems();
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
				if (isDebugMode) {
					LOG.info("UI >> Nearest cities found: " + getSearchResultCollectionFormattedSize(getResultCollection()));
				}
				updateCitiesItems();
				if (isDebugMode) {
					LOG.info("UI >> Nearest cities loaded");
				}
				return true;
			}
		});
		restoreSearch();
	}

	private void updateCitiesItems() {
		SearchResultCollection res = getResultCollection();
		List<QuickSearchListItem> rows = new ArrayList<>();

		if (isDebugMode) {
			LOG.info("UI >> Start last city searching (within nearests)");
		}
		SearchResult lastCity = null;
		if (res != null) {
			citiesLoaded = !res.getCurrentSearchResults().isEmpty();
			long lastCityId = settings.getLastSearchedCity();
			for (SearchResult sr : res.getCurrentSearchResults()) {
				if (sr.objectType == ObjectType.CITY && ((City) sr.object).getId() == lastCityId) {
					lastCity = sr;
					break;
				}
			}
		}
		if (isDebugMode) {
			LOG.info("UI >> Last city found: " + (lastCity != null ? lastCity.localeName : "-"));
		}

		String lastCityName = lastCity == null ? settings.getLastSearchedCityName() : lastCity.localeName;
		if (!Algorithms.isEmpty(lastCityName)) {
			String selectStreets = getString(R.string.search_street).toUpperCase();
			String inCityName = getString(R.string.shared_string_in_name, lastCityName);
			Spannable spannable = new SpannableString((selectStreets + " " + inCityName).toUpperCase());
			spannable.setSpan(new ForegroundColorSpan(getColor(!nightMode ? R.color.icon_color_default_light : R.color.card_and_list_background_light)),
					selectStreets.length() + 1, spannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

			SearchResult lastCityFinal = lastCity;
			rows.add(new QuickSearchButtonListItem(app, R.drawable.ic_action_street_name, spannable, v -> {
				if (lastCityFinal == null) {
					long lastCityId = settings.getLastSearchedCity();
					LatLon lastCityPoint = settings.getLastSearchedPoint();
					if (lastCityId != -1 && lastCityPoint != null) {
						startLastCitySearch(lastCityPoint);
						if (isDebugMode) {
							LOG.info("UI >> Start last city searching (standalone)");
						}
						runCoreSearch("", false, false, new SearchResultListener() {

							boolean cityFound;

							@Override
							public void publish(SearchResultCollection res1, boolean append) {
								if (res1 != null) {
									for (SearchResult sr : res1.getCurrentSearchResults()) {
										if (sr.objectType == ObjectType.CITY && ((City) sr.object).getId() == lastCityId) {
											if (isDebugMode) {
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
			}));
		}
		rows.add(new QuickSearchButtonListItem(app, R.drawable.ic_action_building_number, getString(R.string.start_search_from_city), v -> {
			searchEditText.setHint(R.string.type_city_town);
			startCitySearch();
			updateTabBarVisibility(false);
			runCoreSearch("", false, false);
			openKeyboard();
		}));
		rows.add(new QuickSearchButtonListItem(app, R.drawable.ic_action_postcode, getString(R.string.select_postcode), v -> {
			searchEditText.setHint(R.string.type_postcode);
			startPostcodeSearch();
			mainSearchFragment.getAdapter().clear();
			updateTabBarVisibility(false);
			openKeyboard();
		}));
		rows.add(new QuickSearchButtonListItem(app, R.drawable.ic_action_marker_dark, getString(R.string.coords_search), v -> {
			LatLon latLon = searchUICore.getSearchSettings().getOriginalLocation();
			QuickSearchCoordinatesFragment.showInstance(QuickSearchDialogFragment.this, latLon.getLatitude(), latLon.getLongitude());
		}));

		if (res != null) {
			rows.add(new QuickSearchHeaderListItem(app, getString(R.string.nearest_cities), true));
			int limit = 15;
			for (SearchResult sr : res.getCurrentSearchResults()) {
				if (limit > 0) {
					rows.add(new QuickSearchListItem(app, sr));
				}
				limit--;
			}
		}
		if (addressSearchFragment != null) {
			addressSearchFragment.updateListAdapter(rows, false);
		}
	}

	public void reloadHistory() {
		if (app.isApplicationInitializing()) {
			showProgressBar();
			app.getAppInitializer().addOnFinishListener(result -> {
				if (!paused) {
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
		if (historySearchFragment != null) {
			try {
				List<QuickSearchListItem> rows = new ArrayList<>();
				boolean historyEnabled = settings.SEARCH_HISTORY.get();
				if (historyEnabled) {
					SearchResultCollection res = searchUICore.shallowSearch(SearchHistoryAPI.class, "", null, false, false);
					if (res != null) {
						for (SearchResult sr : res.getCurrentSearchResults()) {
							rows.add(new QuickSearchListItem(app, sr));
						}
					}
				} else {
					OnClickListener listener = v -> {
						FragmentManager fragmentManager = getFragmentManager();
						if (fragmentManager != null) {
							SearchHistorySettingsFragment.showInstance(fragmentManager, this);
						}
					};
					rows.add(new QuickSearchDisabledHistoryItem(app, listener));
				}
				historySearchFragment.updateListAdapter(rows, false, historyEnabled);
			} catch (Exception e) {
				LOG.error(e);
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
		SearchSettings onlineSettings = searchUICore.getSearchSettings().setSearchTypes(ONLINE_SEARCH)
				.setEmptyQueryAllowed(false)
				.setSortByName(false)
				.setRadiusLevel(1);
		searchUICore.updateSettings(onlineSettings);
		setResultCollection(null);
	}

	private void startAddressSearch() {
		SearchSettings addressSettings = searchUICore.getSearchSettings().setEmptyQueryAllowed(true)
				.setSortByName(false)
				.setSearchTypes(CITY, VILLAGE, POSTCODE, HOUSE, STREET_INTERSECTION, STREET, LOCATION, PARTIAL_LOCATION)
				.setRadiusLevel(1);
		searchUICore.updateSettings(addressSettings);
	}

	private void startCitySearch() {
		SearchSettings citySettings = searchUICore.getSearchSettings().setEmptyQueryAllowed(true)
				.setSortByName(true)
				.setSearchTypes(CITY, VILLAGE)
				.setRadiusLevel(1);
		searchUICore.updateSettings(citySettings);
	}

	private void startNearestCitySearch() {
		SearchSettings nearestSettings = searchUICore.getSearchSettings().setEmptyQueryAllowed(true)
				.setSortByName(false)
				.setSearchTypes(CITY)
				.setRadiusLevel(1);
		searchUICore.updateSettings(nearestSettings);
	}

	private void startLastCitySearch(@NonNull LatLon latLon) {
		SearchSettings settings = searchUICore.getSearchSettings()
				.setEmptyQueryAllowed(true)
				.setSortByName(false)
				.setSearchTypes(CITY)
				.setRadiusLevel(1);
		storedOriginalLocation = settings.getOriginalLocation();
		settings.setOriginalLocation(latLon);
		searchUICore.updateSettings(settings);
	}

	private void startPostcodeSearch() {
		SearchSettings postcodeSettings = searchUICore.getSearchSettings().setSearchTypes(POSTCODE)
				.setEmptyQueryAllowed(false)
				.setSortByName(true)
				.setRadiusLevel(1);
		searchUICore.updateSettings(postcodeSettings);
	}

	private void stopAddressSearch() {
		SearchSettings stopAddressSettings = searchUICore.getSearchSettings().resetSearchTypes()
				.setEmptyQueryAllowed(false)
				.setSortByName(false)
				.setRadiusLevel(1);
		searchUICore.updateSettings(stopAddressSettings);
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

	private void runCoreSearch(String text, boolean showQuickResult, boolean searchMore) {
		runCoreSearch(text, showQuickResult, searchMore, defaultResultListener);
	}

	private void runCoreSearch(String text, boolean showQuickResult, boolean searchMore, SearchResultListener resultListener) {
		showProgressBar();
		foundPartialLocation = false;
		updateToolbarButton();
		interruptedSearch = false;
		searching = true;
		cancelPrev = true;

		if (app.isApplicationInitializing() && !text.isEmpty()) {
			app.getAppInitializer().addOnFinishListener(result -> {
				if (!paused) {
					runCoreSearchInternal(text, showQuickResult, searchMore, resultListener);
				}
			});
		} else {
			runCoreSearchInternal(text, showQuickResult, searchMore, resultListener);
		}
	}

	private void runCoreSearchInternal(String text, boolean showQuickResult, boolean searchMore, SearchResultListener resultListener) {
		searchUICore.search(text, showQuickResult, new ResultMatcher<SearchResult>() {
			SearchResultCollection regionResultCollection;
			SearchCoreAPI regionResultApi;
			List<SearchResult> results = new ArrayList<>();

			@Override
			public boolean publish(SearchResult object) {
				if (object.objectType == SEARCH_STARTED) {
					cancelPrev = false;
				}
				if (paused || cancelPrev) {
					return false;
				}
				switch (object.objectType) {
					case SEARCH_STARTED:
						if (resultListener != null) {
							app.runInUIThread(() -> resultListener.searchStarted(object.requiredSearchPhrase));
						}
						break;
					case SEARCH_FINISHED:
						app.runInUIThread(() -> {
							if (paused) {
								return;
							}
							searching = false;
							if (resultListener == null || resultListener.searchFinished(object.requiredSearchPhrase)) {
								hideProgressBar();
								SearchPhrase phrase = object.requiredSearchPhrase;
								onSearchFinished(phrase);
							}
						});
						break;
					case FILTER_FINISHED:
						if (resultListener != null) {
							app.runInUIThread(() -> resultListener.publish(searchUICore.getCurrentSearchResult(), false));
						}
						break;
					case SEARCH_API_FINISHED:
						SearchCoreAPI searchApi = (SearchCoreAPI) object.object;
						List<SearchResult> apiResults;
						SearchPhrase phrase = object.requiredSearchPhrase;
						SearchCoreAPI regionApi = regionResultApi;
						SearchResultCollection regionCollection = regionResultCollection;
						boolean hasRegionCollection = (searchApi == regionApi && regionCollection != null);
						if (hasRegionCollection) {
							apiResults = regionCollection.getCurrentSearchResults();
						} else {
							apiResults = results;
						}
						regionResultApi = null;
						regionResultCollection = null;
						results = new ArrayList<>();
						showApiResults(searchApi, apiResults, phrase, hasRegionCollection, resultListener);
						switch (processTopIndexAfterLoad) {
							case FILTER:
								app.runInUIThread(() -> {
									buttonToolbarFilter.performClick();
								});
								break;
							case MAP:
								app.runInUIThread(() -> {
									buttonToolbarMap.performClick();
								});
								break;
						}
						processTopIndexAfterLoad = ProcessTopIndex.NO;
						break;
					case SEARCH_API_REGION_FINISHED:
						regionResultApi = (SearchCoreAPI) object.object;
						SearchPhrase regionPhrase = object.requiredSearchPhrase;
						regionResultCollection = new SearchResultCollection(regionPhrase).addSearchResults(results, true, true);
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
		app.runInUIThread(() -> {
			foundPartialLocation = true;
			updateToolbarButton();
		});
	}

	private void showApiResults(SearchCoreAPI searchApi,
	                            List<SearchResult> apiResults,
	                            SearchPhrase phrase,
	                            boolean hasRegionCollection,
	                            SearchResultListener resultListener) {
		app.runInUIThread(() -> {
			if (!paused && !cancelPrev) {
				if (isDebugMode) {
					LOG.info("UI >> Showing API results <" + phrase + "> API=<" + searchApi + "> Results=" + apiResults.size());
				}
				boolean append = getResultCollection() != null;
				if (append) {
					if (isDebugMode) {
						LOG.info("UI >> Appending API results <" + phrase + "> API=<" + searchApi + "> Result collection=" + getSearchResultCollectionFormattedSize(getResultCollection()));
					}
					getResultCollection().addSearchResults(apiResults, true, true);
					if (isDebugMode) {
						LOG.info("UI >> API results appended <" + phrase + "> API=<" + searchApi + "> Result collection=" + getSearchResultCollectionFormattedSize(getResultCollection()));
					}
				} else {
					if (isDebugMode) {
						LOG.info("UI >> Assign API results <" + phrase + "> API=<" + searchApi + ">");
					}
					SearchResultCollection resCollection = new SearchResultCollection(phrase);
					resCollection.addSearchResults(apiResults, true, true);
					setResultCollection(resCollection);
					if (isDebugMode) {
						LOG.info("UI >> API results assigned <" + phrase + "> API=<" + searchApi + "> Result collection=" + getSearchResultCollectionFormattedSize(getResultCollection()));
					}
				}
				if (!hasRegionCollection && resultListener != null) {
					resultListener.publish(getResultCollection(), append);
				}
				if (isDebugMode) {
					LOG.info("UI >> API results shown <" + phrase + "> API=<" + searchApi + "> Results=" + getSearchResultCollectionFormattedSize(getResultCollection()));
				}
			}
		});
	}

	private void showRegionResults(BinaryMapIndexReader region,
	                               SearchPhrase phrase,
	                               SearchResultCollection regionResultCollection,
	                               SearchResultListener resultListener) {
		app.runInUIThread(() -> {
			if (!paused && !cancelPrev) {
				if (isDebugMode) {
					LOG.info("UI >> Showing region results <" + phrase + "> Region=<" + region.getFile().getName() + "> Results=" + getSearchResultCollectionFormattedSize(regionResultCollection));
				}
				if (getResultCollection() != null) {
					if (isDebugMode) {
						LOG.info("UI >> Combining region results <" + phrase + "> Region=<" + region.getFile().getName() + "> Result collection=" + getSearchResultCollectionFormattedSize(getResultCollection()));
					}
					SearchResultCollection resCollection = getResultCollection().combineWithCollection(regionResultCollection, true, true);
					if (isDebugMode) {
						LOG.info("UI >> Region results combined <" + phrase + "> Region=<" + region.getFile().getName() + "> Result collection=" + getSearchResultCollectionFormattedSize(resCollection));
					}
					if (resultListener != null) {
						resultListener.publish(resCollection, true);
					}
					if (isDebugMode) {
						LOG.info("UI >> Region results shown <" + phrase + "> Region=<" + region.getFile().getName() + "> Results=" + getSearchResultCollectionFormattedSize(resCollection));
					}
				} else if (resultListener != null) {
					resultListener.publish(regionResultCollection, false);
					if (isDebugMode) {
						LOG.info("UI >> Region results shown <" + phrase + "> Region=<" + region.getFile().getName() + "> Results=" + getSearchResultCollectionFormattedSize(regionResultCollection));
					}
				}
			}
		});
	}

	private String getSearchResultCollectionFormattedSize(@Nullable SearchResultCollection resultCollection) {
		return resultCollection != null ? String.valueOf(resultCollection.getCurrentSearchResults().size()) : "empty";
	}

	public void completeQueryWithObject(@NonNull SearchResult result) {
		app.getSearchHistoryHelper().selectSearchResult(result);

		if (result.object instanceof AbstractPoiType || result.object instanceof PoiUIFilter) {
			reloadHistory();
		}
		if (addressSearch) {
			startAddressSearch();
			if (result.objectType == ObjectType.CITY) {
				if (result.relatedObject instanceof BinaryMapIndexReader) {
					File file = ((BinaryMapIndexReader) result.relatedObject).getFile();
					if (file != null) {
						RegionAddressRepository region = app.getResourceManager().getRegionRepository(file.getName());
						if (region != null) {
							City city = (City) result.object;
							String lastSearchedRegion = app.getSettings().getLastSearchedRegion();
							long lastCityId = settings.getLastSearchedCity();
							if (!lastSearchedRegion.equals(region.getFileName()) || city.getId() != lastCityId) {
								settings.setLastSearchedRegion(region.getFileName(), region.getEstimatedRegionCenter());
								settings.setLastSearchedCity(city.getId(), result.localeName, city.getLocation());
								updateCitiesItems();
							}
						}
					}
				}
			}
		}
		String txt = searchUICore.getPhrase().getText(true);
		replaceQueryWithText(txt);
		if (result.objectType == ObjectType.CITY) {
			openKeyboard();
		}
	}

	private void openKeyboard() {
		searchEditText.requestFocus();
		AndroidUtils.softKeyboardDelayed(requireActivity(), searchEditText);
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
			searchPhrase.syncWordsWithResults();
			searchPhrase.setAcceptPrivate(filter.isShowPrivateNeeded());
			String txt = filter.getName() + (!Algorithms.isEmpty(nameFilter) && filter.isStandardFilter() ? " " + nameFilter : " ");
			searchQuery = txt;
			searchEditText.setText(txt);
			searchEditText.setSelection(txt.length());
			updateToolbarButton();
			runCoreSearch(txt, false, false);
		}
	}

	public void clearLastWord() {
		if (!searchEditText.getText().toString().isEmpty()) {
			String newText = searchUICore.getPhrase().getTextWithoutLastWord();
			searchEditText.setText(newText);
			searchEditText.setSelection(newText.length());
		}
	}

	private void onSearchFinished(SearchPhrase phrase) {
		if (!PluginsHelper.onSearchFinished(this, phrase, isResultEmpty())) {
			addMoreButton(searchUICore.isSearchMoreAvailable(phrase));
		}
	}

	private void addMoreButton(boolean searchMoreAvailable) {
		if (!paused && !cancelPrev && mainSearchFragment != null && !isTextEmpty()) {
			QuickSearchMoreListItem moreListItem = new QuickSearchMoreListItem(app, null, new SearchMoreItemOnClickListener() {
				@Override
				public void onPrimaryButtonClick() {
					increaseSearchRadius();
				}

				@Override
				public void onSecondaryButtonClick() {
					if (!settings.isInternetConnectionAvailable()) {
						app.showToastMessage(R.string.internet_not_available);
						return;
					}
					startOnlineSearch();
					mainSearchFragment.getAdapter().clear();
					updateTabBarVisibility(false);
					runCoreSearch(searchQuery, false, true);
				}
			});
			moreListItem.setInterruptedSearch(interruptedSearch);
			moreListItem.setEmptySearch(isResultEmpty());
			moreListItem.setSearchMoreAvailable(searchMoreAvailable);
			moreListItem.setSecondaryButtonVisible(searchUICore.isOnlineSearch());
			mainSearchFragment.addListItem(moreListItem);
			updateSendEmptySearchBottomBar(isResultEmpty() && !interruptedSearch);
		}
	}

	public void increaseSearchRadius() {
		if (!interruptedSearch) {
			SearchSettings settings = searchUICore.getSearchSettings();
			searchUICore.updateSettings(settings.setRadiusLevel(settings.getRadiusLevel() + 1));
		}
		runCoreSearch(searchQuery, false, true);
	}

	public void addSearchListItem(QuickSearchListItem item) {
		if (mainSearchFragment != null) {
			mainSearchFragment.addListItem(item);
		}
	}

	private void updateSearchResult(SearchResultCollection res, boolean append) {

		if (!paused && mainSearchFragment != null) {
			List<QuickSearchListItem> rows = new ArrayList<>();
			if (res != null && !res.getCurrentSearchResults().isEmpty()) {
				for (SearchResult sr : res.getCurrentSearchResults()) {
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
	                                   @NonNull QuickSearchType searchType,
	                                   @NonNull QuickSearchTab showSearchTab,
	                                   @Nullable LatLon latLon) {
		FragmentManager fragmentManager = mapActivity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			mapActivity.getApp().logEvent("search_open");

			Bundle bundle = new Bundle();
			if (object != null) {
				bundle.putBoolean(QUICK_SEARCH_RUN_SEARCH_FIRST_TIME_KEY, true);
				String objectLocalizedName = searchQuery;

				if (object instanceof PoiCategory c) {
					objectLocalizedName = c.getTranslation();

					SearchUICore searchUICore = mapActivity.getApp().getSearchUICore().getCore();
					SearchPhrase phrase = searchUICore.resetPhrase(objectLocalizedName + " ");
					SearchResult sr = new SearchResult(phrase);
					sr.localeName = objectLocalizedName;
					sr.object = c;
					sr.priority = SEARCH_AMENITY_TYPE_PRIORITY;
					sr.priorityDistance = 0;
					sr.objectType = ObjectType.POI_TYPE;
					searchUICore.selectSearchResult(sr);

					bundle.putBoolean(QUICK_SEARCH_PHRASE_DEFINED_KEY, true);

				} else if (object instanceof PoiUIFilter filter) {
					objectLocalizedName = filter.getName();
					SearchUICore searchUICore = mapActivity.getApp().getSearchUICore().getCore();
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
			fragment.show(fragmentManager, TAG);
			return true;
		}
		return false;
	}

	@Override
	public void onPreferenceChanged(@NonNull String prefId) {
		reloadHistory();
	}

	@Override
	public void updateCompassValue(float value) {
		// 99 in next line used to one-time initialize arrows (with reference vs. fixed-north direction)
		// on non-compass devices
		float lastHeading = heading != null ? heading : 99;
		heading = value;
		if (Math.abs(MapUtils.degreesDiff(lastHeading, heading)) > 5) {
			Location location = this.location;
			app.runInUIThread(() -> updateLocationUI(location, value));
		} else {
			heading = lastHeading;
		}
	}

	@Override
	public void updateLocation(Location location) {
		Float heading = this.heading;
		app.runInUIThread(() -> updateLocationUI(location, heading));
	}

	private void updateLocationUI(Location location, Float heading) {
		if (shouldUpdateContent(location, this.location)) {
			this.location = location;
			updateContent(heading);
		}
	}

	private boolean shouldUpdateContent(Location a, Location b) {
		return b == null || a == null
				|| !MapUtils.areLatLonEqual(a.getLatitude(), a.getLongitude(), b.getLatitude(), b.getLongitude())
				|| Math.abs(MapUtils.degreesDiff(a.getBearing(), b.getBearing())) > MIN_COMPASS_DEGREES_TO_UPDATE_CONTENT;
	}

	private void updateContent(Float heading) {
		if (!paused && !cancelPrev) {
			if (mainSearchFragment != null && searchView.getVisibility() == View.VISIBLE) {
				mainSearchFragment.updateLocation(heading);
			} else if (historySearchFragment != null && viewPager.getCurrentItem() == 0) {
				historySearchFragment.updateLocation(heading);
			} else if (categoriesSearchFragment != null && viewPager.getCurrentItem() == 1) {
				categoriesSearchFragment.updateLocation(heading);
			} else if (addressSearchFragment != null && viewPager.getCurrentItem() == 2) {
				addressSearchFragment.updateLocation(heading);
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
		if (!selectedItems.isEmpty()) {
			String text = selectedItems.size() + " " + getString(R.string.shared_string_selected_lowercase);
			titleEdit.setText(text);
		} else {
			titleEdit.setText("");
		}
	}

	private void shareHistory(List<HistoryEntry> historyEntries) {
		if (!historyEntries.isEmpty()) {
			OnShareHistoryListener listener = new OnShareHistoryListener() {
				@Override
				public void onShareHistoryStarted() {
					showProgressBar();
				}

				@Override
				public void onShareHistoryFinished() {
					hideProgressBar();
				}
			};
			ShareHistoryAsyncTask exportTask = new ShareHistoryAsyncTask(app, historyEntries, listener);
			OsmAndTaskManager.executeTask(exportTask);
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
			app.getSearchHistoryHelper().addNewItemToHistory(nFilter, HistorySource.SEARCH);
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
				bottomMargin = getDimensionPixelSize(R.dimen.fab_margin_bottom_big);
			} else {
				bottomMargin = getDimensionPixelSize(R.dimen.fab_margin_right);
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

	@Override
	public void onUpdatedIndexesList() {
		if (!searching) {
			hideProgressBar();
		}
		PluginsHelper.onNewDownloadIndexes(this);
		updateContent(heading);
		List<Fragment> childFragment = getChildFragmentManager().getFragments();
		for(Fragment fragment : childFragment) {
			if(fragment instanceof DownloadEvents downloadEventsFragment) {
				downloadEventsFragment.onUpdatedIndexesList();
			}
		}
	}

	@Override
	public void downloadInProgress() {
		updateContent(heading);
		List<Fragment> childFragment = getChildFragmentManager().getFragments();
		for(Fragment fragment : childFragment) {
			if(fragment instanceof DownloadEvents downloadEventsFragment) {
				downloadEventsFragment.downloadInProgress();
			}
		}
	}

	@Override
	public void downloadHasFinished() {
		updateContent(heading);
		List<Fragment> childFragment = getChildFragmentManager().getFragments();
		for(Fragment fragment : childFragment) {
			if(fragment instanceof DownloadEvents downloadEventsFragment) {
				downloadEventsFragment.downloadHasFinished();
			}
		}
	}

	public void reloadIndexFiles() {
		if (settings.isInternetConnectionAvailable()) {
			app.getDownloadThread().runReloadIndexFiles();
			showProgressBar();
		}
	}

	public interface SearchResultListener {
		void searchStarted(SearchPhrase phrase);

		void publish(SearchResultCollection res, boolean append);

		// return true if search done, false if next search will be ran immediately
		boolean searchFinished(SearchPhrase phrase);
	}

	public interface SearchVisibilityListener {
		void onVisibilityChanged(boolean visible);
	}

	private PoiUIFilter initPoiUIFilter(TopIndexFilter topIndexFilter,
			ProcessTopIndex processAfter) {
		PoiUIFilter poiUIFilter = app.getPoiFilters().getFilterById(topIndexFilter.getFilterId());
		if (poiUIFilter != null) {
			// use saved filter
			processTopIndexAfterLoad = ProcessTopIndex.NO;
			return poiUIFilter;
		} else if (searchHelper != null && searchHelper.getResultCollection() != null) {
			// collect poi categories and subtypes from searching result
			List<SearchResult> searchResults = searchHelper.getResultCollection().getCurrentSearchResults();
			Map<PoiCategory, LinkedHashSet<String>> acceptedTypes = new HashMap<>();
			for (SearchResult res : searchResults) {
				if (res.object instanceof Amenity) {
					Amenity am = (Amenity) res.object;
					LinkedHashSet<String> subtypes = acceptedTypes.computeIfAbsent(am.getType(), s -> new LinkedHashSet<>());
					subtypes.add(am.getSubType());
				}
			}
			poiUIFilter = app.getPoiFilters().getFilter(topIndexFilter, acceptedTypes);
			processTopIndexAfterLoad = ProcessTopIndex.NO;
			return poiUIFilter;
		}
		processTopIndexAfterLoad = processAfter;
		return null;
	}

	public void showResult(@NonNull PoiUIFilter filter) {
		buttonToolbarText.setText(R.string.shared_string_show_on_map);
		mainSearchFragment.getAdapter().clear();
		updateSearchResult(createSearchResultCollection(app, filter.getCurrentSearchResult()), true);
		((QuickSearchListAdapter) mainSearchFragment.getAdapter()).setPoiUIFilter(filter);
		updateTabBarVisibility(false);
		toolbarEdit.setVisibility(View.GONE);
		searchEditText.setHint(R.string.popular_places);
		searchEditText.setEnabled(false);
		toolbar.setVisibility(View.VISIBLE);
	}

	@NonNull
	public static SearchResultCollection createSearchResultCollection(
			@NonNull OsmandApplication app, @NonNull List<Amenity> amenities) {
		SearchUICore core = app.getSearchUICore().getCore();
		String locale = LocaleHelper.getPreferredPlacesLanguage(app);
		boolean transliterate = app.getSettings().MAP_TRANSLITERATE_NAMES.get();
		SearchSettings settings = core.getSearchSettings().setLang(locale, transliterate);

		SearchPhrase phrase = SearchPhrase.emptyPhrase(settings);
		SearchResultCollection collection = new SearchResultCollection(phrase);

		List<SearchResult> results = new ArrayList<>();
		for (Amenity amenity : amenities) {
			SearchResult result = SearchCoreFactory.createSearchResult(amenity, phrase, core.getPoiTypes());
			results.add(result);
		}
		collection.addSearchResults(results, false, false);
		return collection;
	}
}
