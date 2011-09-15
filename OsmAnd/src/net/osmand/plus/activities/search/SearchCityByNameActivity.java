package net.osmand.plus.activities.search;

import java.util.ArrayList;
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
import android.view.View;
import android.widget.TextView;

public class SearchCityByNameActivity extends SearchByNameAbstractActivity<MapObject> {
	private RegionAddressRepository region;
	
	
	@Override
	public AsyncTask<Object, ?, ?> getInitializeTask() {
		return new AsyncTask<Object, MapObject, Void>(){
			@Override
			protected void onPostExecute(Void result) {
				((TextView)findViewById(R.id.Label)).setText(R.string.incremental_search_city);
				progress.setVisibility(View.INVISIBLE);
				updateSearchText();
			}
			
			@Override
			protected void onPreExecute() {
				((TextView)findViewById(R.id.Label)).setText(R.string.loading_cities);
				progress.setVisibility(View.VISIBLE);
			}
			
			@Override
			protected void onProgressUpdate(MapObject... values) {
				if (hasWindowFocus()) {
					for (MapObject t : values) {
						((NamesAdapter) getListAdapter()).add(t);
					}
				}
			}
			
			@Override
			protected Void doInBackground(Object... params) {
				region = ((OsmandApplication)getApplication()).getResourceManager().getRegionRepository(settings.getLastSearchedRegion());
				if(region != null){
					// preload cities
					region.fillWithSuggestedCities("", new ResultMatcher<MapObject>() {
						
						@Override
						public boolean publish(MapObject object) {
							publishProgress(object);
							return true;
						}
						
						@Override
						public boolean isCancelled() {
							return false;
						}
					}, locationToSearch);
				}
				return null;
			}
		};
	}
	
	@Override
	public List<MapObject> getObjects(String filter, final SearchByNameTask task) {
		if(region != null){
			return region.fillWithSuggestedCities(filter, new ResultMatcher<MapObject>() {
				
				@Override
				public boolean publish(MapObject object) {
					task.progress(object);
					return true;
				}
				
				@Override
				public boolean isCancelled() {
					return task.isCancelled();
				}
			}, locationToSearch);
		}
		return new ArrayList<MapObject>();
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
