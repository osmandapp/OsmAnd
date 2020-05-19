package net.osmand.plus.activities.search;

import java.util.Comparator;
import java.util.List;

import net.osmand.data.City;
import net.osmand.data.MapObject.MapObjectComparator;
import net.osmand.data.Street;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.search.SearchAddressFragment.AddressInformation;
import net.osmand.plus.resources.RegionAddressRepository;
import android.os.AsyncTask;
import android.view.View;

public class SearchStreet2ByNameActivity extends SearchByNameAbstractActivity<Street> {
	private RegionAddressRepository region;
	private City cityOrPostcode;
	private Street street1;
	private OsmandSettings osmandSettings;

	@Override
	protected Comparator<? super Street> createComparator() {
		return new MapObjectComparator(getMyApplication().getSettings().MAP_PREFERRED_LOCALE.get(),
				getMyApplication().getSettings().MAP_TRANSLITERATE_NAMES.get());
	}

	@Override
	protected void reset() {
		//This is really only a "clear input text field", hence do not reset settings here
		//osmandSettings.setLastSearchedIntersectedStreet("", null);
		super.reset();
	}

	@Override
	public AsyncTask<Object, ?, ?> getInitializeTask() {
		return new AsyncTask<Object, Void, List<Street>>(){
			@Override
			protected void onPostExecute(List<Street> result) {
				setLabelText(R.string.incremental_search_street);
				progress.setVisibility(View.INVISIBLE);
				finishInitializing(result);
			}
			
			@Override
			protected void onPreExecute() {
				setLabelText(R.string.loading_streets);
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
	protected AddressInformation getAddressInformation() {
		return AddressInformation.buildStreet(this, settings);
	}	
	
	@Override
	public String getText(Street obj) {
		return obj.getName(region.getLang(), region.isTransliterateNames());
	}
	
	@Override
	public void itemSelected(Street obj) {
		settings.setLastSearchedIntersectedStreet(obj.getName(region.getLang(), region.isTransliterateNames()), obj.getLocation());
		if(isSelectAddres()) {
			finish();
		} else {
			showOnMap(obj.getLocation(), AddressInformation.build2StreetIntersection(this, settings));
		}
	}
}
