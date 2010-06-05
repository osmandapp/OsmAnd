/**
 * 
 */
package com.osmand.activities.search;

import java.util.List;

import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

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

	public static final String AMENITY_TYPE = "amenity_type";

	private List<Amenity> amenityList;

	private Button searchPOILevel;
	private final static int maxCount = 100;
	private final static int finalZoom = 8;
	private int zoom = 13;

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
				if (zoom > finalZoom) {
					--zoom;
				}
				amenityList = resourceManager.searchAmenities(amenityType, lastKnownMapLocation.getLatitude(), lastKnownMapLocation
						.getLongitude(), zoom, -1);
				if (amenityList != null) {
					MapUtils.sortListOfMapObject(amenityList, lastKnownMapLocation.getLatitude(), lastKnownMapLocation.getLongitude());
					amenityAdapter.setNewModel(amenityList);
				}
				searchPOILevel.setEnabled(zoom > finalZoom);

			}
		});

		Bundle bundle = this.getIntent().getExtras();
		String anemity = bundle.getString(AMENITY_TYPE);
		if (anemity != null) {
			ResourceManager resourceManager = ResourceManager.getResourceManager();
			lastKnownMapLocation = OsmandSettings.getLastKnownMapLocation(this);
			amenityType = findAmenityType(anemity);
			if (amenityType != null) {
				amenityList = resourceManager.searchAmenities(amenityType, lastKnownMapLocation.getLatitude(), lastKnownMapLocation
						.getLongitude(), zoom, maxCount);
			} else {
				amenityList = resourceManager.searchAmenities(amenityType, lastKnownMapLocation.getLatitude(), lastKnownMapLocation
						.getLongitude(), zoom + 2, maxCount);
			}

			if (amenityList != null) {
				MapUtils.sortListOfMapObject(amenityList, lastKnownMapLocation.getLatitude(), lastKnownMapLocation.getLongitude());
				if(amenityType == null){
					while (amenityList.size() > 30) {
						amenityList.remove(amenityList.size() - 1);
					}
				}
				amenityAdapter = new AmenityAdapter(amenityList);
				setListAdapter(amenityAdapter);
			}
		}
		// ListActivity has a ListView, which you can get with:
		ListView lv = getListView();

		// Then you can create a listener like so:
		lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> av, View v, int pos, long id) {
				Amenity amenity = amenityList.get(pos);
				String format = amenity.getSimpleFormat(OsmandSettings.usingEnglishNames(v.getContext()));
				if (amenity.getOpeningHours() != null) {
					format += "\nOpening hours : " + amenity.getOpeningHours();
				}
				Toast.makeText(v.getContext(), format, Toast.LENGTH_LONG).show();
				return true;
			}
		});
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
			setNotifyOnChange(false);
			((AmenityAdapter) getListAdapter()).clear();
			for (Object obj : amenityList) {
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
					if(anemity.getOpeningHours() != null) {
						icon.setImageResource(R.drawable.poi);
					} else{
						icon.setImageResource(R.drawable.closed_poi);
					}
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
