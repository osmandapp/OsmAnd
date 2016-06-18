package net.osmand.core.samples.android.sample1.adapters;

import android.graphics.drawable.Drawable;

import net.osmand.core.samples.android.sample1.SampleApplication;
import net.osmand.core.samples.android.sample1.search.AmenitySearchItem;
import net.osmand.core.samples.android.sample1.search.SearchItem;

public class SearchListItem {

	protected SampleApplication app;
	private SearchItem searchItem;
	private double distance;

	public SearchListItem(SampleApplication app, SearchItem searchItem) {
		this.app = app;
		this.searchItem = searchItem;
	}

	public static SearchListItem buildListItem(SampleApplication app, SearchItem item) {

		if (item instanceof AmenitySearchItem) {
			return new AmenitySearchListItem(app, (AmenitySearchItem) item);
		}
		return null;
	}

	public double getLatitude() {
		return searchItem.getLatitude();
	}

	public double getLongitude() {
		return searchItem.getLongitude();
	}

	public String getName() {
		return searchItem.getName();
	}

	public String getType() {
		return searchItem.getType();
	}

	public double getDistance() {
		return distance;
	}

	public void setDistance(double distance) {
		this.distance = distance;
	}

	public Drawable getIcon() {
		return null;
	}

}
