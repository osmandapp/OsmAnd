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
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class SearchCityByNameActivity extends SearchByNameAbstractActivity<MapObject> {
	private RegionAddressRepository region;
	
	
	@Override
	public AsyncTask<Object, ?, ?> getInitializeTask() {
		return new AsyncTask<Object, Void, Void>(){
			@Override
			protected void onPostExecute(Void result) {
				((TextView)findViewById(R.id.Label)).setText(R.string.incremental_search_city);
				progress.setVisibility(View.INVISIBLE);
				resetText();
			}
			
			@Override
			protected void onPreExecute() {
				((TextView)findViewById(R.id.Label)).setText(R.string.loading_cities);
				progress.setVisibility(View.VISIBLE);
			}
			@Override
			protected Void doInBackground(Object... params) {
				region = ((OsmandApplication)getApplication()).getResourceManager().getRegionRepository(settings.getLastSearchedRegion());
				return null;
			}
		};
	}
	
	@Override
	public List<MapObject> getObjects(String filter) {
		List<MapObject> l = new ArrayList<MapObject>();
		if(region != null){
			region.fillWithSuggestedCities(filter, l, locationToSearch);
		}
		return l;
	}
	
	@Override
	public void updateTextView(MapObject obj, TextView txt) {
		LatLon l = obj.getLocation();
		if (getFilter().length() > 2 && locationToSearch != null && l != null) {
			txt.setText(obj.getName(region.useEnglishNames()) + " - " + //$NON-NLS-1$
					OsmAndFormatter.getFormattedDistance((int) MapUtils.getDistance(l, locationToSearch), this)); 
		} else {
			txt.setText(obj.getName(region.useEnglishNames()));
		}
	}
	
	@Override
	public void itemSelected(MapObject obj) {
		if (obj instanceof City) {
			settings.setLastSearchedCity(obj.getId(), obj.getName(region.useEnglishNames()), obj.getLocation());
			if (region.getCityById(obj.getId()) == null) {
				region.addCityToPreloadedList((City) obj);
			}
		} else if(obj instanceof PostCode){
			settings.setLastSearchedPostcode(obj.getName(region.useEnglishNames()), obj.getLocation());
		}
		finish();
		
	}
}
