/**
 * 
 */
package com.osmand.activities.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

import com.osmand.Algoritms;
import com.osmand.R;
import com.osmand.data.Amenity;
import com.osmand.data.Amenity.AmenityType;

/**
 * @author Maxim Frolov
 * 
 */
public class SearchPOIListActivity extends ListActivity {


	List<String> amenityList = new ArrayList<String>();
	Map<AmenityType, List<Amenity>> filter;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.searchpoilist);
		createAmenityTypeList();
		setListAdapter(new AmenityAdapter(amenityList));

	}

	private void createAmenityTypeList() {
		for (AmenityType type : AmenityType.values()) {
			amenityList.add(Algoritms.capitalizeFirstLetterAndLowercase(type.toString()));
		}

	}

	public void onListItemClick(ListView parent, View v, int position, long id) {
		AmenityType amenityType = findAmenityType(amenityList.get(position));
		// folder selected
		if (amenityType != null) {
			Bundle bundle = new Bundle();
			bundle.putString(SearchPOIActivity.ANENITY_TYPE, amenityList.get(position));
			Intent newIntent = new Intent(this.getApplicationContext(), SearchPOIActivity.class);
			newIntent.putExtras(bundle);
			startActivityForResult(newIntent, 0);
		}
	}

	private AmenityType findAmenityType(String string) {
		for (AmenityType type : AmenityType.values()) {
			if (string.equals(Algoritms.capitalizeFirstLetterAndLowercase(type.toString()))) {
				return type;
			}
		}
		return null;

	}

	@SuppressWarnings("unchecked")
	class AmenityAdapter extends ArrayAdapter {
		AmenityAdapter(Object list) {
			super(SearchPOIListActivity.this, R.layout.searchpoi_list, (List<?>) list);
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			LayoutInflater inflater = getLayoutInflater();
			View row = inflater.inflate(R.layout.searchpoifolder_list, parent, false);
			TextView label = (TextView) row.findViewById(R.id.folder_label);
			ImageView icon = (ImageView) row.findViewById(R.id.folder_icon);
			Object model = getModel(position);
			label.setText((String) model);
			icon.setImageResource(R.drawable.folder);
			return (row);
		}

		private Object getModel(int position) {
			return (((AmenityAdapter) getListAdapter()).getItem(position));
		}
	}
}
