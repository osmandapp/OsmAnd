package com.osmand.activities.search;


import java.text.MessageFormat;

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
import com.osmand.data.MapObject;
import com.osmand.data.PostCode;
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
	private Button navigateTo;
	
	private RegionAddressRepository region = null;
	private City city = null;
	private PostCode postcode = null;
	private Street street = null;
	private Building building = null;
	private Street street2 = null;
	private boolean radioBuilding = true;
	private Button searchOnline;
	

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
					OsmandSettings.removeLastSearchedIntersectedStreet(SearchAddressActivity.this);
					startActivity(new Intent(SearchAddressActivity.this, SearchBuildingByNameActivity.class));
				} else {
					OsmandSettings.setLastSearchedIntersectedStreet(SearchAddressActivity.this, ""); //$NON-NLS-1$
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
	
	public void showOnMap(boolean navigateTo){
		LatLon l = null;
		String historyName = null;
		int zoom = 12;
		boolean en = OsmandSettings.usingEnglishNames(this);
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
				String cityName = postcode != null? postcode.getName() :  city.getName(en);
				historyName = MessageFormat.format(getString(R.string.search_history_int_streets), 
						street.getName(en), street2.getName(en), cityName); 
				zoom = 16; 
			}
		} else if (building != null) {
			l = building.getLocation();
			String cityName = postcode != null? postcode.getName() :  city.getName(en);
			historyName = MessageFormat.format(getString(R.string.search_history_building), building.getName(en), street.getName(en), cityName);
			zoom = 16;
		} else if (street != null) {
			l = street.getLocation();
			String cityName = postcode != null? postcode.getName() :  city.getName(en);
			historyName = MessageFormat.format(getString(R.string.search_history_street), street.getName(en), cityName);
			zoom = 14;
		} else if (city != null) {
			l = city.getLocation();
			historyName = MessageFormat.format(getString(R.string.search_history_city), city.getName(en));
			zoom = 12;
		}
		if (l != null) {
			if(navigateTo){
				OsmandSettings.setPointToNavigate(SearchAddressActivity.this, l.getLatitude(), l.getLongitude());
			} else {
				OsmandSettings.setMapLocationToShow(SearchAddressActivity.this, l.getLatitude(), l.getLongitude(), historyName);
				OsmandSettings.setLastKnownMapZoom(SearchAddressActivity.this, zoom);
			}
			
			startActivity(new Intent(SearchAddressActivity.this, MapActivity.class));
		}
	}
	
	protected void updateBuildingSection(){
		if(radioBuilding){
			((TextView)findViewById(R.id.BuildingText)).setText(R.string.search_address_building);
			if(building == null){
				((TextView)findViewById(R.id.BuildingButton)).setText(R.string.choose_building);
			} else {
				((TextView)findViewById(R.id.BuildingButton)).setText(building.getName(region.useEnglishNames()));
			}
		} else {
			((TextView)findViewById(R.id.BuildingText)).setText(R.string.search_address_street);
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
		findViewById(R.id.ResetCity).setEnabled(postcode != null || city != null);
		if(city == null && postcode == null){
			cityButton.setText(R.string.choose_city);
		} else {
			if(postcode != null){
				cityButton.setText(postcode.getName());
			} else {
				cityButton.setText(city.getName(region.useEnglishNames()));
			}
		}
		cityButton.setEnabled(region != null);
		
		findViewById(R.id.ResetStreet).setEnabled(street != null);
		if(street == null){
			streetButton.setText(R.string.choose_street);
		} else {
			streetButton.setText(street.getName(region.useEnglishNames()));
		}
		streetButton.setEnabled(city != null || postcode != null);
		
		if(radioBuilding){
			((RadioButton)findViewById(R.id.RadioBuilding)).setChecked(true);
		} else {
			((RadioButton)findViewById(R.id.RadioIntersStreet)).setChecked(true);
		}
		updateBuildingSection();
		
		buildingButton.setEnabled(street != null);
		
		showOnMap.setEnabled(city != null || street != null);
		navigateTo.setEnabled(city != null || street != null);
	}
	
	public void loadData(){
		if (region != null) {
			if(region.useEnglishNames() != OsmandSettings.usingEnglishNames(this)){
				region.setUseEnglishNames(OsmandSettings.usingEnglishNames(this));
			}
			String postcodeStr = OsmandSettings.getLastSearchedPostcode(this);
			if(postcodeStr != null){
				postcode = region.getPostcode(postcodeStr);
			} else {
				city = region.getCityById(OsmandSettings.getLastSearchedCity(SearchAddressActivity.this));
			}
			
			if (postcode != null || city != null) {
				MapObject o = postcode == null ? city : postcode;
				street = region.getStreetByName(o, OsmandSettings.getLastSearchedStreet(SearchAddressActivity.this));
				if (street != null) {
					String str = OsmandSettings.getLastSearchedIntersectedStreet(SearchAddressActivity.this);
					radioBuilding = str == null;
					if(str != null){
						street2 = region.getStreetByName(o, str);
					} else {
						building = region.getBuildingByName(street, OsmandSettings.getLastSearchedBuilding(SearchAddressActivity.this));
					}
				}
			}
		}		
	}
	
	protected void startLoadDataInThread(String progressMsg){
		final ProgressDialog dlg = ProgressDialog.show(this, getString(R.string.loading), progressMsg, true);
		new Thread("Loader search data") { //$NON-NLS-1$
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
			String postcode = OsmandSettings.getLastSearchedPostcode(this);
			if (!region.areCitiesPreloaded()) {
				progressMsg = getString(R.string.loading_cities);
			} else if (postcode != null && !region.arePostcodesPreloaded()) {
				progressMsg = getString(R.string.loading_postcodes);
			} else if (cityId != -1 && region.getCityById(cityId) != null && region.getCityById(cityId).isEmptyWithStreets()) {
				progressMsg = getString(R.string.loading_streets_buildings);
			} else if (postcode != null && region.getPostcode(postcode) != null && region.getPostcode(postcode).isEmptyWithStreets()) {
				progressMsg = getString(R.string.loading_streets_buildings);
			} else if (OsmandSettings.usingEnglishNames(this) != region.useEnglishNames()) {
				progressMsg = getString(R.string.converting_names);
			}
		}
		postcode = null;
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
