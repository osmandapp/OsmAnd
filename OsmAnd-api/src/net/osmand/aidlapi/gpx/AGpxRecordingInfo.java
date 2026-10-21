package net.osmand.aidlapi.gpx;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

/**
 * State of trip recording: whether a track is being recorded and what it holds so far.
 */
public class AGpxRecordingInfo extends AidlParams {

	private boolean recording;
	private boolean pluginEnabled;
	private float distance;
	private long duration;
	private int points;
	private long lastPointTime;

	public AGpxRecordingInfo(boolean recording, boolean pluginEnabled, float distance, long duration,
	                         int points, long lastPointTime) {
		this.recording = recording;
		this.pluginEnabled = pluginEnabled;
		this.distance = distance;
		this.duration = duration;
		this.points = points;
		this.lastPointTime = lastPointTime;
	}

	public AGpxRecordingInfo(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<AGpxRecordingInfo> CREATOR = new Creator<AGpxRecordingInfo>() {
		@Override
		public AGpxRecordingInfo createFromParcel(Parcel in) {
			return new AGpxRecordingInfo(in);
		}

		@Override
		public AGpxRecordingInfo[] newArray(int size) {
			return new AGpxRecordingInfo[size];
		}
	};

	/**
	 * @return true while OsmAnd records a track
	 */
	public boolean isRecording() {
		return recording;
	}

	/**
	 * @return true when the Trip recording plugin is on; recording cannot start without it
	 */
	public boolean isPluginEnabled() {
		return pluginEnabled;
	}

	/**
	 * @return distance of the current track in meters
	 */
	public float getDistance() {
		return distance;
	}

	/**
	 * @return duration of the current track in milliseconds
	 */
	public long getDuration() {
		return duration;
	}

	/**
	 * @return number of track points in the current track
	 */
	public int getPoints() {
		return points;
	}

	/**
	 * @return time of the last recorded point in milliseconds, 0 if there is none
	 */
	public long getLastPointTime() {
		return lastPointTime;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putBoolean("recording", recording);
		bundle.putBoolean("pluginEnabled", pluginEnabled);
		bundle.putFloat("distance", distance);
		bundle.putLong("duration", duration);
		bundle.putInt("points", points);
		bundle.putLong("lastPointTime", lastPointTime);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		recording = bundle.getBoolean("recording");
		pluginEnabled = bundle.getBoolean("pluginEnabled");
		distance = bundle.getFloat("distance");
		duration = bundle.getLong("duration");
		points = bundle.getInt("points");
		lastPointTime = bundle.getLong("lastPointTime");
	}
}
