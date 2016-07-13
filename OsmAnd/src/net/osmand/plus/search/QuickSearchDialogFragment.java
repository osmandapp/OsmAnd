package net.osmand.plus.search;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityManagerCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;

import net.osmand.AndroidUtils;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.resources.RegionAddressRepository;
import net.osmand.search.SearchUICore;
import net.osmand.search.SearchUICore.SearchResultCollection;
import net.osmand.search.core.ObjectType;
import net.osmand.search.core.SearchResult;
import net.osmand.search.core.SearchSettings;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class QuickSearchDialogFragment extends DialogFragment {

	public static final String TAG = "QuickSearchDialogFragment";
	private static final String QUICK_SEARCH_QUERY_KEY = "quick_search_query_key";
	private ListView listView;
	private SearchListAdapter listAdapter;
	private EditText searchEditText;
	private ProgressBar progressBar;
	private ImageButton clearButton;

	private SearchUICore searchUICore;
	private String searchQuery = "";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		boolean isLightTheme = getMyApplication().getSettings().OSMAND_THEME.get() == OsmandSettings.OSMAND_LIGHT_THEME;
		int themeId = isLightTheme ? R.style.OsmandLightTheme : R.style.OsmandDarkTheme;
		setStyle(STYLE_NO_FRAME, themeId);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		final OsmandApplication app = getMyApplication();
		final MapActivity mapActivity = getMapActivity();
		final View view = inflater.inflate(R.layout.search_dialog_fragment, container, false);

		if (savedInstanceState != null) {
			searchQuery = savedInstanceState.getString(QUICK_SEARCH_QUERY_KEY);
		}
		if (searchQuery == null) {
			searchQuery = getArguments().getString(QUICK_SEARCH_QUERY_KEY);
		}
		if (searchQuery == null)
			searchQuery = "";

		Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
		toolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
		toolbar.setNavigationContentDescription(R.string.access_shared_string_navigate_up);
		toolbar.setNavigationOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

		// Setup search
		String locale = app.getSettings().MAP_PREFERRED_LOCALE.get();
		Collection<RegionAddressRepository> regionAddressRepositories = app.getResourceManager().getAddressRepositories();
		BinaryMapIndexReader[] binaryMapIndexReaderArray = new BinaryMapIndexReader[regionAddressRepositories.size()];
		int i = 0;
		for (RegionAddressRepository rep : regionAddressRepositories) {
			binaryMapIndexReaderArray[i++] = rep.getFile();
		}
		searchUICore = new SearchUICore(app.getPoiTypes(), locale, binaryMapIndexReaderArray);

		LatLon centerLatLon = mapActivity.getMapView().getCurrentRotatedTileBox().getCenterLatLon();
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
						updateSearchResult(searchUICore.getCurrentSearchResult(), true);
					}
				});
			}
		});

		listView = (ListView) view.findViewById(android.R.id.list);
		listAdapter = new SearchListAdapter(getMyApplication());
		listView.setAdapter(listAdapter);
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				SearchListItem item = listAdapter.getItem(position);
				if (item instanceof SearchMoreListItem) {
					((SearchMoreListItem) item).getOnClickListener().onClick(view);
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
						dismiss();
						if (sr.location != null) {
							showOnMap(sr);
						}
					}
					completeQueryWithObject(item.getSearchResult(), updateEditText);
				}
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
				if (!searchQuery.equalsIgnoreCase(newQueryText)) {
					searchQuery = newQueryText;
					runSearch();
				}
			}
		});

		progressBar = (ProgressBar) view.findViewById(R.id.searchProgressBar);
		clearButton = (ImageButton) view.findViewById(R.id.clearButton);
		clearButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (searchEditText.getText().length() == 0) {
					dismiss();
				} else {
					searchEditText.setText("");
				}
			}
		});

		searchEditText.requestFocus();
		AndroidUtils.softKeyboardDelayed(searchEditText);
		runSearch();

		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setShowsDialog(true);
		final boolean isLightContent = getMyApplication().getSettings().isLightContent();
		final int colorId = isLightContent ? R.color.bg_color_light : R.color.bg_color_dark;
		listView.setBackgroundColor(ContextCompat.getColor(getActivity(), colorId));
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putString(QUICK_SEARCH_QUERY_KEY, searchQuery);
	}

	@Override
	public void onResume() {
		super.onResume();
		if (!Algorithms.isEmpty(searchQuery)) {
			searchEditText.setText(searchQuery);
			searchEditText.setSelection(searchQuery.length());
		}
	}

	private void showProgressBar() {
		clearButton.setVisibility(View.GONE);
		progressBar.setVisibility(View.VISIBLE);
	}

	private void hideProgressBar() {
		clearButton.setVisibility(View.VISIBLE);
		progressBar.setVisibility(View.GONE);
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
		return searchUICore.search(text, null);
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

	private void updateSearchResult(SearchResultCollection res, boolean addMore) {

		OsmandApplication app = getMyApplication();

		List<SearchListItem> rows = new ArrayList<>();
		if (res.getCurrentSearchResults().size() > 0) {
			if (addMore) {
				SearchMoreListItem moreListItem = new SearchMoreListItem(app, "Results " + res.getCurrentSearchResults().size() + ", radius " + res.getPhrase().getRadiusLevel() +
						" (show more...)", new OnClickListener() {
					@Override
					public void onClick(View v) {
						SearchSettings settings = searchUICore.getPhrase().getSettings();
						searchUICore.updateSettings(settings.setRadiusLevel(settings.getRadiusLevel() + 1));
						runCoreSearch(searchQuery);
						updateSearchResult(new SearchResultCollection(), false);
					}
				});
				rows.add(moreListItem);
			}
			for (final SearchResult sr : res.getCurrentSearchResults()) {
				SearchListItem listItem = new SearchListItem(app, sr);
				if (sr.location != null) {
					LatLon location = res.getPhrase().getLastTokenLocation();
					listItem.setDistance(MapUtils.getDistance(location, sr.location));
				}
				rows.add(listItem);
			}
		}
		updateListAdapter(rows);
	}

	private void updateListAdapter(List<SearchListItem> listItems) {
		listAdapter.setListItems(listItems);
		if (listAdapter.getCount() > 0) {
			listView.setSelection(0);
		}
	}

	private void showOnMap(SearchResult searchResult) {
		if (searchResult.location != null) {
			PointDescription pointDescription = null;
			Object object = null;
			switch (searchResult.objectType) {
				case POI:
					object = searchResult.object;
					pointDescription = getMapActivity().getMapLayers().getPoiMapLayer().getObjectName(object);
					break;
			}
			getMyApplication().getSettings().setMapLocationToShow(
					searchResult.location.getLatitude(), searchResult.location.getLongitude(),
					searchResult.preferredZoom, pointDescription, true, object);

			MapActivity.launchMapActivityMoveToTop(getActivity());
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

}
