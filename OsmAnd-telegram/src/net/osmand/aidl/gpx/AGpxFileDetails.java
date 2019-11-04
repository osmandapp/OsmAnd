package net.osmand.aidl.gpx;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AGpxFileDetails implements Parcelable {

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

	private List<String> wptCategoryNames = new ArrayList<>();

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
			this.wptCategoryNames = new ArrayList<>(wptCategoryNames);
		}
	}

	public AGpxFileDetails(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<AGpxFileDetails> CREATOR = new
			Creator<AGpxFileDetails>() {
				public AGpxFileDetails createFromParcel(Parcel in) {
					return new AGpxFileDetails(in);
				}

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

	public void writeToParcel(Parcel out, int flags) {
		out.writeFloat(totalDistance);
		out.writeInt(totalTracks);
		out.writeLong(startTime);
		out.writeLong(endTime);
		out.writeLong(timeSpan);
		out.writeLong(timeMoving);
		out.writeFloat(totalDistanceMoving);
		out.writeDouble(diffElevationUp);
		out.writeDouble(diffElevationDown);
		out.writeDouble(avgElevation);
		out.writeDouble(minElevation);
		out.writeDouble(maxElevation);
		out.writeFloat(minSpeed);
		out.writeFloat(maxSpeed);
		out.writeFloat(avgSpeed);
		out.writeInt(points);
		out.writeInt(wptPoints);
		out.writeStringList(wptCategoryNames);
	}

	private void readFromParcel(Parcel in) {
		totalDistance = in.readFloat();
		totalTracks = in.readInt();
		startTime = in.readLong();
		endTime = in.readLong();
		timeSpan = in.readLong();
		timeMoving = in.readLong();
		totalDistanceMoving = in.readFloat();
		diffElevationUp = in.readDouble();
		diffElevationDown = in.readDouble();
		avgElevation = in.readDouble();
		minElevation = in.readDouble();
		maxElevation = in.readDouble();
		minSpeed = in.readFloat();
		maxSpeed = in.readFloat();
		avgSpeed = in.readFloat();
		points = in.readInt();
		wptPoints = in.readInt();
		in.readStringList(wptCategoryNames);
	}

	public int describeContents() {
		return 0;
	}
}