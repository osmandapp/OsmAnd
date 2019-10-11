package net.osmand.aidlapi.maplayer;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

public class UpdateMapLayerParams extends AidlParams {

	private AMapLayer layer;

	public UpdateMapLayerParams(AMapLayer layer) {
		this.layer = layer;
	}

	public UpdateMapLayerParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<UpdateMapLayerParams> CREATOR = new Creator<UpdateMapLayerParams>() {
		@Override
		public UpdateMapLayerParams createFromParcel(Parcel in) {
			return new UpdateMapLayerParams(in);
		}

		@Override
		public UpdateMapLayerParams[] newArray(int size) {
			return new UpdateMapLayerParams[size];
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