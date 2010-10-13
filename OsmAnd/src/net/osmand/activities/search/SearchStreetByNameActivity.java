package net.osmand.activities.search;

import java.util.ArrayList;
import java.util.List;

import net.osmand.OsmandSettings;
import net.osmand.R;
import net.osmand.RegionAddressRepository;
import net.osmand.activities.OsmandApplication;
import net.osmand.data.City;
import net.osmand.data.PostCode;
import net.osmand.data.Street;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

public class SearchStreetByNameActivity extends SearchByNameAbstractActivity<Street> {
	private RegionAddressRepository region;
	private City city;
	private PostCode postcode;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		SharedPreferences prefs = OsmandSettings.getPrefs(this);
		region = ((OsmandApplication)getApplication()).getResourceManager().getRegionRepository(OsmandSettings.getLastSearchedRegion(prefs));
		if(region != null){
			postcode = region.getPostcode(OsmandSettings.getLastSearchedPostcode(prefs));
			if (postcode == null) {
				city = region.getCityById(OsmandSettings.getLastSearchedCity(prefs));
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
