package net.osmand;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import net.osmand.activities.OsmandApplication;
import net.osmand.data.Amenity;
import net.osmand.data.AmenityType;
import net.osmand.osm.MapUtils;
import net.sf.junidecode.Junidecode;

import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.util.Xml;

public class NameFinderPoiFilter extends PoiFilter {

	public static final String FILTER_ID = "name_finder"; //$NON-NLS-1$
	private static final Log log = LogUtil.getLog(NameFinderPoiFilter.class);
	
	List<Amenity> searchedAmenities = new ArrayList<Amenity>();
	

	private String query = ""; //$NON-NLS-1$
	
	public NameFinderPoiFilter(OsmandApplication application) {
		super(null, application);
		this.name = Messages.getMessage("poi_filter_namefinder"); //$NON-NLS-1$
		this.filterId = FILTER_ID;
	}
	
	@Override
	public List<Amenity> searchAgain(double lat, double lon) {
		MapUtils.sortListOfMapObject(searchedAmenities, lat, lon);
		return searchedAmenities;
	}
	
	@Override
	public String getSearchArea() {
		return ""; //$NON-NLS-1$
	}
	
	@Override
	public List<Amenity> initializeNewSearch(double lat, double lon, int firstTimeLimit) {
		return searchFurther(lat, lon);
	}
	@Override
	public boolean isSearchFurtherAvailable() {
		return true;
	}
	
	@Override
	public List<Amenity> searchFurther(double latitude, double longitude) {
		searchOnline(latitude, longitude, query);
		return searchedAmenities;
	}
	
	
	public String searchOnline(double latitude, double longitude, String filter){
		searchedAmenities.clear();
		query = filter;
		String q = query +  " near " +latitude+","+longitude;  //$NON-NLS-1$//$NON-NLS-2$
		try {
			URL url = new URL("http://gazetteer.openstreetmap.org/namefinder/search.xml?find="+URLEncoder.encode(q)); //$NON-NLS-1$
			InputStream stream = url.openStream();
			XmlPullParser parser = Xml.newPullParser();
			parser.setInput(stream, "UTF-8"); //$NON-NLS-1$
			int eventType;
			int namedDepth= 0;
			while ((eventType = parser.next()) != XmlPullParser.END_DOCUMENT) {
				if (eventType == XmlPullParser.START_TAG) {
					if (parser.getName().equals("searchresults")) { //$NON-NLS-1$
						String err = parser.getAttributeValue("", "error"); //$NON-NLS-1$ //$NON-NLS-2$
						if (err != null) {
							stream.close();
							return err;
						}
					}
					if (parser.getName().equals("named")) { //$NON-NLS-1$
						namedDepth++;
						if (namedDepth == 1) {
							try {
								Amenity a = new Amenity();
								a.setLocation(Double.parseDouble(parser.getAttributeValue("", "lat")), //$NON-NLS-1$//$NON-NLS-2$
										Double.parseDouble(parser.getAttributeValue("", "lon"))); //$NON-NLS-1$//$NON-NLS-2$
								a.setId(Long.parseLong(parser.getAttributeValue("", "id"))); //$NON-NLS-1$ //$NON-NLS-2$
								String name = parser.getAttributeValue("", "name");  //$NON-NLS-1$//$NON-NLS-2$
								a.setName(name);
								a.setEnName(Junidecode.unidecode(name));
								a.setType(AmenityType.OTHER);
								a.setSubType(parser.getAttributeValue("", "category"));  //$NON-NLS-1$//$NON-NLS-2$
								searchedAmenities.add(a);
							} catch (NullPointerException e) {
								log.info("Invalid attributes", e); //$NON-NLS-1$
							} catch (NumberFormatException e) {
								log.info("Invalid attributes", e); //$NON-NLS-1$
							}
						}
					}
				} else if (eventType == XmlPullParser.END_TAG) {
					if (parser.getName().equals("named")) { //$NON-NLS-1$
						namedDepth--;
					}
				}
			}
			stream.close();
		} catch (IOException e) {
			log.error("Error loading name finder poi", e); //$NON-NLS-1$
			return Messages.getMessage("input_output_error"); //$NON-NLS-1$
		} catch (XmlPullParserException e) {
			log.error("Error parsing name finder poi", e); //$NON-NLS-1$
			return Messages.getMessage("input_output_error"); //$NON-NLS-1$
		}
		MapUtils.sortListOfMapObject(searchedAmenities, latitude, longitude);
		return null;
	}
	
	public List<Amenity> getSearchedAmenities() {
		return searchedAmenities;
	}

	

}
