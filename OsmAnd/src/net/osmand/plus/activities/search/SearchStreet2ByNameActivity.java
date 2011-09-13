package net.osmand.plus.activities.search;

import java.util.ArrayList;
import java.util.List;

import net.osmand.data.City;
import net.osmand.data.PostCode;
import net.osmand.data.Street;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.RegionAddressRepository;
import net.osmand.plus.activities.OsmandApplication;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.widget.TextView;

public class SearchStreet2ByNameActivity extends SearchByNameAbstractActivity<Street> {
	private RegionAddressRepository region;
	private City city;
	private PostCode postcode;
	private Street street1;
	private List<Street> initialList = new ArrayList<Street>();
	private ProgressDialog progressDlg;
	private OsmandSettings settings;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		settings = OsmandSettings.getOsmandSettings(this);
		region = ((OsmandApplication)getApplication()).getResourceManager().getRegionRepository(settings.getLastSearchedRegion());
		if(region != null){
			postcode = region.getPostcode(settings.getLastSearchedPostcode());
			city = region.getCityById(settings.getLastSearchedCity());
			if(postcode != null){
				street1 = region.getStreetByName(postcode, (settings.getLastSearchedStreet()));
				if(street1 != null){
					city = street1.getCity();
				}
			} else if(city != null){
				street1 = region.getStreetByName(city, (settings.getLastSearchedStreet()));
			}
			if(city != null){
				startLoadDataInThread(getString(R.string.loading_streets));
			}
		}
		
		super.onCreate(savedInstanceState);
		((TextView)findViewById(R.id.Label)).setText(R.string.incremental_search_street);
	}
	
	@Override
	protected void onStop() {
		if(progressDlg != null){
			progressDlg.dismiss();
			progressDlg = null;
		}
		super.onStop();
	}
	
	protected void startLoadDataInThread(String progressMsg){
		progressDlg = ProgressDialog.show(this, getString(R.string.loading), progressMsg, true);
		new Thread("Loader search data") { //$NON-NLS-1$
			@Override
			public void run() {
				try {
					List<Street> t = new ArrayList<Street>();
					region.fillWithSuggestedStreetsIntersectStreets(city, street1, t);
					initialList = t;
				} finally {
					if(progressDlg != null){
						progressDlg.dismiss();
						progressDlg = null;
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								setText(getFilter().toString());
							}
						});
					}					
				}
			}
		}.start();
	}
	@Override
	public List<Street> getObjects(String filter) {
		int ind = 0;
		filter = filter.toLowerCase();
		List<Street> filterList = new ArrayList<Street>();
		if(filter.length() == 0){
			filterList.addAll(initialList);
			return filterList;
		}
		
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
		settings.setLastSearchedIntersectedStreet(obj.getName(region.useEnglishNames()), region.findStreetIntersection(street1, obj));
		finish();
		
	}
}
