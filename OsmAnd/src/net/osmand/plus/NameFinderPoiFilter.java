package net.osmand.plus;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.data.Amenity;
import net.osmand.data.AmenityType;
import net.osmand.util.MapUtils;
import net.sf.junidecode.Junidecode;

import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;


public class NameFinderPoiFilter extends PoiFilter {

	public static final String FILTER_ID = "name_finder"; //$NON-NLS-1$
	private static final Log log = PlatformUtil.getLog(NameFinderPoiFilter.class);
	private static final int LIMIT = 300;
	
	List<Amenity> searchedAmenities = new ArrayList<Amenity>();
	
	private String query = ""; //$NON-NLS-1$
	private String lastError = ""; //$NON-NLS-1$
	
	public NameFinderPoiFilter(OsmandApplication application) {
		super(null, application);
		this.name = application.getString(R.string.poi_filter_nominatim); //$NON-NLS-1$
		this.distanceToSearchValues = new double[] {1, 2, 5, 10, 20, 50, 100, 200, 500 };
		this.filterId = FILTER_ID;
	}
	
	@Override
	public List<Amenity> searchAgain(double lat, double lon) {
		MapUtils.sortListOfMapObject(searchedAmenities, lat, lon);
		return searchedAmenities;
	}
	
	public String getQuery() {
		return query;
	}
	
	public void setQuery(String query) {
		this.query = query;
	}
	
	@Override
	protected List<Amenity> searchAmenities(double lat, double lon, double topLatitude,
			double bottomLatitude, double leftLongitude, double rightLongitude, ResultMatcher<Amenity> matcher) {
		
		final int deviceApiVersion = android.os.Build.VERSION.SDK_INT;

		String NOMINATIM_API;
	
		if (deviceApiVersion >= android.os.Build.VERSION_CODES.GINGERBREAD) {
			NOMINATIM_API = "https://nominatim.openstreetmap.org/search/";
		}
		else {
			NOMINATIM_API = "http://nominatim.openstreetmap.org/search/";
		}
			
		searchedAmenities.clear();
		
		String viewbox = "viewboxlbrt="+((float) leftLongitude)+","+((float) bottomLatitude)+","+((float) rightLongitude)+","+((float) topLatitude);
		try {
			lastError = "";
			String urlq = NOMINATIM_API + URLEncoder.encode(query)+ "?format=xml&addressdetails=1&limit="+LIMIT+"&bounded=1&"+viewbox;
			log.info(urlq);
			URL url = new URL(urlq); //$NON-NLS-1$
			InputStream stream = url.openStream();
			XmlPullParser parser = PlatformUtil.newXMLPullParser();
			parser.setInput(stream, "UTF-8"); //$NON-NLS-1$
			int eventType;
			int namedDepth= 0;
			Amenity a = null;
			while ((eventType = parser.next()) != XmlPullParser.END_DOCUMENT) {
				if (eventType == XmlPullParser.START_TAG) {
					if (parser.getName().equals("searchresults")) { //$NON-NLS-1$
						String err = parser.getAttributeValue("", "error"); //$NON-NLS-1$ //$NON-NLS-2$
						if (err != null && err.length() > 0) {
							lastError = err;
							stream.close();
							return searchedAmenities;
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
								String name = parser.getAttributeValue("", "display_name");  //$NON-NLS-1$//$NON-NLS-2$
								a.setName(name);
								a.setEnName(Junidecode.unidecode(name));
								a.setType(AmenityType.OTHER);
								a.setSubType(parser.getAttributeValue("", "type"));  //$NON-NLS-1$//$NON-NLS-2$
								if (matcher == null || matcher.publish(a)) {
									searchedAmenities.add(a);
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
								a.setEnName(Junidecode.unidecode(name));
							}
						}
					}
				} else if (eventType == XmlPullParser.END_TAG) {
					if (parser.getName().equals("place")) { //$NON-NLS-1$
						namedDepth--;
						if(namedDepth == 0){
							a = null;
						}
					}
				}
			}
			stream.close();
		} catch (IOException e) {
			log.error("Error loading name finder poi", e); //$NON-NLS-1$
			lastError = getApplication().getString(R.string.input_output_error); //$NON-NLS-1$
		} catch (XmlPullParserException e) {
			log.error("Error parsing name finder poi", e); //$NON-NLS-1$
			lastError = getApplication().getString(R.string.input_output_error); //$NON-NLS-1$
		}
		MapUtils.sortListOfMapObject(searchedAmenities, lat, lon);
		return searchedAmenities;
	}
	
	public String getLastError() {
		return lastError;
	}
	
	public List<Amenity> getSearchedAmenities() {
		return searchedAmenities;
	}

	

}
