package net.osmand.aidlapi.navigation;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

public class RemoveBlockedRoadParams extends AidlParams {

	private ABlockedRoad blockedRoad;

	public RemoveBlockedRoadParams(ABlockedRoad blockedRoad) {
		this.blockedRoad = blockedRoad;
	}

	public RemoveBlockedRoadParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<RemoveBlockedRoadParams> CREATOR = new Creator<RemoveBlockedRoadParams>() {
		@Override
		public RemoveBlockedRoadParams createFromParcel(Parcel in) {
			return new RemoveBlockedRoadParams(in);
		}

		@Override
		public RemoveBlockedRoadParams[] newArray(int size) {
			return new RemoveBlockedRoadParams[size];
		}
	};

	public ABlockedRoad getBlockedRoad() {
		return blockedRoad;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putParcelable("blockedRoad", blockedRoad);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		bundle.setClassLoader(ABlockedRoad.class.getClassLoader());
		blockedRoad = bundle.getParcelable("blockedRoad");
	}
}