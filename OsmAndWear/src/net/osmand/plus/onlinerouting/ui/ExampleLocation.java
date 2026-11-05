package net.osmand.plus.onlinerouting.ui;

import androidx.annotation.NonNull;

import net.osmand.data.LatLon;

public enum ExampleLocation {

	AMSTERDAM("Amsterdam",
			new LatLon(52.379189, 4.899431),
			new LatLon(52.308056, 4.764167)),

	BERLIN("Berlin",
			new LatLon(52.520008, 13.404954),
			new LatLon(52.3666652, 13.501997992)),

	NEW_YORK("New York",
			new LatLon(43.000000, -75.000000),
			new LatLon(40.641766, -73.780968)),

	PARIS("Paris",
			new LatLon(48.864716, 2.349014),
			new LatLon(48.948437, 2.434931));

	ExampleLocation(@NonNull String name,
	                @NonNull LatLon cityCenterLatLon,
	                @NonNull LatLon cityAirportLatLon) {
		this.name = name;
		this.cityCenterLatLon = cityCenterLatLon;
		this.cityAirportLatLon = cityAirportLatLon;
	}

	private final String name;
	private final LatLon cityCenterLatLon;
	private final LatLon cityAirportLatLon;

	@NonNull
	public String getName() {
		return name;
	}

	@NonNull
	public LatLon getCityCenterLatLon() {
		return cityCenterLatLon;
	}

	@NonNull
	public LatLon getCityAirportLatLon() {
		return cityAirportLatLon;
	}

}
