/**
 * 
 */
package net.osmand.plus.activities.search;

import java.util.ArrayList;
import java.util.List;

import net.osmand.access.AccessibleToast;
import net.osmand.data.LatLon;
import net.osmand.plus.IconsCache;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.EditPOIFilterActivity;
import net.osmand.plus.activities.search.SearchActivity.SearchActivityChild;
import net.osmand.plus.poi.NameFinderPoiFilter;
import net.osmand.plus.poi.PoiFiltersHelper;
import net.osmand.plus.poi.PoiLegacyFilter;
import net.osmand.plus.poi.SearchByNameFilter;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.resources.ResourceManager;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.ListFragment;
import android.support.v7.widget.PopupMenu;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;



public class SearchPoiFilterFragment extends ListFragment implements SearchActivityChild {

	public static final String SEARCH_LAT = SearchActivity.SEARCH_LAT;
	public static final String SEARCH_LON = SearchActivity.SEARCH_LON;
	public static final int REQUEST_POI_EDIT = 55;

	private EditText searchEditText;
	private SearchPoiByNameTask currentTask = null;
	private PoiFiltersAdapter poiFitlersAdapter;
	
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.searchpoi, container, false);
        
        v.findViewById(R.id.SearchFilterLayout).setVisibility(View.VISIBLE);
        setupSearchEditText((EditText) v.findViewById(R.id.edit));
        setupOptions(v.findViewById(R.id.options));
        v.findViewById(R.id.poiSplitbar).setVisibility(View.GONE);
        return v;
    }
	
	private void setupOptions(View options) {
		options.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showOptionsMenu(v);
			}
		});
	}

	private void setupSearchEditText(EditText e) {
		searchEditText = e;
		searchEditText.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void afterTextChanged(Editable s) {
			}
		});
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		refreshPoiListAdapter();
		setHasOptionsMenu(true);
	}

	public void refreshPoiListAdapter() {
		PoiFiltersHelper poiFilters = getApp().getPoiFilters();
		List<PoiLegacyFilter> filters = new ArrayList<PoiLegacyFilter>() ;
		filters.addAll(poiFilters.getTopDefinedPoiFilters());
		poiFitlersAdapter = new PoiFiltersAdapter(filters);
		setListAdapter(poiFitlersAdapter);
	}
	
	public OsmandApplication getApp(){
		return (OsmandApplication) getActivity().getApplication();
	}
	
	
	private void updateIntentToLaunch(Intent intentToLaunch){
		LatLon loc = null;
		boolean searchAround = false;
		FragmentActivity parent = getActivity();
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

	private void showEditActivity(PoiLegacyFilter poi) {
		Intent newIntent = new Intent(getActivity(), EditPOIFilterActivity.class);
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
	public void onListItemClick(ListView listView, View v, int position, long id) {
		final PoiLegacyFilter filter = ((PoiFiltersAdapter) getListAdapter()).getItem(position);
		if(!(filter instanceof NameFinderPoiFilter)){
			ResourceManager rm = getApp().getResourceManager();
			if(!rm.containsAmenityRepositoryToSearch(filter instanceof SearchByNameFilter)){
				AccessibleToast.makeText(getActivity(), R.string.data_to_search_poi_not_available, Toast.LENGTH_LONG);
				return;
			}
		}
		showFilterActivity(filter);
	}

	private void showFilterActivity(final PoiLegacyFilter filter) {
		final Intent newIntent = new Intent(getActivity(), SearchPOIActivity.class);
		newIntent.putExtra(SearchPOIActivity.AMENITY_FILTER, filter.getFilterId());
		updateIntentToLaunch(newIntent);
		startActivityForResult(newIntent, 0);
	}

	class SearchPoiByNameTask extends AsyncTask<Void, PoiLegacyFilter, List<PoiLegacyFilter>> {

		@Override
		protected List<PoiLegacyFilter> doInBackground(Void... params) {
			return null;
		}
		
		@Override
		protected void onPostExecute(List<PoiLegacyFilter> result) {
			super.onPostExecute(result);
		}
		
	}


	class PoiFiltersAdapter extends ArrayAdapter<PoiLegacyFilter> {
		

		PoiFiltersAdapter(List<PoiLegacyFilter> list) {
			super(getActivity(), R.layout.searchpoifolder_list, list);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View row = convertView;
			if(row == null) {
				LayoutInflater inflater = getActivity().getLayoutInflater();
				row = inflater.inflate(R.layout.searchpoifolder_list, parent, false);
			}
			TextView label = (TextView) row.findViewById(R.id.folder_label);
			ImageView icon = (ImageView) row.findViewById(R.id.folder_icon);
			OsmandApplication app = getMyApplication();
			final PoiLegacyFilter model = getItem(position);
			label.setText(model.getName());
			IconsCache iconsCache = app.getIconsCache();
			if(model.getFilterId().equals(PoiLegacyFilter.CUSTOM_FILTER_ID)) {
				icon.setImageDrawable(iconsCache.getContentIcon(R.drawable.ic_action_filter_dark));
			} else if (model.getFilterId().equals(PoiLegacyFilter.BY_NAME_FILTER_ID)) {
				icon.setImageResource(android.R.drawable.ic_search_category_default);
			} else {
				if(RenderingIcons.containsBigIcon(model.getSimplifiedId())) {
					icon.setImageDrawable(RenderingIcons.getBigIcon(getActivity(), model.getSimplifiedId()));
				} else {
					icon.setImageResource(R.drawable.mx_user_defined);
				}
			}
			return (row);
		}
	}
	
	private void showOptionsMenu(View v) {
		// Show menu with search all, name finder, name finder poi
		IconsCache iconsCache = getMyApplication().getIconsCache();
		final PopupMenu optionsMenu = new PopupMenu(getActivity(), v);

		MenuItem item = optionsMenu.getMenu().add(R.string.poi_filter_custom_filter)
				.setIcon(iconsCache.getContentIcon(R.drawable.ic_action_filter_dark));
		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				PoiLegacyFilter filter = getApp().getPoiFilters().getFilterById(PoiLegacyFilter.CUSTOM_FILTER_ID);
				if(filter != null) {
					filter.clearFilter();
					showEditActivity(filter);
				}
				return true;
			}
		});
		optionsMenu.show();

	}

	@Override
	public void onCreateOptionsMenu(Menu onCreate, MenuInflater inflater) {
		if(getActivity() instanceof SearchActivity) {
			((SearchActivity) getActivity()).getClearToolbar(false);
		}
	}

	@Override
	public void locationUpdate(LatLon l) {
	}

	public OsmandApplication getMyApplication() {
		return (OsmandApplication) getActivity().getApplication();
	}

}
