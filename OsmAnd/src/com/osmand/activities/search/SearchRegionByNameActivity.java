package com.osmand.activities.search;

import java.util.ArrayList;
import java.util.List;

import android.widget.TextView;

import com.osmand.OsmandSettings;
import com.osmand.RegionAddressRepository;
import com.osmand.ResourceManager;

public class SearchRegionByNameActivity extends SearchByNameAbstractActivity<RegionAddressRepository> {
	
	@Override
	public List<RegionAddressRepository> getObjects() {
		return new ArrayList<RegionAddressRepository>(ResourceManager.getResourceManager().getAddressRepositories());
	}
	
	@Override
	public void updateTextView(RegionAddressRepository obj, TextView txt) {
		txt.setText(obj.getName());
	}

	@Override
	public void itemSelected(RegionAddressRepository obj) {
		OsmandSettings.setLastSearchedRegion(this, obj.getName());
		finish();
		
	}
}
