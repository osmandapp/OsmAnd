package com.osmand.activities.search;

import java.util.ArrayList;
import java.util.List;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.widget.TextView;

import com.osmand.OsmandSettings;
import com.osmand.R;
import com.osmand.RegionAddressRepository;
import com.osmand.ResourceManager;
import com.osmand.data.City;
import com.osmand.data.PostCode;
import com.osmand.data.Street;

public class SearchStreet2ByNameActivity extends SearchByNameAbstractActivity<Street> {
	private RegionAddressRepository region;
	private City city;
	private PostCode postcode;
	private Street street1;
	volatile private List<Street> initialList = new ArrayList<Street>();
	private List<Street> filterList = new ArrayList<Street>();
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		region = ResourceManager.getResourceManager().getRegionRepository(OsmandSettings.getLastSearchedRegion(this));
		if(region != null){
			postcode = region.getPostcode(OsmandSettings.getLastSearchedPostcode(this));
			city = region.getCityById(OsmandSettings.getLastSearchedCity(this));
			if(postcode != null){
				street1 = region.getStreetByName(postcode, (OsmandSettings.getLastSearchedStreet(this)));
				if(street1 != null){
					city = street1.getCity();
				}
			} else if(city != null){
				street1 = region.getStreetByName(city, (OsmandSettings.getLastSearchedStreet(this)));
			}
			if(city != null){
				startLoadDataInThread(getString(R.string.loading_streets));
			}
		}
		
		super.onCreate(savedInstanceState);
		((TextView)findViewById(R.id.Label)).setText(R.string.incremental_search_street);
	}
	
	
	protected void startLoadDataInThread(String progressMsg){
		final ProgressDialog dlg = ProgressDialog.show(this, getString(R.string.loading), progressMsg, true);
		new Thread("Loader search data") { //$NON-NLS-1$
			@Override
			public void run() {
				try {
					List<Street> t = new ArrayList<Street>();
					region.fillWithSuggestedStreetsIntersectStreets(city, street1, t);
					initialList = t;
				} finally {
					dlg.dismiss();
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							setText(getFilter().toString());
						}
					});
				}
			}
		}.start();
	}
	@Override
	public List<Street> getObjects(String filter) {
		int ind = 0;
		filter = filter.toLowerCase();
		if(filter.length() == 0){
			return initialList;
		}
		filterList.clear();
		for (Street s : initialList) {
			String lowerCase = s.getName(region.useEnglishNames()).toLowerCase();
			if (lowerCase.startsWith(filter)) {
				filterList.add(ind, s);
				ind++;
			} else if (lowerCase.contains(filter)) {
				filterList.add(s);
			}
		}
		return filterList;
	}
	
	@Override
	public void updateTextView(Street obj, TextView txt) {
		txt.setText(obj.getName(region.useEnglishNames()));
	}
	
	@Override
	public void itemSelected(Street obj) {
		OsmandSettings.setLastSearchedIntersectedStreet(this, obj.getName(region.useEnglishNames()));
		finish();
		
	}
}
