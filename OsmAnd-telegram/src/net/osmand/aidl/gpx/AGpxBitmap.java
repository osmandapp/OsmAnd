package net.osmand.aidl.gpx;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

public class AGpxBitmap implements Parcelable {

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

	public static final Creator<AGpxBitmap> CREATOR = new
			Creator<AGpxBitmap>() {
				public AGpxBitmap createFromParcel(Parcel in) {
					return new AGpxBitmap(in);
				}

				public AGpxBitmap[] newArray(int size) {
					return new AGpxBitmap[size];
				}
			};

	public void writeToParcel(Parcel out, int flags) {
		out.writeParcelable(bitmap, flags);
	}

	private void readFromParcel(Parcel in) {
		bitmap = in.readParcelable(Bitmap.class.getClassLoader());
	}

	public int describeContents() {
		return 0;
	}
}
