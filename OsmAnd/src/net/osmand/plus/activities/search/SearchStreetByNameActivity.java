package net.osmand.plus.activities.search;

import java.util.ArrayList;
import java.util.List;

import net.osmand.ResultMatcher;
import net.osmand.data.City;
import net.osmand.data.PostCode;
import net.osmand.data.Street;
import net.osmand.plus.R;
import net.osmand.plus.RegionAddressRepository;
import net.osmand.plus.activities.OsmandApplication;
import android.os.AsyncTask;
import android.view.View;
import android.widget.TextView;

public class SearchStreetByNameActivity extends SearchByNameAbstractActivity<Street> {
	private RegionAddressRepository region;
	private City city;
	private PostCode postcode;
	
	
	@Override
	public AsyncTask<Object, ?, ?> getInitializeTask() {
		return new AsyncTask<Object, Void, Void>(){
			@Override
			protected void onPostExecute(Void result) {
				((TextView)findViewById(R.id.Label)).setText(R.string.incremental_search_street);
				progress.setVisibility(View.INVISIBLE);
				resetText();
			}
			
			@Override
			protected void onPreExecute() {
				((TextView)findViewById(R.id.Label)).setText(R.string.loading_streets);
				progress.setVisibility(View.VISIBLE);
			}
			@Override
			protected Void doInBackground(Object... params) {
				region = ((OsmandApplication)getApplication()).getResourceManager().getRegionRepository(settings.getLastSearchedRegion());
				if(region != null){
					postcode = region.getPostcode(settings.getLastSearchedPostcode());
					if (postcode == null) {
						city = region.getCityById(settings.getLastSearchedCity());
					}
				}
				return null;
			}
		};
	}
	
	@Override
	public List<Street> getObjects(String filter, final SearchByNameTask task) {
		if (city != null || postcode != null) {
			return region.fillWithSuggestedStreets(postcode == null ? city : postcode, new ResultMatcher<Street>() {
				@Override
				public boolean publish(Street object) {
					task.progress(object);
					return true;
				}
			}, filter);
		}
		return new ArrayList<Street>();
	}
	
	@Override
	public void updateTextView(Street obj, TextView txt) {
		txt.setText(obj.getName(region.useEnglishNames()));
	}
	
	@Override
	public void itemSelected(Street obj) {
		settings.setLastSearchedStreet(obj.getName(region.useEnglishNames()), obj.getLocation());
		finish();
		
	}
}
