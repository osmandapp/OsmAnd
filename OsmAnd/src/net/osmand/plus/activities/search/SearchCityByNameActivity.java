package net.osmand.plus.activities.search;

import java.util.ArrayList;
import java.util.List;

import net.osmand.OsmAndFormatter;
import net.osmand.data.City;
import net.osmand.data.MapObject;
import net.osmand.data.PostCode;
import net.osmand.osm.LatLon;
import net.osmand.osm.MapUtils;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.RegionAddressRepository;
import net.osmand.plus.activities.OsmandApplication;
import android.os.Bundle;
import android.widget.TextView;

public class SearchCityByNameActivity extends SearchByNameAbstractActivity<MapObject> {
	private RegionAddressRepository region;
	private LatLon location;
	private OsmandSettings settings;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		settings = ((OsmandApplication)getApplication()).getSettings();
		region = ((OsmandApplication)getApplication()).getResourceManager().getRegionRepository(settings.getLastSearchedRegion());
		location = settings.getLastKnownMapLocation();
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
					OsmAndFormatter.getFormattedDistance((int) MapUtils.getDistance(l, location), this)); 
		} else {
			txt.setText(obj.getName(region.useEnglishNames()));
		}
	}
	
	@Override
	public void itemSelected(MapObject obj) {
		if (obj instanceof City) {
			settings.setLastSearchedCity(obj.getId());
			if (region.getCityById(obj.getId()) == null) {
				region.addCityToPreloadedList((City) obj);
			}
		} else if(obj instanceof PostCode){
			settings.setLastSearchedPostcode(obj.getName());
		}
		finish();
		
	}
}
