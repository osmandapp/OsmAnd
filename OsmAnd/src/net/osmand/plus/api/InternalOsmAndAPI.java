package net.osmand.plus.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import net.osmand.ResultMatcher;
import net.osmand.data.Amenity;
import net.osmand.plus.PoiFilter;
import net.osmand.plus.TargetPointsHelper;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;

public interface InternalOsmAndAPI {

	public XmlSerializer newSerializer();

	public XmlPullParser newPullParser();
	
	public InputStream openAsset(String name) throws IOException;
	
	public List<Amenity> searchAmenities(PoiFilter filter,
			double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude,
			double lat, double lon, ResultMatcher<Amenity> matcher);
	
	public List<Amenity> searchAmenitiesByName(String searchQuery,
			double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude, 
			double lat, double lon, ResultMatcher<Amenity> matcher);

	
	public boolean isNavigationServiceStarted();
	
	public boolean isNavigationServiceStartedForNavigation();
	
	public void startNavigationService(boolean forNavigation);
	
	public void stopNavigationService();
	
}
