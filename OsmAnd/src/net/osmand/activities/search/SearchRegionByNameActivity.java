package net.osmand.activities.search;

import java.util.ArrayList;
import java.util.List;

import net.osmand.OsmandSettings;
import net.osmand.R;
import net.osmand.RegionAddressRepository;
import net.osmand.activities.OsmandApplication;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;


public class SearchRegionByNameActivity extends SearchByNameAbstractActivity<RegionAddressRepository> {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		((TextView)findViewById(R.id.Label)).setText(R.string.choose_available_region);
		if(((OsmandApplication)getApplication()).getResourceManager().getAddressRepositories().isEmpty()){
			Toast.makeText(this, R.string.none_region_found, Toast.LENGTH_LONG).show();
		}
	}
	
	@Override
	public List<RegionAddressRepository> getObjects(String filter) {
		return new ArrayList<RegionAddressRepository>(((OsmandApplication)getApplication()).getResourceManager().getAddressRepositories());
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
