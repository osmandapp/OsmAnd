package net.osmand.aidlapi.map;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

/**
 * Screenshot of the map screen as JPEG.
 */
public class MapScreenshotParams extends AidlParams {

	private int maxWidth = 0;
	private int quality = 0;

	public MapScreenshotParams(int maxWidth, int quality) {
		this.maxWidth = maxWidth;
		this.quality = quality;
	}

	public MapScreenshotParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<MapScreenshotParams> CREATOR = new Creator<MapScreenshotParams>() {
		@Override
		public MapScreenshotParams createFromParcel(Parcel in) {
			return new MapScreenshotParams(in);
		}

		@Override
		public MapScreenshotParams[] newArray(int size) {
			return new MapScreenshotParams[size];
		}
	};

	/**
	 * @return max width in pixels, the image keeps the aspect ratio; 0 is the screen width
	 */
	public int getMaxWidth() {
		return maxWidth;
	}

	/**
	 * @return JPEG quality 1-100; 0 is the default (80)
	 */
	public int getQuality() {
		return quality;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putInt("maxWidth", maxWidth);
		bundle.putInt("quality", quality);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		maxWidth = bundle.getInt("maxWidth", 0);
		quality = bundle.getInt("quality", 0);
	}
}
