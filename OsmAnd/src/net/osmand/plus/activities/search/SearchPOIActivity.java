/**
 * 
 */
package net.osmand.plus.activities.search;


import gnu.trove.set.hash.TLongHashSet;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.londatiga.android.ActionItem;
import net.londatiga.android.QuickAction;
import net.osmand.ResultMatcher;
import net.osmand.access.AccessibleToast;
import net.osmand.access.NavigationInfo;
import net.osmand.data.Amenity;
import net.osmand.data.AmenityType;
import net.osmand.data.LatLon;
import net.osmand.osm.MapRenderingTypes;
import net.osmand.plus.NameFinderPoiFilter;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmAndLocationProvider.OsmAndCompassListener;
import net.osmand.plus.OsmAndLocationProvider.OsmAndLocationListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.PoiFilter;
import net.osmand.plus.R;
import net.osmand.plus.SearchByNameFilter;
import net.osmand.plus.activities.EditPOIFilterActivity;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.MapActivityActions;
import net.osmand.plus.activities.OsmandListActivity;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.util.Algorithms;
import net.osmand.util.OpeningHoursParser;
import net.osmand.util.OpeningHoursParser.OpeningHours;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.text.util.Linkify;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.internal.ResourcesCompat;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener;
import com.actionbarsherlock.view.Window;

/**
 * Search poi activity
 */
public class SearchPOIActivity extends OsmandListActivity implements OsmAndCompassListener, OsmAndLocationListener {

	public static final String AMENITY_FILTER = "net.osmand.amenity_filter"; //$NON-NLS-1$
	public static final String SEARCH_LAT = SearchActivity.SEARCH_LAT; //$NON-NLS-1$
	public static final String SEARCH_LON = SearchActivity.SEARCH_LON; //$NON-NLS-1$
	private static final float MIN_DISTANCE_TO_RESEARCH = 20;
	private static final float MIN_DISTANCE_TO_REFRESH = 5;
	private static final int SEARCH_MORE = 0;
	private static final int SHOW_ON_MAP = 1;
	private static final int FILTER = 2;

	private PoiFilter filter;
	private AmenityAdapter amenityAdapter;
	private EditText searchFilter;
	private View searchFilterLayout;
	
	private boolean searchNearBy = false;
	private net.osmand.Location location = null; 
	private Float heading = null;
	
	private Handler uiHandler;
	private OsmandSettings settings;
	private Path directionPath = new Path();
	private float width = 24;
	private float height = 24;
	
