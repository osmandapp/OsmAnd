package net.osmand.plus.profiles.onlinerouting;

import net.osmand.data.LatLon;

public enum ExampleLocation {
	AMSTERDAM("Amsterdam", new LatLon(52.379189, 4.899431)),
	BERLIN("Berlin", new LatLon(52.520008, 13.404954)),
	NEW_YORK("New York", new LatLon(43.000000, -75.000000)),
	PARIS("Paris", new LatLon(48.864716, 2.349014));

	ExampleLocation(String title, LatLon latLon) {
		this.title = title;
		this.latLon = latLon;
	}

	private String title;
	private LatLon latLon;

	public String getTitle() {
		return title;
	}

	public LatLon getLatLon() {
		return latLon;
	}
}
