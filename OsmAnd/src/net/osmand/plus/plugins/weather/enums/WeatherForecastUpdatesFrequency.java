package net.osmand.plus.plugins.weather.enums;

public enum WeatherForecastUpdatesFrequency {
	UNDEFINED(-1),
	SEMI_DAILY(43_200),
	DAILY(86_400),
	WEEKLY(604_800);

	WeatherForecastUpdatesFrequency(int secondsRequired) {
		this.secondsRequired = secondsRequired;
	}

	private final int secondsRequired;

	public int getSecondsRequired() {
		return secondsRequired;
	}
}
