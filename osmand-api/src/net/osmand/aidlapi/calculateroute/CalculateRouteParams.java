package net.osmand.aidlapi.calculateroute;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;
import net.osmand.aidlapi.map.ALatLon;

import java.util.ArrayList;
import java.util.List;

public class CalculateRouteParams extends AidlParams {

	private ALatLon startPoint;
	private String startPointName;
	private ALatLon endPoint;
	private String endPointName;
	private ArrayList<ALatLon> intermediatePoints = new ArrayList<>();
	private ArrayList<String> intermediateNames = new ArrayList<>();

	public CalculateRouteParams(ALatLon startPoint, String startPointName,
	                            ALatLon endPoint, String endPointName,
	                            List<ALatLon> intermediatePoints, List<String> intermediateNames) {

		if (endPoint == null) {
			throw new IllegalArgumentException("endPoint cannot be null");
		}

		this.startPoint = startPoint;
		this.startPointName = startPointName;
		this.endPoint = endPoint;
		this.endPointName = endPointName;
		if (intermediatePoints != null) {
			this.intermediatePoints.addAll(intermediatePoints);
		}
		if (intermediateNames != null) {
			this.intermediateNames.addAll(intermediateNames);
		}
	}

	public CalculateRouteParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<CalculateRouteParams> CREATOR = new Creator<CalculateRouteParams>() {
		@Override
		public CalculateRouteParams createFromParcel(Parcel in) {
			return new CalculateRouteParams(in);
		}

		@Override
		public CalculateRouteParams[] newArray(int size) {
			return new CalculateRouteParams[size];
		}
	};

	public ALatLon getStartPoint() {
		return startPoint;
	}

	public String getStartPointName() {
		return startPointName;
	}

	public ALatLon getEndPoint() {
		return endPoint;
	}

	public String getEndPointName() {
		return endPointName;
	}

	public List<ALatLon> getIntermediatePoints() {
		return intermediatePoints;
	}

	public List<String> getIntermediateNames() {
		return intermediateNames;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putParcelable("startPoint", startPoint);
		bundle.putString("startPointName", startPointName);
		bundle.putParcelable("endPoint", endPoint);
		bundle.putString("endPointName", endPointName);
		bundle.putParcelableArrayList("intermediatePoints", intermediatePoints);
		bundle.putStringArrayList("intermediateNames", intermediateNames);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		bundle.setClassLoader(ALatLon.class.getClassLoader());
		startPoint = bundle.getParcelable("startPoint");
		startPointName = bundle.getString("startPointName");
		endPoint = bundle.getParcelable("endPoint");
		endPointName = bundle.getString("endPointName");
		intermediatePoints = bundle.getParcelableArrayList("intermediatePoints");
		intermediateNames = bundle.getStringArrayList("intermediateNames");
	}
}