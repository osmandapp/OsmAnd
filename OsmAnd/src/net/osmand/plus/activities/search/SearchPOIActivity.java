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

import net.osmand.Algoritms;
import net.osmand.LogUtil;
import net.osmand.OsmAndFormatter;
import net.osmand.ResultMatcher;
import net.osmand.data.Amenity;
import net.osmand.data.AmenityType;
import net.osmand.osm.LatLon;
import net.osmand.osm.OpeningHoursParser;
import net.osmand.osm.OpeningHoursParser.OpeningHoursRule;
import net.osmand.plus.NameFinderPoiFilter;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.PoiFilter;
import net.osmand.plus.R;
import net.osmand.plus.SearchByNameFilter;
import net.osmand.plus.activities.EditPOIFilterActivity;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.OsmandApplication;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Paint.Style;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.AsyncTask.Status;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Search poi activity
 */
public class SearchPOIActivity extends ListActivity implements SensorEventListener {

	public static final String AMENITY_FILTER = "net.osmand.amenity_filter"; //$NON-NLS-1$
	public static final String SEARCH_LAT = SearchActivity.SEARCH_LAT; //$NON-NLS-1$
	public static final String SEARCH_LON = SearchActivity.SEARCH_LON; //$NON-NLS-1$
	private static final int GPS_TIMEOUT_REQUEST = 1000;
	private static final int GPS_DIST_REQUEST = 5;
	private static final int MIN_DISTANCE_TO_RESEARCH = 70;
	private static final int MIN_DISTANCE_TO_UPDATE = 6;


	private Button searchPOILevel;
	private ImageButton showOnMap;
	private ImageButton showFilter;
	private PoiFilter filter;
	private AmenityAdapter amenityAdapter;
	private TextView searchArea;
	private EditText searchFilter;
	private View searchFilterLayout;
	
	private boolean searchNearBy = false;
	private Location location = null; 
	private Float heading = null;
	
	private String currentLocationProvider = null;
	private boolean sensorRegistered = false;
	private Handler uiHandler;
	private OsmandSettings settings;
	
