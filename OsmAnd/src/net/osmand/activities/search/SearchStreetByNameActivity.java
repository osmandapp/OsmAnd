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
	private String oldfilter = "";
	
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
		((TextView)findViewById(R.id.Label)).setText(isCountrySearch() ? R.string.incremental_search_street_in_country : R.string.incremental_search_street);
	}
	
	@Override
	public List<Street> getObjects(String filter) {
		NamesAdapter list = (NamesAdapter)getListAdapter();
		boolean countrySearch = isCountrySearch();
		boolean incremental = filter.startsWith(oldfilter) && oldfilter.length() > 0 && list.getCount() > 0;
		this.oldfilter = filter;
		List<Street> result = new ArrayList<Street>();
		if (incremental) {
			List<Street> streets = new ArrayList<Street>();
			for (int i = 0; i < list.getCount(); i++ ) {
				streets.add((Street) list.getItem(i));
			}
			region.fillWithSuggestedStreets(filter,result,streets,countrySearch);
		} else {
			if (!countrySearch) {
				region.fillWithSuggestedStreets(postcode == null ? city : postcode, filter, result);
			} else if (filter.length() > 2) {
				region.fillWithSuggestedStreets(filter, result);
			}
		}
		return result;
	}

	private boolean isCountrySearch() {
		return city == null && postcode == null;
	}
	
	@Override
	public void updateTextView(Street obj, TextView txt) {
		if (city != null || postcode != null) {
			txt.setText(obj.getName(region.useEnglishNames()));
		} else {
			txt.setText(obj.getDisplayName(region.useEnglishNames()));
		}
	}
	
	@Override
	public void itemSelected(Street obj) {
		if (obj.getCity() != null) { //can be in case of postcode
			OsmandSettings.setLastSearchedCity(this, obj.getCity().getId());
		}
		OsmandSettings.setLastSearchedStreet(this, obj.getName(region.useEnglishNames()));
		finish();
	}
}
