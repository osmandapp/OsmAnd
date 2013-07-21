package net.osmand.plus.activities.search;

import gnu.trove.map.hash.TLongObjectHashMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import net.osmand.ResultMatcher;
import net.osmand.access.AccessibleToast;
import net.osmand.data.Amenity;
import net.osmand.data.AmenityType;
import net.osmand.data.City;
import net.osmand.data.LatLon;
import net.osmand.data.MapObject;
import net.osmand.data.Street;
import net.osmand.plus.ClientContext;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.OsmandListActivity;
import net.osmand.plus.resources.RegionAddressRepository;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.util.MapUtils;
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
		getSupportActionBar().setTitle(R.string.search_osm_offline);
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
						while(getMyApplication().isApplicationInitializing()) {
							Thread.sleep(200);
						}
						Collection<? extends MapObject> results = extract(
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
				distanceLabel.setText(OsmAndFormatter.getFormattedDistance(dist,(ClientContext) getApplication()));
			} else {
				distanceLabel.setText(""); //$NON-NLS-1$
			}
			label.setText(getString(model));
			return row;
		}
	}
	
	private String getString(MapObject o){
		if(o instanceof Amenity) {
			return OsmAndFormatter.getPoiSimpleFormat((Amenity) o, getMyApplication(), false);
		}
		if(o instanceof Street) {
			return getString(R.string.address) + " " + ((Street) o).getCity().getName() + " " + o.getName();
		}
		return getString(R.string.address) + " : " + o.toString();
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		MapObject item = ((MapObjectAdapter) getListAdapter()).getItem(position);
		OsmandSettings settings = getMyApplication().getSettings();
		settings.setMapLocationToShow(item.getLocation().getLatitude(), item.getLocation().getLongitude(), 
				settings.getLastKnownMapZoom(), getString(item)); //$NON-NLS-1$
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
		if ("http".equalsIgnoreCase(data.getScheme()) && "maps.google.com".equals(data.getHost())) {
			String q = data.getQueryParameter("q").split(" ")[0];
			if (q.indexOf(',') != -1) {
				int i = q.indexOf(',');
				String lat = q.substring(0, i);
				String lon = q.substring(i + 1);
				if (lat.indexOf(":") != -1) {
					i = lat.indexOf(":");
					lat = lat.substring(i + 1);
				}
				try {
					double llat = Double.parseDouble(lat.trim());
					double llon = Double.parseDouble(lon.trim());
					return new GeoPointSearch(llat, llon);
				} catch (NumberFormatException e) {
					showErrorMessage(q);
				}
			} else {
				showErrorMessage(q);
			}
		} else if (data.getSchemeSpecificPart().indexOf("0,0?") != -1) {
			// it is 0,0? that means a search
			return new GeoAddressSearch(data.getQuery());
		} else {
			String geo = data.getSchemeSpecificPart();
			if(geo == null) {
				showErrorMessage("");
			} else {
				int latIndex = geo.indexOf(',');
				int lonIndex = geo.indexOf('?');
				lonIndex = lonIndex > 0 ? lonIndex : geo.length();
				if (latIndex > 0) {
					try {
						double lat = Double.parseDouble(geo.substring(0, latIndex).trim());
						double lon = Double.parseDouble(geo.substring(latIndex + 1, lonIndex).trim());
						return new GeoPointSearch(lat, lon);
					} catch (NumberFormatException e) {
						showErrorMessage(geo);
					}
				} else {
					showErrorMessage(geo);
				}
			}
		}
		return new Empty();
	}

	private final class GeoAddressSearch implements MyService {
		private List<String> elements;

		public GeoAddressSearch(String query) {
			query = query.replaceAll("%20", ",").replaceAll("%0A",",")
					.replaceAll("\n",",").replaceAll("\t",",")
					.replaceAll(" ", ",");
			System.out.println(query);
			//String is split on each comma
			String[] s = query.substring(query
					.indexOf("q=") + 2).split(",");
			
			elements = new ArrayList<String>();
			for (int i = 0;  i<s.length; i++) {
				elements.add(s[i].replace('+', ' ').trim());
			}
		}
		
		public MapObject checkGeoPoint() {
			// TODO Auto-generated method stub
			double lat = Double.NaN;
			double lon = Double.NaN;
			for(String e : elements) {
				if(e.startsWith("S") || e.startsWith("N")) {
					try {
						lat = Double.parseDouble(e.substring(1));
						if(e.startsWith("S")) {
							lat = -lat;
						}
					} catch(NumberFormatException es) {}
				} else if(e.startsWith("E") || e.startsWith("W")) {
					try {
						lon = Double.parseDouble(e.substring(1));
						if(e.startsWith("W")) {
							lon = -lon;
						}
					} catch(NumberFormatException es) {}
				} else if(e.contains(".")) {
					try {
						double n = Double.parseDouble(e);
						if(Double.isNaN(lat)) {
							lat = n;
						} else {
							lon =n;
						}
					} catch(NumberFormatException es) {}
				}
				
			}
			if(Double.isNaN(lat) || Double.isNaN(lon)) {
				return null;
			}
			
			Amenity point = new Amenity();
			((Amenity)point).setType(AmenityType.USER_DEFINED);
			((Amenity)point).setSubType("");
			point.setLocation(lat, lon);
			point.setName("Lat: " + lat + ",Lon:" + lon);
			return point;
		}

		@Override
		public Collection<? extends MapObject> execute() {
			if (elements.isEmpty()) {
				return Collections.emptyList();
			}
			List<String> q = new ArrayList<String>(elements);
			MapObject geo = checkGeoPoint();
			if(geo != null) {
				return Collections.singleton(geo);
			}

			// now try to search the City, Street, Etc.. if Street is not found,
			// try to search POI
			Collection<RegionAddressRepository> countriesToSearch = limitSearchToCountries(q);
			// search cities for found countries
			final List<Street> allStreets = new ArrayList<Street>();
			final TLongObjectHashMap<City> cityIds = new TLongObjectHashMap<City>();
			for (RegionAddressRepository rar : countriesToSearch) {
				for (String element : q) {
					if (element != null && element.length() > 2) {
						rar.searchMapObjectsByName(element, new ResultMatcher<MapObject>() {
							@Override
							public boolean publish(MapObject object) {
								if (object instanceof City && object.getId() != null) {
									cityIds.put(object.getId(), (City) object);
								} else if (object instanceof Street) {
									allStreets.add((Street) object);
								}
								return false;
							}

							@Override
							public boolean isCancelled() {
								return false;
							}
						});
					}
				}
			}
			if(cityIds.isEmpty()) {
				return allStreets;
			}
			final List<MapObject> connectedStreets = new ArrayList<MapObject>();
			Iterator<Street> p = allStreets.iterator();
			while(p.hasNext()) {
				Street s = p.next();
				if(cityIds.contains(s.getCity().getId())) {
					connectedStreets.add(s);
				} else {
					boolean tooFar = true;
					for(City c : cityIds.valueCollection()) {
						if(MapUtils.getDistance(c.getLocation(), s.getLocation()) < 50000) {
							tooFar = false;
							break;
						}
					}
					if(tooFar) {
						p.remove();
					}
				}
			}
			if(connectedStreets.isEmpty()) {
				List<MapObject> all = new ArrayList<MapObject>();
				all.addAll(cityIds.valueCollection());
				all.addAll(allStreets);
				return all;
			} else {
				// add all other results to connected streets
				connectedStreets.addAll(cityIds.valueCollection());
				return connectedStreets;
			}
		}

		private Collection<RegionAddressRepository> limitSearchToCountries(List<String> q) {
			ResourceManager resourceManager = resourceManager();
			List<RegionAddressRepository> foundCountries = new ArrayList<RegionAddressRepository>();
			RegionAddressRepository country;
			Iterator<String> it = q.iterator();
			while(it.hasNext()) {
				String maybeCountry = it.next();
				country = resourceManager.getRegionRepository(maybeCountry);
				if (country != null) {
					foundCountries.add(country);
					it.remove();
				}
			}
			Collection<RegionAddressRepository> countriesToSearch = foundCountries;
			if (foundCountries.isEmpty()) {
				// there is no country, we have to search each country
				countriesToSearch = resourceManager.getAddressRepositories();
			}
			return countriesToSearch;
		}


	}

	private ResourceManager resourceManager() {
		return getMyApplication().getResourceManager();
	}

	

	private void showErrorMessage(final String geo) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				AccessibleToast.makeText(GeoIntentActivity.this,
						getString(R.string.search_offline_geo_error, geo),
						Toast.LENGTH_LONG);
			}
		});
	}
	
	private class Empty implements MyService {

		@Override
		public Collection<MapObject> execute() {
			return Collections.emptyList();
		}
		
	}
	
	private class GeoPointSearch implements MyService {
		private MapObject point;
		/**
		 * geo:latitude,longitude geo:latitude,longitude?z=zoom
		 */
		public GeoPointSearch(double lat , double lon ) {
			// TODO zoom is omited for now
			point = new Amenity();
			((Amenity)point).setType(AmenityType.USER_DEFINED);
			((Amenity)point).setSubType("");
			point.setLocation(lat, lon);
			point.setName("Lat: " + lat + ",Lon:" + lon);
		}


		@Override
		public Collection<? extends MapObject> execute() {
			if (point != null) {
				return Collections.singletonList(point);
			} else {
				return Collections.emptyList();
			}
		}

	}

	private interface MyService {

		public Collection<? extends MapObject> execute();
	}

}
