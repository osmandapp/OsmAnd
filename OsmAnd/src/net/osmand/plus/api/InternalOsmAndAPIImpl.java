package net.osmand.plus.api;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import net.osmand.NativeLibrary;
import net.osmand.ResultMatcher;
import net.osmand.data.Amenity;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.PoiFilter;
import net.osmand.plus.ResourceManager;
import net.osmand.plus.access.AccessibilityMode;
import net.osmand.plus.render.NativeOsmandLibrary;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;

import android.content.Context;
import android.util.Xml;
import android.view.accessibility.AccessibilityManager;

public class InternalOsmAndAPIImpl implements InternalOsmAndAPI {

	private OsmandApplication app;

	public InternalOsmAndAPIImpl(OsmandApplication app) {
		this.app = app;
	}

	@Override
	public XmlSerializer newSerializer() {
		return Xml.newSerializer();
	}

	@Override
	public XmlPullParser newPullParser() {
		return Xml.newPullParser();
	}

	@Override
	public String getPackageName() {
		return app.getPackageName();
	}

	@Override
	public InputStream openAsset(String name) throws IOException {
		return app.getAssets().open(name);
	}

	@Override
	public File getAppDir() {
		return app.getSettings().extendOsmandPath(ResourceManager.APP_DIR);
	}

	@Override
	public File getAppDir(String extend) {
		return app.getSettings().extendOsmandPath(ResourceManager.APP_DIR + extend);
	}

	@Override
	public NativeLibrary getNativeLibrary() {
		return NativeOsmandLibrary.getLoadedLibrary();
	}
	
	public boolean accessibilityExtensions() {
		return app.getSettings().ACCESSIBILITY_EXTENSIONS.get();
	}

	@Override
	public boolean accessibilityEnabled() {
		final AccessibilityMode mode = app.getSettings().ACCESSIBILITY_MODE.get();
		if (mode == AccessibilityMode.ON)
			return true;
		else if (mode == AccessibilityMode.OFF)
			return false;
		return ((AccessibilityManager) app.getSystemService(Context.ACCESSIBILITY_SERVICE)).isEnabled();
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


}
