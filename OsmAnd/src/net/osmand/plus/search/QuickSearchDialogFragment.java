package net.osmand.plus.search;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;

import net.osmand.AndroidUtils;
import net.osmand.Location;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.Amenity;
import net.osmand.data.Building;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmAndLocationProvider.OsmAndCompassListener;
import net.osmand.plus.OsmAndLocationProvider.OsmAndLocationListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.OsmAndListFragment;
import net.osmand.plus.dashboard.DashLocationFragment;
import net.osmand.plus.helpers.SearchHistoryHelper;
import net.osmand.plus.helpers.SearchHistoryHelper.HistoryEntry;
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
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class QuickSearchDialogFragment extends DialogFragment implements OsmAndCompassListener, OsmAndLocationListener {

	public static final String TAG = "QuickSearchDialogFragment";
	private static final String QUICK_SEARCH_QUERY_KEY = "quick_search_query_key";
	private ViewPager viewPager;
	private SearchFragmentPagerAdapter pagerAdapter;
	private TabLayout tabLayout;
	private View tabToolbarView;
	private View tabsView;
	private View searchView;
	private SearchMainListFragment mainSearchFragment;
	private SearchHistoryListFragment historySearchFragment;
	private SearchCategoriesListFragment categoriesSearchFragment;

	private EditText searchEditText;
	private ProgressBar progressBar;
	private ImageButton clearButton;

	private SearchUICore searchUICore;
	private String searchQuery = "";
	private SearchResultCollection resultCollection;

	private net.osmand.Location location = null;
	private Float heading = null;

	public static final int SEARCH_FAVORITE_API_PRIORITY = 2;
	public static final int SEARCH_FAVORITE_OBJECT_PRIORITY = 10;
	public static final int SEARCH_HISTORY_API_PRIORITY = 3;
	public static final int SEARCH_HISTORY_OBJECT_PRIORITY = 10;


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

		Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
		toolbar.setNavigationIcon(app.getIconsCache().getThemedIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha));
		toolbar.setNavigationContentDescription(R.string.access_shared_string_navigate_up);
		toolbar.setNavigationOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

		setupSearch(mapActivity);

		viewPager = (ViewPager) view.findViewById(R.id.pager);
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
				}
			}
		});

		addMainSearchFragment();

		searchEditText.requestFocus();
		AndroidUtils.softKeyboardDelayed(searchEditText);

		return view;
	}

	public void addMainSearchFragment() {
		FragmentManager childFragMan = getChildFragmentManager();
		FragmentTransaction childFragTrans = childFragMan.beginTransaction();
		mainSearchFragment = new SearchMainListFragment();
		childFragTrans.add(R.id.search_view, mainSearchFragment);
		childFragTrans.addToBackStack("SearchMainListFragment");
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

		/*
		List<BinaryMapIndexReader> files = new ArrayList<>();
		File file = new File(Environment.getExternalStorageDirectory() + "/osmand");
		if (file.exists() && file.listFiles() != null) {
			for (File obf : file.listFiles()) {
				if (!obf.isDirectory() && obf.getName().endsWith(".obf")) {
					try {
						BinaryMapIndexReader bmir = new BinaryMapIndexReader(new RandomAccessFile(obf, "r"), obf);
						files.add(bmir);
					} catch (Exception e1) {
						e1.printStackTrace();
					}
				}
			}
		}

		searchUICore = new SearchUICore(app.getPoiTypes(), locale, files.toArray(new BinaryMapIndexReader[files.size()]));
		*/

		LatLon centerLatLon;
		if (location != null) {
			centerLatLon = new LatLon(location.getLatitude(), location.getLongitude());
		} else {
			centerLatLon = mapActivity.getMapView().getCurrentRotatedTileBox().getCenterLatLon();
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

		// Setup favorites search api
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
					if (phrase.getLastWord().length() <= 1 && phrase.isNoSelectedType()) {
						resultMatcher.publish(sr);
					} else if (phrase.getNameStringMatcher().matches(sr.localeName)) {
						resultMatcher.publish(sr);
					}
				}
				return true;
			}

			@Override
			public int getSearchPriority(SearchPhrase p) {
				if(!p.isNoSelectedType() || p.getLastWord().isEmpty()) {
					return -1;
				}
				return SEARCH_FAVORITE_API_PRIORITY;
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
	}

	@Override
	public void onResume() {
		super.onResume();
		OsmandApplication app = getMyApplication();
		app.getLocationProvider().addCompassListener(this);
		app.getLocationProvider().addLocationListener(this);
		location = app.getLocationProvider().getLastKnownLocation();
		updateLocation(location);
	}

	@Override
	public void onPause() {
		super.onPause();
		OsmandApplication app = getMyApplication();
		getChildFragmentManager().popBackStack();
		app.getLocationProvider().removeLocationListener(this);
		app.getLocationProvider().removeCompassListener(this);
		mainSearchFragment = null;
		historySearchFragment = null;
		categoriesSearchFragment = null;
	}

	private void showProgressBar() {
		updateClearButtonVisibility(false);
		progressBar.setVisibility(View.VISIBLE);
	}

	private void hideProgressBar() {
		updateClearButtonVisibility(true);
		progressBar.setVisibility(View.GONE);
	}

	private void updateClearButtonVisibility(boolean show) {
		if (show) {
			clearButton.setVisibility(searchEditText.length() > 0 ? View.VISIBLE : View.GONE);
		} else {
			clearButton.setVisibility(View.GONE);
		}
	}

	private void updateTabbarVisibility(boolean show) {
		if (show && tabsView.getVisibility() == View.GONE) {
			tabToolbarView.setVisibility(View.VISIBLE);
			tabsView.setVisibility(View.VISIBLE);
			searchView.setVisibility(View.GONE);
		} else if (!show && tabsView.getVisibility() == View.VISIBLE) {
			tabToolbarView.setVisibility(View.GONE);
			tabsView.setVisibility(View.GONE);
			searchView.setVisibility(View.VISIBLE);
		}
	}

	public void onSearchListFragmentResume(SearchListFragment searchListFragment) {
		SearchPhrase sp;
		switch (searchListFragment.getType()) {
			case HISTORY:
				historySearchFragment = (SearchHistoryListFragment) searchListFragment;
				SearchHistoryAPI historyAPI =
						new SearchHistoryAPI(getMyApplication());
				final List<SearchResult> history = new ArrayList<>();
				sp = new SearchPhrase(null).generateNewPhrase("", searchUICore.getSearchSettings());
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
				if (history.size() > 0) {
					searchUICore.sortSearchResults(sp, history);
					List<QuickSearchListItem> rows = new ArrayList<>();
					OsmandApplication app = getMyApplication();
					for (SearchResult sr : history) {
						rows.add(new QuickSearchListItem(app, sr));
					}
					searchListFragment.updateListAdapter(rows, false);
				}
				break;

			case CATEGORIES:
				categoriesSearchFragment = (SearchCategoriesListFragment) searchListFragment;

				SearchAmenityTypesAPI amenityTypesAPI =
						new SearchAmenityTypesAPI(getMyApplication().getPoiTypes());
				final List<SearchResult> amenityTypes = new ArrayList<>();
				sp = new SearchPhrase(null).generateNewPhrase("", searchUICore.getSearchSettings());
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
					searchListFragment.updateListAdapter(rows, false);
				}
				break;

			case MAIN:
				if (!Algorithms.isEmpty(searchQuery)) {
					String txt = searchQuery;
					searchQuery = "";
					searchEditText.setText(txt);
					searchEditText.setSelection(txt.length());
				}
				break;
		}
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
					default:
						results.add(object);
				}

				return false;
			}

			@Override
			public boolean isCancelled() {
				return false;
			}
		});
	}

	private void completeQueryWithObject(SearchResult sr, boolean updateEditText) {
		searchUICore.selectSearchResult(sr);
		String txt = searchUICore.getPhrase().getText(true);
		if (updateEditText) {
			searchQuery = txt;
			searchEditText.setText(txt);
			searchEditText.setSelection(txt.length());
		}
		runCoreSearch(txt);
	}

	private void addMoreButton() {
		OsmandApplication app = getMyApplication();
		if ((!searchUICore.getPhrase().getLastWord().isEmpty() || searchUICore.getPhrase().isLastWord(ObjectType.POI_TYPE))
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

			if (mainSearchFragment != null) {
				mainSearchFragment.addListItem(moreListItem);
			}
		}
	}

	private void updateSearchResult(SearchResultCollection res, boolean appended) {

		OsmandApplication app = getMyApplication();

		List<QuickSearchListItem> rows = new ArrayList<>();
		if (res.getCurrentSearchResults().size() > 0) {
			for (final SearchResult sr : res.getCurrentSearchResults()) {
				rows.add(new QuickSearchListItem(app, sr));
			}
		}
		if (mainSearchFragment != null) {
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

	public class SearchFragmentPagerAdapter extends FragmentPagerAdapter {
		private final String[] fragments = new String[]{SearchHistoryListFragment.class.getName(),
				SearchCategoriesListFragment.class.getName()};
		private final int[] titleIds = new int[]{SearchHistoryListFragment.TITLE,
				SearchCategoriesListFragment.TITLE};
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

	public static class SearchHistoryListFragment extends SearchListFragment {
		public static final int TITLE = R.string.shared_string_history;

		@Override
		public SearchListFragmentType getType() {
			return SearchListFragmentType.HISTORY;
		}
	}

	public static class SearchCategoriesListFragment extends SearchListFragment {
		public static final int TITLE = R.string.search_categories;

		@Override
		public SearchListFragmentType getType() {
			return SearchListFragmentType.CATEGORIES;
		}
	}

	public static class SearchMainListFragment extends SearchListFragment {

		@Override
		public SearchListFragmentType getType() {
			return SearchListFragmentType.MAIN;
		}
	}

	public static abstract class SearchListFragment extends OsmAndListFragment {

		private QuickSearchDialogFragment dialogFragment;
		private QuickSearchListAdapter listAdapter;
		private boolean touching;

		enum SearchListFragmentType {
			HISTORY,
			CATEGORIES,
			MAIN
		}

		public abstract SearchListFragmentType getType();

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			return inflater.inflate(R.layout.search_dialog_list_layout, container, false);
		}

		@Override
		public void onViewCreated(View view, Bundle savedInstanceState) {
			super.onViewCreated(view, savedInstanceState);
			ListView listView = getListView();
			if (listView != null) {
				View header = getLayoutInflater(savedInstanceState).inflate(R.layout.list_shadow_header, null);
				View footer = getLayoutInflater(savedInstanceState).inflate(R.layout.list_shadow_footer, null);
				listView.addHeaderView(header, null, false);
				listView.addFooterView(footer, null, false);
			}
		}

		@Override
		public void onListItemClick(ListView l, View view, int position, long id) {
			QuickSearchListItem item = listAdapter.getItem(position - l.getHeaderViewsCount());
			if (item instanceof QuickSearchMoreListItem) {
				((QuickSearchMoreListItem) item).getOnClickListener().onClick(view);
			} else {
				SearchResult sr = item.getSearchResult();

				boolean updateEditText = true;
				if (sr.objectType == ObjectType.POI
						|| sr.objectType == ObjectType.LOCATION
						|| sr.objectType == ObjectType.HOUSE
						|| sr.objectType == ObjectType.FAVORITE
						|| sr.objectType == ObjectType.RECENT_OBJ
						|| sr.objectType == ObjectType.WPT
						|| sr.objectType == ObjectType.STREET_INTERSECTION) {

					updateEditText = false;
					dialogFragment.dismiss();
					showOnMap(sr);
				} else {
					dialogFragment.completeQueryWithObject(item.getSearchResult(), updateEditText);
				}
			}
		}

		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
			super.onActivityCreated(savedInstanceState);
			dialogFragment = (QuickSearchDialogFragment) getParentFragment();
			listAdapter = new QuickSearchListAdapter(getMyApplication(), getActivity());
			setListAdapter(listAdapter);
			ListView listView = getListView();
			listView.setBackgroundColor(getResources().getColor(
							getMyApplication().getSettings().isLightContent() ? R.color.ctx_menu_info_view_bg_light
									: R.color.ctx_menu_info_view_bg_dark));
			listView.setOnTouchListener(new View.OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					switch (event.getAction()) {
						case MotionEvent.ACTION_DOWN:
						case MotionEvent.ACTION_POINTER_DOWN:
							touching = true;
							break;
						case MotionEvent.ACTION_UP:
						case MotionEvent.ACTION_POINTER_UP:
						case MotionEvent.ACTION_CANCEL:
							touching = false;
							break;
					}
					return false;
				}
			});
		}

		public ArrayAdapter<?> getAdapter() {
			return listAdapter;
		}

		@Override
		public void onResume() {
			super.onResume();
			int screenOrientation = DashLocationFragment.getScreenOrientation(getActivity());
			listAdapter.setScreenOrientation(screenOrientation);
			dialogFragment.onSearchListFragmentResume(this);
		}

		private void showOnMap(SearchResult searchResult) {
			if (searchResult.location != null) {
				OsmandApplication app = getMyApplication();
				PointDescription pointDescription = null;
				Object object = searchResult.object;
				switch (searchResult.objectType) {
					case POI:
						String poiSimpleFormat = OsmAndFormatter.getPoiStringWithoutType(
								(Amenity) object, searchResult.requiredSearchPhrase.getSettings().getLang());
						pointDescription = new PointDescription(PointDescription.POINT_TYPE_POI, poiSimpleFormat);
						break;
					case RECENT_OBJ:
						HistoryEntry entry = (HistoryEntry) object;
						pointDescription = entry.getName();
						break;
					case FAVORITE:
						FavouritePoint fav = (FavouritePoint) object;
						pointDescription = fav.getPointDescription();
						break;
					case HOUSE:
						pointDescription = new PointDescription(PointDescription.POINT_TYPE_ADDRESS,
								QuickSearchListItem.getName(app, searchResult) + ", " + QuickSearchListItem.getTypeName(app, searchResult));
						break;
					case LOCATION:
						LatLon latLon = (LatLon) object;
						pointDescription = new PointDescription(latLon.getLatitude(), latLon.getLongitude());
						break;
					case STREET_INTERSECTION:
						pointDescription = new PointDescription(PointDescription.POINT_TYPE_ADDRESS,
								QuickSearchListItem.getName(app, searchResult));
						break;
					case WPT:
						WptPt wpt = (WptPt) object;
						pointDescription = wpt.getPointDescription(getMyApplication());
						break;
				}
				getMyApplication().getSettings().setMapLocationToShow(
						searchResult.location.getLatitude(), searchResult.location.getLongitude(),
						searchResult.preferredZoom, pointDescription, true, object);

				MapActivity.launchMapActivityMoveToTop(getActivity());
			}
		}

		public MapActivity getMapActivity() {
			return (MapActivity)getActivity();
		}

		public void updateLocation(LatLon latLon, Float heading) {
			if (listAdapter != null && !touching) {
				listAdapter.setLocation(latLon);
				listAdapter.setHeading(heading);
				listAdapter.notifyDataSetChanged();
			}
		}

		public void updateListAdapter(List<QuickSearchListItem> listItems, boolean appended) {
			if (listAdapter != null) {
				listAdapter.setListItems(listItems);
				if (listAdapter.getCount() > 0 && !appended) {
					getListView().setSelection(0);
				}
			}
		}

		public void addListItem(QuickSearchListItem listItem) {
			if (listItem != null) {
				listAdapter.addListItem(listItem);
			}
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
				if (phrase.getLastWord().length() <= 1 && phrase.isNoSelectedType()) {
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
}
