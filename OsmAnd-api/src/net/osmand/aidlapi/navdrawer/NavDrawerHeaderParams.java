package net.osmand.aidlapi.navdrawer;

import android.os.Bundle;
import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.osmand.aidlapi.AidlParams;

public class NavDrawerHeaderParams extends AidlParams {

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
		readFromParcel(in);
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

	@Override
	protected void readFromBundle(Bundle bundle) {
		imageUri = bundle.getString("imageUri", "");
		packageName = bundle.getString("packageName", "");
		intent = bundle.getString("intent");
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putString("imageUri", imageUri);
		bundle.putString("packageName", packageName);
		bundle.putString("intent", intent);
	}
}