package net.osmand.aidl.navdrawer;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class NavDrawerFooterParams implements Parcelable {

	@NonNull
	private String packageName;
	@Nullable
	private String intent;
	@Nullable
	private String appName;

	@NonNull
	public String getPackageName() {
		return packageName;
	}

	@Nullable
	public String getIntent() {
		return intent;
	}

	@Nullable
	public String getAppName() {
		return appName;
	}

	public NavDrawerFooterParams(@NonNull String packageName, @Nullable String intent,
			@Nullable String appName) {
		this.packageName = packageName;
		this.intent = intent;
		this.appName = appName;
	}

	protected NavDrawerFooterParams(Parcel in) {
		packageName = in.readString();
		intent = in.readString();
		appName = in.readString();
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(packageName);
		dest.writeString(intent);
		dest.writeString(appName);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	public static final Creator<NavDrawerFooterParams> CREATOR = new Creator<NavDrawerFooterParams>() {
		@Override
		public NavDrawerFooterParams createFromParcel(Parcel in) {
			return new NavDrawerFooterParams(in);
		}

		@Override
		public NavDrawerFooterParams[] newArray(int size) {
			return new NavDrawerFooterParams[size];
		}
	};
}
