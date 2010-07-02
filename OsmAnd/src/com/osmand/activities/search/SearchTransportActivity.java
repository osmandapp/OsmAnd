/**
 * 
 */
package com.osmand.activities.search;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.AlertDialog.Builder;
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

import com.osmand.Messages;
import com.osmand.OsmandSettings;
import com.osmand.R;
import com.osmand.ResourceManager;
import com.osmand.TransportIndexRepository;
import com.osmand.TransportIndexRepository.RouteInfoLocation;
import com.osmand.data.TransportRoute;
import com.osmand.data.TransportStop;
import com.osmand.osm.LatLon;
import com.osmand.osm.MapUtils;

/**
 * @author Maxim Frolov
 * 
 */
public class SearchTransportActivity extends ListActivity {



	private Button searchTransportLevel;
	private TransportStopAdapter stopsAdapter;
	private LatLon lastKnownMapLocation;
	private LatLon locationToGo;
	private TextView searchArea;
	private TransportIndexRepository repo;
	
	private final static int finalZoom = 13;
	private final static int initialZoom = 17;
	private int zoom = initialZoom;

	
	

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.search_transport);
		searchTransportLevel = (Button) findViewById(R.id.SearchPOILevelButton);
		searchArea = (TextView) findViewById(R.id.SearchAreaText);
		searchTransportLevel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(isSearchFurtherAvailable()){
					zoom --;
					searchFurther();
				}
			}
		});

		stopsAdapter = new TransportStopAdapter(new ArrayList<RouteInfoLocation>());
		setListAdapter(stopsAdapter);

	}
	
	public String getSearchArea(){
		// TODO
		if(zoom <= 14){
			int d = (int) (1 * (1 << (14 - zoom)));
			return " < " + d + " " + Messages.getMessage(Messages.KEY_KM);  //$NON-NLS-1$//$NON-NLS-2$
		} else {
			return " < 500 " + Messages.getMessage(Messages.KEY_M);  //$NON-NLS-1$
		}
	}
	public boolean isSearchFurtherAvailable() {
		return zoom >= finalZoom;
	}
	
	public void searchFurther(){
		if (lastKnownMapLocation != null) {
			List<TransportIndexRepository> rs = ResourceManager.getResourceManager().searchTransportRepositories(lastKnownMapLocation.getLatitude(), 
					lastKnownMapLocation.getLongitude());
			if(!rs.isEmpty()){
				repo = rs.get(0);
				searchTransportLevel.setEnabled(isSearchFurtherAvailable());
				List<RouteInfoLocation> res = repo.searchTransportRouteStops(lastKnownMapLocation.getLatitude(), lastKnownMapLocation.getLongitude(), 
						locationToGo, zoom);
				// TODO add progress
				stopsAdapter.setNewModel(res);
			} else {
				repo = null;
				stopsAdapter.clear();
			}
		} else {
			stopsAdapter.clear();
		}
		searchTransportLevel.setEnabled(isSearchFurtherAvailable());
		searchArea.setText(getSearchArea());
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		lastKnownMapLocation = OsmandSettings.getLastKnownMapLocation(this);
		locationToGo = OsmandSettings.getPointToNavigate(this);
		searchFurther();
	}



	public void onListItemClick(ListView parent, View v, int position, long id) {
		RouteInfoLocation item = ((TransportStopAdapter)getListAdapter()).getItem(position);
		Builder builder = new AlertDialog.Builder(this);
		List<String> items = new ArrayList<String>();
		List<TransportStop> stops = item.getDirection() ? item.getRoute().getForwardStops() : item.getRoute().getBackwardStops();
		for(TransportStop st : stops){
			String n = st.getName(OsmandSettings.usingEnglishNames(this));
			if(locationToGo != null){
				n += " - " + MapUtils.getFormattedDistance((int) MapUtils.getDistance(locationToGo, st.getLocation())); //$NON-NLS-1$
			}
			items.add(n);
		}
		builder.setItems(items.toArray(new String[items.size()]), null);
		builder.show();
	}
	

	class TransportStopAdapter extends ArrayAdapter<RouteInfoLocation> {
		TransportStopAdapter(List<RouteInfoLocation> list) {
			super(SearchTransportActivity.this, R.layout.search_transport_list_item, list);
			this.setNotifyOnChange(false);
		}

		public void setNewModel(List<RouteInfoLocation> stopsList) {
			setNotifyOnChange(false);
			((TransportStopAdapter) getListAdapter()).clear();
			for (RouteInfoLocation obj : stopsList) {
				this.add(obj);
			}
			this.notifyDataSetChanged();
			setNotifyOnChange(true);
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			View row = convertView;
			if (row == null) {
				LayoutInflater inflater = getLayoutInflater();
				row = inflater.inflate(R.layout.search_transport_list_item, parent, false);
			}
			TextView label = (TextView) row.findViewById(R.id.label);
			TextView distanceLabel = (TextView) row.findViewById(R.id.distance);
			ImageView icon = (ImageView) row.findViewById(R.id.search_icon);
			RouteInfoLocation stop = getItem(position);

			TransportRoute route = stop.getRoute();
			StringBuilder labelW = new StringBuilder(150);
			labelW.append(route.getType()).append(" ").append(route.getRef()); //$NON-NLS-1$
			if (locationToGo != null) {
				labelW.append(" - ").append(MapUtils.getFormattedDistance(stop.getDistToLocation())); //$NON-NLS-1$
			}
			labelW.append("\n").append(route.getName(OsmandSettings.usingEnglishNames(SearchTransportActivity.this))); //$NON-NLS-1$
			label.setText(labelW.toString());
			// TODO icons
			if (stop.getDistToLocation() < 400) {
				icon.setImageResource(R.drawable.poi);
			} else {
				icon.setImageResource(R.drawable.closed_poi);
			}
			int dist = (int) (MapUtils.getDistance(stop.getStart().getLocation(), lastKnownMapLocation));
			distanceLabel.setText(" " + MapUtils.getFormattedDistance(dist)); //$NON-NLS-1$

			return (row);
		}
	}

}
