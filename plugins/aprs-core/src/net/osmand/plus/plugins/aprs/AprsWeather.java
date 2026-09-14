package net.osmand.plus.plugins.aprs;

public class AprsWeather {

	public int windDirection = -1;
	public int windSpeed = -1;
	public int windGust = -1;
	public float temperatureF = Float.NaN;
	public float rainfallHour = Float.NaN;
	public float rainfall24h = Float.NaN;
	public float barometerMbar = Float.NaN;
	public int humidity = -1;

	public boolean hasData() {
		return windDirection >= 0 || windSpeed >= 0 || windGust >= 0
				|| !Float.isNaN(temperatureF) || !Float.isNaN(rainfallHour)
				|| !Float.isNaN(rainfall24h) || !Float.isNaN(barometerMbar)
				|| humidity >= 0;
	}
}
