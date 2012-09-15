package net.osmand.plus.activities.search;

import java.util.Comparator;
import java.util.List;

import net.osmand.Algoritms;
import net.osmand.ResultMatcher;
import net.osmand.data.Building;
import net.osmand.data.City;
import net.osmand.data.Street;
import net.osmand.osm.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.RegionAddressRepository;
import android.os.AsyncTask;
import android.view.View;

public class SearchBuildingByNameActivity extends SearchByNameAbstractActivity<Building> {
	private RegionAddressRepository region;
	private City city;
	private Street street;
	
	@Override
	protected Comparator<? super Building> createComparator() {
		return new Comparator<Building>() {
			@Override
			public int compare(Building o1, Building o2) {
				int i1 = Algoritms.extractFirstIntegerNumber(o1.getName());
				int i2 = Algoritms.extractFirstIntegerNumber(o2.getName());
				return i1 - i2;
			}
		};
	}
	
	@Override
	public AsyncTask<Object, ?, ?> getInitializeTask() {
		return new AsyncTask<Object, Void, List<Building>>(){
			@Override
			protected void onPostExecute(List<Building> result) {
				setLabelText(R.string.incremental_search_building);
				progress.setVisibility(View.INVISIBLE);
				finishInitializing(result);
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
				return hno + " [" + obj.getName(region.useEnglishNames())+"]";
			}
		}
		return obj.getName(region.useEnglishNames());
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
		LatLon loc = obj.getLocation();
		if(obj.getInterpolationInterval() > 0 || obj.getInterpolationType() != null){
			String hno = getCurrentFilter();
			float interpolation = obj.interpolation(hno);
			if (interpolation >= 0) {
				text = hno;
				if (interpolation > 0 && obj.getLatLon2() != null) {
					double lat1 = loc.getLatitude();
					double lat2 = obj.getLatLon2().getLatitude();
					double lon1 = loc.getLongitude();
					double lon2 = obj.getLatLon2().getLongitude();
					loc = new LatLon(interpolation * (lat2 - lat1) + lat1, interpolation * (lon2 - lon1) + lon1);
				}
			}
		}
		settings.setLastSearchedBuilding(text, loc);
		finish();
		
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



}
