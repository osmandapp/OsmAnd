package net.osmand.core.samples.android.sample1.adapters;

import net.osmand.core.jni.LatLon;
import net.osmand.core.jni.PointI;
import net.osmand.core.jni.Utilities;
import net.osmand.core.samples.android.sample1.SampleApplication;
import net.osmand.core.samples.android.sample1.search.objects.SearchObject;
import net.osmand.core.samples.android.sample1.search.objects.SearchPositionObject;

public class SearchListPositionItem extends SearchListItem {

	private double latitude;
	private double longitude;


	public SearchListPositionItem(SampleApplication app, SearchPositionObject searchObject) {
		super(app, searchObject);
		PointI position31 = searchObject.getPosition31();
		LatLon latLon = Utilities.convert31ToLatLon(position31);
		latitude = latLon.getLatitude();
		longitude = latLon.getLongitude();
	}

	public SearchPositionObject getSearchPositionObject() {
		return (SearchPositionObject) getSearchObject();
	}

	public double getLatitude() {
		return latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public double getDistance() {
		return getSearchPositionObject().getDistance();
	}

	public void setDistance(double distance) {
		getSearchPositionObject().setDistance(distance);
	}

}
