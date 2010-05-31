package com.osmand.activities.search;

import java.util.Collection;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;

import com.osmand.R;
import com.osmand.RegionAddressRepository;
import com.osmand.ResourceManager;

public class SearchRegionByNameActivity extends Activity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.search_by_name);
		Collection<RegionAddressRepository> repos = ResourceManager.getResourceManager().getAddressRepositories();
//		setListAdapter(new ArrayAdapter<T>);
	}
	
	/*class NamesAdapter extends ArrayAdapter<Object> {
		NamesAdapter(Object list) {
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
		}*/
}
