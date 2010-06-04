package com.osmand.activities.search;


import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.osmand.OsmandSettings;
import com.osmand.R;
import com.osmand.RegionAddressRepository;
import com.osmand.ResourceManager;
import com.osmand.activities.MapActivity;
import com.osmand.data.Building;
import com.osmand.data.City;
import com.osmand.data.Street;
import com.osmand.osm.LatLon;
import com.osmand.osm.Node;
import com.osmand.osm.Way;

public class SearchAddressActivity extends Activity {

	private Button showOnMap;
	private Button streetButton;
	private Button cityButton;
	private Button countryButton;
	private Button buildingButton;
	
	private RegionAddressRepository region = null;
	private City city = null;
	private Street street = null;
	private Building building = null;
	private Street street2 = null;
	private boolean radioBuilding = true;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.search_address);
		
		showOnMap = (Button) findViewById(R.id.ShowOnMap);
		streetButton = (Button) findViewById(R.id.StreetButton);
		cityButton = (Button) findViewById(R.id.CityButton);
		countryButton = (Button) findViewById(R.id.CountryButton);
		buildingButton = (Button) findViewById(R.id.BuildingButton);
		attachListeners();
	}
	
	private void attachListeners() {
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
					OsmandSettings.removeLastSearchedIntersectedStreet(SearchAddressActivity.this);
					startActivity(new Intent(SearchAddressActivity.this, SearchBuildingByNameActivity.class));
				} else {
					OsmandSettings.setLastSearchedIntersectedStreet(SearchAddressActivity.this, "");
					startActivity(new Intent(SearchAddressActivity.this, SearchStreet2ByNameActivity.class));
				}
			}
		});
		showOnMap.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				LatLon l = null;
				int zoom = 12;
				if (street2 != null && street != null) {
					region.preloadWayNodes(street2);
					region.preloadWayNodes(street);
					Node inters = null;
					for(Way w : street2.getWayNodes()){
						for(Way w2 : street.getWayNodes()){
							for(Node n : w.getNodes()){
								for(Node n2 : w2.getNodes()){
									if(n.getId() == n2.getId()){
										inters = n;
										break;
									}
								}
							}
						}
					}
					if(inters != null){
						l = inters.getLatLon();
						zoom = 16; 
					}
				} else if (building != null) {
					l = building.getLocation();
					zoom = 16;
				} else if (street != null) {
					l = street.getLocation();
					zoom = 14;
				} else if (city != null) {
					l = city.getLocation();
					zoom = 12;
				}
				if (l != null) {
					OsmandSettings.setLastKnownMapLocation(SearchAddressActivity.this, l.getLatitude(), l.getLongitude());
					OsmandSettings.setLastKnownMapZoom(SearchAddressActivity.this, zoom);
					startActivity(new Intent(SearchAddressActivity.this, MapActivity.class));
				}

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
	
	protected void updateBuildingSection(){
		if(radioBuilding){
			((TextView)findViewById(R.id.BuildingText)).setText("Building");
			if(building == null){
				((TextView)findViewById(R.id.BuildingButton)).setText(R.string.choose_building);
			} else {
				((TextView)findViewById(R.id.BuildingButton)).setText(building.getName(region.useEnglishNames()));
			}
		} else {
			((TextView)findViewById(R.id.BuildingText)).setText("Street 2");
			if(street2 == null){
				((TextView)findViewById(R.id.BuildingButton)).setText(R.string.choose_intersected_street);
			} else {
				((TextView)findViewById(R.id.BuildingButton)).setText(street2.getName(region.useEnglishNames()));
			}
		}
		findViewById(R.id.ResetBuilding).setEnabled(building != null || street2 != null);
	}

	protected void updateUI(){
		findViewById(R.id.ResetCountry).setEnabled(region != null);
		if(region == null){
			countryButton.setText(R.string.ChooseCountry);
		} else {
			countryButton.setText(region.getName());
		}
		findViewById(R.id.ResetCity).setEnabled(city != null);
		if(city == null){
			cityButton.setText(R.string.choose_city);
		} else {
			cityButton.setText(city.getName(region.useEnglishNames()));
		}
		cityButton.setEnabled(region != null);
		
		findViewById(R.id.ResetStreet).setEnabled(street != null);
		if(street == null){
			streetButton.setText(R.string.choose_street);
		} else {
			streetButton.setText(street.getName(region.useEnglishNames()));
		}
		streetButton.setEnabled(city != null);
		
		if(radioBuilding){
			((RadioButton)findViewById(R.id.RadioBuilding)).setChecked(true);
		} else {
			((RadioButton)findViewById(R.id.RadioIntersStreet)).setChecked(true);
		}
		updateBuildingSection();
		
		buildingButton.setEnabled(street != null);
		
		showOnMap.setEnabled(building != null || city != null || street != null);
	}
	
	public void loadData(){
		if (region != null) {
			if(region.useEnglishNames() != OsmandSettings.usingEnglishNames(this)){
				region.setUseEnglishNames(OsmandSettings.usingEnglishNames(this));
			}
			city = region.getCityById(OsmandSettings.getLastSearchedCity(SearchAddressActivity.this));
			if (city != null) {
				street = region.getStreetByName(city, OsmandSettings.getLastSearchedStreet(SearchAddressActivity.this));
				if (street != null) {
					String str = OsmandSettings.getLastSearchedIntersectedStreet(SearchAddressActivity.this);
					radioBuilding = str == null;
					if(str != null){
						street2 = region.getStreetByName(city, str);
					} else {
						building = region.getBuildingByName(street, OsmandSettings
								.getLastSearchedBuilding(SearchAddressActivity.this));
					}
				}
			}
		}		
	}
	
	protected void startLoadDataInThread(String progressMsg){
		final ProgressDialog dlg = ProgressDialog.show(this, "Loading", progressMsg, true);
		new Thread("Loader search data") {
			@Override
			public void run() {
				try {
					loadData();
				} finally {
					dlg.dismiss();
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							updateUI();
						}
					});
				}
			}
		}.start();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		region = null;
		String lastSearchedRegion = OsmandSettings.getLastSearchedRegion(SearchAddressActivity.this);
		region = ResourceManager.getResourceManager().getRegionRepository(lastSearchedRegion);
		String progressMsg = null;
		// try to determine whether progress dialog & new thread needed
		if (region != null) {
			Long cityId = OsmandSettings.getLastSearchedCity(this);
			if (!region.areCitiesPreloaded()) {
				progressMsg = "Loading cities...";
			} else if (cityId != -1 && region.getCityById(cityId) != null && region.getCityById(cityId).isEmptyWithStreets()) {
				progressMsg = "Loading streets/buildings...";
			} else if (OsmandSettings.usingEnglishNames(this) != region.useEnglishNames()) {
				progressMsg = "Converting native/english names...";
			}
		}
		city = null;
		street = null;
		building = null;
		
		if (progressMsg != null) {
			startLoadDataInThread(progressMsg);
		} else {
			loadData();
			updateUI();
		}
		
	}
	
	@Override
	protected void onPause() {
		// Do not reset settings (cause it is not so necessary)
//		if(building == null && OsmandSettings.getLastSearchedBuilding(this).length() > 0){
//			OsmandSettings.setLastSearchedBuilding(this, "");
//		}
//		if(street == null && OsmandSettings.getLastSearchedStreet(this).length() > 0){
//			OsmandSettings.setLastSearchedStreet(this, "");
//		}
//		if(city == null && OsmandSettings.getLastSearchedCity(this) != -1){
//			OsmandSettings.setLastSearchedCity(this, -1l);
//		}
		super.onPause();
	}
	

}
