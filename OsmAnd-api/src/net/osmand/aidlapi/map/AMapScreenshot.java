package net.osmand.aidlapi.map;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

/**
 * Screenshot of the map screen: the map with tracks, 3D and widgets as the user sees it.
 */
public class AMapScreenshot extends AidlParams {

	private byte[] image;
	private int width;
	private int height;

	public AMapScreenshot(byte[] image, int width, int height) {
		this.image = image;
		this.width = width;
		this.height = height;
	}

	public AMapScreenshot(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<AMapScreenshot> CREATOR = new Creator<AMapScreenshot>() {
		@Override
		public AMapScreenshot createFromParcel(Parcel in) {
			return new AMapScreenshot(in);
		}

		@Override
		public AMapScreenshot[] newArray(int size) {
			return new AMapScreenshot[size];
		}
	};

	/**
	 * @return JPEG image
	 */
	public byte[] getImage() {
		return image;
	}

	/**
	 * @return image width in pixels
	 */
	public int getWidth() {
		return width;
	}

	/**
	 * @return image height in pixels
	 */
	public int getHeight() {
		return height;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putByteArray("image", image);
		bundle.putInt("width", width);
		bundle.putInt("height", height);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		image = bundle.getByteArray("image");
		width = bundle.getInt("width");
		height = bundle.getInt("height");
	}
}
