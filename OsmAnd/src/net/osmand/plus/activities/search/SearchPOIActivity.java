/**
 *
 */
package net.osmand.plus.activities.search;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;

import net.osmand.Location;
import net.osmand.ResultMatcher;
import net.osmand.access.AccessibilityAssistant;
import net.osmand.access.NavigationInfo;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.osm.PoiType;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmAndLocationProvider.OsmAndCompassListener;
import net.osmand.plus.OsmAndLocationProvider.OsmAndLocationListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.R.color;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.UiUtilities.UpdateLocationViewCache;
import net.osmand.plus.activities.EditPOIFilterActivity;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.OsmandListActivity;
import net.osmand.plus.poi.NominatimPoiFilter;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.poi.PoiUIFilter.AmenityNameFilter;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.views.DirectionDrawable;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;
import net.osmand.util.OpeningHoursParser;
import net.osmand.util.OpeningHoursParser.OpeningHours;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import gnu.trove.set.hash.TLongHashSet;

/**
 * Search poi activity
 */
public class SearchPOIActivity extends OsmandListActivity implements OsmAndCompassListener, OsmAndLocationListener {

	public static final String AMENITY_FILTER = "net.osmand.amenity_filter"; //$NON-NLS-1$
	public static final String SEARCH_NEARBY = SearchActivity.SEARCH_NEARBY; //$NON-NLS-1$
	public static final String SEARCH_LAT = SearchActivity.SEARCH_LAT; //$NON-NLS-1$
	public static final String SEARCH_LON = SearchActivity.SEARCH_LON; //$NON-NLS-1$
	private static final float MIN_DISTANCE_TO_RESEARCH = 100;
	private static final float MIN_DISTANCE_TO_REFRESH = 5;
	private static final int SEARCH_MORE = 0;
	private static final int SHOW_ON_MAP = 1;
	private static final int FILTER = 2;

	private static final int EDIT_FILTER = 4;
	private static final int DELETE_FILTER = 5;
	private static final int SAVE_FILTER = 6;


	private PoiUIFilter filter;
	private AmenityAdapter amenityAdapter;
	private EditText searchFilter;
	private View searchFilterLayout;
	private NavigationInfo navigationInfo;

	private boolean searchNearBy = false;
	private net.osmand.Location location = null;
	private net.osmand.Location lastSearchedLocation = null;
	private Float heading = null;

	private SearchAmenityTask currentSearchTask = null;
	private AccessibilityAssistant accessibilityAssistant;

	private OsmandApplication app;
	private MenuItem showFilterItem;
	private MenuItem showOnMapItem;
	private MenuItem searchPOILevel;
	private static int RESULT_REQUEST_CODE = 54;

	private CharSequence tChange;

