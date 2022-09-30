package net.osmand.aidlapi.customization;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

public class ZoomLimitsParams extends AidlParams {

	public static final String MIN_ZOOM_KEY = "minZoom";
	public static final String MAX_ZOOM_KEY = "maxZoom";

	private int minZoom;
	private int maxZoom;

	public ZoomLimitsParams(int minZoom, int maxZoom) {
		this.minZoom = minZoom;
		this.maxZoom = maxZoom;
	}

	public ZoomLimitsParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<ZoomLimitsParams> CREATOR = new Creator<ZoomLimitsParams>() {
		@Override
		public ZoomLimitsParams createFromParcel(Parcel in) {
			return new ZoomLimitsParams(in);
		}

		@Override
		public ZoomLimitsParams[] newArray(int size) {
			return new ZoomLimitsParams[size];
		}
	};

	public int getMinZoom() {
		return minZoom;
	}

	public int getMaxZoom() {
		return maxZoom;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putInt(MIN_ZOOM_KEY, minZoom);
		bundle.putInt(MAX_ZOOM_KEY, maxZoom);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		minZoom = bundle.getInt(MIN_ZOOM_KEY);
		maxZoom = bundle.getInt(MAX_ZOOM_KEY);
	}
}