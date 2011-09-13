package net.osmand.plus.activities.search;


import java.text.MessageFormat;

import net.osmand.Algoritms;
import net.osmand.osm.LatLon;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

public class SearchAddressActivity extends Activity  {

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
		osmandSettings = OsmandSettings.getOsmandSettings(SearchAddressActivity.this);
		attachListeners();
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
				startActivity(new Intent(SearchAddressActivity.this, SearchRegionByNameActivity.class));
			}
		});
		cityButton.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v) {
				startActivity(new Intent(SearchAddressActivity.this, SearchCityByNameActivity.class));
			}
		});
		streetButton.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v) {
				startActivity(new Intent(SearchAddressActivity.this, SearchStreetByNameActivity.class));
			}
		});
		buildingButton.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v) {
				if(radioBuilding){
					osmandSettings.removeLastSearchedIntersectedStreet();
					startActivity(new Intent(SearchAddressActivity.this, SearchBuildingByNameActivity.class));
				} else {
					osmandSettings.setLastSearchedIntersectedStreet(""); //$NON-NLS-1$
					startActivity(new Intent(SearchAddressActivity.this, SearchStreet2ByNameActivity.class));
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
				updateUI();
			}
		 });
		 findViewById(R.id.ResetStreet).setOnClickListener(new View.OnClickListener(){
				@Override
				public void onClick(View v) {
					street = null;
					building = null;
					street2 = null;
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
		int zoom = 12;
		if (street2 != null && street != null) {
			String cityName = postcode != null ? postcode : city;
			historyName = MessageFormat.format(getString(R.string.search_history_int_streets), street, street2,
					cityName);
			zoom = 16;
		} else if (building != null) {
			String cityName = postcode != null ? postcode : city;
			historyName = MessageFormat.format(getString(R.string.search_history_building), building, street,
					cityName);
			zoom = 16;
		} else if (street != null) {
			String cityName = postcode != null ? postcode : city;
			historyName = MessageFormat.format(getString(R.string.search_history_street), street, cityName);
			zoom = 14;
		} else if (city != null) {
			historyName = MessageFormat.format(getString(R.string.search_history_city), city);
			zoom = 12;
		}
		if (navigateTo) {
			osmandSettings.setPointToNavigate(searchPoint.getLatitude(), searchPoint.getLongitude(), historyName);
		} else {
			osmandSettings.setMapLocationToShow(searchPoint.getLatitude(), searchPoint.getLongitude(), zoom, historyName);
		}

		MapActivity.launchMapActivityMoveToTop(SearchAddressActivity.this);
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
		findViewById(R.id.ResetCountry).setEnabled(!Algoritms.isEmpty(region));
		if(Algoritms.isEmpty(region)){
			countryButton.setText(R.string.ChooseCountry);
		} else {
			countryButton.setText(region);
		}
		findViewById(R.id.ResetCity).setEnabled(postcode != null || city != null);
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
		if(street == null){
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
				if (Algoritms.isEmpty(street)) {
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
		showOnMap.setEnabled(searchPoint != null);
		navigateTo.setEnabled(searchPoint != null);

		region = null;
		postcode = null;
		city = null;
		street = null;
		building = null;
		region = osmandSettings.getLastSearchedRegion();
		loadData();
		updateUI();

		// TODO other can be moved to specific searches 
//		if (region != null) {
//			Long cityId = osmandSettings.getLastSearchedCity();
//			String postcode = osmandSettings.getLastSearchedPostcode();
//			if (!region.areCitiesPreloaded()) {
//				progressMsg = getString(R.string.loading_cities);
//			} else if (postcode != null && !region.arePostcodesPreloaded()) {
//				progressMsg = getString(R.string.loading_postcodes);
//			} else if (cityId != -1 && region.getCityById(cityId) != null && region.getCityById(cityId).isEmptyWithStreets()) {
//				progressMsg = getString(R.string.loading_streets_buildings);
//			} else if (postcode != null && region.getPostcode(postcode) != null && region.getPostcode(postcode).isEmptyWithStreets()) {
//				progressMsg = getString(R.string.loading_streets_buildings);
//			} else if (osmandSettings.USE_ENGLISH_NAMES.get() != region.useEnglishNames()) {
//				progressMsg = getString(R.string.converting_names);
//			}
//		}
		
	}

}
