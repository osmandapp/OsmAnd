package com.osmand.activities.search;


import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;

import com.osmand.OsmandSettings;
import com.osmand.R;
import com.osmand.RegionAddressRepository;
import com.osmand.ResourceManager;
import com.osmand.data.Building;
import com.osmand.data.City;
import com.osmand.data.Street;

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
	
	@Override
	protected void onResume() {
		super.onResume();
		region = null;
		city = null;
		street = null;
		building = null;
		String lastSearchedRegion = OsmandSettings.getLastSearchedRegion(this);
		region = ResourceManager.getResourceManager().getRegionRepository(lastSearchedRegion);
		if(region != null){
			city = region.getCityById(OsmandSettings.getLastSearchedCity(this));
			if(city != null){
				street = region.getStreetByName(city, OsmandSettings.getLastSearchedStreet(this));
			}
		}
		
		updateUI();
	}
	

}
