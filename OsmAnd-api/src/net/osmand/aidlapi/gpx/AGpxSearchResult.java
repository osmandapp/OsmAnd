package net.osmand.aidlapi.gpx;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

import java.util.ArrayList;

/**
 * A page of track files found by searchGpx.
 */
public class AGpxSearchResult extends AidlParams {

	private int total;
	private int found;
	private ArrayList<AGpxFile> files;
	private ArrayList<String> activityTypes;

	public AGpxSearchResult(int total, int found, ArrayList<AGpxFile> files, ArrayList<String> activityTypes) {
		this.total = total;
		this.found = found;
		this.files = files;
		this.activityTypes = activityTypes;
	}

	public AGpxSearchResult(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<AGpxSearchResult> CREATOR = new Creator<AGpxSearchResult>() {
		@Override
		public AGpxSearchResult createFromParcel(Parcel in) {
			return new AGpxSearchResult(in);
		}

		@Override
		public AGpxSearchResult[] newArray(int size) {
			return new AGpxSearchResult[size];
		}
	};

	/**
	 * @return number of all track files
	 */
	public int getTotal() {
		return total;
	}

	/**
	 * @return number of tracks matching the filters
	 */
	public int getFound() {
		return found;
	}

	/**
	 * @return tracks of the requested page
	 */
	public ArrayList<AGpxFile> getFiles() {
		return files;
	}

	/**
	 * @return activity ids set on any track, to filter by
	 */
	public ArrayList<String> getActivityTypes() {
		return activityTypes;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putInt("total", total);
		bundle.putInt("found", found);
		bundle.putParcelableArrayList("files", files);
		bundle.putStringArrayList("activityTypes", activityTypes);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		bundle.setClassLoader(AGpxFile.class.getClassLoader());
		total = bundle.getInt("total");
		found = bundle.getInt("found");
		files = bundle.getParcelableArrayList("files");
		activityTypes = bundle.getStringArrayList("activityTypes");
	}
}
