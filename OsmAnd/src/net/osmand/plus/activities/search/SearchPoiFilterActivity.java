/**
 * 
 */
package net.osmand.plus.activities.search;

import java.util.ArrayList;
import java.util.List;

import net.osmand.data.LatLon;
import net.osmand.plus.NameFinderPoiFilter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.PoiFilter;
import net.osmand.plus.PoiFiltersHelper;
import net.osmand.plus.R;
import net.osmand.plus.SearchByNameFilter;
import net.osmand.plus.access.AccessibleToast;
import net.osmand.plus.activities.EditPOIFilterActivity;
import net.osmand.plus.activities.search.SearchActivity.SearchActivityChild;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.resources.ResourceManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.app.SherlockListFragment;


public class SearchPoiFilterActivity extends SherlockListFragment  implements SearchActivityChild {

	public static final String SEARCH_LAT = SearchActivity.SEARCH_LAT;
	public static final String SEARCH_LON = SearchActivity.SEARCH_LON;
	public static final int REQUEST_POI_EDIT = 55;
	
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		// ListActivity has a ListView, which you can get with:
		ListView lv = getListView();

		// Then you can create a listener like so:
		lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> av, View v, int pos, long id) {
				PoiFilter poi = ((AmenityAdapter) getListAdapter()).getItem(pos);
				if(!poi.isStandardFilter() || poi.getFilterId().equals(PoiFilter.CUSTOM_FILTER_ID)) {
					showEditActivity(poi);
					return true;
				}
				return false;
			}
		});
		
		refreshPoiListAdapter();
	}

	public void refreshPoiListAdapter() {
		PoiFiltersHelper poiFilters = getApp().getPoiFilters();
		List<PoiFilter> filters = new ArrayList<PoiFilter>() ;
		filters.addAll(poiFilters.getTopStandardFilters());
		filters.addAll(poiFilters.getUserDefinedPoiFilters());
		filters.addAll(poiFilters.getOsmDefinedPoiFilters());
		filters.add(poiFilters.getNameFinderPOIFilter());
		setListAdapter(new AmenityAdapter(filters));
	}
	
	public OsmandApplication getApp(){
		return (OsmandApplication) getSherlockActivity().getApplication();
	}
	
	
	private void updateIntentToLaunch(Intent intentToLaunch){
		LatLon loc = null;
		boolean searchAround = false;
		SherlockFragmentActivity parent = getSherlockActivity();
		if (loc == null && parent instanceof SearchActivity) {
			loc = ((SearchActivity) parent).getSearchPoint();
			searchAround = ((SearchActivity) parent).isSearchAroundCurrentLocation();
		}
		if (loc == null && !searchAround) {
			loc = getApp().getSettings().getLastKnownMapLocation();
		}
		if(loc != null && !searchAround) {
			intentToLaunch.putExtra(SearchActivity.SEARCH_LAT, loc.getLatitude());
			intentToLaunch.putExtra(SearchActivity.SEARCH_LON, loc.getLongitude());
		}
	}

	private void showEditActivity(PoiFilter poi) {
		Intent newIntent = new Intent(getSherlockActivity(), EditPOIFilterActivity.class);
		// folder selected
		newIntent.putExtra(EditPOIFilterActivity.AMENITY_FILTER, poi.getFilterId());
		updateIntentToLaunch(newIntent);
		startActivityForResult(newIntent, REQUEST_POI_EDIT);
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == REQUEST_POI_EDIT) {
			refreshPoiListAdapter();
		}
	}

	@Override
	public void onListItemClick(ListView parent, View v, int position, long id) {
		final PoiFilter filter = ((AmenityAdapter) getListAdapter()).getItem(position);
		if (filter.getFilterId().equals(PoiFilter.CUSTOM_FILTER_ID)) {
			filter.clearFilter();
			showEditActivity(filter);
			return;
		}
		if(!(filter instanceof NameFinderPoiFilter)){
			ResourceManager rm = getApp().getResourceManager();
			if(!rm.containsAmenityRepositoryToSearch(filter instanceof SearchByNameFilter)){
				AccessibleToast.makeText(getSherlockActivity(), R.string.data_to_search_poi_not_available, Toast.LENGTH_LONG);
				return;
			}
		}
		final Intent newIntent = new Intent(getSherlockActivity(), SearchPOIActivity.class);
		newIntent.putExtra(SearchPOIActivity.AMENITY_FILTER, filter.getFilterId());
		updateIntentToLaunch(newIntent);
		startActivityForResult(newIntent, 0);
	}



	class AmenityAdapter extends ArrayAdapter<PoiFilter> {
		AmenityAdapter(List<PoiFilter> list) {
			super(getSherlockActivity(), R.layout.searchpoifolder_list, list);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View row = convertView;
			if(row == null) {
				LayoutInflater inflater = getSherlockActivity().getLayoutInflater();
				row = inflater.inflate(R.layout.searchpoifolder_list, parent, false);
			}
			TextView label = (TextView) row.findViewById(R.id.folder_label);
			ImageView icon = (ImageView) row.findViewById(R.id.folder_icon);
			final PoiFilter model = getItem(position);
			label.setText(model.getName());
			if(model.getFilterId().equals(PoiFilter.CUSTOM_FILTER_ID)) {
				icon.setImageResource(android.R.drawable.ic_input_get);
			} else if (model.getFilterId().equals(PoiFilter.BY_NAME_FILTER_ID)) {
				icon.setImageResource(android.R.drawable.ic_search_category_default);
			} else {
				if(RenderingIcons.containsBigIcon(model.getSimplifiedId())) {
					icon.setImageDrawable(RenderingIcons.getBigIcon(getActivity(), model.getSimplifiedId()));
				} else {
					icon.setImageResource(R.drawable.mx_user_defined);
				}
			}
			ImageView editIcon = (ImageView) row.findViewById(R.id.folder_edit_icon);
			if (model.isStandardFilter()) {
				editIcon.setVisibility(View.GONE);
			} else {
				editIcon.setVisibility(View.VISIBLE);
			}
			editIcon.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					showEditActivity(model);
				}
			});
			
			return (row);
		}

	}

	@Override
	public void locationUpdate(LatLon l) {
	}

}
