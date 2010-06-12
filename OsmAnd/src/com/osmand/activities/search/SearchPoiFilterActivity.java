/**
 * 
 */
package com.osmand.activities.search;

import java.util.List;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.osmand.OsmandSettings;
import com.osmand.PoiFilter;
import com.osmand.PoiFiltersHelper;
import com.osmand.R;

/**
 * @author Maxim Frolov
 * 
 */
public class SearchPoiFilterActivity extends ListActivity {


	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.searchpoilist);
		List<PoiFilter> filters = PoiFiltersHelper.getOsmDefinedPoiFilters(this);
		setListAdapter(new AmenityAdapter(filters));
	}


	public void onListItemClick(ListView parent, View v, int position, long id) {
		PoiFilter filter = ((AmenityAdapter) getListAdapter()).getItem(position);
		Bundle bundle = new Bundle();
		Intent newIntent = new Intent(SearchPoiFilterActivity.this, SearchPOIActivity.class);
		// folder selected
		OsmandSettings.setPoiFilterForMap(this, filter.getFilterId());
		bundle.putString(SearchPOIActivity.AMENITY_FILTER, filter.getFilterId());
		newIntent.putExtras(bundle);
		startActivityForResult(newIntent, 0);
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
			icon.setImageResource(R.drawable.folder);
			return (row);
		}

	}
}
