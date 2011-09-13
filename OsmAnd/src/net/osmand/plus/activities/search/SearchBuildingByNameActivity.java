package net.osmand.plus.activities.search;

import java.util.ArrayList;
import java.util.List;

import net.osmand.data.Building;
import net.osmand.data.City;
import net.osmand.data.PostCode;
import net.osmand.data.Street;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.RegionAddressRepository;
import net.osmand.plus.activities.OsmandApplication;
import android.os.Bundle;
import android.widget.TextView;

public class SearchBuildingByNameActivity extends SearchByNameAbstractActivity<Building> {
	private RegionAddressRepository region;
	private City city;
	private Street street;
	private PostCode postcode;
	private OsmandSettings settings;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		settings = OsmandSettings.getOsmandSettings(this);
		region = ((OsmandApplication)getApplication()).getResourceManager().getRegionRepository(settings.getLastSearchedRegion());
		if(region != null){
			postcode = region.getPostcode(settings.getLastSearchedPostcode());
			city = region.getCityById(settings.getLastSearchedCity());
			if(postcode != null){
				street = region.getStreetByName(postcode, settings.getLastSearchedStreet());
			} else if(city != null){
				street = region.getStreetByName(city, settings.getLastSearchedStreet());
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
		settings.setLastSearchedBuilding(obj.getName(region.useEnglishNames()), obj.getLocation());
		finish();
		
	}
}
