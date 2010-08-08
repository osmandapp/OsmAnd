/**
 * 
 */
package com.osmand.activities.search;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.app.ListActivity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
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
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.osmand.Algoritms;
import com.osmand.LogUtil;
import com.osmand.NameFinderPoiFilter;
import com.osmand.OsmandSettings;
import com.osmand.PoiFilter;
import com.osmand.PoiFiltersHelper;
import com.osmand.R;
import com.osmand.activities.MapActivity;
import com.osmand.data.Amenity;
import com.osmand.osm.LatLon;
import com.osmand.osm.MapUtils;
import com.osmand.osm.OpeningHoursParser;
import com.osmand.osm.OpeningHoursParser.OpeningHoursRule;

/**
 * @author Maxim Frolov
 * 
 */
public class SearchPOIActivity extends ListActivity implements SensorEventListener {

	public static final String AMENITY_FILTER = "com.osmand.amenity_filter"; //$NON-NLS-1$
	public static final String SEARCH_LAT = "com.osmand.am_search_lat"; //$NON-NLS-1$
	public static final String SEARCH_LON = "com.osmand.am_search_lon"; //$NON-NLS-1$
	private static final int GPS_TIMEOUT_REQUEST = 1000;
	private static final int GPS_DIST_REQUEST = 5;
	private static final int MIN_DISTANCE_TO_RESEARCH = 70;
	private static final int MIN_DISTANCE_TO_UPDATE = 6;


	private Button searchPOILevel;
	private Button showOnMap;
	private PoiFilter filter;
	private AmenityAdapter amenityAdapter;
	private TextView searchArea;
	private EditText searchFilter;
	private View searchFilterLayout;
	
	private boolean searchNearBy = false;
	private Location location = null; 
	private Location searchedLocation = null;
	private Float heading = null;
	
	private String currentLocationProvider = null;
	private boolean sensorRegistered = false;
	private Handler uiHandler;
	

	
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		setContentView(R.layout.searchpoi);
		Bundle bundle = this.getIntent().getExtras();
		String filterId = bundle.getString(AMENITY_FILTER);
		filter = PoiFiltersHelper.getFilterById(this, filterId);
		
		uiHandler = new Handler();
		searchPOILevel = (Button) findViewById(R.id.SearchPOILevelButton);
		searchArea = (TextView) findViewById(R.id.SearchAreaText);
		searchFilter = (EditText) findViewById(R.id.SearchFilter);
		searchFilterLayout = findViewById(R.id.SearchFilterLayout);
		showOnMap = (Button) findViewById(R.id.ShowOnMap);
		
		searchPOILevel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(isNameFinderFilter()){
					String query = searchFilter.getText().toString();
					if(query.length() == 0){
						Toast.makeText(SearchPOIActivity.this, R.string.poi_namefinder_query_empty, Toast.LENGTH_LONG).show();
						return;
					}
					String res = ((NameFinderPoiFilter) filter).searchOnline(location.getLatitude(), location.getLongitude(), query);
					if(res != null){
						Toast.makeText(SearchPOIActivity.this, res, Toast.LENGTH_LONG).show();
					}
					amenityAdapter.setNewModel(((NameFinderPoiFilter) filter).getSearchedAmenities());
					showOnMap.setEnabled(amenityAdapter.getCount() > 0);
				} else {
					amenityAdapter.setNewModel(filter.searchFurther(location.getLatitude(), location.getLongitude()));
				}
				
