/**
 * 
 */
package com.osmand.activities;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
import com.osmand.OsmandSettings;
import com.osmand.R;
import com.osmand.ResourceManager;
import com.osmand.data.Amenity;
import com.osmand.data.Amenity.AmenityType;
import com.osmand.osm.LatLon;
import com.osmand.osm.MapUtils;

/**
 * @author Maxim Frolov
 * 
 */
public class SearchActivity extends ListActivity {

	public static final String ANENITY_TYPE = "amenity_type";

	List<String> amenityList = new ArrayList<String>();
	Map<AmenityType, List<Amenity>> filter;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.search);
		createAmenityTypeList();
		Bundle bundle = this.getIntent().getExtras();
		String anemity = bundle.getString(ANENITY_TYPE);
		if (anemity != null) {
			AmenityType amenityType = findAmenityType(anemity);
			createAmenityFilter();
			List<Amenity> list = filter.get(amenityType);
			if(list != null) {
				setListAdapter(new AmenityAdapter(filter.get(amenityType)));
			}
		} else {
			setListAdapter(new AmenityAdapter(amenityList));
		}
	}

	private void createAmenityTypeList() {
		for (AmenityType type : AmenityType.values()) {
			amenityList.add(Algoritms.capitalizeFirstLetterAndLowercase(type.toString()));
		}

	}

	private void createAmenityFilter() {
		ResourceManager resourceManager = ResourceManager.getResourceManager();
		filter = new TreeMap<AmenityType, List<Amenity>>();
		LatLon lastKnownMapLocation = OsmandSettings.getLastKnownMapLocation(this);
		List<Amenity> closestAmenities = resourceManager.searchAmenities(lastKnownMapLocation.getLatitude(),
				lastKnownMapLocation.getLongitude(), 13, 500);
		MapUtils.sortListOfMapObject(closestAmenities, lastKnownMapLocation.getLatitude(), lastKnownMapLocation.getLongitude());
		for (Amenity n : closestAmenities) {
			AmenityType type = n.getType();
			if (!filter.containsKey(type)) {
				filter.put(type, new ArrayList<Amenity>());
			}
			filter.get(type).add(n);
		}

	}

	public void onListItemClick(ListView parent, View v, int position, long id) {
		AmenityType amenityType = findAmenityType(amenityList.get(position));
		// folder selected
		if (amenityType != null) {
			Bundle bundle = new Bundle();
			bundle.putString(ANENITY_TYPE, amenityList.get(position));
			Intent newIntent = new Intent(this.getApplicationContext(), SearchActivity.class);
			newIntent.putExtras(bundle);
			startActivityForResult(newIntent, 0);
		} else {
			// poi selected
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
			super(SearchActivity.this, R.layout.searchlist, (List<?>) list);
		}
		
		@Override
		public int getCount() {
			int c = super.getCount();
			return c > 20 ? 20 : c;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			LayoutInflater inflater = getLayoutInflater();
			View row = inflater.inflate(R.layout.searchlist, parent, false);
			TextView label = (TextView) row.findViewById(R.id.label);
			ImageView icon = (ImageView) row.findViewById(R.id.icon);
			Object model = getModel(position);
			if (model instanceof String) {
				label.setText((String) model);
				icon.setImageResource(R.drawable.folder);
			} else if (model instanceof Amenity) {
				Amenity anemity = (Amenity) model;
				if (anemity != null) {
					LatLon lastKnownMapLocation = OsmandSettings.getLastKnownMapLocation(SearchActivity.this);
					int dist = (int) (MapUtils.getDistance(anemity.getLocation(), lastKnownMapLocation.getLatitude(), lastKnownMapLocation
							.getLongitude()));
					String str = anemity.getStringWithoutType() + " [" + dist + " m ]";
					label.setText(str);
					icon.setImageResource(R.drawable.poi);
				}
			}
			return (row);
		}

		private Object getModel(int position) {
			return (((AmenityAdapter) getListAdapter()).getItem(position));
		}
	}

}
