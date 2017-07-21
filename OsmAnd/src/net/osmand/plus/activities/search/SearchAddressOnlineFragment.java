package net.osmand.plus.activities.search;

import java.io.InputStream;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.osm.io.NetworkUtils;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.search.SearchActivity.SearchActivityChild;
import net.osmand.plus.dialogs.DirectionsDialogs;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParser;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.PopupMenu;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class SearchAddressOnlineFragment extends Fragment implements SearchActivityChild, OnItemClickListener {
	
	private LatLon location;
	private final static Log log = PlatformUtil.getLog(SearchAddressOnlineFragment.class);

	private static PlacesAdapter adapter = null;
	private OsmandSettings settings;
	private View view;
	private EditText searchText;
	
	
	@Override
	public void onCreateOptionsMenu(Menu onCreate, MenuInflater inflater) {
		Menu menu = onCreate;
		if(getActivity() instanceof SearchActivity) {
			menu = ((SearchActivity) getActivity()).getClearToolbar(true).getMenu();
		}
		MenuItem menuItem = menu.add(0, 1, 0, R.string.search_offline_clear_search);
		MenuItemCompat.setShowAsAction(menuItem, MenuItemCompat.SHOW_AS_ACTION_ALWAYS | MenuItemCompat.SHOW_AS_ACTION_WITH_TEXT);
		menuItem = menuItem.setIcon(R.drawable.ic_action_gremove_dark);
		menuItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				searchText.setText("");
				adapter.clear();
				return true;
			}
		});
		if (getActivity() instanceof SearchActivity) {
			menuItem = menu.add(0, 0, 0, R.string.search_offline_address);
			MenuItemCompat.setShowAsAction(menuItem, MenuItemCompat.SHOW_AS_ACTION_ALWAYS | MenuItemCompat.SHOW_AS_ACTION_WITH_TEXT);
			menuItem = menuItem.setIcon(R.drawable.ic_sdcard);
			menuItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					((SearchActivity) getActivity()).startSearchAddressOffline();
					return true;
				}
			});
		}
	}
	
	public View onCreateView(android.view.LayoutInflater inflater, android.view.ViewGroup container, Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.search_address_online, container, false);
		adapter = new PlacesAdapter(new ArrayList<SearchAddressOnlineFragment.Place>());
		settings = ((OsmandApplication) getActivity().getApplication()).getSettings();

		searchText = (EditText) view.findViewById(R.id.SearchText);
		Button searchButton = (Button) view.findViewById(R.id.SearchButton);
		searchButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
				inputMethodManager.hideSoftInputFromWindow(searchText.getWindowToken(), 0); // Remove keyboard

				searchPlaces(searchText.getText().toString());
			}
		});
		setHasOptionsMenu(true);
		location = settings.getLastKnownMapLocation();
		ListView lv = (ListView) view.findViewById(android.R.id.list);
		lv.setAdapter(adapter);
		lv.setOnItemClickListener(this);
		return view;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		Intent intent = getActivity().getIntent();
		if(intent != null){
			double lat = intent.getDoubleExtra(SearchActivity.SEARCH_LAT, 0);
			double lon = intent.getDoubleExtra(SearchActivity.SEARCH_LON, 0);
			if(lat != 0 || lon != 0){
				adapter.location = new LatLon(lat, lon);
			}
		}
		if (location == null && getActivity() instanceof SearchActivity) {
			location = ((SearchActivity) getActivity()).getSearchPoint();
		}
		if (location == null) {
			location = settings.getLastKnownMapLocation();
		}
		locationUpdate(location);
	}
	
	@Override
	public void locationUpdate(LatLon l) {
		location = l;
		if(adapter != null){
			adapter.updateLocation(l);
		}
	}

	protected void searchPlaces(final String search) {
		
		if(Algorithms.isEmpty(search)){
			return;
		}
		new AsyncTask<Void, Void, Void>() {
			List<Place> places = null;
			String warning = null;
			protected void onPreExecute() {
				view.findViewById(R.id.ProgressBar).setVisibility(View.VISIBLE);
			};
			@Override
			protected Void doInBackground(Void... params) {
				try {
					
					final int deviceApiVersion = android.os.Build.VERSION.SDK_INT;

					String NOMINATIM_API;
				
					if (deviceApiVersion >= android.os.Build.VERSION_CODES.GINGERBREAD) {
						NOMINATIM_API = "https://nominatim.openstreetmap.org/search";
					}
					else {
						NOMINATIM_API = "https://nominatim.openstreetmap.org/search";
					}
					
					final List<Place> places = new ArrayList<Place>();
					StringBuilder b = new StringBuilder();
					b.append(NOMINATIM_API); //$NON-NLS-1$
					b.append("?format=xml&addressdetails=0&accept-language=").append(Locale.getDefault().getLanguage()); //$NON-NLS-1$
					b.append("&q=").append(URLEncoder.encode(search, "UTF-8")); //$NON-NLS-1$
					
					log.info("Searching address at : " + b); //$NON-NLS-1$
					URLConnection conn = NetworkUtils.getHttpURLConnection(b.toString());
					conn.setDoInput(true);
					conn.setRequestProperty("User-Agent", Version.getFullVersion((OsmandApplication) getActivity().getApplication())); //$NON-NLS-1$
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
						this.places = null;
						warning = getString(R.string.search_nothing_found);
					} else {
						this.places = places; 
					}
				} catch(Exception e){
					log.error("Error searching address", e); //$NON-NLS-1$
					warning = getString(R.string.shared_string_io_error) + " : " + e.getMessage();
				}
				return null;
			}
			protected void onPostExecute(Void result) {
				view.findViewById(R.id.ProgressBar).setVisibility(View.INVISIBLE);
				if(places == null){
					Toast.makeText(getActivity(), warning, Toast.LENGTH_LONG).show();
				} else {
					adapter.setPlaces(places);
				}
			};
		}.execute((Void) null);
		
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Place item = adapter.getItem(position);

		LatLon location = new LatLon(item.lat, item.lon);
		settings.setMapLocationToShow(location.getLatitude(), location.getLongitude(),
				Math.max(15, settings.getLastKnownMapZoom()),
				new PointDescription(PointDescription.POINT_TYPE_ADDRESS, item.displayName),
				true,
				item); //$NON-NLS-1$
		MapActivity.launchMapActivityMoveToTop(getActivity());
	}
	
	private static class Place {
		public double lat;
		public double lon;
		public String displayName;
	}
	
	class PlacesAdapter extends ArrayAdapter<Place> {
		private LatLon location;

		public void updateLocation(LatLon l) {
			location = l;
			notifyDataSetChanged();
		}

		public PlacesAdapter(List<Place> places) {
			super(getActivity(), R.layout.search_address_online_list_item, places);
		}
		
		public void setPlaces(List<Place> places) {
			setNotifyOnChange(false);
			clear();
			for(Place p : places) {
				add(p);
			}
			setNotifyOnChange(true);
			notifyDataSetChanged();
			
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View row = convertView;
			if (row == null) {
				LayoutInflater inflater = getActivity().getLayoutInflater();
				row = inflater.inflate(R.layout.search_address_online_list_item, parent, false);
			}
			Place model = getItem(position);
			TextView label = (TextView) row.findViewById(R.id.label);
			TextView distanceLabel = (TextView) row.findViewById(R.id.distance_label);
			if(location != null){
				int dist = (int) (MapUtils.getDistance(location, model.lat, model.lon));
				distanceLabel.setText(OsmAndFormatter.getFormattedDistance(dist, (OsmandApplication) getActivity().getApplication()));
			} else {
				distanceLabel.setText(""); //$NON-NLS-1$
			}
			label.setText(model.displayName);
			return row;
		}
		
	}

	

}
