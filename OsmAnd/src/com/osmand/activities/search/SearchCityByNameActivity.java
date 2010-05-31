package com.osmand.activities.search;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.widget.TextView;

import com.osmand.OsmandSettings;
import com.osmand.RegionAddressRepository;
import com.osmand.ResourceManager;
import com.osmand.data.City;
import com.osmand.osm.LatLon;
import com.osmand.osm.MapUtils;

public class SearchCityByNameActivity extends SearchByNameAbstractActivity<City> {
	private RegionAddressRepository region;
	private LatLon location;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		region = ResourceManager.getResourceManager().getRegionRepository(OsmandSettings.getLastSearchedRegion(this));
		location = OsmandSettings.getLastKnownMapLocation(this);
		super.onCreate(savedInstanceState);
	}
	
	@Override
	public List<City> getObjects(String filter) {
		List<City> l = new ArrayList<City>();
		if(region != null){
			region.fillWithSuggestedCities(filter, l);
		}
		return l;
	}
	
	@Override
	public void updateTextView(City obj, TextView txt) {
		if(getFilter().length() > 2){
			txt.setText(obj.getName() + " - " + MapUtils.getFormattedDistance((int) MapUtils.getDistance(obj.getLocation(), location)));
		} else {
			txt.setText(obj.getName());
		}
	}
	
	@Override
	public void itemSelected(City obj) {
		OsmandSettings.setLastSearchedCity(this, obj.getId());
		if(region.getCityById(obj.getId()) == null){
			region.registerCity(obj);
		}
		finish();
		
	}
}