	// never null represents current running task or last finished
	private SearchAmenityTask currentSearchTask = new SearchAmenityTask(null);
	private OsmandApplication app;
	private MenuItem showFilterItem;
	private MenuItem showOnMapItem;
	private MenuItem searchPOILevel;
	private Button searchFooterButton;
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean light = getMyApplication().getSettings().isLightActionBar();
		searchPOILevel = menu.add(0, SEARCH_MORE, 0, R.string.search_POI_level_btn).setShowAsActionFlags(
				MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		searchPOILevel.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(com.actionbarsherlock.view.MenuItem item) {

				return searchMore();
			}

		});
		updateSearchPoiTextButton(false);
		
		showFilterItem = menu.add(0, FILTER, 0, R.string.search_poi_filter).setShowAsActionFlags(
				MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		showFilterItem = showFilterItem.setIcon(light ? R.drawable.ic_action_filter_light: R.drawable.ic_action_filter_dark);
		showFilterItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(com.actionbarsherlock.view.MenuItem item) {
				if(isSearchByNameFilter()){
					Intent newIntent = new Intent(SearchPOIActivity.this, EditPOIFilterActivity.class);
					newIntent.putExtra(EditPOIFilterActivity.AMENITY_FILTER, PoiFilter.CUSTOM_FILTER_ID);
					if(location != null) {
						newIntent.putExtra(EditPOIFilterActivity.SEARCH_LAT, location.getLatitude());
						newIntent.putExtra(EditPOIFilterActivity.SEARCH_LON, location.getLongitude());
					}
					startActivity(newIntent);
				} else {
					if (searchFilterLayout.getVisibility() == View.GONE) {
						searchFilterLayout.setVisibility(View.VISIBLE);
					} else {
						searchFilter.setText(""); //$NON-NLS-1$
						searchFilterLayout.setVisibility(View.GONE);
					}
				}
				return true;
			}
		});
		updateShowFilterItem();
		if(isSearchByNameFilter() || isNameFinderFilter()) {
			showFilterItem.setVisible(false);
		}
		

		showOnMapItem = menu.add(0, SHOW_ON_MAP, 0, R.string.search_shown_on_map).setShowAsActionFlags(
				MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		showOnMapItem = showOnMapItem.setIcon(light ? R.drawable.ic_action_map_marker_light : R.drawable.ic_action_map_marker_dark);
		showOnMapItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(com.actionbarsherlock.view.MenuItem item) {
				if (searchFilter.getVisibility() == View.VISIBLE) {
					filter.setNameFilter(searchFilter.getText().toString());
				}
				settings.setPoiFilterForMap(filter.getFilterId());
				settings.SHOW_POI_OVER_MAP.set(true);
				if (location != null) {
					settings.setMapLocationToShow(location.getLatitude(), location.getLongitude(), 15);
				}
				MapActivity.launchMapActivityMoveToTop(SearchPOIActivity.this);
				return true;

			}
		});
		return true;
	}
	
	private boolean searchMore() {
		String query = searchFilter.getText().toString().trim();
		if (query.length() < 2 && (isNameFinderFilter() || isSearchByNameFilter())) {
			AccessibleToast.makeText(SearchPOIActivity.this, R.string.poi_namefinder_query_empty, Toast.LENGTH_LONG).show();
			return true;
		}
		if (isNameFinderFilter() && !Algorithms.objectEquals(((NameFinderPoiFilter) filter).getQuery(), query)) {
			filter.clearPreviousZoom();
			((NameFinderPoiFilter) filter).setQuery(query);
			runNewSearchQuery(SearchAmenityRequest.buildRequest(location, SearchAmenityRequest.NEW_SEARCH_INIT));
		} else if (isSearchByNameFilter() && !Algorithms.objectEquals(((SearchByNameFilter) filter).getQuery(), query)) {
			showFilterItem.setVisible(false);
			filter.clearPreviousZoom();
			showPoiCategoriesByNameFilter(query, location);
			((SearchByNameFilter) filter).setQuery(query);
			runNewSearchQuery(SearchAmenityRequest.buildRequest(location, SearchAmenityRequest.NEW_SEARCH_INIT));
		} else {
			runNewSearchQuery(SearchAmenityRequest.buildRequest(location, SearchAmenityRequest.SEARCH_FURTHER));
		}
		return true;
	}

	
	@Override
	public void onCreate(Bundle icicle) {
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		super.onCreate(icicle);
		setContentView(R.layout.searchpoi);
		
		getSupportActionBar().setTitle(R.string.searchpoi_activity);
		getSupportActionBar().setIcon(R.drawable.tab_search_poi_icon);
		getSherlock().setProgressBarIndeterminateVisibility(false);
		
		app = (OsmandApplication)getApplication();
		
		uiHandler = new Handler();
		searchFilter = (EditText) findViewById(R.id.SearchFilter);
		searchFilterLayout = findViewById(R.id.SearchFilterLayout);
		directionPath = createDirectionPath();
		
		settings = ((OsmandApplication) getApplication()).getSettings();
		
		searchFilter.addTextChangedListener(new TextWatcher(){
			@Override
			public void afterTextChanged(Editable s) {
				if(!isNameFinderFilter() && !isSearchByNameFilter()){
					amenityAdapter.getFilter().filter(s);
				} else {
					if(searchPOILevel != null)  {
						searchPOILevel.setEnabled(true);
						searchPOILevel.setTitle(R.string.search_button);
					}
					searchFooterButton.setEnabled(true);
					searchFooterButton.setText(R.string.search_button);
					// Cancel current search request here?
				}
			}
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}
		});
		addFooterView();
		amenityAdapter = new AmenityAdapter(new ArrayList<Amenity>());
		setListAdapter(amenityAdapter);
		
		
		
	}

	private void addFooterView() {
		final FrameLayout ll = new FrameLayout(this);
		android.widget.FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		lp.gravity = Gravity.CENTER_HORIZONTAL;
		searchFooterButton = new Button(this);
		searchFooterButton.setText(R.string.search_POI_level_btn);
		searchFooterButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				searchMore();
			}
		});
		searchFooterButton.setLayoutParams(lp);
		ll.addView(searchFooterButton);
		
		getListView().addFooterView(ll);
	}
	
	private Path createDirectionPath() {
		int h = 15;
		int w = 4;
		float sarrowL = 8; // side of arrow
		float harrowL = (float) Math.sqrt(2) * sarrowL; // hypotenuse of arrow
		float hpartArrowL = (float) (harrowL - w) / 2;
		Path path = new Path();
		path.moveTo(width / 2, height - (height - h) / 3);
		path.rMoveTo(w / 2, 0);
		path.rLineTo(0, -h);
		path.rLineTo(hpartArrowL, 0);
		path.rLineTo(-harrowL / 2, -harrowL / 2); // center
		path.rLineTo(-harrowL / 2, harrowL / 2);
		path.rLineTo(hpartArrowL, 0);
		path.rLineTo(0, h);
		
		Matrix pathTransform = new Matrix();
		WindowManager mgr = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
		DisplayMetrics dm = new DisplayMetrics();
		mgr.getDefaultDisplay().getMetrics(dm);
		pathTransform.postScale(dm.density, dm.density);
		path.transform(pathTransform);
		width *= dm.density;
		height *= dm.density;
		return path;
	}

	@Override
	protected void onResume() {
		super.onResume();
		Bundle bundle = this.getIntent().getExtras();
		if(bundle.containsKey(SEARCH_LAT) && bundle.containsKey(SEARCH_LON)){
			location = new net.osmand.Location("internal"); //$NON-NLS-1$
			location.setLatitude(bundle.getDouble(SEARCH_LAT));
			location.setLongitude(bundle.getDouble(SEARCH_LON));
			searchNearBy = false;
		} else {
			location = null;
			searchNearBy = true;
		}
		
		
		String filterId = bundle.getString(AMENITY_FILTER);
		PoiFilter filter = app.getPoiFilters().getFilterById(filterId);
		if (filter != this.filter) {
			this.filter = filter;
			if (filter != null) {
				filter.clearPreviousZoom();
			} else {
				amenityAdapter.setNewModel(Collections.<Amenity> emptyList(), "");
			}
			// run query again
			clearSearchQuery();
		}
		if(filter != null) {
			filter.clearNameFilter();
		}
		
	
		updateSubtitle();
		updateSearchPoiTextButton(false);
		updateShowFilterItem();
		if (filter != null) {
			if (searchNearBy) {
				app.getLocationProvider().addLocationListener(this);
				location = app.getLocationProvider().getLastKnownLocation();
				app.getLocationProvider().resumeAllUpdates();
			}
			updateLocation(location);
		}
		if(isNameFinderFilter()){
			searchFilterLayout.setVisibility(View.VISIBLE);
		} else if(isSearchByNameFilter() ){
			searchFilterLayout.setVisibility(View.VISIBLE);
		}
		app.getLocationProvider().addCompassListener(this);
		app.getLocationProvider().registerOrUnregisterCompassListener(true);
	}


	private void updateShowFilterItem() {
		if(showFilterItem != null) {
			showFilterItem.setVisible(filter != null);
		}
	}

	private void updateSubtitle() {
		if(filter != null) {
			getSupportActionBar().setSubtitle(filter.getName() + " " + filter.getSearchArea());
		}
	}
	
	private void showPoiCategoriesByNameFilter(String query, net.osmand.Location loc){
		OsmandApplication app = (OsmandApplication) getApplication();
		if(loc != null){
			Map<AmenityType, List<String>> map = app.getResourceManager().searchAmenityCategoriesByName(query, loc.getLatitude(), loc.getLongitude());
			if(!map.isEmpty()){
				PoiFilter filter = ((OsmandApplication)getApplication()).getPoiFilters().getFilterById(PoiFilter.CUSTOM_FILTER_ID);
				if(filter != null){
					showFilterItem.setVisible(true);
					filter.setMapToAccept(map);
				}
				
				String s = typesToString(map);
				AccessibleToast.makeText(this, getString(R.string.poi_query_by_name_matches_categories) + s, Toast.LENGTH_LONG).show();
			}
		}
	}
	
	private String typesToString(Map<AmenityType, List<String>> map) {
		StringBuilder b = new StringBuilder();
		int count = 0;
		Iterator<Entry<AmenityType, List<String>>> iterator = map.entrySet().iterator();
		while(iterator.hasNext() && count < 4){
			Entry<AmenityType, List<String>> e = iterator.next();
			b.append("\n").append(OsmAndFormatter.toPublicString(e.getKey(), getMyApplication())).append(" - ");
			if(e.getValue() == null){
				b.append("...");
			} else {
				for(int j=0; j<e.getValue().size() && j < 3; j++){
					if(j > 0){
						b.append(", ");
					}
					b.append(e.getValue().get(j));
				}
			}
		}
		if(iterator.hasNext()){
			b.append("\n...");
		}
		return b.toString();
	}

	private void updateSearchPoiTextButton(boolean taskAlreadyFinished) {
		boolean enabled = false;
		int title = R.string.search_POI_level_btn;

		if (location == null) {
			title = R.string.search_poi_location;
			enabled = false;
		} else if (filter != null && !isNameFinderFilter() && !isSearchByNameFilter()) {
			title = R.string.search_POI_level_btn;
			enabled = (taskAlreadyFinished || currentSearchTask.getStatus() != Status.RUNNING) && filter.isSearchFurtherAvailable();
		} else if (filter != null) {
			title = R.string.search_button;
			enabled = (taskAlreadyFinished || currentSearchTask.getStatus() != Status.RUNNING) && filter.isSearchFurtherAvailable();
		}
		if (searchPOILevel != null) {
			searchPOILevel.setEnabled(enabled);
			searchPOILevel.setTitle(title);
		}
		if(ResourcesCompat.getResources_getBoolean(this, R.bool.abs__split_action_bar_is_narrow)) {
			searchFooterButton.setVisibility(View.GONE);
		} else {
			searchFooterButton.setVisibility(View.VISIBLE);
			searchFooterButton.setEnabled(enabled);
			searchFooterButton.setText(title);
		}
	}
	
	
	private net.osmand.Location getSearchedLocation(){
		return currentSearchTask.getSearchedLocation();
	}
	
	private synchronized void runNewSearchQuery(SearchAmenityRequest request){
		if(currentSearchTask.getStatus() == Status.FINISHED ||
				currentSearchTask.getSearchedLocation() == null){
			currentSearchTask = new SearchAmenityTask(request);
			currentSearchTask.execute();
		}
	}
	
	private synchronized void clearSearchQuery(){
		if(currentSearchTask.getStatus() == Status.FINISHED ||
				currentSearchTask.getSearchedLocation() == null){
			currentSearchTask = new SearchAmenityTask(null);
		}
	}
	
	
	public boolean isNameFinderFilter(){
		return filter instanceof NameFinderPoiFilter; 
	}
	
	public boolean isSearchByNameFilter(){
		return filter != null && PoiFilter.BY_NAME_FILTER_ID.equals(filter.getFilterId()); 
	}
	

	@Override
	public void updateLocation(net.osmand.Location location) {
		boolean handled = false;
		if (location != null && filter != null) {
			net.osmand.Location searchedLocation = getSearchedLocation();
			if (searchedLocation == null) {
  				searchedLocation = location;
				if (!isNameFinderFilter() && !isSearchByNameFilter()) {
					runNewSearchQuery(SearchAmenityRequest.buildRequest(location, SearchAmenityRequest.NEW_SEARCH_INIT));
				}
				handled = true;
			} else if (location.distanceTo(searchedLocation) > MIN_DISTANCE_TO_RESEARCH) {
				searchedLocation = location;
				runNewSearchQuery(SearchAmenityRequest.buildRequest(location, SearchAmenityRequest.SEARCH_AGAIN));
				handled = true;
			} else if (location.distanceTo(searchedLocation) > MIN_DISTANCE_TO_REFRESH){
				handled = true;
			}
		} else {
			if(location != null){
				handled = true;
			}
		}
		if (handled) {
			this.location = location;
			updateSearchPoiTextButton(false);
			// Get the top position from the first visible element
			int idx = getListView().getFirstVisiblePosition();
			View vfirst = getListView().getChildAt(0);
			int pos = 0;
			if (vfirst != null)
				pos = vfirst.getTop();
			amenityAdapter.notifyDataSetInvalidated();
			// Restore the position
			getListView().setSelectionFromTop(idx, pos);
		}	
		
	}

	@Override
	public void updateCompassValue(float value) {
		heading = value;
		if(!uiHandler.hasMessages(5)){
			Message msg = Message.obtain(uiHandler, new Runnable(){
				@Override
				public void run() {
					amenityAdapter.notifyDataSetChanged();
				}
			});
			msg.what = 5;
			uiHandler.sendMessageDelayed(msg, 100);
		}
	}
	
	
	@Override
	protected void onPause() {
		super.onPause();
		if (searchNearBy) {
			app.getLocationProvider().pauseAllUpdates();
			app.getLocationProvider().removeCompassListener(this);
			app.getLocationProvider().removeLocationListener(this);
		}
	}
	
	

	@Override
	public void onListItemClick(ListView parent, View v, int position, long id) {
		final Amenity amenity = ((AmenityAdapter) getListAdapter()).getItem(position);
		final QuickAction qa = new QuickAction(v);
		String poiSimpleFormat = OsmAndFormatter.getPoiSimpleFormat(amenity, getMyApplication(), settings.usingEnglishNames());
		String name = poiSimpleFormat;
		int z = Math.max(16, settings.getLastKnownMapZoom());
		MapActivityActions.createDirectionsActions(qa, amenity.getLocation(), amenity, name, z, this, true , null);
		ActionItem poiDescription = new ActionItem();
		poiDescription.setIcon(getResources().getDrawable(R.drawable.ic_action_note_light));
		poiDescription.setTitle(getString(R.string.poi_context_menu_showdescription));
		final StringBuilder d = getDescriptionContent(amenity);
		poiDescription.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// Build text
				
				// Find and format links
				SpannableString spannable = new SpannableString(d);
				Linkify.addLinks(spannable, Linkify.ALL);

				// Create dialog
				Builder bs = new AlertDialog.Builder(v.getContext());
				bs.setTitle(OsmAndFormatter.getPoiSimpleFormat(amenity, getMyApplication(),
						settings.USE_ENGLISH_NAMES.get()));
				bs.setMessage(spannable);
				AlertDialog dialog = bs.show();

				// Make links clickable
				TextView textView = (TextView) dialog.findViewById(android.R.id.message);
				textView.setMovementMethod(LinkMovementMethod.getInstance());
				textView.setLinksClickable(true);
			}

		});
		if(d.toString().trim().length() > 0) { 
			qa.addActionItem(poiDescription);
		}
		if (((OsmandApplication)getApplication()).getInternalAPI().accessibilityEnabled()) {
			ActionItem showDetails = new ActionItem();
			showDetails.setTitle(getString(R.string.show_details));
			showDetails.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					showPOIDetails(amenity, settings.usingEnglishNames());
				}
			});
			qa.addActionItem(showDetails);
		}
		qa.show();
		
		
	}
	
	private StringBuilder getDescriptionContent(final Amenity amenity) {
		StringBuilder d = new StringBuilder();
		if(amenity.getOpeningHours() != null) {
			d.append(getString(R.string.opening_hours) + " : ").append(amenity.getOpeningHours()).append("\n");
		}
		if(amenity.getPhone() != null) {
			d.append(getString(R.string.phone) + " : ").append(amenity.getPhone()).append("\n");
		}
		if(amenity.getSite() != null) {
			d.append(getString(R.string.website) + " : ").append(amenity.getSite()).append("\n");
		}
		if(amenity.getDescription() != null) {
			d.append(amenity.getDescription());
		}
		return d;
	}
	
	
	static class SearchAmenityRequest {
		private static final int SEARCH_AGAIN = 1;
		private static final int NEW_SEARCH_INIT = 2;
		private static final int SEARCH_FURTHER = 3;
		private int type;
		private net.osmand.Location location;
		
		public static SearchAmenityRequest buildRequest(net.osmand.Location l, int type){
			SearchAmenityRequest req = new SearchAmenityRequest();
			req.type = type;
			req.location = l;
			return req;
			
		}
	}
	
	class SearchAmenityTask extends AsyncTask<Void, Amenity, List<Amenity>> implements ResultMatcher<Amenity> {
		
		private SearchAmenityRequest request;
		private TLongHashSet existingObjects = null;
		private TLongHashSet updateExisting;
		
		public SearchAmenityTask(SearchAmenityRequest request){
			this.request = request;
			
		}
		
		net.osmand.Location getSearchedLocation(){
			return request != null ? request.location : null; 
		}

		@Override
		protected void onPreExecute() {
			getSherlock().setProgressBarIndeterminateVisibility(true);
			if(searchPOILevel != null) {
				searchPOILevel.setEnabled(false);
			}
			searchFooterButton.setEnabled(false);
			existingObjects = new TLongHashSet();
			updateExisting = new TLongHashSet();
			if(request.type == SearchAmenityRequest.NEW_SEARCH_INIT){
				amenityAdapter.clear();
			} else if (request.type == SearchAmenityRequest.SEARCH_FURTHER) {
				List<Amenity> list = amenityAdapter.getOriginalAmenityList();
				for (Amenity a : list) {
					updateExisting.add(getAmenityId(a));
				}
			}
		}
		private long getAmenityId(Amenity a){
			return (a.getId() << 8) + a.getType().ordinal();
		}
		
		@Override
		protected void onPostExecute(List<Amenity> result) {
			getSherlock().setProgressBarIndeterminateVisibility(false);
			updateSearchPoiTextButton(true);
			if (isNameFinderFilter()) {
				if (!Algorithms.isEmpty(((NameFinderPoiFilter) filter).getLastError())) {
					AccessibleToast.makeText(SearchPOIActivity.this, ((NameFinderPoiFilter) filter).getLastError(), Toast.LENGTH_LONG).show();
				}
				amenityAdapter.setNewModel(result, "");
				showOnMapItem.setEnabled(amenityAdapter.getCount() > 0);
			} else if (isSearchByNameFilter()) {
				showOnMapItem.setEnabled(amenityAdapter.getCount() > 0);
				amenityAdapter.setNewModel(result, "");
			} else {
				amenityAdapter.setNewModel(result, searchFilter.getText().toString());
			}
			updateSubtitle();
		}
		
		@Override
		protected void onProgressUpdate(Amenity... values) {
			for(Amenity a : values){
				amenityAdapter.add(a);
			}
		}


		@Override
		protected List<Amenity> doInBackground(Void... params) {
			if (request.location != null) {
				if (request.type == SearchAmenityRequest.NEW_SEARCH_INIT) {
					return filter.initializeNewSearch(request.location.getLatitude(), request.location.getLongitude(), -1, this);
				} else if (request.type == SearchAmenityRequest.SEARCH_FURTHER) {
					return filter.searchFurther(request.location.getLatitude(), request.location.getLongitude(), this);
				} else if (request.type == SearchAmenityRequest.SEARCH_AGAIN) {
					return filter.searchAgain(request.location.getLatitude(), request.location.getLongitude());
				}
			}
			return Collections.emptyList();
		}

		@Override
		public boolean publish(Amenity object) {
			long id = getAmenityId(object);
			if (existingObjects != null && !existingObjects.contains(id)) {
				existingObjects.add(id);
				if (request.type == SearchAmenityRequest.NEW_SEARCH_INIT) {
					publishProgress(object);
				} else if (request.type == SearchAmenityRequest.SEARCH_FURTHER) {
					if(!updateExisting.contains(id)){
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
		AmenityAdapter(List<Amenity> list) {
			super(SearchPOIActivity.this, R.layout.searchpoi_list, list);
			originalAmenityList = new ArrayList<Amenity>(list);
			this.setNotifyOnChange(false);
		}
		

		public List<Amenity> getOriginalAmenityList() {
			return originalAmenityList;
		}


		public void setNewModel(List<Amenity> amenityList, String filter) {
			setNotifyOnChange(false);
			originalAmenityList = new ArrayList<Amenity>(amenityList);
			clear();
			for (Amenity obj : amenityList) {
				add(obj);
			}
			getFilter().filter(filter);
			setNotifyOnChange(true);
			this.notifyDataSetChanged();
			
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
			ImageView direction = (ImageView) row.findViewById(R.id.poi_direction);
			ImageView icon = (ImageView) row.findViewById(R.id.poi_icon);
			Amenity amenity = getItem(position);
			net.osmand.Location loc = location;
			if(loc != null){
				mes = new float[2];
				LatLon l = amenity.getLocation();
				net.osmand.Location.distanceBetween(l.getLatitude(), l.getLongitude(), loc.getLatitude(), loc.getLongitude(), mes);
			}
			int opened = -1;
			if (amenity.getOpeningHours() != null) {
				OpeningHours rs = OpeningHoursParser.parseOpenedHours(amenity.getOpeningHours());
				if (rs != null) {
					Calendar inst = Calendar.getInstance();
					inst.setTimeInMillis(System.currentTimeMillis());
					boolean work = false;
					work = rs.isOpenedForTime(inst);
					if (work) {
						opened = 0;
					} else {
						opened = 1;
					}
				}
			}
			if(loc != null){
				DirectionDrawable draw = new DirectionDrawable();
				Float h = heading;
				float a = h != null ? h : 0;
				draw.setAngle(mes[1] - a + 180);
				draw.setOpenedColor(opened);
				direction.setImageDrawable(draw);
			} else {
				if(opened == -1){
					direction.setImageResource(R.drawable.poi);
				} else if(opened == 0){
					direction.setImageResource(R.drawable.opened_poi);
				} else {
					direction.setImageResource(R.drawable.closed_poi);
				}
			}
			StringBuilder tag = new StringBuilder();
			StringBuilder value = new StringBuilder();
			MapRenderingTypes.getDefault().getAmenityTagValue(amenity.getType(), amenity.getSubType(),
					tag, value);
			if(RenderingIcons.containsBigIcon(tag + "_" + value)) {
				icon.setImageResource(RenderingIcons.getBigIconResourceId(tag + "_" + value));
			} else if(RenderingIcons.containsBigIcon(value.toString())) {
				icon.setImageResource(RenderingIcons.getBigIconResourceId(value.toString()));
			} else {
				icon.setImageDrawable(null);
			}

			String distance = "  ";
			if(mes != null){
				distance = " " + OsmAndFormatter.getFormattedDistance((int) mes[0], getMyApplication()) + "  "; //$NON-NLS-1$
			}
			String poiType = OsmAndFormatter.getPoiStringWithoutType(amenity, settings.usingEnglishNames());
			label.setText(distance + poiType, TextView.BufferType.SPANNABLE);
			((Spannable) label.getText()).setSpan(new ForegroundColorSpan(getResources().getColor(R.color.color_distance)), 0, distance.length() - 1, 0);
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
				if (constraint == null || constraint.length() == 0) {
					results.values = listToFilter;
					results.count = listToFilter.size();
				} else {
					String lowerCase = constraint.toString()
							.toLowerCase();
					List<Amenity> filter = new ArrayList<Amenity>();
					for (Amenity item : listToFilter) {
						String lower = OsmAndFormatter.getPoiStringWithoutType(item, settings.usingEnglishNames()).toLowerCase();
						if(lower.indexOf(lowerCase) != -1){
							filter.add(item);
						}
					}
					results.values = filter;
					results.count = filter.size();
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

	private void showPOIDetails(final Amenity amenity, boolean en) {
		AlertDialog.Builder b = new AlertDialog.Builder(SearchPOIActivity.this);
		b.setTitle(OsmAndFormatter.getPoiSimpleFormat(amenity, getMyApplication(), en));
		b.setPositiveButton(R.string.default_buttons_ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();
			}
		});
		List<String> attributes = new ArrayList<String>();
		NavigationInfo navigationInfo = app.getLocationProvider().getNavigationInfo();
		String direction = navigationInfo.getDirectionString(amenity.getLocation(), heading);
		if (direction != null)
			attributes.add(direction);
		if (amenity.getPhone() != null) 
			attributes.add(getString(R.string.phone) + " " + amenity.getPhone());
		if (amenity.getOpeningHours() != null)
			attributes.add(getString(R.string.opening_hours) + " " + amenity.getOpeningHours());
		attributes.add(getString(R.string.navigate_point_latitude) + " " + Double.toString(amenity.getLocation().getLatitude()));
		attributes.add(getString(R.string.navigate_point_longitude) + " " + Double.toString(amenity.getLocation().getLongitude()));
		b.setItems(attributes.toArray(new String[attributes.size()]),
			new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
				}
			});
		b.show();
	}

	
	class DirectionDrawable extends Drawable {
		Paint paintRouteDirection;
		
		private float angle;
		
		public DirectionDrawable(){
			paintRouteDirection = new Paint();
			paintRouteDirection.setStyle(Style.FILL_AND_STROKE);
			paintRouteDirection.setColor(getResources().getColor(R.color.color_unknown));
			paintRouteDirection.setAntiAlias(true);
		}
		
		public void setOpenedColor(int opened){
			if(opened == 0){
				paintRouteDirection.setColor(getResources().getColor(R.color.color_ok));
			} else if(opened == -1){
				paintRouteDirection.setColor(getResources().getColor(R.color.color_unknown));
			} else {
				paintRouteDirection.setColor(getResources().getColor(R.color.color_warning));
			}
		}
		
		
		public void setAngle(float angle){
			this.angle = angle;
		}

		@Override
		public void draw(Canvas canvas) {
			canvas.rotate(angle, width/2, height/2);
			canvas.drawPath(directionPath, paintRouteDirection);
		}

		@Override
		public int getOpacity() {
			return 0;
		}

		@Override
		public void setAlpha(int alpha) {
			paintRouteDirection.setAlpha(alpha);
			
		}

		@Override
		public void setColorFilter(ColorFilter cf) {
			paintRouteDirection.setColorFilter(cf);
		}
	}


}
