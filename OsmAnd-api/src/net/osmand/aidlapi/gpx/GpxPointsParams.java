package net.osmand.aidlapi.gpx;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

/**
 * Page of track points of a GPX file or of the track being recorded.
 */
public class GpxPointsParams extends AidlParams {

	private String fileName;
	private int offset;
	private int limit;

	public GpxPointsParams(String fileName, int offset, int limit) {
		this.fileName = fileName;
		this.offset = offset;
		this.limit = limit;
	}

	public GpxPointsParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<GpxPointsParams> CREATOR = new Creator<GpxPointsParams>() {
		@Override
		public GpxPointsParams createFromParcel(Parcel in) {
			return new GpxPointsParams(in);
		}

		@Override
		public GpxPointsParams[] newArray(int size) {
			return new GpxPointsParams[size];
		}
	};

	/**
	 * @return file relative to the tracks folder; empty for the track being recorded
	 */
	public String getFileName() {
		return fileName;
	}

	/**
	 * @return index of the first point
	 */
	public int getOffset() {
		return offset;
	}

	/**
	 * @return max points to return
	 */
	public int getLimit() {
		return limit;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putString("fileName", fileName);
		bundle.putInt("offset", offset);
		bundle.putInt("limit", limit);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		fileName = bundle.getString("fileName");
		offset = bundle.getInt("offset");
		limit = bundle.getInt("limit");
	}
}
