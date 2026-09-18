package net.osmand.aidlapi.navigation;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

public class GetActiveRouteParams extends AidlParams {

	private boolean includePassedSegment;

	public GetActiveRouteParams() {
	}

	public GetActiveRouteParams(boolean includePassedSegment) {
		this.includePassedSegment = includePassedSegment;
	}

	protected GetActiveRouteParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<GetActiveRouteParams> CREATOR = new Creator<GetActiveRouteParams>() {
		@Override
		public GetActiveRouteParams createFromParcel(Parcel in) {
			return new GetActiveRouteParams(in);
		}

		@Override
		public GetActiveRouteParams[] newArray(int size) {
			return new GetActiveRouteParams[size];
		}
	};

	public boolean isIncludePassedSegment() {
		return includePassedSegment;
	}

	public void setIncludePassedSegment(boolean includePassedSegment) {
		this.includePassedSegment = includePassedSegment;
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		includePassedSegment = bundle.getBoolean("includePassedSegment");
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putBoolean("includePassedSegment", includePassedSegment);
	}
}
