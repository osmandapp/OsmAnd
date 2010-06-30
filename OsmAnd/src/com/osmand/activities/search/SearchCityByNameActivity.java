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
import com.osmand.data.MapObject;
import com.osmand.data.PostCode;
import com.osmand.osm.LatLon;
import com.osmand.osm.MapUtils;

public class SearchCityByNameActivity extends SearchByNameAbstractActivity<MapObject> {
	private RegionAddressRepository region;
	private LatLon location;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		region = ResourceManager.getResourceManager().getRegionRepository(OsmandSettings.getLastSearchedRegion(this));
		location = OsmandSettings.getLastKnownMapLocation(this);
		super.onCreate(savedInstanceState);
		((TextView)findViewById(R.id.Label)).setText(R.string.incremental_search_city);
	}
	
	@Override
	public List<MapObject> getObjects(String filter) {
		List<MapObject> l = new ArrayList<MapObject>();
		if(region != null){
			region.fillWithSuggestedCities(filter, l, location);
		}
		return l;
	}
	
	@Override
	public void updateTextView(MapObject obj, TextView txt) {
		LatLon l = obj.getLocation();
		if (getFilter().length() > 2 && location != null && l != null) {
			txt.setText(obj.getName(region.useEnglishNames()) + " - " + //$NON-NLS-1$
					MapUtils.getFormattedDistance((int) MapUtils.getDistance(l, location))); 
		} else {
			txt.setText(obj.getName(region.useEnglishNames()));
		}
	}
	
	@Override
	public void itemSelected(MapObject obj) {
		if (obj instanceof City) {
			OsmandSettings.setLastSearchedCity(this, obj.getId());
			if (region.getCityById(obj.getId()) == null) {
				region.registerCity((City) obj);
			}
		} else if(obj instanceof PostCode){
			OsmandSettings.setLastSearchedPostcode(this, obj.getName());
		}
		finish();
		
	}
}
