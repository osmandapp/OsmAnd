package net.osmand.aidlapi.gpx;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

import java.util.ArrayList;

/**
 * Page of track points.
 */
public class AGpxPoints extends AidlParams {

	private int total;
	private ArrayList<AGpxPoint> points;

	public AGpxPoints(int total, ArrayList<AGpxPoint> points) {
		this.total = total;
		this.points = points;
	}

	public AGpxPoints(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<AGpxPoints> CREATOR = new Creator<AGpxPoints>() {
		@Override
		public AGpxPoints createFromParcel(Parcel in) {
			return new AGpxPoints(in);
		}

		@Override
		public AGpxPoints[] newArray(int size) {
			return new AGpxPoints[size];
		}
	};

	/**
	 * @return number of track points in the whole track
	 */
	public int getTotal() {
		return total;
	}

	/**
	 * @return points of this page
	 */
	public ArrayList<AGpxPoint> getPoints() {
		return points;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putInt("total", total);
		bundle.putParcelableArrayList("points", points);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		total = bundle.getInt("total");
		bundle.setClassLoader(AGpxPoint.class.getClassLoader());
		points = bundle.getParcelableArrayList("points");
	}
}
