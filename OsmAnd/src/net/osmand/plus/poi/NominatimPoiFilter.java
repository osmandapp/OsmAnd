package net.osmand.plus.poi;

import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.binary.ObfConstants;
import net.osmand.data.Amenity;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiType;
import net.osmand.osm.edit.Entity.EntityType;
import net.osmand.osm.io.NetworkUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.poi.PoiFilterUtils.AmenityNameFilter;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;
import net.osmand.util.TransliterationHelper;

import org.apache.commons.logging.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class NominatimPoiFilter extends PoiUIFilter {

	private static final Log log = PlatformUtil.getLog(NominatimPoiFilter.class);

	private static final String FILTER_ID = "name_finder";
	private static final String NOMINATIM_API = "https://photon.adrianofrongillo.ovh/api";
	private static final int MIN_SEARCH_DISTANCE_ON_MAP = 20000;
	private static final int LIMIT = 300;

	private String lastError = "";
	private final boolean bboxSearch;
	
	public NominatimPoiFilter(OsmandApplication application, boolean noBbox) {
		super(application);
		this.bboxSearch = !noBbox;
		this.name = app.getString(R.string.poi_filter_nominatim);
		if (!bboxSearch) {
			this.name += " - " + app.getString(R.string.shared_string_address);
			this.distanceToSearchValues = new double[] {500, 10000};
			this.filterId = FILTER_ID + "_address";
		} else {
			this.name += " - " + app.getString(R.string.shared_string_places);
			this.distanceToSearchValues = new double[] {1, 2, 5, 10, 20, 100, 500, 10000};
			this.filterId = FILTER_ID + "_places";
		}
	}
	

	@Override
	public boolean isAutomaticallyIncreaseSearch() {
		return false;
	}
	
	// do nothing test jackdaw lane, oxford"
	@Override
	public AmenityNameFilter getNameFilter() {
		return a -> true;
	}
	
	@Override
	protected List<Amenity> searchAmenitiesInternal(double lat, double lon, double topLatitude,
	                                                double bottomLatitude, double leftLongitude,
	                                                double rightLongitude, int zoom,
	                                                ResultMatcher<Amenity> matcher) {
		currentSearchResult = new ArrayList<>();
		if (Algorithms.isEmpty(getFilterByName())) {
			return currentSearchResult;
		}

		double baseDistY = MapUtils.getDistance(lat, lon, lat - 1, lon);
		double baseDistX = MapUtils.getDistance(lat, lon, lat, lon - 1);
		double distance = MIN_SEARCH_DISTANCE_ON_MAP;
		topLatitude = Math.max(topLatitude, Math.min(lat + (distance / baseDistY), 84.));
		bottomLatitude = Math.min(bottomLatitude, Math.max(lat - (distance / baseDistY), -84.));
		leftLongitude = Math.min(leftLongitude, Math.max(lon - (distance / baseDistX), -180));
		rightLongitude = Math.max(rightLongitude, Math.min(lon + (distance / baseDistX), 180));

		String viewbox = "viewboxlbrt=" + ((float) leftLongitude) + "," + ((float) bottomLatitude)
				+ "," + ((float) rightLongitude) + "," + ((float) topLatitude);
		try {
			lastError = "";
			String urlq = NOMINATIM_API + "?q=" + URLEncoder.encode(getFilterByName()) +
					"&lat=" + lat + "&lon=" + lon + "&limit=" + LIMIT;

			log.info("Online search: " + urlq);
			URLConnection connection = NetworkUtils.getHttpURLConnection(urlq); //$NON-NLS-1$
			connection.setRequestProperty("User-Agent", Version.getFullVersion(app));

			InputStream stream = connection.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
			StringBuilder responseBuilder = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				responseBuilder.append(line);
			}
			stream.close();

			MapPoiTypes poiTypes = ((OsmandApplication) getApplication()).getPoiTypes();

			try {
				JSONObject root = new JSONObject(responseBuilder.toString());
				if (root.has("features")) {
					JSONArray features = root.getJSONArray("features");
					for (int i = 0; i < features.length(); i++) {
						JSONObject feature = features.getJSONObject(i);
						JSONObject properties = feature.optJSONObject("properties");
						JSONObject geometry = feature.optJSONObject("geometry");
						if (properties != null && geometry != null) {
							JSONArray coordinates = geometry.optJSONArray("coordinates");
							if (coordinates != null && coordinates.length() >= 2) {
								Amenity a = new Amenity();
								double featureLon = coordinates.getDouble(0);
								double featureLat = coordinates.getDouble(1);
								a.setLocation(featureLat, featureLon);

								long osmId = properties.optLong("osm_id", 0);
								String osmTypeStr = properties.optString("osm_type", "N").toUpperCase();
								EntityType osmType;
								if ("N".equals(osmTypeStr)) osmType = EntityType.NODE;
								else if ("W".equals(osmTypeStr)) osmType = EntityType.WAY;
								else if ("R".equals(osmTypeStr)) osmType = EntityType.RELATION;
								else osmType = EntityType.NODE;

								long id = ObfConstants.createMapObjectIdFromCleanOsmId(osmId, osmType);
								a.setId(id);

								String name = properties.optString("name", "");
								if (name.isEmpty()) {
									// Fallback if name is empty, create a display name from other fields
									String street = properties.optString("street", "");
									String housenumber = properties.optString("housenumber", "");
									String city = properties.optString("city", "");
									if (!street.isEmpty()) {
										name = street + (housenumber.isEmpty() ? "" : " " + housenumber) + (city.isEmpty() ? "" : ", " + city);
									}
								}
								a.setName(name);
								a.setEnName(TransliterationHelper.transliterate(name));

								String subType = properties.optString("osm_value", "");
								a.setSubType(subType);
								PoiType pt = poiTypes.getPoiTypeByKey(a.getSubType());
								a.setType(pt != null ? pt.getCategory() : poiTypes.getOtherPoiCategory());

								if (matcher == null || matcher.publish(a)) {
									currentSearchResult.add(a);
								}
							}
						}
					}
				}
			} catch (JSONException e) {
				log.error("Error parsing photon poi json", e);
				lastError = getApplication().getString(R.string.shared_string_io_error);
			}

		} catch (IOException e) {
			log.error("Error loading name finder poi", e); //$NON-NLS-1$
			lastError = getApplication().getString(R.string.shared_string_io_error); //$NON-NLS-1$
		}
		MapUtils.sortListOfMapObject(currentSearchResult, lat, lon);
		return currentSearchResult;
	}
	
	public String getLastError() {
		return lastError;
	}
}
