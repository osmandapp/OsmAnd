package com.osmand.activities.search;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParser;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.osmand.Algoritms;
import com.osmand.LogUtil;
import com.osmand.OsmandSettings;
import com.osmand.R;
import com.osmand.Version;
import com.osmand.activities.MapActivity;
import com.osmand.osm.LatLon;
import com.osmand.osm.MapUtils;

public class SearchAddressOnlineActivity extends ListActivity {
	
	private LatLon location;
	private final static Log log = LogUtil.getLog(SearchAddressOnlineActivity.class);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.search_address_online);
		Button searchOffline = (Button) findViewById(R.id.SearchOffline);
		if (getParent() instanceof SearchActivity) {
			searchOffline.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					((SearchActivity) getParent()).startSearchAddressOffline();
				}
			});
		} else {
			searchOffline.setVisibility(View.INVISIBLE);
		}
		
		Button searchButton = (Button) findViewById(R.id.SearchButton);
		searchButton.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v) {
				searchPlaces(((EditText) findViewById(R.id.SearchText)).getText().toString());
			}
		});
		location = OsmandSettings.getLastKnownMapLocation(this);
	}

	protected void searchPlaces(final String search) {
		if(Algoritms.isEmpty(search)){
			return;
		}
		
		final ProgressDialog dlg = ProgressDialog.show(this, getString(R.string.searching), getString(R.string.searching_address));
		new Thread(new Runnable(){
			@Override
			public void run() {
				try {
					final List<Place> places = new ArrayList<Place>();
					StringBuilder b = new StringBuilder();
					b.append("http://nominatim.openstreetmap.org/search/"); //$NON-NLS-1$
					b.append(URLEncoder.encode(search));
					b.append("?format=xml&addressdetails=0&accept-language=").append(Locale.getDefault().getLanguage()); //$NON-NLS-1$
					log.info("Searching address at : " + b.toString()); //$NON-NLS-1$
					URL url = new URL(b.toString());
					URLConnection conn = url.openConnection();
					conn.setDoInput(true);
					conn.setRequestProperty("User-Agent", Version.APP_NAME_VERSION); //$NON-NLS-1$
					conn.connect();
					InputStream is = conn.getInputStream();
					XmlPullParser parser = Xml.newPullParser();
					parser.setInput(is, "UTF-8"); //$NON-NLS-1$
					int ev;
					while ((ev = parser.next()) != XmlPullParser.END_DOCUMENT) {
						if(ev == XmlPullParser.START_TAG){
							if(parser.getName().equals("place")){ //$NON-NLS-1$
								String lat = parser.getAttributeValue("", "lat"); //$NON-NLS-1$ //$NON-NLS-2$
								String lon = parser.getAttributeValue("", "lon");  //$NON-NLS-1$//$NON-NLS-2$
								String displayName = parser.getAttributeValue("", "display_name"); //$NON-NLS-1$ //$NON-NLS-2$
								if(lat != null && lon != null && displayName != null){
									Place p = new Place();
									p.lat = Double.parseDouble(lat);
									p.lon = Double.parseDouble(lon);
									p.displayName = displayName;
									places.add(p);
								}
							}
						}

					}
					is.close();
					if(places.isEmpty()){
						showResult(R.string.search_nothing_found, null);
					} else {
						showResult(0, places);
					}
				} catch(Exception e){
					log.error("Error searching address", e); //$NON-NLS-1$
					showResult(R.string.error_io_error, null);
				} finally {
					dlg.dismiss();
				}
			}
			
		}, "SearchingAddress").start(); //$NON-NLS-1$
		
	}
	
	private void showResult(final int warning, final List<Place> places) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if(places == null){
					Toast.makeText(SearchAddressOnlineActivity.this, getString(warning), Toast.LENGTH_LONG).show();
				} else {
					setListAdapter(new PlacesAdapter(places));
				}
			}
		});
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		Place item = ((PlacesAdapter) getListAdapter()).getItem(position);
		OsmandSettings.setMapLocationToShow(this, item.lat, item.lon, getString(R.string.address)+ " : " + item.displayName); //$NON-NLS-1$
		startActivity(new Intent(this, MapActivity.class));
	}
	
	private static class Place {
		public double lat;
		public double lon;
		public String displayName;
	}
	
	class PlacesAdapter extends ArrayAdapter<Place> {

		public PlacesAdapter(List<Place> places) {
			super(SearchAddressOnlineActivity.this, R.layout.search_address_online_list_item, places);
		}
		
		public View getView(int position, View convertView, ViewGroup parent) {
			View row = convertView;
			if (row == null) {
				LayoutInflater inflater = getLayoutInflater();
				row = inflater.inflate(R.layout.search_address_online_list_item, parent, false);
			}
			Place model = getItem(position);
			TextView label = (TextView) row.findViewById(R.id.label);
			TextView distanceLabel = (TextView) row.findViewById(R.id.distance_label);
			if(location != null){
				int dist = (int) (MapUtils.getDistance(location, model.lat, model.lon));
				distanceLabel.setText(MapUtils.getFormattedDistance(dist));
			} else {
				distanceLabel.setText(""); //$NON-NLS-1$
			}
			label.setText(model.displayName);
			return row;
		}
		
	}
	

}
