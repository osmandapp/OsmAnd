package net.osmand.plus.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import net.osmand.ResultMatcher;
import net.osmand.data.Amenity;
import net.osmand.plus.NavigationService;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.PoiFilter;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;

import android.content.Intent;
import android.util.Xml;

public class InternalOsmAndAPIImpl implements InternalOsmAndAPI {

	private OsmandApplication app;

	public InternalOsmAndAPIImpl(OsmandApplication app) {
		this.app = app;
	}


	@Override
	public List<Amenity> searchAmenities(PoiFilter filter, double topLatitude, double leftLongitude, double bottomLatitude,
			double rightLongitude, double lat, double lon, ResultMatcher<Amenity> matcher) {
		return app.getResourceManager().searchAmenities(filter, topLatitude, leftLongitude, bottomLatitude, rightLongitude, lat, lon, matcher);
	}

	@Override
	public List<Amenity> searchAmenitiesByName(String searchQuery, double topLatitude, double leftLongitude, double bottomLatitude,
			double rightLongitude, double lat, double lon, ResultMatcher<Amenity> matcher) {
		return app.getResourceManager().searchAmenitiesByName(searchQuery, topLatitude, leftLongitude, bottomLatitude, rightLongitude, lat, lon, matcher);
	}


	@Override
	public boolean isNavigationServiceStarted() {
		return app.getNavigationService() != null;
	}

	@Override
	public boolean isNavigationServiceStartedForNavigation() {
		return app.getNavigationService() != null && app.getNavigationService().startedForNavigation();
	}

	@Override
	public void startNavigationService(boolean forNavigation) {
		Intent serviceIntent = new Intent(app, NavigationService.class);
		if(forNavigation) {
			serviceIntent.putExtra(NavigationService.NAVIGATION_START_SERVICE_PARAM, true);
		}
		app.startService(serviceIntent);
	}

	@Override
	public void stopNavigationService() {
		Intent serviceIntent = new Intent(app, NavigationService.class);
		app.stopService(serviceIntent);
		
	}


}
