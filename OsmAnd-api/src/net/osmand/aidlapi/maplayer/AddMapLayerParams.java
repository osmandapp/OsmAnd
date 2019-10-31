package net.osmand.aidlapi.maplayer;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

public class AddMapLayerParams extends AidlParams {

	private AMapLayer layer;

	public AddMapLayerParams(AMapLayer layer) {
		this.layer = layer;
	}

	public AddMapLayerParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<AddMapLayerParams> CREATOR = new Creator<AddMapLayerParams>() {
		@Override
		public AddMapLayerParams createFromParcel(Parcel in) {
			return new AddMapLayerParams(in);
		}

		@Override
		public AddMapLayerParams[] newArray(int size) {
			return new AddMapLayerParams[size];
		}
	};

	public AMapLayer getLayer() {
		return layer;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putParcelable("layer", layer);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		bundle.setClassLoader(AMapLayer.class.getClassLoader());
		layer = bundle.getParcelable("layer");
	}
}