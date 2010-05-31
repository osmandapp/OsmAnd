/**
 * 
 */
package com.osmand.activities.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.osmand.Algoritms;
import com.osmand.OsmandSettings;
import com.osmand.R;
import com.osmand.ResourceManager;
import com.osmand.activities.MapActivity;
import com.osmand.data.Amenity;
import com.osmand.data.Amenity.AmenityType;
import com.osmand.osm.LatLon;
import com.osmand.osm.MapUtils;

/**
 * @author Maxim Frolov
 * 
 */
public class SearchPOIActivity extends ListActivity {

	public static final String ANENITY_TYPE = "amenity_type";

	Map<AmenityType, List<Amenity>> filter;

	private List<Amenity> amenityList;

	private Button searchPOILevel;
	private int zoom = 12;

	private AmenityType amenityType;

	private AmenityAdapter amenityAdapter;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.searchpoi);
		searchPOILevel =  (Button) findViewById(R.id.SearchPOILevelButton);
		searchPOILevel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {

			}
		});
		Bundle bundle = this.getIntent().getExtras();
		String anemity = bundle.getString(ANENITY_TYPE);
		if (anemity != null) {
			amenityType = findAmenityType(anemity);
			createAmenityFilter(zoom);
			amenityList = filter.get(amenityType);
			if(amenityList != null) {
				amenityAdapter = new AmenityAdapter(amenityList);
				setListAdapter(amenityAdapter);
			}
		} 
	}



	private void createAmenityFilter(int zoom) {
		ResourceManager resourceManager = ResourceManager.getResourceManager();
		filter = new TreeMap<AmenityType, List<Amenity>>();
		LatLon lastKnownMapLocation = OsmandSettings.getLastKnownMapLocation(this);
		List<Amenity> closestAmenities = resourceManager.searchAmenities(lastKnownMapLocation.getLatitude(),
				lastKnownMapLocation.getLongitude(), zoom, 500);
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
		SharedPreferences prefs = getSharedPreferences(OsmandSettings.SHARED_PREFERENCES_NAME, MODE_WORLD_READABLE);
		if(prefs != null ){
			Amenity amenity = amenityList.get(position);
			OsmandSettings.setLastKnownMapLocation(this,amenity.getLocation().getLatitude(),amenity.getLocation().getLongitude());
			Intent newIntent = new Intent(this.getApplicationContext(), MapActivity.class);
			startActivity(newIntent);
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
			super(SearchPOIActivity.this, R.layout.searchpoi_list, (List<?>) list);
		}
		
		@Override
		public int getCount() {
			int c = super.getCount();
			return c > 20 ? 20 : c;
		}
		
		

		public View getView(int position, View convertView, ViewGroup parent) {
			LayoutInflater inflater = getLayoutInflater();
			View row = inflater.inflate(R.layout.searchpoi_list, parent, false);
			TextView label = (TextView) row.findViewById(R.id.poi_label);
			TextView distanceLabel = (TextView) row.findViewById(R.id.poidistance_label);
			ImageView icon = (ImageView) row.findViewById(R.id.poi_icon);
			Object model = getModel(position);
			if (model instanceof Amenity) {
				Amenity anemity = (Amenity) model;
				if (anemity != null) {
					LatLon lastKnownMapLocation = OsmandSettings.getLastKnownMapLocation(SearchPOIActivity.this);
					int dist = (int) (MapUtils.getDistance(anemity.getLocation(), lastKnownMapLocation.getLatitude(), lastKnownMapLocation
							.getLongitude()));
					String str = anemity.getStringWithoutType();
					label.setText(str);
					icon.setImageResource(R.drawable.poi);
					distanceLabel.setText(" " +dist + " m  ");
				}
			}
			return (row);
		}

		private Object getModel(int position) {
			return (((AmenityAdapter) getListAdapter()).getItem(position));
		}
	}

}
