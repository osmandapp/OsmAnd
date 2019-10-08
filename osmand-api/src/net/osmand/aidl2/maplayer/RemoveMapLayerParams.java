package net.osmand.aidl2.maplayer;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidl2.AidlParams;

public class RemoveMapLayerParams extends AidlParams {

	private String id;

	public RemoveMapLayerParams(String id) {
		this.id = id;
	}

	public RemoveMapLayerParams(Parcel in) {
		super(in);
	}

	public static final Creator<RemoveMapLayerParams> CREATOR = new Creator<RemoveMapLayerParams>() {
		@Override
		public RemoveMapLayerParams createFromParcel(Parcel in) {
			return new RemoveMapLayerParams(in);
		}

		@Override
		public RemoveMapLayerParams[] newArray(int size) {
			return new RemoveMapLayerParams[size];
		}
	};

	public String getId() {
		return id;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putString("id", id);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		id = bundle.getString("id");
	}
}