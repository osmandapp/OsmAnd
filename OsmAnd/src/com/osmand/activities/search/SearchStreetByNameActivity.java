package com.osmand.activities.search;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.widget.TextView;

import com.osmand.OsmandSettings;
import com.osmand.RegionAddressRepository;
import com.osmand.ResourceManager;
import com.osmand.data.City;
import com.osmand.data.Street;

public class SearchStreetByNameActivity extends SearchByNameAbstractActivity<Street> {
	private RegionAddressRepository region;
	private City city;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		region = ResourceManager.getResourceManager().getRegionRepository(OsmandSettings.getLastSearchedRegion(this));
		if(region != null){
			city = region.getCityById(OsmandSettings.getLastSearchedCity(this));
		}
		super.onCreate(savedInstanceState);
	}
	
	@Override
	public List<Street> getObjects() {
		List<Street> l = new ArrayList<Street>();
		if(city != null){
			region.fillWithSuggestedStreets(city, "", l);
		}
		return l;
	}
	
	@Override
	public void updateTextView(Street obj, TextView txt) {
		txt.setText(obj.getName());
	}
	
	@Override
	public void itemSelected(Street obj) {
		OsmandSettings.setLastSearchedStreet(this, obj.getName());
		finish();
		
	}
}
