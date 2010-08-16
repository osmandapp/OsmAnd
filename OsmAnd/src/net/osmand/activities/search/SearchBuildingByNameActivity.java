package net.osmand.activities.search;

import java.util.ArrayList;
import java.util.List;

import net.osmand.OsmandSettings;
import net.osmand.R;
import net.osmand.RegionAddressRepository;
import net.osmand.ResourceManager;
import net.osmand.data.Building;
import net.osmand.data.City;
import net.osmand.data.PostCode;
import net.osmand.data.Street;
import android.os.Bundle;
import android.widget.TextView;

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
