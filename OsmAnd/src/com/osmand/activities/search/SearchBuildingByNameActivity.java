package com.osmand.activities.search;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.widget.TextView;

import com.osmand.OsmandSettings;
import com.osmand.R;
import com.osmand.RegionAddressRepository;
import com.osmand.ResourceManager;
import com.osmand.data.Building;
import com.osmand.data.City;
import com.osmand.data.PostCode;
import com.osmand.data.Street;

public class SearchBuildingByNameActivity extends SearchByNameAbstractActivity<Building> {
	private RegionAddressRepository region;
	private City city;
	private Street street;
	private PostCode postcode;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		region = ResourceManager.getResourceManager().getRegionRepository(OsmandSettings.getLastSearchedRegion(this));
		if(region != null){
			postcode = region.getPostcode(OsmandSettings.getLastSearchedPostcode(this));
			city = region.getCityById(OsmandSettings.getLastSearchedCity(this));
			if(postcode != null){
				street = region.getStreetByName(postcode, OsmandSettings.getLastSearchedStreet(this));
			} else if(city != null){
				street = region.getStreetByName(city, OsmandSettings.getLastSearchedStreet(this));
			}
		}
		super.onCreate(savedInstanceState);
		((TextView)findViewById(R.id.Label)).setText(R.string.incremental_search_building);
	}
	
	@Override
	public List<Building> getObjects(String filter) {
		List<Building> l = new ArrayList<Building>();
		if(street != null){
			region.fillWithSuggestedBuildings(postcode, street, filter, l);
		}
		return l;
	}
	
	@Override
	public void updateTextView(Building obj, TextView txt) {
		txt.setText(obj.getName(region.useEnglishNames()));
	}
	
	@Override
	public void itemSelected(Building obj) {
		OsmandSettings.setLastSearchedBuilding(this, obj.getName(region.useEnglishNames()));
		finish();
		
	}
}
