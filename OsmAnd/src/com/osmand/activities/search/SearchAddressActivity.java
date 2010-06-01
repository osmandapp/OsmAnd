package com.osmand.activities.search;


import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;

import com.osmand.OsmandSettings;
import com.osmand.R;
import com.osmand.RegionAddressRepository;
import com.osmand.ResourceManager;
import com.osmand.activities.MapActivity;
import com.osmand.data.Building;
import com.osmand.data.City;
import com.osmand.data.Street;
import com.osmand.osm.LatLon;

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
	private ProgressDialog dlg;

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
				startActivity(new Intent(SearchAddressActivity.this, SearchBuildingByNameActivity.class));
			}
		});
		showOnMap.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				LatLon l = null;
				int zoom = 12;
				if (building != null) {
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
					updateUI();
				}
		 });
		 findViewById(R.id.ResetCity).setOnClickListener(new View.OnClickListener(){
				@Override
				public void onClick(View v) {
					city = null;
					street = null;
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
					building = null;
					updateUI();
				}
		 });
		
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
			cityButton.setText(city.getName());
		}
		cityButton.setEnabled(region != null);
		
		findViewById(R.id.ResetStreet).setEnabled(street != null);
		if(street == null){
			streetButton.setText(R.string.choose_street);
		} else {
			streetButton.setText(street.getName());
		}
		streetButton.setEnabled(city != null);
		
		findViewById(R.id.ResetBuilding).setEnabled(building != null);
		if(building == null){
			buildingButton.setText(R.string.choose_building);
		} else {
			buildingButton.setText(building.getName());
		}
		buildingButton.setEnabled(street != null);
		showOnMap.setEnabled(building != null || city != null || street != null);
	}
	
	public void loadData(){
		if (region != null) {
			city = region.getCityById(OsmandSettings.getLastSearchedCity(SearchAddressActivity.this));
			if (city != null) {
				street = region.getStreetByName(city, OsmandSettings.getLastSearchedStreet(SearchAddressActivity.this));
				if (street != null) {
					building = region.getBuildingByName(street, OsmandSettings
							.getLastSearchedBuilding(SearchAddressActivity.this));
				}
			}
		}		
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		region = null;
		
		String lastSearchedRegion = OsmandSettings.getLastSearchedRegion(SearchAddressActivity.this);
		region = ResourceManager.getResourceManager().getRegionRepository(lastSearchedRegion);
		String progressMsg = null;
		// try to determine whether progress dialog & new thread needed 
		if(!region.areCitiesPreloaded()){
			progressMsg = "Loading cities...";
		} else if(city == null || city.getId() != OsmandSettings.getLastSearchedCity(this)){
			progressMsg = "Loading streets/buildings...";
		}
		city = null;
		street = null;
		building = null;
		
		if (progressMsg != null) {
			dlg = ProgressDialog.show(this, "Loading", progressMsg, true);
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
		} else {
			loadData();
			updateUI();
		}
		
	}
	
	@Override
	protected void onPause() {
		if(building == null && OsmandSettings.getLastSearchedBuilding(this).length() > 0){
			OsmandSettings.setLastSearchedBuilding(this, "");
		}
		if(street == null && OsmandSettings.getLastSearchedStreet(this).length() > 0){
			OsmandSettings.setLastSearchedStreet(this, "");
		}
		if(city == null && OsmandSettings.getLastSearchedCity(this) != -1){
			OsmandSettings.setLastSearchedCity(this, -1l);
		}
		super.onPause();
	}
	

}
