package net.osmand.plus.poi;

import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.data.Amenity;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiType;
import net.osmand.osm.io.NetworkUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.poi.PoiFilterUtils.AmenityNameFilter;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;
import net.osmand.util.TransliterationHelper;

import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class NominatimPoiFilter extends PoiUIFilter {

	private static final Log log = PlatformUtil.getLog(NominatimPoiFilter.class);

	private static final String FILTER_ID = "name_finder";
	private static final String NOMINATIM_API = "https://nominatim.openstreetmap.org/search";
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
			String urlq = NOMINATIM_API + "?format=xml" +
					"&addressdetails=0&accept-language=" + Locale.getDefault().getLanguage() +
					"&q=" + URLEncoder.encode(getFilterByName()) +
					"&addressdetails=1" + // nclude a breakdown of the address into elements
					"&limit=" + LIMIT;
			if (bboxSearch) {
				urlq += "&bounded=1&" + viewbox;
			}
			log.info("Online search: " + urlq);
			URLConnection connection = NetworkUtils.getHttpURLConnection(urlq); //$NON-NLS-1$
			InputStream stream = connection.getInputStream();
			XmlPullParser parser = PlatformUtil.newXMLPullParser();
			parser.setInput(stream, "UTF-8"); //$NON-NLS-1$
			int eventType;
			int namedDepth = 0;
			Amenity a = null;
			MapPoiTypes poiTypes = ((OsmandApplication) getApplication()).getPoiTypes();
			while ((eventType = parser.next()) != XmlPullParser.END_DOCUMENT) {
				if (eventType == XmlPullParser.START_TAG) {
					if (parser.getName().equals("searchresults")) { //$NON-NLS-1$
						String err = parser.getAttributeValue("", "error"); //$NON-NLS-1$ //$NON-NLS-2$
						if (err != null && err.length() > 0) {
							lastError = err;
							stream.close();
							break;
						}
					}
					if (parser.getName().equals("place")) { //$NON-NLS-1$
						namedDepth++;
						if (namedDepth == 1) {
							try {
								a = new Amenity();
								a.setLocation(Double.parseDouble(parser.getAttributeValue("", "lat")), //$NON-NLS-1$//$NON-NLS-2$
										Double.parseDouble(parser.getAttributeValue("", "lon"))); //$NON-NLS-1$//$NON-NLS-2$
								a.setId(Long.parseLong(parser.getAttributeValue("", "place_id"))); //$NON-NLS-1$ //$NON-NLS-2$
								String name = parser.getAttributeValue("", "display_name"); //$NON-NLS-1$//$NON-NLS-2$
								a.setName(name);
								a.setEnName(TransliterationHelper.transliterate(getName()));
								a.setSubType(parser.getAttributeValue("", "type")); //$NON-NLS-1$//$NON-NLS-2$
								PoiType pt = poiTypes.getPoiTypeByKey(a.getSubType());
								a.setType(pt != null ? pt.getCategory() : poiTypes.getOtherPoiCategory());
								if (matcher == null || matcher.publish(a)) {
									currentSearchResult.add(a);
								}
							} catch (NumberFormatException e) {
								log.info("Invalid attributes", e); //$NON-NLS-1$
							}
						}
					} else if (a != null && parser.getName().equals(a.getSubType())) {
						if (parser.next() == XmlPullParser.TEXT) {
							String name = parser.getText();
							if (name != null) {
								a.setName(name);
								a.setEnName(TransliterationHelper.transliterate(getName()));
							}
						}
					}
				} else if (eventType == XmlPullParser.END_TAG) {
					if (parser.getName().equals("place")) { //$NON-NLS-1$
						namedDepth--;
						if (namedDepth == 0) {
							a = null;
						}
					}
				}
			}
			stream.close();
		} catch (IOException e) {
			log.error("Error loading name finder poi", e); //$NON-NLS-1$
			lastError = getApplication().getString(R.string.shared_string_io_error); //$NON-NLS-1$
		} catch (XmlPullParserException e) {
			log.error("Error parsing name finder poi", e); //$NON-NLS-1$
			lastError = getApplication().getString(R.string.shared_string_io_error); //$NON-NLS-1$
		}
		MapUtils.sortListOfMapObject(currentSearchResult, lat, lon);
		return currentSearchResult;
	}
	
	public String getLastError() {
		return lastError;
	}
}
