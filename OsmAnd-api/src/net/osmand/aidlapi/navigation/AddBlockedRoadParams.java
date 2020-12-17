package net.osmand.aidlapi.navigation;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

public class AddBlockedRoadParams extends AidlParams {

	private ABlockedRoad blockedRoad;

	public AddBlockedRoadParams(ABlockedRoad blockedRoad) {
		this.blockedRoad = blockedRoad;
	}

	public AddBlockedRoadParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<AddBlockedRoadParams> CREATOR = new Creator<AddBlockedRoadParams>() {
		@Override
		public AddBlockedRoadParams createFromParcel(Parcel in) {
			return new AddBlockedRoadParams(in);
		}

		@Override
		public AddBlockedRoadParams[] newArray(int size) {
			return new AddBlockedRoadParams[size];
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