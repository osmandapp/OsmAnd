package net.osmand.plus.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import net.osmand.NativeLibrary;
import net.osmand.ResultMatcher;
import net.osmand.data.Amenity;
import net.osmand.plus.NavigationService;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.PoiFilter;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.access.AccessibilityMode;
import net.osmand.plus.render.NativeOsmandLibrary;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
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
	public NativeLibrary getNativeLibrary() {
		return NativeOsmandLibrary.getLoadedLibrary();
	}
	
	public boolean accessibilityExtensions() {
		return (Build.VERSION.SDK_INT < 14) ? app.getSettings().ACCESSIBILITY_EXTENSIONS.get() : false;
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

	@Override
	public String getVersionName() {
		try {
			PackageInfo info = app.getPackageManager().getPackageInfo(app.getPackageName(), 0);
			return info.versionName;
		} catch (NameNotFoundException e) {
			return "";
		}
	}

	@Override
	public int getVersionCode() {
		try {
			PackageInfo info = app.getPackageManager().getPackageInfo(app.getPackageName(), 0);
			return info.versionCode;
		} catch (NameNotFoundException e) {
			return 0;
		}
	}

	@Override
	public String getDeviceName() {
		return Build.DEVICE;
	}

	@Override
	public String getBrandName() {
		return Build.BRAND;
	}

	@Override
	public String getModelName() {
		return Build.MODEL;
	}

	@Override
	public TargetPointsHelper getTargetPointsHelper() {
		return app.getTargetPointsHelper();
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
