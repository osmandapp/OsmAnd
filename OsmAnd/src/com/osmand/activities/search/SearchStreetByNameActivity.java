package com.osmand.activities.search;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.widget.TextView;

import com.osmand.OsmandSettings;
import com.osmand.R;
import com.osmand.RegionAddressRepository;
import com.osmand.ResourceManager;
import com.osmand.data.City;
import com.osmand.data.PostCode;
import com.osmand.data.Street;

public class SearchStreetByNameActivity extends SearchByNameAbstractActivity<Street> {
	private RegionAddressRepository region;
	private City city;
	private PostCode postcode;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		region = ResourceManager.getResourceManager().getRegionRepository(OsmandSettings.getLastSearchedRegion(this));
		if(region != null){
			postcode = region.getPostcode(OsmandSettings.getLastSearchedPostcode(this));
			if (postcode == null) {
				city = region.getCityById(OsmandSettings.getLastSearchedCity(this));
			}
		}
		super.onCreate(savedInstanceState);
		((TextView)findViewById(R.id.Label)).setText(R.string.incremental_search_street);
	}
	
	@Override
	public List<Street> getObjects(String filter) {
		List<Street> l = new ArrayList<Street>();
		if (city != null || postcode != null) {
			region.fillWithSuggestedStreets(postcode == null ? city : postcode, filter, l);
		}
		return l;
	}
	
	@Override
	public void updateTextView(Street obj, TextView txt) {
		txt.setText(obj.getName(region.useEnglishNames()));
	}
	
	@Override
	public void itemSelected(Street obj) {
		OsmandSettings.setLastSearchedStreet(this, obj.getName(region.useEnglishNames()));
		finish();
		
	}
}
