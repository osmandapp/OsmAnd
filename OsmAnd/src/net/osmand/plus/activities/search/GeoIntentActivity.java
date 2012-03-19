package net.osmand.plus.activities.search;

import gnu.trove.map.hash.TLongObjectHashMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import net.osmand.OsmAndFormatter;
import net.osmand.access.AccessibleToast;
import net.osmand.ResultMatcher;
import net.osmand.data.City;
import net.osmand.data.MapObject;
import net.osmand.data.Street;
import net.osmand.osm.LatLon;
import net.osmand.osm.MapUtils;
import net.osmand.osm.Node;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.RegionAddressRepository;
import net.osmand.plus.ResourceManager;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.OsmandListActivity;
import android.app.Dialog;
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

public class GeoIntentActivity extends OsmandListActivity {

	private ProgressDialog progressDlg;
	private LatLon location;
	private ProgressDialog startProgressDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.search_address_offline);
		startProgressDialog = new ProgressDialog(this);
		getMyApplication().checkApplicationIsBeingInitialized(this, startProgressDialog);
		location = getMyApplication().getSettings().getLastKnownMapLocation();
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
						if (progressDlg != null) {
							progressDlg.dismiss();
						}
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

	@Override
	protected Dialog onCreateDialog(int id) {
		if(id == OsmandApplication.PROGRESS_DIALOG){
			return startProgressDialog;
		}
		return super.onCreateDialog(id);
	}
	
	private void showResult(final int warning, final List<MapObject> places) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (places == null) {
					AccessibleToast.makeText(GeoIntentActivity.this, getString(warning),
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

		@Override
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
				distanceLabel.setText(OsmAndFormatter.getFormattedDistance(dist,getContext()));
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
		MapObject item = ((MapObjectAdapter) getListAdapter()).getItem(position);
		OsmandSettings settings = getMyApplication().getSettings();
		settings.setMapLocationToShow(item.getLocation().getLatitude(), item.getLocation().getLongitude(), 
				settings.getLastKnownMapZoom(), getString(R.string.address) + " : " + item.toString()); //$NON-NLS-1$
		MapActivity.launchMapActivityMoveToTop(this);
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
			final List<MapObject> results = new ArrayList<MapObject>();
			final List<MapObject> connectedStreets = new ArrayList<MapObject>();
			for (RegionAddressRepository rar : countriesToSearch) {
				final TLongObjectHashMap<City> cityIds = new TLongObjectHashMap<City>();
				for (String element : elements) {
					rar.searchMapObjectsByName(element, new ResultMatcher<MapObject>() {
						@Override
						public boolean publish(MapObject object) {
							if (object instanceof City && object.getId() != null) {
								cityIds.put(object.getId(), (City) object);
							} else if(object instanceof Street) {
								City c = ((Street)object).getCity();
								if(c != null && c.getId() != null && cityIds.containsKey(c.getId().longValue())) {
									connectedStreets.add((Street) object);
									return false;
								}
							}
							results.add(object);
							return false;
						}

						@Override
						public boolean isCancelled() {
							return false;
						}
					});
				}
			}
			
			
			// add all other results to connected streets
			connectedStreets.addAll(results);
			return connectedStreets;
		}


	}

	private ResourceManager resourceManager() {
		return getMyApplication().getResourceManager();
	}

	private OsmandApplication getMyApplication() {
		return ((OsmandApplication) getApplication());
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
					double latitude = Double.parseDouble(geo.substring(0, latIndex));
					double longitude = Double.parseDouble(geo.substring(latIndex + 1, lonIndex));
					// TODO zoom is omited for now
					point = new MapObject(new Node(latitude, longitude, -1)) {
						private static final long serialVersionUID = -7028586132795853725L;
					};
					point.setName("Lat: " + latitude + ",Lon:" + longitude);
				} catch (NumberFormatException e) {
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							AccessibleToast.makeText(GeoIntentActivity.this,
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
