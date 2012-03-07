package net.osmand.plus.activities.search;

import java.util.ArrayList;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.RegionAddressRepository;
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
		initialListToFilter = new ArrayList<RegionAddressRepository>(((OsmandApplication)getApplication()).getResourceManager().getAddressRepositories());
		NamesAdapter namesAdapter = new NamesAdapter(new ArrayList<RegionAddressRepository>(initialListToFilter)); //$NON-NLS-1$
		setListAdapter(namesAdapter);
	}
	
	
	@Override
	public String getText(RegionAddressRepository obj) {
		return obj.getName();
	}

	@Override
	public void itemSelected(RegionAddressRepository obj) {
		OsmandApplication.getSettings().setLastSearchedRegion(obj.getName(), obj.getEstimatedRegionCenter());
		finish();
	}
	
}
