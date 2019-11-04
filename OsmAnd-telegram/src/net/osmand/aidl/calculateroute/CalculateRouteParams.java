package net.osmand.aidl.calculateroute;

import android.os.Parcel;
import android.os.Parcelable;

import net.osmand.aidl.map.ALatLon;

import java.util.ArrayList;
import java.util.List;

public class CalculateRouteParams implements Parcelable {

	private ALatLon startPoint;
	private String startPointName;
	private ALatLon endPoint;
	private String endPointName;
	private List<ALatLon> intermediatePoints = new ArrayList<>();
	private List<String> intermediateNames = new ArrayList<>();

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

	public static final Parcelable.Creator<CalculateRouteParams> CREATOR = new
			Parcelable.Creator<CalculateRouteParams>() {
				public CalculateRouteParams createFromParcel(Parcel in) {
					return new CalculateRouteParams(in);
				}

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

	public void writeToParcel(Parcel out, int flags) {
		out.writeParcelable(startPoint, flags);
		out.writeString(startPointName);
		out.writeParcelable(endPoint, flags);
		out.writeString(endPointName);
		out.writeTypedList(intermediatePoints);
		out.writeStringList(intermediateNames);
	}

	private void readFromParcel(Parcel in) {
		startPoint = in.readParcelable(ALatLon.class.getClassLoader());
		startPointName = in.readString();
		endPoint = in.readParcelable(ALatLon.class.getClassLoader());
		endPointName = in.readString();
		in.readTypedList(intermediatePoints, ALatLon.CREATOR);
		in.readStringList(intermediateNames);
	}

	public int describeContents() {
		return 0;
	}
}
