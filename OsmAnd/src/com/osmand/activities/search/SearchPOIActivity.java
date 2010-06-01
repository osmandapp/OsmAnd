/**
 * 
 */
package com.osmand.activities.search;

import java.util.List;
import java.util.Map;

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
	private int maxCount = 100;

	private AmenityType amenityType;

	private AmenityAdapter amenityAdapter;

	private LatLon lastKnownMapLocation;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.searchpoi);
		searchPOILevel = (Button) findViewById(R.id.SearchPOILevelButton);
		searchPOILevel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				ResourceManager resourceManager = ResourceManager.getResourceManager();
				if ( zoom  > 7) {
					--zoom;
				}
				amenityList = resourceManager.searchAmenities(amenityType, lastKnownMapLocation.getLatitude(), lastKnownMapLocation
						.getLongitude(), zoom, -1);
				if (amenityList != null) {
					amenityAdapter.setNewModel(amenityList);
				}

			}
		});
		Bundle bundle = this.getIntent().getExtras();
		String anemity = bundle.getString(ANENITY_TYPE);
		if (anemity != null) {
			ResourceManager resourceManager = ResourceManager.getResourceManager();
			lastKnownMapLocation = OsmandSettings.getLastKnownMapLocation(this);
			amenityType = findAmenityType(anemity);
			amenityList = resourceManager.searchAmenities(amenityType, lastKnownMapLocation.getLatitude(), lastKnownMapLocation
					.getLongitude(), zoom, maxCount);
			if (amenityList != null) {
				amenityAdapter = new AmenityAdapter(amenityList);
				setListAdapter(amenityAdapter);
			}
		}
	}

	public void onListItemClick(ListView parent, View v, int position, long id) {
		SharedPreferences prefs = getSharedPreferences(OsmandSettings.SHARED_PREFERENCES_NAME, MODE_WORLD_READABLE);
		if (prefs != null) {
			Amenity amenity = amenityList.get(position);
			OsmandSettings.setLastKnownMapLocation(this, amenity.getLocation().getLatitude(), amenity.getLocation().getLongitude());
			Intent newIntent = new Intent(SearchPOIActivity.this, MapActivity.class);
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
			this.setNotifyOnChange(false);
		}

		public void setNewModel(List<?> amenityList) {
			((AmenityAdapter) getListAdapter()).clear();
			for(Object obj: amenityList){
				this.add(obj);
			}
			this.notifyDataSetChanged();
			
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			View row = convertView;
			if (row == null) {
				LayoutInflater inflater = getLayoutInflater();
				row = inflater.inflate(R.layout.searchpoi_list, parent, false);
			}
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
					String str = anemity.getStringWithoutType(OsmandSettings.usingEnglishNames(SearchPOIActivity.this));
					label.setText(str);
					icon.setImageResource(R.drawable.poi);
					distanceLabel.setText(" " + dist + " m  ");
				}
			}
			return (row);
		}
		
		private Object getModel(int position) {
			return (((AmenityAdapter) getListAdapter()).getItem(position));
		}
	}

}
