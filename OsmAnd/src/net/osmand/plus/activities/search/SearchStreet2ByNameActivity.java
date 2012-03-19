package net.osmand.plus.activities.search;

import java.util.List;

import net.osmand.data.City;
import net.osmand.data.Street;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.RegionAddressRepository;
import android.os.AsyncTask;
import android.view.View;
import android.widget.TextView;

public class SearchStreet2ByNameActivity extends SearchByNameAbstractActivity<Street> {
	private RegionAddressRepository region;
	private City cityOrPostcode;
	private Street street1;
	
	
	@Override
	public AsyncTask<Object, ?, ?> getInitializeTask() {
		return new AsyncTask<Object, Void, List<Street>>(){
			@Override
			protected void onPostExecute(List<Street> result) {
				((TextView)findViewById(R.id.Label)).setText(R.string.incremental_search_street);
				progress.setVisibility(View.INVISIBLE);
				finishInitializing(result);
			}
			
			@Override
			protected void onPreExecute() {
				((TextView)findViewById(R.id.Label)).setText(R.string.loading_streets);
				progress.setVisibility(View.VISIBLE);
			}
			@Override
			protected List<Street> doInBackground(Object... params) {
				region = ((OsmandApplication)getApplication()).getResourceManager().getRegionRepository(settings.getLastSearchedRegion());
				if(region != null){
					cityOrPostcode = region.getCityById(settings.getLastSearchedCity(), settings.getLastSearchedCityName());
					if(cityOrPostcode != null){
						street1 = region.getStreetByName(cityOrPostcode, (settings.getLastSearchedStreet()));
					}
					if(cityOrPostcode != null && street1 != null){
						return region.getStreetsIntersectStreets(street1);
					}
				}
				return null;
			}
		};
	}
	
	
	
	@Override
	public String getText(Street obj) {
		return obj.getName(region.useEnglishNames());
	}
	
	@Override
	public void itemSelected(Street obj) {
		settings.setLastSearchedIntersectedStreet(obj.getName(region.useEnglishNames()), obj.getLocation());
		finish();
	}
}
