package net.osmand.plus.activities.search;

import java.util.ArrayList;
import java.util.List;

import net.osmand.data.City;
import net.osmand.data.PostCode;
import net.osmand.data.Street;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.RegionAddressRepository;
import net.osmand.plus.activities.OsmandApplication;
import android.os.Bundle;
import android.widget.TextView;

public class SearchStreetByNameActivity extends SearchByNameAbstractActivity<Street> {
	private RegionAddressRepository region;
	private City city;
	private PostCode postcode;
	private OsmandSettings settings;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		settings = OsmandSettings.getOsmandSettings(this);
		region = ((OsmandApplication)getApplication()).getResourceManager().getRegionRepository(settings.getLastSearchedRegion());
		if(region != null){
			postcode = region.getPostcode(settings.getLastSearchedPostcode());
			if (postcode == null) {
				city = region.getCityById(settings.getLastSearchedCity());
			}
		}
		super.onCreate(savedInstanceState);
		((TextView)findViewById(R.id.Label)).setText(R.string.incremental_search_street);
	}
	
	@Override
	public List<Street> getObjects(String filter) {
		List<Street> l = new ArrayList<Street>();
		if (city != null || postcode != null) {
			region.fillWithSuggestedStreets(postcode == null ? city : postcode, l, filter);
		}
		return l;
	}
	
	@Override
	public void updateTextView(Street obj, TextView txt) {
		txt.setText(obj.getName(region.useEnglishNames()));
	}
	
	@Override
	public void itemSelected(Street obj) {
		settings.setLastSearchedStreet(obj.getName(region.useEnglishNames()), obj.getLocation());
		finish();
		
	}
}
