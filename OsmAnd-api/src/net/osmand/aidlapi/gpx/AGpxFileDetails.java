package net.osmand.aidlapi.gpx;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AGpxFileDetails extends AidlParams {

	private float totalDistance = 0;
	private int totalTracks = 0;
	private long startTime = Long.MAX_VALUE;
	private long endTime = Long.MIN_VALUE;
	private long timeSpan = 0;
	private long timeMoving = 0;
	private float totalDistanceMoving = 0;

	private double diffElevationUp = 0;
	private double diffElevationDown = 0;
	private double avgElevation = 0;
	private double minElevation = 99999;
	private double maxElevation = -100;

	private float minSpeed = Float.MAX_VALUE;
	private float maxSpeed = 0;
	private float avgSpeed;

	private int points;
	private int wptPoints = 0;

	private ArrayList<String> wptCategoryNames = new ArrayList<>();

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
	}
}