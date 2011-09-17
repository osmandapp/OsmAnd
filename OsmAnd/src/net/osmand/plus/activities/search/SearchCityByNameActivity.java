package net.osmand.plus.activities.search;

import java.util.List;

import net.osmand.OsmAndFormatter;
import net.osmand.ResultMatcher;
import net.osmand.data.City;
import net.osmand.data.MapObject;
import net.osmand.data.PostCode;
import net.osmand.osm.LatLon;
import net.osmand.osm.MapUtils;
import net.osmand.plus.R;
import net.osmand.plus.RegionAddressRepository;
import net.osmand.plus.activities.OsmandApplication;
import android.os.AsyncTask;
import android.os.Message;
import android.view.View;
import android.widget.TextView;

public class SearchCityByNameActivity extends SearchByNameAbstractActivity<MapObject> {
	private RegionAddressRepository region;
	
	
	@Override
	public AsyncTask<Object, ?, ?> getInitializeTask() {
		return new AsyncTask<Object, MapObject, List<MapObject>>(){
			@Override
			protected void onPostExecute(List<MapObject> result) {
				((TextView)findViewById(R.id.Label)).setText(R.string.incremental_search_city);
				progress.setVisibility(View.INVISIBLE);
				finishInitializing(result);
			}
			
			@Override
			protected void onPreExecute() {
				((TextView)findViewById(R.id.Label)).setText(R.string.loading_cities);
				progress.setVisibility(View.VISIBLE);
			}
			
			
			@Override
			protected List<MapObject> doInBackground(Object... params) {
				region = ((OsmandApplication)getApplication()).getResourceManager().getRegionRepository(settings.getLastSearchedRegion());
				if(region != null){
					// preload cities
					region.preloadCities(new ResultMatcher<MapObject>() {
						
						@Override
						public boolean publish(MapObject object) {
							addObjectToInitialList(object);
							return true;
						}
						
						@Override
						public boolean isCancelled() {
							return false;
						}
					});
					return region.getLoadedCities();
				}
				return null;
			}
		};
	}
	
	@Override
	protected void filterLoop(String query, List<MapObject> list) {
		if(!initializeTaskIsFinished() || query.length() <= 2){
			super.filterLoop(query, list);
		} else {
			region.fillWithSuggestedCities(query, new ResultMatcher<MapObject>() {
				@Override
				public boolean isCancelled() {
					return namesFilter.isCancelled;
				}

				@Override
				public boolean publish(MapObject object) {
					Message msg = uiHandler.obtainMessage(MESSAGE_ADD_ENTITY, object);
					msg.sendToTarget();
					return true;
				}
			}, locationToSearch);
		}
	}

	
	@Override
	public String getText(MapObject obj) {
		LatLon l = obj.getLocation();
		if (getFilter().length() > 2 && locationToSearch != null && l != null) {
			return obj.getName(region.useEnglishNames()) + " - " + //$NON-NLS-1$
					OsmAndFormatter.getFormattedDistance((int) MapUtils.getDistance(l, locationToSearch), this); 
		} else {
			return obj.getName(region.useEnglishNames());
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
