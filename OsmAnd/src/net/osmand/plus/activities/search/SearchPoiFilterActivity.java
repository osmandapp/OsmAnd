/**
 * 
 */
package net.osmand.plus.activities.search;

import java.util.ArrayList;
import java.util.List;

import net.osmand.osm.LatLon;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.PoiFilter;
import net.osmand.plus.PoiFiltersHelper;
import net.osmand.plus.R;
import net.osmand.plus.activities.EditPOIFilterActivity;
import net.osmand.plus.activities.OsmandApplication;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;


/**
 * @author Maxim Frolov
 * 
 */
public class SearchPoiFilterActivity extends ListActivity {

	private Typeface typeFace;
	public final static String SEARCH_LAT = "search_lat";  //$NON-NLS-1$
	public final static String SEARCH_LON = "search_lon";  //$NON-NLS-1$
	
	private boolean searchNearBy = true;
	private double latitude = 0;
	private double longitude = 0;

	
	
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.searchpoilist);
		Bundle extras = getIntent().getExtras();
		if(extras != null && extras.containsKey(SEARCH_LAT) && extras.containsKey(SEARCH_LON)){
			searchNearBy = false;
			latitude = extras.getDouble(SEARCH_LAT);
			longitude = extras.getDouble(SEARCH_LON);
		} else {
			LatLon loc = OsmandSettings.getLastKnownMapLocation(OsmandSettings.getPrefs(this));
			latitude = loc.getLatitude();
			longitude = loc.getLongitude();
		}
		
		typeFace = Typeface.create((String)null, Typeface.ITALIC);
		
		// ListActivity has a ListView, which you can get with:
		ListView lv = getListView();

		// Then you can create a listener like so:
		lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> av, View v, int pos, long id) {
				PoiFilter poi = ((AmenityAdapter) getListAdapter()).getItem(pos);
				showEditActivity(poi);
				return true;
			}
		});
	}
	@Override
	protected void onResume() {
		super.onResume();
		PoiFiltersHelper poiFilters = ((OsmandApplication)getApplication()).getPoiFilters();
		List<PoiFilter> filters = new ArrayList<PoiFilter>(poiFilters.getUserDefinedPoiFilters()) ;
		filters.addAll(poiFilters.getOsmDefinedPoiFilters());
		filters.add(poiFilters.getNameFinderPOIFilter());
		setListAdapter(new AmenityAdapter(filters));
	}

	private void showEditActivity(PoiFilter poi) {
		if(!poi.isStandardFilter()) {
			Intent newIntent = new Intent(SearchPoiFilterActivity.this, EditPOIFilterActivity.class);
			// folder selected
			newIntent.putExtra(EditPOIFilterActivity.AMENITY_FILTER, poi.getFilterId());
			if(!searchNearBy){
				newIntent.putExtra(EditPOIFilterActivity.SEARCH_LAT, latitude);
				newIntent.putExtra(EditPOIFilterActivity.SEARCH_LON, longitude);
			}
			startActivityForResult(newIntent, 0);
		}
	}
	public void onListItemClick(ListView parent, View v, int position, long id) {
		final PoiFilter filter = ((AmenityAdapter) getListAdapter()).getItem(position);
		if(filter.getFilterId().equals(PoiFilter.CUSTOM_FILTER_ID)){
			showEditActivity(filter);
			return;
		}
		final Intent newIntent = new Intent(SearchPoiFilterActivity.this, SearchPOIActivity.class);
		newIntent.putExtra(SearchPOIActivity.AMENITY_FILTER, filter.getFilterId());
		if (searchNearBy) {
			AlertDialog.Builder b = new AlertDialog.Builder(this);
			b.setItems(new String[] { getString(R.string.search_nearby), getString(R.string.search_near_map) },
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							if (which == 1) {
								newIntent.putExtra(SearchPOIActivity.SEARCH_LAT, latitude);
								newIntent.putExtra(SearchPOIActivity.SEARCH_LON, longitude);
							}
							startActivityForResult(newIntent, 0);
						}
					});
			b.show();
		} else {
			newIntent.putExtra(SearchPOIActivity.SEARCH_LAT, latitude);
			newIntent.putExtra(SearchPOIActivity.SEARCH_LON, longitude);
			startActivityForResult(newIntent, 0);
		}
		
			
		
	}



	class AmenityAdapter extends ArrayAdapter<PoiFilter> {
		AmenityAdapter(List<PoiFilter> list) {
			super(SearchPoiFilterActivity.this, R.layout.searchpoi_list, list);
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			LayoutInflater inflater = getLayoutInflater();
			View row = inflater.inflate(R.layout.searchpoifolder_list, parent, false);
			TextView label = (TextView) row.findViewById(R.id.folder_label);
			ImageView icon = (ImageView) row.findViewById(R.id.folder_icon);
			PoiFilter model = getItem(position);
			label.setText(model.getName());
			if(model.getFilterId().equals(PoiFilter.CUSTOM_FILTER_ID)){
				label.setTypeface(typeFace);
			}
			icon.setImageResource(model.isStandardFilter() ? R.drawable.folder : R.drawable.favorites);
			return (row);
		}

	}
}