				searchedLocation = location;
				searchArea.setText(filter.getSearchArea());
				searchPOILevel.setEnabled(filter.isSearchFurtherAvailable());

			}
		});
		if(isNameFinderFilter()){
			searchFilterLayout.setVisibility(View.VISIBLE);
		}
		searchFilter.addTextChangedListener(new TextWatcher(){

			@Override
			public void afterTextChanged(Editable s) {
				if(!isNameFinderFilter()){
					amenityAdapter.getFilter().filter(s);
				}
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}
			
		});

		
		if(bundle.containsKey(SEARCH_LAT) && bundle.containsKey(SEARCH_LON)){
			location = new Location("internal"); //$NON-NLS-1$
			location.setLatitude(bundle.getDouble(SEARCH_LAT));
			location.setLongitude(bundle.getDouble(SEARCH_LON));
			searchNearBy = false;
		} else {
			location = null;
			searchNearBy = true;
		}
		
		if(isNameFinderFilter()){
			showOnMap.setEnabled(false);
		} else {
			showOnMap.setEnabled(filter != null);
		}
		
		showOnMap.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				OsmandSettings.setPoiFilterForMap(SearchPOIActivity.this, filter.getFilterId());
				OsmandSettings.setShowPoiOverMap(SearchPOIActivity.this, true);
				if(/*searchNearBy && */location != null){
					OsmandSettings.setMapLocationToShow(SearchPOIActivity.this, location.getLatitude(), location.getLongitude(), 15);
				}
				Intent newIntent = new Intent(SearchPOIActivity.this, MapActivity.class);
				startActivity(newIntent);
			}
		});
		
		if (filter != null) {
			amenityAdapter = new AmenityAdapter(new ArrayList<Amenity>());
			if(location == null){
				filter.clearPreviousZoom();
			} else if(!isNameFinderFilter()) {
				searchedLocation = location;
				amenityAdapter.setNewModel(filter.initializeNewSearch(location.getLatitude(), location.getLongitude(), 40));
			}
			setListAdapter(amenityAdapter);
			searchPOILevel.setEnabled(filter.isSearchFurtherAvailable());
			searchArea.setText(filter.getSearchArea());
		} else {
			searchPOILevel.setEnabled(false);
		}
		// ListActivity has a ListView, which you can get with:
		ListView lv = getListView();

		// Then you can create a listener like so:
		lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> av, View v, int pos, long id) {
				Amenity amenity = ((AmenityAdapter) getListAdapter()).getItem(pos);
				String format = amenity.getSimpleFormat(OsmandSettings.usingEnglishNames(v.getContext()));
				if (amenity.getOpeningHours() != null) {
					format += "\n"+getString(R.string.opening_hours) + " : " + amenity.getOpeningHours(); //$NON-NLS-1$ //$NON-NLS-2$
				}
				Toast.makeText(v.getContext(), format, Toast.LENGTH_LONG).show();
				return true;
			}
		});
	}
	
	public void setLocation(Location l){
		registerUnregisterSensor(l);
		boolean handled = false;
		if (l != null && filter != null) {
			if (location == null) {
				searchedLocation = l;
				if (!isNameFinderFilter()) {
					amenityAdapter.setNewModel(filter.searchAgain(l.getLatitude(), l.getLongitude()));
					searchPOILevel.setText(R.string.search_POI_level_btn);
				} else {
					searchPOILevel.setText(R.string.search_button);
				}
				searchPOILevel.setEnabled(filter.isSearchFurtherAvailable());
				searchArea.setText(filter.getSearchArea());
				handled = true;
			} else if (searchedLocation != null && l.distanceTo(searchedLocation) > MIN_DISTANCE_TO_RESEARCH) {
				amenityAdapter.setNewModel(filter.searchAgain(l.getLatitude(), l.getLongitude()));
				handled = true;
			} else if(location.distanceTo(l) > MIN_DISTANCE_TO_UPDATE){
				handled = true;
			}
		} else {
			if(location != null){
				searchPOILevel.setText(R.string.search_poi_location);
				searchPOILevel.setEnabled(false);
				handled = true;
			}
		}
		
		if(handled) {
			location = l;
			amenityAdapter.notifyDataSetChanged();
		}
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
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean m = super.onCreateOptionsMenu(menu);
		if (!isNameFinderFilter()) {
			final MenuItem me = menu.add(R.string.show_poi_filter);
			me.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {

				@Override
				public boolean onMenuItemClick(MenuItem item) {
					if (searchFilterLayout.getVisibility() == View.GONE) {
						searchFilterLayout.setVisibility(View.VISIBLE);
						me.setTitle(R.string.hide_poi_filter);
					} else {
						searchFilter.setText(""); //$NON-NLS-1$
						searchFilterLayout.setVisibility(View.GONE);
						me.setTitle(R.string.show_poi_filter);
					}
					return true;
				}

			});
		}
		return m;
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

	
	@Override
	protected void onResume() {
		super.onResume();
		if(searchNearBy){
			location = null;
			amenityAdapter.notifyDataSetChanged();
			searchPOILevel.setEnabled(false);
		} else {
			setLocation(location);
		}
		if(searchNearBy && location == null){
			searchPOILevel.setText(R.string.search_poi_location);
		} else if(isNameFinderFilter()){
			searchPOILevel.setText(R.string.search_button);
		} else {
			searchPOILevel.setText(R.string.search_POI_level_btn);
		}
		if (searchNearBy) {
			LocationManager service = (LocationManager) getSystemService(LOCATION_SERVICE);
			service.requestLocationUpdates(LocationManager.GPS_PROVIDER, GPS_TIMEOUT_REQUEST, GPS_DIST_REQUEST, gpsListener);
			currentLocationProvider = LocationManager.GPS_PROVIDER;
			if(!isRunningOnEmulator()){
				// try to always  ask for network provide it is faster way to find location
				service.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, GPS_TIMEOUT_REQUEST, GPS_DIST_REQUEST, networkListener);
				currentLocationProvider = LocationManager.NETWORK_PROVIDER;
			} 
		}
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

	public void onListItemClick(ListView parent, View v, int position, long id) {
		if(filter != null){
			OsmandSettings.setPoiFilterForMap(SearchPOIActivity.this, filter.getFilterId());
			OsmandSettings.setShowPoiOverMap(SearchPOIActivity.this, true);
		}
		int z = OsmandSettings.getLastKnownMapZoom(this);
		Amenity amenity = ((AmenityAdapter) getListAdapter()).getItem(position);
		OsmandSettings.setMapLocationToShow(this, amenity.getLocation().getLatitude(), amenity.getLocation().getLongitude(), 
				Math.max(16, z), getString(R.string.poi)+" : " + amenity.getSimpleFormat(OsmandSettings.usingEnglishNames(this))); //$NON-NLS-1$
		Intent newIntent = new Intent(SearchPOIActivity.this, MapActivity.class);
		startActivity(newIntent);
	}
	
	class DirectionDrawable extends Drawable {
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
				paintRouteDirection.setColor(Color.rgb(100, 0, 255));
			} else if(opened == -1){
				paintRouteDirection.setColor(Color.rgb(150, 150, 150));
			} else {
				paintRouteDirection.setColor(Color.rgb(220, 100, 80));
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

	class AmenityAdapter extends ArrayAdapter<Amenity> {
		AmenityAdapter(List<Amenity> list) {
			super(SearchPOIActivity.this, R.layout.searchpoi_list, list);
			this.setNotifyOnChange(false);
		}

		public void setNewModel(List<Amenity> amenityList) {
			setNotifyOnChange(false);
			clear();
			for (Amenity obj : amenityList) {
				this.add(obj);
			}
			setNotifyOnChange(true);
			this.notifyDataSetChanged();

		}

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
			if(location != null){
				mes = new float[2];
				LatLon l = amenity.getLocation();
				Location.distanceBetween(l.getLatitude(), l.getLongitude(), location.getLatitude(), location.getLongitude(), mes);
			}
			
			String str = amenity.getStringWithoutType(OsmandSettings.usingEnglishNames(SearchPOIActivity.this));
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
			if(location != null){
				DirectionDrawable draw = new DirectionDrawable();
				float a = heading != null ? heading : 0;
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
				distanceLabel.setText(" " + MapUtils.getFormattedDistance((int) mes[0])); //$NON-NLS-1$
			}
			return (row);
		}
	}


}
