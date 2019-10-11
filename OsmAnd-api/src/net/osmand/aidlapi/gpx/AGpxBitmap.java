package net.osmand.aidlapi.gpx;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Parcel;
import android.support.annotation.NonNull;

import net.osmand.aidlapi.AidlParams;

public class AGpxBitmap extends AidlParams {

	private Bitmap bitmap;

	public AGpxBitmap(@NonNull Bitmap bitmap) {
		this.bitmap = bitmap;
	}

	public AGpxBitmap(Parcel in) {
		readFromParcel(in);
	}

	public Bitmap getBitmap() {
		return bitmap;
	}

	public static final Creator<AGpxBitmap> CREATOR = new Creator<AGpxBitmap>() {
		@Override
		public AGpxBitmap createFromParcel(Parcel in) {
			return new AGpxBitmap(in);
		}

		@Override
		public AGpxBitmap[] newArray(int size) {
			return new AGpxBitmap[size];
		}
	};

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putParcelable("bitmap", bitmap);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		bundle.setClassLoader(Bitmap.class.getClassLoader());
		bitmap = bundle.getParcelable("bitmap");
	}
}