	@Override
	public boolean onCreateOptionsMenu(Menu omenu) {
		Menu menu = getClearToolbar(true).getMenu();
		searchPOILevel = menu.add(0, SEARCH_MORE, 0, R.string.search_POI_level_btn);
		searchPOILevel.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		searchPOILevel.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				return search();
			}

		});
		showFilterItem = menu.add(0, FILTER, 0, R.string.search_poi_filter);
		showFilterItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		showFilterItem = showFilterItem.setIcon(getMyApplication().getUIUtilities().getIcon(
				R.drawable.ic_action_filter_dark));
		showFilterItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				if (searchFilterLayout.getVisibility() == View.GONE) {
					searchFilterLayout.setVisibility(View.VISIBLE);
					searchFilter.requestFocus();
					InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.showSoftInput(searchFilter, InputMethodManager.SHOW_IMPLICIT);
				} else {
					if (filter != null) {
						searchFilter.setText(filter.getSavedFilterByName() == null ? "" :
								filter.getSavedFilterByName());
					}
					searchFilterLayout.setVisibility(View.GONE);
				}
				return true;
			}
		});

		showOnMapItem = menu.add(0, SHOW_ON_MAP, 0, R.string.shared_string_show_on_map);
		showOnMapItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		showOnMapItem = showOnMapItem.setIcon(getMyApplication().getUIUtilities().getIcon(
				R.drawable.ic_show_on_map));
		showOnMapItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				OsmandSettings settings = app.getSettings();
				filter.setFilterByName(searchFilter.getText().toString().trim());
				app.getPoiFilters().addSelectedPoiFilter(filter);
				if (location != null) {
					settings.setMapLocationToShow(location.getLatitude(), location.getLongitude(), 15);
				}
				MapActivity.launchMapActivityMoveToTop(SearchPOIActivity.this);
				return true;

			}
		});
		showOnMapItem.setEnabled(!isNameSearch() || amenityAdapter.getCount() > 0);
		if (filter != null && !isNameSearch()) {
			createMenuItem(omenu, SAVE_FILTER, R.string.edit_filter_save_as_menu_item, R.drawable.ic_action_favorite,
					MenuItem.SHOW_AS_ACTION_IF_ROOM);
			if (!filter.isStandardFilter()) {
				createMenuItem(omenu, DELETE_FILTER, R.string.shared_string_delete, R.drawable.ic_action_delete_dark,
						MenuItem.SHOW_AS_ACTION_IF_ROOM);
			}
		}
		updateButtonState(false);
		return true;
	}

	@Override
	protected void onDestroy() {
		// Issue 2657
		super.onDestroy();
		if (!(currentSearchTask == null || currentSearchTask.getStatus() == Status.FINISHED)) {
			currentSearchTask.cancel(true);
		}
	}

	public Toolbar getClearToolbar(boolean visible) {
		final Toolbar tb = (Toolbar) findViewById(R.id.poiSplitbar);
		tb.setTitle(null);
		tb.getMenu().clear();
		tb.setVisibility(visible ? View.VISIBLE : View.GONE);
		return tb;
	}

	private boolean search() {
		String query = searchFilter.getText().toString().trim();
		if (query.length() < 2 && isNameSearch()) {
			Toast.makeText(SearchPOIActivity.this, R.string.poi_namefinder_query_empty, Toast.LENGTH_LONG)
					.show();
			return true;
		}
		if ((isNameSearch() && !Algorithms.objectEquals(filter.getFilterByName(), query))) {
			filter.clearPreviousZoom();
			filter.setFilterByName(query);
			runNewSearchQuery(location, NEW_SEARCH_INIT);
		} else {
			filter.setFilterByName(query);
			runNewSearchQuery(location, SEARCH_FURTHER);
		}
		return true;
	}

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.searchpoi);

		getSupportActionBar().setTitle(R.string.searchpoi_activity);
		// getSupportActionBar().setIcon(R.drawable.tab_search_poi_icon);
		setSupportProgressBarIndeterminateVisibility(false);

		app = (OsmandApplication) getApplication();
		amenityAdapter = new AmenityAdapter(new ArrayList<Amenity>());
		setListAdapter(amenityAdapter);
		searchFilterLayout = findViewById(R.id.SearchFilterLayout);
		searchFilter = (EditText) findViewById(R.id.searchEditText);
		accessibilityAssistant = new AccessibilityAssistant(this);
		navigationInfo = new NavigationInfo(app);
		searchFilter.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				tChange = s;
				// Issue #2667 (3)
				if (currentSearchTask == null) {
					changeFilter(tChange);
				}
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}
		});
		searchFilter.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
				}
			}
		});
		searchFilter.setHint(R.string.filter_poi_hint);
		((ImageView) findViewById(R.id.search_icon)).setImageDrawable(
				getMyApplication().getUIUtilities().getThemedIcon(R.drawable.ic_action_filter_dark));
		((ImageView) findViewById(R.id.options)).
				setImageDrawable(getMyApplication().getUIUtilities().getThemedIcon(R.drawable.ic_overflow_menu_white));
		findViewById(R.id.options).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showOptionsMenu(v);
			}
		});
		updateIntent(getIntent());
	}

	public void updateIntent(Intent intent) {
		Bundle bundle = intent.getExtras();
		if (bundle.containsKey(SEARCH_LAT) && bundle.containsKey(SEARCH_LON)) {
			location = new net.osmand.Location("internal"); //$NON-NLS-1$
			location.setLatitude(bundle.getDouble(SEARCH_LAT));
			location.setLongitude(bundle.getDouble(SEARCH_LON));
		}
		searchNearBy = bundle.containsKey(SEARCH_NEARBY);

		String filterId = bundle.getString(AMENITY_FILTER);
		this.filter = app.getPoiFilters().getFilterById(filterId);
		if (filter != null) {
			if (filter.isEmpty() && !isNameSearch()) {
				showEditActivity(filter);
			} else {
				filter.clearPreviousZoom();
				// run query again
				runNewSearchQuery(location, NEW_SEARCH_INIT);
			}
		} else {
			amenityAdapter.setNewModel(Collections.<Amenity>emptyList());
			finish();
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		updateIntent(intent);
	}


	@Override
	protected void onResume() {
		super.onResume();
		updateButtonState(false);
		if (filter != null) {
			String text = filter.getFilterByName() != null ? filter.getFilterByName() : "";
			searchFilter.setText(text);
			searchFilterLayout.setVisibility(text.length() > 0 || isNameSearch() ? View.VISIBLE : View.GONE);

			app.getLocationProvider().removeCompassListener(app.getLocationProvider().getNavigationInfo());
			app.getLocationProvider().addCompassListener(this);
			app.getLocationProvider().registerOrUnregisterCompassListener(true);
		}

		if (searchNearBy) {
			app.getLocationProvider().addLocationListener(this);
			location = app.getLocationProvider().getLastKnownLocation();
			app.getLocationProvider().resumeAllUpdates();
		}
		updateLocation(location);
	}

	private void changeFilter(CharSequence s) {
		String queue = s.toString().trim();
		// if (!isNameSearch() ) {
		amenityAdapter.getFilter().filter(queue);
		String cfilter = filter == null || filter.getFilterByName() == null ? "" :
				filter.getFilterByName().toLowerCase();
		if (!isNameSearch() && !queue.toString().toLowerCase().startsWith(cfilter)) {
			filter.setFilterByName(queue.toString());
			runNewSearchQuery(location, SEARCH_AGAIN);
		}
		updateButtonState(false);
	}


	private void showOptionsMenu(View v) {
		// Show menu with search all, name finder, name finder poi
		UiUtilities iconsCache = getMyApplication().getUIUtilities();
		final PopupMenu optionsMenu = new PopupMenu(this, v);

		final PoiUIFilter f = this.filter;
		MenuItem item = optionsMenu.getMenu().add(R.string.shared_string_edit)
				.setIcon(iconsCache.getThemedIcon(R.drawable.ic_action_edit_dark));
		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				PoiUIFilter custom = getMyApplication().getPoiFilters().getCustomPOIFilter();
				custom.replaceWithPoiFilter(f);
				showEditActivity(custom);
				return true;
			}
		});
		addFilter(optionsMenu, getString(R.string.shared_string_is_open));
		addFilter(optionsMenu, getString(R.string.shared_string_is_open_24_7));
		Map<String, PoiType> poiAdditionals = f.getPoiAdditionals();
		if (poiAdditionals != null) {
			TreeMap<String, PoiType> adds = new TreeMap<String, PoiType>();
			for (PoiType vtype : poiAdditionals.values()) {
				if (vtype.isTopVisible()) {
					adds.put(vtype.getTranslation().replace(' ', ':').toLowerCase(), vtype);
				}
			}
			for (String vtype : adds.keySet()) {
				addFilter(optionsMenu, vtype);
			}
		}

		optionsMenu.show();
	}

	private void addFilter(PopupMenu optionsMenu, final String value) {
		UiUtilities iconsCache = getMyApplication().getUIUtilities();
		MenuItem item = optionsMenu.getMenu().add(getString(R.string.search_poi_filter) + ": " + value)
				.setIcon(iconsCache.getThemedIcon(R.drawable.ic_action_filter_dark));
		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				if (searchFilterLayout.getVisibility() == View.GONE) {
					searchFilterLayout.setVisibility(View.VISIBLE);
				}
				searchFilter.setText((searchFilter.getText().toString() + " " + value.replace(' ', '_').toLowerCase()).trim());
				return true;
			}
		});
	}

	private void showEditActivity(PoiUIFilter poi) {
		Intent newIntent = new Intent(this, EditPOIFilterActivity.class);
		// folder selected
		newIntent.putExtra(EditPOIFilterActivity.AMENITY_FILTER, poi.getFilterId());
		if (location != null) {
			newIntent.putExtra(SearchActivity.SEARCH_LAT, location.getLatitude());
			newIntent.putExtra(SearchActivity.SEARCH_LON, location.getLongitude());
		}
		if (searchNearBy) {
			newIntent.putExtra(SearchActivity.SEARCH_NEARBY, true);
		}
		startActivityForResult(newIntent, RESULT_REQUEST_CODE);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == RESULT_REQUEST_CODE && resultCode == EditPOIFilterActivity.EDIT_ACTIVITY_RESULT_OK) {
			PoiUIFilter custom = app.getPoiFilters().getCustomPOIFilter();
			if (this.filter.isStandardFilter()) {
				this.filter = custom;
				if (!Algorithms.isEmpty(searchFilter.getText().toString())) {
					this.filter.setFilterByName(searchFilter.getText().toString());
				} else {
					this.filter.setFilterByName(null);
				}
			} else {
				this.filter.replaceWithPoiFilter(custom);
			}
			filter.clearPreviousZoom();
			// run query again
			runNewSearchQuery(location, NEW_SEARCH_INIT);
		}
		if (filter == null || filter.isEmpty()) {
			finish();
		}
	}

	private void updateButtonState(boolean next) {
		if (showFilterItem != null) {
			showFilterItem.setVisible(filter != null && !isNameSearch());
		}
		if (filter != null) {
			int maxLength = 24;
			String name = filter.getGeneratedName(maxLength);

			// Next displays the actual query term instead of the generic "Seach by name", can be enabled for debugging or in general
			if (isNameSearch()) {
				name = "'" + filter.getFilterByName() + "'";
			}

			if (name.length() >= maxLength) {
				name = name.substring(0, maxLength) + getString(R.string.shared_string_ellipsis);
			}
			if (filter instanceof NominatimPoiFilter && !((NominatimPoiFilter) filter).isPlacesQuery()) {
				// nothing to add
			} else {
				name += " " + filter.getSearchArea(next);
			}
			getSupportActionBar().setTitle(name);
		}
		if (searchPOILevel != null) {
			int title = location == null ? R.string.search_poi_location : R.string.search_POI_level_btn;
			boolean taskAlreadyFinished = currentSearchTask == null || currentSearchTask.getStatus() != Status.RUNNING;
			boolean enabled = taskAlreadyFinished && location != null &&
					filter != null && filter.isSearchFurtherAvailable();
			if (isNameSearch() && !Algorithms.objectEquals(searchFilter.getText().toString(), filter.getFilterByName())) {
				title = R.string.search_button;
				// Issue #2667 (2)
				if (currentSearchTask == null) {
					enabled = true;
				}
			}
			searchPOILevel.setEnabled(enabled);
			searchPOILevel.setTitle(title);
		}
	}

	private synchronized void runNewSearchQuery(net.osmand.Location location, int requestType) {
		if (currentSearchTask == null || currentSearchTask.getStatus() == Status.FINISHED) {
			currentSearchTask = new SearchAmenityTask(location, requestType);
			currentSearchTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
	}

	public boolean isNominatimFilter() {
		return filter instanceof NominatimPoiFilter;
	}

	public boolean isOfflineSearchByNameFilter() {
		return filter != null && PoiUIFilter.BY_NAME_FILTER_ID.equals(filter.getFilterId());
	}

	public boolean isNameSearch() {
		return isNominatimFilter() || isOfflineSearchByNameFilter();
	}

	@Override
	public void updateLocation(net.osmand.Location location) {
		boolean handled = false;
		if (location != null && filter != null) {
			net.osmand.Location searchedLocation = lastSearchedLocation;
			if (searchedLocation == null) {
				if (!isNameSearch()) {
					runNewSearchQuery(location, NEW_SEARCH_INIT);
				}
				handled = true;
			} else if (location.distanceTo(searchedLocation) > MIN_DISTANCE_TO_RESEARCH) {
				searchedLocation = location;
				runNewSearchQuery(location, SEARCH_AGAIN);
				handled = true;
			} else if (location.distanceTo(searchedLocation) > MIN_DISTANCE_TO_REFRESH) {
				handled = true;
			}
		} else {
			if (location != null) {
				handled = true;
			}
		}
		if (handled) {
			this.location = location;
			ListView lv = getListView();
			final int index = lv.getFirstVisiblePosition();
			View v = lv.getChildAt(0);
			final int top = (v == null) ? 0 : v.getTop();
			accessibilityAssistant.lockEvents();
			amenityAdapter.notifyDataSetChanged();
			lv.setSelectionFromTop(index, top);
			updateButtonState(false);
			accessibilityAssistant.unlockEvents();
			navigationInfo.updateLocation(location);
		}
	}

	@Override
	public void updateCompassValue(float value) {
		// 99 in next line used to one-time initalize arrows (with reference vs. fixed-north direction) on non-compass
		// devices
		float lastHeading = heading != null ? heading : 99;
		heading = value;
		if (heading != null && Math.abs(MapUtils.degreesDiff(lastHeading, heading)) > 5) {
			accessibilityAssistant.lockEvents();
			amenityAdapter.notifyDataSetChanged();
			accessibilityAssistant.unlockEvents();
		} else {
			heading = lastHeading;
		}
		// Comment out and use lastHeading above to see if this fixes issues seen on some devices
		// if(!uiHandler.hasMessages(COMPASS_REFRESH_MSG_ID)){
		// Message msg = Message.obtain(uiHandler, new Runnable(){
		// @Override
		// public void run() {
		// amenityAdapter.notifyDataSetChanged();
		// }
		// });
		// msg.what = COMPASS_REFRESH_MSG_ID;
		// uiHandler.sendMessageDelayed(msg, 100);
		// }
		final View selected = accessibilityAssistant.getFocusedView();
		if (selected != null) {
			try {
				int position = getListView().getPositionForView(selected);
				if ((position != AdapterView.INVALID_POSITION) && (position >= getListView().getHeaderViewsCount())) {
					navigationInfo.updateTargetDirection(amenityAdapter.getItem(position - getListView().getHeaderViewsCount()).
							getLocation(), heading.floatValue());
				}
			} catch (Exception e) {
				return;
			}
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		app.getLocationProvider().pauseAllUpdates();
		app.getLocationProvider().removeCompassListener(this);
		app.getLocationProvider().addCompassListener(app.getLocationProvider().getNavigationInfo());
		if (searchNearBy) {
			app.getLocationProvider().removeLocationListener(this);
		}
		if (!app.accessibilityEnabled()) {
			app.getLocationProvider().removeCompassListener(this);
		}
	}

	@Override
	public void onDetachedFromWindow() {
		accessibilityAssistant.forgetFocus();
	}

	@Override
	public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
		final Amenity amenity = ((AmenityAdapter) getListAdapter()).getItem(position);
		final OsmandSettings settings = app.getSettings();
		String poiSimpleFormat = OsmAndFormatter.getPoiStringWithoutType(amenity,
				app.getSettings().MAP_PREFERRED_LOCALE.get(), 
				app.getSettings().MAP_TRANSLITERATE_NAMES.get());
		PointDescription name = new PointDescription(PointDescription.POINT_TYPE_POI, poiSimpleFormat);
		int z = Math.max(16, settings.getLastKnownMapZoom());

		LatLon location = amenity.getLocation();
		settings.setMapLocationToShow(location.getLatitude(), location.getLongitude(),
				z,
				name,
				true,
				amenity); //$NON-NLS-1$
		MapActivity.launchMapActivityMoveToTop(this);
	}


	private static final int SEARCH_AGAIN = 1;
	private static final int NEW_SEARCH_INIT = 2;
	private static final int SEARCH_FURTHER = 3;

	class SearchAmenityTask extends AsyncTask<Void, Amenity, List<Amenity>> implements ResultMatcher<Amenity> {

		private int requestType;
		private TLongHashSet existingObjects = null;
		private TLongHashSet updateExisting;
		private Location searchLocation;

		public SearchAmenityTask(net.osmand.Location location, int requestType) {
			this.searchLocation = location;
			this.requestType = requestType;
		}

		net.osmand.Location getSearchedLocation() {
			return searchLocation;
		}

		@Override
		protected void onPreExecute() {
			setSupportProgressBarIndeterminateVisibility(true);
			if (searchPOILevel != null) {
				searchPOILevel.setEnabled(false);
			}
			existingObjects = new TLongHashSet();
			updateExisting = new TLongHashSet();
			if (requestType == NEW_SEARCH_INIT) {
				amenityAdapter.clear();
			} else if (requestType == SEARCH_FURTHER) {
				List<Amenity> list = amenityAdapter.getOriginalAmenityList();
				for (Amenity a : list) {
					updateExisting.add(getAmenityId(a));
				}
			}
			updateButtonState(requestType == SEARCH_FURTHER);
		}

		private long getAmenityId(Amenity a) {
			return (a.getId() << 8) + a.getType().ordinal();
		}

		@Override
		protected void onPostExecute(List<Amenity> result) {
			setSupportProgressBarIndeterminateVisibility(false);
			currentSearchTask = null;
			updateButtonState(false);
			if (isNameSearch()) {
				if (isNominatimFilter() && !Algorithms.isEmpty(((NominatimPoiFilter) filter).getLastError())) {
					Toast.makeText(SearchPOIActivity.this, ((NominatimPoiFilter) filter).getLastError(),
							Toast.LENGTH_LONG).show();
				}
				amenityAdapter.setNewModel(result);
				if (showOnMapItem != null) {
					showOnMapItem.setEnabled(amenityAdapter.getCount() > 0);
				}
			} else {
				amenityAdapter.setNewModel(result);
			}
			// Issue #2667 (1)
			if (tChange != null) {
				changeFilter(tChange);
				tChange = null;
			}
			amenityAdapter.notifyDataSetChanged();
			lastSearchedLocation = searchLocation;
		}

		@Override
		protected void onProgressUpdate(Amenity... values) {
			for (Amenity a : values) {
				amenityAdapter.add(a);
			}
		}

		@Override
		protected List<Amenity> doInBackground(Void... params) {
			if (searchLocation != null) {
				if (requestType == NEW_SEARCH_INIT) {
					return filter.initializeNewSearch(searchLocation.getLatitude(), searchLocation.getLongitude(),
							-1, this, -1);
				} else if (requestType == SEARCH_FURTHER) {
					return filter.searchFurther(searchLocation.getLatitude(), searchLocation.getLongitude(), this);
				} else if (requestType == SEARCH_AGAIN) {
					return filter.searchAgain(searchLocation.getLatitude(), searchLocation.getLongitude());
				}
			}
			return Collections.emptyList();
		}

		@Override
		public boolean publish(Amenity object) {
			long id = getAmenityId(object);
			if (existingObjects != null && !existingObjects.contains(id)) {
				existingObjects.add(id);
				if (requestType == NEW_SEARCH_INIT) {
					publishProgress(object);
				} else if (requestType == SEARCH_FURTHER) {
					if (!updateExisting.contains(id)) {
						publishProgress(object);
					}
				}
				return true;
			}
			return false;
		}

	}

	class AmenityAdapter extends ArrayAdapter<Amenity> {
		private AmenityFilter listFilter;
		private List<Amenity> originalAmenityList;
		private UpdateLocationViewCache updateLocationViewCache;

		AmenityAdapter(List<Amenity> list) {
			super(SearchPOIActivity.this, R.layout.searchpoi_list, list);
			updateLocationViewCache = getMyApplication().getUIUtilities().getUpdateLocationViewCache();
			originalAmenityList = new ArrayList<Amenity>(list);
			this.setNotifyOnChange(false);
		}

		public List<Amenity> getOriginalAmenityList() {
			return originalAmenityList;
		}

		public void setNewModel(List<Amenity> amenityList) {
			setNotifyOnChange(false);
			updateLocationViewCache = getMyApplication().getUIUtilities().getUpdateLocationViewCache();
			originalAmenityList = new ArrayList<Amenity>(amenityList);
			clear();
			for (Amenity obj : amenityList) {
				add(obj);
			}
			setNotifyOnChange(true);
			this.notifyDataSetInvalidated();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View row = convertView;
			if (row == null) {
				LayoutInflater inflater = getLayoutInflater();
				row = inflater.inflate(R.layout.searchpoi_list, parent, false);
			}
			float[] mes = null;
			TextView label = (TextView) row.findViewById(R.id.poi_label);
			TextView distanceText = (TextView) row.findViewById(R.id.distance);
			TextView timeText = (TextView) row.findViewById(R.id.time);
			ImageView direction = (ImageView) row.findViewById(R.id.poi_direction);
			ImageView timeIcon = (ImageView) row.findViewById(R.id.time_icon);
			ImageView icon = (ImageView) row.findViewById(R.id.poi_icon);
			Amenity amenity = getItem(position);
			if (amenity.getOpeningHours() != null) {
				OpeningHours rs = OpeningHoursParser.parseOpenedHours(amenity.getOpeningHours());
				if (rs != null) {
					Calendar inst = Calendar.getInstance();
					inst.setTimeInMillis(System.currentTimeMillis());
					boolean worksNow = rs.isOpenedForTime(inst);
					inst.setTimeInMillis(System.currentTimeMillis() + 30 * 60 * 1000); // 30 minutes later
					boolean worksLater = rs.isOpenedForTime(inst);
					int colorId = worksNow ? worksLater ? color.color_ok : color.color_intermediate : color.color_warning;

					timeIcon.setVisibility(View.VISIBLE);
					timeText.setVisibility(View.VISIBLE);
					timeIcon.setImageDrawable(app.getUIUtilities().getIcon(R.drawable.ic_action_time_16, colorId));
					timeText.setTextColor(app.getResources().getColor(colorId));
					String rt = rs.getCurrentRuleTime(inst);
					timeText.setText(rt == null ? "" : rt);
				} else {
					timeIcon.setVisibility(View.GONE);
					timeText.setVisibility(View.GONE);
				}
			} else {
				timeIcon.setVisibility(View.GONE);
				timeText.setVisibility(View.GONE);
			}
			Drawable dd = direction.getDrawable();
			DirectionDrawable draw;
			if (dd instanceof DirectionDrawable) {
				draw = (DirectionDrawable) dd;
			} else {
				draw = new DirectionDrawable(getMyApplication(), 24, 24,
						R.drawable.ic_direction_arrow, R.color.color_distance);
				direction.setImageDrawable(draw);
			}
			net.osmand.Location loc = location;
			if(searchNearBy) {
				updateLocationViewCache.specialFrom = null; 
			} else if(loc != null) {
				updateLocationViewCache.specialFrom = new LatLon(loc.getLatitude(), loc.getLongitude());
			}
			getMyApplication().getUIUtilities().updateLocationView(updateLocationViewCache, direction, distanceText, amenity.getLocation());
			direction.setImageDrawable(draw);
			PoiType st = amenity.getType().getPoiTypeByKeyName(amenity.getSubType());
			if (st != null) {
				if (RenderingIcons.containsBigIcon(st.getIconKeyName())) {
					icon.setImageResource(RenderingIcons.getBigIconResourceId(st.getIconKeyName()));
				} else if (RenderingIcons.containsBigIcon(st.getOsmTag() + "_" + st.getOsmValue())) {
					icon.setImageResource(RenderingIcons.getBigIconResourceId(st.getOsmTag() + "_" + st.getOsmValue()));
				} else {
					icon.setImageDrawable(null);
				}
			} else {
				icon.setImageDrawable(null);
			}

			String poiType = OsmAndFormatter.getPoiStringWithoutType(amenity, 
					app.getSettings().MAP_PREFERRED_LOCALE.get(),
					app.getSettings().MAP_TRANSLITERATE_NAMES.get());
			label.setText(poiType);
			ViewCompat.setAccessibilityDelegate(row, accessibilityAssistant);
			return (row);
		}

		@Override
		public Filter getFilter() {
			if (listFilter == null) {
				listFilter = new AmenityFilter();
			}
			return listFilter;
		}

		private final class AmenityFilter extends Filter {

			@Override
			protected FilterResults performFiltering(CharSequence constraint) {
				FilterResults results = new FilterResults();
				List<Amenity> listToFilter = originalAmenityList;
				if (constraint == null || constraint.length() == 0 || filter == null) {
					results.values = listToFilter;
					results.count = listToFilter.size();
				} else {
					List<Amenity> res = new ArrayList<Amenity>();
					AmenityNameFilter nm = filter.getNameFilter(constraint.toString().toLowerCase());
					for (Amenity item : listToFilter) {
						if (nm.accept(item)) {
							res.add(item);
						}
					}
					results.values = res;
					results.count = res.size();
				}
				return results;
			}

			@SuppressWarnings("unchecked")
			@Override
			protected void publishResults(CharSequence constraint, FilterResults results) {
				clear();
				for (Amenity item : (Collection<Amenity>) results.values) {
					add(item);
				}
			}
		}
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		switch (itemId) {
			case android.R.id.home:
				if (getIntent().hasExtra(MapActivity.INTENT_KEY_PARENT_MAP_ACTIVITY)) {
					Intent newIntent = new Intent(this, SearchActivity.class);
					if (location != null) {
						newIntent.putExtra(SearchActivity.SEARCH_LAT, location.getLatitude());
						newIntent.putExtra(SearchActivity.SEARCH_LON, location.getLongitude());
						if (searchNearBy) {
							newIntent.putExtra(SearchActivity.SEARCH_NEARBY, true);
						}
					}
					newIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
					startActivity(newIntent);
					finish();
					return true;
				}
				break;
			case DELETE_FILTER:
				removePoiFilter();
				return true;
			case SAVE_FILTER:
				savePoiFilter();
				return true;

		}
		return super.onOptionsItemSelected(item);
	}


	private void removePoiFilter() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.edit_filter_delete_dialog_title);
		builder.setNegativeButton(R.string.shared_string_no, null);
		builder.setPositiveButton(R.string.shared_string_yes, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {

				if (app.getPoiFilters().removePoiFilter(filter)) {
					Toast.makeText(
							SearchPOIActivity.this,
							MessageFormat.format(SearchPOIActivity.this.getText(R.string.edit_filter_delete_message).toString(),
									filter.getName()), Toast.LENGTH_SHORT).show();
					SearchPOIActivity.this.finish();
				}

			}
		});
		builder.create().show();
	}

	public void savePoiFilter() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.edit_filter_save_as_menu_item);
		final EditText editText = new EditText(this);
		if (filter.isStandardFilter()) {
			editText.setText((filter.getName() + " " + searchFilter.getText()).trim());
		} else {
			editText.setText(filter.getName());
		}
		LinearLayout ll = new LinearLayout(this);
		ll.setPadding(5, 3, 5, 0);
		ll.addView(editText, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		builder.setView(ll);
		builder.setNegativeButton(R.string.shared_string_cancel, null);
		builder.setPositiveButton(R.string.shared_string_yes, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				PoiUIFilter nFilter = new PoiUIFilter(editText.getText().toString(),
						null,
						filter.getAcceptedTypes(), (OsmandApplication) getApplication());
				if (searchFilter.getText().toString().length() > 0) {
					nFilter.setSavedFilterByName(searchFilter.getText().toString());
				}
				if (app.getPoiFilters().createPoiFilter(nFilter, false)) {
					Toast.makeText(
							SearchPOIActivity.this,
							MessageFormat.format(SearchPOIActivity.this.getText(R.string.edit_filter_create_message).toString(),
									editText.getText().toString()), Toast.LENGTH_SHORT).show();
				}
				SearchPOIActivity.this.finish();
			}
		});
		builder.create().show();

	}

}
