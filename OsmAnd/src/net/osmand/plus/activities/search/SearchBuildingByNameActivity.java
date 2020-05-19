package net.osmand.plus.activities.search;

import java.util.Comparator;
import java.util.List;

import android.widget.AdapterView;
import net.osmand.ResultMatcher;
import net.osmand.data.Building;
import net.osmand.data.City;
import net.osmand.data.LatLon;
import net.osmand.data.Street;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.search.SearchAddressFragment.AddressInformation;
import net.osmand.plus.resources.RegionAddressRepository;
import net.osmand.util.Algorithms;
import android.os.AsyncTask;
import android.view.View;
import android.widget.Toast;

public class SearchBuildingByNameActivity extends SearchByNameAbstractActivity<Building> {
	private RegionAddressRepository region;
	private City city;
	private Street street;
	
	@Override
	protected Comparator<? super Building> createComparator() {
		return new Comparator<Building>() {
			@Override
			public int compare(Building o1, Building o2) {
				int i1 = Algorithms.extractFirstIntegerNumber(o1.getName());
				int i2 = Algorithms.extractFirstIntegerNumber(o2.getName());
				return i1 - i2;
			}
		};
	}

	@Override
	protected void reset() {
		//This is really only a "clear input text field", hence do not reset settings here
		//osmandSettings.setLastSearchedBuilding("", null);
		super.reset();
	}

	@Override
	public AsyncTask<Object, ?, ?> getInitializeTask() {
		return new AsyncTask<Object, Void, List<Building>>(){
			@Override
			protected void onPostExecute(List<Building> result) {
				setLabelText(R.string.incremental_search_building);
				progress.setVisibility(View.INVISIBLE);
				finishInitializing(result);
				if (result == null || result.isEmpty()) {
					Toast.makeText(SearchBuildingByNameActivity.this, 
							R.string.no_buildings_found, Toast.LENGTH_LONG).show();
                    quitActivity(SearchStreet2ByNameActivity.class);
				}
			}
			
			@Override
			protected void onPreExecute() {
				setLabelText(R.string.loading_streets_buildings);
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
		if(obj.getInterpolationInterval() > 0 || obj.getInterpolationType() != null){
			String hno = getCurrentFilter();
			if(hno.length() > 0 && obj.belongsToInterpolation(hno)) {
				return hno + " [" + obj.getName(region.getLang(), region.isTransliterateNames())+"]";
			}
		}
		return obj.getName(region.getLang(), region.isTransliterateNames());
	}
	
	@Override
	public String getShortText(Building obj) {
		if(obj.getInterpolationInterval() > 0 || obj.getInterpolationType() != null){
			return "";
		}
		return super.getShortText(obj);
	}
	
	
	@Override
	public void itemSelected(Building obj) {
		String text = getText(obj);
		String hno = getCurrentFilter();
		LatLon loc = obj.getLocation();
		float interpolation = obj.interpolation(hno);
		if(interpolation >= 0) {
			loc = obj.getLocation(interpolation);
			text = hno;
		}
		settings.setLastSearchedBuilding(text, loc);
		if(isSelectAddres()) {
			finish();
		} else {
			showOnMap(loc, AddressInformation.buildBuilding(this, settings));
		}
	}
	
	@Override
	protected AddressInformation getAddressInformation() {
		return AddressInformation.buildStreet(this, settings);
	}
	
	
	@Override
	public boolean filterObject(Building obj, String filter){
		if(super.filterObject(obj, filter)){
			return true;
		}
		if(obj.belongsToInterpolation(filter)){
			return true;
		}
		return false;
		
	}


	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

	}
}