	// never null represents current running task or last finished
	private SearchAmenityTask currentSearchTask = new SearchAmenityTask(null); 

	
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.searchpoi);
		
		uiHandler = new Handler();
		searchPOILevel = (Button) findViewById(R.id.SearchPOILevelButton);
		searchArea = (TextView) findViewById(R.id.SearchAreaText);
		searchFilter = (EditText) findViewById(R.id.SearchFilter);
		searchFilterLayout = findViewById(R.id.SearchFilterLayout);
		showOnMap = (ImageButton) findViewById(R.id.ShowOnMap);
		showFilter = (ImageButton) findViewById(R.id.ShowFilter);
		
		settings = OsmandSettings.getOsmandSettings(this);
		
		searchPOILevel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				String query = searchFilter.getText().toString();
				if (query.length() < 2 && (isNameFinderFilter() || isSearchByNameFilter())) {
					Toast.makeText(SearchPOIActivity.this, R.string.poi_namefinder_query_empty, Toast.LENGTH_LONG).show();
					return;
				}
				if(isNameFinderFilter() && 
						!Algoritms.objectEquals(((NameFinderPoiFilter) filter).getQuery(), query)){
					filter.clearPreviousZoom();
					((NameFinderPoiFilter) filter).setQuery(query);
					runNewSearchQuery(SearchAmenityRequest.buildRequest(location, SearchAmenityRequest.NEW_SEARCH_INIT));
				} else if(isSearchByNameFilter() && 
						!Algoritms.objectEquals(((SearchByNameFilter) filter).getQuery(), query)){
					showFilter.setVisibility(View.INVISIBLE);
					filter.clearPreviousZoom();
					showPoiCategoriesByNameFilter(query, location);
					((SearchByNameFilter) filter).setQuery(query);
					runNewSearchQuery(SearchAmenityRequest.buildRequest(location, SearchAmenityRequest.NEW_SEARCH_INIT));
				} else {
					runNewSearchQuery(SearchAmenityRequest.buildRequest(location, SearchAmenityRequest.SEARCH_FURTHER));
				}
			}
		});
		
		showFilter.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
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
			}
		});
		
		searchFilter.addTextChangedListener(new TextWatcher(){
			@Override
			public void afterTextChanged(Editable s) {
				if(!isNameFinderFilter() && !isSearchByNameFilter()){
					amenityAdapter.getFilter().filter(s);
				} else {
					searchPOILevel.setEnabled(true);
					searchPOILevel.setText(R.string.search_button);
				}
			}
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}
		});
		
		showOnMap.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(searchFilter.getVisibility() == View.VISIBLE) {
					filter.setNameFilter(searchFilter.getText().toString());
				}
				settings.setPoiFilterForMap(filter.getFilterId());
				settings.SHOW_POI_OVER_MAP.set(true);
				if(location != null){
					settings.setMapLocationToShow(location.getLatitude(), location.getLongitude(), 15);
				}
				MapActivity.launchMapActivityMoveToTop(SearchPOIActivity.this);
			}
		});
		amenityAdapter = new AmenityAdapter(new ArrayList<Amenity>());
		setListAdapter(amenityAdapter);
		
		// ListActivity has a ListView, which you can get with:
		ListView lv = getListView();

		lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> av, View v, int pos, long id) {
				final Amenity amenity = ((AmenityAdapter) getListAdapter()).getItem(pos);
				onLongClick(amenity);
				return true;
			}
		});
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		Bundle bundle = this.getIntent().getExtras();
		if(bundle.containsKey(SEARCH_LAT) && bundle.containsKey(SEARCH_LON)){
			location = new Location("internal"); //$NON-NLS-1$
			location.setLatitude(bundle.getDouble(SEARCH_LAT));
			location.setLongitude(bundle.getDouble(SEARCH_LON));
			searchNearBy = false;
		} else {
			location = null;
			searchNearBy = true;
		}
		
		String filterId = bundle.getString(AMENITY_FILTER);
		PoiFilter filter = ((OsmandApplication)getApplication()).getPoiFilters().getFilterById(filterId);
		if (filter != this.filter) {
			this.filter = filter;
			if (filter != null) {
				filter.clearPreviousZoom();
			} else {
				amenityAdapter.setNewModel(Collections.<Amenity> emptyList(), "");
				searchPOILevel.setText(R.string.search_POI_level_btn);
				searchPOILevel.setEnabled(false);
			}
			// run query again
			clearSearchQuery();
		}
		if(filter != null) {
			filter.clearFilter();
		}
		
		if(isNameFinderFilter()){
			showFilter.setVisibility(View.GONE);
			searchFilterLayout.setVisibility(View.VISIBLE);
		} else if(isSearchByNameFilter() ){
			showFilter.setVisibility(View.INVISIBLE);
			searchFilterLayout.setVisibility(View.VISIBLE);
		} else {
			showFilter.setVisibility(View.VISIBLE);
			showOnMap.setEnabled(filter != null);
		}
		showOnMap.setVisibility(isSearchByNameFilter() ? View.GONE : View.VISIBLE);
		
		if (filter != null) {
			searchArea.setText(filter.getSearchArea());
			updateSearchPoiTextButton();
			
			setLocation(location);
			if (searchNearBy) {
				LocationManager service = (LocationManager) getSystemService(LOCATION_SERVICE);
				service.requestLocationUpdates(LocationManager.GPS_PROVIDER, GPS_TIMEOUT_REQUEST, GPS_DIST_REQUEST, gpsListener);
				currentLocationProvider = LocationManager.GPS_PROVIDER;
				if (!isRunningOnEmulator()) {
					// try to always ask for network provide it is faster way to find location
					service.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, GPS_TIMEOUT_REQUEST, GPS_DIST_REQUEST,
									networkListener);
					currentLocationProvider = LocationManager.NETWORK_PROVIDER;
				}
			}
		}
	}
	
	private void showPoiCategoriesByNameFilter(String query, Location loc){
		OsmandApplication app = (OsmandApplication) getApplication();
		if(loc != null){
			Map<AmenityType, List<String>> map = app.getResourceManager().searchAmenityCategoriesByName(query, loc.getLatitude(), loc.getLongitude());
			if(!map.isEmpty()){
				PoiFilter filter = ((OsmandApplication)getApplication()).getPoiFilters().getFilterById(PoiFilter.CUSTOM_FILTER_ID);
				if(filter != null){
					showFilter.setVisibility(View.VISIBLE);
					filter.setMapToAccept(map);
				}
				
				String s = typesToString(map);
				Toast.makeText(this, getString(R.string.poi_query_by_name_matches_categories) + s, Toast.LENGTH_LONG).show();
			}
		}
	}
	
	private String typesToString(Map<AmenityType, List<String>> map) {
		StringBuilder b = new StringBuilder();
		int count = 0;
		Iterator<Entry<AmenityType, List<String>>> iterator = map.entrySet().iterator();
		while(iterator.hasNext() && count < 4){
			Entry<AmenityType, List<String>> e = iterator.next();
			b.append("\n").append(OsmAndFormatter.toPublicString(e.getKey(), this)).append(" - ");
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

	private void updateSearchPoiTextButton(){
		if(location == null){
			searchPOILevel.setText(R.string.search_poi_location);
			searchPOILevel.setEnabled(false);
		} else if (!isNameFinderFilter() && !isSearchByNameFilter()) {
			searchPOILevel.setText(R.string.search_POI_level_btn);
			searchPOILevel.setEnabled(currentSearchTask.getStatus() != Status.RUNNING &&
					filter.isSearchFurtherAvailable());
		} else {
			searchPOILevel.setText(R.string.search_button);
			searchPOILevel.setEnabled(currentSearchTask.getStatus() != Status.RUNNING && 
					filter.isSearchFurtherAvailable());
		}
	}
	
	public void setLocation(Location l){
		registerUnregisterSensor(l);
		boolean handled = false;
		if (l != null && filter != null) {
			Location searchedLocation = getSearchedLocation();
			if (searchedLocation == null) {
  				searchedLocation = l;
				if (!isNameFinderFilter() && !isSearchByNameFilter()) {
					runNewSearchQuery(SearchAmenityRequest.buildRequest(l, SearchAmenityRequest.NEW_SEARCH_INIT));
				}
				handled = true;
			} else if (l.distanceTo(searchedLocation) > MIN_DISTANCE_TO_RESEARCH) {
				runNewSearchQuery(SearchAmenityRequest.buildRequest(l, SearchAmenityRequest.SEARCH_AGAIN));
				handled = true;
			} else if(location == null || location.distanceTo(l) > MIN_DISTANCE_TO_UPDATE){
				handled = true;
			}
		} else {
			if(location != null){
				handled = true;
			}
		}
		if(handled) {
			location = l;
			updateSearchPoiTextButton();
			amenityAdapter.notifyDataSetInvalidated();
		}
		
	}
	
	private Location getSearchedLocation(){
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
	
	
	private void onLongClick(final Amenity amenity) {
		String format = OsmAndFormatter.getPoiSimpleFormat(amenity, SearchPOIActivity.this, settings.USE_ENGLISH_NAMES.get());
		if (amenity.getOpeningHours() != null) {
			Toast.makeText(this, format + "  " + getString(R.string.opening_hours) + " : " + amenity.getOpeningHours(), Toast.LENGTH_LONG).show();
		}
		
		AlertDialog.Builder builder = new AlertDialog.Builder(SearchPOIActivity.this);
		builder.setTitle(format);
		builder.setItems(new String[]{getString(R.string.show_poi_on_map), getString(R.string.navigate_to)}, new DialogInterface.OnClickListener(){

			@Override
			public void onClick(DialogInterface dialog, int which) {
				if(which == 0){
					int z = settings.getLastKnownMapZoom();
					String poiSimpleFormat = OsmAndFormatter.getPoiSimpleFormat(amenity, SearchPOIActivity.this, settings.usingEnglishNames());
					String name = getString(R.string.poi)+" : " + poiSimpleFormat;
					settings.setMapLocationToShow( 
							amenity.getLocation().getLatitude(), amenity.getLocation().getLongitude(), 
							Math.max(16, z), name, name, amenity);
				} else if(which == 1){
					LatLon l = amenity.getLocation();
					String poiSimpleFormat = OsmAndFormatter.getPoiSimpleFormat(amenity, SearchPOIActivity.this, settings.usingEnglishNames());
					settings.setPointToNavigate(l.getLatitude(), l.getLongitude(), getString(R.string.poi)+" : " + poiSimpleFormat);
				}
				if(filter != null){
					settings.setPoiFilterForMap(filter.getFilterId());
					settings.SHOW_POI_OVER_MAP.set(true);
				}
				
				MapActivity.launchMapActivityMoveToTop(SearchPOIActivity.this);
				
			}
			
		});
		builder.show();
	}
	
	private boolean isRunningOnEmulator(){
		if (Build.DEVICE.equals("generic")) { //$NON-NLS-1$ 
			return true;
		}  
		return false;
	}
	
	public boolean isNameFinderFilter(){
		return filter instanceof NameFinderPoiFilter; 
	}
	
	public boolean isSearchByNameFilter(){
		return filter != null && PoiFilter.BY_NAME_FILTER_ID.equals(filter.getFilterId()); 
	}
	

	@Override
	public void onSensorChanged(SensorEvent event) {
		// Attention : sensor produces a lot of events & can hang the system
		if(heading != null && Math.abs(heading - event.values[0]) < 4){
			// this is very small variation
			return;
		}
		heading = event.values[0];
		
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
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	
	
	
	private void registerUnregisterSensor(Location location){
    	// show point view only if gps enabled
    	if(location == null){
    		if(sensorRegistered) {
    			Log.d(LogUtil.TAG, "Disable sensor"); //$NON-NLS-1$
    			((SensorManager) getSystemService(SENSOR_SERVICE)).unregisterListener(this);
    			sensorRegistered = false;
    			heading = null;
    		}
    	} else {
    		if(!sensorRegistered){
    			Log.d(LogUtil.TAG, "Enable sensor"); //$NON-NLS-1$
    			SensorManager sensorMgr = (SensorManager) getSystemService(SENSOR_SERVICE);
    			Sensor s = sensorMgr.getDefaultSensor(Sensor.TYPE_ORIENTATION);
    			if (s != null) {
    				sensorMgr.registerListener(this, s, SensorManager.SENSOR_DELAY_UI);
    			}
    			sensorRegistered = true;
    		}
    	}
    }
	
	@Override
	protected void onPause() {
		super.onPause();
		if (searchNearBy) {
			LocationManager service = (LocationManager) getSystemService(LOCATION_SERVICE);
			service.removeUpdates(gpsListener);
			service.removeUpdates(networkListener);

			SensorManager sensorMgr = (SensorManager) getSystemService(SENSOR_SERVICE);
			sensorMgr.unregisterListener(this);
			sensorRegistered = false;
			currentLocationProvider = null;
		}
	}

	@Override
	public void onListItemClick(ListView parent, View v, int position, long id) {
		if(filter != null){
			settings.setPoiFilterForMap(filter.getFilterId());
			settings.SHOW_POI_OVER_MAP.set(true);
		}
		int z = settings.getLastKnownMapZoom();
		Amenity amenity = ((AmenityAdapter) getListAdapter()).getItem(position);
		String poiSimpleFormat = OsmAndFormatter.getPoiSimpleFormat(amenity, this, settings.usingEnglishNames());
		String name = getString(R.string.poi)+" : " + poiSimpleFormat;
		settings.setMapLocationToShow( amenity.getLocation().getLatitude(), amenity.getLocation().getLongitude(), 
				Math.max(16, z), name, name, amenity); //$NON-NLS-1$
		MapActivity.launchMapActivityMoveToTop(SearchPOIActivity.this);
	}
	
	
	static class SearchAmenityRequest {
		private static final int SEARCH_AGAIN = 1;
		private static final int NEW_SEARCH_INIT = 2;
		private static final int SEARCH_FURTHER = 3;
		private int type;
		private Location location;
		
		public static SearchAmenityRequest buildRequest(Location l, int type){
			SearchAmenityRequest req = new SearchAmenityRequest();
			req.type = type;
			req.location = l;
			return req;
			
		}
	}
	
	class SearchAmenityTask extends AsyncTask<Void, Amenity, List<Amenity>> implements ResultMatcher<Amenity> {
		
		private SearchAmenityRequest request;
		private TLongHashSet existingObjects = null;
		private final static int LIMIT_TO_LIVE_SEARCH = 150;
		
		public SearchAmenityTask(SearchAmenityRequest request){
			this.request = request;
			
		}
		
		Location getSearchedLocation(){
			return request != null ? request.location : null; 
		}

		@Override
		protected void onPreExecute() {
			findViewById(R.id.ProgressBar).setVisibility(View.VISIBLE);
			findViewById(R.id.SearchAreaText).setVisibility(View.GONE);
			searchPOILevel.setEnabled(false);
			if(request.type == SearchAmenityRequest.NEW_SEARCH_INIT){
				amenityAdapter.clear();
			} else if (request.type == SearchAmenityRequest.SEARCH_FURTHER) {
				List<Amenity> list = amenityAdapter.getOriginalAmenityList();
				if (list.size() < LIMIT_TO_LIVE_SEARCH) {
					existingObjects = new TLongHashSet();
					for (Amenity a : list) {
						existingObjects.add(a.getId());
					}
				}
			}
		}
		
		@Override
		protected void onPostExecute(List<Amenity> result) {
			findViewById(R.id.ProgressBar).setVisibility(View.GONE);
			findViewById(R.id.SearchAreaText).setVisibility(View.VISIBLE);
			searchPOILevel.setEnabled(filter.isSearchFurtherAvailable());
			searchPOILevel.setText(R.string.search_POI_level_btn);
			if (isNameFinderFilter()) {
				if (!Algoritms.isEmpty(((NameFinderPoiFilter) filter).getLastError())) {
					Toast.makeText(SearchPOIActivity.this, ((NameFinderPoiFilter) filter).getLastError(), Toast.LENGTH_LONG).show();
				}
				amenityAdapter.setNewModel(result, "");
				showOnMap.setEnabled(amenityAdapter.getCount() > 0);
			} else if (isSearchByNameFilter()) {
				amenityAdapter.setNewModel(result, "");
			} else {
				amenityAdapter.setNewModel(result, searchFilter.getText().toString());
			}
			searchArea.setText(filter.getSearchArea());
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
			if(request.type == SearchAmenityRequest.NEW_SEARCH_INIT){
				publishProgress(object);
			} else if (request.type == SearchAmenityRequest.SEARCH_FURTHER) {
				if(existingObjects != null && !existingObjects.contains(object.getId())){
					publishProgress(object);
				}
			}
			
			return true;
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
			TextView distanceLabel = (TextView) row.findViewById(R.id.poidistance_label);
			ImageView icon = (ImageView) row.findViewById(R.id.poi_icon);
			Amenity amenity = getItem(position);
			Location loc = location;
			if(loc != null){
				mes = new float[2];
				LatLon l = amenity.getLocation();
				Location.distanceBetween(l.getLatitude(), l.getLongitude(), loc.getLatitude(), loc.getLongitude(), mes);
			}
			String str = OsmAndFormatter.getPoiStringWithoutType(amenity, settings.usingEnglishNames());
			label.setText(str);
			int opened = -1;
			if (amenity.getOpeningHours() != null) {
				List<OpeningHoursRule> rs = OpeningHoursParser.parseOpenedHours(amenity.getOpeningHours());
				if (rs != null) {
					Calendar inst = Calendar.getInstance();
					inst.setTimeInMillis(System.currentTimeMillis());
					boolean work = false;
					for (OpeningHoursRule p : rs) {
						if (p.isOpenedForTime(inst)) {
							work = true;
							break;
						}
					}
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
				icon.setImageDrawable(draw);
			} else {
				if(opened == -1){
					icon.setImageResource(R.drawable.poi);
				} else if(opened == 0){
					icon.setImageResource(R.drawable.opened_poi);
				} else {
					icon.setImageResource(R.drawable.closed_poi);
				}
			}

			if(mes == null){
				distanceLabel.setText(""); //$NON-NLS-1$
			} else {
				distanceLabel.setText(" " + OsmAndFormatter.getFormattedDistance((int) mes[0], SearchPOIActivity.this)); //$NON-NLS-1$
			}
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

	
	// Working with location listeners
	private LocationListener networkListener = new LocationListener(){
		@Override
		public void onLocationChanged(Location location) {
			setLocation(location);
		}

		@Override
		public void onProviderDisabled(String provider) {
			setLocation(null);
		}

		@Override
		public void onProviderEnabled(String provider) {
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			if(LocationProvider.OUT_OF_SERVICE == status){
				setLocation(null);
			}
		}
	};
	private LocationListener gpsListener = new LocationListener(){
		@Override
		public void onLocationChanged(Location location) {
			setLocation(location);
		}

		@Override
		public void onProviderDisabled(String provider) {
			setLocation(null);
		}

		@Override
		public void onProviderEnabled(String provider) {
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			LocationManager service = (LocationManager) getSystemService(LOCATION_SERVICE);
			// do not change provider for temporarily unavailable (possible bug for htc hero 2.1 ?)
			if (LocationProvider.OUT_OF_SERVICE == status /*|| LocationProvider.TEMPORARILY_UNAVAILABLE == status*/) {
				if(LocationProvider.OUT_OF_SERVICE == status){
					setLocation(null);
				}
				if (!isRunningOnEmulator() && service.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
					if (!Algoritms.objectEquals(currentLocationProvider, LocationManager.NETWORK_PROVIDER)) {
						currentLocationProvider = LocationManager.NETWORK_PROVIDER;
						service.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, GPS_TIMEOUT_REQUEST, GPS_DIST_REQUEST, this);
					}
				}
			} else if (LocationProvider.AVAILABLE == status) {
				if (!Algoritms.objectEquals(currentLocationProvider, LocationManager.GPS_PROVIDER)) {
					currentLocationProvider = LocationManager.GPS_PROVIDER;
					service.removeUpdates(networkListener);
				}
			}
		}
	};
	
	static class DirectionDrawable extends Drawable {
		Paint paintRouteDirection;
		Path path = new Path();
		private float angle;
		
		private final int width = 24;
		private final int height = 24;
		
		public DirectionDrawable(){
			paintRouteDirection = new Paint();
			paintRouteDirection.setStyle(Style.FILL_AND_STROKE);
			paintRouteDirection.setColor(Color.rgb(100, 0, 255));
			paintRouteDirection.setAntiAlias(true);
			
			int h = 15;
			int w = 4;
			float sarrowL = 8; // side of arrow
			float harrowL = (float) Math.sqrt(2) * sarrowL; // hypotenuse of arrow
			float hpartArrowL = (float) (harrowL - w) / 2;
			
			path.moveTo(width / 2, height - (height - h) / 3);
			path.rMoveTo(w / 2, 0);
			path.rLineTo(0, -h);
			path.rLineTo(hpartArrowL, 0);
			path.rLineTo(-harrowL / 2, -harrowL / 2); // center
			path.rLineTo(-harrowL / 2, harrowL / 2);
			path.rLineTo(hpartArrowL, 0);
			path.rLineTo(0, h);
		}
		
		public void setOpenedColor(int opened){
			if(opened == 0){
				paintRouteDirection.setColor(Color.rgb(0, 205, 0));
			} else if(opened == -1){
				paintRouteDirection.setColor(Color.rgb(150, 150, 150));
			} else {
				paintRouteDirection.setColor(Color.rgb(238, 0, 0));
			}
		}
		
		
		public void setAngle(float angle){
			this.angle = angle;
		}

		@Override
		public void draw(Canvas canvas) {
			canvas.rotate(angle, width/2, height/2);
			canvas.drawPath(path, paintRouteDirection);
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
