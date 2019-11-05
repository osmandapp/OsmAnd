package net.osmand.aidl.navdrawer;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class NavDrawerHeaderParams implements Parcelable {

	@NonNull
	private String imageUri;
	@NonNull
	private String packageName;
	@Nullable
	private String intent;

	@NonNull
	public String getImageUri() {
		return imageUri;
	}

	@NonNull
	public String getPackageName() {
		return packageName;
	}

	@Nullable
	public String getIntent() {
		return intent;
	}

	public NavDrawerHeaderParams(@NonNull String imageUri, @NonNull String packageName,
			@Nullable String intent) {
		this.imageUri = imageUri;
		this.packageName = packageName;
		this.intent = intent;
	}

	public NavDrawerHeaderParams(Parcel in) {
		imageUri = in.readString();
		packageName = in.readString();
		intent = in.readString();
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(imageUri);
		dest.writeString(packageName);
		dest.writeString(intent);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	public static final Creator<NavDrawerHeaderParams> CREATOR = new Creator<NavDrawerHeaderParams>() {
		@Override
		public NavDrawerHeaderParams createFromParcel(Parcel in) {
			return new NavDrawerHeaderParams(in);
		}

		@Override
		public NavDrawerHeaderParams[] newArray(int size) {
			return new NavDrawerHeaderParams[size];
		}
	};
}
