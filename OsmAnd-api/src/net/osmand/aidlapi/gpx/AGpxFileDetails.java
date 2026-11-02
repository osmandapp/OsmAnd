package net.osmand.aidlapi.gpx;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AGpxFileDetails extends AidlParams {

	private float totalDistance;
	private int totalTracks;
	private long startTime = Long.MAX_VALUE;
	private long endTime = Long.MIN_VALUE;
	private long timeSpan;
	private long timeMoving;
	private float totalDistanceMoving;

	private double diffElevationUp;
	private double diffElevationDown;
	private double avgElevation;
	private double minElevation = 99999;
	private double maxElevation = -100;

	private float minSpeed = Float.MAX_VALUE;
	private float maxSpeed;
	private float avgSpeed;

	private int points;
	private int wptPoints;

	private ArrayList<String> wptCategoryNames = new ArrayList<>();

	// from external sensors, 0 when the track has none
	private float avgHeartRate;
	private int minHeartRate;
	private int maxHeartRate;
	private float avgSensorSpeed;
	private float maxSensorSpeed;
	private float avgPower;
	private int maxPower;
	private float avgCadence;
	private float maxCadence;
	private float avgTemperature;
	private int maxTemperature;

	public AGpxFileDetails(float totalDistance, int totalTracks,
	                       long startTime, long endTime,
	                       long timeSpan, long timeMoving, float totalDistanceMoving,
	                       double diffElevationUp, double diffElevationDown,
	                       double avgElevation, double minElevation, double maxElevation,
	                       float minSpeed, float maxSpeed, float avgSpeed,
	                       int points, int wptPoints, Set<String> wptCategoryNames) {
		this.totalDistance = totalDistance;
		this.totalTracks = totalTracks;
		this.startTime = startTime;
		this.endTime = endTime;
		this.timeSpan = timeSpan;
		this.timeMoving = timeMoving;
		this.totalDistanceMoving = totalDistanceMoving;
		this.diffElevationUp = diffElevationUp;
		this.diffElevationDown = diffElevationDown;
		this.avgElevation = avgElevation;
		this.minElevation = minElevation;
		this.maxElevation = maxElevation;
		this.minSpeed = minSpeed;
		this.maxSpeed = maxSpeed;
		this.avgSpeed = avgSpeed;
		this.points = points;
		this.wptPoints = wptPoints;
		if (wptCategoryNames != null) {
			this.wptCategoryNames.addAll(wptCategoryNames);
		}
	}

	public AGpxFileDetails(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<AGpxFileDetails> CREATOR = new Creator<AGpxFileDetails>() {
		@Override
		public AGpxFileDetails createFromParcel(Parcel in) {
			return new AGpxFileDetails(in);
		}

		@Override
		public AGpxFileDetails[] newArray(int size) {
			return new AGpxFileDetails[size];
		}
	};

	public float getTotalDistance() {
		return totalDistance;
	}

	public int getTotalTracks() {
		return totalTracks;
	}

	public long getStartTime() {
		return startTime;
	}

	public long getEndTime() {
		return endTime;
	}

	public long getTimeSpan() {
		return timeSpan;
	}

	public long getTimeMoving() {
		return timeMoving;
	}

	public float getTotalDistanceMoving() {
		return totalDistanceMoving;
	}

	public double getDiffElevationUp() {
		return diffElevationUp;
	}

	public double getDiffElevationDown() {
		return diffElevationDown;
	}

	public double getAvgElevation() {
		return avgElevation;
	}

	public double getMinElevation() {
		return minElevation;
	}

	public double getMaxElevation() {
		return maxElevation;
	}

	public float getMinSpeed() {
		return minSpeed;
	}

	public float getMaxSpeed() {
		return maxSpeed;
	}

	public float getAvgSpeed() {
		return avgSpeed;
	}

	public int getPoints() {
		return points;
	}

	public int getWptPoints() {
		return wptPoints;
	}

	public List<String> getWptCategoryNames() {
		return wptCategoryNames;
	}

	/**
	 * @return average heart rate, bpm, 0 if none
	 */
	public float getAvgHeartRate() {
		return avgHeartRate;
	}

	/**
	 * @return min heart rate, bpm, 0 if none
	 */
	public int getMinHeartRate() {
		return minHeartRate;
	}

	/**
	 * @return max heart rate, bpm, 0 if none
	 */
	public int getMaxHeartRate() {
		return maxHeartRate;
	}

	/**
	 * @return average speed from a speed sensor, m/s, 0 if none
	 */
	public float getAvgSensorSpeed() {
		return avgSensorSpeed;
	}

	/**
	 * @return max speed from a speed sensor, m/s, 0 if none
	 */
	public float getMaxSensorSpeed() {
		return maxSensorSpeed;
	}

	/**
	 * @return average power, W, 0 if none
	 */
	public float getAvgPower() {
		return avgPower;
	}

	/**
	 * @return max power, W, 0 if none
	 */
	public int getMaxPower() {
		return maxPower;
	}

	/**
	 * @return average cadence, rpm, 0 if none
	 */
	public float getAvgCadence() {
		return avgCadence;
	}

	/**
	 * @return max cadence, rpm, 0 if none
	 */
	public float getMaxCadence() {
		return maxCadence;
	}

	/**
	 * @return average temperature, °C, 0 if none
	 */
	public float getAvgTemperature() {
		return avgTemperature;
	}

	/**
	 * @return max temperature, °C, 0 if none
	 */
	public int getMaxTemperature() {
		return maxTemperature;
	}

	public void setHeartRate(float avgHeartRate, int minHeartRate, int maxHeartRate) {
		this.avgHeartRate = avgHeartRate;
		this.minHeartRate = minHeartRate;
		this.maxHeartRate = maxHeartRate;
	}

	public void setSensorSpeed(float avgSensorSpeed, float maxSensorSpeed) {
		this.avgSensorSpeed = avgSensorSpeed;
		this.maxSensorSpeed = maxSensorSpeed;
	}

	public void setPower(float avgPower, int maxPower) {
		this.avgPower = avgPower;
		this.maxPower = maxPower;
	}

	public void setCadence(float avgCadence, float maxCadence) {
		this.avgCadence = avgCadence;
		this.maxCadence = maxCadence;
	}

	public void setTemperature(float avgTemperature, int maxTemperature) {
		this.avgTemperature = avgTemperature;
		this.maxTemperature = maxTemperature;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putFloat("totalDistance", totalDistance);
		bundle.putInt("totalTracks", totalTracks);
		bundle.putLong("startTime", startTime);
		bundle.putLong("endTime", endTime);
		bundle.putLong("timeSpan", timeSpan);
		bundle.putLong("timeMoving", timeMoving);
		bundle.putFloat("totalDistanceMoving", totalDistanceMoving);
		bundle.putDouble("diffElevationUp", diffElevationUp);
		bundle.putDouble("diffElevationDown", diffElevationDown);
		bundle.putDouble("avgElevation", avgElevation);
		bundle.putDouble("minElevation", minElevation);
		bundle.putDouble("maxElevation", maxElevation);
		bundle.putFloat("minSpeed", minSpeed);
		bundle.putFloat("maxSpeed", maxSpeed);
		bundle.putFloat("avgSpeed", avgSpeed);
		bundle.putInt("points", points);
		bundle.putInt("wptPoints", wptPoints);
		bundle.putStringArrayList("wptCategoryNames", wptCategoryNames);
		// sensor values go only when present: a track list is one binder transaction (~1 MB),
		// readers default to 0
		if (avgHeartRate != 0) {
			bundle.putFloat("avgHeartRate", avgHeartRate);
		}
		if (minHeartRate != 0) {
			bundle.putInt("minHeartRate", minHeartRate);
		}
		if (maxHeartRate != 0) {
			bundle.putInt("maxHeartRate", maxHeartRate);
		}
		if (avgSensorSpeed != 0) {
			bundle.putFloat("avgSensorSpeed", avgSensorSpeed);
		}
		if (maxSensorSpeed != 0) {
			bundle.putFloat("maxSensorSpeed", maxSensorSpeed);
		}
		if (avgPower != 0) {
			bundle.putFloat("avgPower", avgPower);
		}
		if (maxPower != 0) {
			bundle.putInt("maxPower", maxPower);
		}
		if (avgCadence != 0) {
			bundle.putFloat("avgCadence", avgCadence);
		}
		if (maxCadence != 0) {
			bundle.putFloat("maxCadence", maxCadence);
		}
		if (avgTemperature != 0) {
			bundle.putFloat("avgTemperature", avgTemperature);
		}
		if (maxTemperature != 0) {
			bundle.putInt("maxTemperature", maxTemperature);
		}
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		totalDistance = bundle.getFloat("totalDistance");
		totalTracks = bundle.getInt("totalTracks");
		startTime = bundle.getLong("startTime");
		endTime = bundle.getLong("endTime");
		timeSpan = bundle.getLong("timeSpan");
		timeMoving = bundle.getLong("timeMoving");
		totalDistanceMoving = bundle.getFloat("totalDistanceMoving");
		diffElevationUp = bundle.getDouble("diffElevationUp");
		diffElevationDown = bundle.getDouble("diffElevationDown");
		avgElevation = bundle.getDouble("avgElevation");
		minElevation = bundle.getDouble("minElevation");
		maxElevation = bundle.getDouble("maxElevation");
		minSpeed = bundle.getFloat("minSpeed");
		maxSpeed = bundle.getFloat("maxSpeed");
		avgSpeed = bundle.getFloat("avgSpeed");
		points = bundle.getInt("points");
		wptPoints = bundle.getInt("wptPoints");
		wptCategoryNames = bundle.getStringArrayList("wptCategoryNames");
		avgHeartRate = bundle.getFloat("avgHeartRate");
		minHeartRate = bundle.getInt("minHeartRate");
		maxHeartRate = bundle.getInt("maxHeartRate");
		avgSensorSpeed = bundle.getFloat("avgSensorSpeed");
		maxSensorSpeed = bundle.getFloat("maxSensorSpeed");
		avgPower = bundle.getFloat("avgPower");
		maxPower = bundle.getInt("maxPower");
		avgCadence = bundle.getFloat("avgCadence");
		maxCadence = bundle.getFloat("maxCadence");
		avgTemperature = bundle.getFloat("avgTemperature");
		maxTemperature = bundle.getInt("maxTemperature");
	}
}