package net.osmand.activities.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import net.osmand.OsmandSettings;
import net.osmand.R;
import net.osmand.RegionAddressRepository;
import net.osmand.ResourceManager;
import net.osmand.activities.MapActivity;
import net.osmand.activities.OsmandApplication;
import net.osmand.data.City;
import net.osmand.data.MapObject;
import net.osmand.data.PostCode;
import net.osmand.data.Street;
import net.osmand.osm.LatLon;
import net.osmand.osm.MapUtils;
import net.osmand.osm.Node;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class GeoIntentActivity extends ListActivity {

	private ProgressDialog progressDlg;
	private LatLon location;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.search_address_offline);
		location = OsmandSettings.getLastKnownMapLocation(OsmandSettings
				.getPrefs(this));
		final Intent intent = getIntent();
		if (intent != null) {
			progressDlg = ProgressDialog.show(this,
					getString(R.string.searching),
					getString(R.string.searching_address));
			final Thread searcher = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						Collection<MapObject> results = extract(
								intent.getData()).execute();
						// show the first result on map, and populate the list!
						if (!results.isEmpty()) {
							showResult(0, new ArrayList<MapObject>(results));
						} else {
							showResult(R.string.search_nothing_found, null);
						}
					} catch (Exception e) {
						e.printStackTrace();
						showResult(R.string.error_doing_search, null);
					} finally {
						progressDlg.dismiss();
					}
				}
			}, "SearchingAddress");
			searcher.start();
			progressDlg.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					searcher.interrupt();
				}
			});
			progressDlg.setCancelable(true);

		}
		// finish();
	}

	private void showResult(final int warning, final List<MapObject> places) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (places == null) {
					Toast.makeText(GeoIntentActivity.this, getString(warning),
							Toast.LENGTH_LONG).show();
				} else {
					setListAdapter(new MapObjectAdapter(places));
					if (places.size() == 1) {
						onListItemClick(getListView(), getListAdapter()
								.getView(0, null, null), 0, getListAdapter()
								.getItemId(0));
					}
				}
			}
		});
	}

	class MapObjectAdapter extends ArrayAdapter<MapObject> {

		public MapObjectAdapter(List<MapObject> places) {
			super(GeoIntentActivity.this,
					R.layout.search_address_offline_list_item, places);
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			View row = convertView;
			if (row == null) {
				LayoutInflater inflater = getLayoutInflater();
				row = inflater.inflate(
						R.layout.search_address_offline_list_item, parent,
						false);
			}
			MapObject model = getItem(position);
			TextView label = (TextView) row.findViewById(R.id.label);
			TextView distanceLabel = (TextView) row
					.findViewById(R.id.distance_label);
			if (location != null) {
				int dist = (int) (MapUtils.getDistance(location, model
						.getLocation().getLatitude(), model.getLocation()
						.getLongitude()));
				distanceLabel.setText(MapUtils.getFormattedDistance(dist));
			} else {
				distanceLabel.setText(""); //$NON-NLS-1$
			}
			label.setText(model.toString());
			return row;
		}
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		MapObject item = ((MapObjectAdapter) getListAdapter())
				.getItem(position);
		OsmandSettings.setMapLocationToShow(this, item.getLocation()
				.getLatitude(), item.getLocation().getLongitude(),
				getString(R.string.address) + " : " + item.toString()); //$NON-NLS-1$
		startActivity(new Intent(this, MapActivity.class));
	}

	@Override
	protected void onStop() {
		dismiss();
		super.onStop();
	}

	private void dismiss() {
		if (progressDlg != null) {
			progressDlg.dismiss();
			progressDlg = null;
		}
	}

	/**
	 * geo:latitude,longitude<BR>
	 * geo:latitude,longitude?z=zoom<BR>
	 * geo:0,0?q=my+street+address<BR>
	 * geo:0,0?q=business+near+city
	 * 
	 * @param data
	 * @return
	 */
	private MyService extract(Uri data) {
		// it is 0,0? that means a search
		if (data.getSchemeSpecificPart().indexOf("0,0?") != -1) {
			return new GeoAddressSearch(data.getQuery());
		} else {
			return new GeoPointSearch(data.getSchemeSpecificPart());
		}
	}

	private final class GeoAddressSearch implements MyService {
		private List<String> elements;

		public GeoAddressSearch(String query) {
			StringTokenizer s = new StringTokenizer(query.substring(query
					.indexOf("q=") + 2), ",");
			elements = new ArrayList<String>(s.countTokens());
			while (s.hasMoreTokens()) {
				elements.add(s.nextToken().replace('+', ' ').trim());
			}
		}

		@Override
		public Collection<MapObject> execute() {
			if (elements.isEmpty()) {
				return Collections.emptyList();
			}

			// now try to search the City, Street, Etc.. if Street is not found,
			// try to search POI
			ResourceManager resourceManager = resourceManager();
			List<RegionAddressRepository> foundCountries = new ArrayList<RegionAddressRepository>();
			RegionAddressRepository country;
			for (String maybeCountry : elements) {
				country = resourceManager.getRegionRepository(maybeCountry);
				if (country != null) {
					foundCountries.add(country);
				}
			}
			Collection<RegionAddressRepository> countriesToSearch = foundCountries;
			if (foundCountries.isEmpty()) {
				// there is no country, we have to search each country
				countriesToSearch = resourceManager.getAddressRepositories();
			}

			// search cities for found countries
			Map<RegionAddressRepository, List<MapObject>> citiesForRegion = new HashMap<RegionAddressRepository, List<MapObject>>();
			for (RegionAddressRepository rar : countriesToSearch) {
				List<MapObject> citiesFound = new ArrayList<MapObject>();
				for (String maybeCity : elements) {
					rar.fillWithSuggestedCities(maybeCity, citiesFound, null);
				}
				if (!citiesFound.isEmpty()) {
					citiesForRegion.put(rar, citiesFound);
				}
			}
			// no cities found, we should locate the country only
			Map<MapObject, List<Street>> streetsForCity = new HashMap<MapObject, List<Street>>();
			if (citiesForRegion.isEmpty()) {
				for (RegionAddressRepository rar : countriesToSearch) {
					ArrayList<MapObject> allcities = new ArrayList<MapObject>();
					rar.fillWithSuggestedCities("", allcities, location);
					findStreetsForCities(streetsForCity, rar, allcities);
				}
			} else {
				// we have cities, now search for streets?
				for (RegionAddressRepository rar : citiesForRegion.keySet()) {
					findStreetsForCities(streetsForCity, rar,
							citiesForRegion.get(rar));
				}
			}

			// don't go deeper, now populate result list
			Set<MapObject> results = new HashSet<MapObject>();
			// add all found lists
			for (List<Street> streets : streetsForCity.values()) {
				results.addAll(streets);
			}
			// add all found cities for which street was not found
			for (List<MapObject> cities : citiesForRegion.values()) {
				cities.removeAll(streetsForCity.keySet());
				results.addAll(cities);
			}
			// TODO add all regions for which city was not found
			return results;
		}

		private void findStreetsForCities(
				Map<MapObject, List<Street>> streetsForCity,
				RegionAddressRepository rar, List<MapObject> allcities) {
			for (MapObject city : allcities) {
				List<Street> streets = new ArrayList<Street>();
				rar.fillWithSuggestedStreets(city, streets,
						elements.toArray(new String[] {}));
				// we must do this, or we will fill up the whole memory (streets
				// are preloaded...)
				// TODO some street iterator would be better, is it possible to
				// create one?
				if (city instanceof City) {
					((City) city).removeAllStreets();
				} else if (city instanceof PostCode) {
					((PostCode) city).removeAllStreets();
				}
				if (!streets.isEmpty()) {
					streetsForCity.put(city, streets);
				}
			}
		}

	}

	private ResourceManager resourceManager() {
		return ((OsmandApplication) getApplication()).getResourceManager();
	}

	private class GeoPointSearch implements MyService {

		private MapObject point;

		/**
		 * geo:latitude,longitude geo:latitude,longitude?z=zoom
		 */
		public GeoPointSearch(final String geo) {
			int latIndex = geo.indexOf(',');
			int lonIndex = geo.indexOf('?');
			lonIndex = lonIndex > 0 ? lonIndex : geo.length();
			if (latIndex > 0) {
				try {
					double latitude = Double.parseDouble(geo.substring(0,
							latIndex));
					double longitude = Double.parseDouble(geo.substring(
							latIndex + 1, lonIndex));
					// TODO zoom is omited for now
					point = new MapObject(new Node(latitude, longitude, -1)) {
					};
					point.setName("Lat: " + latitude + ",Lon:" + longitude);
				} catch (NumberFormatException e) {
					runOnUiThread(new Runnable() {
						public void run() {
							Toast.makeText(GeoIntentActivity.this,
									getString(R.string.search_offline_geo_error, geo),
									Toast.LENGTH_LONG);
						}
					});
				}
			}
		}

		@Override
		public Collection<MapObject> execute() {
			if (point != null) {
				return Collections.singletonList(point);
			} else {
				return Collections.emptyList();
			}
		}

	}

	private interface MyService {

		public Collection<MapObject> execute();
	}

}
