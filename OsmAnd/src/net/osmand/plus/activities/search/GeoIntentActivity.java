package net.osmand.plus.activities.search;

import android.os.AsyncTask;
import gnu.trove.map.hash.TLongObjectHashMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.osmand.ResultMatcher;
import net.osmand.access.AccessibleToast;
import net.osmand.data.Amenity;
import net.osmand.data.AmenityType;
import net.osmand.data.City;
import net.osmand.data.LatLon;
import net.osmand.data.MapObject;
import net.osmand.data.Street;
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
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_address_offline);
        getSupportActionBar().setTitle(R.string.search_osm_offline);
        startProgressDialog = new ProgressDialog(this);
        getMyApplication().checkApplicationIsBeingInitialized(this, startProgressDialog);
        location = getMyApplication().getSettings().getLastKnownMapLocation();

        final Intent intent = getIntent();
        if (intent != null)
        {
            final ProgressDialog progress = ProgressDialog.show(GeoIntentActivity.this, getString(R.string.searching), getString(R.string.searching_address));
            final GeoIntentTask task = new GeoIntentTask(progress, intent);

            progress.setOnCancelListener(new OnCancelListener()
            {
                @Override
                public void onCancel(DialogInterface dialog)
                {
                    task.cancel(true);
                }
            });
            progress.setCancelable(true);

            task.execute();
			setIntent(null);
        }
    }

    private class GeoIntentTask extends AsyncTask<Void,Void, ExecutionResult>
    {
        private final ProgressDialog progress;
        private final Intent intent;

        private GeoIntentTask(final ProgressDialog progress, final Intent intent)
        {
            this.progress = progress;
            this.intent = intent;
        }

        @Override
        protected void onPreExecute()
        {
        }

        @Override
        protected ExecutionResult doInBackground(Void... nothing)
        {
            try
            {
                while (getMyApplication().isApplicationInitializing())
                {
                    Thread.sleep(200);
                }
                return extract(intent.getScheme(), intent.getData()).execute();
            }
            catch (Exception e)
            {
                return null;
            }
        }

        @Override
        protected void onPostExecute(ExecutionResult result)
        {
            progress.dismiss();
            if (result != null)
            {
                if (result.isEmpty())
                {
                    AccessibleToast.makeText(GeoIntentActivity.this, getString(R.string.search_nothing_found), Toast.LENGTH_LONG).show();
                }
                else
                {
                    if (result.hasZoom())
                    {
                        getMyApplication().getSettings().setLastKnownMapZoom(result.getZoom());
                    }

                    final List<MapObject> places = new ArrayList<MapObject>(result.getMapObjects());
					setListAdapter(new MapObjectAdapter(places));
					if (places.size() == 1)
                    {
                        onListItemClick(
                                getListView(),
                                getListAdapter().getView(0, null, null),
                                0,
                                getListAdapter().getItemId(0)
                        );
                    }
                }
            }
            else
            {
                AccessibleToast.makeText(GeoIntentActivity.this, getString(R.string.search_offline_geo_error, intent.getData()), Toast.LENGTH_LONG).show();
            }
        }

    }

	@Override
	protected Dialog onCreateDialog(int id) {
		if(id == OsmandApplication.PROGRESS_DIALOG){
			return startProgressDialog;
		}
		return super.onCreateDialog(id);
	}
	
	private class MapObjectAdapter extends ArrayAdapter<MapObject> {

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
				distanceLabel.setText(OsmAndFormatter.getFormattedDistance(dist,(OsmandApplication) getApplication()));
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
     * Extracts information from geo and map intents:
     *
	 * geo:47.6,-122.3<br/>
     * geo:47.6,-122.3?z=11<br/>
     * geo:0,0?q=34.99,-106.61(Treasure)<br/>
     * geo:0,0?q=1600+Amphitheatre+Parkway%2C+CA<br/>
	 *
	 * @param scheme The intent scheme
	 * @param data The intent uri
	 * @return
	 */
    private MyService extract(final String scheme, final Uri data)
    {
        if ("http".equals(scheme))
        {
            String q = null;
            String parameter = data.getQueryParameter("q");
            if (parameter == null)
            {
                parameter = data.getQueryParameter("daddr");
            }
            if (parameter != null)
            {
                q = parameter.split(" ")[0];
            }
            if (q.indexOf(',') != -1)
            {
                int i = q.indexOf(',');
                String lat = q.substring(0, i);
                String lon = q.substring(i + 1);
                if (lat.indexOf(':') != -1)
                {
                    i = lat.indexOf(':');
                    lat = lat.substring(i + 1);
                }
                try
                {
                    double llat = Double.parseDouble(lat.trim());
                    double llon = Double.parseDouble(lon.trim());
                    return new GeoPointSearch(llat, llon);
                }
                catch (NumberFormatException e)
                {
                    return null;
                }
            }
            else
            {
                return null;
            }
        }
        if ("geo".equals(scheme) || "osmand.geo".equals(scheme))
        {
            //geo:
            final String schemeSpecific = data.getSchemeSpecificPart();
            if (schemeSpecific == null)
            {
                return null;
            }
            if (schemeSpecific.startsWith("0,0?q="))
            {
                //geo:0,0?q=34.99,-106.61(Treasure)
                //geo:0,0?q=1600+Amphitheatre+Parkway%2C+CA
                final String query = schemeSpecific.substring("0,0?q=".length());

                final Matcher matcher = Pattern.compile("([\\-0-9.]+),([\\-0-9.]+)(?:,[\\-0-9.]+)?\\((.+?)\\)").matcher(query);
                if (matcher.matches())
                {
                    final double lat = Double.valueOf(matcher.group(1));
                    final double lon = Double.valueOf(matcher.group(2));
                    final String name = matcher.group(3);

                    return new GeoPointSearch(lat, lon, name);
                }
                else
                {
                    //we suppose it's a search
                    return new GeoAddressSearch(query);
                }
            }
            else
            {
                //geo:47.6,-122.3
                //geo:47.6,-122.3?z=11
                //allow for http://tools.ietf.org/html/rfc5870 (geo uri) , just ignore everything after ';'
                final String pattern = "([\\-0-9.]+),([\\-0-9.]+)(?:,([\\-0-9.]+))?(?:\\?z=([0-9]+))?(?:;.*)?";
				int indexQ = schemeSpecific.indexOf("&q");
				final Matcher matcher;
				if (indexQ != -1){
					final String schemeQ = schemeSpecific.substring(0,indexQ);
					matcher = Pattern.compile(pattern).matcher(schemeQ);
				} else {
					matcher = Pattern.compile(pattern).matcher(schemeSpecific);
				}
				
				final String pattern2 = "([\\-0-9.]+),([\\-0-9.]+)(?:.*)"; //c:geo
				final Matcher matcher2 = Pattern.compile(pattern2).matcher(schemeSpecific);

                if (matcher.matches())
                {
                    final double lat = Double.valueOf(matcher.group(1));
                    final double lon = Double.valueOf(matcher.group(2));
                    if (matcher.group(4) == null)
                    {
                        return new GeoPointSearch(lat, lon);
                    }
                    else
                    {
                        return new GeoPointSearch(lat, lon, Integer.valueOf(matcher.group(4)));
                    }
                } else if (matcher2.matches()) {
                    final double lat = Double.valueOf(matcher2.group(1));
                    final double lon = Double.valueOf(matcher2.group(2));
                    return new GeoPointSearch(lat, lon);
                } else {
                    return null;
                }
            }
        }
        return null;
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
		public ExecutionResult execute() {
			if (elements.isEmpty()) {
				return ExecutionResult.EMPTY;
			}
			List<String> q = new ArrayList<String>(elements);
			MapObject geo = checkGeoPoint();
			if(geo != null) {
				return new ExecutionResult(Collections.singleton(geo));
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
				return new ExecutionResult(allStreets);
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
				return new ExecutionResult(all);
			} else {
				// add all other results to connected streets
				connectedStreets.addAll(cityIds.valueCollection());
				return new ExecutionResult(connectedStreets);
			}
		}

		private Collection<RegionAddressRepository> limitSearchToCountries(List<String> q) {
			ResourceManager resourceManager = getMyApplication().getResourceManager();
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

    private static class GeoPointSearch implements MyService {
		private final  MapObject point;
        private final int zoom;

        public GeoPointSearch(double lat , double lon)
        {
            this(lat, lon, ExecutionResult.NO_ZOOM);
        }

        public GeoPointSearch(double lat , double lon, int zoom)
        {
            this(lat, lon, "Lat: " + lat + ",Lon: " + lon, zoom);
        }

        public GeoPointSearch(double lat , double lon, String name )
        {
            this(lat, lon, name,ExecutionResult.NO_ZOOM);
        }

        public GeoPointSearch(double lat , double lon, String name, int zoom )
        {
			final Amenity amenity = new Amenity();
            amenity.setLocation(lat, lon);
            amenity.setName(name);
            amenity.setType(AmenityType.USER_DEFINED);
            amenity.setSubType("");

            this.point = amenity;
            this.zoom = zoom;
		}

        @Override
		public ExecutionResult execute() {
			if (point != null) {
				return new ExecutionResult(Collections.singletonList(point), zoom);
			} else {
				return ExecutionResult.EMPTY;
			}
		}

	}

    private static class ExecutionResult
    {
        public static final int NO_ZOOM = -1;
        public static final ExecutionResult EMPTY = new ExecutionResult(new ArrayList<MapObject>(), NO_ZOOM);

        private final Collection<? extends MapObject> mapObjects;
        private final int zoom;

        public ExecutionResult(final Collection<? extends MapObject> mapObjects)
        {
            this(mapObjects, NO_ZOOM);
        }

        public ExecutionResult(final Collection<? extends MapObject> mapObjects, final int zoom)
        {
            this.mapObjects = mapObjects;
            this.zoom = zoom;
        }

        public boolean isEmpty()
        {
            return mapObjects.isEmpty();
        }

        public boolean hasZoom()
        {
            return zoom != NO_ZOOM;
        }

        public Collection<? extends MapObject> getMapObjects()
        {
            return mapObjects;
        }

        public int getZoom()
        {
            return zoom;
        }

        @Override
        public String toString()
        {
            return "ExecutionResult{" +
                    "mapObjects=" + mapObjects +
                    ", zoom=" + zoom +
                    '}';
        }
    }

	private static interface MyService
    {
		public ExecutionResult execute();
	}
}
