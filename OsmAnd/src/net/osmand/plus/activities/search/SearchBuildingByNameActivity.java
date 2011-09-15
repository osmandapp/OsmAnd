package net.osmand.plus.activities.search;

import java.util.ArrayList;
import java.util.List;

import net.osmand.ResultMatcher;
import net.osmand.data.Building;
import net.osmand.data.City;
import net.osmand.data.PostCode;
import net.osmand.data.Street;
import net.osmand.plus.R;
import net.osmand.plus.RegionAddressRepository;
import net.osmand.plus.activities.OsmandApplication;
import android.os.AsyncTask;
import android.view.View;
import android.widget.TextView;

public class SearchBuildingByNameActivity extends SearchByNameAbstractActivity<Building> {
	private RegionAddressRepository region;
	private City city;
	private Street street;
	private PostCode postcode;
	
	@Override
	public AsyncTask<Object, ?, ?> getInitializeTask() {
		return new AsyncTask<Object, Void, Void>(){
			@Override
			protected void onPostExecute(Void result) {
				((TextView)findViewById(R.id.Label)).setText(R.string.incremental_search_building);
				progress.setVisibility(View.INVISIBLE);
				updateSearchText();
			}
			
			@Override
			protected void onPreExecute() {
				((TextView)findViewById(R.id.Label)).setText(R.string.loading_streets_buildings);
				progress.setVisibility(View.VISIBLE);
			}
			@Override
			protected Void doInBackground(Object... params) {
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
				if(street != null){
					// preload here to avoid concurrent modification
					region.fillWithSuggestedBuildings(postcode, street, "", null);
				}
				
				return null;
			}
		};
	}
	
	@Override
	public List<Building> getObjects(String filter, final SearchByNameTask task) {
		if(street != null){
			return region.fillWithSuggestedBuildings(postcode, street, filter, new ResultMatcher<Building>() {
				@Override
				public boolean publish(Building object) {
					task.progress(object);
					return true;
				}
				@Override
				public boolean isCancelled() {
					return task.isCancelled();
				}
			});
		}
		return new ArrayList<Building>();
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
