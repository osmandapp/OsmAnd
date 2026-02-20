package net.osmand.plus.plugins.monitoring.live;

import androidx.annotation.NonNull;

import net.osmand.Location;

class LiveMonitoringData {

	protected static final int NUMBER_OF_LIVE_DATA_FIELDS = 14;    //change the value after each addition\deletion of data field

	protected final double lat;
	protected final double lon;
	protected final float alt;
	protected final float speed;
	protected final float bearing;
	protected final float hdop;
	protected final long time;
	protected final int battery;

	protected long timeToArrival;
	protected long timeToIntermediateOrFinish;
	protected int distanceToArrivalOrMarker;
	protected int distanceToIntermediateOrFinish;

	public LiveMonitoringData(@NonNull Location location, long time, int battery) {
		this(location.getLatitude(), location.getLongitude(), (float) location.getAltitude(),
				location.getSpeed(), location.getAccuracy(), location.getBearing(), time, battery);
	}

	public LiveMonitoringData(double lat, double lon, float alt, float speed, float hdop,
			float bearing, long time, int battery) {
		this.lat = lat;
		this.lon = lon;
		this.alt = alt;
		this.speed = speed;
		this.hdop = hdop;
		this.time = time;
		this.bearing = bearing;
		this.battery = battery;
	}

	public void setTimesAndDistances(long timeToArrival, long timeToIntermediateOrFinish,
			int distanceToArrivalOrMarker, int distanceToIntermediateOrFinish) {
		this.timeToArrival = timeToArrival;
		this.timeToIntermediateOrFinish = timeToIntermediateOrFinish;
		this.distanceToArrivalOrMarker = distanceToArrivalOrMarker;
		this.distanceToIntermediateOrFinish = distanceToIntermediateOrFinish;
	}
}
