package com.osmand.activities.search;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.widget.TextView;

import com.osmand.OsmandSettings;
import com.osmand.R;
import com.osmand.RegionAddressRepository;
import com.osmand.ResourceManager;

public class SearchRegionByNameActivity extends SearchByNameAbstractActivity<RegionAddressRepository> {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		((TextView)findViewById(R.id.Label)).setText(R.string.choose_available_region);
	}
	
	@Override
	public List<RegionAddressRepository> getObjects(String filter) {
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
	
	@Override
	public boolean isFilterableByDefault() {
		return true;
	}
}
