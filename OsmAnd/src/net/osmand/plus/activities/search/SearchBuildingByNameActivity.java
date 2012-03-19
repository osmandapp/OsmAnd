package net.osmand.plus.activities.search;

import java.util.List;

import net.osmand.ResultMatcher;
import net.osmand.data.Building;
import net.osmand.data.City;
import net.osmand.data.Street;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.RegionAddressRepository;
import android.os.AsyncTask;
import android.view.View;
import android.widget.TextView;

public class SearchBuildingByNameActivity extends SearchByNameAbstractActivity<Building> {
	private RegionAddressRepository region;
	private City city;
	private Street street;
	
	@Override
	public AsyncTask<Object, ?, ?> getInitializeTask() {
		return new AsyncTask<Object, Void, List<Building>>(){
			@Override
			protected void onPostExecute(List<Building> result) {
				((TextView)findViewById(R.id.Label)).setText(R.string.incremental_search_building);
				progress.setVisibility(View.INVISIBLE);
				finishInitializing(result);
			}
			
			@Override
			protected void onPreExecute() {
				((TextView)findViewById(R.id.Label)).setText(R.string.loading_streets_buildings);
				progress.setVisibility(View.VISIBLE);
			}
			@Override
			protected List<Building> doInBackground(Object... params) {
				region = ((OsmandApplication)getApplication()).getResourceManager().getRegionRepository(settings.getLastSearchedRegion());
				if(region != null){
					city = region.getCityById(settings.getLastSearchedCity(), settings.getLastSearchedCityName());
					street = region.getStreetByName(city, settings.getLastSearchedStreet());
				}
				if(street != null){
					// preload here to avoid concurrent modification
					region.preloadBuildings(street, new ResultMatcher<Building>() {
						@Override
						public boolean isCancelled() {
							return false;
						}

						@Override
						public boolean publish(Building object) {
							addObjectToInitialList(object);
							return true;
						}
					});
					return street.getBuildings();
				}
				return null;
			}
		};
	}
	
	@Override
	public String getText(Building obj) {
		return obj.getName(region.useEnglishNames());
	}
	
	@Override
	public void itemSelected(Building obj) {
		settings.setLastSearchedBuilding(obj.getName(region.useEnglishNames()), obj.getLocation());
		finish();
		
	}



}
