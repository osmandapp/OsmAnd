package net.osmand.aidlapi.gpx;

import android.os.Bundle;
import android.os.Parcel;

import androidx.annotation.Nullable;

import net.osmand.aidlapi.AidlParams;

/**
 * Filters, order and page for searching track files. Unset filters match every track.
 */
public class GpxSearchParams extends AidlParams {

	public static final String SORT_NEWEST = "newest";
	public static final String SORT_OLDEST = "oldest";
	public static final String SORT_LONGEST = "longest";
	public static final String SORT_NAME = "name";

	private String query;
	private String folder;
	private String activityType;
	private long fromTime;
	private long toTime;
	private double minDistance;
	private double maxDistance;
	private boolean shownOnly;
	private double minDescent;
	private double minElevationRange;
	private float minMaxSpeed;
	private float maxMaxSpeed;
	private float maxAvgSpeed;
	private String sort = SORT_NEWEST;
	private int offset;
	private int limit = 50;

	public GpxSearchParams() {
	}

	public GpxSearchParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<GpxSearchParams> CREATOR = new Creator<GpxSearchParams>() {
		@Override
		public GpxSearchParams createFromParcel(Parcel in) {
			return new GpxSearchParams(in);
		}

		@Override
		public GpxSearchParams[] newArray(int size) {
			return new GpxSearchParams[size];
		}
	};

	/**
	 * Part of the file path, case insensitive.
	 */
	public void setQuery(@Nullable String query) {
		this.query = query;
	}

	@Nullable
	public String getQuery() {
		return query;
	}

	/**
	 * Folder relative to the tracks folder, e.g. "rec"; subfolders match too.
	 */
	public void setFolder(@Nullable String folder) {
		this.folder = folder;
	}

	@Nullable
	public String getFolder() {
		return folder;
	}

	/**
	 * Activity id, e.g. "skiing".
	 */
	public void setActivityType(@Nullable String activityType) {
		this.activityType = activityType;
	}

	@Nullable
	public String getActivityType() {
		return activityType;
	}

	/**
	 * Track start time range in milliseconds; 0 leaves the side open.
	 */
	public void setTimeRange(long fromTime, long toTime) {
		this.fromTime = fromTime;
		this.toTime = toTime;
	}

	public long getFromTime() {
		return fromTime;
	}

	public long getToTime() {
		return toTime;
	}

	/**
	 * Distance range in meters; 0 leaves the side open.
	 */
	public void setDistanceRange(double minDistance, double maxDistance) {
		this.minDistance = minDistance;
		this.maxDistance = maxDistance;
	}

	public double getMinDistance() {
		return minDistance;
	}

	public double getMaxDistance() {
		return maxDistance;
	}

	/**
	 * Only tracks shown on the map.
	 */
	public void setShownOnly(boolean shownOnly) {
		this.shownOnly = shownOnly;
	}

	public boolean isShownOnly() {
		return shownOnly;
	}

	/**
	 * Min total descent in meters; 0 for any.
	 */
	public void setMinDescent(double minDescent) {
		this.minDescent = minDescent;
	}

	public double getMinDescent() {
		return minDescent;
	}

	/**
	 * Min difference between the highest and the lowest point in meters; 0 for any.
	 */
	public void setMinElevationRange(double minElevationRange) {
		this.minElevationRange = minElevationRange;
	}

	public double getMinElevationRange() {
		return minElevationRange;
	}

	/**
	 * Range of the track's max speed in m/s; 0 leaves the side open.
	 */
	public void setMaxSpeedRange(float minMaxSpeed, float maxMaxSpeed) {
		this.minMaxSpeed = minMaxSpeed;
		this.maxMaxSpeed = maxMaxSpeed;
	}

	public float getMinMaxSpeed() {
		return minMaxSpeed;
	}

	public float getMaxMaxSpeed() {
		return maxMaxSpeed;
	}

	/**
	 * Upper limit of the average speed over the whole time in m/s; 0 for any.
	 */
	public void setMaxAvgSpeed(float maxAvgSpeed) {
		this.maxAvgSpeed = maxAvgSpeed;
	}

	public float getMaxAvgSpeed() {
		return maxAvgSpeed;
	}

	/**
	 * One of SORT_NEWEST (default), SORT_OLDEST, SORT_LONGEST, SORT_NAME.
	 */
	public void setSort(@Nullable String sort) {
		this.sort = sort;
	}

	@Nullable
	public String getSort() {
		return sort;
	}

	public void setPage(int offset, int limit) {
		this.offset = offset;
		this.limit = limit;
	}

	public int getOffset() {
		return offset;
	}

	public int getLimit() {
		return limit;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putString("query", query);
		bundle.putString("folder", folder);
		bundle.putString("activityType", activityType);
		bundle.putLong("fromTime", fromTime);
		bundle.putLong("toTime", toTime);
		bundle.putDouble("minDistance", minDistance);
		bundle.putDouble("maxDistance", maxDistance);
		bundle.putBoolean("shownOnly", shownOnly);
		bundle.putDouble("minDescent", minDescent);
		bundle.putDouble("minElevationRange", minElevationRange);
		bundle.putFloat("minMaxSpeed", minMaxSpeed);
		bundle.putFloat("maxMaxSpeed", maxMaxSpeed);
		bundle.putFloat("maxAvgSpeed", maxAvgSpeed);
		bundle.putString("sort", sort);
		bundle.putInt("offset", offset);
		bundle.putInt("limit", limit);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		query = bundle.getString("query");
		folder = bundle.getString("folder");
		activityType = bundle.getString("activityType");
		fromTime = bundle.getLong("fromTime");
		toTime = bundle.getLong("toTime");
		minDistance = bundle.getDouble("minDistance");
		maxDistance = bundle.getDouble("maxDistance");
		shownOnly = bundle.getBoolean("shownOnly");
		minDescent = bundle.getDouble("minDescent");
		minElevationRange = bundle.getDouble("minElevationRange");
		minMaxSpeed = bundle.getFloat("minMaxSpeed");
		maxMaxSpeed = bundle.getFloat("maxMaxSpeed");
		maxAvgSpeed = bundle.getFloat("maxAvgSpeed");
		sort = bundle.getString("sort", SORT_NEWEST);
		offset = bundle.getInt("offset");
		limit = bundle.getInt("limit", 50);
	}
}
