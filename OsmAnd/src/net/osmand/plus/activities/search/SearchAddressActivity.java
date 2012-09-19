package net.osmand.plus.activities.search;


import java.text.MessageFormat;

import net.osmand.Algoritms;
import net.osmand.osm.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.RegionAddressRepository;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.MapActivityActions;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

public class SearchAddressActivity extends Activity {

	public static final String SELECT_ADDRESS_POINT_INTENT_KEY = "SELECT_ADDRESS_POINT_INTENT_KEY";
	public static final int SELECT_ADDRESS_POINT_RESULT_OK = 1;	
	public static final String SELECT_ADDRESS_POINT_LAT = "SELECT_ADDRESS_POINT_LAT";
	public static final String SELECT_ADDRESS_POINT_LON = "SELECT_ADDRESS_POINT_LON";
	
	private Button showOnMap;
	private Button streetButton;
	private Button cityButton;
	private Button countryButton;
	private Button buildingButton;
	private Button navigateTo;
	
	private String region = null;
	private String city = null;
	private String postcode = null;
	private String street = null;
	private String building = null;
	private String street2 = null;
	private boolean radioBuilding = true;
	private Button searchOnline;
	
	private OsmandSettings osmandSettings;
	private LatLon searchPoint = null;

	private boolean selectAddressMode;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.search_address);
		
		showOnMap = (Button) findViewById(R.id.ShowOnMap);
		navigateTo = (Button) findViewById(R.id.NavigateTo);
		streetButton = (Button) findViewById(R.id.StreetButton);
		cityButton = (Button) findViewById(R.id.CityButton);
		countryButton = (Button) findViewById(R.id.CountryButton);
		buildingButton = (Button) findViewById(R.id.BuildingButton);
		searchOnline = (Button) findViewById(R.id.SearchOnline);
		osmandSettings = ((OsmandApplication) getApplication()).getSettings();
		attachListeners();
	}
	
	private Intent createIntent(Class<?> cl){
		LatLon location = null;
		Intent intent = getIntent();
		if(intent != null){
			double lat = intent.getDoubleExtra(SearchActivity.SEARCH_LAT, 0);
			double lon = intent.getDoubleExtra(SearchActivity.SEARCH_LON, 0);
			if(lat != 0 || lon != 0){
				location = new LatLon(lat, lon);
			}
		}
		if (location == null && getParent() instanceof SearchActivity) {
			location = ((SearchActivity) getParent()).getSearchPoint();
		}
		Intent newIntent = new Intent(SearchAddressActivity.this, cl);
		if (location != null) {
			newIntent.putExtra(SearchActivity.SEARCH_LAT, location.getLatitude());
			newIntent.putExtra(SearchActivity.SEARCH_LON, location.getLongitude());
		}
		return newIntent;
	}
	
	private void attachListeners() {
		if (getParent() instanceof SearchActivity) {
			searchOnline.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					((SearchActivity) getParent()).startSearchAddressOnline();
				}
			});
		} else {
			searchOnline.setVisibility(View.INVISIBLE);
		}
		countryButton.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v) {
				startActivity(createIntent(SearchRegionByNameActivity.class));
			}
		});
		cityButton.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v) {
				startActivity(createIntent(SearchCityByNameActivity.class));
			}
		});
		streetButton.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v) {
				startActivity(createIntent(SearchStreetByNameActivity.class));
			}
		});
		buildingButton.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v) {
				if(radioBuilding){
					startActivity(createIntent(SearchBuildingByNameActivity.class));
				} else {
					startActivity(createIntent(SearchStreet2ByNameActivity.class));
				}
			}
		});
		navigateTo.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showOnMap(true);
			}
		});
		showOnMap.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showOnMap(false);
			}
		});
		findViewById(R.id.ResetBuilding).setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v) {
				building = null;
				searchPoint = null;
				updateUI();
			}
		 });
		 findViewById(R.id.ResetStreet).setOnClickListener(new View.OnClickListener(){
				@Override
				public void onClick(View v) {
					street = null;
					building = null;
					street2 = null;
					searchPoint = null;
					updateUI();
				}
		 });
		 findViewById(R.id.ResetCity).setOnClickListener(new View.OnClickListener(){
				@Override
				public void onClick(View v) {
					postcode = null;
					city = null;
					street = null;
					street2 = null;
					building = null;
					searchPoint = null;
					updateUI();
				}
		 });
		 findViewById(R.id.ResetCountry).setOnClickListener(new View.OnClickListener(){
				@Override
				public void onClick(View v) {
					region = null;
					postcode = null;
					city = null;
					street = null;
					street2 = null;
					building = null;
					searchPoint = null;
					updateUI();
				}
		 });
		 ((RadioGroup)findViewById(R.id.RadioGroup)).setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){

				@Override
				public void onCheckedChanged(RadioGroup group, int checkedId) {
					SearchAddressActivity.this.radioBuilding = checkedId == R.id.RadioBuilding;
					if(radioBuilding){
						SearchAddressActivity.this.street2 = null;
					} else {
						SearchAddressActivity.this.building = null;
					}
					updateBuildingSection();
				}
				
			});
	}
	
	public void showOnMap(boolean navigateTo) {
		if (searchPoint == null) {
			return;
		}
		String historyName = null;
		String objectName = "";
		int zoom = 12;
		if (!Algoritms.isEmpty(street2) && !Algoritms.isEmpty(street)) {
			String cityName = !Algoritms.isEmpty(postcode) ? postcode : city;
			objectName = street;
			historyName = MessageFormat.format(getString(R.string.search_history_int_streets), street, street2,
					cityName);
			zoom = 16;
		} else if (!Algoritms.isEmpty(building)) {
			String cityName = !Algoritms.isEmpty(postcode) ? postcode : city;
			objectName = street + " " + building;
			historyName = MessageFormat.format(getString(R.string.search_history_building), building, street,
					cityName);
			zoom = 16;
		} else if (!Algoritms.isEmpty(street)) {
			String cityName = postcode != null ? postcode : city;
			objectName = street;
			historyName = MessageFormat.format(getString(R.string.search_history_street), street, cityName);
			zoom = 15;
		} else if (!Algoritms.isEmpty(city)) {
			historyName = MessageFormat.format(getString(R.string.search_history_city), city);
			objectName = city;
			zoom = 13;
		}
		if(selectAddressMode){
			Intent intent = getIntent();
			intent.putExtra(SELECT_ADDRESS_POINT_INTENT_KEY, objectName);
			intent.putExtra(SELECT_ADDRESS_POINT_LAT, searchPoint.getLatitude());
			intent.putExtra(SELECT_ADDRESS_POINT_LON, searchPoint.getLongitude());
			setResult(SELECT_ADDRESS_POINT_RESULT_OK, intent);
			finish();
		} else {
			if (navigateTo) {
				MapActivityActions.navigateToPoint(SearchAddressActivity.this, searchPoint.getLatitude(), searchPoint.getLongitude(), historyName);
			} else {
				osmandSettings.setMapLocationToShow(searchPoint.getLatitude(), searchPoint.getLongitude(), zoom, historyName);
				MapActivity.launchMapActivityMoveToTop(SearchAddressActivity.this);
			}
			
		}
	}
	
	
	protected void updateBuildingSection(){
		if(radioBuilding){
			((TextView)findViewById(R.id.BuildingText)).setText(R.string.search_address_building);
			if(Algoritms.isEmpty(building)){
				((TextView)findViewById(R.id.BuildingButton)).setText(R.string.choose_building);
			} else {
				((TextView)findViewById(R.id.BuildingButton)).setText(building);
			}
		} else {
			((TextView)findViewById(R.id.BuildingText)).setText(R.string.search_address_street);
			if(Algoritms.isEmpty(street2)){
				((TextView)findViewById(R.id.BuildingButton)).setText(R.string.choose_intersected_street);
			} else {
				((TextView)findViewById(R.id.BuildingButton)).setText(street2);
			}
		}
		findViewById(R.id.ResetBuilding).setEnabled(!Algoritms.isEmpty(street2) || !Algoritms.isEmpty(building));
	}

	protected void updateUI(){
		showOnMap.setEnabled(searchPoint != null);
		navigateTo.setEnabled(searchPoint != null);
		if(selectAddressMode) {
			navigateTo.setText(R.string.search_select_point);
			showOnMap.setVisibility(View.INVISIBLE);
			findViewById(R.id.SearchOnline).setVisibility(View.INVISIBLE);
		} else {
			navigateTo.setText(R.string.navigate_to);
			findViewById(R.id.SearchOnline).setVisibility(View.VISIBLE);
			showOnMap.setVisibility(View.VISIBLE);
		}
		findViewById(R.id.ResetCountry).setEnabled(!Algoritms.isEmpty(region));
		if(Algoritms.isEmpty(region)){
			countryButton.setText(R.string.ChooseCountry);
		} else {
			countryButton.setText(region);
		}
		findViewById(R.id.ResetCity).setEnabled(!Algoritms.isEmpty(city) || !Algoritms.isEmpty(postcode));
		if(Algoritms.isEmpty(city) && Algoritms.isEmpty(postcode)){
			cityButton.setText(R.string.choose_city);
		} else {
			if(!Algoritms.isEmpty(postcode)){
				cityButton.setText(postcode);
			} else {
				cityButton.setText(city);
			}
		}
		cityButton.setEnabled(!Algoritms.isEmpty(region));
		
		findViewById(R.id.ResetStreet).setEnabled(!Algoritms.isEmpty(street));
		if(Algoritms.isEmpty(street)){
			streetButton.setText(R.string.choose_street);
		} else {
			streetButton.setText(street);
		}
		streetButton.setEnabled(!Algoritms.isEmpty(city) || !Algoritms.isEmpty(postcode));
		
		buildingButton.setEnabled(!Algoritms.isEmpty(street));
		((RadioGroup)findViewById(R.id.RadioGroup)).setVisibility(Algoritms.isEmpty(street) ? View.GONE : View.VISIBLE);
		
		if(radioBuilding){
			((RadioButton)findViewById(R.id.RadioBuilding)).setChecked(true);
		} else {
			((RadioButton)findViewById(R.id.RadioIntersStreet)).setChecked(true);
		}
		updateBuildingSection();
		
	}
	
	public void loadData() {
		if (!Algoritms.isEmpty(region)) {
			String postcodeStr = osmandSettings.getLastSearchedPostcode();
			if (!Algoritms.isEmpty(postcodeStr)) {
				postcode = postcodeStr;
			} else {
				city = osmandSettings.getLastSearchedCityName();
			}

			if (!Algoritms.isEmpty(postcode) || !Algoritms.isEmpty(city)) {
				street = osmandSettings.getLastSearchedStreet();
				if (!Algoritms.isEmpty(street)) {
					String str = osmandSettings.getLastSearchedIntersectedStreet();
					radioBuilding = Algoritms.isEmpty(str);
					if (!radioBuilding) {
						street2 = str;
					} else {
						building = osmandSettings.getLastSearchedBuilding();
					}
				}
			}
		}
	}
	
	
	@Override
	protected void onResume() {
		super.onResume();

		searchPoint = osmandSettings.getLastSearchedPoint();
		
		Intent intent = getIntent();
		if (intent != null) {
			selectAddressMode = intent.hasExtra(SELECT_ADDRESS_POINT_INTENT_KEY);
		} else {
			selectAddressMode = false;
		}
		findViewById(R.id.TopTextView).setVisibility(selectAddressMode? View.VISIBLE : View.GONE);

		region = null;
		postcode = null;
		city = null;
		street = null;
		building = null;
		region = osmandSettings.getLastSearchedRegion();
		RegionAddressRepository reg = ((OsmandApplication)getApplication()).getResourceManager().getRegionRepository(region);
		if(reg != null && reg.useEnglishNames() != osmandSettings.USE_ENGLISH_NAMES.get()){
			reg.setUseEnglishNames(osmandSettings.USE_ENGLISH_NAMES.get());
		}
		loadData();
		updateUI();
		
	}

}
