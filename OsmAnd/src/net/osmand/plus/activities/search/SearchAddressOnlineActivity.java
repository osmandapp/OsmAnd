package net.osmand.plus.activities.search;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.londatiga.android.QuickAction;
import net.osmand.PlatformUtil;
import net.osmand.access.AccessibleToast;
import net.osmand.data.LatLon;
import net.osmand.util.MapUtils;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivityActions;
import net.osmand.plus.activities.OsmandListActivity;
import net.osmand.plus.activities.search.SearchActivity.SearchActivityChild;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParser;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class SearchAddressOnlineActivity extends OsmandListActivity implements SearchActivityChild {
	
	private LatLon location;
	private ProgressDialog progressDlg;
	private final static Log log = PlatformUtil.getLog(SearchAddressOnlineActivity.class);

	private static PlacesAdapter lastResult = null;
	private OsmandSettings settings;
	
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
		
		settings = ((OsmandApplication) getApplication()).getSettings();
		
		final EditText searchText = (EditText) findViewById(R.id.SearchText);
		
		Button clearSearch = (Button) findViewById(R.id.ClearSearch);
		clearSearch.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				searchText.setText("");
				lastResult = null;
				setListAdapter(null);
			}
		});
		
		Button searchButton = (Button) findViewById(R.id.SearchButton);
		searchButton.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v) {
				InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
				inputMethodManager.hideSoftInputFromWindow(searchText.getWindowToken(), 0); // Remove keyboard

				searchPlaces(searchText.getText().toString());
			}
		});
		location = settings.getLastKnownMapLocation();
		
		if (lastResult != null) {
			setListAdapter(lastResult);
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		Intent intent = getIntent();
		if(intent != null){
			double lat = intent.getDoubleExtra(SearchActivity.SEARCH_LAT, 0);
			double lon = intent.getDoubleExtra(SearchActivity.SEARCH_LON, 0);
			if(lat != 0 || lon != 0){
				location = new LatLon(lat, lon);
			}
		}
		if (location == null && getParent() instanceof SearchActivity) {
			location = ((SearchActivity) getParent()).getSearchPoint();
		}
		if (location == null) {
			location = settings.getLastKnownMapLocation();
		}
	}
	
	@Override
	public void locationUpdate(LatLon l) {
		location = l;
		if(lastResult != null){
			lastResult.notifyDataSetInvalidated();
		}
	}

	protected void searchPlaces(final String search) {
		if(Algorithms.isEmpty(search)){
			return;
		}
		
		progressDlg = ProgressDialog.show(this, getString(R.string.searching), getString(R.string.searching_address));
		new Thread(new Runnable(){
			@Override
			public void run() {
				try {
					final List<Place> places = new ArrayList<Place>();
					StringBuilder b = new StringBuilder();
					b.append("http://nominatim.openstreetmap.org/search"); //$NON-NLS-1$
					b.append("?format=xml&addressdetails=0&accept-language=").append(Locale.getDefault().getLanguage()); //$NON-NLS-1$
					b.append("&q=").append(URLEncoder.encode(search)); //$NON-NLS-1$
					
					log.info("Searching address at : " + b.toString()); //$NON-NLS-1$
					URL url = new URL(b.toString());
					URLConnection conn = url.openConnection();
					conn.setDoInput(true);
					conn.setRequestProperty("User-Agent", Version.getFullVersion(getMyApplication())); //$NON-NLS-1$
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
					if(progressDlg != null){
						progressDlg.dismiss();
						progressDlg = null;
					}
				}
			}
			
		}, "SearchingAddress").start(); //$NON-NLS-1$
	}
	
	private void showResult(final int warning, final List<Place> places) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if(places == null){
					AccessibleToast.makeText(SearchAddressOnlineActivity.this, getString(warning), Toast.LENGTH_LONG).show();
				} else {
					lastResult = new PlacesAdapter(places);
					setListAdapter(lastResult);
				}
			}
		});
	}
	

	@Override
	protected void onStop() {
		if(progressDlg != null){
			progressDlg.dismiss();
			progressDlg = null;
		}
		super.onStop();
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		Place item = ((PlacesAdapter) getListAdapter()).getItem(position);
		QuickAction qa = new QuickAction(v);
		MapActivityActions.createDirectionsActions(qa, new LatLon(item.lat, item.lon), item, 
				getString(R.string.address)+ " : " + item.displayName, Math.max(15, settings.getLastKnownMapZoom()), 
				this, true, null);
		qa.show();
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
		
		@Override
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
				distanceLabel.setText(OsmAndFormatter.getFormattedDistance(dist, getMyApplication()));
			} else {
				distanceLabel.setText(""); //$NON-NLS-1$
			}
			label.setText(model.displayName);
			return row;
		}
		
	}
	

}
